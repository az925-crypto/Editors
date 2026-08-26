package com.zaaam.editors.ui.tools

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zaaam.editors.ui.theme.RetroTokens

// Komponen shared layar ALAT — terjemahan 1:1 mockup/phase2.html (approved).
// Gaya selaras FilesScreen: clip → background → border/clickable, TANPA shadow berat.
// Material3 cuma Text/BasicTextField dasar; sisanya custom supaya tidak generic.

@Composable
fun ToolsHeroCard(kicker: String, title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(RetroTokens.Card)
            .border(1.dp, RetroTokens.Border, RoundedCornerShape(18.dp))
            .padding(start = 14.dp, end = 14.dp, top = 13.dp, bottom = 11.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(RetroTokens.Olive)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = kicker.uppercase(),
                color = RetroTokens.Muted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.9.sp
            )
        }
        Text(
            text = title,
            color = RetroTokens.Ink,
            fontSize = 21.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.4).sp,
            modifier = Modifier.padding(top = 5.dp)
        )
        if (subtitle.isNotEmpty()) {
            Text(
                text = subtitle,
                color = RetroTokens.Muted,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun ToolsSectionLabel(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = text.uppercase(),
            color = RetroTokens.Muted,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.9.sp
        )
        Box(Modifier.weight(1f).height(1.dp).background(RetroTokens.Border))
    }
}

// Kotak stencil 34dp dengan kode 2 huruf + LED dot warna per alat/file (ala mockup).
@Composable
fun ToolsStencil(letters: String, ledColor: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(RetroTokens.Card)
            .border(1.dp, RetroTokens.Border, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(text = letters, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = RetroTokens.Graphite)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(3.dp)
                .size(7.dp)
                .clip(CircleShape)
                .background(ledColor)
        )
    }
}

// Kartu cartridge menu HUB / baris file terbesar: [stencil] judul+meta ›
@Composable
fun ToolsCartridgeRow(
    stencil: String,
    title: String,
    subtitle: String,
    ledColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(RetroTokens.Card)
            .border(1.dp, RetroTokens.Border, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ToolsStencil(stencil, ledColor)
        Column(Modifier.weight(1f)) {
            Text(title, color = RetroTokens.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(subtitle, color = RetroTokens.Muted, fontSize = 11.sp, fontFamily = FontFamily.Monospace, maxLines = 1)
        }
        Text("›", color = RetroTokens.Muted, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ToolsChip(label: String, selected: Boolean, onClick: () -> Unit, showDot: Boolean = true) {
    Row(
        modifier = Modifier
            .height(28.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) RetroTokens.Graphite else RetroTokens.Card)
            .border(1.dp, if (selected) RetroTokens.Graphite else RetroTokens.Border, RoundedCornerShape(999.dp))
            .clickable { onClick() }
            .padding(horizontal = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (showDot) {
            Box(
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (selected) RetroTokens.Olive else RetroTokens.DimBone)
            )
        }
        Text(
            label.uppercase(),
            color = if (selected) RetroTokens.Card else RetroTokens.Muted,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

// Checkbox kotak olive ala .check mockup. v0.2 duplikat: TANPA aksi hapus — seleksi kosmetik.
@Composable
fun ToolsCheckSquare(checked: Boolean, onToggle: () -> Unit) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(if (checked) RetroTokens.Olive else RetroTokens.Card)
            .border(1.5.dp, if (checked) RetroTokens.Olive else RetroTokens.DimBone, RoundedCornerShape(7.dp))
            .clickable { onToggle() },
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            // Tick: huruf L tipis dirotasi -45° (meniru ::after mockup).
            Box(
                modifier = Modifier
                    .size(width = 10.dp, height = 6.dp)
                    .rotate(-45f)
            ) {
                Box(
                    Modifier
                        .width(2.dp)
                        .height(6.dp)
                        .background(RetroTokens.LcdBg)
                )
                Box(
                    Modifier
                        .width(10.dp)
                        .height(2.dp)
                        .align(Alignment.BottomStart)
                        .background(RetroTokens.LcdBg)
                )
            }
        }
    }
}

@Composable
fun ToolsField(value: String, onValueChange: (String) -> Unit, hint: String, modifier: Modifier = Modifier, mono: Boolean = false) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(
            color = RetroTokens.Ink,
            fontSize = 13.sp,
            fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default
        ),
        cursorBrush = SolidColor(RetroTokens.Olive),
        decorationBox = { inner ->
            Row(
                modifier = Modifier
                    .height(if (mono) 36.dp else 44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(RetroTokens.Card)
                    .border(1.dp, RetroTokens.Border, RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (value.isEmpty()) {
                    Text(hint, color = RetroTokens.Muted, fontSize = 13.sp)
                }
                inner()
            }
        },
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun ToolsProgressBar(done: Int, total: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(RetroTokens.Card)
            .border(1.dp, RetroTokens.Border, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Row(Modifier.fillMaxWidth()) {
            Text("Memindai…", color = RetroTokens.Muted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.weight(1f))
            // Kontrak reviewer: total bisa 0 (root gagal) — jangan pernah div-by-zero.
            val t = if (total <= done) done else total
            Text("$done file", color = RetroTokens.Muted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            if (t > 0 && total != Int.MAX_VALUE) {
                Text(" · $t", color = RetroTokens.DimBone, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        }
        Box(
            Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(RetroTokens.Border)
        ) {
            val fraction = if (total <= 0) 0f else (done.toFloat() / total).coerceIn(0f, 1f)
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(4.dp))
                    .background(RetroTokens.Olive)
            )
        }
    }
}

@Composable
fun ToolsBarRow(label: String, valueText: String, fraction: Float, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            Text(label, color = RetroTokens.Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1)
            Text(valueText, color = RetroTokens.Muted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }
        Box(
            Modifier
                .padding(top = 5.dp)
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(RetroTokens.Border)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .fillMaxSize()
                    .clip(RoundedCornerShape(4.dp))
                    .background(RetroTokens.Olive)
            )
        }
    }
}

enum class LedState { IDLE, SAVING, SAVED, ERROR }

@Composable
fun ToolsLedPill(state: LedState, savedAtText: String? = null, modifier: Modifier = Modifier) {
    val blink = rememberInfiniteTransition(label = "led")
    val alpha by blink.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(450), RepeatMode.Reverse),
        label = "ledAlpha"
    )
    val dotColor = when (state) {
        LedState.IDLE -> RetroTokens.DimBone
        LedState.SAVING -> RetroTokens.LedOrange.copy(alpha = alpha)
        LedState.SAVED -> RetroTokens.LedGreen
        LedState.ERROR -> RetroTokens.Brick
    }
    val text = when (state) {
        LedState.IDLE -> "IDLE"
        LedState.SAVING -> "MENYIMPAN"
        LedState.SAVED -> savedAtText ?: "TERSIMPAN"
        LedState.ERROR -> "GAGAL SIMPAN"
    }
    Row(
        modifier = modifier
            .height(24.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(RetroTokens.Graphite.copy(alpha = 0.06f))
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(dotColor))
        Text(text, color = RetroTokens.Muted, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun ToolsTagPill(text: String) {
    Box(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .border(1.dp, RetroTokens.Border, RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 1.dp)
    ) {
        Text(text, color = RetroTokens.Muted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun ToolsBannerBrick(title: String, body: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(RetroTokens.BrickWash)
            .border(1.dp, RetroTokens.Brick.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 11.dp)
    ) {
        Text(title, color = RetroTokens.Brick, fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold)
        Text(body, color = RetroTokens.Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
fun ToolsNote(text: String, modifier: Modifier = Modifier, solid: Boolean = false) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (solid) RetroTokens.Card else RetroTokens.Graphite.copy(alpha = 0.05f))
            .border(1.dp, if (solid) RetroTokens.Border else RetroTokens.DimBone, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(text, color = RetroTokens.Muted, fontSize = 11.5.sp)
    }
}

@Composable
fun ToolsPrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Box(
        modifier = modifier
            .height(44.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) RetroTokens.Olive else RetroTokens.OlivePress)
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = RetroTokens.LcdBg, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
    }
}

@Composable
fun ToolsSecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Box(
        modifier = modifier
            .height(44.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .clip(RoundedCornerShape(14.dp))
            .background(RetroTokens.Card)
            .border(1.dp, RetroTokens.Border, RoundedCornerShape(14.dp))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = RetroTokens.Ink, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
    }
}

// Sheet konfirmasi blocking ala .sheet mockup: scrim gelap + kartu bawah rounded.
// Dipakai khusus aksi destruktif ("Tidak bisa dibatalkan") & hasil impor. Panggil dari
// Box root layar (overlay fillMaxSize di atas konten).
@Composable
fun ToolsSheet(
    visible: Boolean,
    title: String,
    body: String,
    confirmLabel: String?,
    dismissLabel: String?,
    onConfirm: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    if (!visible) return
    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(onDismiss) { detectTapGestures { onDismiss() } }
            .background(RetroTokens.Graphite.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 10.dp, end = 10.dp, bottom = 74.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(RetroTokens.Card)
                .border(1.dp, RetroTokens.Border, RoundedCornerShape(20.dp))
                .padding(18.dp)
        ) {
            Text(title, color = RetroTokens.Ink, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            Text(body, color = RetroTokens.Muted, fontSize = 12.5.sp, modifier = Modifier.padding(top = 4.dp))
            Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (dismissLabel != null) {
                    ToolsSecondaryButton(dismissLabel, onDismiss, Modifier.weight(1f))
                }
                if (confirmLabel != null && onConfirm != null) {
                    ToolsPrimaryButton(confirmLabel, onConfirm, Modifier.weight(1f))
                } else if (dismissLabel == null && confirmLabel == null) {
                    ToolsPrimaryButton("OKE", onDismiss, Modifier.weight(1f))
                }
            }
        }
    }
}
