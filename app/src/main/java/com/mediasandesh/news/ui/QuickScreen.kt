package com.mediasandesh.news.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediasandesh.news.LocalePrefs
import com.mediasandesh.news.R
import com.mediasandesh.news.Speaker
import com.mediasandesh.news.data.Library
import com.mediasandesh.news.data.NewsTranslator
import com.mediasandesh.news.data.Repository
import com.mediasandesh.news.data.WpPost
import com.mediasandesh.news.data.decodeHtml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val SUMMARY_WORDS = 70
private const val SEGMENTS = 10
private val StoryShape = RoundedCornerShape(24.dp)
private val WhatsApp = Color(0xFF25D366)

fun WpPost.shortSummary(): String {
    val source = summary.ifBlank { content.rendered.decodeHtml() }
        .replace(Regex("\\[…\\]|\\[&hellip;\\]|\\[\\.\\.\\.\\]"), "")
        .replace(Regex("(?i)(read more|और पढ़ें).*$"), "")
        .trim()
    val words = source.split(Regex("\\s+")).filter { it.isNotBlank() }
    return if (words.size <= SUMMARY_WORDS) words.joinToString(" ") else words.take(SUMMARY_WORDS).joinToString(" ") + "…"
}

private data class Quick(val headline: String, val summary: String)

@Composable
fun QuickScreen(onOpen: (Int) -> Unit, onHome: () -> Unit = {}) {
    val context = LocalContext.current
    val english = LocalePrefs.get(context) == "en"
    val scope = rememberCoroutineScope()
    var posts by remember { mutableStateOf<List<WpPost>>(emptyList()) }
    var texts by remember { mutableStateOf<Map<Int, Quick>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    var page by remember { mutableIntStateOf(1) }
    var hasMore by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    val categoryNames = rememberCategoryNames(english)
    val pageCount = posts.size + if (!hasMore && posts.isNotEmpty()) 1 else 0
    val pager = rememberPagerState { pageCount }

    DisposableEffect(Unit) { onDispose { Speaker.stop() } }

    fun original(p: WpPost) = Quick(p.headline, p.shortSummary())
    fun textOf(p: WpPost) = texts[p.id] ?: original(p)

    suspend fun translate(items: List<WpPost>) {
        if (!english) return
        for (p in items) {
            if (p.id in texts) continue
            val base = original(p)
            val done = withContext(Dispatchers.IO) {
                Quick(NewsTranslator.text(base.headline), NewsTranslator.text(base.summary))
            }
            texts = texts + (p.id to done)
        }
    }

    LaunchedEffect(retry) {
        loading = posts.isEmpty()
        error = null
        try {
            val loaded = withContext(Dispatchers.IO) { Repository.latest() }
            posts = loaded
            page = 1
            hasMore = loaded.size >= 10
            loading = false
        } catch (e: Exception) {
            error = e.message ?: "Network error"
            loading = false
        }
    }

    LaunchedEffect(pager.currentPage, posts.size) {
        if (posts.isEmpty()) return@LaunchedEffect
        val current = pager.currentPage
        translate(posts.drop(current).take(3))
        if (current >= posts.size - 3 && hasMore && !loadingMore) {
            loadingMore = true
            try {
                val next = withContext(Dispatchers.IO) { Repository.latest(page + 1) }
                page += 1
                hasMore = next.size >= 10
                posts = (posts + next).distinctBy { it.id }
            } catch (_: Exception) {
                hasMore = false
            }
            loadingMore = false
        }
    }

    when {
        loading -> Loading()
        error != null && posts.isEmpty() -> ErrorState(error!!, onRetry = { retry++ })
        else -> Column(Modifier.fillMaxSize().background(Paper)) {
            OfflineBanner()
            val current = pager.currentPage.coerceAtMost(posts.lastIndex)
            ProgressRow(
                current = current,
                total = posts.size,
                top5Playing = Speaker.playing == "top5",
                onTop5 = {
                    if (Speaker.playing == "top5") {
                        Speaker.stop()
                    } else scope.launch {
                        val top = posts.take(5)
                        translate(top)
                        val parts = buildList {
                            add(context.getString(R.string.listen_top5_intro))
                            top.forEachIndexed { i, p ->
                                val q = textOf(p)
                                add("${i + 1}. ${q.headline}. ${q.summary}")
                            }
                        }
                        Speaker.speak(context, "top5", parts)
                    }
                }
            )
            VerticalPager(
                state = pager,
                modifier = Modifier.fillMaxSize(),
                key = { if (it < posts.size) posts[it].id else -1 },
                beyondViewportPageCount = 1
            ) { index ->
                if (index >= posts.size) {
                    CaughtUp(posts.size, onHome = onHome, onMore = { scope.launch { pager.animateScrollToPage(0) } })
                } else {
                    val post = posts[index]
                    val next = posts.getOrNull(index + 1)
                    QuickStory(
                        post = post,
                        text = textOf(post),
                        place = post.place()?.label(english),
                        category = post.categoryTag(categoryNames),
                        first = index == 0,
                        next = next?.let { textOf(it).headline },
                        onOpen = onOpen
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressRow(current: Int, total: Int, top5Playing: Boolean, onTop5: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("${current + 1}", color = Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text(" / $total", color = Muted, fontWeight = FontWeight.Medium, fontSize = 13.sp)
        Spacer(Modifier.width(10.dp))
        val filled = current % SEGMENTS + 1
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(SEGMENTS) { i ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(if (i < filled) BrandRed else Hairline)
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Row(
            Modifier
                .clip(CircleShape)
                .background(if (top5Playing) BrandRed else RedInk.copy(alpha = 0.1f))
                .clickable(onClick = onTop5)
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (top5Playing) Icons.Default.Stop else Icons.Default.GraphicEq,
                contentDescription = null,
                tint = if (top5Playing) Color.White else RedInk,
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                stringResource(if (top5Playing) R.string.listen_stop else R.string.listen_top5),
                color = if (top5Playing) Color.White else RedInk,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun RoundAction(icon: ImageVector, description: String, background: Color, tint: Color, onClick: () -> Unit) {
    Box(
        Modifier.size(38.dp).clip(CircleShape).background(background).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(19.dp))
    }
}

@Composable
private fun QuickStory(
    post: WpPost,
    text: Quick,
    place: String?,
    category: String?,
    first: Boolean,
    next: String?,
    onOpen: (Int) -> Unit
) {
    val context = LocalContext.current
    val id = "quick-${post.id}"
    val playing = Speaker.playing == id
    val saved = Library.isSaved(post.id)
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .shadow(4.dp, StoryShape, clip = false, ambientColor = Color(0x1A000000), spotColor = Color(0x1A000000))
                .clip(StoryShape)
                .background(CardWhite)
        ) {
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 10f).background(ChipBg)) {
                NewsImage(post.imageUrl, null, Modifier.fillMaxSize(), widthPx = 1000, heightPx = 625)
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(0.45f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.7f))
                    )
                )
                Row(
                    Modifier.align(Alignment.BottomStart).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        Modifier.clip(CircleShape).background(BrandRed).padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(stringResource(R.string.tab_quick), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    if (place != null) {
                        Spacer(Modifier.width(6.dp))
                        Row(
                            Modifier.clip(CircleShape).background(Color.Black.copy(alpha = 0.55f)).padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFFFB4AB), modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(3.dp))
                            Text(place, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 11.sp)
                        }
                    }
                }
            }
            Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.brand_name), color = RedInk, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Text("  •  " + formatDate(post.date), color = Muted, fontSize = 12.sp)
                    if (category != null) {
                        Text("  •  ", color = Muted, fontSize = 12.sp)
                        Text(
                            category,
                            color = Ink,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(ChipBg).padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text.headline,
                    color = Ink,
                    fontSize = 19.sp,
                    lineHeight = 27.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    Text(
                        text.summary,
                        fontSize = 15.sp,
                        lineHeight = 24.sp,
                        color = Muted,
                        overflow = TextOverflow.Ellipsis
                    )
                    Box(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(28.dp)
                            .background(Brush.verticalGradient(listOf(Color.Transparent, CardWhite)))
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        Modifier
                            .clip(CircleShape)
                            .background(BrandRed)
                            .clickable { onOpen(post.id) }
                            .padding(horizontal = 16.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.quick_read_full), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        RoundAction(
                            if (playing) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                            stringResource(if (playing) R.string.listen_stop else R.string.listen),
                            background = if (playing) BrandRed else ChipBg,
                            tint = if (playing) Color.White else Ink
                        ) { Speaker.toggle(context, id) { listOf(text.headline, text.summary) } }
                        RoundAction(ImageVector.vectorResource(R.drawable.ic_whatsapp), stringResource(R.string.share), WhatsApp, Color.White) {
                            shareText(context, text.headline, post.link, whatsapp = true)
                        }
                        RoundAction(Icons.Default.Share, stringResource(R.string.share), ChipBg, Ink) {
                            shareText(context, text.headline, post.link, whatsapp = false)
                        }
                        RoundAction(
                            if (saved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            stringResource(if (saved) R.string.unsave else R.string.save),
                            background = ChipBg,
                            tint = if (saved) RedInk else Ink
                        ) { Library.toggleSaved(post) }
                    }
                }
            }
        }
        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 10.dp), contentAlignment = Alignment.TopCenter) {
            if (next != null) {
                Row(
                    Modifier
                        .padding(top = 14.dp)
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(CardWhite.copy(alpha = 0.75f))
                        .padding(start = 16.dp, end = 12.dp, top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(BrandRed))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        next,
                        color = Ink,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Default.ExpandLess, contentDescription = null, tint = Muted, modifier = Modifier.size(18.dp))
                }
            } else {
                Spacer(Modifier.height(20.dp))
            }
            if (first && next != null) SwipeHint()
        }
    }
}

@Composable
private fun SwipeHint() {
    val bounce = rememberInfiniteTransition(label = "swipeHint")
    val y by bounce.animateFloat(0f, -5f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "hintY")
    Row(
        Modifier
            .offset(y = y.dp)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(CardWhite)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = RedInk, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(stringResource(R.string.quick_swipe), color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
    }
}

@Composable
private fun CaughtUp(count: Int, onHome: () -> Unit, onMore: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
            .clip(StoryShape)
            .background(CardWhite)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(72.dp))
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.quick_done_title), color = Ink, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.quick_done_body, count), color = Muted, fontSize = 14.sp)
        Spacer(Modifier.height(24.dp))
        Text(
            stringResource(R.string.quick_done_home),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clip(CircleShape).background(BrandRed).clickable(onClick = onHome).padding(horizontal = 24.dp, vertical = 11.dp)
        )
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(R.string.quick_done_again),
            color = RedInk,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .clickable(onClick = onMore)
                .padding(horizontal = 24.dp, vertical = 11.dp)
        )
    }
}
