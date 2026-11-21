package com.example.worker

import com.example.network.*
import kotlinx.coroutines.runBlocking
import okhttp3.Request
import org.junit.Assert.*
import org.junit.Test

class WorkerSessionRecoveryTest {
    private fun expired() = NetworkResult.Error("expirada", error = SuapError.SessionExpired())
    @Test fun rejectedSessionHasOneHttpLoginBudgetAcrossModules() = runBlocking {
        var requests = 0; var logins = 0
        val recovery = WorkerSessionRecovery(true, { requests++; expired() }, { logins++; NetworkResult.Success(true) })
        repeat(2) { recovery.execute(Request.Builder().url("https://suap.ifal.edu.br/")) }
        assertEquals(1, logins)
        assertEquals(3, requests) // first + retry, second module without another login
    }
    @Test fun govBrNeverUsesPasswordLogin() = runBlocking {
        var logins = 0
        val recovery = WorkerSessionRecovery(false, { expired() }, { logins++; NetworkResult.Success(true) })
        recovery.execute(Request.Builder().url("https://suap.ifal.edu.br/"))
        assertEquals(0, logins)
    }
    @Test fun invalidCredentialsDoNotTriggerAnotherAttemptAndAreNotTransient() = runBlocking {
        var logins = 0
        val recovery = WorkerSessionRecovery(true, { expired() }, { logins++; NetworkResult.Error("inválidas", error = SuapError.InvalidCredentials) })
        repeat(3) { recovery.execute(Request.Builder().url("https://suap.ifal.edu.br/")) }
        assertEquals(1, logins)
        assertFalse(SuapError.InvalidCredentials.isTransient)
        assertFalse(SuapError.VerificationRequired.isTransient)
        assertTrue(SuapError.NoInternet.isTransient)
        assertTrue(SuapError.Timeout.isTransient)
        assertTrue(SuapError.SuapUnavailable.isTransient)
    }
}
