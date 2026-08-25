package com.zaaam.editors.ui.components
import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import com.zaaam.editors.di.AppContainer
@Composable fun SafDialog(container: AppContainer, onDismiss: () -> Unit = {}, onGranted: () -> Unit = {}) { Text("SAF") }
