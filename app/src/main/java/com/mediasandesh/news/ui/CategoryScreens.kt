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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.mediasandesh.news.data.Cities
import com.mediasandesh.news.data.Library
import com.mediasandesh.news.data.NewsTranslator
import com.mediasandesh.news.data.Repository
import com.mediasandesh.news.data.WpCategory
import com.mediasandesh.news.data.WpPost
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val menuOrder = listOf(
    "देश", "विदेश", "विज्ञानं", "राजनीति", "क्राइम", "खेल",
    "उत्तरप्रदेश", "उत्तराखण्ड", "उधम सिंह नगर", "रुद्रपुर",
    "किसान", "पर्यटन", "शिक्षा", "मनोरंजन", "स्वास्थ्य", "संस्कृति",
    "फिल्म", "पुलिस", "अन्य"
)

private const val CATEGORY_URL = "https://www.mediasandesh.com/?cat="

@Composable
fun CategoriesScreen(onOpen: (WpCategory) -> Unit) {
    var menu by remember { mutableStateOf<List<WpCategory>>(emptyList()) }
    var towns by remember { mutableStateOf<List<WpCategory>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    val english = LocalePrefs.get(LocalContext.current) == "en"

    LaunchedEffect(retry) {
        loading = true
        error = null
        try {
            val loaded = withContext(Dispatchers.IO) {
                Repository.categories().filter { it.count > 0 && it.name != "Featured" }
            }
            val main = menuOrder.mapNotNull { name -> loaded.find { it.name == name } }
            val rest = loaded.filter { it.name !in menuOrder }.sortedByDescending { it.count }
            if (english) {
                menu = main.map { it.copy(name = NewsTranslator.categoryKnown(it.name)) }
                towns = rest.map { it.copy(name = NewsTranslator.categoryKnown(it.name)) }
                loading = false
                menu = withContext(Dispatchers.IO) { NewsTranslator.categories(main) }
                towns = withContext(Dispatchers.IO) { NewsTranslator.categories(rest) }
            } else {
                menu = main
                towns = rest
                loading = false
            }
        } catch (e: Exception) {
            error = e.message ?: "Network error"
            loading = false
        }
    }

    when {
        loading && menu.isEmpty() && towns.isEmpty() -> Loading()
        error != null && menu.isEmpty() && towns.isEmpty() -> ErrorState(error!!, onRetry = { retry++ })
        else -> LazyVerticalGrid(
            columns = GridCells.Adaptive(150.dp),
            modifier = Modifier.fillMaxSize().background(Paper),
            contentPadding = PaddingValues(10.dp)
        ) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                SectionTitle(stringResource(R.string.section_main_categories))
            }
            items(menu.distinctBy { it.id }, key = { "menu-${it.id}" }) { CategoryChip(it) { onOpen(it) } }
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                SectionTitle(stringResource(R.string.section_towns))
            }
            items(towns.distinctBy { it.id }, key = { "town-${it.id}" }) { CategoryChip(it) { onOpen(it) } }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryPostsScreen(
    categoryId: Int,
    categoryName: String,
    onOpen: (Int) -> Unit,
    onBack: () -> Unit = {},
    onAllCategories: () -> Unit = {},
    onLibrary: () -> Unit = {},
    onSubmit: () -> Unit = {}
) {
    val context = LocalContext.current
    val english = LocalePrefs.get(context) == "en"
    var currentId by rememberSaveable { mutableIntStateOf(categoryId) }
    var currentName by rememberSaveable { mutableStateOf(categoryName) }

    var posts by remember(currentId) { mutableStateOf<List<WpPost>>(emptyList()) }
    var loading by remember(currentId) { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var error by remember(currentId) { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    var page by remember(currentId) { mutableIntStateOf(1) }
    var hasMore by remember(currentId) { mutableStateOf(true) }
    var loadingMore by remember(currentId) { mutableStateOf(false) }
    var allCategories by remember { mutableStateOf<List<WpCategory>>(emptyList()) }
    val list = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val names = rememberCategoryNames(english)

    LaunchedEffect(Unit) {
        allCategories = withContext(Dispatchers.IO) { runCatching { Repository.categories() }.getOrDefault(emptyList()) }
    }

    val city = Cities.find(currentId)
    val siblings: List<Pair<Int, String>> = remember(allCategories, currentId, english) {
        val base = if (city != null || Cities.find(categoryId) != null) {
            Cities.all.map { it.id to it.label(english) }
        } else {
            menuOrder.mapNotNull { name -> allCategories.find { it.name == name && it.count > 0 } }
                .map { it.id to if (english) NewsTranslator.categoryKnown(it.name) else it.name }
        }
        if (base.any { it.first == currentId }) base else listOf(currentId to currentName) + base
    }
    val total = allCategories.find { it.id == currentId }?.count

    suspend fun shown(items: List<WpPost>) =
        if (english) withContext(Dispatchers.IO) { NewsTranslator.postsTitles(items) } else items

    LaunchedEffect(currentId, retry) {
        loading = posts.isEmpty()
        error = null
        try {
            val loaded = withContext(Dispatchers.IO) { Repository.byCategory(currentId) }
            page = 1
            hasMore = loaded.size >= 10
            posts = loaded
            loading = false
            refreshing = false
            posts = shown(loaded)
        } catch (e: Exception) {
            error = e.message ?: "Network error"
            loading = false
            refreshing = false
        }
    }

    val nearEnd by remember {
        derivedStateOf {
            val last = list.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= list.layoutInfo.totalItemsCount - 3
        }
    }
    LaunchedEffect(nearEnd, hasMore, posts.size) {
        if (!nearEnd || !hasMore || loadingMore || posts.isEmpty()) return@LaunchedEffect
        loadingMore = true
        scope.launch {
            try {
                val next = withContext(Dispatchers.IO) { Repository.byCategory(currentId, page + 1) }
                page += 1
                hasMore = next.size >= 10
                posts = (posts + shown(next)).distinctBy { it.id }
            } catch (_: Exception) {
                hasMore = false
            } finally {
                loadingMore = false
            }
        }
    }

    val otherNames = names - currentId
    fun place(post: WpPost) = post.city()?.takeIf { it.id != currentId }?.label(english)

    Column(Modifier.fillMaxSize().background(Paper)) {
        CategoryHeader(
            name = currentName,
            subtitle = if (city != null) stringResource(R.string.cat_fresh_in, Cities.district.label(english)) else stringResource(R.string.cat_fresh),
            total = total,
            onBack = onBack,
            onShare = { shareText(context, currentName + " — " + context.getString(R.string.brand_name), CATEGORY_URL + currentId, whatsapp = false) }
        )
        SiblingChips(siblings, currentId) { id, name ->
            if (id != currentId) {
                currentId = id
                currentName = name
                scope.launch { list.scrollToItem(0) }
            }
        }
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = { refreshing = true; retry++ },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(Modifier.fillMaxSize(), state = list, contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp)) {
                item(key = "offline") { OfflineBanner() }
                when {
                    loading -> item(key = "skeleton") { NewsSkeleton(showHero = true) }
                    error != null && posts.isEmpty() -> item(key = "error") {
                        LoadProblem(
                            offline = !Repository.isOnline(),
                            message = error!!,
                            onRetry = { retry++ },
                            onLibrary = onLibrary
                        )
                    }
                    posts.isEmpty() -> item(key = "empty") { EmptyCategory(currentName, onAllCategories) }
                    else -> {
                        val lead = posts.first()
                        item(key = "lead-${lead.id}") {
                            FeaturedStory(lead, lead.categoryTag(otherNames) ?: currentName, place(lead)) { onOpen(lead.id) }
                        }
                        if (posts.size > 1) item(key = "section") { TopStoriesLabel() }
                        items(posts.drop(1).distinctBy { it.id }, key = { "cat-${it.id}" }) {
                            CompactStory(it, it.categoryTag(otherNames), place(it)) { onOpen(it.id) }
                        }
                        if (loadingMore) item(key = "more") { LoadingMoreRow() }
                    }
                }
                if (!loading && (error == null || posts.isNotEmpty())) {
                    item(key = "submit") { SubmitNudge(currentName, onSubmit) }
                }
            }
        }
    }
}

@Composable
private fun CategoryHeader(name: String, subtitle: String, total: Int?, onBack: () -> Unit, onShare: () -> Unit) {
    val pulse = rememberInfiniteTransition(label = "live")
    val dot by pulse.animateFloat(0.35f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "liveDot")
    Row(
        Modifier.fillMaxWidth().background(CardWhite).padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.back),
            tint = Ink,
            modifier = Modifier.size(48.dp).clip(CircleShape).clickable(onClick = onBack).padding(12.dp)
        )
        Column(Modifier.weight(1f).padding(start = 2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(9.dp).alpha(dot).clip(CircleShape).background(BrandRed))
                Text(
                    name,
                    color = Ink,
                    fontSize = 22.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Text(subtitle, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (total != null && total > 0) {
            Text(
                pluralStringResource(R.plurals.cat_total, total, NumberFormat.getIntegerInstance(Locale("en", "IN")).format(total)),
                color = Muted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clip(CircleShape).background(ChipBg).padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
        Icon(
            Icons.Default.Share,
            contentDescription = stringResource(R.string.share),
            tint = Muted,
            modifier = Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onShare).padding(11.dp)
        )
    }
}

@Composable
private fun SiblingChips(chips: List<Pair<Int, String>>, selected: Int, onPick: (Int, String) -> Unit) {
    if (chips.size < 2) return
    val state = rememberLazyListState()
    LaunchedEffect(selected, chips.size) {
        val index = chips.indexOfFirst { it.first == selected }
        if (index > 0) state.animateScrollToItem((index - 1).coerceAtLeast(0))
    }
    LazyRow(
        state = state,
        modifier = Modifier.fillMaxWidth().background(CardWhite),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(chips, key = { it.first }) { (id, name) ->
            val active = id == selected
            Row(
                Modifier
                    .clip(CircleShape)
                    .background(if (active) BrandRed else ChipBg)
                    .clickable { onPick(id, name) }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (active) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(Color.White))
                    Spacer(Modifier.width(6.dp))
                }
                Text(name, color = if (active) Color.White else Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
    }
}

@Composable
private fun FeaturedStory(post: WpPost, tag: String, place: String?, onClick: () -> Unit) {
    val context = LocalContext.current
    val saved = Library.isSaved(post.id)
    Column(
        Modifier
            .padding(top = 12.dp)
            .fillMaxWidth()
            .menuCard()
            .background(CardWhite)
            .clickable(onClick = onClick)
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(ChipBg)) {
            NewsImage(post.imageUrl, post.headline, Modifier.fillMaxSize(), widthPx = 1000, heightPx = 563)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.5f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.55f))))
            Row(
                Modifier.align(Alignment.TopStart).padding(12.dp).clip(CircleShape).background(BrandRed).padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(Color.White))
                Spacer(Modifier.width(5.dp))
                Text(tag, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp)) {
            Text(
                post.headline,
                color = Ink,
                fontSize = 18.sp,
                lineHeight = 27.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            val summary = post.summary.trim()
            if (summary.isNotEmpty()) {
                Text(
                    summary,
                    color = Muted,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = RedInk, modifier = Modifier.size(14.dp))
                    Text(formatDate(post.date), color = Muted, fontSize = 12.sp, maxLines = 1, modifier = Modifier.padding(start = 4.dp))
                    if (place != null) {
                        Text("  •  ", color = Muted, fontSize = 12.sp)
                        Icon(Icons.Default.Place, contentDescription = null, tint = RedInk, modifier = Modifier.size(14.dp))
                        Text(place, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 2.dp))
                    }
                }
                Icon(
                    if (saved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = stringResource(if (saved) R.string.unsave else R.string.save),
                    tint = if (saved) RedInk else Muted,
                    modifier = Modifier.size(36.dp).clip(CircleShape).clickable { Library.toggleSaved(post) }.padding(8.dp)
                )
                Row(
                    Modifier
                        .clip(CircleShape)
                        .background(ChipBg)
                        .clickable { shareText(context, post.headline, post.link, whatsapp = false) }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = WhatsAppBrand, modifier = Modifier.size(15.dp))
                    Text(stringResource(R.string.share), color = Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 5.dp))
                }
            }
        }
    }
}

@Composable
private fun TopStoriesLabel() {
    Row(Modifier.padding(top = 20.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(4.dp).height(18.dp).clip(CircleShape).background(BrandRed))
        Text(stringResource(R.string.cat_top), color = Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun CompactStory(post: WpPost, tag: String?, place: String?, onClick: () -> Unit) {
    val saved = Library.isSaved(post.id)
    Row(
        Modifier
            .padding(top = 10.dp)
            .fillMaxWidth()
            .menuCard(RoundedCornerShape(14.dp))
            .background(CardWhite)
            .clickable(onClick = onClick)
            .padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (tag != null) {
                    Text(tag, color = RedInk, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    Text("  •  ", color = Muted, fontSize = 11.sp)
                }
                Text(formatDate(post.date), color = Muted, fontSize = 11.sp, maxLines = 1)
            }
            Text(
                post.headline,
                color = Ink,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
            Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                if (place != null) {
                    Icon(Icons.Default.Place, contentDescription = null, tint = Muted, modifier = Modifier.size(13.dp))
                    Text(place, color = Muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(start = 3.dp))
                } else {
                    Spacer(Modifier.weight(1f))
                }
                Icon(
                    if (saved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = stringResource(if (saved) R.string.unsave else R.string.save),
                    tint = if (saved) RedInk else Muted,
                    modifier = Modifier.size(30.dp).clip(CircleShape).clickable { Library.toggleSaved(post) }.padding(6.dp)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        NewsImage(
            url = post.imageUrl,
            description = null,
            modifier = Modifier.size(92.dp).clip(RoundedCornerShape(10.dp)).background(ChipBg),
            widthPx = 276,
            heightPx = 276
        )
    }
}

@Composable
private fun LoadingMoreRow() {
    Row(
        Modifier.padding(top = 14.dp).fillMaxWidth().menuCard(RoundedCornerShape(14.dp)).background(CardWhite).padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(color = BrandRed, strokeWidth = 2.5.dp, modifier = Modifier.size(20.dp))
        Text(stringResource(R.string.cat_loading_more), color = Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 12.dp))
    }
}

@Composable
private fun StateCard(content: @Composable () -> Unit) {
    Column(
        Modifier.padding(top = 16.dp).fillMaxWidth().menuCard().background(CardWhite).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) { content() }
}

@Composable
private fun StateIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, badge: androidx.compose.ui.graphics.vector.ImageVector?) {
    Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(80.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = RedInk, modifier = Modifier.size(40.dp))
        }
        if (badge != null) {
            Box(
                Modifier.align(Alignment.BottomEnd).padding(end = 6.dp, bottom = 4.dp).size(28.dp).clip(CircleShape).background(BrandRed),
                contentAlignment = Alignment.Center
            ) {
                Icon(badge, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun RedButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, leading: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.padding(top = 20.dp).fillMaxWidth().clip(CircleShape).background(BrandRed).clickable(onClick = onClick).padding(vertical = 13.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading) Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.padding(end = 8.dp).size(20.dp))
        Text(text, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        if (!leading) Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.padding(start = 8.dp).size(18.dp))
    }
}

@Composable
private fun EmptyCategory(name: String, onAllCategories: () -> Unit) {
    StateCard {
        StateIcon(Icons.Default.Newspaper, null)
        Text(stringResource(R.string.cat_empty_title), color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 12.dp))
        Text(
            stringResource(R.string.cat_empty_body, name),
            color = Muted,
            fontSize = 14.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
        RedButton(stringResource(R.string.cat_empty_cta), Icons.AutoMirrored.Filled.ArrowForward, leading = false, onClick = onAllCategories)
    }
}

@Composable
private fun LoadProblem(offline: Boolean, message: String, onRetry: () -> Unit, onLibrary: () -> Unit) {
    StateCard {
        StateIcon(if (offline) Icons.Default.WifiOff else Icons.Default.Newspaper, Icons.Default.PriorityHigh)
        Text(
            stringResource(if (offline) R.string.offline_title else R.string.load_failed),
            color = Ink,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp)
        )
        Text(
            if (offline) stringResource(R.string.cat_offline_body) else message,
            color = Muted,
            fontSize = 14.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
        RedButton(stringResource(R.string.retry), Icons.Default.Refresh, leading = true, onClick = onRetry)
        if (offline) {
            Row(
                Modifier.padding(top = 20.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(ChipBg.copy(alpha = 0.6f)).clickable(onClick = onLibrary).padding(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(Modifier.size(30.dp).clip(CircleShape).background(ChipBg), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.BookmarkAdded, contentDescription = null, tint = RedInk, modifier = Modifier.size(18.dp))
                }
                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                    Text(stringResource(R.string.cat_offline_tip_title), color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.cat_offline_tip_body), color = Muted, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 2.dp))
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Muted, modifier = Modifier.padding(top = 4.dp).size(20.dp))
            }
        }
    }
}

@Composable
private fun SubmitNudge(name: String, onSubmit: () -> Unit) {
    Row(
        Modifier.padding(top = 20.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(ChipBg).clickable(onClick = onSubmit).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Campaign, contentDescription = null, tint = RedInk, modifier = Modifier.size(21.dp))
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(stringResource(R.string.cat_submit_title, name), color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(stringResource(R.string.cat_submit_body), color = Muted, fontSize = 12.sp)
        }
        Row(
            Modifier.clip(CircleShape).background(CardWhite).padding(start = 14.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.cat_submit_cta), color = RedInk, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = RedInk, modifier = Modifier.size(18.dp))
        }
    }
}
