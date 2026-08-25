package com.zaaam.editors

import android.app.Application
import com.zaaam.editors.core.editor.EditorEngine
import com.zaaam.editors.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class EditorsApp : Application() {
    lateinit var container: AppContainer
        private set

    // Scope terpisah dari activity/viewmodel supaya preload tetap jalan walau UI belum siap.
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        instance = this

        // CRITICAL 1: load tema + grammar TextMate (400-1200ms) di background saat proses start,
        // BUKAN di AndroidView factory (main thread) supaya buka file pertama tidak freeze.
        appScope.launch {
            EditorEngine.initTextMate(this@EditorsApp)
        }
    }

    companion object {
        lateinit var instance: EditorsApp
            private set
    }
}
