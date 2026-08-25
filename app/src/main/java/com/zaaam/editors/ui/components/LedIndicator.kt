package com.zaaam.editors.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LedIndicator(
    state: State = State.IDLE,
    modifier: Modifier = Modifier,
    label: String = ""
) {
    val color = when (state) {
        State.ORANGE_PULSE -> androidx.compose.ui.graphics.Color(0xFFFF6A2B)
        State.GREEN -> androidx.compose.ui.graphics.Color(0xFF2DB466)
        State.RED -> androidx.compose.ui.graphics.Color(0xFFE53935)
        else -> androidx.compose.ui.graphics.Color(0xFF8FA06A)
    }

    Box(
        modifier = modifier
            .size(7.dp)
            .background(color, CircleShape)
            .align(Alignment.CenterVertically)
    ) {
        if (label.isNotBlank()) {
            androidx.compose.material3.Text(
                text = label,
                fontSize = 6.sp,
                color = androidx.compose.ui.graphics.Color.White,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center)
            )
        }
    }
}

enum class State {
    IDLE, ORANGE_PULSE, GREEN, RED
}