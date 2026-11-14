package com.example.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class FriendsManagerConfigurationTest {
    @Test
    fun missingDefaultFirebaseAppLeavesFriendsUnavailableWithoutCrashing() {
        val context = RuntimeEnvironment.getApplication()

        assertFalse(FriendsManager.isFirebaseConfigured(context))
        assertFalse(FriendsManager.isUserSignedIn())
        assertNull(FriendsManager.currentUserId())
    }
}
