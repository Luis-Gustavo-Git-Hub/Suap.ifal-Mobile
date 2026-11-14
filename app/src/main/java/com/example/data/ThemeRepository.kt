package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.ui.theme.AppTheme
import com.example.ui.theme.CustomThemeData

class ThemeRepository private constructor(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    fun getSavedThemeId(): String {
        return prefs.getString("saved_app_theme", AppTheme.SYSTEM.id) ?: AppTheme.SYSTEM.id
    }

    fun saveThemeId(themeId: String) {
        prefs.edit().putString("saved_app_theme", themeId).apply()
    }

    companion object {
        @Volatile
        private var INSTANCE: ThemeRepository? = null

        fun getInstance(context: Context): ThemeRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ThemeRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
