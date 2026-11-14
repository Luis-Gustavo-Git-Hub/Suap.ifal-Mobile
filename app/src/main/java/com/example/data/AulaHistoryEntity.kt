package com.example.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "aulas_history")
data class AulaHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subjectUrl: String,
    val date: String,
    val etapa: String,
    val numAulas: String,
    val numFaltas: String,
    val conteudo: String,
    val professor: String,
    val attachmentUrl: String = "",
    val attachmentTitle: String = "",
    val rawHtml: String = "",
    val linksJson: String = ""
)

fun AulaHistory.toEntity(subjectUrl: String): AulaHistoryEntity {
    val serializedLinks = links.joinToString(";;") { "${it.title}|${it.url}" }
    return AulaHistoryEntity(
        subjectUrl = subjectUrl,
        date = data,
        etapa = etapa,
        numAulas = numAulas,
        numFaltas = numFaltas,
        conteudo = conteudo,
        professor = professor,
        attachmentUrl = attachmentUrl,
        attachmentTitle = attachmentTitle,
        rawHtml = rawHtml,
        linksJson = serializedLinks
    )
}

fun AulaHistoryEntity.toDomain(): AulaHistory {
    val deserializedLinks = if (linksJson.isBlank()) {
        emptyList()
    } else {
        linksJson.split(";;").mapNotNull {
            val parts = it.split("|")
            if (parts.size >= 2) {
                AulaLink(title = parts[0], url = parts[1])
            } else {
                null
            }
        }
    }
    return AulaHistory(
        data = date,
        etapa = etapa,
        numAulas = numAulas,
        numFaltas = numFaltas,
        conteudo = conteudo,
        professor = professor,
        attachmentUrl = attachmentUrl,
        attachmentTitle = attachmentTitle,
        rawHtml = rawHtml,
        links = deserializedLinks
    )
}

@Dao
interface AulaHistoryDao {
    @Query("SELECT * FROM aulas_history WHERE subjectUrl = :subjectUrl")
    suspend fun getAulasForSubject(subjectUrl: String): List<AulaHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAulas(aulas: List<AulaHistoryEntity>)

    @Query("DELETE FROM aulas_history WHERE subjectUrl = :subjectUrl")
    suspend fun deleteAulasForSubject(subjectUrl: String)

    @Query("DELETE FROM aulas_history")
    suspend fun deleteAll()
}
