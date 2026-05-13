package com.aibot.crm.data.repository

import com.aibot.crm.data.db.dao.InteractionDao
import com.aibot.crm.data.db.dao.PersonDao
import com.aibot.crm.data.db.entities.Interaction
import com.aibot.crm.data.db.entities.Person
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CrmRepository(
    private val personDao: PersonDao,
    private val interactionDao: InteractionDao,
) {

    fun observeAllPersons(): Flow<List<Person>> = personDao.observeAll()

    fun observeInteractionsForPerson(personId: Long): Flow<List<Interaction>> =
        interactionDao.observeForPerson(personId)

    /**
     * Finds or creates a person by display name, then logs the interaction.
     * Returns the newly created interaction id.
     */
    suspend fun logInteraction(
        personName: String,
        notes: String?,
        needs: String?,
        wants: String?,
        timestamp: Long,
    ): LogResult {
        val normalized = personName.trim().lowercase()
        val person = personDao.findByNormalizedName(normalized)
            ?: run {
                val newId = personDao.insert(
                    Person(displayName = personName.trim(), normalizedName = normalized)
                )
                personDao.getById(newId) ?: return LogResult.Error("Failed to create contact")
            }

        val interactionId = interactionDao.insert(
            Interaction(
                personId = person.id,
                notes = notes?.takeIf { it.isNotBlank() },
                needs = needs?.takeIf { it.isNotBlank() },
                wants = wants?.takeIf { it.isNotBlank() },
                timestamp = timestamp,
            )
        )
        return LogResult.Success(person = person, interactionId = interactionId)
    }

    /**
     * Returns a structured context object for a person: profile + full interaction history.
     */
    suspend fun getRelationshipContext(personName: String): RelationshipContext? {
        val normalized = personName.trim().lowercase()
        val candidates = personDao.search(normalized)
        val person = candidates.firstOrNull() ?: return null

        val interactions = interactionDao.getForPerson(person.id)
        val latest = interactions.firstOrNull()

        return RelationshipContext(
            person = person,
            interactions = interactions,
            latestInteractionSummary = latest?.let { buildInteractionSummary(it) },
            totalInteractions = interactions.size,
            needsSummary = interactions.mapNotNull { it.needs }.distinct().joinToString("; "),
            wantsSummary = interactions.mapNotNull { it.wants }.distinct().joinToString("; "),
        )
    }

    suspend fun getPersonById(id: Long): Person? = personDao.getById(id)

    suspend fun updatePerson(person: Person) = personDao.update(person)

    suspend fun deletePerson(person: Person) = personDao.delete(person)

    suspend fun searchPersons(query: String): List<Person> =
        personDao.search(query.trim().lowercase())

    private fun buildInteractionSummary(i: Interaction): String {
        val date = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(i.timestamp))
        return buildString {
            append("[$date]")
            i.notes?.let { append(" Notes: $it.") }
            i.needs?.let { append(" Needs: $it.") }
            i.wants?.let { append(" Wants: $it.") }
        }
    }
}

sealed class LogResult {
    data class Success(val person: Person, val interactionId: Long) : LogResult()
    data class Error(val message: String) : LogResult()
}

data class RelationshipContext(
    val person: Person,
    val interactions: List<Interaction>,
    val latestInteractionSummary: String?,
    val totalInteractions: Int,
    val needsSummary: String,
    val wantsSummary: String,
)
