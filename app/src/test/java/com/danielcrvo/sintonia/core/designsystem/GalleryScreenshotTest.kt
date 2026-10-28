package com.danielcrvo.sintonia.core.designsystem

import android.content.Context
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.danielcrvo.sintonia.core.designsystem.theme.SintoniaTheme
import com.danielcrvo.sintonia.core.designsystem.theme.ThemeMode
import com.danielcrvo.sintonia.testing.saveScreenshot
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w400dp-h1500dp-xxhdpi")
class GalleryScreenshotTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun disableAnimations() {
        val resolver = ApplicationProvider.getApplicationContext<Context>().contentResolver
        Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
    }

    @Test
    fun galleryDark() {
        rule.setContent { SintoniaTheme(mode = ThemeMode.Dark) { ComponentGallery(Modifier.width(400.dp)) } }
        rule.saveScreenshot("gallery-dark")
    }

    @Test
    fun galleryLight() {
        rule.setContent { SintoniaTheme(mode = ThemeMode.Light) { ComponentGallery(Modifier.fillMaxSize()) } }
        rule.saveScreenshot("gallery-light")
    }
}
