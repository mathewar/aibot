package com.aibot.crm

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aibot.crm.data.db.CrmDatabase
import com.aibot.crm.data.repository.CrmRepository
import com.aibot.crm.data.repository.LogResult
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CrmRepositoryTest {

    private lateinit var db: CrmDatabase
    private lateinit var repo: CrmRepository

    @Before
    fun setup() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, CrmDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = CrmRepository(db.personDao(), db.interactionDao())
    }

    @After
    fun teardown() = db.close()

    // ------------------------------------------------------------------
    // logInteraction
    // ------------------------------------------------------------------

    @Test
    fun logInteraction_createsPersonOnFirstContact() = runTest {
        val result = repo.logInteraction("Alice", "Discussed budget", null, null, System.currentTimeMillis())
        assertTrue(result is LogResult.Success)
        val ctx = repo.getRelationshipContext("Alice")
        assertNotNull(ctx)
        assertEquals(1, ctx!!.totalInteractions)
    }

    @Test
    fun logInteraction_reusesPerson_caseInsensitive() = runTest {
        repo.logInteraction("Bob Smith", "First call", null, null, System.currentTimeMillis())
        repo.logInteraction("bob smith", "Second call", null, null, System.currentTimeMillis())
        val ctx = repo.getRelationshipContext("Bob Smith")
        // Both interactions should be on the same person record
        assertEquals(2, ctx!!.totalInteractions)
    }

    @Test
    fun logInteraction_storesNeedsAndWants() = runTest {
        repo.logInteraction(
            personName = "Carol",
            notes = "Coffee chat",
            needs = "new laptop",
            wants = "remote work",
            timestamp = System.currentTimeMillis(),
        )
        val ctx = repo.getRelationshipContext("Carol")!!
        assertEquals("new laptop", ctx.needsSummary)
        assertEquals("remote work", ctx.wantsSummary)
    }

    // ------------------------------------------------------------------
    // getRelationshipContext
    // ------------------------------------------------------------------

    @Test
    fun getRelationshipContext_returnsNullForUnknownPerson() = runTest {
        val ctx = repo.getRelationshipContext("Nobody")
        assertNull(ctx)
    }

    @Test
    fun getRelationshipContext_aggregatesNeedsAcrossInteractions() = runTest {
        val ts = System.currentTimeMillis()
        repo.logInteraction("Dave", "Call 1", needs = "budget approval", wants = null, timestamp = ts)
        repo.logInteraction("Dave", "Call 2", needs = "design mockup", wants = "faster turnaround", timestamp = ts + 1000)
        val ctx = repo.getRelationshipContext("Dave")!!
        assertTrue(ctx.needsSummary.contains("budget approval"))
        assertTrue(ctx.needsSummary.contains("design mockup"))
        assertEquals(2, ctx.totalInteractions)
    }

    @Test
    fun getRelationshipContext_latestInteractionIsFirst() = runTest {
        val now = System.currentTimeMillis()
        repo.logInteraction("Eve", "Older", null, null, now - 10_000)
        repo.logInteraction("Eve", "Newer", null, null, now)
        val ctx = repo.getRelationshipContext("Eve")!!
        assertTrue(ctx.latestInteractionSummary!!.contains("Newer"))
    }
}
