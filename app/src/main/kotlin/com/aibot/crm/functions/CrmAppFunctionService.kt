package com.aibot.crm.functions

import android.os.Bundle
import android.util.Log
import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionFunctionNotFoundException
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.AppFunctionService
import com.aibot.crm.data.db.CrmDatabase
import com.aibot.crm.data.repository.CrmRepository
import com.aibot.crm.data.repository.LogResult
import com.aibot.crm.data.repository.RelationshipContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val TAG = "CrmAppFunctionService"

/**
 * Exposes two App Functions to on-device AI agents (Gemini, etc.):
 *
 *   • logInteraction  — record a conversation/touchpoint with a contact
 *   • getRelationshipContext — retrieve full history for a contact
 *
 * All data is stored in a Room database in the app's private internal
 * storage. No network permission is declared; data never leaves the device.
 *
 * Android 16 EAP: extend AppFunctionService and override onExecuteFunction.
 * The system binds this service when an AI agent invokes one of the
 * functions declared in res/xml/app_functions.xml.
 */
class CrmAppFunctionService : AppFunctionService() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private val repository: CrmRepository by lazy {
        val db = CrmDatabase.getInstance(applicationContext)
        CrmRepository(db.personDao(), db.interactionDao())
    }

    // -------------------------------------------------------------------------
    // AppFunctionService entry point
    // -------------------------------------------------------------------------

    override fun onExecuteFunction(
        appFunctionContext: AppFunctionContext,
        request: Bundle,
        callback: AppFunctionCallback,
    ) {
        val functionId = request.getString(KEY_FUNCTION_ID)
            ?: return callback.onError(AppFunctionInvalidArgumentException("Missing function id"))

        Log.d(TAG, "Executing function: $functionId, caller: ${appFunctionContext.callingPackage}")

        scope.launch {
            try {
                val result: String = when (functionId) {
                    FN_LOG_INTERACTION -> executeLogInteraction(request)
                    FN_GET_CONTEXT -> executeGetRelationshipContext(request)
                    else -> throw AppFunctionFunctionNotFoundException(
                        "Unknown function: $functionId"
                    )
                }
                callback.onSuccess(Bundle().apply { putString(KEY_RESULT, result) })
            } catch (e: AppFunctionFunctionNotFoundException) {
                callback.onError(e)
            } catch (e: AppFunctionInvalidArgumentException) {
                callback.onError(e)
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error in $functionId", e)
                callback.onError(AppFunctionInvalidArgumentException("Internal error: ${e.message}"))
            }
        }
    }

    // -------------------------------------------------------------------------
    // logInteraction
    // -------------------------------------------------------------------------

    private suspend fun executeLogInteraction(params: Bundle): String {
        val person = params.getString("person")?.takeIf { it.isNotBlank() }
            ?: throw AppFunctionInvalidArgumentException("'person' is required")

        val notes = params.getString("notes")
        val needs = params.getString("needs")
        val wants = params.getString("wants")
        val timestamp = if (params.containsKey("timestamp"))
            params.getLong("timestamp")
        else
            System.currentTimeMillis()

        return when (val result = repository.logInteraction(person, notes, needs, wants, timestamp)) {
            is LogResult.Success -> Json.encodeToString(
                LogInteractionResponse(
                    success = true,
                    interactionId = result.interactionId,
                    personId = result.person.id,
                    personDisplayName = result.person.displayName,
                    message = "Logged interaction with ${result.person.displayName}",
                )
            )
            is LogResult.Error -> Json.encodeToString(
                LogInteractionResponse(
                    success = false,
                    message = result.message,
                )
            )
        }
    }

    // -------------------------------------------------------------------------
    // getRelationshipContext
    // -------------------------------------------------------------------------

    private suspend fun executeGetRelationshipContext(params: Bundle): String {
        val person = params.getString("person")?.takeIf { it.isNotBlank() }
            ?: throw AppFunctionInvalidArgumentException("'person' is required")

        val context = repository.getRelationshipContext(person)
            ?: return Json.encodeToString(
                RelationshipContextResponse(found = false, personName = person)
            )

        return Json.encodeToString(context.toResponse())
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    companion object {
        private const val KEY_FUNCTION_ID = "functionId"
        private const val KEY_RESULT = "result"
        private const val FN_LOG_INTERACTION = "logInteraction"
        private const val FN_GET_CONTEXT = "getRelationshipContext"
    }
}

// -------------------------------------------------------------------------
// Response models (serialized to JSON and returned to the AI agent)
// -------------------------------------------------------------------------

@Serializable
private data class LogInteractionResponse(
    val success: Boolean,
    val interactionId: Long = -1,
    val personId: Long = -1,
    val personDisplayName: String = "",
    val message: String = "",
)

@Serializable
private data class RelationshipContextResponse(
    val found: Boolean,
    val personName: String,
    val personId: Long = -1,
    val email: String? = null,
    val phone: String? = null,
    val totalInteractions: Int = 0,
    val latestInteraction: String? = null,
    val needsSummary: String = "",
    val wantsSummary: String = "",
    val interactions: List<InteractionResponse> = emptyList(),
)

@Serializable
private data class InteractionResponse(
    val id: Long,
    val timestampMs: Long,
    val notes: String?,
    val needs: String?,
    val wants: String?,
)

private fun RelationshipContext.toResponse() = RelationshipContextResponse(
    found = true,
    personName = person.displayName,
    personId = person.id,
    email = person.email,
    phone = person.phone,
    totalInteractions = totalInteractions,
    latestInteraction = latestInteractionSummary,
    needsSummary = needsSummary,
    wantsSummary = wantsSummary,
    interactions = interactions.map { i ->
        InteractionResponse(
            id = i.id,
            timestampMs = i.timestamp,
            notes = i.notes,
            needs = i.needs,
            wants = i.wants,
        )
    },
)
