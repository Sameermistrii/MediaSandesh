package com.mediasandesh.news.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.text.style.TextAlign
import com.mediasandesh.news.R
import com.mediasandesh.news.data.Library
import com.mediasandesh.news.data.Repository
import com.mediasandesh.news.data.VideoItem
import com.mediasandesh.news.data.WpCategory
import com.mediasandesh.news.data.WpPost
import java.time.Duration
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

fun formatDate(raw: String): String {
    return try {
        val parsed = OffsetDateTime.parse(if (raw.contains("T") && !raw.endsWith("Z") && raw.length == 19) "${raw}Z" else raw)
        val minutes = Duration.between(parsed.toInstant(), java.time.Instant.now()).toMinutes().coerceAtLeast(0)
        val hindi = Locale.getDefault().language == "hi"
        when {
            minutes < 60 -> if (hindi) "$minutes मिनट पहले" else "${minutes}m ago"
            minutes < 60 * 24 -> if (hindi) "${minutes / 60} घंटे पहले" else "${minutes / 60}h ago"
            minutes < 60 * 24 * 7 -> if (hindi) "${minutes / (60 * 24)} दिन पहले" else "${minutes / (60 * 24)}d ago"
            else -> parsed.format(DateTimeFormatter.ofPattern(if (hindi) "d MMMM" else "d MMM", if (hindi) Locale("hi", "IN") else Locale.ENGLISH))
        }
    } catch (_: Exception) {
        raw.take(10)
    }
}

@Composable
fun Loading(message: String? = null) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = BrandRed)
        if (!message.isNullOrBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(message, color = Muted)
        }
    }
}

/** Grey placeholder cards shaped like the news list, shown while stories load. */
@Composable
fun NewsSkeleton(showHero: Boolean = true) {
    val pulse = rememberInfiniteTransition(label = "skeleton")
    val alpha by pulse.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(750), RepeatMode.Reverse),
        label = "skeletonAlpha"
    )
    val block = ChipBg.copy(alpha = alpha)
    Column(Modifier.fillMaxSize().background(Paper).padding(horizontal = 16.dp, vertical = 16.dp)) {
        if (showHero) {
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 10f).clip(RoundedCornerShape(16.dp)).background(block))
            Spacer(Modifier.height(16.dp))
            Box(Modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(16.dp)).background(block))
            Spacer(Modifier.height(24.dp))
        }
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CardWhite)) {
            repeat(5) { i ->
                if (i > 0) HorizontalDivider(color = Hairline, modifier = Modifier.padding(horizontal = 16.dp))
                Row(Modifier.fillMaxWidth().padding(16.dp)) {
                    Column(Modifier.weight(1f)) {
                        Box(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(4.dp)).background(block))
                        Spacer(Modifier.height(8.dp))
                        Box(Modifier.fillMaxWidth(0.75f).height(14.dp).clip(RoundedCornerShape(4.dp)).background(block))
                        Spacer(Modifier.height(10.dp))
                        Box(Modifier.width(80.dp).height(10.dp).clip(RoundedCornerShape(4.dp)).background(block))
                    }
                    Spacer(Modifier.width(12.dp))
                    Box(Modifier.width(96.dp).height(80.dp).clip(RoundedCornerShape(12.dp)).background(block))
                }
            }
        }
    }
}

private val TickerInk = Color(0xFF1C1C1C)

@Composable
fun ErrorState(message: String, onRetry: () -> Unit, onOpenLibrary: (() -> Unit)? = null) {
    val offline = !Repository.isOnline()
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(if (offline) R.string.offline_title else R.string.load_failed),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (offline) stringResource(R.string.offline_body) else message,
            color = Muted,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.retry),
            color = BrandRed,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clip(RoundedCornerShape(20.dp)).clickable(onClick = onRetry).padding(horizontal = 16.dp, vertical = 8.dp)
        )
        if (onOpenLibrary != null && (Library.saved.isNotEmpty() || Library.history.isNotEmpty())) {
            Text(
                stringResource(R.string.offline_open_library),
                color = BrandRed,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clip(RoundedCornerShape(20.dp)).clickable(onClick = onOpenLibrary).padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
fun OfflineBanner() {
    if (!Repository.offline) return
    Text(
        stringResource(R.string.offline_banner),
        color = Color.White,
        fontSize = 13.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().background(Color(0xFF444444)).padding(vertical = 6.dp, horizontal = 12.dp)
    )
}

@Composable
internal fun NewsImage(url: String?, description: String?, modifier: Modifier, widthPx: Int, heightPx: Int) {
    val context = LocalContext.current
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(url)
            .size(widthPx, heightPx)
            .crossfade(false)
            .allowHardware(true)
            .build(),
        contentDescription = description,
        modifier = modifier,
        contentScale = ContentScale.Crop
    )
}

@Composable
fun HeroStory(post: WpPost, label: String = stringResource(R.string.hero_breaking), onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(260.dp).clickable(onClick = onClick)) {
        NewsImage(
            url = post.imageUrl,
            description = post.headline,
            modifier = Modifier.fillMaxSize().background(Color(0xFF222222)),
            widthPx = 900,
            heightPx = 600
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.82f))
                )
            )
        )
        Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            Text(
                label,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.background(BrandRed).padding(horizontal = 8.dp, vertical = 3.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                post.headline,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 28.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            Text("${stringResource(R.string.brand_name)}  ·  ${formatDate(post.date)}", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
        }
    }
}

@Composable
fun PostCard(post: WpPost, onClick: () -> Unit, large: Boolean = false) {
    if (large) {
        HeroStory(post, onClick = onClick)
    } else {
        CompactPost(post, onClick)
    }
}

@Composable
fun CompactPost(post: WpPost, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).background(CardWhite)) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    post.headline,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 21.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Text("${stringResource(R.string.brand_name)}  ·  ${formatDate(post.date)}", style = MaterialTheme.typography.labelMedium)
            }
            NewsImage(
                url = post.imageUrl,
                description = null,
                modifier = Modifier.size(width = 104.dp, height = 76.dp).clip(RoundedCornerShape(6.dp)).background(ChipBg),
                widthPx = 320,
                heightPx = 240
            )
        }
        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(1.dp).background(Hairline))
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = BrandRed,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
    )
}

@Composable
fun CategoryChip(category: WpCategory, onClick: () -> Unit) {
    Column(
        Modifier.padding(6.dp).clip(RoundedCornerShape(10.dp)).background(CardWhite).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(category.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
        Text(stringResource(R.string.story_count, category.count), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun VideoCard(video: VideoItem, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).background(CardWhite)) {
        Box {
            NewsImage(
                url = video.thumbnail,
                description = video.title,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                widthPx = 800,
                heightPx = 450
            )
            Box(
                Modifier.align(Alignment.Center).size(52.dp).clip(RoundedCornerShape(26.dp)).background(BrandRed),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
            }
        }
        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text(video.title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(video.published, style = MaterialTheme.typography.labelMedium)
        }
    }
}

fun shareText(context: android.content.Context, title: String, link: String, whatsapp: Boolean) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "$title\n$link")
        if (whatsapp) setPackage("com.whatsapp")
    }
    try {
        val label = context.getString(R.string.share_via)
        context.startActivity(if (whatsapp) send else Intent.createChooser(send, label))
    } catch (_: Exception) {
        context.startActivity(Intent.createChooser(send.apply { setPackage(null) }, context.getString(R.string.share_via)))
    }
}

fun openUrl(context: android.content.Context, url: String) {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}

@Composable
fun Ticker(posts: List<WpPost>, onClick: (WpPost) -> Unit) {
    if (posts.isEmpty()) return
    LazyRow(
        modifier = Modifier.fillMaxWidth().background(Accent),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
    ) {
        item { Text(stringResource(R.string.ticker) + "  ", color = TickerInk, modifier = Modifier.padding(end = 8.dp, top = 2.dp)) }
        items(posts.take(8)) { post ->
            Text(
                post.headline,
                maxLines = 1,
                color = TickerInk,
                modifier = Modifier.clickable { onClick(post) }.padding(end = 18.dp)
            )
        }
    }
}
