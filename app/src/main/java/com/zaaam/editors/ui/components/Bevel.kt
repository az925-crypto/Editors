package com.zaaam.editors.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.zaaam.editors.ui.theme.LocalBevelSpec
import com.zaaam.editors.ui.theme.LocalRetroShapes
import com.zaaam.editors.ui.theme.RetroTokens
import com.zaaam.editors.ui.theme.bevelSpec
import com.zaaam.editors.ui.theme.retroShapes

@Composable
fun BevelShell(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val shapes = retroShapes
    val bevel = bevelSpec

    Box(
        modifier = modifier
            .background(RetroTokens.Shell, RoundedCornerShape(shapes.shell))
            .padding(2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(
                            bevel.highlightTop,
                            RetroTokens.Shell,
                            bevel.shadowBottom
                        ),
                        start = androidx.compose.ui.geometry.Offset.Zero,
                        end = androidx.compose.ui.geometry.Offset(0f, 100.dp.toPx())
                    ),
                    RoundedCornerShape(shapes.shell)
                )
        ) {
            content()
        }
    }
}

@Composable
fun BevelCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val shapes = retroShapes
    val bevel = bevelSpec

    Box(
        modifier = modifier
            .background(RetroTokens.Card, RoundedCornerShape(shapes.card))
            .padding(1.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(
                            bevel.highlightTop,
                            RetroTokens.Card,
                            bevel.shadowBottom
                        ),
                        start = androidx.compose.ui.geometry.Offset.Zero,
                        end = androidx.compose.ui.geometry.Offset(0f, 100.dp.toPx())
                    ),
                    RoundedCornerShape(shapes.card)
                )
        ) {
            content()
        }
    }
}

@Composable
fun PressDepressed(
    pressed: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .graphicsLayer {
                translationY = if (pressed) 1.dp.toPx() else 0f
                shadowElevation = if (pressed) 0.dp.toPx() else 4.dp.toPx()
            }
    ) {
        content()
    }
}