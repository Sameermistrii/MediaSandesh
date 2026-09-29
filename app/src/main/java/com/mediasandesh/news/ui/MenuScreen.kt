package com.mediasandesh.news.ui

import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediasandesh.news.AppPrefs
import com.mediasandesh.news.LocalePrefs
import com.mediasandesh.news.R
import com.mediasandesh.news.data.Cities
import com.mediasandesh.news.data.NewsTranslator
import com.mediasandesh.news.data.Repository
import com.mediasandesh.news.data.WpCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal const val DESK_PHONE = "+919411340800"
internal const val DESK_WHATSAPP_NUMBER = "919411340800"
internal const val DESK_MAIL = "mediasandesh4@gmail.com"

internal val WhatsAppBrand = Color(0xFF25D366)
private val MenuCard = RoundedCornerShape(16.dp)
private val MenuShadow = Color(0x14000000)
private const val MENU_TOPICS = 6

internal fun Modifier.menuCard(shape: RoundedCornerShape = MenuCard) = this
    .shadow(2.dp, shape, clip = false, ambientColor = MenuShadow, spotColor = MenuShadow)
    .clip(shape)

internal enum class Social(@DrawableRes val icon: Int, val label: String, val url: String, val color: Color) {
    YOUTUBE(R.drawable.ic_youtube, "YouTube", "https://www.youtube.com/@mediasandeshuk5605", Color(0xFFFF0000)),
    FACEBOOK(R.drawable.ic_facebook, "Facebook", "https://www.facebook.com/profile.php?id=100022282011682", Color(0xFF1877F2)),
    INSTAGRAM(R.drawable.ic_instagram, "Instagram", "https://www.instagram.com/mediasandesh/", Color(0xFFE1306C)),
    X(R.drawable.ic_x, "X", "https://twitter.com/mediasandesh", Color(0xFF111111)),
    WHATSAPP(R.drawable.ic_whatsapp, "WhatsApp", "https://wa.me/$DESK_WHATSAPP_NUMBER", WhatsAppBrand)
}

internal fun appVersion(context: android.content.Context): String =
    runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()

internal fun openWhatsApp(context: android.content.Context, message: String) =
    openUrl(context, "https://wa.me/$DESK_WHATSAPP_NUMBER?text=${Uri.encode(message)}")

@Composable
internal fun BrandMark(size: Dp) {
    Image(
        painterResource(R.mipmap.ic_launcher_foreground),
        contentDescription = null,
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(colorResource(R.color.ic_launcher_background))
            .graphicsLayer { scaleX = 1.45f; scaleY = 1.45f }
    )
}

@Composable
internal fun SocialRow(size: Dp = 44.dp) {
    val context = LocalContext.current
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Social.entries.forEach { s ->
            Box(
                Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(
                        if (s == Social.INSTAGRAM) Brush.linearGradient(listOf(Color(0xFFFD1D1D), Color(0xFFE1306C), Color(0xFF833AB4)))
                        else Brush.linearGradient(listOf(s.color, s.color))
                    )
                    .clickable { openUrl(context, s.url) },
                contentAlignment = Alignment.Center
            ) {
                Icon(painterResource(s.icon), contentDescription = s.label, tint = Color.White, modifier = Modifier.size(size * 0.46f))
            }
        }
    }
}

@Composable
internal fun SectionLabel(text: String) {
    Row(Modifier.padding(start = 4.dp, top = 24.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(4.dp).height(16.dp).clip(RoundedCornerShape(2.dp)).background(BrandRed))
        Spacer(Modifier.width(8.dp))
        Text(text, color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MenuScreen(
    onClose: () -> Unit,
    onTab: (String) -> Unit,
    onRoute: (String) -> Unit,
    onCategory: (Int, String) -> Unit
) {
    val context = LocalContext.current
    val english = LocalePrefs.get(context) == "en"
    var topics by remember { mutableStateOf<List<WpCategory>>(emptyList()) }
    LaunchedEffect(Unit) {
        topics = withContext(Dispatchers.IO) { runCatching { Repository.trending() }.getOrDefault(emptyList()) }.take(MENU_TOPICS)
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(Paper)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 28.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            BrandMark(44.dp)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(stringResource(R.string.brand_name), color = BrandRed, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp, maxLines = 1)
                Text(stringResource(R.string.menu_tagline), color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(ChipBg).clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close), tint = Ink, modifier = Modifier.size(22.dp))
            }
        }

        Column(
            Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
                .menuCard()
                .background(Brush.linearGradient(listOf(Color(0xFFD0121A), Color(0xFFA5020D))))
                .clickable { onRoute("submit") }
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f).padding(end = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Color(0xFFFFDAD6), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.submit_title), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }
                    Text(
                        stringResource(R.string.menu_submit_body),
                        color = Color(0xFFFFE2DF),
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                Box(Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Campaign, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
            Row(
                Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(WhatsAppBrand)
                    .clickable { onRoute("submit") }
                    .padding(vertical = 11.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(painterResource(R.drawable.ic_whatsapp), contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.menu_submit_whatsapp), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        SectionLabel(stringResource(R.string.menu_browse))
        Column(Modifier.fillMaxWidth().menuCard().background(CardWhite).padding(4.dp)) {
            MenuRow(Icons.Default.GridView, stringResource(R.string.menu_all_categories), strong = true) { onRoute("categories") }
            if (topics.isNotEmpty()) {
                FlowRow(
                    Modifier.padding(start = 56.dp, end = 8.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    topics.forEach { c ->
                        val name = if (english) NewsTranslator.categoryKnown(c.name) else c.name
                        Text(
                            name,
                            color = Ink,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(ChipBg)
                                .clickable { onCategory(c.id, name) }
                                .padding(horizontal = 11.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        SectionLabel(stringResource(R.string.menu_app))
        Column(Modifier.fillMaxWidth().menuCard().background(CardWhite).padding(4.dp)) {
            MenuRow(Icons.Default.Settings, stringResource(R.string.menu_settings), muted = true) { onRoute("settings") }
            MenuRow(Icons.Default.Info, stringResource(R.string.menu_about), muted = true) { onRoute("about") }
            MenuRow(Icons.Default.SupportAgent, stringResource(R.string.menu_contact), muted = true) { onRoute("contact") }
            MenuRow(Icons.Default.VerifiedUser, stringResource(R.string.menu_privacy), muted = true) { onRoute("privacy") }
        }

        Column(
            Modifier.padding(top = 24.dp).fillMaxWidth().menuCard().background(CardWhite).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.menu_follow), color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.height(12.dp))
            SocialRow(40.dp)
        }

        Column(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.menu_version, appVersion(context)), color = Muted, fontSize = 12.sp)
            Text(stringResource(R.string.menu_made), color = Muted.copy(alpha = 0.8f), fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun MenuRow(
    icon: ImageVector,
    label: String,
    badge: String? = null,
    strong: Boolean = false,
    muted: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(36.dp).clip(CircleShape).background(if (muted) ChipBg else RedInk.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = if (muted) Muted else RedInk, modifier = Modifier.size(20.dp))
        }
        Row(Modifier.weight(1f).padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                color = Ink,
                fontSize = 15.sp,
                fontWeight = if (strong) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (badge != null) {
                Text(
                    badge,
                    color = RedInk,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 6.dp).clip(CircleShape).background(RedInk.copy(alpha = 0.12f)).padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Muted, modifier = Modifier.size(20.dp))
    }
}
