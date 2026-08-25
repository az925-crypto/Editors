package com.zaaam.editors

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.zaaam.editors.ui.theme.RetroTheme

class MainActivity : ComponentActivity() {
    private val container by lazy { (application as EditorsApp).container }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { RetroTheme { AppRoot(container) } }
    }
}