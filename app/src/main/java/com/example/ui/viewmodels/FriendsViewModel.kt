package com.example.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.ui.FriendsManager
import kotlinx.coroutines.flow.StateFlow

class FriendsViewModel(application: Application) : AndroidViewModel(application) {
    val currentUserId: String?
        get() = FriendsManager.currentUserId()
}
