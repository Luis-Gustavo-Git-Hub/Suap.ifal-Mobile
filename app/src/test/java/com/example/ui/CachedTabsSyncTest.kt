package com.example.ui

import android.app.Application
import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.SubjectRepository
import com.example.network.SyncMetrics
import com.example.sync.SyncState
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CachedTabsSyncTest {
    @get:Rule val compose = createComposeRule()
    @Test fun openingAndSwitchingTabsDuringSyncMakesNoRequests() {
        val context = RuntimeEnvironment.getApplication()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        val prefs = context.getSharedPreferences("cached-tabs-test", Context.MODE_PRIVATE)
        prefs.edit().putString("saved_avisos_json", """[{"sender":"Professor de teste","subject":"Aviso salvo offline","date":"15/11/2026","isRead":false,"detailUrl":"/aviso/1"}]""").commit()
        val model = SuapViewModel(context, SubjectRepository(db.subjectDao()), prefs)
        val store = ViewModelStore().apply { put("model", model) }
        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Unconfined)
        model.syncCoordinator.bind(scope) { emptyMap() }
        model.syncCoordinator.loginPolicy = { null }
        model.syncCoordinator.credentialsChanged()
        model.requestSync()
        val selected = mutableStateOf(0)
        val before = SyncMetrics.requests.get()
        compose.setContent {
            MyApplicationTheme {
                if (selected.value == 0) AvisosScreen(model) else HorariosScreen(model, emptyList(), {})
            }
        }
        compose.onNodeWithText("Aviso salvo offline").assertExists()
        compose.onNodeWithText("Sincronizando...").assertExists()
        repeat(4) { index -> compose.runOnIdle { selected.value = (index + 1) % 2 }; compose.waitForIdle() }
        assertEquals(before, SyncMetrics.requests.get())
        compose.runOnIdle { model.syncCoordinator.fail(com.example.network.SuapError.NoInternet) }
        compose.onNodeWithText("Aviso salvo offline").assertExists()
        assertEquals(before, SyncMetrics.requests.get())
        compose.onNodeWithText("Não foi possível atualizar os avisos").assertDoesNotExist()
        compose.runOnIdle { model.syncCoordinator.sessionAccepted(); model.syncCoordinator.parsed(true) }
        repeat(4) { index -> compose.runOnIdle { selected.value = (index + 1) % 2 }; compose.waitForIdle() }
        assertEquals(before, SyncMetrics.requests.get())
        scope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
        store.clear()
        db.close()
    }
}
