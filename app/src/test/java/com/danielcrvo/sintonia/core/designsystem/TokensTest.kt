package com.danielcrvo.sintonia.core.designsystem

import com.danielcrvo.sintonia.core.designsystem.theme.DarkColors
import com.danielcrvo.sintonia.core.designsystem.theme.LightColors
import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class TokensTest {
    @Test
    fun `dark and light themes set isDark correctly`() {
        assertTrue(DarkColors.isDark)
        assertTrue(!LightColors.isDark)
    }

    @Test
    fun `background and main text never match`() {
        assertNotEquals(DarkColors.background, DarkColors.text)
        assertNotEquals(LightColors.background, LightColors.text)
    }

    @Test
    fun `brand color is the same in both themes`() {
        assertTrue(DarkColors.brand == LightColors.brand)
    }
}
