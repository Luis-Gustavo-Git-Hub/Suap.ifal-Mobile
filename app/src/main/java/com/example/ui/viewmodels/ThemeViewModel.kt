package com.example.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.data.ThemeRepository
import com.example.ui.theme.AppTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ThemeViewModel(application: Application) : AndroidViewModel(application) {
    private val themeRepository = ThemeRepository.getInstance(application)

    private val _currentTheme = MutableStateFlow<AppTheme>(AppTheme.valueOf(themeRepository.getSavedThemeId()))
    val currentTheme: StateFlow<AppTheme> = _currentTheme.asStateFlow()

    fun setTheme(theme: AppTheme) {
        _currentTheme.value = theme
        themeRepository.saveThemeId(theme.id)
    }
}
