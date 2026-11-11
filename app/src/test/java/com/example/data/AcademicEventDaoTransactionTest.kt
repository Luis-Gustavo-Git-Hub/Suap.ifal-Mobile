package com.example.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AcademicEventDaoTransactionTest {
    @Test
    fun calendarObservers_doNotSeeAnEmptyCalendarDuringReplacement() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), AppDatabase::class.java
        ).build()
        val emissions = Channel<List<AcademicEventEntity>>(Channel.UNLIMITED)
        val dao = database.academicEventDao()
        val oldEvent = AcademicEventEntity(dateInfo = "01/10/2026", description = "Evento anterior", color = "#FFFFFF")
        val newEvent = oldEvent.copy(dateInfo = "28/10/2026", description = "Evento atualizado")
        var collector: kotlinx.coroutines.Job? = null
        try {
            dao.replaceAll(listOf(oldEvent))
            collector = launch(Dispatchers.IO) { dao.getAllEvents().collect { emissions.send(it) } }
            withTimeout(5_000) {
                assertEquals("Evento anterior", emissions.receive().single().description)
                dao.replaceAll(listOf(newEvent))
                var updated: List<AcademicEventEntity>
                do {
                    updated = emissions.receive()
                    assertTrue("A transient empty value can trigger incorrect fallback writes", updated.isNotEmpty())
                } while (updated.single().description != "Evento atualizado")
                assertEquals("Evento atualizado", updated.single().description)
                dao.replaceAll(emptyList())
                var cleared: List<AcademicEventEntity>
                do {
                    cleared = emissions.receive()
                } while (cleared.isNotEmpty())
                assertTrue("An intentional clear must still propagate", cleared.isEmpty())
            }
        } finally {
            collector?.cancelAndJoin()
            emissions.close()
            database.close()
        }
    }
}
