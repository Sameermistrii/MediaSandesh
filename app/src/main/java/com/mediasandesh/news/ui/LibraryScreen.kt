package com.mediasandesh.news.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediasandesh.news.LocalePrefs
import com.mediasandesh.news.R
import com.mediasandesh.news.data.Library
import com.mediasandesh.news.data.NewsTranslator
import com.mediasandesh.news.data.Rendered
import com.mediasandesh.news.data.WpPost
import com.mediasandesh.news.data.decodeHtml
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val Card16 = RoundedCornerShape(16.dp)
private val SoftShadow = Color(0x14000000)
private const val WORDS_PER_MINUTE = 200

private fun Modifier.softCard(shape: RoundedCornerShape = Card16) = this
    .shadow(2.dp, shape, clip = false, ambientColor = SoftShadow, spotColor = SoftShadow)
    .clip(shape)

private enum class ReadGroup { TODAY, YESTERDAY, WEEK, EARLIER }

@Composable
fun LibraryScreen(onOpen: (Int) -> Unit, onHome: () -> Unit = {}) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val english = LocalePrefs.get(LocalContext.current) == "en"
    val categoryNames = rememberCategoryNames(english)
    var titles by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    LaunchedEffect(english, Library.saved, Library.history) {
        if (!english) return@LaunchedEffect
        val pending = (Library.saved + Library.history).distinctBy { it.id }.filter { it.id !in titles }
        if (pending.isEmpty()) return@LaunchedEffect
        val translated = withContext(Dispatchers.IO) { NewsTranslator.postsTitles(pending) }
        titles = titles + translated.associate { it.id to it.title.rendered }
    }
    fun List<WpPost>.shown() = distinctBy { it.id }.map { p ->
        titles[p.id]?.let { p.copy(title = Rendered(it)) } ?: p
    }
    val saved = Library.saved.shown()
    val history = Library.history.shown()

    LazyColumn(
        Modifier.fillMaxSize().background(Paper),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item(key = "header") {
            Column(Modifier.padding(horizontal = 16.dp).padding(top = 8.dp, bottom = 12.dp)) {
                Text(stringResource(R.string.library_title), color = Ink, fontWeight = FontWeight.Bold, fontSize = 26.sp)
                Text(stringResource(R.string.library_subtitle), color = Muted, fontSize = 13.sp)
            }
        }
        item(key = "stats") {
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(Icons.Default.Bookmark, saved.size, stringResource(R.string.library_stat_saved), highlight = true, Modifier.weight(1f)) { tab = 0 }
                StatCard(Icons.Default.History, history.size, stringResource(R.string.library_stat_read), highlight = false, Modifier.weight(1f)) { tab = 1 }
            }
        }
        item(key = "toggle") {
            SegmentedToggle(tab, saved.size) { tab = it }
        }
        if (tab == 0) savedList(saved, categoryNames, english, onOpen, onHome)
        else historyList(history, categoryNames, english, onOpen, onHome)
    }
}

@Composable
private fun StatCard(icon: ImageVector, count: Int, label: String, highlight: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.softCard().background(CardWhite).clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (highlight) BrandRed.copy(alpha = 0.12f) else ChipBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = if (highlight) RedInk else Muted)
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(count.toString(), color = Ink, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 24.sp)
            Text(label, color = Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun SegmentedToggle(tab: Int, savedCount: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .fillMaxWidth()
            .clip(CircleShape)
            .background(ChipBg)
            .padding(4.dp)
    ) {
        listOf(0, 1).forEach { index ->
            val selected = tab == index
            Row(
                Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .background(if (selected) BrandRed else Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(vertical = 9.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(if (index == 0) R.string.library_stat_saved else R.string.library_history),
                    color = if (selected) Color.White else Muted,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                if (index == 0 && savedCount > 0) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        savedCount.toString(),
                        color = if (selected) Color.White else Muted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (selected) Color.White.copy(alpha = 0.22f) else CardWhite)
                            .padding(horizontal = 7.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}

private fun LazyListScope.savedList(
    posts: List<WpPost>,
    categoryNames: Map<Int, String>,
    english: Boolean,
    onOpen: (Int) -> Unit,
    onHome: () -> Unit
) {
    if (posts.isEmpty()) {
        item(key = "saved-empty") {
            EmptyState(R.string.library_empty_title, R.string.library_saved_empty, showTip = true, onHome = onHome)
        }
        return
    }
    item(key = "offline") {
        Row(
            Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 12.dp)
                .fillMaxWidth()
                .clip(CircleShape)
                .background(BrandRed.copy(alpha = 0.09f))
                .padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.OfflinePin, contentDescription = null, tint = RedInk, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.library_offline), color = Ink, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
    items(posts, key = { "saved-${it.id}" }) { post ->
        LibraryCard(
            post = post,
            category = post.categoryTag(categoryNames),
            place = post.place()?.label(english),
            meta = {
                val minutes = post.readMinutes()
                Text(
                    formatDate(post.date) + if (minutes > 0) " · " + stringResource(R.string.article_read_time, minutes) else "",
                    color = Muted,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Default.Bookmark,
                    contentDescription = stringResource(R.string.unsave),
                    tint = RedInk,
                    modifier = Modifier.size(34.dp).clip(CircleShape).clickable { Library.toggleSaved(post) }.padding(7.dp)
                )
            },
            onClick = { onOpen(post.id) }
        )
    }
}

private fun LazyListScope.historyList(
    posts: List<WpPost>,
    categoryNames: Map<Int, String>,
    english: Boolean,
    onOpen: (Int) -> Unit,
    onHome: () -> Unit
) {
    if (posts.isEmpty()) {
        item(key = "history-empty") {
            EmptyState(R.string.library_history_empty_title, R.string.library_history_empty, showTip = false, onHome = onHome)
        }
        return
    }
    item(key = "history-bar") {
        Row(
            Modifier.padding(start = 16.dp, end = 8.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(BrandRed))
            Spacer(Modifier.width(6.dp))
            Text(pluralStringResource(R.plurals.library_read_total, posts.size, posts.size), color = Muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Row(
                Modifier.clip(RoundedCornerShape(8.dp)).clickable { Library.clearHistory() }.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = RedInk, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.library_clear_history), color = RedInk, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
    val today = LocalDate.now()
    val zone = ZoneId.systemDefault()
    fun groupOf(post: WpPost): ReadGroup {
        val at = Library.readAt[post.id] ?: return ReadGroup.EARLIER
        val day = Instant.ofEpochMilli(at).atZone(zone).toLocalDate()
        return when {
            day == today -> ReadGroup.TODAY
            day == today.minusDays(1) -> ReadGroup.YESTERDAY
            day.isAfter(today.minusDays(7)) -> ReadGroup.WEEK
            else -> ReadGroup.EARLIER
        }
    }
    val locale = if (english) Locale.ENGLISH else Locale("hi", "IN")
    val clock = DateTimeFormatter.ofPattern("h:mm a", locale)
    val dayFormat = DateTimeFormatter.ofPattern("d MMM, h:mm a", locale)
    posts.groupBy(::groupOf).toSortedMap().forEach { (group, items) ->
        item(key = "group-$group") {
            Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(5.dp).height(16.dp).clip(CircleShape).background(BrandRed))
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(
                        when (group) {
                            ReadGroup.TODAY -> R.string.library_today
                            ReadGroup.YESTERDAY -> R.string.library_yesterday
                            ReadGroup.WEEK -> R.string.library_week
                            ReadGroup.EARLIER -> R.string.library_earlier
                        }
                    ),
                    color = Ink,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(Modifier.width(8.dp))
                Text(pluralStringResource(R.plurals.library_group_count, items.size, items.size), color = Muted, fontSize = 11.sp)
            }
        }
        items(items, key = { "history-${it.id}" }) { post ->
            val at = Library.readAt[post.id]
            LibraryCard(
                post = post,
                category = post.categoryTag(categoryNames),
                place = post.place()?.label(english),
                meta = {
                    Icon(Icons.Default.DoneAll, contentDescription = null, tint = RedInk, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        at?.let {
                            val time = Instant.ofEpochMilli(it).atZone(zone)
                            (if (group == ReadGroup.TODAY || group == ReadGroup.YESTERDAY) clock else dayFormat).format(time)
                        } ?: formatDate(post.date),
                        color = Muted,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Muted, modifier = Modifier.size(20.dp))
                },
                onClick = { onOpen(post.id) }
            )
        }
    }
    item(key = "history-note") {
        Column(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(32.dp).clip(CircleShape).background(ChipBg), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.History, contentDescription = null, tint = Muted, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.library_history_note, Library.MAX_HISTORY), color = Muted, fontSize = 12.sp)
        }
    }
}

private fun WpPost.readMinutes(): Int {
    val words = content.rendered.decodeHtml().split(Regex("\\s+")).count { it.isNotBlank() }
    return if (words == 0) 0 else (words / WORDS_PER_MINUTE).coerceAtLeast(1)
}

@Composable
private fun LibraryCard(
    post: WpPost,
    category: String?,
    place: String?,
    meta: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .fillMaxWidth()
            .softCard()
            .background(CardWhite)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Column(Modifier.weight(1f).heightIn(min = 88.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (category != null) {
                        Text(
                            category,
                            color = RedInk,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .clip(RoundedCornerShape(4.dp))
                                .background(BrandRed.copy(alpha = 0.1f))
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    if (place != null) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Muted, modifier = Modifier.size(12.dp))
                        Text(place, color = Muted, fontSize = 11.sp, maxLines = 1)
                    }
                }
                Spacer(Modifier.height(5.dp))
                Text(
                    post.headline,
                    color = Ink,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, content = meta)
        }
        Spacer(Modifier.width(12.dp))
        Box(Modifier.size(88.dp).clip(RoundedCornerShape(12.dp)).background(ChipBg)) {
            NewsImage(post.imageUrl, post.headline, Modifier.fillMaxSize(), widthPx = 260, heightPx = 260)
        }
    }
}

@Composable
private fun EmptyState(title: Int, body: Int, showTip: Boolean, onHome: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(170.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(150.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(BrandRed.copy(alpha = 0.14f), Color.Transparent)))
            )
            Box(
                Modifier.size(104.dp).softCard(RoundedCornerShape(20.dp)).background(CardWhite),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Newspaper, contentDescription = null, tint = Muted.copy(alpha = 0.55f), modifier = Modifier.size(64.dp))
            }
            Icon(
                Icons.Default.Bookmark,
                contentDescription = null,
                tint = BrandRed,
                modifier = Modifier.size(52.dp).offset(x = 38.dp, y = (-34).dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(stringResource(title), color = Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(body),
            color = Muted,
            fontSize = 14.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 320.dp)
        )
        Spacer(Modifier.height(20.dp))
        Row(
            Modifier
                .widthIn(max = 320.dp)
                .fillMaxWidth()
                .shadow(6.dp, CircleShape, ambientColor = BrandRed, spotColor = BrandRed)
                .clip(CircleShape)
                .background(BrandRed)
                .clickable(onClick = onHome)
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.library_browse), color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(Modifier.width(6.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        if (showTip) {
            Spacer(Modifier.height(24.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(CardWhite).padding(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    Modifier.size(28.dp).clip(CircleShape).background(Accent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Accent, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    stringResource(R.string.library_tip) + " " + stringResource(R.string.library_tip_body),
                    color = Muted,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            }
        }
    }
}
