package com.mediasandesh.news.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.mediasandesh.news.LocalePrefs
import com.mediasandesh.news.MainActivity
import com.mediasandesh.news.R
import com.mediasandesh.news.data.NewsTranslator
import com.mediasandesh.news.data.Repository
import com.mediasandesh.news.ui.formatDate
import com.mediasandesh.news.withAppLocale
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

class HeadlinesWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { manager.updateAppWidget(it, views(context, it)) }
        manager.notifyAppWidgetViewDataChanged(ids, R.id.widget_list)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) refresh(context)
    }

    companion object {
        const val EXTRA_POST = "post_id"
        private const val ACTION_REFRESH = "com.mediasandesh.news.widget.REFRESH"

        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, HeadlinesWidget::class.java))
            if (ids.isNotEmpty()) manager.notifyAppWidgetViewDataChanged(ids, R.id.widget_list)
        }

        private fun views(context: Context, widgetId: Int): RemoteViews {
            val localized = context.withAppLocale()
            val openApp = PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val openStory = PendingIntent.getActivity(
                context, 1,
                Intent(context, MainActivity::class.java).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                ),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            val refresh = PendingIntent.getBroadcast(
                context, 2,
                Intent(context, HeadlinesWidget::class.java).setAction(ACTION_REFRESH),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val adapter = Intent(context, HeadlinesService::class.java)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                .also { it.data = android.net.Uri.parse(it.toUri(Intent.URI_INTENT_SCHEME)) }

            return RemoteViews(context.packageName, R.layout.widget_headlines).apply {
                setOnClickPendingIntent(R.id.widget_header, openApp)
                setOnClickPendingIntent(R.id.widget_refresh, refresh)
                @Suppress("DEPRECATION")
                setRemoteAdapter(R.id.widget_list, adapter)
                setEmptyView(R.id.widget_list, R.id.widget_empty)
                setTextViewText(R.id.widget_empty, localized.getString(R.string.widget_loading))
                setPendingIntentTemplate(R.id.widget_list, openStory)
            }
        }
    }
}

class HeadlinesService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory = HeadlinesFactory(applicationContext)
}

private class Headline(val id: Int, val title: String, val meta: String, val image: Bitmap?)

private class HeadlinesFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {
    private var items = emptyList<Headline>()

    override fun onCreate() = Unit

    override fun onDataSetChanged() {
        context.withAppLocale()
        val loaded = runBlocking {
            withTimeoutOrNull(25_000) {
                runCatching {
                    var posts = Repository.latest(1).take(8)
                    if (LocalePrefs.get(context) == "en") {
                        posts = withTimeoutOrNull(8_000) { NewsTranslator.postsTitles(posts) } ?: posts
                    }
                    posts.map { post ->
                        Headline(post.id, post.headline, formatDate(post.date), post.imageUrl?.let { thumbnail(it) })
                    }
                }.getOrNull()
            }
        }
        if (!loaded.isNullOrEmpty()) items = loaded
    }

    private suspend fun thumbnail(url: String): Bitmap? {
        val request = ImageRequest.Builder(context).data(url).size(240, 156).allowHardware(false).build()
        return (context.imageLoader.execute(request) as? SuccessResult)?.drawable?.toBitmap(240, 156)
    }

    override fun getViewAt(position: Int): RemoteViews {
        val item = items.getOrNull(position) ?: return RemoteViews(context.packageName, R.layout.widget_item)
        return RemoteViews(context.packageName, R.layout.widget_item).apply {
            setTextViewText(R.id.item_title, item.title)
            setTextViewText(R.id.item_meta, item.meta)
            if (item.image != null) {
                setImageViewBitmap(R.id.item_image, item.image)
                setViewVisibility(R.id.item_image, android.view.View.VISIBLE)
            } else {
                setViewVisibility(R.id.item_image, android.view.View.GONE)
            }
            setOnClickFillInIntent(R.id.item_root, Intent().putExtra(HeadlinesWidget.EXTRA_POST, item.id))
        }
    }

    override fun getCount() = items.size
    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount() = 1
    override fun getItemId(position: Int) = items.getOrNull(position)?.id?.toLong() ?: position.toLong()
    override fun hasStableIds() = true
    override fun onDestroy() = Unit
}
