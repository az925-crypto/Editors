package com.zaaam.editors.core.editor

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class TabState(
    val uri: String,
    val displayName: String,
    val dirty: Boolean = false,
    val lastSavedAt: Long = 0,
    val binary: Boolean = false
)

class EditorSession {
    private val _tabs = MutableStateFlow<List<TabState>>(emptyList())
    val tabs: StateFlow<List<TabState>> = _tabs

    var activeTab: String? = null

    fun addTab(state: TabState) {
        if (!_tabs.value.any { it.uri == state.uri }) {
            _tabs.update { it + state }
        }
        activeTab = state.uri
    }

    fun closeTab(uri: String) {
        _tabs.update { it.filter { item -> item.uri != uri } }
        if (activeTab == uri) {
            activeTab = _tabs.value.lastOrNull()?.uri
        }
    }

    fun markDirty(uri: String, dirty: Boolean) {
        _tabs.update { list -> list.map { if (it.uri == uri) it.copy(dirty = dirty) else it } }
    }

    fun markSaved(uri: String) {
        _tabs.update { list -> list.map { if (it.uri == uri) it.copy(dirty = false, lastSavedAt = System.currentTimeMillis()) else it } }
    }
}

