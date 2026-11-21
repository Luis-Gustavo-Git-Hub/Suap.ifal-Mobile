package com.example.ui

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.ui.components.AppPageHeader
import com.example.ui.components.AppButton
import com.example.ui.theme.*
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w320dp-h1000dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ThemeComponentsTest(private val theme: AppTheme, private val font: AppFontFamily) {
    @get:Rule val compose = createComposeRule()

    @Test fun headerWrapsAndActionWorksWithLargeFonts() {
        var clicked = false
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                MyApplicationTheme(appTheme = theme, fontOverride = font) {
                    assertTrue(MaterialTheme.typography.labelSmall.fontSize.value >= 11f)
                    val scheme = MaterialTheme.colorScheme
                    listOf(scheme.onPrimary to scheme.primary, scheme.onPrimaryContainer to scheme.primaryContainer,
                        scheme.onSecondary to scheme.secondary, scheme.onTertiary to scheme.tertiary,
                        scheme.onSurface to scheme.surface, scheme.onError to scheme.error).forEach { (fg, bg) ->
                        val base = bg.compositeOver(scheme.surface)
                        val x = fg.compositeOver(base).luminance()
                        val y = base.luminance()
                        assertTrue("Unreadable color pair for ${theme.id}", (maxOf(x, y) + 0.05f) / (minOf(x, y) + 0.05f) >= 4.5f)
                    }
                    Column(Modifier.width(320.dp)) {
                        AppPageHeader(title = "Olá, estudante com nome composto", titleMaxLines = null,
                            subtitle = "Seu resumo acadêmico")
                        AppButton(text = "Continuar", onClick = { clicked = true })
                    }
                }
            }
        }
        compose.onNodeWithText("Olá, estudante com nome composto").assertIsDisplayed()
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText("Olá, estudante com nome composto")
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue(layouts.isNotEmpty())
        assertTrue(layouts.joinToString { "size=${it.size}, constraints=${it.layoutInput.constraints}, lines=${it.lineCount}, exceededWidth=${it.didOverflowWidth}, exceededHeight=${it.didOverflowHeight}" }, layouts.none { it.hasVisualOverflow })
        compose.onNodeWithText("Continuar").assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(clicked) }
    }

    companion object {
        @JvmStatic @ParameterizedRobolectricTestRunner.Parameters(name = "{0} / {1}")
        fun themes(): List<Array<Any>> {
            val themes = AppTheme.values() + AppTheme.ROBIN_KAWA + listOf(false, true).map { dark ->
                AppTheme.Custom(CustomThemeData("amber-$dark", "Âmbar", ThemeColor.AMBER,
                    ThemeColor.AMBER, isDark = dark))
            }
            return themes.flatMap { theme ->
                listOf(AppFontFamily.FIRA_CODE, AppFontFamily.CINZEL, AppFontFamily.FREDOKA)
                    .map { font -> arrayOf<Any>(theme, font) }
            }
        }
    }
}
