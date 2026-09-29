package com.mediasandesh.news.ui

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.ActivityInfo
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import com.mediasandesh.news.AppPrefs
import com.mediasandesh.news.LocalePrefs
import com.mediasandesh.news.R
import com.mediasandesh.news.data.Cities
import com.mediasandesh.news.data.NewsTranslator
import com.mediasandesh.news.data.VideoItem
import com.mediasandesh.news.data.VideoPage
import com.mediasandesh.news.data.Videos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val translatedTitles = mutableStateMapOf<String, String>()

// YouTube refuses embeds (error 152) unless the page origin is a real website.
private const val EMBED_ORIGIN = "https://www.mediasandesh.com"
private const val CHANNEL_URL = "https://www.youtube.com/channel/UCjGTEJjc-859tcF-7WPXeYg?sub_confirmation=1"
private const val NEW_VIDEO_HOURS = 48L

private val Card16 = RoundedCornerShape(16.dp)
private val Thumb12 = RoundedCornerShape(12.dp)
private val SoftShadow = Color(0x14000000)
private val WhatsAppGreen = Color(0xFF25D366)

private fun Modifier.softCard(shape: RoundedCornerShape = Card16) = this
    .shadow(2.dp, shape, clip = false, ambientColor = SoftShadow, spotColor = SoftShadow)
    .clip(shape)

private fun playerHtml(videoId: String, loop: Boolean) = """
<!DOCTYPE html><html><head>
<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no">
<style>html,body{margin:0;padding:0;width:100%;height:100%;background:#000;overflow:hidden}iframe{position:fixed;top:0;left:0;border:0}</style>
</head><body><div id="p"></div>
<script src="https://www.youtube.com/iframe_api"></script>
<script>
var player;
function fit() {
  var f = document.querySelector('iframe');
  if (f && innerHeight > 0) { f.style.width = innerWidth + 'px'; f.style.height = innerHeight + 'px'; }
}
addEventListener('resize', fit);
function onYouTubeIframeAPIReady() {
  fit();
  player = new YT.Player('p', {
    width: '100%', height: '100%', videoId: '$videoId',
    playerVars: { autoplay: 1, playsinline: 1, controls: 1, fs: 1, rel: 0, iv_load_policy: 3, modestbranding: 1, origin: '$EMBED_ORIGIN' },
    events: {
      onReady: function(e) { fit(); e.target.playVideo(); },
      onStateChange: function(e) { if (e.data === 0 && $loop) { player.seekTo(0); player.playVideo(); } },
      onError: function(e) { Bridge.error(e.data); }
    }
  });
}
</script></body></html>
"""

private fun VideoItem.shownTitle(english: Boolean) = if (english) translatedTitles[id] ?: title else title

private suspend fun translateTitles(items: List<VideoItem>) {
    for (v in items) {
        if (v.id in translatedTitles) continue
        translatedTitles[v.id] = withContext(Dispatchers.IO) { NewsTranslator.text(v.title) }
    }
}

private fun compactViews(n: Long): String = when {
    n < 1_000 -> "$n"
    n < 100_000 -> String.format("%.1fK", n / 1000.0).replace(".0K", "K")
    n < 10_000_000 -> String.format("%.1fL", n / 100_000.0).replace(".0L", "L")
    else -> String.format("%.1fCr", n / 10_000_000.0).replace(".0Cr", "Cr")
}

private fun VideoItem.isNew(): Boolean = runCatching {
    java.time.Duration.between(java.time.OffsetDateTime.parse(published).toInstant(), java.time.Instant.now()).toHours() < NEW_VIDEO_HOURS
}.getOrDefault(false)

@Composable
private fun videoMeta(v: VideoItem, withBrand: Boolean = false): String {
    val parts = mutableListOf<String>()
    if (withBrand) parts += stringResource(R.string.brand_name)
    v.views?.let { parts += stringResource(R.string.video_views_short, compactViews(it)) }
    if (v.published.isNotBlank()) parts += formatDate(v.published)
    return parts.joinToString(" · ")
}

private fun videoLink(id: String) = "https://youtu.be/$id"

private fun copyLink(context: Context, link: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("link", link))
    Toast.makeText(context, context.getString(R.string.video_link_copied), Toast.LENGTH_SHORT).show()
}

private enum class VideoFilter { ALL, RECENT, SHORTS, POPULAR, CITY }

@Composable
fun VideosScreen(onPlay: (String) -> Unit, onShort: (Int) -> Unit) {
    val context = LocalContext.current
    val english = LocalePrefs.get(context) == "en"
    var recent by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var shorts by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var all by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var nextToken by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    var filter by rememberSaveable { mutableStateOf(VideoFilter.ALL) }
    val list = rememberLazyListState()
    val city = Cities.find(AppPrefs.cityId)

    LaunchedEffect(retry) {
        loading = all.isEmpty()
        error = null
        try {
            withContext(Dispatchers.IO) {
                shorts = runCatching { Videos.shorts() }.getOrDefault(emptyList())
                recent = runCatching { Videos.recent() }.getOrDefault(emptyList()).filter { r -> shorts.none { it.id == r.id } }
                val first = runCatching { Videos.page("") }.getOrElse { if (recent.isEmpty()) throw it else VideoPage(recent, null) }
                all = first.videos
                nextToken = first.nextToken
            }
            loading = false
            if (english) translateTitles(shorts + recent.take(6) + all.take(10))
        } catch (e: Exception) {
            error = e.message ?: "Videos could not be loaded"
            loading = false
        }
    }

    val nearEnd by remember {
        derivedStateOf {
            val last = list.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= list.layoutInfo.totalItemsCount - 4
        }
    }
    LaunchedEffect(nearEnd, nextToken, filter) {
        val token = nextToken ?: return@LaunchedEffect
        if (!nearEnd || loadingMore || all.isEmpty() || filter == VideoFilter.SHORTS || filter == VideoFilter.POPULAR) return@LaunchedEffect
        loadingMore = true
        try {
            val page = withContext(Dispatchers.IO) { Videos.page(token) }
            all = (all + page.videos).distinctBy { it.id }
            nextToken = page.nextToken
            if (english) translateTitles(page.videos)
        } catch (_: Exception) {
            nextToken = null
        } finally {
            loadingMore = false
        }
    }
    LaunchedEffect(english, list.firstVisibleItemIndex) {
        if (english) translateTitles(all.drop((list.firstVisibleItemIndex - 4).coerceAtLeast(0)).take(10))
    }

    val merged = remember(all, recent) { (recent.sortedByDescending { it.published } + all).distinctBy { it.id } }
    val popular = remember(recent) { recent.filter { it.views != null }.sortedByDescending { it.views } }
    val shown = when (filter) {
        VideoFilter.ALL, VideoFilter.RECENT -> merged
        VideoFilter.POPULAR -> popular
        VideoFilter.CITY -> merged.filter { v -> city != null && (v.title.contains(city.hi) || v.title.contains(city.en, ignoreCase = true)) }
        VideoFilter.SHORTS -> emptyList()
    }

    when {
        loading -> NewsSkeleton()
        error != null && all.isEmpty() -> ErrorState(error!!, onRetry = { retry++ })
        else -> LazyColumn(Modifier.fillMaxSize().background(Paper), state = list, contentPadding = PaddingValues(bottom = 24.dp)) {
            item(key = "offline") { OfflineBanner() }
            item(key = "chips") {
                val chips = buildList {
                    add(VideoFilter.ALL to stringResource(R.string.video_all))
                    add(VideoFilter.RECENT to stringResource(R.string.video_recent_chip))
                    if (shorts.isNotEmpty()) add(VideoFilter.SHORTS to stringResource(R.string.video_shorts))
                    if (popular.isNotEmpty()) add(VideoFilter.POPULAR to stringResource(R.string.video_popular_chip))
                    if (city != null) add(VideoFilter.CITY to city.label(english))
                }
                FilterChips(chips, filter) { filter = it }
            }
            if (filter == VideoFilter.SHORTS) {
                items(shorts.chunked(2), key = { row -> "short-row-${row.first().id}" }) { row ->
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { v ->
                            ShortCard(v, english, Modifier.weight(1f).aspectRatio(9f / 16f)) { onShort(shorts.indexOf(v)) }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                return@LazyColumn
            }
            if (shown.isEmpty()) {
                item(key = "empty") { Text(stringResource(R.string.video_empty), color = Muted, modifier = Modifier.padding(24.dp)) }
                return@LazyColumn
            }
            val lead = shown.first()
            item(key = "lead-${lead.id}") { FeaturedVideo(lead, english) { onPlay(lead.id) } }
            if (filter == VideoFilter.ALL && shorts.isNotEmpty()) {
                item(key = "shorts-title") {
                    ShortsHeader(onAll = { filter = VideoFilter.SHORTS })
                }
                item(key = "shorts") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(shorts, key = { _, v -> "short-${v.id}" }) { i, v ->
                            ShortCard(v, english, Modifier.width(132.dp).height(234.dp)) { onShort(i) }
                        }
                    }
                }
            }
            item(key = "latest-title") {
                BarHeader(
                    stringResource(
                        when (filter) {
                            VideoFilter.POPULAR -> R.string.video_popular
                            else -> R.string.video_latest
                        }
                    )
                )
            }
            items(shown.drop(1), key = { "v-${it.id}" }) { v -> VideoCard(v, english) { onPlay(v.id) } }
            if (loadingMore) item(key = "more") { LoadingDots() }
        }
    }
}

@Composable
private fun FilterChips(chips: List<Pair<VideoFilter, String>>, selected: VideoFilter, onPick: (VideoFilter) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(chips, key = { it.first.name }) { (f, label) ->
            val active = f == selected
            Row(
                Modifier
                    .clip(CircleShape)
                    .background(if (active) BrandRed else ChipBg)
                    .clickable { onPick(f) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (f == VideoFilter.SHORTS) {
                    Icon(Icons.Default.SmartDisplay, contentDescription = null, tint = if (active) Color.White else RedInk, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    label,
                    color = if (active) Color.White else Ink,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun PlayCircleBadge(size: Int) {
    Box(
        Modifier.size(size.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = BrandRed, modifier = Modifier.size((size * 0.6f).dp))
    }
}

@Composable
private fun FeaturedVideo(v: VideoItem, english: Boolean, onClick: () -> Unit) {
    val context = LocalContext.current
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .softCard()
            .background(CardWhite)
            .clickable(onClick = onClick)
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black), contentAlignment = Alignment.Center) {
            AsyncImage(model = v.thumbnail, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            PlayCircleBadge(60)
            if (v.isNew()) {
                Row(
                    Modifier.align(Alignment.TopStart).padding(12.dp).clip(CircleShape).background(BrandRed).padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(Color.White))
                    Spacer(Modifier.width(5.dp))
                    Text(stringResource(R.string.video_new), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 8.dp)) {
            Text(
                v.shownTitle(english),
                color = Ink,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                lineHeight = 25.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 8.dp)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.brand_name), color = RedInk, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    videoMeta(v).let { if (it.isBlank()) "" else " · $it" },
                    color = Muted,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Default.Share,
                    contentDescription = stringResource(R.string.share),
                    tint = Muted,
                    modifier = Modifier.size(38.dp).clip(CircleShape)
                        .clickable { shareText(context, v.shownTitle(english), videoLink(v.id), whatsapp = false) }
                        .padding(9.dp)
                )
            }
        }
    }
}

@Composable
private fun ShortsHeader(onAll: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 24.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(4.dp).height(20.dp).clip(RoundedCornerShape(2.dp)).background(BrandRed))
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier.size(24.dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.SmartDisplay, contentDescription = null, tint = RedInk, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(6.dp))
        Text(stringResource(R.string.video_shorts), color = Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
        Text(
            stringResource(R.string.see_all) + " ›",
            color = RedInk,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            modifier = Modifier.clip(CircleShape).clickable(onClick = onAll).padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ShortCard(v: VideoItem, english: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(14.dp)).background(Color.Black).clickable(onClick = onClick)) {
        AsyncImage(model = v.thumbnail, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.85f))))
        Row(
            Modifier.align(Alignment.TopStart).padding(8.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.5f)).padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.PlayCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
            Spacer(Modifier.width(3.dp))
            Text(stringResource(R.string.video_short_badge), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }
        Column(Modifier.align(Alignment.BottomStart).padding(10.dp)) {
            Text(
                v.shownTitle(english),
                color = Color.White,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            v.views?.let {
                Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Visibility, contentDescription = null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.video_views_short, compactViews(it)), color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun VideoCard(v: VideoItem, english: Boolean, playing: Boolean = false, onClick: () -> Unit) {
    val context = LocalContext.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .softCard(RoundedCornerShape(14.dp))
            .background(if (playing) MaterialTheme.colorScheme.secondaryContainer else CardWhite)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (playing) {
            Box(Modifier.width(3.dp).height(72.dp).clip(CircleShape).background(BrandRed))
            Spacer(Modifier.width(8.dp))
        }
        Box(Modifier.width(132.dp).aspectRatio(16f / 9f).clip(Thumb12).background(ChipBg), contentAlignment = Alignment.Center) {
            AsyncImage(model = v.thumbnail, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            if (!playing) PlayCircleBadge(30)
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            if (playing) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PlayCircle, contentDescription = null, tint = RedInk, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.video_playing_now), color = RedInk, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(3.dp))
            }
            Text(
                v.shownTitle(english),
                color = Ink,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            val meta = videoMeta(v)
            if (meta.isNotBlank()) Text(meta, fontSize = 12.sp, color = Muted, modifier = Modifier.padding(top = 4.dp), maxLines = 1)
        }
        if (!playing) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.share),
                tint = Muted,
                modifier = Modifier.size(32.dp).clip(CircleShape)
                    .clickable { shareText(context, v.shownTitle(english), videoLink(v.id), whatsapp = false) }
                    .padding(6.dp)
            )
        }
    }
}

/** YouTube player that keeps one instance and switches videos in place. */
@Composable
private fun InlinePlayer(
    videoId: String,
    modifier: Modifier,
    loop: Boolean = false,
    onError: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val errorCallback by rememberUpdatedState(onError)
    var fullscreen by remember { mutableStateOf<Pair<View, () -> Unit>?>(null) }
    val web = remember {
        WebView(context).apply {
            setBackgroundColor(android.graphics.Color.BLACK)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            addJavascriptInterface(object {
                @JavascriptInterface
                fun error(code: Int) {
                    post { errorCallback() }
                }
            }, "Bridge")
            webChromeClient = object : WebChromeClient() {
                override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                    val activity = context as? Activity ?: return callback.onCustomViewHidden()
                    val decor = activity.window.decorView as ViewGroup
                    view.setBackgroundColor(android.graphics.Color.BLACK)
                    decor.addView(view, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                    WindowCompat.getInsetsController(activity.window, decor).apply {
                        hide(WindowInsetsCompat.Type.systemBars())
                        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    }
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    fullscreen = view to {
                        leaveFullscreen(context, view)
                        fullscreen = null
                        callback.onCustomViewHidden()
                    }
                }

                override fun onHideCustomView() {
                    fullscreen?.first?.let { leaveFullscreen(context, it) }
                    fullscreen = null
                }
            }
        }
    }

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> web.onPause()
                Lifecycle.Event.ON_RESUME -> web.onResume()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            fullscreen?.first?.let { leaveFullscreen(context, it) }
            web.destroy()
            (context as? Activity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
    LaunchedEffect(videoId) {
        web.loadDataWithBaseURL(EMBED_ORIGIN, playerHtml(videoId, loop), "text/html", "utf-8", null)
    }

    AndroidView(factory = { web }, modifier = modifier.background(Color.Black))

    BackHandler(enabled = fullscreen != null) { fullscreen?.second?.invoke() }
}

private fun leaveFullscreen(context: Context, view: View) {
    val activity = context as? Activity ?: return
    (view.parent as? ViewGroup)?.removeView(view)
    WindowCompat.getInsetsController(activity.window, activity.window.decorView).show(WindowInsetsCompat.Type.systemBars())
    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
}

@Composable
private fun ActionTile(icon: ImageVector, label: String, background: Color, tint: Color, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).background(background).clickable(onClick = onClick).padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, color = tint, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun ChannelLogo(size: Int) {
    Image(
        painterResource(R.mipmap.ic_launcher_foreground),
        contentDescription = null,
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(colorResource(R.color.ic_launcher_background))
            .graphicsLayer { scaleX = 1.45f; scaleY = 1.45f }
    )
}

@Composable
fun VideoPlayerScreen(startId: String) {
    val context = LocalContext.current
    val english = LocalePrefs.get(context) == "en"
    var currentId by rememberSaveable { mutableStateOf(startId) }
    var failed by remember(currentId) { mutableStateOf(false) }
    var upNext by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var expanded by remember(currentId) { mutableStateOf(false) }
    val list = rememberLazyListState()
    val video = Videos.find(currentId) ?: VideoItem(currentId, "", "", "https://i.ytimg.com/vi/$currentId/hqdefault.jpg")
    val link = videoLink(currentId)

    LaunchedEffect(Unit) {
        upNext = withContext(Dispatchers.IO) {
            runCatching { (Videos.recent() + Videos.page("").videos).distinctBy { it.id } }.getOrDefault(emptyList())
        }
        if (english) translateTitles(listOf(video) + upNext.take(12))
    }
    LaunchedEffect(currentId) {
        list.scrollToItem(0)
        if (english) translateTitles(listOf(video))
    }

    Column(Modifier.fillMaxSize().background(Paper)) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black), contentAlignment = Alignment.Center) {
            InlinePlayer(currentId, Modifier.fillMaxSize(), onError = { failed = true })
            if (failed) {
                Column(Modifier.fillMaxSize().background(Color.Black).padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.video_cannot_play), color = Color.White)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        stringResource(R.string.video_open_youtube),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(CircleShape).background(BrandRed)
                            .clickable { openUrl(context, "https://www.youtube.com/watch?v=$currentId") }
                            .padding(horizontal = 18.dp, vertical = 9.dp)
                    )
                }
            }
        }
        LazyColumn(Modifier.fillMaxSize(), state = list, contentPadding = PaddingValues(bottom = 24.dp)) {
            item(key = "info") {
                Column(Modifier.fillMaxWidth().background(CardWhite).padding(16.dp)) {
                    Text(video.shownTitle(english), color = Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 26.sp)
                    Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(videoMeta(video), color = Muted, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        if (video.description.isNotBlank()) {
                            Row(
                                Modifier.clip(CircleShape).background(ChipBg).clickable { expanded = !expanded }
                                    .padding(start = 10.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(stringResource(R.string.video_details), color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Icon(
                                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = Ink,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    if (expanded) {
                        Text(
                            video.description,
                            color = Ink.copy(alpha = 0.85f),
                            fontSize = 14.sp,
                            lineHeight = 21.sp,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                    }
                    Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionTile(ImageVector.vectorResource(R.drawable.ic_whatsapp), stringResource(R.string.video_whatsapp), WhatsAppGreen.copy(alpha = 0.14f), Color(0xFF128C7E), Modifier.weight(1f)) {
                            shareText(context, video.shownTitle(english), link, whatsapp = true)
                        }
                        ActionTile(Icons.Default.Share, stringResource(R.string.share), ChipBg, Ink, Modifier.weight(1f)) {
                            shareText(context, video.shownTitle(english), link, whatsapp = false)
                        }
                        ActionTile(Icons.Default.ContentCopy, stringResource(R.string.video_copy_link), ChipBg, Ink, Modifier.weight(1f)) {
                            copyLink(context, link)
                        }
                        ActionTile(Icons.Default.SmartDisplay, stringResource(R.string.video_youtube), MaterialTheme.colorScheme.secondaryContainer, RedInk, Modifier.weight(1f)) {
                            openUrl(context, "https://www.youtube.com/watch?v=$currentId")
                        }
                    }
                }
            }
            item(key = "channel") {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp)
                        .softCard()
                        .background(CardWhite)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ChannelLogo(44)
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.brand_name), color = Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.Verified, contentDescription = null, tint = RedInk, modifier = Modifier.size(15.dp))
                        }
                        Text(stringResource(R.string.video_channel), color = Muted, fontSize = 12.sp)
                    }
                    Row(
                        Modifier.clip(CircleShape).background(Color(0xFFA5000E)).clickable { openUrl(context, CHANNEL_URL) }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(stringResource(R.string.video_subscribe), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
            val next = upNext.filter { it.id != currentId }
            if (next.isNotEmpty()) {
                item(key = "next-title") { BarHeader(stringResource(R.string.video_up_next)) }
                item(key = "playing-$currentId") { VideoCard(video, english, playing = true) {} }
                items(next, key = { "next-${it.id}" }) { v -> VideoCard(v, english) { currentId = v.id } }
            }
        }
    }
}

@Composable
private fun ShortAction(icon: ImageVector, label: String, background: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(48.dp).clip(CircleShape).background(background).clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
fun ShortsScreen(start: Int, onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val english = LocalePrefs.get(context) == "en"
    var shorts by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    LaunchedEffect(Unit) {
        shorts = withContext(Dispatchers.IO) { runCatching { Videos.shorts() }.getOrDefault(emptyList()) }
        if (english) translateTitles(shorts)
    }
    if (shorts.isEmpty()) {
        Box(Modifier.fillMaxSize().background(Color.Black)) { Loading() }
        return
    }
    val pager = rememberPagerState(initialPage = start.coerceIn(0, shorts.lastIndex)) { shorts.size }
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        val bars = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        val wasLight = bars?.isAppearanceLightStatusBars ?: false
        bars?.isAppearanceLightStatusBars = false
        onDispose { bars?.isAppearanceLightStatusBars = wasLight }
    }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        VerticalPager(
            state = pager,
            modifier = Modifier.fillMaxSize(),
            key = { shorts[it].id }
        ) { index ->
            val v = shorts[index]
            val title = v.shownTitle(english)
            val link = "https://youtube.com/shorts/${v.id}"
            Box(Modifier.fillMaxSize()) {
                if (index == pager.settledPage) {
                    InlinePlayer(v.id, Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(top = 56.dp, bottom = 200.dp), loop = true)
                } else {
                    AsyncImage(model = v.thumbnail, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
                Column(
                    Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 12.dp, bottom = 230.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ShortAction(Icons.Default.Share, stringResource(R.string.share), Color.White.copy(alpha = 0.18f)) {
                        shareText(context, title, link, whatsapp = false)
                    }
                    ShortAction(ImageVector.vectorResource(R.drawable.ic_whatsapp), stringResource(R.string.video_whatsapp), WhatsAppGreen) {
                        shareText(context, title, link, whatsapp = true)
                    }
                    ShortAction(Icons.Default.ContentCopy, stringResource(R.string.video_copy_link), Color.White.copy(alpha = 0.18f)) {
                        copyLink(context, link)
                    }
                }
                Column(
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))))
                        .navigationBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ChannelLogo(34)
                        Text(
                            "@MediaSandesh",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(start = 8.dp, end = 10.dp)
                        )
                        Text(
                            stringResource(R.string.video_subscribe),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.clip(CircleShape).background(BrandRed)
                                .clickable { openUrl(context, CHANNEL_URL) }
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                    Text(
                        title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                    val meta = videoMeta(v)
                    if (meta.isNotBlank()) Text(meta, color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    Row(
                        Modifier.padding(top = 10.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.16f))
                            .clickable { openUrl(context, link) }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.SmartDisplay, contentDescription = null, tint = Color(0xFFFF4B4F), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(stringResource(R.string.video_open_youtube), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    if (index == 0 && shorts.size > 1) {
                        val bounce = rememberInfiniteTransition(label = "shortsHint")
                        val y by bounce.animateFloat(0f, -4f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "shortsHintY")
                        Row(
                            Modifier.fillMaxWidth().padding(top = 12.dp).offset(y = y.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.shorts_swipe), color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                        }
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.35f)).clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = Color.White)
            }
            Text(
                stringResource(R.string.video_shorts),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.padding(start = 10.dp).weight(1f)
            )
            Row(
                Modifier.clip(CircleShape).background(Color.Black.copy(alpha = 0.35f)).padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChannelLogo(26)
                Text(stringResource(R.string.brand_name), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}
