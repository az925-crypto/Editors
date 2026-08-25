package com.zaaam.editors

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.zaaam.editors.di.AppContainer
import com.zaaam.editors.session.AppScreen
import com.zaaam.editors.ui.components.BootOverlay
import com.zaaam.editors.ui.components.BottomNavPhysical
import com.zaaam.editors.ui.components.HardwareBar
import com.zaaam.editors.ui.components.SafDialog
import com.zaaam.editors.ui.editor.EditorScreen
import com.zaaam.editors.ui.files.FilesScreen
import com.zaaam.editors.ui.preview.PreviewScreen
import com.zaaam.editors.ui.theme.LocalBevelSpec
import com.zaaam.editors.ui.theme.LocalLedPalette
import com.zaaam.editors.ui.theme.LocalLcdPalette
import com.zaaam.editors.ui.theme.LocalRetroShapes
import com.zaaam.editors.ui.theme.LocalRetroTypography
import com.zaaam.editors.ui.theme.RetroTokens
import com.zaaam.editors.ui.theme.bevelSpec
import com.zaaam.editors.ui.theme.ledPalette
import com.zaaam.editors.ui.theme.lcdPalette
import com.zaaam.editors.ui.theme.retroShapes
import com.zaaam.editors.ui.theme.retroTypography
import androidx.compose.ui.unit.px

@Composable
fun AppRoot(container: AppContainer) {
    val lcdPalette = LocalLcdPalette.current
    val ledPalette = LocalLedPalette.current
    val shapes = LocalRetroShapes.current
    val typography = LocalRetroTypography.current
    val bevelSpec = LocalBevelSpec.current

    var showBoot by remember { mutableStateOf(true) }
    val showSafDialog = remember { mutableStateOf(false) }

    val screen by container.screenState.collectAsStateWithLifecycle()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = RetroTokens.Shell
    ) {
        Column(Modifier.fillMaxSize()) {
            HardwareBar(container)

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopStart
            ) {
                when (screen) {
                    AppScreen.FILES -> FilesScreen(container)
                    AppScreen.EDITOR -> EditorScreen(container)
                    AppScreen.PREVIEW -> PreviewScreen(container)
                }

                BottomNavPhysical(container)
            }

            if (showBoot) {
                BootOverlay(onComplete = { showBoot = false })
            }

            if (showSafDialog.value) {
                SafDialog(
                    container = container,
                    onDismiss = { showSafDialog.value = false },
                    onGranted = {
                        showSafDialog.value = false
                        container.screenState.value = AppScreen.FILES
                    }
                )
            }
        }
    }
}