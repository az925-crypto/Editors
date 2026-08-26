package com.zaaam.editors.session

// Sub-tab dalam layar ALAT. HUB = daftar 5 alat; sisanya layar masing-masing.
// State-nya StateFlow di AppContainer (bukan nav-compose) — konsisten pola AppScreen.
enum class ToolsTab {
    HUB, ANALYZE, DUPES, FIND_REPLACE, HEX, SNIPPETS
}
