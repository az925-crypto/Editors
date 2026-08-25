package com.zaaam.editors.ui.components

import androidx.compose.animation.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zaaam.editors.R
import com.zaaam.editors.ui.theme.LocalRetroShapes
import com.zaaam.editors.ui.theme.LocalRetroTypography
import com.zaaam.editors.ui.theme.RetroTokens
import com.zaaam.editors.ui.theme.retroShapes
import com.zaaam.editors.ui.theme.retroTypography

@Composable
fun BootOverlay(
    onComplete: () -> Unit
) {
    val shapes = retroShapes
    val typography = retroTypography

    var progress by remember { mutableStateOf(0f) }
    var skipped by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A2010))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "zaaam/editors",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = RetroTokens.Olive
            )

            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(4.dp)
                    .background(RetroTokens.Border, RoundedCornerShape(2.dp))
                    .padding(top = 16.dp)
            ) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(progress * 120.dp)
                        .background(RetroTokens.Olive, RoundedCornerShape(2.dp))
                )
            }

            Button(
                onClick = { skipped = true; onComplete() },
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Text(text = stringResource(id = R.string.boot_skip))
            }
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = if (skipped) 1f else progress,
        animationSpec = androidx.compose.animation.spring(dampingRatio = Spring.DampingRatioMediumStiff)
    )

    androidx.compose.runtime.LaunchedEffect(Unit) {
        var current = 0f
        while (current < 1f && !skipped) {
            current += 0.02f
            progress = current
            kotlinx.coroutines.delay(18)
        }
        if (!skipped) onComplete()
    }
}