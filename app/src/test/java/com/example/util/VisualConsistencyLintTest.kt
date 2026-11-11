package com.example.util

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.regex.Pattern

/**
 * Linter test to enforce design system rules.
 * Fails if raw RoundedCornerShape(N.dp), fontSize = N.sp, or raw Color(0x...)
 * are introduced outside of ui/theme and ui/components without explicit exemptions.
 */
class VisualConsistencyLintTest {

    // All screens and dialogs have been successfully migrated to Material Design 3 tokens!
    // Allowed exemptions are only for domain data palettes (e.g. subject categorization palette).
    private val allowedLegacyFiles = emptySet<String>()

    @Test
    fun testNoHardcodedStylingOutsideDesignSystem() {
        val uiDir = File("app/src/main/java/com/example/ui")
        val violations = mutableListOf<String>()

        val roundedPattern = Pattern.compile("RoundedCornerShape\\s*\\(\\s*\\d+(\\.\\d+)?\\.dp\\s*\\)")
        val fontSizePattern = Pattern.compile("fontSize\\s*=\\s*\\d+(\\.\\d+)?\\.sp")
        val hexColorPattern = Pattern.compile("Color\\s*\\(\\s*0x[0-9a-fA-F]+\\s*\\)")

        uiDir.walkTopDown().filter { it.extension == "kt" }.forEach { file ->
            val relPath = file.toRelativeString(uiDir).replace("\\", "/")
            val fileName = file.name

            // Ignore ui/theme and ui/components
            if (relPath.startsWith("theme/") || relPath.startsWith("components/")) {
                return@forEach
            }

            // Exemption check for screens awaiting migration
            if (allowedLegacyFiles.contains(fileName)) {
                return@forEach
            }

            val lines = file.readLines()
            lines.forEachIndexed { index, line ->
                val lineNum = index + 1
                if (roundedPattern.matcher(line).find()) {
                    violations.add("$fileName:$lineNum -> Hardcoded RoundedCornerShape used: ${line.trim()}")
                }
                if (fontSizePattern.matcher(line).find()) {
                    violations.add("$fileName:$lineNum -> Hardcoded fontSize used: ${line.trim()}")
                }
                if (hexColorPattern.matcher(line).find()) {
                    // Explicit exemption: user custom swatch presets in cardColorPresets in FriendsScreen
                    if (fileName == "FriendsScreen.kt" && line.contains("to Color(0x")) {
                        // Allowed palette picker definition
                    } else if (fileName == "AssuntosScreen.kt" && lineNum > 430) {
                        // Allowed academic course domain color classifier (computeSubjectColor)
                    } else {
                        violations.add("$fileName:$lineNum -> Hardcoded Color(0x...) used: ${line.trim()}")
                    }
                }
            }
        }

        assertTrue(
            "Found styling violations in migrated files:\n${violations.joinToString("\n")}",
            violations.isEmpty()
        )
    }
}
