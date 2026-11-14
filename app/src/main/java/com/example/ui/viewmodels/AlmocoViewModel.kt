package com.example.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.data.AlmocoRepository

class AlmocoViewModel(application: Application) : AndroidViewModel(application) {
    private val almocoRepository = AlmocoRepository.getInstance(application)
}
