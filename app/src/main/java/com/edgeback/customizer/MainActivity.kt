package com.edgeback.customizer

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.edgeback.customizer.ui.CustomizeScreen
import com.edgeback.customizer.ui.Dock
import com.edgeback.customizer.ui.HomeScreen
import com.edgeback.customizer.ui.SettingsScreen

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val prefs = Prefs(applicationContext)
        setContent {
            EbcTheme(prefs) { App(prefs) }
        }
    }
}

@Composable
private fun App(prefs: Prefs) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val hidden = remember { mutableStateOf(false) }
    val connection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -6f) hidden.value = true
                else if (available.y > 6f) hidden.value = false
                return Offset.Zero
            }
        }
    }
    LaunchedEffect(tab) { hidden.value = false }

    // keep System UI in sync with the settings (debounced while a slider is dragged)
    val ctx = LocalContext.current
    LaunchedEffect(prefs.enableHooks.value, prefs.haptic.value, prefs.zonePercent.value) {
        delay(300)
        withContext(Dispatchers.IO) { ConfigSync.push(ctx, prefs) }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
      Box(Modifier.fillMaxSize().nestedScroll(connection)) {
        when (tab) {
            0 -> HomeScreen(prefs)
            1 -> CustomizeScreen(prefs)
            else -> SettingsScreen(prefs)
        }
        AnimatedVisibility(
            visible = !(hidden.value && prefs.dockHide.value),
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            Dock(selected = tab, onSelect = { tab = it }, prefs = prefs)
        }
      }
    }
}
