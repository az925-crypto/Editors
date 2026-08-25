package com.zaaam.editors.di

import android.app.Application
import com.zaaam.editors.core.editor.EditorSession
import com.zaaam.editors.core.fs.FileKindResolver
import com.zaaam.editors.core.fs.FileOps
import com.zaaam.editors.core.fs.HiddenFiles
import com.zaaam.editors.core.fs.SafFileSystemImpl
import com.zaaam.editors.core.fs.TreeAccess
import com.zaaam.editors.session.AppScreen
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow

class AppContainer(application: Application) {
    val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    val mainDispatcher: CoroutineDispatcher = Dispatchers.Main

    val fileSystem = SafFileSystemImpl(application.contentResolver)
    val treeAccess = TreeAccess(application.contentResolver)
    val hiddenFiles = HiddenFiles()
    val fileKindResolver = FileKindResolver()
    val fileOps = FileOps()

    val editorSession = EditorSession()
    val screenState = MutableStateFlow(AppScreen.FILES)
}