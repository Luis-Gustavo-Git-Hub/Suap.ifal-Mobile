package com.example.sync

import com.example.network.SuapError
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class SyncCoordinatorTest {
    @Test fun openingAndRepeatedTriggersShareOneLogin() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val owner = SyncCoordinator { 100_000 }
        var pipelines = 0
        owner.bind(scope) { pipelines++; emptyMap() }
        assertTrue(owner.requestSync())
        repeat(10) { assertFalse(owner.requestSync()) }
        assertTrue(owner.loginRequested.value)
        owner.sessionAccepted()
        owner.parsed(true)
        assertEquals(1, pipelines)
        assertTrue(owner.state.value is SyncState.Synced)
        scope.cancel()
    }
    @Test fun manualRefreshUsesReadySessionAndRespectsCooldown() = runBlocking {
        var clock = 0L
        val owner = SyncCoordinator { clock }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val modules = mutableListOf<SyncModule?>()
        owner.bind(scope) { modules.add(it); emptyMap() }
        owner.sessionAccepted()
        owner.parsed(true)
        assertTrue(owner.requestSync(true, SyncModule.AVISOS))
        assertFalse(owner.loginRequested.value)
        assertFalse(owner.requestSync(true, SyncModule.SCHEDULE))
        clock = 60_000
        assertTrue(owner.requestSync(true, SyncModule.SCHEDULE))
        assertEquals(listOf(null, SyncModule.AVISOS, SyncModule.SCHEDULE), modules)
        scope.cancel()
    }
    @Test fun rejectionIsSingleFlightAndStaleResponsesCannotRestartLogin() = runBlocking {
        val owner = SyncCoordinator { 0 }
        owner.sessionAccepted()
        val version = owner.sessionVersion
        assertTrue(owner.onSessionRejected(version))
        repeat(10) { assertFalse(owner.onSessionRejected(version)) }
        val waiters = List(10) { async { owner.awaitSession(1000) } }
        owner.sessionAccepted()
        assertTrue(waiters.awaitAll().all { it })
        assertFalse(owner.onSessionRejected(version))
    }
    @Test fun actionRequiredStopsAutomaticRetriesUntilCredentialsChange() = runBlocking {
        for (error in listOf(SuapError.InvalidCredentials, SuapError.VerificationRequired, SuapError.SessionExpired(true))) {
            val owner = SyncCoordinator { 0 }
            owner.fail(error)
            repeat(5) { assertFalse(owner.requestSync()); assertFalse(owner.onSessionRejected()) }
            assertFalse(owner.loginRequested.value)
            assertFalse(owner.awaitSession(10))
            owner.credentialsChanged()
            assertTrue(owner.requestSync())
        }
    }
    @Test fun partialFailureKeepsSuccessfulModuleTimestamps() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val owner = SyncCoordinator { 0 }
        owner.bind(scope) {
            owner.markSynced(SyncModule.AVISOS)
            mapOf(SyncModule.CALENDAR to SuapError.NoInternet)
        }
        owner.requestSync(); owner.sessionAccepted(); owner.parsed(true)
        val result = owner.state.value as SyncState.Synced
        assertEquals(SuapError.NoInternet, result.partialFailures[SyncModule.CALENDAR])
        assertTrue(owner.lastSyncedAt.value.containsKey(SyncModule.AVISOS))
        assertFalse(owner.lastSyncedAt.value.containsKey(SyncModule.CALENDAR))
        scope.cancel()
    }
}
