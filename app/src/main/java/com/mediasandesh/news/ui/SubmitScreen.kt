package com.mediasandesh.news.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.mediasandesh.news.AppPrefs
import com.mediasandesh.news.LocalePrefs
import com.mediasandesh.news.R
import com.mediasandesh.news.data.Cities
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MAX_MEDIA = 5
private const val MAX_HEADLINE = 80
private const val MEDIA_COLUMNS = 4
private const val QUICK_CITIES = 9

private val Card16 = RoundedCornerShape(16.dp)
private val Field12 = RoundedCornerShape(12.dp)
private val Emerald = Color(0xFF059669)

private val categories = listOf(
    R.string.submit_cat_accident,
    R.string.submit_cat_crime,
    R.string.submit_cat_politics,
    R.string.submit_cat_problem,
    R.string.submit_cat_event,
    R.string.submit_cat_weather,
    R.string.submit_cat_other
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubmitScreen(onHome: () -> Unit = {}) {
    val context = LocalContext.current
    val english = LocalePrefs.get(context) == "en"
    val scope = rememberCoroutineScope()
    val defaultPlace = Cities.find(AppPrefs.cityId)?.label(english) ?: ""

    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var place by rememberSaveable { mutableStateOf(defaultPlace) }
    var headline by rememberSaveable { mutableStateOf("") }
    var details by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<Int?>(null) }
    var confirmed by rememberSaveable { mutableStateOf(false) }
    var media by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var preparing by remember { mutableStateOf(false) }
    var showErrors by remember { mutableStateOf(false) }
    var sentVia by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingVia by rememberSaveable { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(MAX_MEDIA)
    ) { picked -> media = (media + picked).distinct().take(MAX_MEDIA) }
    val sender = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        sentVia = pendingVia
    }

    val valid = headline.isNotBlank() && details.isNotBlank() && place.isNotBlank() && confirmed

    fun message() = buildString {
        appendLine("📰 ${context.getString(R.string.submit_title)} — Media Sandesh App")
        appendLine()
        appendLine("${context.getString(R.string.submit_headline)} ${headline.trim()}")
        appendLine("${context.getString(R.string.submit_place)}: ${place.trim()}")
        category?.let { appendLine("${context.getString(R.string.submit_category)}: ${context.getString(it)}") }
        appendLine()
        appendLine(details.trim())
        appendLine()
        append("${context.getString(R.string.submit_from)}: ${name.trim().ifBlank { "-" }}")
        if (phone.isNotBlank()) append(", +91 ${phone.trim()}")
    }

    fun send(whatsapp: Boolean) {
        if (!valid) {
            showErrors = true
            Toast.makeText(context, context.getString(R.string.submit_fill), Toast.LENGTH_SHORT).show()
            return
        }
        preparing = true
        scope.launch {
            val files = withContext(Dispatchers.IO) { shareableCopies(context, media) }
            preparing = false
            pendingVia = if (whatsapp) "whatsapp" else "email"
            if (whatsapp) sendWhatsApp(context, sender, message(), files)
            else sendEmail(context, sender, headline.trim(), message(), files)
        }
    }

    fun reset() {
        headline = ""
        details = ""
        place = defaultPlace
        category = null
        confirmed = false
        media = emptyList()
        showErrors = false
        sentVia = null
    }

    val via = sentVia
    if (via != null) {
        SubmitDone(
            headline = headline.trim(),
            place = place.trim(),
            files = media.size,
            sender = name.trim(),
            phone = phone.trim(),
            whatsapp = via == "whatsapp",
            onResend = { send(via == "whatsapp") },
            onAnother = ::reset,
            onHome = onHome
        )
        return
    }

    Column(Modifier.fillMaxSize().background(Paper)) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SubmitHero()
            SectionHeader(stringResource(R.string.submit_section))

            FormCard {
                FieldLabel(stringResource(R.string.submit_headline), required = true) {
                    Text(stringResource(R.string.submit_chars, headline.length, MAX_HEADLINE), color = Muted, fontSize = 11.sp)
                }
                FilledField(
                    value = headline,
                    onChange = { headline = it.take(MAX_HEADLINE) },
                    hint = stringResource(R.string.submit_headline_hint),
                    error = showErrors && headline.isBlank()
                )
                if (showErrors && headline.isBlank()) RequiredNote()
                else Tip(Icons.Default.CheckCircle, stringResource(R.string.submit_headline_tip), RedInk)
            }

            FormCard {
                FieldLabel(stringResource(R.string.submit_place), required = true, icon = Icons.Default.LocationOn)
                FilledField(
                    value = place,
                    onChange = { place = it },
                    hint = stringResource(R.string.submit_place_hint),
                    error = showErrors && place.isBlank()
                )
                if (showErrors && place.isBlank()) RequiredNote()
                Text(
                    stringResource(R.string.submit_quick_pick),
                    color = Muted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                )
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Cities.all.take(QUICK_CITIES).forEach { city ->
                        val label = city.label(english)
                        Chip(label, selected = place == label, icon = Icons.Default.LocationOn) { place = label }
                    }
                }
            }

            FormCard {
                FieldLabel(stringResource(R.string.submit_category)) {
                    Text(stringResource(R.string.submit_optional), color = Muted, fontSize = 11.sp)
                }
                CategoryChips(category) { category = if (category == it) null else it }
            }

            FormCard {
                FieldLabel(stringResource(R.string.submit_details), required = true)
                Text(
                    stringResource(R.string.submit_details_hint),
                    color = Muted,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                FilledField(
                    value = details,
                    onChange = { details = it },
                    hint = "",
                    error = showErrors && details.isBlank(),
                    minHeight = 130
                )
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { if (showErrors && details.isBlank()) RequiredNote() }
                    val words = details.trim().split(Regex("\\s+")).count { it.isNotEmpty() }
                    Text(pluralStringResource(R.plurals.submit_words, words, words), color = Muted, fontSize = 11.sp)
                }
            }

            FormCard {
                FieldLabel(stringResource(R.string.submit_media), icon = Icons.Default.PermMedia) {
                    Text(
                        "${media.size}/$MAX_MEDIA",
                        color = RedInk,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(CircleShape).background(RedInk.copy(alpha = 0.12f)).padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                MediaGrid(
                    media = media,
                    onRemove = { media = media - it },
                    onAdd = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) }
                )
                Tip(Icons.Default.Info, stringResource(R.string.submit_media_tip), Muted)
            }

            FormCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Badge, contentDescription = null, tint = RedInk, modifier = Modifier.size(20.dp))
                    Text(stringResource(R.string.submit_you), color = Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 6.dp))
                }
                Text(stringResource(R.string.submit_you_hint), color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp, bottom = 12.dp))
                Text(stringResource(R.string.submit_name), color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 4.dp))
                FilledField(value = name, onChange = { name = it }, hint = stringResource(R.string.submit_name_hint), error = false)
                Text(stringResource(R.string.submit_phone), color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                FilledField(
                    value = phone,
                    onChange = { phone = it.filter { c -> c.isDigit() || c == ' ' }.take(12) },
                    hint = stringResource(R.string.submit_phone_hint),
                    error = false,
                    keyboard = KeyboardType.Phone,
                    prefix = "+91"
                )
                Row(
                    Modifier.padding(top = 10.dp).fillMaxWidth().clip(Field12).clickable { confirmed = !confirmed },
                    verticalAlignment = Alignment.Top
                ) {
                    Checkbox(
                        checked = confirmed,
                        onCheckedChange = { confirmed = it },
                        colors = CheckboxDefaults.colors(checkedColor = RedInk, uncheckedColor = if (showErrors && !confirmed) RedInk else Muted)
                    )
                    Text(
                        stringResource(R.string.submit_confirm),
                        color = if (showErrors && !confirmed) RedInk else Muted,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        modifier = Modifier.padding(top = 12.dp, end = 4.dp)
                    )
                }
            }
        }

        if (!WindowInsets.isImeVisible) {
            SubmitBar(preparing = preparing, onWhatsApp = { send(true) }, onEmail = { send(false) })
        }
    }
}

@Composable
private fun SubmitHero() {
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(4.dp, Card16, clip = false)
            .clip(Card16)
            .background(Brush.linearGradient(listOf(Color(0xFFA5000E), Color(0xFFD0121A), Color(0xFFC92623))))
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Campaign, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Column(Modifier.padding(start = 12.dp)) {
                Text(stringResource(R.string.submit_hero_title), color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.submit_hero_body), color = Color(0xFFFFE2DF), fontSize = 13.sp, lineHeight = 19.sp)
            }
        }
        Row(
            Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
                .clip(Field12)
                .background(Color.White.copy(alpha = 0.12f))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Step(stringResource(R.string.submit_step_1), stringResource(R.string.submit_step_write), Color.White, BrandRed)
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
            Step(stringResource(R.string.submit_step_2), stringResource(R.string.submit_step_media), Color.White, BrandRed)
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
            Step(stringResource(R.string.submit_step_3), stringResource(R.string.submit_step_send), WhatsAppBrand, Color.White)
        }
    }
}

@Composable
private fun Step(number: String, label: String, badge: Color, digit: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(20.dp).clip(CircleShape).background(badge), contentAlignment = Alignment.Center) {
            Text(number, color = digit, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 6.dp))
    }
}

@Composable
private fun SectionHeader(title: String) {
    Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(5.dp).size(width = 5.dp, height = 20.dp).clip(RoundedCornerShape(3.dp)).background(BrandRed))
        Text(title, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun FormCard(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().menuCard(Card16).background(CardWhite).padding(16.dp), content = content)
}

@Composable
private fun FieldLabel(label: String, required: Boolean = false, icon: ImageVector? = null, trailing: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = RedInk, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(5.dp))
        }
        Text(label, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        if (required) Text(" *", color = RedInk, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
        trailing?.invoke()
    }
}

@Composable
private fun FilledField(
    value: String,
    onChange: (String) -> Unit,
    hint: String,
    error: Boolean,
    minHeight: Int = 0,
    keyboard: KeyboardType = KeyboardType.Text,
    prefix: String? = null
) {
    val multiline = minHeight > 0
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = !multiline,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        textStyle = TextStyle(color = Ink, fontSize = 15.sp, lineHeight = 23.sp),
        cursorBrush = SolidColor(RedInk),
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = if (multiline) minHeight.dp else 48.dp)
                    .clip(Field12)
                    .background(ChipBg.copy(alpha = 0.7f))
                    .then(if (error) Modifier.border(1.5.dp, RedInk, Field12) else Modifier),
                verticalAlignment = if (multiline) Alignment.Top else Alignment.CenterVertically
            ) {
                if (prefix != null) {
                    Box(Modifier.heightIn(min = 48.dp).background(ChipBg).padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                        Text(prefix, color = Muted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Box(Modifier.weight(1f).padding(horizontal = 14.dp, vertical = 12.dp)) {
                    if (value.isEmpty() && hint.isNotEmpty()) Text(hint, color = Muted.copy(alpha = 0.8f), fontSize = 15.sp)
                    inner()
                }
            }
        }
    )
}

@Composable
private fun Tip(icon: ImageVector, text: String, color: Color) {
    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
private fun RequiredNote() = Tip(Icons.Default.Info, stringResource(R.string.submit_required), RedInk)

@Composable
private fun Chip(label: String, selected: Boolean, icon: ImageVector? = null, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(CircleShape)
            .background(if (selected) RedInk else ChipBg.copy(alpha = 0.8f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selected) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
        } else if (icon != null) {
            Icon(icon, contentDescription = null, tint = RedInk, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(3.dp))
        }
        Text(
            label,
            color = if (selected) Color.White else Ink,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryChips(selected: Int?, onSelect: (Int) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        categories.forEach { res -> Chip(stringResource(res), selected = selected == res) { onSelect(res) } }
    }
}

@Composable
private fun MediaGrid(media: List<Uri>, onRemove: (Uri) -> Unit, onAdd: () -> Unit) {
    val context = LocalContext.current
    val slots = MAX_MEDIA.coerceAtLeast(media.size + 1)
    val cells = (0 until slots).chunked(MEDIA_COLUMNS)
    Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        cells.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { i ->
                    Box(Modifier.weight(1f).aspectRatio(1f).clip(Field12)) {
                        when {
                            i < media.size -> {
                                val uri = media[i]
                                val video = context.contentResolver.getType(uri)?.startsWith("video") == true
                                AsyncImage(model = uri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().background(ChipBg))
                                if (video) Icon(Icons.Default.PlayCircle, contentDescription = null, tint = Color.White, modifier = Modifier.align(Alignment.Center))
                                Box(
                                    Modifier.align(Alignment.TopEnd).padding(4.dp).size(22.dp).clip(CircleShape).background(RedInk).clickable { onRemove(uri) },
                                    contentAlignment = Alignment.Center
                                ) { Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear), tint = Color.White, modifier = Modifier.size(14.dp)) }
                                Text(
                                    stringResource(if (video) R.string.submit_media_video else R.string.submit_media_photo, i + 1),
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    modifier = Modifier.align(Alignment.BottomStart).padding(4.dp).clip(RoundedCornerShape(3.dp)).background(Color.Black.copy(alpha = 0.6f)).padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                            i == media.size && media.size < MAX_MEDIA -> Column(
                                Modifier.fillMaxSize().background(ChipBg.copy(alpha = 0.8f)).clickable(onClick = onAdd),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(Modifier.size(28.dp).clip(CircleShape).background(RedInk.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = RedInk, modifier = Modifier.size(17.dp))
                                }
                                Text(stringResource(R.string.submit_add), color = RedInk, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 3.dp))
                            }
                            else -> Column(
                                Modifier.fillMaxSize().background(ChipBg.copy(alpha = 0.4f)),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null, tint = Muted.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
                                Text(stringResource(R.string.submit_media_empty), color = Muted.copy(alpha = 0.7f), fontSize = 10.sp)
                            }
                        }
                    }
                }
                repeat(MEDIA_COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun SubmitBar(preparing: Boolean, onWhatsApp: () -> Unit, onEmail: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(12.dp)
            .background(CardWhite)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        if (preparing) {
            Box(Modifier.fillMaxWidth().padding(vertical = 14.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = RedInk) }
        } else {
            Row(
                Modifier.fillMaxWidth().clip(Field12).background(WhatsAppBrand).clickable(onClick = onWhatsApp).padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(painterResource(R.drawable.ic_whatsapp), contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                Text(stringResource(R.string.submit_whatsapp), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 10.dp))
            }
            Row(
                Modifier.padding(top = 8.dp).fillMaxWidth().clip(Field12).border(1.dp, Hairline, Field12).clickable(onClick = onEmail).padding(vertical = 11.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Email, contentDescription = null, tint = Muted, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.submit_email), color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp))
            }
        }
        Text(
            stringResource(R.string.submit_note_short),
            color = Muted,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
    }
}

// ---------------------------------------------------------------- Success

@Composable
private fun SubmitDone(
    headline: String,
    place: String,
    files: Int,
    sender: String,
    phone: String,
    whatsapp: Boolean,
    onResend: () -> Unit,
    onAnother: () -> Unit,
    onHome: () -> Unit
) {
    val context = LocalContext.current
    Column(
        Modifier
            .fillMaxSize()
            .background(Paper)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().menuCard(RoundedCornerShape(24.dp)).background(CardWhite).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(Modifier.size(112.dp).clip(CircleShape).background(Emerald.copy(alpha = 0.08f)))
                Box(Modifier.size(96.dp).clip(CircleShape).background(Emerald.copy(alpha = 0.14f)))
                Box(Modifier.size(78.dp).clip(CircleShape).background(Emerald), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(42.dp))
                }
            }
            Text(
                stringResource(R.string.submit_done_title),
                color = Ink,
                fontSize = 22.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                stringResource(if (whatsapp) R.string.submit_done_body_whatsapp else R.string.submit_done_body_email),
                color = Muted,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp)
            )
            Row(
                Modifier
                    .padding(top = 14.dp)
                    .clip(CircleShape)
                    .border(1.dp, if (whatsapp) WhatsAppBrand else Hairline, CircleShape)
                    .clickable(onClick = onResend)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (whatsapp) Icon(painterResource(R.drawable.ic_whatsapp), contentDescription = null, tint = WhatsAppBrand, modifier = Modifier.size(18.dp))
                else Icon(Icons.Default.Email, contentDescription = null, tint = Ink, modifier = Modifier.size(18.dp))
                Text(
                    stringResource(if (whatsapp) R.string.submit_reopen_whatsapp else R.string.submit_reopen_email),
                    color = if (whatsapp) Color(0xFF128C4A) else Ink,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        Column(Modifier.fillMaxWidth().menuCard(Card16).background(CardWhite).padding(16.dp)) {
            SectionHeader(stringResource(R.string.submit_done_summary))
            Column(
                Modifier.padding(top = 12.dp).fillMaxWidth().clip(Field12).background(ChipBg.copy(alpha = 0.6f)).padding(12.dp)
            ) {
                Text(stringResource(R.string.submit_done_headline), color = Muted, fontSize = 11.sp)
                Text(headline, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 23.sp)
            }
            SummaryRow(Icons.Default.LocationOn, stringResource(R.string.submit_place), RedInk) {
                Text(place, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            SummaryRow(Icons.Default.PhotoLibrary, stringResource(R.string.submit_done_files), Muted) {
                Text(
                    if (files == 0) stringResource(R.string.submit_done_none) else pluralStringResource(R.plurals.submit_done_files_count, files, files),
                    color = Ink,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(ChipBg).padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            if (sender.isNotEmpty() || phone.isNotEmpty()) {
                SummaryRow(Icons.Default.Person, stringResource(R.string.submit_from), Muted) {
                    val masked = if (phone.length > 5) "+91 ${phone.take(5)} •••••" else phone
                    Text(listOf(sender, masked).filter { it.isNotEmpty() }.joinToString(" · "), color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            SummaryRow(Icons.Default.Send, stringResource(R.string.submit_done_via), Emerald) {
                Row(
                    Modifier.clip(CircleShape).background(Emerald.copy(alpha = 0.12f)).padding(horizontal = 9.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(Emerald))
                    Text(
                        stringResource(if (whatsapp) R.string.submit_done_whatsapp else R.string.submit_done_email),
                        color = Emerald,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 5.dp)
                    )
                }
            }
        }

        Column(Modifier.fillMaxWidth().clip(Card16).background(Color(0xFFFFF8E1)).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(28.dp).clip(CircleShape).background(Color(0xFFFDE68A)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.HourglassTop, contentDescription = null, tint = Color(0xFF78350F), modifier = Modifier.size(17.dp))
                }
                Text(stringResource(R.string.submit_next_title), color = Color(0xFF451A03), fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 10.dp))
            }
            listOf(
                Icons.Default.CheckCircle to R.string.submit_next_1,
                Icons.Default.PhoneInTalk to R.string.submit_next_2,
                Icons.Default.Newspaper to R.string.submit_next_3
            ).forEach { (icon, text) ->
                Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.Top) {
                    Icon(icon, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.padding(top = 2.dp).size(16.dp))
                    Text(stringResource(text), color = Color(0xFF78350F), fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().clip(Field12).background(BrandRed).clickable(onClick = onAnother).padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.AddCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            Text(stringResource(R.string.submit_another), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
        }
        Row(
            Modifier.fillMaxWidth().menuCard(Field12).background(CardWhite).clickable(onClick = onHome).padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Home, contentDescription = null, tint = Muted, modifier = Modifier.size(20.dp))
            Text(stringResource(R.string.submit_home), color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp))
        }

        Column(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.submit_helpline), color = Muted, fontSize = 12.sp, lineHeight = 18.sp, textAlign = TextAlign.Center)
            Row(
                Modifier
                    .padding(top = 4.dp)
                    .clip(CircleShape)
                    .clickable { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$DESK_PHONE"))) }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Call, contentDescription = null, tint = RedInk, modifier = Modifier.size(15.dp))
                Text("+91 94113 40800", color = RedInk, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 5.dp))
            }
        }
    }
}

@Composable
private fun SummaryRow(icon: ImageVector, label: String, tint: Color, value: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 12.dp, start = 4.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Text(label, color = Muted, fontSize = 13.sp, modifier = Modifier.padding(start = 6.dp).weight(1f))
        value()
    }
}

// ---------------------------------------------------------------- Sending

private fun shareableCopies(context: Context, uris: List<Uri>): List<Uri> {
    val dir = File(context.cacheDir, "submit").apply {
        deleteRecursively()
        mkdirs()
    }
    return uris.mapIndexedNotNull { i, uri ->
        runCatching {
            val resolver = context.contentResolver
            val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            } ?: "media_$i"
            val file = File(dir, "${i}_${name.replace(Regex("[^A-Za-z0-9._-]"), "_")}")
            resolver.openInputStream(uri)!!.use { input -> file.outputStream().use { input.copyTo(it) } }
            FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        }.getOrNull()
    }
}

private fun mimeOf(uri: Uri): String =
    MimeTypeMap.getSingleton().getMimeTypeFromExtension(uri.lastPathSegment.orEmpty().substringAfterLast('.', "").lowercase())
        ?: "application/octet-stream"

/** WhatsApp drops attachments sent as a wildcard type, so share the real type or its family. */
private fun sharedType(files: List<Uri>): String {
    val types = files.map(::mimeOf).distinct()
    val families = types.map { it.substringBefore('/') }.distinct()
    return when {
        types.size == 1 -> types.first()
        families.size == 1 -> "${families.first()}/*"
        else -> "*/*"
    }
}

private fun mediaIntent(action: String, text: String, files: List<Uri>) = Intent(action).apply {
    type = if (files.isEmpty()) "text/plain" else sharedType(files)
    putExtra(Intent.EXTRA_TEXT, text)
    if (files.size == 1) putExtra(Intent.EXTRA_STREAM, files.first())
    if (files.size > 1) putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(files))
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
}

private fun sendWhatsApp(context: Context, launcher: ActivityResultLauncher<Intent>, text: String, files: List<Uri>) {
    if (files.isEmpty()) {
        try {
            launcher.launch(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$DESK_WHATSAPP_NUMBER?text=${Uri.encode(text)}")))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, context.getString(R.string.submit_no_whatsapp), Toast.LENGTH_LONG).show()
        }
        return
    }
    val action = if (files.size > 1) Intent.ACTION_SEND_MULTIPLE else Intent.ACTION_SEND
    for (pkg in listOf("com.whatsapp", "com.whatsapp.w4b")) {
        try {
            launcher.launch(mediaIntent(action, text, files).apply {
                setPackage(pkg)
                putExtra("jid", "$DESK_WHATSAPP_NUMBER@s.whatsapp.net")
            })
            return
        } catch (_: ActivityNotFoundException) {
        }
    }
    Toast.makeText(context, context.getString(R.string.submit_no_whatsapp), Toast.LENGTH_LONG).show()
}

private fun sendEmail(context: Context, launcher: ActivityResultLauncher<Intent>, subject: String, text: String, files: List<Uri>) {
    val action = if (files.size > 1) Intent.ACTION_SEND_MULTIPLE else Intent.ACTION_SEND
    val intent = mediaIntent(action, text, files).apply {
        type = if (files.isEmpty()) "message/rfc822" else "*/*"
        putExtra(Intent.EXTRA_EMAIL, arrayOf(DESK_MAIL))
        putExtra(Intent.EXTRA_SUBJECT, "[App] $subject")
    }
    try {
        launcher.launch(Intent.createChooser(intent, context.getString(R.string.submit_email)))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, context.getString(R.string.submit_no_email), Toast.LENGTH_LONG).show()
    }
}
