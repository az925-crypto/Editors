package com.zaaam.editors.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zaaam.editors.core.preview.ConsoleEntry
import com.zaaam.editors.ui.preview.PreviewWebViewPanel
import com.zaaam.editors.ui.theme.RetroTokens

// Pane preview split di layar Editor — pasangan live dari editor sora.
//
// WHY data lewat parameter + onConsole ke PreviewViewModel SHARED (instance sama dengan
// layar Preview, karena viewModel{} activity-scoped tanpa NavHost): pipeline
// tick→debounce→compose tetap SATU, tidak ada composer/collector kedua yang bisa
// double-compose atau divergen. Pane ini murni render + blank state.
@Composable
fun SplitPreviewPane(
    activeDisplayName: String,
    renderedHtml: String,
    isLoading: Boolean,
    onConsole: (ConsoleEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.background(RetroTokens.Shell)) {
        // Header mini ala label CONSOLE — kiri kicker, kanan nama file aktif.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .background(RetroTokens.Card)
                .border(width = 1.dp, color = RetroTokens.Border)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "PRATINJAU",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.9.sp,
                color = RetroTokens.Dim
            )
            Text(
                text = activeDisplayName,
                modifier = Modifier.weight(1f).padding(start = 12.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                color = RetroTokens.Graphite
            )
        }
        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = RetroTokens.Olive
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .border(1.dp, RetroTokens.Border, RoundedCornerShape(12.dp))
        ) {
            if (renderedHtml.isBlank()) {
                // WHY pane blank = box teks singkat, BUKAN DEMO_PREVIEW_HTML — demo hanya
                // milik layar Preview penuh; di split user butuh sinyal "dokumen kosong /
                // belum ter-render", bukan preview palsu yang mengesankan isi file.
                Box(
                    modifier = Modifier.fillMaxSize().background(RetroTokens.Card),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Belum ada pratinjau",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = RetroTokens.Graphite
                        )
                        Text(
                            text = "Hasil render dokumen aktif akan muncul di sini.",
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            textAlign = TextAlign.Center,
                            color = RetroTokens.Dim
                        )
                    }
                }
            } else {
                PreviewWebViewPanel(
                    renderedHtml = renderedHtml,
                    reloadSeq = 0,
                    onConsole = onConsole,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

