package com.zaaam.editors.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zaaam.editors.di.AppContainer
import com.zaaam.editors.ui.theme.LocalRetroShapes
import com.zaaam.editors.ui.theme.LocalRetroTypography
import com.zaaam.editors.ui.theme.RetroTokens
import com.zaaam.editors.ui.theme.retroShapes
import com.zaaam.editors.ui.theme.retroTypography
import com.zaaam.editors.R

@Composable
fun SafDialog(
    container: AppContainer,
    onDismiss: () -> Unit,
    onGranted: () -> Unit
) {
    val shapes = retroShapes
    val typography = retroTypography

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x00000000))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .wrapContentSize(Alignment.Center)
                .background(RetroTokens.Card, RoundedCornerShape(shapes.dialog))
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(id = R.string.saf_dialog_title),
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = RetroTokens.Graphite
                )

                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PermissionRow(icon = "📁", text = stringResource(id = R.string.saf_perm_1))
                    PermissionRow(icon = "📁", text = stringResource(id = R.string.saf_perm_2))
                    PermissionRow(icon = "🔒", text = stringResource(id = R.string.saf_perm_3))
                }

                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { onDismiss() },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = RetroTokens.Border,
                            contentColor = RetroTokens.Graphite
                        )
                    ) {
                        Text(text = stringResource(id = R.string.saf_btn_later))
                    }

                    Button(
                        onClick = {
                            onGranted()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text(text = stringResource(id = R.string.saf_btn_pick))
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionRow(icon: String, text: String) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = icon, fontSize = 16.sp)
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            color = RetroTokens.Dim
        )
    }
}