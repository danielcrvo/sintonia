package com.danielcrvo.sintonia.testing

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import java.io.File

private const val PagingSettleMillis = 300L

/** Draws the decorView directly: Compose captureToImage never finishes on Robolectric. */
fun AndroidComposeTestRule<*, ComponentActivity>.saveScreenshot(name: String) {
    waitForIdle()
    // Paging delivers pages on another thread; give them a moment before drawing.
    Thread.sleep(PagingSettleMillis)
    waitForIdle()
    val view = activity.window.decorView
    val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
    view.draw(Canvas(bitmap))
    val dir = File("build/screenshots").apply { mkdirs() }
    File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
}
