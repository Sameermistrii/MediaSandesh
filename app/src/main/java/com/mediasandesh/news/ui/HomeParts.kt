package com.mediasandesh.news.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.mutableStateOf
import com.mediasandesh.news.R
import com.mediasandesh.news.data.Cities
import com.mediasandesh.news.data.City
import com.mediasandesh.news.data.NewsTranslator
import com.mediasandesh.news.data.Repository
import com.mediasandesh.news.data.WpPost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Broad categories that say little about a story, used only when nothing more specific exists. */
private val genericCategories = setOf(1, 79, Cities.UDHAM_SINGH_NAGAR, Cities.UTTARAKHAND)

/** The town a story is about: one named in the headline, then the summary, wins over other tagged towns. */
fun WpPost.city(): City? = categories.mapNotNull { Cities.find(it) }.let { found ->
    fun named(text: String) = found.firstOrNull { text.contains(it.hi) || text.contains(it.en, ignoreCase = true) }
    named(headline) ?: named(summary.take(300)) ?: found.firstOrNull()
}

fun WpPost.place(): City? = city()
    ?: Cities.district.takeIf { it.id in categories }
    ?: Cities.state.takeIf { it.id in categories }

fun WpPost.categoryTag(names: Map<Int, String>): String? = categories
    .firstOrNull { it !in genericCategories && Cities.find(it) == null && names[it] != null }
    ?.let { names[it] }

@Composable
fun rememberCategoryNames(english: Boolean): Map<Int, String> {
    var names by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    LaunchedEffect(english) {
        val cats = withContext(Dispatchers.IO) { runCatching { Repository.categories() }.getOrDefault(emptyList()) }
        names = cats.associate { it.id to if (english) NewsTranslator.categoryKnown(it.name) else it.name }
    }
    return names
}

private val CardShape = RoundedCornerShape(16.dp)
private val Pill = RoundedCornerShape(50)
private val DeepRed = Color(0xFF93000A)
private val Amber = Color(0xFFFFC53D)
private val WhatsAppGreen = Color(0xFF047857)

private fun Modifier.card() = this
    .padding(horizontal = 16.dp)
    .shadow(2.dp, CardShape, clip = false, ambientColor = Color(0x14000000), spotColor = Color(0x14000000))
    .clip(CardShape)

/** A chip row whose first entry is a filled red "trending" pill. */
@Composable
fun HomeTopicRow(label: String?, topics: List<Pair<Int, String>>, state: androidx.compose.foundation.lazy.LazyListState, onTopic: (Int, String) -> Unit) {
    LazyRow(
        state = state,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (label != null) {
            item(key = "trend-label") {
                Text(
                    "🔥 $label",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clip(Pill)
                        .background(BrandRed)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
        items(topics, key = { "topic-${it.first}" }) { (id, name) ->
            Text(
                name,
                color = Ink,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                modifier = Modifier
                    .clip(Pill)
                    .background(ChipBg)
                    .clickable { onTopic(id, name) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

/** One headline at a time, rotating every few seconds behind a pulsing "latest" pill. */
@Composable
fun HomeTicker(posts: List<WpPost>, onClick: (WpPost) -> Unit) {
    val items = posts.take(8)
    if (items.isEmpty()) return
    var index by remember(items.size) { mutableIntStateOf(0) }
    LaunchedEffect(items.size) {
        while (true) {
            delay(4000)
            index = (index + 1) % items.size
        }
    }
    val pulse = rememberInfiniteTransition(label = "tickerDot")
    val dot by pulse.animateFloat(0.3f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "dotAlpha")
    Row(
        Modifier
            .fillMaxWidth()
            .card()
            .background(CardWhite)
            .clickable { onClick(items[index % items.size]) }
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            Modifier.clip(Pill).background(BrandRed).padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(6.dp).alpha(dot).clip(CircleShape).background(Color.White))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.ticker), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        AnimatedContent(
            targetState = index % items.size,
            transitionSpec = {
                (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
            },
            label = "tickerHeadline",
            modifier = Modifier.weight(1f).padding(horizontal = 10.dp)
        ) { i ->
            Text(
                items[i].headline,
                color = Ink,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(Icons.Default.Campaign, contentDescription = null, tint = RedInk, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun HomeHero(post: WpPost, place: String?, onClick: () -> Unit) {
    val context = LocalContext.current
    Box(
        Modifier
            .fillMaxWidth()
            .card()
            .aspectRatio(16f / 10f)
            .clickable(onClick = onClick)
    ) {
        NewsImage(
            url = post.imageUrl,
            description = post.headline,
            modifier = Modifier.fillMaxSize().background(Color(0xFF222222)),
            widthPx = 1000,
            heightPx = 625
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(0.3f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.88f)))
        )
        Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.hero_breaking),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(BrandRed).padding(horizontal = 8.dp, vertical = 3.dp)
                )
                if (place != null) {
                    Spacer(Modifier.width(10.dp))
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Text(place, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                post.headline,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                lineHeight = 26.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.brand_name) + " · " + formatDate(post.date),
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .clickable { shareText(context, post.headline, post.link, whatsapp = false) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Share, contentDescription = stringResource(R.string.share), tint = Color.White, modifier = Modifier.size(17.dp))
                }
            }
        }
    }
}

@Composable
fun QuickCard(onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .card()
            .background(Brush.horizontalGradient(listOf(BrandRed, DeepRed)))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Bolt, contentDescription = null, tint = Amber, modifier = Modifier.size(28.dp))
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.tab_quick), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.quick_badge),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(Pill).background(Color.White.copy(alpha = 0.22f)).padding(horizontal = 7.dp, vertical = 2.dp)
                )
            }
            Text(
                stringResource(R.string.quick_subtitle),
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(
            Modifier.clip(Pill).background(Color.White).padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.quick_start), color = BrandRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(Modifier.width(4.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = BrandRed, modifier = Modifier.size(15.dp))
        }
    }
}

/** A bold title led by a short red bar, with an optional action on the right. */
@Composable
fun BarHeader(title: String, action: String? = null, onAction: () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 24.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(4.dp).height(20.dp).clip(RoundedCornerShape(2.dp)).background(BrandRed))
        Spacer(Modifier.width(8.dp))
        Text(title, color = Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
        if (action != null) {
            Row(
                Modifier.clip(Pill).clickable(onClick = onAction).padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(action, color = RedInk, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = RedInk, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** Several stories grouped in one rounded card, separated by hairlines. */
@Composable
fun LatestGroup(posts: List<WpPost>, tagFor: (WpPost) -> String?, onOpen: (WpPost) -> Unit) {
    val brand = stringResource(R.string.brand_name)
    Column(Modifier.fillMaxWidth().card().background(CardWhite)) {
        posts.forEachIndexed { i, post ->
            if (i > 0) HorizontalDivider(color = Hairline, modifier = Modifier.padding(horizontal = 16.dp))
            Row(
                Modifier.fillMaxWidth().clickable { onOpen(post) }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        post.headline,
                        color = Ink,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(6.dp))
                    val tag = tagFor(post)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            tag ?: brand,
                            color = if (tag != null) BrandRed else Muted,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                        Text(" · " + formatDate(post.date), color = Muted, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.width(12.dp))
                NewsImage(
                    url = post.imageUrl,
                    description = null,
                    modifier = Modifier.width(96.dp).height(80.dp).clip(RoundedCornerShape(12.dp)).background(ChipBg),
                    widthPx = 288,
                    heightPx = 240
                )
            }
        }
    }
}

@Composable
fun CityPill(city: String, onChange: () -> Unit) {
    Row(
        Modifier
            .padding(start = 16.dp, bottom = 12.dp)
            .clip(Pill)
            .background(ChipBg)
            .clickable(onClick = onChange)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.LocationOn, contentDescription = null, tint = RedInk, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(stringResource(R.string.your_city, city), color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.width(6.dp))
        Text(stringResource(R.string.city_change), color = RedInk, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

/** Horizontally scrolling text cards for the reader's own city. */
@Composable
fun CityCarousel(posts: List<WpPost>, tagFor: (WpPost) -> String?, onOpen: (WpPost) -> Unit) {
    val context = LocalContext.current
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(posts, key = { "city-${it.id}" }) { post ->
            Column(
                Modifier
                    .width(256.dp)
                    .padding(vertical = 4.dp)
                    .shadow(2.dp, CardShape, clip = false, ambientColor = Color(0x14000000), spotColor = Color(0x14000000))
                    .clip(CardShape)
                    .background(CardWhite)
                    .clickable { onOpen(post) }
                    .padding(14.dp)
            ) {
                tagFor(post)?.let {
                    Text(
                        it,
                        color = RedInk,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(MaterialThemeRedTint()).padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                }
                Text(
                    post.headline,
                    color = Ink,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.height(66.dp)
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = Muted, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(formatDate(post.date), color = Muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Icon(
                        Icons.Default.Share,
                        contentDescription = stringResource(R.string.share),
                        tint = Muted,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable { shareText(context, post.headline, post.link, whatsapp = false) }
                            .padding(6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MaterialThemeRedTint(): Color = androidx.compose.material3.MaterialTheme.colorScheme.secondaryContainer

@Composable
fun SubmitCard(onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
            .card()
            .background(CardWhite)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(WhatsAppGreen),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(stringResource(R.string.submit_promo_title), color = Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(stringResource(R.string.submit_promo_body), color = Muted, fontSize = 12.sp, lineHeight = 17.sp)
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(WhatsAppGreen)
                .clickable(onClick = onClick)
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(painterResource(R.drawable.ic_whatsapp), contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.submit_whatsapp), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

/** A standalone story card: category label, headline, time, square thumbnail. */
@Composable
fun MoreCard(post: WpPost, tag: String?, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .card()
            .background(CardWhite)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            if (tag != null) {
                Text(tag, color = RedInk, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(Modifier.height(4.dp))
            }
            Text(
                post.headline,
                color = Ink,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = Muted, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
                Text(formatDate(post.date), color = Muted, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.width(12.dp))
        NewsImage(
            url = post.imageUrl,
            description = null,
            modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp)).background(ChipBg),
            widthPx = 240,
            heightPx = 240
        )
    }
}

@Composable
fun LoadingDots() {
    val bounce = rememberInfiniteTransition(label = "dots")
    Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(3) { i ->
                val y by bounce.animateFloat(
                    initialValue = 0f,
                    targetValue = -6f,
                    animationSpec = infiniteRepeatable(
                        tween(400, delayMillis = i * 130),
                        RepeatMode.Reverse
                    ),
                    label = "dot$i"
                )
                Box(Modifier.offset(y = y.dp).size(8.dp).clip(CircleShape).background(BrandRed))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.loading_more), color = Muted, fontSize = 12.sp)
    }
}
