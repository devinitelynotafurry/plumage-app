package dev.plumage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dagger.hilt.android.AndroidEntryPoint
import dev.plumage.data.prefs.SettingsRepository
import dev.plumage.domain.model.Settings
import dev.plumage.ui.nav.PlumageNavHost
import dev.plumage.ui.theme.PlumageTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by settingsRepository.settings.collectAsState(initial = Settings())
            PlumageTheme(useDynamicColor = settings.useDynamicColor) {
                PlumageNavHost()
            }
        }
    }
}
