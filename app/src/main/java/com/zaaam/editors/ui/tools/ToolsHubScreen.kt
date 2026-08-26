package com.zaaam.editors.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zaaam.editors.di.AppContainer
import com.zaaam.editors.session.ToolsTab
import com.zaaam.editors.ui.theme.RetroTokens

// Pane HUB ala mockup: hero + 5 kartu cartridge AN/DQ/FR/HX/SN. Murni navigasi lokal —
// tanpa state sendiri selain menulis container.toolsTab.
@Composable
fun ToolsHubScreen(container: AppContainer) {
    val menu = listOf(
        Triple("AN", "Analisa Penyimpanan", ToolsTab.ANALYZE),
        Triple("DQ", "Cari Duplikat", ToolsTab.DUPES),
        Triple("FR", "Ganti Massal", ToolsTab.FIND_REPLACE),
        Triple("HX", "Editor Heks", ToolsTab.HEX),
        Triple("SN", "Snippet", ToolsTab.SNIPPETS)
    )
    val subtitles = mapOf(
        ToolsTab.ANALYZE to "file & folder terbesar dalam tree",
        ToolsTab.DUPES to "hash SHA-1, aman utk file berubah",
        ToolsTab.FIND_REPLACE to "cari & ganti lintas file teks",
        ToolsTab.HEX to "byte-level, file biner \u2264 16 MB",
        ToolsTab.SNIPPETS to "impor/ekspor JSON \u00b7 Codexa menunggu spec"
    )
    val ledOf = mapOf(
        ToolsTab.ANALYZE to RetroTokens.Olive,
        ToolsTab.DUPES to RetroTokens.LedOrange,
        ToolsTab.FIND_REPLACE to RetroTokens.Olive,
        ToolsTab.HEX to RetroTokens.Brick,
        ToolsTab.SNIPPETS to RetroTokens.Olive
    )
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            ToolsHeroCard(
                kicker = "ALAT \u00b7 KOTAK ALAT",
                title = "Peralatan",
                subtitle = "5 alat \u00b7 semua jalan offline di device"
            )
        }
        item { ToolsSectionLabel("MENU \u00b7 PILIH ALAT", Modifier.padding(top = 4.dp)) }
        items(menu, key = { it.third.name }) { (stencil, title, tab) ->
            ToolsCartridgeRow(
                stencil = stencil,
                title = title,
                subtitle = subtitles[tab] ?: "",
                ledColor = ledOf[tab] ?: RetroTokens.Olive,
                onClick = { container.toolsTab.value = tab }
            )
        }
    }
}
