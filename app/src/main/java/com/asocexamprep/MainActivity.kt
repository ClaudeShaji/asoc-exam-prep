package com.asocexamprep

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.asocexamprep.ui.RadioPrepRoot
import com.asocexamprep.ui.Tab
import com.asocexamprep.ui.theme.RadioPrepTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Debug builds accept `--es debug_screen <route>` to open a screen directly.
        val start = if (BuildConfig.DEBUG && savedInstanceState == null)
            intent.getStringExtra("debug_screen") ?: Tab.HOME.route
        else Tab.HOME.route
        setContent { RadioPrepTheme { RadioPrepRoot(startRoute = start) } }
        // Catch up on the daily goal, quests and badges earned since last time.
        lifecycleScope.launch { container.game.refresh() }
    }
}
