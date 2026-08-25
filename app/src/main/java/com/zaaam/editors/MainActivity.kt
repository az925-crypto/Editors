package com.zaaam.editors

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.lifecycleScope
import com.zaaam.editors.di.AppContainer
import com.zaaam.editors.session.AppScreen
import com.zaaam.editors.ui.theme.RetroTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val container: AppContainer
        get() = EditorsApp.instance.container

    private val pickDirectory = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        uri?.let {
            takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            container.persistTreeUri(it.toString())
            container.screenState.value = AppScreen.FILES
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EditorsApp.instance = this

        setContent {
            RetroTheme {
                MaterialTheme {
                    CompositionLocalProvider(
                        androidx.compose.ui.platform.LocalContext provides this,
                        androidx.compose.ui.platform.LocalConfiguration provides configuration,
                        androidx.compose.ui.platform.LocalDensity provides density,
                        androidx.compose.ui.platform.LocalFontFamilyResolver provides fontFamilyResolver,
                        androidx.compose.ui.platform.LocalViewConfiguration provides viewConfiguration
                    ) {
                        AppRoot(container)
                    }
                }
            }
        }

        container.treeUriFlow.collect { uriString ->
            if (uriString == null && container.screenState.value != AppScreen.FILES) {
                container.screenState.value = AppScreen.FILES
            }
        }.also { lifecycleScope.launch { container.editorSession.tabs.collect { _ = it } } }
    }
}