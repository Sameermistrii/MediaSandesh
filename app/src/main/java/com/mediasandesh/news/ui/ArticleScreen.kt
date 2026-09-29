package com.mediasandesh.news.ui

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material3.MaterialTheme
import coil.imageLoader
import com.mediasandesh.news.AppPrefs
import com.mediasandesh.news.LocalePrefs
import com.mediasandesh.news.R
import com.mediasandesh.news.Speaker
import com.mediasandesh.news.data.Library
import com.mediasandesh.news.data.NewsTranslator
import com.mediasandesh.news.data.Repository
import com.mediasandesh.news.data.WpPost
import com.mediasandesh.news.data.decodeHtml
import org.json.JSONArray
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val fontSizes = intArrayOf(15, 17, 20, 23)
private const val WORDS_PER_MINUTE = 200
private val hiddenTagCategories = setOf(1, 79)

@Composable
fun ArticleScreen(postId: Int, onOpen: (Int) -> Unit, onBack: () -> Unit = {}, onSubmit: () -> Unit = {}) {
    var post by remember { mutableStateOf<WpPost?>(null) }
    var original by remember { mutableStateOf<WpPost?>(null) }
    var related by remember { mutableStateOf<List<WpPost>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var translating by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    var progress by remember { mutableFloatStateOf(0f) }
    val context = LocalContext.current
    val english = LocalePrefs.get(context) == "en"
    val categoryNames = rememberCategoryNames(english)

    DisposableEffect(postId) { onDispose { Speaker.stop() } }

    LaunchedEffect(postId, retry) {
        loading = true
        translating = false
        error = null
        try {
            val loaded = withContext(Dispatchers.IO) { Repository.post(postId) }
            original = loaded
            post = loaded
            loading = false
            Library.markRead(loaded)
            if (english) {
                translating = true
                post = withContext(Dispatchers.IO) { NewsTranslator.postFull(loaded) }
                translating = false
            }
            val more = withContext(Dispatchers.IO) { runCatching { Repository.related(loaded) }.getOrDefault(emptyList()) }
            related = more
            if (english && more.isNotEmpty()) {
                related = withContext(Dispatchers.IO) { NewsTranslator.postsTitles(more) }
            }
        } catch (e: Exception) {
            error = e.message ?: "Network error"
            loading = false
            translating = false
        }
    }

    val item = post
    val listenId = "article-${item?.id}"
    val listening = item != null && Speaker.playing == listenId
    val web = remember { arrayOfNulls<WebView>(1) }
    fun toggleListen() {
        val p = item ?: return
        if (listening) return Speaker.stop()
        val view = web[0] ?: return Speaker.speak(context, listenId, listOf(p.headline, p.content.rendered.decodeHtml()))
        val script = "(function(){var h=document.querySelector('h1'),b=document.querySelector('.body');" +
            "return [h?h.innerText:'',b?b.innerText:''];})()"
        view.evaluateJavascript(script) { result ->
            val shown = runCatching { JSONArray(result) }.getOrNull()
            val headline = shown?.optString(0).orEmpty().ifBlank { p.headline }
            val body = shown?.optString(1).orEmpty().ifBlank { p.content.rendered.decodeHtml() }
            Speaker.speak(context, listenId, listOf(headline, body))
        }
    }

    Column(Modifier.fillMaxSize().background(CardWhite)) {
        ArticleTopBar(
            item = item,
            saveTarget = original ?: item,
            listening = listening,
            onBack = onBack,
            onListen = ::toggleListen
        )
        Box(Modifier.fillMaxWidth().height(3.dp).background(Hairline)) {
            Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(3.dp).background(BrandRed))
        }
        when {
            loading -> Loading()
            error != null || item == null -> ErrorState(error ?: stringResource(R.string.article_missing), onRetry = { retry++ })
            else -> {
                if (translating) {
                    Text(
                        stringResource(R.string.translating),
                        color = Muted,
                        modifier = Modifier.fillMaxWidth().background(ChipBg).padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
                Box(Modifier.fillMaxWidth().weight(1f)) {
                    ArticleWeb(
                        post = item,
                        category = item.categoryTag(categoryNames),
                        place = item.place()?.label(english),
                        tags = item.categories.filter { it !in hiddenTagCategories }.mapNotNull { categoryNames[it] }.distinct(),
                        related = related,
                        english = english,
                        listening = listening,
                        onOpen = onOpen,
                        onListen = ::toggleListen,
                        onSubmit = onSubmit,
                        onProgress = { progress = it },
                        onWebView = { web[0] = it },
                        modifier = Modifier.fillMaxSize()
                    )
                    TextSizePill(Modifier.align(Alignment.BottomEnd).padding(16.dp))
                }
            }
        }
    }
}

@Composable
private fun ArticleTopBar(item: WpPost?, saveTarget: WpPost?, listening: Boolean, onBack: () -> Unit, onListen: () -> Unit) {
    val context = LocalContext.current
    val saved = item != null && Library.isSaved(item.id)
    Row(
        Modifier.fillMaxWidth().height(56.dp).background(CardWhite).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = Ink)
        }
        Image(
            painterResource(R.mipmap.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(colorResource(R.color.ic_launcher_background))
                .graphicsLayer { scaleX = 1.45f; scaleY = 1.45f }
        )
        Text(
            stringResource(R.string.brand_name),
            color = BrandRed,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 17.sp,
            maxLines = 1,
            modifier = Modifier.padding(start = 8.dp).weight(1f)
        )
        if (item != null) {
            IconButton(onClick = onListen) {
                Icon(
                    if (listening) Icons.Default.StopCircle else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = stringResource(if (listening) R.string.listen_stop else R.string.listen),
                    tint = if (listening) RedInk else Ink
                )
            }
            IconButton(onClick = { saveTarget?.let { Library.toggleSaved(it) } }) {
                Icon(
                    if (saved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = stringResource(if (saved) R.string.unsave else R.string.save),
                    tint = if (saved) RedInk else Ink
                )
            }
            IconButton(onClick = { shareText(context, item.headline, item.link, whatsapp = false) }) {
                Icon(Icons.Default.Share, contentDescription = stringResource(R.string.share), tint = Ink)
            }
        }
    }
}

@Composable
private fun TextSizePill(modifier: Modifier) {
    val context = LocalContext.current
    Row(
        modifier
            .shadow(6.dp, CircleShape)
            .clip(CircleShape)
            .background(CardWhite),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val smaller = AppPrefs.textSize > 0
        val larger = AppPrefs.textSize < fontSizes.lastIndex
        Text(
            "अ-",
            color = if (smaller) Ink else Muted.copy(alpha = 0.5f),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier
                .clickable(enabled = smaller) { AppPrefs.setTextSize(context, AppPrefs.textSize - 1) }
                .padding(horizontal = 14.dp, vertical = 9.dp)
        )
        Box(Modifier.width(1.dp).height(18.dp).background(Hairline))
        Text(
            "अ+",
            color = if (larger) RedInk else Muted.copy(alpha = 0.5f),
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            modifier = Modifier
                .clickable(enabled = larger) { AppPrefs.setTextSize(context, AppPrefs.textSize + 1) }
                .padding(horizontal = 14.dp, vertical = 7.dp)
        )
    }
}

private class WebState {
    var page: String? = null
    var loaded = false
    var relatedJs: String? = null
    var listening: Boolean? = null
}

private fun String.html(): String = replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

private fun String.js(): String = replace("\\", "\\\\").replace("'", "\\'").replace("\n", " ").replace("\r", " ")

private fun Color.css(): String = String.format("#%06X", 0xFFFFFF and toArgb())

private fun svg(path: String, size: Int = 18) =
    """<svg width="$size" height="$size" viewBox="0 0 24 24" fill="currentColor"><path d="$path"/></svg>"""

private const val ICON_PLAY = "M8 5v14l11-7z"
private const val ICON_STOP = "M6 6h12v12H6z"
private const val ICON_PIN = "M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5a2.5 2.5 0 010-5 2.5 2.5 0 010 5z"
private const val ICON_CLOCK = "M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zm.5-13H11v6l5.25 3.15.75-1.23-4.5-2.67z"
private const val ICON_VERIFIED = "M12 2a10 10 0 100 20 10 10 0 000-20zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"
private const val ICON_WHATSAPP = "M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 012.893 6.994c-.003 5.45-4.437 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0012.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L.057 24l6.305-1.654a11.882 11.882 0 005.683 1.448h.005c6.554 0 11.89-5.335 11.893-11.893a11.821 11.821 0 00-3.48-8.413z"
private const val ICON_SEND = "M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"
private const val ICON_LINK = "M3.9 12c0-1.71 1.39-3.1 3.1-3.1h4V7H7a5 5 0 000 10h4v-1.9H7c-1.71 0-3.1-1.39-3.1-3.1zM8 13h8v-2H8v2zm9-6h-4v1.9h4c1.71 0 3.1 1.39 3.1 3.1s-1.39 3.1-3.1 3.1h-4V17h4a5 5 0 000-10z"
private const val ICON_CAMERA = "M12 15.2a3.2 3.2 0 100-6.4 3.2 3.2 0 000 6.4zM9 2L7.17 4H4a2 2 0 00-2 2v12a2 2 0 002 2h16a2 2 0 002-2V6a2 2 0 00-2-2h-3.17L15 2H9z"

private fun fullDate(raw: String, english: Boolean): String = runCatching {
    val locale = if (english) Locale.ENGLISH else Locale("hi", "IN")
    LocalDateTime.parse(raw.take(19)).format(DateTimeFormatter.ofPattern("d MMMM yyyy", locale))
}.getOrDefault("")

private fun share(context: Context, target: String, post: WpPost) {
    val link = Uri.encode(post.link)
    val text = Uri.encode(post.headline)
    when (target) {
        "whatsapp" -> shareText(context, post.headline, post.link, whatsapp = true)
        "facebook" -> openUrl(context, "https://www.facebook.com/sharer/sharer.php?u=$link")
        "x" -> openUrl(context, "https://twitter.com/intent/tweet?text=$text&url=$link")
        "telegram" -> openUrl(context, "https://t.me/share/url?url=$link&text=$text")
        "copy" -> {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("link", post.link))
            Toast.makeText(context, context.getString(R.string.video_link_copied), Toast.LENGTH_SHORT).show()
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun ArticleWeb(
    post: WpPost,
    category: String?,
    place: String?,
    tags: List<String>,
    related: List<WpPost>,
    english: Boolean,
    listening: Boolean,
    onOpen: (Int) -> Unit,
    onListen: () -> Unit,
    onSubmit: () -> Unit,
    onProgress: (Float) -> Unit,
    onWebView: (WebView?) -> Unit,
    modifier: Modifier
) {
    DisposableEffect(Unit) { onDispose { onWebView(null) } }
    val openArticle by rememberUpdatedState(onOpen)
    val listen by rememberUpdatedState(onListen)
    val submit by rememberUpdatedState(onSubmit)
    val progress by rememberUpdatedState(onProgress)
    val current by rememberUpdatedState(post)
    val context = LocalContext.current
    val background = CardWhite.css()
    val page_ = Paper.css()
    val text = Ink.css()
    val muted = Muted.css()
    val line = Hairline.css()
    val chip = ChipBg.css()
    val red = BrandRed.css()
    val redInk = RedInk.css()
    val tint = MaterialTheme.colorScheme.secondaryContainer.css()
    val fontPx = fontSizes[AppPrefs.textSize.coerceIn(0, fontSizes.lastIndex)]
    val state = remember { WebState() }

    val listenTitle = stringResource(R.string.article_listen_title)
    val listenStop = stringResource(R.string.article_listen_stop)
    val words = remember(post) { post.content.rendered.decodeHtml().split(Regex("\\s+")).count { it.isNotBlank() } }
    val minutes = (words / WORDS_PER_MINUTE).coerceAtLeast(1)
    val listenTime = stringResource(R.string.article_listen_time, minutes)
    val readTime = stringResource(R.string.article_read_time, minutes)
    val shareTitle = stringResource(R.string.article_share_title)
    val copyLabel = stringResource(R.string.article_copy)
    val submitTitle = stringResource(R.string.article_submit_title)
    val submitBody = stringResource(R.string.article_submit_body)
    val submitCta = stringResource(R.string.article_submit_cta)
    val relatedTitle = stringResource(R.string.section_related)

    val page = remember(post, background, text, category, place, tags) {
        val body = post.content.rendered
            .replace(Regex("(?is)<script.*?>.*?</script>"), "")
            .replace(Regex("(?is)</?(meta|base|form|object|embed)\\b[^>]*>"), "")
        val nonce = java.util.UUID.randomUUID().toString().replace("-", "")
        val image = post.imageUrl?.let { "<div class=\"hero\"><img src=\"${it.html()}\"></div>" } ?: ""
        val pills = buildString {
            category?.let { append("<span class=\"pill cat\">${it.html()}</span>") }
            place?.let { append("<span class=\"pill loc\">${svg(ICON_PIN, 14)}${it.html()}</span>") }
        }
        val author = post.authorName
        val initial = author.trim().firstOrNull()?.toString()?.html() ?: "म"
        val date = fullDate(post.date, english)
        val tagHtml = tags.joinToString("") { "<span class=\"tag\">#${it.replace(" ", "").html()}</span>" }
        """
        <!DOCTYPE html><html><head><meta charset="utf-8">
        <meta http-equiv="Content-Security-Policy" content="default-src 'none'; script-src 'nonce-$nonce'; style-src 'unsafe-inline'; img-src https: http: data:; media-src https: http:; frame-src https:; font-src https: data:; base-uri 'none'; form-action 'none'">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <style>
          * { box-sizing: border-box; }
          body { font-family: 'Noto Sans', sans-serif; font-size: ${fontPx}px; line-height: 1.8; color: $text; background: $background; margin: 0; padding: 0 0 88px; word-wrap: break-word; -webkit-tap-highlight-color: transparent; }
          .wrap { padding: 0 16px; }
          svg { vertical-align: middle; flex-shrink: 0; }
          .pills { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 16px; }
          .pill { display: inline-flex; align-items: center; gap: 4px; padding: 4px 12px; border-radius: 999px; font-size: 12px; font-weight: 700; line-height: 1.4; }
          .pill.cat { background: $red; color: #fff; }
          .pill.loc { background: $chip; color: $muted; font-weight: 600; }
          .pill.loc svg { color: $redInk; }
          h1 { font-size: 24px; line-height: 1.42; font-weight: 800; margin: 12px 0 14px; letter-spacing: -0.2px; }
          .author { display: flex; align-items: center; gap: 10px; margin-bottom: 16px; }
          .avatar { width: 40px; height: 40px; border-radius: 50%; background: $tint; color: $redInk; display: flex; align-items: center; justify-content: center; font-weight: 800; font-size: 17px; flex-shrink: 0; }
          .aname { font-weight: 700; font-size: 14px; line-height: 1.3; display: flex; align-items: center; gap: 4px; }
          .aname svg { color: $redInk; }
          .ameta { color: $muted; font-size: 12px; line-height: 1.4; display: flex; align-items: center; flex-wrap: wrap; gap: 4px; }
          .listen { display: flex; align-items: center; gap: 12px; padding: 10px 14px 10px 10px; border-radius: 16px; background: $page_; border: 1px solid $line; margin-bottom: 16px; text-decoration: none; color: $text; }
          .play { width: 42px; height: 42px; border-radius: 50%; background: $red; color: #fff; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
          .ltext { flex: 1; line-height: 1.35; }
          .ltitle { display: block; font-weight: 700; font-size: 14px; }
          .lsub { display: block; font-size: 12px; color: $muted; margin-top: 2px; }
          .wave { display: flex; align-items: center; gap: 3px; height: 28px; }
          .wave i { width: 4px; border-radius: 4px; background: $red; opacity: .85; }
          .listening .wave i { animation: wv .9s ease-in-out infinite alternate; }
          .wave i:nth-child(2) { animation-delay: .15s } .wave i:nth-child(3) { animation-delay: .3s } .wave i:nth-child(4) { animation-delay: .45s } .wave i:nth-child(5) { animation-delay: .6s }
          @keyframes wv { from { transform: scaleY(.4) } to { transform: scaleY(1) } }
          .hero { width: 100%; aspect-ratio: 16 / 10; border-radius: 16px; overflow: hidden; background: $chip; margin-bottom: 18px; }
          .hero img { width: 100%; height: 100%; object-fit: cover; display: block; }
          img, iframe, video { max-width: 100%; height: auto; border-radius: 12px; }
          iframe { width: 100%; aspect-ratio: 16 / 9; border: 0; }
          p { margin: 0 0 16px; }
          .body, .body * { font-size: 1em !important; line-height: 1.8 !important; font-family: 'Noto Sans', sans-serif !important; color: inherit !important; background: transparent !important; }
          .body h2, .body h3, .body h4 { font-size: 1.12em !important; line-height: 1.5 !important; font-weight: 800 !important; border-left: 5px solid $red; padding-left: 10px; margin: 22px 0 12px; }
          .body .plain { font-size: 1em !important; line-height: 1.8 !important; font-weight: 400 !important; border: 0; padding: 0; margin: 0 0 16px; }
          .body blockquote { margin: 18px 0; padding: 14px 16px; border-left: 5px solid $red; border-radius: 0 14px 14px 0; background: $tint !important; font-style: italic; }
          .body blockquote p { margin: 0; }
          .body figcaption, .body .wp-caption-text { color: $muted !important; font-size: 0.8em !important; line-height: 1.5 !important; margin-top: 6px; }
          .body figure { margin: 16px 0; }
          .body a { color: $redInk !important; }
          .section { margin-top: 26px; }
          .stitle { font-weight: 800; font-size: 16px; margin-bottom: 10px; display: flex; align-items: center; gap: 8px; }
          .bar { width: 4px; height: 20px; border-radius: 2px; background: $red; }
          .share { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
          .sbtn { width: 44px; height: 44px; border-radius: 50%; display: inline-flex; align-items: center; justify-content: center; color: #fff; text-decoration: none; font-weight: 800; font-size: 18px; }
          .copy { height: 44px; padding: 0 14px; border-radius: 999px; background: $chip; color: $text; display: inline-flex; align-items: center; gap: 6px; text-decoration: none; font-size: 13px; font-weight: 600; }
          .tags { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 18px; }
          .tag { padding: 5px 12px; border-radius: 999px; background: $chip; font-size: 12px; font-weight: 500; line-height: 1.5; }
          .rel { display: flex; gap: 12px; align-items: center; padding: 12px; border-radius: 16px; background: $background; border: 1px solid $line; margin-bottom: 10px; text-decoration: none; color: $text; }
          .rel .rc { flex: 1; min-width: 0; }
          .rel .rt { font-weight: 700; font-size: 15px; line-height: 1.45; display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden; }
          .rel .rm { color: $muted; font-size: 12px; margin-top: 4px; display: flex; align-items: center; gap: 4px; font-weight: 400; }
          .rel img { width: 96px; height: 80px; object-fit: cover; border-radius: 12px; flex-shrink: 0; background: $chip; }
          .submit { margin-top: 26px; padding: 16px; border-radius: 16px; background: #047857; color: #fff; display: flex; align-items: center; gap: 12px; text-decoration: none; line-height: 1.35; }
          .submit .ic { width: 44px; height: 44px; border-radius: 12px; background: rgba(255,255,255,.18); display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
          .submit .st { display: block; font-weight: 800; font-size: 15px; line-height: 1.35; }
          .submit .sb { display: block; font-size: 12px; opacity: .9; line-height: 1.4; margin-top: 2px; }
          .submit .cta { margin-left: auto; background: #fff; color: #047857; border-radius: 999px; padding: 8px 12px; font-size: 12px; font-weight: 800; white-space: nowrap; }
        </style>
        <script nonce="$nonce">
          document.addEventListener('DOMContentLoaded', function() {
            document.querySelectorAll('.body h1, .body h2, .body h3, .body h4, .body h5, .body h6').forEach(function(h) {
              if (h.textContent.trim().length > 90) h.classList.add('plain');
            });
          });
          document.addEventListener('error', function(e) { if (e.target.tagName === 'IMG') e.target.style.display = 'none'; }, true);
          function setListening(on) {
            var card = document.getElementById('listen');
            if (!card) return;
            card.classList.toggle('listening', on);
            document.getElementById('lplay').innerHTML = on ? '${svg(ICON_STOP, 20).js()}' : '${svg(ICON_PLAY, 24).js()}';
            document.getElementById('ltitle').textContent = on ? '${listenStop.js()}' : '${listenTitle.js()}';
          }
        </script>
        </head><body>
          <div class="wrap">
            <div class="pills">$pills</div>
            <h1>${post.headline.html()}</h1>
            <div class="author">
              <div class="avatar">$initial</div>
              <div>
                <div class="aname">${author.html()} ${svg(ICON_VERIFIED, 15)}</div>
                <div class="ameta">${if (date.isNotBlank()) "<span>${date.html()}</span><span>•</span>" else ""}<span>${formatDate(post.date).html()}</span><span>•</span>${svg(ICON_CLOCK, 12)}<span>${readTime.html()}</span></div>
              </div>
            </div>
            <a class="listen" id="listen" href="msapp://listen">
              <span class="play" id="lplay">${svg(ICON_PLAY, 24)}</span>
              <span class="ltext"><span class="ltitle" id="ltitle">${listenTitle.html()}</span><span class="lsub">${listenTime.html()}</span></span>
              <span class="wave"><i style="height:10px"></i><i style="height:22px"></i><i style="height:14px"></i><i style="height:26px"></i><i style="height:16px"></i></span>
            </a>
            $image
            <div class="body">$body</div>
            <div class="section">
              <div class="stitle">${shareTitle.html()}</div>
              <div class="share">
                <a class="sbtn" style="background:#25D366" href="msapp://share/whatsapp">${svg(ICON_WHATSAPP, 24)}</a>
                <a class="sbtn" style="background:#1877F2" href="msapp://share/facebook">f</a>
                <a class="sbtn" style="background:#000" href="msapp://share/x">𝕏</a>
                <a class="sbtn" style="background:#229ED9" href="msapp://share/telegram">${svg(ICON_SEND, 20)}</a>
                <a class="copy" href="msapp://share/copy">${svg(ICON_LINK, 18)}${copyLabel.html()}</a>
              </div>
            </div>
            ${if (tagHtml.isNotEmpty()) "<div class=\"tags\">$tagHtml</div>" else ""}
            <div id="related"></div>
            <a class="submit" href="msapp://submit">
              <span class="ic">${svg(ICON_CAMERA, 22)}</span>
              <span><span class="st">${submitTitle.html()}</span><span class="sb">${submitBody.html()}</span></span>
              <span class="cta">${submitCta.html()}</span>
            </a>
          </div>
        </body></html>
        """.trimIndent()
    }

    val relatedJs = remember(related, relatedTitle) {
        if (related.isEmpty()) null else {
            val cards = related.joinToString("") { r ->
                val thumb = r.imageUrl?.let { "<img src=\"${it.html()}\">" } ?: ""
                "<a class=\"rel\" href=\"msapp://article/${r.id}\"><span class=\"rc\"><span class=\"rt\">${r.headline.html()}</span>" +
                    "<span class=\"rm\">${svg(ICON_CLOCK, 12)}${formatDate(r.date).html()}</span></span>$thumb</a>"
            }
            val html = "<div class=\"section\"><div class=\"stitle\"><span class=\"bar\"></span>${relatedTitle.html()}</div>$cards</div>"
            "document.getElementById('related').innerHTML = '${html.js()}';"
        }
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                onWebView(this)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = true
                setBackgroundColor(0)
                setOnScrollChangeListener { v, _, y, _, _ ->
                    val web = v as WebView
                    val max = web.contentHeight * web.resources.displayMetrics.density - web.height
                    progress(if (max > 0) y / max else 0f)
                }
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        val url = request.url
                        // Page-initiated navigations (no tap) must never trigger app actions or launch other apps.
                        if (!request.hasGesture()) return true
                        if (url.scheme == "msapp") {
                            when (url.host) {
                                "article" -> url.lastPathSegment?.toIntOrNull()?.let { openArticle(it) }
                                "listen" -> listen()
                                "share" -> url.lastPathSegment?.let { share(context, it, current) }
                                "submit" -> submit()
                            }
                        } else if (request.isForMainFrame && url.scheme in setOf("http", "https")) {
                            runCatching { openUrl(context, url.toString()) }
                        }
                        return true
                    }

                    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                        val url = request.url.toString()
                        if (request.method != "GET" || !url.startsWith("http")) return null
                        val snapshot = runCatching { ctx.imageLoader.diskCache?.openSnapshot(url) }.getOrNull() ?: return null
                        return snapshot.use {
                            runCatching {
                                val mime = when {
                                    url.contains(".png", true) -> "image/png"
                                    url.contains(".webp", true) -> "image/webp"
                                    url.contains(".gif", true) -> "image/gif"
                                    else -> "image/jpeg"
                                }
                                WebResourceResponse(mime, null, it.data.toFile().readBytes().inputStream())
                            }.getOrNull()
                        }
                    }

                    override fun onPageFinished(view: WebView, url: String?) {
                        state.loaded = true
                        view.evaluateJavascript("document.body.style.fontSize='${fontSizes[AppPrefs.textSize.coerceIn(0, fontSizes.lastIndex)]}px';", null)
                        state.relatedJs?.let { view.evaluateJavascript(it, null) }
                        state.listening?.let { view.evaluateJavascript("setListening($it);", null) }
                    }
                }
            }
        },
        update = { web ->
            if (state.page != page) {
                state.page = page
                state.loaded = false
                web.settings.cacheMode = if (Repository.offline) WebSettings.LOAD_CACHE_ELSE_NETWORK else WebSettings.LOAD_DEFAULT
                web.loadDataWithBaseURL("https://www.mediasandesh.com", page, "text/html", "utf-8", null)
            }
            if (state.relatedJs != relatedJs) {
                state.relatedJs = relatedJs
                if (state.loaded && relatedJs != null) web.evaluateJavascript(relatedJs, null)
            }
            if (state.listening != listening) {
                state.listening = listening
                if (state.loaded) web.evaluateJavascript("setListening($listening);", null)
            }
            if (state.loaded) web.evaluateJavascript("document.body.style.fontSize='${fontPx}px';", null)
        },
        modifier = modifier
    )
}
