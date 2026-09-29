package com.mediasandesh.news.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.text.format.Formatter
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.imageLoader
import com.mediasandesh.news.AppPrefs
import com.mediasandesh.news.LocalePrefs
import com.mediasandesh.news.R
import com.mediasandesh.news.data.Cities
import com.mediasandesh.news.data.Library
import com.mediasandesh.news.data.Repository
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val SITE_URL = "https://www.mediasandesh.com"
private const val MAP_URL = "https://maps.google.com/?q=Rudrapur,+Udham+Singh+Nagar,+Uttarakhand"
private const val YEARS_SERVING = 15
private const val RNI_YEAR = 2012

private val Round12 = RoundedCornerShape(12.dp)

@Composable
private fun ScreenColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Paper)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp)
            .padding(top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content
    )
}

@Composable
private fun WhiteCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().menuCard().background(CardWhite).padding(16.dp), content = content)
}

@Composable
private fun CardTitle(icon: ImageVector, title: String, trailing: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = RedInk, modifier = Modifier.size(20.dp))
        Text(title, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, modifier = Modifier.padding(start = 8.dp).weight(1f))
        trailing?.invoke()
    }
}

@Composable
private fun BarTitle(title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(5.dp).height(20.dp).clip(RoundedCornerShape(3.dp)).background(RedInk))
        Text(title, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun SmallPill(text: String, color: Color = Ink, background: Color = ChipBg) {
    Text(
        text,
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.clip(CircleShape).background(background).padding(horizontal = 9.dp, vertical = 3.dp)
    )
}

// ---------------------------------------------------------------- Settings

@Composable
fun SettingsScreen(onSaved: () -> Unit = {}) {
    val context = LocalContext.current
    val current = LocalePrefs.get(context)
    val scope = rememberCoroutineScope()
    var cacheBytes by remember { mutableLongStateOf(-1L) }
    var cleared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { cacheBytes = withContext(Dispatchers.IO) { context.cacheDir.sizeOnDisk() } }

    fun setLanguage(lang: String) {
        if (lang == current) return
        LocalePrefs.set(context, lang)
        (context as Activity).recreate()
    }

    ScreenColumn {
        WhiteCard {
            CardTitle(Icons.Default.Translate, stringResource(R.string.settings_language))
            Text(
                stringResource(R.string.settings_language_hint),
                color = Muted,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                modifier = Modifier.padding(top = 6.dp, bottom = 14.dp)
            )
            Segmented(
                options = listOf(stringResource(R.string.lang_hindi), stringResource(R.string.lang_english)),
                selected = if (current == "en") 1 else 0,
                check = true
            ) { setLanguage(if (it == 1) "en" else "hi") }
        }

        WhiteCard {
            CardTitle(Icons.Default.Palette, stringResource(R.string.settings_theme))
            Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ThemeTile(stringResource(R.string.theme_system), ThemePreview.SYSTEM, AppPrefs.theme == AppPrefs.THEME_SYSTEM) {
                    AppPrefs.setTheme(context, AppPrefs.THEME_SYSTEM)
                }
                ThemeTile(stringResource(R.string.theme_light), ThemePreview.LIGHT, AppPrefs.theme == AppPrefs.THEME_LIGHT) {
                    AppPrefs.setTheme(context, AppPrefs.THEME_LIGHT)
                }
                ThemeTile(stringResource(R.string.theme_dark), ThemePreview.DARK, AppPrefs.theme == AppPrefs.THEME_DARK) {
                    AppPrefs.setTheme(context, AppPrefs.THEME_DARK)
                }
            }
        }

        val sizes = listOf(R.string.text_small, R.string.text_normal, R.string.text_large, R.string.text_xlarge).map { stringResource(it) }
        val level = AppPrefs.textSize.coerceIn(0, 3)
        WhiteCard {
            CardTitle(Icons.Default.FormatSize, stringResource(R.string.settings_text_size)) { SmallPill(sizes[level]) }
            Spacer(Modifier.height(14.dp))
            Segmented(options = sizes, selected = level, compact = true) { AppPrefs.setTextSize(context, it) }
            Column(
                Modifier.padding(top = 14.dp).fillMaxWidth().clip(Round12).background(ChipBg.copy(alpha = 0.6f)).padding(14.dp).animateContentSize()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Visibility, contentDescription = null, tint = RedInk, modifier = Modifier.size(14.dp))
                    Text(stringResource(R.string.settings_preview_label), color = RedInk, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp))
                }
                Text(
                    stringResource(R.string.settings_preview_headline),
                    color = Ink,
                    fontWeight = FontWeight.Bold,
                    fontSize = (15 + level * 3).sp,
                    lineHeight = (22 + level * 4).sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
                Text(
                    stringResource(R.string.text_preview),
                    color = Muted,
                    fontSize = (13 + level * 2).sp,
                    lineHeight = (20 + level * 3).sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        WhiteCard {
            CardTitle(Icons.Default.Newspaper, stringResource(R.string.settings_data))
            val savedCount = Library.saved.size
            SettingRow(
                Icons.Default.Bookmark,
                stringResource(R.string.settings_saved),
                stringResource(R.string.settings_saved_hint),
                onClick = onSaved
            ) {
                SmallPill(pluralStringResource(R.plurals.settings_saved_count, savedCount, savedCount))
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Muted, modifier = Modifier.size(20.dp))
            }
            SettingRow(
                Icons.Default.CleaningServices,
                stringResource(R.string.settings_cache),
                when {
                    cacheBytes < 0 -> "…"
                    else -> stringResource(R.string.settings_cache_size, Formatter.formatShortFileSize(context, cacheBytes))
                }
            ) {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (cleared) ChipBg else RedInk.copy(alpha = 0.12f))
                        .clickable(enabled = !cleared && cacheBytes > 0) {
                            scope.launch {
                                cacheBytes = withContext(Dispatchers.IO) { clearCaches(context) }
                                cleared = true
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (cleared) Icons.Default.CheckCircle else Icons.Default.Delete,
                        contentDescription = null,
                        tint = if (cleared) Muted else RedInk,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        stringResource(if (cleared) R.string.settings_cache_done else R.string.settings_cache_clear),
                        color = if (cleared) Muted else RedInk,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }

        WhiteCard {
            CardTitle(Icons.Default.Info, stringResource(R.string.settings_about_app))
            val shareTitle = stringResource(R.string.settings_share_text)
            SettingRow(Icons.Default.Share, stringResource(R.string.settings_share_app), onClick = {
                shareText(context, shareTitle, SITE_URL, whatsapp = false)
            }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Muted, modifier = Modifier.size(20.dp))
            }
            SettingRow(Icons.Default.Terminal, stringResource(R.string.settings_version)) {
                SmallPill("v" + appVersion(context), color = Muted)
            }
        }

        Column(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Newspaper, contentDescription = null, tint = RedInk, modifier = Modifier.size(16.dp))
                Text(
                    stringResource(R.string.settings_footer_title),
                    color = RedInk,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
            Text(
                stringResource(R.string.settings_footer_body),
                color = Muted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

private fun File.sizeOnDisk(): Long = walkTopDown().filter { it.isFile }.sumOf { it.length() }

private fun clearCaches(context: android.content.Context): Long {
    runCatching { Repository.http.cache?.evictAll() }
    runCatching { context.imageLoader.diskCache?.clear() }
    runCatching { context.imageLoader.memoryCache?.clear() }
    listOf("video_pages", "submit").forEach { runCatching { File(context.cacheDir, it).deleteRecursively() } }
    return context.cacheDir.sizeOnDisk()
}

@Composable
private fun Segmented(options: List<String>, selected: Int, check: Boolean = false, compact: Boolean = false, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(ChipBg).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Row(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (on) RedInk else Color.Transparent)
                    .clickable { onSelect(i) }
                    .padding(vertical = if (compact) 7.dp else 9.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (check && on) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    label,
                    color = if (on) Color.White else Muted,
                    fontSize = if (compact) 12.sp else 14.sp,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}

private enum class ThemePreview { SYSTEM, LIGHT, DARK }

@Composable
private fun androidx.compose.foundation.layout.RowScope.ThemeTile(label: String, preview: ThemePreview, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier
            .weight(1f)
            .clip(Round12)
            .background(ChipBg.copy(alpha = 0.55f))
            .then(if (selected) Modifier.border(1.5.dp, RedInk, Round12) else Modifier)
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        val (bg, accent, line) = when (preview) {
            ThemePreview.SYSTEM -> Triple(Color(0xFFDFE3E8), Color(0xFF5F5E5E), Color(0xFF5D3F3C))
            ThemePreview.LIGHT -> Triple(Color.White, BrandRed, Color(0xFF181C20))
            ThemePreview.DARK -> Triple(Color(0xFF2D3135), Color(0xFFFFB4AB), Color(0xFFDFE3E8))
        }
        Box(Modifier.fillMaxWidth().aspectRatio(4f / 3f).clip(RoundedCornerShape(6.dp))) {
            if (preview == ThemePreview.SYSTEM) {
                Row(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f).fillMaxHeight().background(Color.White))
                    Box(Modifier.weight(1f).fillMaxHeight().background(Color(0xFF2D3135)))
                }
            } else {
                Box(Modifier.fillMaxSize().background(bg))
            }
            Column(Modifier.fillMaxSize().padding(6.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Box(Modifier.width(10.dp).height(4.dp).clip(CircleShape).background(accent))
                    Box(Modifier.size(6.dp).clip(CircleShape).background(accent))
                }
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Box(Modifier.fillMaxWidth(0.8f).height(5.dp).clip(CircleShape).background(line.copy(alpha = 0.4f)))
                    Box(Modifier.fillMaxWidth(0.55f).height(4.dp).clip(CircleShape).background(line.copy(alpha = 0.22f)))
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                color = if (selected) RedInk else Ink,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            if (selected) Icon(Icons.Default.CheckCircle, contentDescription = null, tint = RedInk, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit = {}
) {
    Row(
        Modifier
            .padding(top = 10.dp)
            .fillMaxWidth()
            .clip(Round12)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(ChipBg), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = RedInk, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(title, color = Ink, fontSize = 15.sp)
            if (subtitle != null) Text(subtitle, color = Muted, fontSize = 12.sp, lineHeight = 17.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) { trailing() }
    }
}

// ---------------------------------------------------------------- About

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AboutScreen(onHome: () -> Unit = {}, onCategory: (Int, String) -> Unit = { _, _ -> }) {
    val context = LocalContext.current
    val english = LocalePrefs.get(context) == "en"
    ScreenColumn {
        WhiteCard {
            Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(84.dp).clip(CircleShape).background(RedInk.copy(alpha = 0.08f)), contentAlignment = Alignment.Center) {
                    BrandMark(64.dp)
                }
                Text(
                    stringResource(R.string.brand_name),
                    color = Ink,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    modifier = Modifier.padding(top = 14.dp)
                )
                Text(
                    stringResource(R.string.about_title),
                    color = Muted,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Row(
                    Modifier
                        .padding(top = 14.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFDAD5))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🏅", fontSize = 13.sp)
                    Text(
                        stringResource(R.string.about_badge, YEARS_SERVING),
                        color = Color(0xFF410002),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }
        }

        Column(Modifier.fillMaxWidth().menuCard().background(CardWhite).background(ChipBg.copy(alpha = 0.6f)).padding(18.dp)) {
            BarTitle(stringResource(R.string.about_intro_title))
            Text(
                stringResource(R.string.about_body),
                color = Ink,
                fontSize = 15.sp,
                lineHeight = 24.sp,
                modifier = Modifier.padding(top = 10.dp)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("$YEARS_SERVING+", stringResource(R.string.about_stat_years))
            StatTile("${Cities.all.size}", stringResource(R.string.about_stat_cities))
            StatTile("$RNI_YEAR", stringResource(R.string.about_stat_rni))
        }

        WhiteCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { BarTitle(stringResource(R.string.about_editor_title)) }
                SmallPill(stringResource(R.string.about_editor_badge), color = Muted)
            }
            Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box {
                    Box(
                        Modifier.size(60.dp).clip(CircleShape).background(Brush.linearGradient(listOf(BrandRed, Color(0xFFA5020D)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(stringResource(R.string.about_editor_name).take(1), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = RedInk,
                        modifier = Modifier.align(Alignment.BottomEnd).size(20.dp).clip(CircleShape).background(CardWhite).padding(1.dp)
                    )
                }
                Column(Modifier.padding(start = 14.dp)) {
                    Text(stringResource(R.string.about_editor_name), color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text(stringResource(R.string.about_editor_role), color = RedInk, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Text(stringResource(R.string.about_editor_place), color = Muted, fontSize = 12.sp)
                }
            }
        }

        WhiteCard {
            BarTitle(stringResource(R.string.about_coverage))
            FlowRow(
                Modifier.padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Cities.all.forEach { city ->
                    Row(
                        Modifier
                            .clip(CircleShape)
                            .background(ChipBg)
                            .clickable { onCategory(city.id, city.label(english)) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = RedInk, modifier = Modifier.size(14.dp))
                        Text(city.label(english), color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 3.dp))
                    }
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().menuCard().background(ChipBg).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(CardWhite), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = RedInk, modifier = Modifier.size(24.dp))
            }
            Column(Modifier.padding(start = 12.dp)) {
                Text(stringResource(R.string.about_rni_title), color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(stringResource(R.string.about_rni_body), color = Muted, fontSize = 12.sp, lineHeight = 17.sp)
            }
        }

        Row(
            Modifier.fillMaxWidth().height(50.dp).clip(Round12).background(RedInk).clickable(onClick = onHome),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Newspaper, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            Text(stringResource(R.string.about_read_latest), color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.StatTile(value: String, label: String) {
    Column(
        Modifier.weight(1f).menuCard().background(CardWhite).padding(vertical = 14.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = RedInk, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
        Text(label, color = Muted, fontSize = 11.sp, textAlign = TextAlign.Center, lineHeight = 15.sp, modifier = Modifier.padding(top = 2.dp))
    }
}

// ---------------------------------------------------------------- Contact

@Composable
fun ContactScreen() {
    val context = LocalContext.current
    val whatsappHello = stringResource(R.string.contact_whatsapp_message)
    val adsHello = stringResource(R.string.contact_ads_message)
    ScreenColumn {
        Row(
            Modifier.fillMaxWidth().menuCard().background(CardWhite).padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(9.dp).clip(CircleShape).background(RedInk))
            Text(
                stringResource(R.string.contact_newsroom),
                color = Ink,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 8.dp).weight(1f)
            )
            SmallPill(stringResource(R.string.contact_editor_short), color = Muted)
        }

        ContactCard(
            iconContent = { Icon(painterResource(R.drawable.ic_whatsapp), contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp)) },
            iconBackground = WhatsAppBrand,
            title = stringResource(R.string.contact_whatsapp_title),
            tag = stringResource(R.string.contact_whatsapp_tag),
            body = stringResource(R.string.contact_whatsapp_body),
            detail = "+91 94113 40800",
            cta = stringResource(R.string.contact_whatsapp_cta),
            ctaIcon = Icons.AutoMirrored.Filled.ArrowForward
        ) { openWhatsApp(context, whatsappHello) }

        ContactCard(
            iconContent = { Icon(Icons.Default.Call, contentDescription = null, tint = RedInk, modifier = Modifier.size(26.dp)) },
            iconBackground = RedInk.copy(alpha = 0.12f),
            title = stringResource(R.string.contact_call_title),
            tag = stringResource(R.string.contact_call_tag),
            body = stringResource(R.string.contact_call_body),
            detail = "+91 94113 40800",
            cta = stringResource(R.string.contact_call_cta),
            ctaIcon = Icons.Default.PhoneInTalk
        ) { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$DESK_PHONE"))) }

        ContactCard(
            iconContent = { Icon(Icons.Default.Email, contentDescription = null, tint = Muted, modifier = Modifier.size(26.dp)) },
            iconBackground = ChipBg,
            title = stringResource(R.string.contact_email_title),
            tag = null,
            body = stringResource(R.string.contact_email_body),
            detail = DESK_MAIL,
            cta = stringResource(R.string.contact_email_cta),
            ctaIcon = Icons.AutoMirrored.Filled.Send
        ) { runCatching { context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$DESK_MAIL"))) } }

        WhiteCard {
            CardTitle(Icons.Default.LocationCity, stringResource(R.string.contact_office)) {
                SmallPill(stringResource(R.string.contact_office_tag), color = Muted)
            }
            Box(
                Modifier
                    .padding(top = 14.dp)
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(Round12)
                    .background(Brush.linearGradient(listOf(Color(0xFFE8EEF5), Color(0xFFDDE7DD))))
                    .clickable { openUrl(context, MAP_URL) },
                contentAlignment = Alignment.Center
            ) {
                MapGrid()
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(42.dp).clip(CircleShape).background(BrandRed), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Text(
                        stringResource(R.string.contact_map_label),
                        color = Color(0xFF181C20),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.95f)).padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }
            Row(Modifier.padding(top = 14.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = RedInk, modifier = Modifier.size(20.dp))
                Text(stringResource(R.string.contact_address), color = Ink, fontSize = 15.sp, lineHeight = 22.sp, modifier = Modifier.padding(start = 6.dp))
            }
            Row(
                Modifier
                    .padding(top = 14.dp)
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(ChipBg)
                    .clickable { openUrl(context, MAP_URL) }
                    .padding(vertical = 11.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Directions, contentDescription = null, tint = RedInk, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.contact_directions), color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 6.dp))
            }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .menuCard()
                .background(Brush.linearGradient(listOf(Color(0xFFA5000E), Color(0xFFC92623), Color(0xFFD0121A))))
                .padding(16.dp)
        ) {
            Row(
                Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.2f)).padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFFFFDAD5), modifier = Modifier.size(15.dp))
                Text(stringResource(R.string.contact_ads_badge), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 5.dp))
            }
            Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Campaign, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                Text(stringResource(R.string.contact_ads_title), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp))
            }
            Text(
                stringResource(R.string.contact_ads_body),
                color = Color(0xFFFFE2DF),
                fontSize = 13.sp,
                lineHeight = 19.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
            Column(
                Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color.Black.copy(alpha = 0.2f)).padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AdLine(Icons.Default.Email, DESK_MAIL)
                AdLine(Icons.Default.Call, "+91 94113 40800")
            }
            Row(
                Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth()
                    .clip(Round12)
                    .background(Color.White)
                    .clickable { openWhatsApp(context, adsHello) }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.contact_ads_cta), color = BrandRed, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = BrandRed, modifier = Modifier.padding(start = 4.dp).size(18.dp))
            }
        }

        WhiteCard {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.contact_follow), color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text(stringResource(R.string.contact_follow_body), color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(14.dp))
                SocialRow(44.dp)
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Muted, modifier = Modifier.size(16.dp))
            Text(stringResource(R.string.contact_footer), color = Muted, fontSize = 11.sp, modifier = Modifier.padding(start = 6.dp))
        }
    }
}

@Composable
private fun MapGrid() {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceEvenly) {
        repeat(4) { Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.7f))) }
    }
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly) {
        repeat(6) { Box(Modifier.fillMaxHeight().width(1.dp).background(Color.White.copy(alpha = 0.7f))) }
    }
}

@Composable
private fun AdLine(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Color(0xFFFFDAD5), modifier = Modifier.size(16.dp))
        Text(text, color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(start = 6.dp))
    }
}

@Composable
private fun ContactCard(
    iconContent: @Composable () -> Unit,
    iconBackground: Color,
    title: String,
    tag: String?,
    body: String,
    detail: String,
    cta: String,
    ctaIcon: ImageVector,
    onClick: () -> Unit
) {
    Column(Modifier.fillMaxWidth().menuCard().background(CardWhite).clickable(onClick = onClick).padding(16.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Box(Modifier.size(48.dp).clip(Round12).background(iconBackground), contentAlignment = Alignment.Center) { iconContent() }
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    if (tag != null) {
                        Spacer(Modifier.width(6.dp))
                        SmallPill(tag, color = RedInk, background = RedInk.copy(alpha = 0.1f))
                    }
                }
                Text(body, color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                Text(detail, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.4.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            Text(cta, color = RedInk, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Icon(ctaIcon, contentDescription = null, tint = RedInk, modifier = Modifier.padding(start = 4.dp).size(16.dp))
        }
    }
}

// ---------------------------------------------------------------- Privacy

private class PolicySection(val title: Int, val points: List<Pair<ImageVector, Int>>, val highlight: Int? = null)

private val policy = listOf(
    PolicySection(
        R.string.privacy_s1_title,
        listOf(
            Icons.Default.VerifiedUser to R.string.privacy_s1_p1,
            Icons.Default.LocationOn to R.string.privacy_s1_p2,
            Icons.Default.Campaign to R.string.privacy_s1_p3
        )
    ),
    PolicySection(
        R.string.privacy_s2_title,
        listOf(
            Icons.Default.Newspaper to R.string.privacy_s2_p1,
            Icons.Default.Translate to R.string.privacy_s2_p2,
            Icons.Default.PhoneInTalk to R.string.privacy_s2_p3
        ),
        highlight = R.string.privacy_s2_p4
    ),
    PolicySection(R.string.privacy_s3_title, listOf(Icons.Default.Bookmark to R.string.privacy_s3_p1)),
    PolicySection(
        R.string.privacy_s4_title,
        listOf(Icons.Default.Delete to R.string.privacy_s4_p1, Icons.Default.CleaningServices to R.string.privacy_s4_p2)
    ),
    PolicySection(R.string.privacy_s5_title, listOf(Icons.Default.Email to R.string.privacy_s5_p1))
)

@Composable
fun PrivacyScreen() {
    val context = LocalContext.current
    var open by remember { mutableIntStateOf(0) }
    ScreenColumn {
        WhiteCard {
            Box(Modifier.size(48.dp).clip(CircleShape).background(RedInk.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = RedInk, modifier = Modifier.size(26.dp))
            }
            Text(
                stringResource(R.string.privacy_heading),
                color = Ink,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                lineHeight = 30.sp,
                modifier = Modifier.padding(top = 12.dp)
            )
            Text(stringResource(R.string.privacy_subheading), color = Muted, fontSize = 15.sp, modifier = Modifier.padding(top = 4.dp))
            Row(
                Modifier.padding(top = 12.dp).clip(CircleShape).background(ChipBg).padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = RedInk, modifier = Modifier.size(14.dp))
                Text(stringResource(R.string.privacy_updated), color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 5.dp))
            }
        }

        WhiteCard {
            Text(stringResource(R.string.privacy_body), color = Ink, fontSize = 16.sp, lineHeight = 26.sp)
        }

        policy.forEachIndexed { index, section ->
            val expanded = open == index
            Column(Modifier.fillMaxWidth().menuCard().background(CardWhite).animateContentSize()) {
                Row(
                    Modifier.fillMaxWidth().clickable { open = if (expanded) -1 else index }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.width(5.dp).height(22.dp).clip(RoundedCornerShape(3.dp)).background(RedInk))
                    Text(
                        "${index + 1}. " + stringResource(section.title),
                        color = Ink,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                        modifier = Modifier.padding(horizontal = 10.dp).weight(1f)
                    )
                    Icon(
                        Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = Muted,
                        modifier = Modifier.size(22.dp).rotate(if (expanded) 180f else 0f)
                    )
                }
                if (expanded) {
                    Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        section.points.forEach { (icon, text) -> PolicyPoint(icon, stringResource(text)) }
                        section.highlight?.let { PolicyPoint(Icons.Default.CheckCircle, stringResource(it), strong = true) }
                    }
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().menuCard().background(ChipBg).padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = RedInk, modifier = Modifier.size(22.dp))
            Column(Modifier.padding(start = 10.dp)) {
                Text(stringResource(R.string.privacy_law_title), color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(stringResource(R.string.privacy_law_body), color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
            }
        }

        Row(
            Modifier.fillMaxWidth().menuCard().background(CardWhite).padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.HelpOutline, contentDescription = null, tint = Muted, modifier = Modifier.size(20.dp))
            Text(
                stringResource(R.string.privacy_question),
                color = Muted,
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 8.dp).weight(1f)
            )
            Row(
                Modifier
                    .clip(CircleShape)
                    .background(RedInk)
                    .clickable { runCatching { context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$DESK_MAIL"))) } }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Email, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                Text(stringResource(R.string.menu_contact), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 5.dp))
            }
        }
    }
}

@Composable
private fun PolicyPoint(icon: ImageVector, text: String, strong: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(ChipBg.copy(alpha = 0.6f)).padding(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, contentDescription = null, tint = RedInk, modifier = Modifier.padding(top = 2.dp).size(20.dp))
        val label = text.substringBefore(':', "")
        val styled = buildAnnotatedString {
            if (label.isNotEmpty() && label.length < 40) {
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append("$label:") }
                append(text.substringAfter(':'))
            } else append(text)
        }
        Text(
            styled,
            color = if (strong) RedInk else Ink,
            fontSize = 14.sp,
            lineHeight = 22.sp,
            fontWeight = if (strong) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(start = 10.dp)
        )
    }
}
