package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.example.ui.components.*
import com.example.ui.theme.*
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ThemeGalleryTest {
    @get:Rule val compose = createComposeRule()
    @Test fun renderEveryTheme() = renderGallery(1f, "")
    @Test @Config(qualifiers = "w320dp-h640dp")
    fun renderLargeTextOnSmallScreen() = renderGallery(2f, "-large-text")
    @Test @Config(qualifiers = "w800dp-h360dp")
    fun renderLandscape() = renderGallery(1.3f, "-landscape")
    @Test @Config(qualifiers = "w320dp-h640dp")
    fun dashboardWithLargeText() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                MyApplicationTheme(AppTheme.DRACULA) {
                    Surface(color = MaterialTheme.colorScheme.background) {
                        DashboardScreen(studentName = "Maria", campusName = "IFAL • CAMPUS MACEIÓ",
                            globalFrequency = "95%", globalAbsences = "2", totalClasses = "20", passedClasses = "19",
                            subjectsCount = 5, lastSyncTime = "14/11/26 12:00", hasSigaaCredentials = false,
                            hasSuapCredentials = false, onNavigateToBoletim = {}, onNavigateToAssuntos = {},
                            onNavigateToAlmoco = {}, onSyncClick = {})
                    }
                }
            }
        }
        compose.onRoot().captureRoboImage("build/outputs/ui-review/dashboard-large-text.png")
    }
    private fun renderGallery(fontScale: Float, suffix: String) {
        val selected = mutableStateOf<AppTheme>(AppTheme.LIGHT)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
            MyApplicationTheme(selected.value) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ScreenHeader("Seu espaço acadêmico", selected.value.displayName)
                        AppCard {
                            Column(Modifier.padding(16.dp)) {
                                SectionHeader("Boletim")
                                InfoRow("Disciplina", "Matemática")
                                StatusChip("Aprovado", MaterialTheme.appColors.success.color)
                            }
                        }
                        AppTextField("", {}, label = { Text("Matrícula") }, modifier = Modifier.fillMaxWidth())
                        AppTextField("senha", {}, label = { Text("Senha") }, password = true, modifier = Modifier.fillMaxWidth())
                        TermsCheckRow(true, {}, {})
                        PrimaryButton({}, Modifier.fillMaxWidth()) { Text("Entrar") }
                        SecondaryButton({}, Modifier.fillMaxWidth()) { Text("Entrar pelo navegador") }
                        DestructiveButton({}, Modifier.fillMaxWidth()) { Text("Apagar dados") }
                    }
                }
            }
            }
        }
        val themes = AppTheme.values() + listOf(false, true).map { dark ->
            AppTheme.Custom(CustomThemeData("amber-$dark", "Âmbar", ThemeColor.AMBER, ThemeColor.AMBER, ThemeColor.AMBER, dark))
        }
        for (theme in themes) {
            compose.runOnIdle { selected.value = theme }
            compose.waitForIdle()
            compose.onRoot().captureRoboImage("build/outputs/ui-review/${theme.id}$suffix.png")
        }
    }
}
