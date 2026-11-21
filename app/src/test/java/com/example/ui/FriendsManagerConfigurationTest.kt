package com.example.ui

import com.google.firebase.FirebaseApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class FriendsManagerConfigurationTest {
    @Test
    fun registeredSuapFirebaseProjectMakesFriendsAvailable() {
        val context = RuntimeEnvironment.getApplication()

        assertTrue(FriendsManager.isFirebaseConfigured(context))
        assertEquals("suap-ifal", FirebaseApp.getInstance().options.projectId)
    }
}
