package com.mediasandesh.news.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediasandesh.news.AppPrefs
import com.mediasandesh.news.LocalePrefs
import com.mediasandesh.news.R
import com.mediasandesh.news.data.Cities
import com.mediasandesh.news.data.City
import com.mediasandesh.news.data.Library
import com.mediasandesh.news.data.NewsTranslator
import com.mediasandesh.news.data.Repository
import com.mediasandesh.news.data.Weather
import com.mediasandesh.news.data.WeatherNow
import com.mediasandesh.news.data.WpPost
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Card16 = RoundedCornerShape(16.dp)
private val SoftShadow = Color(0x14000000)

private fun Modifier.softCard(shape: RoundedCornerShape = Card16) = this
    .shadow(2.dp, shape, clip = false, ambientColor = SoftShadow, spotColor = SoftShadow)
    .clip(shape)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalScreen(onOpen: (Int) -> Unit) {
    val context = LocalContext.current
    val english = LocalePrefs.get(context) == "en"
    val city = Cities.find(AppPrefs.cityId)
    var picking by remember { mutableStateOf(false) }

    if (city == null) {
        CityPicker(english, selected = null, onClose = null) { AppPrefs.setCity(context, it.id) }
        return
    }

    val scopes = listOfNotNull(city, city.district?.let { Cities.district }, Cities.state)
    var scopeId by rememberSaveable(city.id) { mutableIntStateOf(city.id) }
    val scope = scopes.find { it.id == scopeId } ?: city
    var weather by remember(city.id) { mutableStateOf(Weather.cached(city)) }
    LaunchedEffect(city.id) {
        weather = withContext(Dispatchers.IO) { runCatching { Weather.get(city) }.getOrNull() } ?: weather
    }

    Column(Modifier.fillMaxSize().background(Paper)) {
        CityFeed(
            categoryId = scope.id,
            label = scope.label(english),
            english = english,
            onOpen = onOpen,
            headerReady = weather != null
        ) {
            CitySelector(city, english) { picking = true }
            ScopeChips(scopes, scope.id, english) { scopeId = it }
            weather?.let { WeatherCard(it, Weather.fetchedAt(city), english) }
        }
    }

    if (picking) {
        val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { picking = false }, sheetState = sheet, containerColor = CardWhite) {
            CityPicker(english, selected = city, onClose = { picking = false }) {
                AppPrefs.setCity(context, it.id)
                picking = false
            }
        }
    }
}

@Composable
private fun CitySelector(city: City, english: Boolean, onChange: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp)
            .softCard()
            .background(CardWhite)
            .clickable(onClick = onChange)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = RedInk, modifier = Modifier.size(24.dp))
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(city.label(english), color = Ink, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(
                Cities.districtOf(city, english) + ", " + stringResource(R.string.city_state_suffix),
                color = Muted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(
            Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.city_change_long), color = RedInk, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = RedInk, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun ScopeChips(scopes: List<City>, selected: Int, english: Boolean, onPick: (Int) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(scopes, key = { "scope-${it.id}" }) { s ->
            val active = s.id == selected
            Row(
                Modifier
                    .clip(CircleShape)
                    .background(if (active) BrandRed else ChipBg)
                    .clickable { onPick(s.id) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (active) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    s.label(english),
                    color = if (active) Color.White else Ink,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun WeatherCard(w: WeatherNow, fetchedAt: Long?, english: Boolean) {
    val locale = if (english) Locale.ENGLISH else Locale("hi", "IN")
    val sky = if (w.isDay) listOf(Color(0xFF1E88E5), Color(0xFF64B5F6)) else listOf(Color(0xFF1A237E), Color(0xFF3949AB))
    val shape = RoundedCornerShape(20.dp)
    val updated = fetchedAt?.let {
        if (System.currentTimeMillis() - it < 60_000) stringResource(R.string.just_now)
        else formatDate(Instant.ofEpochMilli(it).atOffset(ZoneOffset.UTC).toString())
    }
    Column(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .softCard(shape)
            .background(Brush.linearGradient(sky))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Cloud, contentDescription = null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(5.dp))
            Text(stringResource(R.string.weather_local), color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp, modifier = Modifier.weight(1f))
            if (updated != null) {
                Row(
                    Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.2f)).padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(Color.White))
                    Spacer(Modifier.width(5.dp))
                    Text(stringResource(R.string.weather_updated, updated), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
        Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${w.temp}°", color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Bold, lineHeight = 48.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(Weather.condition(w.code, english), color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                }
                Text(
                    stringResource(R.string.weather_feels, w.feelsLike) + "  ·  " + stringResource(R.string.weather_wind, w.wind),
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp
                )
                Text(stringResource(R.string.weather_humidity, w.humidity), color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
            }
            Box(
                Modifier.size(64.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(Weather.icon(w.code, w.isDay), fontSize = 34.sp)
            }
        }
        Row(Modifier.padding(top = 14.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            w.days.forEachIndexed { i, d ->
                val name = when (i) {
                    0 -> stringResource(R.string.weather_today)
                    1 -> stringResource(R.string.weather_tomorrow)
                    else -> runCatching {
                        LocalDate.parse(d.date).dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, locale)
                    }.getOrDefault(d.date)
                }
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.18f))
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text(Weather.icon(d.code), fontSize = 20.sp, modifier = Modifier.padding(vertical = 2.dp))
                    Text("${d.max}° / ${d.min}°", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    if (d.rainChance > 0) Text("☔ ${d.rainChance}%", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun CityPicker(english: Boolean, selected: City?, onClose: (() -> Unit)?, onPick: (City) -> Unit) {
    var query by remember { mutableStateOf("") }
    var choice by remember { mutableStateOf(selected) }
    val shown = Cities.all.filter {
        query.isBlank() || it.hi.contains(query.trim()) || it.en.contains(query.trim(), ignoreCase = true)
    }
    val fullScreen = onClose == null
    Column(
        (if (fullScreen) Modifier.fillMaxSize() else Modifier.fillMaxWidth())
            .background(CardWhite)
            .navigationBarsPadding()
    ) {
        Column(Modifier.padding(horizontal = 20.dp).padding(top = if (onClose == null) 20.dp else 0.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.city_pick_title), color = Ink, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    Cities.district.label(english),
                    color = RedInk,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer).padding(horizontal = 8.dp, vertical = 2.dp)
                )
                Spacer(Modifier.weight(1f))
                if (onClose != null) {
                    Box(
                        Modifier.size(32.dp).clip(CircleShape).background(ChipBg).clickable(onClick = onClose),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Ink, modifier = Modifier.size(18.dp))
                    }
                }
            }
            Text(stringResource(R.string.city_pick_body), color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp, bottom = 14.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(ChipBg).padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = Muted, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text(stringResource(R.string.city_search), color = Muted, fontSize = 14.sp)
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(color = Ink, fontSize = 14.sp),
                        cursorBrush = SolidColor(BrandRed),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 10.dp)) {
                Text(stringResource(R.string.city_major), color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(stringResource(R.string.city_count, shown.size), color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = if (fullScreen) Modifier.weight(1f) else Modifier.height(420.dp),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(shown, key = { it.id }) { c ->
                CityTile(c, english, active = c.id == choice?.id) { choice = c }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .shadow(8.dp)
                .background(CardWhite)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.city_selected), color = Muted, fontSize = 11.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = RedInk, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(choice?.label(english) ?: "—", color = Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
            val enabled = choice != null
            Row(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (enabled) Color(0xFFA5000E) else ChipBg)
                    .clickable(enabled = enabled) { choice?.let(onPick) }
                    .padding(horizontal = 22.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.city_confirm), color = if (enabled) Color.White else Muted, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = if (enabled) Color.White else Muted, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun CityTile(city: City, english: Boolean, active: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Box {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(if (active) MaterialTheme.colorScheme.secondaryContainer else ChipBg.copy(alpha = 0.6f))
                .then(if (active) Modifier.border(BorderStroke(2.dp, RedInk), shape) else Modifier)
                .clickable(onClick = onClick)
                .padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier.size(34.dp).clip(CircleShape).background(if (active) CardWhite else ChipBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (active) Icons.Default.LocationOn else Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = if (active) RedInk else Muted,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                city.label(english),
                color = if (active) RedInk else Ink,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                Cities.districtOf(city, english),
                color = if (active) RedInk else Muted,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (active) {
            Box(
                Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp).size(20.dp).clip(CircleShape).background(RedInk),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
            }
        }
    }
}

@Composable
private fun LocalLead(post: WpPost, label: String, category: String?, onClick: () -> Unit) {
    val context = LocalContext.current
    val saved = Library.isSaved(post.id)
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .softCard()
            .background(CardWhite)
            .clickable(onClick = onClick)
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 10f).background(ChipBg)) {
            NewsImage(post.imageUrl, post.headline, Modifier.fillMaxSize(), widthPx = 1000, heightPx = 625)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.35f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.85f))))
            Row(
                Modifier.align(Alignment.TopStart).padding(12.dp).clip(CircleShape).background(BrandRed).padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(3.dp))
                Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                post.headline,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                lineHeight = 26.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
            )
        }
        Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
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
                    modifier = Modifier.weight(1f, fill = false).clip(RoundedCornerShape(4.dp)).background(ChipBg).padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Spacer(Modifier.weight(1f))
            Icon(
                if (saved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                contentDescription = stringResource(if (saved) R.string.unsave else R.string.save),
                tint = if (saved) RedInk else Muted,
                modifier = Modifier.size(36.dp).clip(CircleShape).clickable { Library.toggleSaved(post) }.padding(8.dp)
            )
            Icon(
                Icons.Default.Share,
                contentDescription = stringResource(R.string.share),
                tint = Muted,
                modifier = Modifier.size(36.dp).clip(CircleShape).clickable { shareText(context, post.headline, post.link, whatsapp = false) }.padding(8.dp)
            )
        }
    }
}

@Composable
private fun LocalCard(post: WpPost, area: String?, category: String?, onClick: () -> Unit) {
    val context = LocalContext.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .softCard()
            .background(CardWhite)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            val pill = area ?: category
            val extra = if (area != null) category else null
            if (pill != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        Modifier.clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.secondaryContainer).padding(horizontal = 7.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (area != null) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = RedInk, modifier = Modifier.size(11.dp))
                            Spacer(Modifier.width(2.dp))
                        }
                        Text(pill, color = RedInk, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }
                    if (extra != null) {
                        Spacer(Modifier.width(8.dp))
                        Text(extra, color = Muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(Modifier.height(6.dp))
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
                Text(formatDate(post.date), color = Muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Icon(
                    Icons.Default.Share,
                    contentDescription = stringResource(R.string.share),
                    tint = Muted,
                    modifier = Modifier.size(30.dp).clip(CircleShape).clickable { shareText(context, post.headline, post.link, whatsapp = false) }.padding(6.dp)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        NewsImage(
            url = post.imageUrl,
            description = null,
            modifier = Modifier.size(88.dp).clip(RoundedCornerShape(12.dp)).background(ChipBg),
            widthPx = 264,
            heightPx = 264
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CityFeed(
    categoryId: Int,
    label: String,
    english: Boolean,
    onOpen: (Int) -> Unit,
    headerReady: Boolean,
    header: @Composable () -> Unit
) {
    var posts by remember(categoryId) { mutableStateOf<List<WpPost>>(emptyList()) }
    var loading by remember(categoryId) { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var error by remember(categoryId) { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    var page by remember(categoryId) { mutableIntStateOf(1) }
    var hasMore by remember(categoryId) { mutableStateOf(true) }
    var loadingMore by remember(categoryId) { mutableStateOf(false) }
    val list = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val categoryNames = rememberCategoryNames(english)

    suspend fun shown(items: List<WpPost>) =
        if (english) withContext(Dispatchers.IO) { NewsTranslator.postsTitles(items) } else items

    LaunchedEffect(categoryId, retry) {
        loading = posts.isEmpty()
        error = null
        try {
            val loaded = withContext(Dispatchers.IO) { Repository.byCategory(categoryId) }
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

    LaunchedEffect(headerReady) {
        if (headerReady && list.firstVisibleItemIndex <= 2) list.scrollToItem(0)
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
                val next = withContext(Dispatchers.IO) { Repository.byCategory(categoryId, page + 1) }
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

    fun area(post: WpPost) = post.city()?.takeIf { it.id != categoryId }?.label(english)

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = { refreshing = true; retry++ },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(Modifier.fillMaxSize(), state = list, contentPadding = PaddingValues(bottom = 24.dp)) {
            item(key = "offline") { OfflineBanner() }
            item(key = "header") { header() }
            item(key = "title") { BarHeader(stringResource(R.string.city_latest, label)) }
            when {
                loading -> item(key = "skeleton") { NewsSkeleton(showHero = true) }
                error != null && posts.isEmpty() -> item(key = "error") {
                    Box(Modifier.fillMaxWidth().height(320.dp)) { ErrorState(error!!, onRetry = { retry++ }) }
                }
                posts.isEmpty() -> item(key = "empty") {
                    Text(stringResource(R.string.category_empty), color = Muted, modifier = Modifier.padding(24.dp))
                }
                else -> {
                    posts.first().let { lead ->
                        item(key = "lead-${lead.id}") {
                            LocalLead(lead, area(lead) ?: label, lead.categoryTag(categoryNames)) { onOpen(lead.id) }
                        }
                    }
                    item(key = "gap") { Spacer(Modifier.height(6.dp)) }
                    items(posts.drop(1).distinctBy { it.id }, key = { "local-${it.id}" }) {
                        LocalCard(it, area(it), it.categoryTag(categoryNames)) { onOpen(it.id) }
                    }
                    if (loadingMore) item(key = "more") { LoadingDots() }
                }
            }
        }
    }
}
