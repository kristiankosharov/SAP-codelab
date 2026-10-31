package com.sap.codelab.android

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sap.codelab.R
import com.sap.codelab.ui.components.text.UiText
import com.sap.codelab.ui.components.text.asString
import com.sap.codelab.ui.components.theme.CodelabTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class UiBasicsTest {

    @get:Rule
    val compose = createComposeRule()

    private val app = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun ui_text_resolves_with_a_context() {
        assertEquals("plain", UiText.Dynamic("plain").asString(app))
        assertEquals("Save", UiText.Resource(R.string.action_save).asString(app))
    }

    @Test
    fun ui_text_resolves_in_composition_and_the_theme_has_both_modes() {
        compose.setContent {
            CodelabTheme(darkTheme = true) { Text(UiText.Resource(R.string.action_retry).asString()) }
            CodelabTheme(darkTheme = false) { Text(UiText.Dynamic("light").asString(), color = MaterialTheme.colorScheme.primary) }
        }
        compose.onNodeWithText("Retry").assertIsDisplayed()
        compose.onNodeWithText("light").assertIsDisplayed()
    }

    @Test
    @Config(sdk = [30])
    fun without_dynamic_colors_the_default_schemes_are_used() {
        compose.setContent {
            CodelabTheme(darkTheme = true) { Text("dark") }
            CodelabTheme(darkTheme = false) { Text("light") }
        }
        compose.onNodeWithText("dark").assertIsDisplayed()
        compose.onNodeWithText("light").assertIsDisplayed()
    }
}
