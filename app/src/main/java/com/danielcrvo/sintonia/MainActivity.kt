package com.danielcrvo.sintonia

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.os.LocaleListCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.danielcrvo.sintonia.core.data.ParentalRepository
import com.danielcrvo.sintonia.core.designsystem.theme.DeviceClass
import com.danielcrvo.sintonia.core.designsystem.theme.SintoniaTheme
import com.danielcrvo.sintonia.core.designsystem.theme.resolveDark
import com.danielcrvo.sintonia.navigation.SintoniaNavHost
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** AppCompatActivity: per-app language switching before Android 13 goes through AppCompatDelegate. */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val viewModel: AppViewModel by viewModels()

    @Inject lateinit var parental: ParentalRepository

    override fun onStop() {
        super.onStop()
        parental.relock()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { viewModel.state.value == null }
        enableEdgeToEdge()
        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            val app = state ?: return@setContent
            val deviceClass = remember { DeviceClass.detect(this) }

            LaunchedEffect(app.language) {
                val tags = app.language.tag ?: ""
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tags))
            }

            // System bars follow the app theme (not the system one) and stay transparent.
            val dark = app.themeMode.resolveDark(isSystemInDarkTheme())
            DisposableEffect(dark) {
                val style = if (dark) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }

            SintoniaTheme(mode = app.themeMode, deviceClass = deviceClass) {
                SintoniaNavHost(hasLists = app.hasLists)
            }
        }
    }
}
