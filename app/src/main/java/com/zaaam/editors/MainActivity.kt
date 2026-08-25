package com.zaaam.editors
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import com.zaaam.editors.ui.theme.RetroTheme
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { RetroTheme { Text("zaaam/editors - skeleton v0.1") } }
    }
}
