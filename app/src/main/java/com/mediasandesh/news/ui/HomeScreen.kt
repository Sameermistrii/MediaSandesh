package com.mediasandesh.news.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mediasandesh.news.AppPrefs
import com.mediasandesh.news.LocalePrefs
import com.mediasandesh.news.R
import com.mediasandesh.news.data.Cities
import com.mediasandesh.news.data.NewsTranslator
import com.mediasandesh.news.data.Repository
import com.mediasandesh.news.data.WpPost
import com.mediasandesh.news.widget.HeadlinesWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class Topic(val id: Int, val hi: String, val en: String)

private val topics = listOf(
    Topic(152, "रुद्रपुर", "Rudrapur"),
    Topic(150, "उत्तराखण्ड", "Uttarakhand"),
    Topic(135, "उधम सिंह नगर", "Udham Singh Nagar"),
    Topic(78, "राजनीति", "Politics"),
    Topic(77, "क्राइम", "Crime"),
    Topic(76, "खेल", "Sports"),
    Topic(74, "देश", "Nation")
)

@Composable
private fun TopicRow(onCategory: (Int, String) -> Unit) {
    val english = LocalePrefs.get(LocalContext.current) == "en"
    var trending by remember { mutableStateOf<List<Topic>>(emptyList()) }
    LaunchedEffect(english) {
        val found = withContext(Dispatchers.IO) { runCatching { Repository.trending() }.getOrDefault(emptyList()) }
        trending = found.map { Topic(it.id, it.name, NewsTranslator.categoryKnown(it.name)) }
    }
    val shown = trending.ifEmpty { topics }
    val row = remember(trending.isNotEmpty()) { LazyListState() }
    HomeTopicRow(
        label = if (trending.isNotEmpty()) stringResource(R.string.search_trending) else null,
        topics = shown.map { it.id to if (english) it.en else it.hi },
        state = row,
        onTopic = onCategory
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpen: (Int) -> Unit,
    onCategory: (Int, String) -> Unit,
    onLibrary: () -> Unit,
    onQuick: () -> Unit,
    onLocal: () -> Unit,
    onSubmit: () -> Unit
) {
    var posts by remember { mutableStateOf<List<WpPost>>(emptyList()) }
    var local by remember { mutableStateOf<List<WpPost>>(emptyList()) }
    val city = Cities.find(AppPrefs.cityId)
    var picks by remember { mutableStateOf<List<WpPost>>(emptyList()) }
    var older by remember { mutableStateOf<List<WpPost>>(emptyList()) }
    var page by remember { mutableIntStateOf(1) }
    var loadingMore by remember { mutableStateOf(false) }
    var endReached by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var translating by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val english = LocalePrefs.get(context) == "en"
    val list = rememberLazyListState()
    val categoryNames = rememberCategoryNames(english)

    LaunchedEffect(retry) {
        loading = true
        translating = false
        error = null
        older = emptyList()
        page = 1
        endReached = false
        try {
            val latest = withContext(Dispatchers.IO) { Repository.latest() }
            val featured = withContext(Dispatchers.IO) {
                runCatching { Repository.featured() }.getOrDefault(emptyList())
            }.filter { pick -> latest.none { it.id == pick.id } }
            posts = latest
            if (!Repository.offline) HeadlinesWidget.refresh(context)
            picks = featured
            loading = false
            refreshing = false
            if (english) {
                translating = true
                posts = withContext(Dispatchers.IO) { NewsTranslator.postsTitles(latest) }
                picks = withContext(Dispatchers.IO) { NewsTranslator.postsTitles(featured) }
                translating = false
            }
        } catch (e: Exception) {
            error = e.message ?: "Network error"
            loading = false
            refreshing = false
            translating = false
        }
    }

    LaunchedEffect(city?.id, retry) {
        val c = city ?: run { local = emptyList(); return@LaunchedEffect }
        val loaded = withContext(Dispatchers.IO) {
            runCatching { Repository.byCategory(c.id) }.getOrDefault(emptyList())
        }
        local = loaded
        if (english && loaded.isNotEmpty()) local = withContext(Dispatchers.IO) { NewsTranslator.postsTitles(loaded) }
    }

    val nearEnd by remember {
        derivedStateOf {
            val info = list.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && last >= info.totalItemsCount - 3
        }
    }
    LaunchedEffect(retry) {
        snapshotFlow { Triple(nearEnd, posts.size, older.size) }.collect { (near, shownCount, _) ->
            if (!near || shownCount == 0 || endReached || Repository.offline) return@collect
            loadingMore = true
            try {
                val next = page + 1
                val loaded = withContext(Dispatchers.IO) { runCatching { Repository.latest(next) }.getOrNull() }
                if (loaded.isNullOrEmpty()) {
                    endReached = true
                } else {
                    val shown = if (english) withContext(Dispatchers.IO) { NewsTranslator.postsTitles(loaded) } else loaded
                    older = (older + shown).distinctBy { it.id }
                    page = next
                }
            } finally {
                loadingMore = false
            }
        }
    }

    fun cityOf(post: WpPost) = post.city()

    fun categoryTag(post: WpPost): String? = post.categoryTag(categoryNames)

    fun tagFor(post: WpPost): String? = cityOf(post)?.label(english) ?: categoryTag(post)

    fun placeFor(post: WpPost): String? = post.place()?.label(english)

    when {
        loading && posts.isEmpty() -> NewsSkeleton()
        error != null && posts.isEmpty() -> ErrorState(error!!, onRetry = { retry++ }, onOpenLibrary = onLibrary)
        else -> PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = { refreshing = true; retry++ },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                Modifier.fillMaxSize().background(Paper),
                state = list,
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item(key = "offline") { OfflineBanner() }
                if (translating) {
                    item(key = "translating") { SectionTitle(stringResource(R.string.translating)) }
                }
                item(key = "topics") { TopicRow(onCategory) }
                item(key = "ticker") { HomeTicker(posts) { onOpen(it.id) } }
                posts.firstOrNull()?.let { lead ->
                    item(key = "hero") {
                        androidx.compose.foundation.layout.Box(Modifier.padding(top = 16.dp)) {
                            HomeHero(lead, placeFor(lead)) { onOpen(lead.id) }
                        }
                    }
                }
                item(key = "quick-card") { QuickCard(onQuick) }
                val latest = posts.drop(1).take(4).distinctBy { it.id }
                if (latest.isNotEmpty()) {
                    item(key = "latest-title") { BarHeader(stringResource(R.string.section_latest)) }
                    item(key = "latest-group") { LatestGroup(latest, ::tagFor) { onOpen(it.id) } }
                }
                val cityPosts = local.filter { l -> posts.take(5).none { it.id == l.id } }.take(6)
                if (city == null) {
                    item(key = "city-title") {
                        BarHeader("📍 " + stringResource(R.string.tab_local), stringResource(R.string.city_choose), onLocal)
                    }
                    item(key = "city-empty") {
                        Text(
                            stringResource(R.string.city_pick_body),
                            color = Muted,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(CardWhite)
                                .clickable(onClick = onLocal)
                                .padding(16.dp)
                        )
                    }
                } else if (cityPosts.isNotEmpty()) {
                    item(key = "city-title") {
                        BarHeader("📍 " + stringResource(R.string.city_news, city.label(english)), stringResource(R.string.see_all), onLocal)
                    }
                    item(key = "city-pill") { CityPill(city.label(english), onLocal) }
                    item(key = "city-row") { CityCarousel(cityPosts, { p -> cityOf(p)?.takeIf { it.id != city.id }?.label(english) ?: categoryTag(p) }) { onOpen(it.id) } }
                }
                item(key = "submit-card") { SubmitCard(onSubmit) }
                val more = (posts.drop(5) + picks + older)
                    .filter { m -> cityPosts.none { it.id == m.id } && latest.none { it.id == m.id } && m.id != posts.firstOrNull()?.id }
                    .distinctBy { it.id }
                if (more.isNotEmpty()) {
                    item(key = "more-title") { BarHeader(stringResource(R.string.section_more)) }
                    items(more, key = { "more-${it.id}" }) { MoreCard(it, tagFor(it)) { onOpen(it.id) } }
                }
                if (loadingMore) item(key = "loading-more") { LoadingDots() }
            }
        }
    }
}
