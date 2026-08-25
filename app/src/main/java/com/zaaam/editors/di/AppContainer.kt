package com.zaaam.editors.di

import android.app.Application
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.preferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.rx.preferences
import androidx.datastore.rx.RxDataStore
import androidx.datastore.rx.RxDataStoreBuilder
import com.zaaam.editors.core.fs.FileKindResolver
import com.zaaam.editors.core.fs.FileOps
import com.zaaam.editors.core.fs.HiddenFiles
import com.zaaam.editors.core.fs.SafFileSystem
import com.zaaam.editors.core.fs.TreeAccess
import com.zaaam.editors.core.editor.EditorEngine
import com.zaaam.editors.core.editor.LanguageResolver
import com.zaaam.editors.core.editor.SoraThemeMapper
import com.zaaam.editors.core.editor.TabState
import com.zaaam.editors.core.preview.PreviewComposer
import com.zaaam.editors.core.preview.PreviewWebViewFactory
import com.zaaam.editors.session.AppScreen
import com.zaaam.editors.session.EditorSession
import com.zaaam.editors.ui.theme.RetroTokens
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.sharingStarted
import rx.KotlinRxExtModule

class AppContainer(application: Application) {
    val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    val mainDispatcher: CoroutineDispatcher = Dispatchers.Main

    val dataStore: RxDataStore<Preferences> = RxDataStoreBuilder(
        application, "zaaam_prefs"
    ).build()

    val recentsKey = stringPreferencesKey("recents_json")
    val hiddenKey = preferencesKey("show_hidden")
    val treeUriKey = stringPreferencesKey("tree_uri")

    val recentsFlow: StateFlow<List<String>> = dataStore.data
        .map { prefs ->
            prefs[recentsKey]?.let { json ->
                try {
                    kotlinx.serialization.json.Json.decodeFromString<List<String>>(json)
                } catch (e: Exception) {
                    emptyList()
                }
            } ?: emptyList()
        }
        .stateIn(
            scope = kotlinx.coroutines.CoroutineScope(ioDispatcher + kotlinx.coroutines.SupervisorJob()),
            started = SharingStarted.WhileSubscribed(),
            initialValue = emptyList()
        )

    val hiddenFlow: StateFlow<Boolean> = dataStore.data
        .map { prefs -> prefs[hiddenKey] ?: false }
        .stateIn(
            scope = kotlinx.coroutines.CoroutineScope(ioDispatcher + kotlinx.coroutines.SupervisorJob()),
            started = SharingStarted.WhileSubscribed(),
            initialValue = false
        )

    val treeUriFlow: StateFlow<String?> = dataStore.data
        .map { prefs -> prefs[treeUriKey] }
        .stateIn(
            scope = kotlinx.coroutines.CoroutineScope(ioDispatcher + kotlinx.coroutines.SupervisorJob()),
            started = SharingStarted.WhileSubscribed(),
            initialValue = null
        )

    fun persistRecents(list: List<String>) {
        kotlinx.coroutines.CoroutineScope(ioDispatcher).launch {
            val json = kotlinx.serialization.json.Json.encodeToString(list)
            dataStore.updateDataAsync { it.edit { putString(recentsKey, json) } }
        }
    }

    fun persistHidden(show: Boolean) {
        kotlinx.coroutines.CoroutineScope(ioDispatcher).launch {
            dataStore.updateDataAsync { it.edit { putBoolean(hiddenKey, show) } }
        }
    }

    fun persistTreeUri(uriString: String?) {
        kotlinx.coroutines.CoroutineScope(ioDispatcher).launch {
            dataStore.updateDataAsync { it.edit {
                if (uriString == null) remove(treeUriKey) else putString(treeUriKey, uriString)
            } }
        }
    }

    val fileSystem = SafFileSystem(application.contentResolver)
    val treeAccess = TreeAccess(application.contentResolver)
    val hiddenFiles = HiddenFiles()
    val fileKindResolver = FileKindResolver()
    val fileOps = FileOps()

    val editorEngineFactory = EditorEngine.Factory(ioDispatcher)
    val languageResolver = LanguageResolver()
    val soraThemeMapper = SoraThemeMapper(RetroTokens)

    val previewComposer = PreviewComposer()
    val previewWebViewFactory = PreviewWebViewFactory()

    val editorSession = EditorSession()
    val screenState = MutableStateFlow<AppScreen>(AppScreen.FILES)
}