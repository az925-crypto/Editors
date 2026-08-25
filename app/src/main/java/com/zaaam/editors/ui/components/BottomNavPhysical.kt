package com.zaaam.editors.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zaaam.editors.di.AppContainer
import com.zaaam.editors.session.AppScreen
import com.zaaam.editors.ui.theme.LocalRetroShapes
import com.zaaam.editors.ui.theme.LocalRetroTypography
import com.zaaam.editors.ui.theme.RetroTokens
import com.zaaam.editors.ui.theme.retroShapes
import com.zaaam.editors.ui.theme.retroTypography
import androidx.compose.ui.unit.px
import com.zaaam.editors.R

@Composable
fun BottomNavPhysical(container: AppContainer) {
    val shapes = retroShapes
    val typography = retroTypography

    val screen by container.screenState.collectAsStateWithLifecycle()
    val hasTabs by container.editorSession.tabs.collectAsStateWithLifecycle { it.isNotEmpty() }
    val activeIsWeb by container.editorSession.tabs.collectAsStateWithLifecycle {
        it.find { it.uri == container.editorSession.activeTab }?.let { it.uri.endsWith(".html") || it.uri.endsWith(".css") || it.uri.endsWith(".js") } ?: false
    }

    val items = listOf(
        NavItem(AppScreen.FILES, R.string.nav_files, true),
        NavItem(AppScreen.EDITOR, R.string.nav_editor, hasTabs),
        NavItem(AppScreen.PREVIEW, R.string.nav_preview, activeIsWeb)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(RetroTokens.Shell)
            .padding(top = 8.dp, bottom = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isActive = screen == item.screen
                val isEnabled = item.enabled
                val alpha = if (isEnabled) 1f else 0.38f

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .weight(1f)
                        .background(
                            if (isActive) RetroTokens.Graphite else Color.Transparent,
                            RoundedCornerShape(12.dp)
                        )
                        .alpha(alpha)
                        .clickable(enabled = isEnabled) {
                            if (isEnabled) container.screenState.value = item.screen
                        }
                        .padding(horizontal = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = item.stringRes),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (isActive) RetroTokens.Olive else RetroTokens.Dim.copy(alpha = alpha)
                        )
                    }
                }
            }
        }
    }
}

data class NavItem(
    val screen: AppScreen,
    val stringRes: Int,
    val enabled: Boolean
)