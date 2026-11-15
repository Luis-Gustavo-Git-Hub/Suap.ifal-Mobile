package com.example.sync

import com.example.network.SuapError
import com.example.util.SyncFailureReason
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

sealed interface SyncState {
    data object Idle : SyncState
    data class Syncing(val stage: String) : SyncState
    data class Synced(val at: Long, val partialFailures: Map<SyncModule, SuapError> = emptyMap()) : SyncState
    data class Failed(val reason: SyncFailureReason, val requiresUserAction: Boolean) : SyncState
}
enum class SyncModule { BOLETIM, SCHEDULE, CALENDAR, AVERAGES, AVISOS }

/** One foreground session owner. Waiting detail requests share the same login completion. */
class SyncCoordinator(private val now: () -> Long = { android.os.SystemClock.elapsedRealtime() }) {
    private val lock = Any()
    private val mutableState = MutableStateFlow<SyncState>(SyncState.Idle)
    val state: StateFlow<SyncState> = mutableState.asStateFlow()
    private val userActions = MutableStateFlow(0)
    val userLoginRequests: StateFlow<Int> = userActions.asStateFlow()
    fun requestUserLogin() { userActions.value++ }
    fun consumeUserLoginRequest() { userActions.value = 0 }
    private val mutableLogin = MutableStateFlow(false)
    val loginRequested: StateFlow<Boolean> = mutableLogin.asStateFlow()
    private val times = MutableStateFlow<Map<SyncModule, Long>>(emptyMap())
    val lastSyncedAt: StateFlow<Map<SyncModule, Long>> = times.asStateFlow()
    private var completion: CompletableDeferred<Boolean>? = null
    private var sessionReady = false
    private var blocked = false
    @Volatile var sessionVersion: Long = 0; private set
    private var recoveryUsed = false
    private var lastManual: Long? = null
    private var runModules: (suspend (SyncModule?) -> Map<SyncModule, SuapError>)? = null
    private var pipelineJob: Job? = null
    private var scope: CoroutineScope? = null
    var loginPolicy: () -> SuapError? = { null }

    fun bind(scope: CoroutineScope, modules: suspend (SyncModule?) -> Map<SyncModule, SuapError>) {
        synchronized(lock) { this.scope = scope; runModules = modules }
    }
    fun requestSync(force: Boolean = false, module: SyncModule? = null): Boolean = synchronized(lock) {
        if (mutableState.value is SyncState.Syncing || blocked) return false
        if (!force && mutableState.value != SyncState.Idle) return false
        if (force && lastManual?.let { now() - it < 60_000 } == true) return false
        if (force) { lastManual = now(); recoveryUsed = false }
        if (!sessionReady || module == null) loginPolicy()?.let { fail(it); return false }
        completion = CompletableDeferred()
        mutableState.value = SyncState.Syncing(if (sessionReady && module != null) "Atualizando dados" else "Autenticando")
        if (sessionReady && module != null) launchModules(module) else mutableLogin.value = true
        true
    }
    fun credentialsChanged() = synchronized(lock) {
        blocked = false; sessionReady = false; lastManual = null; recoveryUsed = false
        if (mutableState.value is SyncState.Failed) mutableState.value = SyncState.Idle
    }
    fun reset() = synchronized(lock) {
        pipelineJob?.cancel()
        completion?.complete(false)
        sessionReady = false; blocked = false; recoveryUsed = false; lastManual = null
        mutableLogin.value = false; userActions.value = 0; times.value = emptyMap()
        mutableState.value = SyncState.Idle
    }
    fun sessionAccepted() = synchronized(lock) {
        sessionVersion++
        sessionReady = true
        mutableState.value = SyncState.Syncing("Lendo boletim")
        mutableLogin.value = false
        completion?.complete(true)
    }
    fun parsed(success: Boolean) {
        if (!success) { fail(SuapError.ParseError); return }
        markSynced(SyncModule.BOLETIM)
        markSynced(SyncModule.SCHEDULE)
        launchModules(null)
    }
    private fun launchModules(module: SyncModule?) {
        val owner = scope ?: return fail(SuapError.Unknown)
        val pipelineVersion = sessionVersion
        pipelineJob = owner.launch {
            mutableState.value = SyncState.Syncing("Atualizando dados")
            try {
                val failures = runModules?.invoke(module).orEmpty()
                if (mutableState.value !is SyncState.Failed && !mutableLogin.value && pipelineVersion == sessionVersion) {
                    mutableState.value = SyncState.Synced(System.currentTimeMillis(), failures)
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { fail(SuapError.fromException(e)) }
        }
    }
    fun markSynced(module: SyncModule) = synchronized(lock) {
        times.value = times.value + (module to System.currentTimeMillis())
    }
    fun restoreTimes(value: Map<SyncModule, Long>) = synchronized(lock) { times.value = value }
    fun onSessionRejected(expectedVersion: Long = sessionVersion): Boolean = synchronized(lock) {
        if (expectedVersion != sessionVersion) return false // a newer login already repaired this response
        if (blocked || mutableLogin.value) return false
        sessionReady = false
        if (recoveryUsed) { fail(SuapError.SessionExpired(true)); return false }
        loginPolicy()?.let { fail(it); return false }
        recoveryUsed = true
        pipelineJob?.cancel()
        completion = CompletableDeferred()
        mutableState.value = SyncState.Syncing("Autenticando")
        mutableLogin.value = true
        true
    }
    fun fail(error: SuapError) = synchronized(lock) {
        sessionReady = false
        blocked = error.requiresUserAction
        mutableLogin.value = false
        mutableState.value = SyncState.Failed(error.reason, error.requiresUserAction)
        completion?.complete(false)
        Unit
    }
    suspend fun awaitSession(timeoutMs: Long = 45_000): Boolean {
        val waiter = synchronized(lock) { if (sessionReady) return true; if (mutableState.value is SyncState.Syncing) completion else null }
        return waiter?.let { withTimeoutOrNull(timeoutMs) { it.await() } } ?: false
    }
    val currentError: SuapError? get() = (state.value as? SyncState.Failed)?.let { SuapError.fromReason(it.reason) }
    val isSyncing: Boolean get() = state.value is SyncState.Syncing
    companion object { val shared = SyncCoordinator() }
}
