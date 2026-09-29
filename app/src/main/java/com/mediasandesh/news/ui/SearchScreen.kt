package com.mediasandesh.news.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediasandesh.news.AppPrefs
import com.mediasandesh.news.LocalePrefs
import com.mediasandesh.news.R
import com.mediasandesh.news.data.Cities
import com.mediasandesh.news.data.NewsTranslator
import com.mediasandesh.news.data.Repository
import com.mediasandesh.news.data.VideoItem
import com.mediasandesh.news.data.Videos
import com.mediasandesh.news.data.WpCategory
import com.mediasandesh.news.data.WpPost
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val Card16 = RoundedCornerShape(16.dp)
private val SoftShadow = Color(0x14000000)
private const val PAGE_SIZE = 15
private const val LATEST_COUNT = 5

private fun Modifier.softCard(shape: RoundedCornerShape = Card16) = this
    .shadow(2.dp, shape, clip = false, ambientColor = SoftShadow, spotColor = SoftShadow)
    .clip(shape)

private enum class SearchFilter(val label: Int) {
    ALL(R.string.search_filter_all),
    NEWS(R.string.search_filter_news),
    VIDEOS(R.string.search_filter_videos),
    TODAY(R.string.search_filter_today),
    WEEK(R.string.search_filter_week)
}

private fun WpPost.day(): LocalDate? = runCatching { LocalDateTime.parse(date.take(19)).toLocalDate() }.getOrNull()

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(onOpen: (Int) -> Unit, onCategory: (Int, String) -> Unit, onVideo: (String) -> Unit = {}) {
    val context = LocalContext.current
    val english = LocalePrefs.get(context) == "en"
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = remember { FocusRequester() }
    val categoryNames = rememberCategoryNames(english)

    var query by rememberSaveable { mutableStateOf("") }
    var submitted by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(SearchFilter.ALL) }
    var posts by remember { mutableStateOf<List<WpPost>>(emptyList()) }
    var videos by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var topics by remember { mutableStateOf<List<WpCategory>>(emptyList()) }
    var latest by remember { mutableStateOf<List<WpPost>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var loadingMore by remember { mutableStateOf(false) }
    var hasMore by remember { mutableStateOf(false) }
    var page by remember { mutableIntStateOf(1) }
    var error by remember { mutableStateOf<String?>(null) }
    var token by remember { mutableIntStateOf(0) }

    fun run(q: String) {
        val clean = q.trim()
        if (clean.isEmpty()) return
        query = clean
        submitted = clean
        filter = SearchFilter.ALL
        AppPrefs.addSearch(context, clean)
        keyboard?.hide()
        token++
    }

    fun reset() {
        query = ""
        submitted = ""
        posts = emptyList()
        videos = emptyList()
        runCatching { focus.requestFocus() }
    }

    val voice = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let(::run)
        }
    }
    fun startVoice() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (english) "en-IN" else "hi-IN")
            .putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(R.string.search_voice))
        try {
            voice.launch(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, context.getString(R.string.search_voice_missing), Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (submitted.isBlank()) runCatching { focus.requestFocus() }
        topics = withContext(Dispatchers.IO) { runCatching { Repository.trending() }.getOrDefault(emptyList()) }
        val fresh = withContext(Dispatchers.IO) { runCatching { Repository.latest().take(LATEST_COUNT) }.getOrDefault(emptyList()) }
        latest = fresh
        if (english && fresh.isNotEmpty()) latest = withContext(Dispatchers.IO) { NewsTranslator.postsTitles(fresh) }
    }

    LaunchedEffect(token) {
        if (submitted.isBlank()) return@LaunchedEffect
        loading = true
        error = null
        page = 1
        videos = emptyList()
        try {
            val found = withContext(Dispatchers.IO) { Repository.search(submitted) }
            posts = found
            hasMore = found.size >= PAGE_SIZE
            loading = false
            val words = submitted.lowercase().split(Regex("\\s+")).filter { it.length >= 2 }
            videos = withContext(Dispatchers.IO) {
                runCatching { Videos.recent() }.getOrDefault(emptyList())
                    .filter { v -> words.isNotEmpty() && words.all { v.title.lowercase().contains(it) } }
            }
            if (english) posts = withContext(Dispatchers.IO) { NewsTranslator.postsTitles(found) }
        } catch (e: Exception) {
            error = e.message ?: "Search failed"
            loading = false
        }
    }

    suspend fun loadMore() {
        if (loadingMore || !hasMore) return
        loadingMore = true
        try {
            val next = withContext(Dispatchers.IO) { Repository.search(submitted, page + 1) }
            page += 1
            hasMore = next.size >= PAGE_SIZE
            val shown = if (english) withContext(Dispatchers.IO) { NewsTranslator.postsTitles(next) } else next
            posts = (posts + shown).distinctBy { it.id }
        } catch (_: Exception) {
            hasMore = false
        } finally {
            loadingMore = false
        }
    }

    Column(Modifier.fillMaxSize().background(Paper)) {
        SearchField(
            value = query,
            onValueChange = { query = it },
            onSearch = { run(query) },
            onClear = ::reset,
            onVoice = ::startVoice,
            focus = focus
        )

        when {
            submitted.isBlank() -> LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                if (AppPrefs.recentSearches.isNotEmpty()) {
                    item(key = "recent-title") {
                        Row(
                            Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.search_recent), color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                            Text(
                                stringResource(R.string.search_clear_all),
                                color = RedInk,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { AppPrefs.clearSearches(context) }.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    item(key = "recent-list") {
                        Column(Modifier.padding(horizontal = 16.dp).fillMaxWidth().softCard().background(CardWhite).padding(vertical = 4.dp)) {
                            AppPrefs.recentSearches.forEach { q ->
                                Row(
                                    Modifier.fillMaxWidth().clickable { run(q) }.padding(start = 14.dp, end = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Schedule, contentDescription = null, tint = Muted, modifier = Modifier.size(20.dp))
                                    Text(
                                        q,
                                        color = Ink,
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 12.dp)
                                    )
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = stringResource(R.string.clear),
                                        tint = Muted,
                                        modifier = Modifier.size(36.dp).clip(CircleShape).clickable { AppPrefs.removeSearch(context, q) }.padding(9.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                if (topics.isNotEmpty()) {
                    item(key = "topics-title") { BarHeader(stringResource(R.string.search_topics)) }
                    item(key = "topics") {
                        TopicChips(topics, english) { id, name -> onCategory(id, name) }
                    }
                }
                item(key = "cities-title") { BarHeader(stringResource(R.string.search_cities)) }
                item(key = "cities") {
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Cities.all.forEach { city ->
                            Row(
                                Modifier
                                    .softCard(RoundedCornerShape(50))
                                    .background(CardWhite)
                                    .clickable { onCategory(city.id, city.label(english)) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = RedInk, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(city.label(english), color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }
                    }
                }
                if (latest.isNotEmpty()) {
                    item(key = "latest-title") {
                        Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = RedInk, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.search_latest), color = Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                            Box(Modifier.size(8.dp).clip(CircleShape).background(BrandRed))
                        }
                    }
                    itemsIndexed(latest, key = { _, p -> "latest-${p.id}" }) { i, p ->
                        NumberedStory(i + 1, p, p.categoryTag(categoryNames), p.place()?.label(english)) { onOpen(p.id) }
                    }
                }
            }
            loading -> NewsSkeleton(showHero = false)
            error != null -> ErrorState(error!!, onRetry = { token++ })
            posts.isEmpty() && videos.isEmpty() -> NoResults(topics, english, onReset = ::reset, onTopic = onCategory)
            else -> {
                val today = LocalDate.now()
                val shownPosts = when (filter) {
                    SearchFilter.VIDEOS -> emptyList()
                    SearchFilter.TODAY -> posts.filter { it.day() == today }
                    SearchFilter.WEEK -> posts.filter { d -> d.day()?.isAfter(today.minusDays(7)) == true }
                    else -> posts
                }.distinctBy { it.id }
                val shownVideos = if (filter == SearchFilter.ALL || filter == SearchFilter.VIDEOS) videos else emptyList()
                val total = shownPosts.size + shownVideos.size
                val paging = hasMore && filter != SearchFilter.VIDEOS
                LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                    item(key = "offline") { OfflineBanner() }
                    item(key = "filters") {
                        Row(
                            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SearchFilter.entries.forEach { f ->
                                val selected = f == filter
                                Row(
                                    Modifier
                                        .clip(CircleShape)
                                        .background(if (selected) BrandRed else ChipBg)
                                        .clickable { filter = f }
                                        .padding(horizontal = 14.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (f == SearchFilter.VIDEOS) {
                                        Icon(Icons.Default.PlayCircle, contentDescription = null, tint = if (selected) Color.White else Muted, modifier = Modifier.size(15.dp))
                                        Spacer(Modifier.width(4.dp))
                                    }
                                    Text(
                                        stringResource(f.label),
                                        color = if (selected) Color.White else Muted,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                    item(key = "count") {
                        Row(Modifier.padding(horizontal = 16.dp).padding(top = 12.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(6.dp).clip(CircleShape).background(BrandRed))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                pluralStringResource(R.plurals.search_count, if (paging) 2 else total, submitted, if (paging) "$total+" else "$total"),
                                color = Muted,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    if (total == 0) {
                        item(key = "filter-empty") {
                            Text(
                                stringResource(R.string.search_filter_empty),
                                color = Muted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(32.dp)
                            )
                        }
                    }
                    val lead = shownPosts.take(1)
                    items(lead, key = { "post-${it.id}" }) { p ->
                        ResultCard(p, submitted, p.categoryTag(categoryNames), p.place()?.label(english)) { onOpen(p.id) }
                    }
                    items(shownVideos, key = { "video-${it.id}" }) { v -> VideoResult(v, submitted) { onVideo(v.id) } }
                    items(shownPosts.drop(1), key = { "post-${it.id}" }) { p ->
                        ResultCard(p, submitted, p.categoryTag(categoryNames), p.place()?.label(english)) { onOpen(p.id) }
                    }
                    if (paging) {
                        item(key = "more-$page") {
                            LaunchedEffect(page) { loadMore() }
                            LoadingDots()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    onVoice: () -> Unit,
    focus: FocusRequester
) {
    Row(
        Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .height(50.dp)
            .clip(CircleShape)
            .background(ChipBg)
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = Muted, modifier = Modifier.size(22.dp))
        Box(Modifier.weight(1f).padding(horizontal = 10.dp), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(stringResource(R.string.search_hint), color = Muted, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(color = Ink, fontSize = 15.sp),
                cursorBrush = SolidColor(BrandRed),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                modifier = Modifier.fillMaxWidth().focusRequester(focus)
            )
        }
        if (value.isNotEmpty()) {
            Icon(
                Icons.Default.Close,
                contentDescription = stringResource(R.string.clear),
                tint = Muted,
                modifier = Modifier.size(36.dp).clip(CircleShape).clickable(onClick = onClear).padding(8.dp)
            )
        }
        Icon(
            Icons.Default.Mic,
            contentDescription = stringResource(R.string.search_voice),
            tint = RedInk,
            modifier = Modifier.size(42.dp).clip(CircleShape).clickable(onClick = onVoice).padding(9.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TopicChips(topics: List<WpCategory>, english: Boolean, onTopic: (Int, String) -> Unit) {
    FlowRow(
        Modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        topics.forEach { c ->
            val name = if (english) NewsTranslator.categoryKnown(c.name) else c.name
            Text(
                "#${name.replace(" ", "")}",
                color = Ink,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(ChipBg)
                    .clickable { onTopic(c.id, name) }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            )
        }
    }
}

@Composable
private fun NumberedStory(rank: Int, post: WpPost, category: String?, place: String?, onClick: () -> Unit) {
    Row(
        Modifier
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .fillMaxWidth()
            .softCard()
            .background(CardWhite)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text("%02d".format(rank), color = RedInk, fontWeight = FontWeight.Bold, fontSize = 22.sp, modifier = Modifier.width(42.dp))
        Column(Modifier.weight(1f)) {
            Text(post.headline, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 21.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(formatDate(post.date), color = Muted, fontSize = 12.sp)
                category?.let {
                    Text("  •  ", color = Muted, fontSize = 12.sp)
                    Text(it, color = RedInk, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
                place?.let {
                    Text("  •  ", color = Muted, fontSize = 12.sp)
                    Text(it, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun highlighted(text: String, query: String): AnnotatedString {
    val words = query.split(Regex("\\s+")).filter { it.length >= 2 }
    val red = RedInk
    return buildAnnotatedString {
        append(text)
        words.forEach { w ->
            var start = text.indexOf(w, ignoreCase = true)
            while (start >= 0) {
                addStyle(SpanStyle(color = red, fontWeight = FontWeight.Bold), start, start + w.length)
                start = text.indexOf(w, start + w.length, ignoreCase = true)
            }
        }
    }
}

@Composable
private fun ResultCard(post: WpPost, query: String, category: String?, place: String?, onClick: () -> Unit) {
    Row(
        Modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .fillMaxWidth()
            .softCard()
            .background(CardWhite)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Column(Modifier.weight(1f).heightIn(min = 88.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Column {
                category?.let {
                    Text(
                        it,
                        color = RedInk,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        modifier = Modifier.clip(CircleShape).background(BrandRed.copy(alpha = 0.1f)).padding(horizontal = 8.dp, vertical = 1.dp)
                    )
                    Spacer(Modifier.height(5.dp))
                }
                Text(
                    highlighted(post.headline, query),
                    color = Ink,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(formatDate(post.date), color = Muted, fontSize = 12.sp)
                place?.let {
                    Text("  •  ", color = Muted, fontSize = 12.sp)
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = RedInk, modifier = Modifier.size(13.dp))
                    Text(it, color = RedInk, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Box(Modifier.size(88.dp).clip(RoundedCornerShape(12.dp)).background(ChipBg)) {
            NewsImage(post.imageUrl, post.headline, Modifier.fillMaxSize(), widthPx = 260, heightPx = 260)
        }
    }
}

@Composable
private fun VideoResult(video: VideoItem, query: String, onClick: () -> Unit) {
    Row(
        Modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .fillMaxWidth()
            .softCard()
            .background(CardWhite)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Column(Modifier.weight(1f).heightIn(min = 88.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Column {
                Row(
                    Modifier.clip(CircleShape).background(BrandRed).padding(start = 6.dp, end = 8.dp, top = 1.dp, bottom = 1.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                    Text(stringResource(R.string.search_filter_videos), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(5.dp))
                Text(
                    highlighted(video.title, query),
                    color = Ink,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (video.published.isNotBlank()) {
                Text(formatDate(video.published), color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Box(Modifier.size(width = 120.dp, height = 88.dp).clip(RoundedCornerShape(12.dp)).background(Color.Black), contentAlignment = Alignment.Center) {
            NewsImage(video.thumbnail, video.title, Modifier.fillMaxSize(), widthPx = 360, heightPx = 264)
            Box(Modifier.size(34.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.55f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun NoResults(topics: List<WpCategory>, english: Boolean, onReset: () -> Unit, onTopic: (Int, String) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
        item(key = "empty") {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(170.dp), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .size(150.dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(BrandRed.copy(alpha = 0.14f), Color.Transparent)))
                    )
                    Box(Modifier.size(104.dp).softCard(RoundedCornerShape(20.dp)).background(CardWhite), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Newspaper, contentDescription = null, tint = Muted.copy(alpha = 0.55f), modifier = Modifier.size(64.dp))
                    }
                    Box(
                        Modifier.offset(x = 36.dp, y = 30.dp).size(56.dp).shadow(6.dp, CircleShape).clip(CircleShape).background(BrandRed),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.search_empty), color = Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.search_empty_body), color = Muted, fontSize = 14.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(18.dp))
                Text(
                    stringResource(R.string.search_reset),
                    color = RedInk,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(BrandRed.copy(alpha = 0.1f))
                        .clickable(onClick = onReset)
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                )
            }
        }
        if (topics.isNotEmpty()) {
            item(key = "try-title") { BarHeader(stringResource(R.string.search_try_topics)) }
            item(key = "try") { TopicChips(topics, english, onTopic) }
        }
    }
}
