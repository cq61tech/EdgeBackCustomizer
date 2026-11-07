package com.edgeback.customizer

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.calculateBottomPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Dock(
                selected = tab,
                onSelect = { tab = it },
                floating = prefs.dockFloating.value,
                labels = prefs.dockLabels.value,
            )
        },
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(bottom = inner.calculateBottomPadding())) {
            when (tab) {
                0 -> HomeScreen(prefs)
                1 -> CustomizeScreen(prefs)
                else -> SettingsScreen(prefs)
            }
        }
    }
}
