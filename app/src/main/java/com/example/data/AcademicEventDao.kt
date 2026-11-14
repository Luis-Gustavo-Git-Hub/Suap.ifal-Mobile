package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface AcademicEventDao {
    @Query("SELECT * FROM academic_events")
    fun getAllEvents(): Flow<List<AcademicEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<AcademicEventEntity>)

    @Query("DELETE FROM academic_events")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(events: List<AcademicEventEntity>) {
        deleteAll()
        insertAll(events)
    }
}
