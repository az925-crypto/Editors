package com.zaaam.editors.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zaaam.editors.ui.theme.RetroTokens

@Composable
fun SafDialog(
    isPicking: Boolean,
    error: String?,
    onPick: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(RetroTokens.Graphite.copy(alpha = 0.45f))
        )
        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.90f)
                .widthIn(max = 360.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = RetroTokens.Card),
            border = BorderStroke(1.dp, RetroTokens.Border),
            elevation = CardDefaults.cardElevation(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Akses file diperlukan",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    color = RetroTokens.Graphite
                )
                Text(
                    text = "Agar bisa buka dan edit file langsung dari HP tanpa pindah app, izinkan akses ke satu folder. Kamu bisa pilih folder Projects atau storage utama.",
                    fontSize = 13.sp,
                    color = RetroTokens.DimDeep,
                    lineHeight = 19.sp
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    CheckRow(text = "Buka file kode langsung di editor")
                    CheckRow(text = "Termasuk file hidden (.thumbnails, .editorconfig)")
                    CheckRow(text = "Tetap offline — tanpa izin internet")
                }
                if (error != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = RetroTokens.BrickWash),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, RetroTokens.Brick.copy(alpha = 0.18f))
                    ) {
                        Text(
                            text = error,
                            modifier = Modifier.padding(12.dp),
                            fontSize = 12.sp,
                            color = RetroTokens.Brick
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.height(44.dp)
                    ) {
                        Text(
                            text = "Nanti",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = RetroTokens.Graphite
                        )
                    }
                    Button(
                        onClick = onPick,
                        enabled = !isPicking,
                        modifier = Modifier.height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RetroTokens.Olive,
                            contentColor = RetroTokens.Ink
                        ),
                        elevation = ButtonDefaults.buttonElevation(6.dp),
                        border = BorderStroke(1.dp, RetroTokens.Border)
                    ) {
                        if (isPicking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = RetroTokens.Ink
                            )
                        } else {
                            Text(
                                text = "Pilih folder",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = RetroTokens.Ink
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckRow(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(RetroTokens.Olive),
            contentAlignment = Alignment.Center
        ) {
            Text("✓", fontSize = 10.sp, color = RetroTokens.Ink, fontWeight = FontWeight.Bold)
        }
        Text(text = text, fontSize = 13.sp, color = RetroTokens.Graphite)
    }
}
