package com.zaaam.editors.ui.tools

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zaaam.editors.di.AppContainer
import com.zaaam.editors.session.ToolsTab
import com.zaaam.editors.ui.theme.RetroTokens

// Wrapper layar ALAT: sub-tab pill (ttab ala mockup) + switch pane + BackHandler
// sub-tab → HUB. BackHandler TIDAK mengonsumsi back saat sudah di HUB — biarkan sistem.
@Composable
fun ToolsScreen(container: AppContainer) {
    val tab by container.toolsTab.collectAsState()
    BackHandler(enabled = tab != ToolsTab.HUB) { container.toolsTab.value = ToolsTab.HUB }
    Column(modifier = Modifier.fillMaxSize()) {
        ToolsTtabRow(
            selected = tab,
            onSelect = { container.toolsTab.value = it },
            modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 10.dp)
        )
        Box(modifier = Modifier.weight(1f)) {
            when (tab) {
                ToolsTab.HUB -> ToolsHubScreen(container)
                ToolsTab.ANALYZE -> AnalyzerScreen(container)
                ToolsTab.DUPES -> DuplicatesScreen(container)
                ToolsTab.FIND_REPLACE -> FindReplaceScreen(container)
                ToolsTab.HEX -> HexScreen(container)
                ToolsTab.SNIPPETS -> SnippetsScreen(container)
            }
        }
    }
}

// Baris pill sub-nav ala .ttabs mockup: pill 30dp, aktif Graphite+bone, idle Card+Border.
@Composable
private fun ToolsTtabRow(selected: ToolsTab, onSelect: (ToolsTab) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val items = listOf(
            "ALAT" to ToolsTab.HUB,
            "ANALISA" to ToolsTab.ANALYZE,
            "DUPLIKAT" to ToolsTab.DUPES,
            "GANTI MASSAL" to ToolsTab.FIND_REPLACE,
            "HEKS" to ToolsTab.HEX,
            "SNIPPET" to ToolsTab.SNIPPETS
        )
        items.forEach { (label, tab) ->
            val on = selected == tab
            Box(
                modifier = Modifier
                    .height(30.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (on) RetroTokens.Graphite else RetroTokens.Card)
                    .border(1.dp, if (on) RetroTokens.Graphite else RetroTokens.Border, RoundedCornerShape(999.dp))
                    .clickable { onSelect(tab) }
                    .padding(horizontal = 13.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (on) RetroTokens.Card else RetroTokens.Dim,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}
