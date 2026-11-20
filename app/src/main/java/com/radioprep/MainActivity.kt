package com.radioprep

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.radioprep.ui.RadioPrepRoot
import com.radioprep.ui.Tab
import com.radioprep.ui.theme.RadioPrepTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Debug builds accept `--es debug_screen <route>` to open a screen directly.
        val start = if (BuildConfig.DEBUG && savedInstanceState == null)
            intent.getStringExtra("debug_screen") ?: Tab.HOME.route
        else Tab.HOME.route
        setContent { RadioPrepTheme { RadioPrepRoot(startRoute = start) } }
    }
}
