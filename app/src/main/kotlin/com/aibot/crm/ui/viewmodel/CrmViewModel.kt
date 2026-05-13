package com.aibot.crm.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aibot.crm.data.db.CrmDatabase
import com.aibot.crm.data.db.entities.Interaction
import com.aibot.crm.data.db.entities.Person
import com.aibot.crm.data.repository.CrmRepository
import com.aibot.crm.data.repository.LogResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CrmViewModel(app: Application) : AndroidViewModel(app) {

    private val db = CrmDatabase.getInstance(app)
    private val repository = CrmRepository(db.personDao(), db.interactionDao())

    val persons: StateFlow<List<Person>> = repository.observeAllPersons()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Detail screen state
    private val _selectedPersonId = MutableStateFlow<Long?>(null)
    val selectedPersonId = _selectedPersonId.asStateFlow()

    private val _selectedPerson = MutableStateFlow<Person?>(null)
    val selectedPerson = _selectedPerson.asStateFlow()

    private val _interactions = MutableStateFlow<List<Interaction>>(emptyList())
    val interactions = _interactions.asStateFlow()

    private val _snackMessage = MutableStateFlow<String?>(null)
    val snackMessage = _snackMessage.asStateFlow()

    fun selectPerson(id: Long) {
        _selectedPersonId.value = id
        viewModelScope.launch {
            _selectedPerson.value = repository.getPersonById(id)
            repository.observeInteractionsForPerson(id).collect { list ->
                _interactions.value = list
            }
        }
    }

    fun logInteraction(
        personName: String,
        notes: String,
        needs: String,
        wants: String,
        timestamp: Long = System.currentTimeMillis(),
    ) {
        viewModelScope.launch {
            when (val r = repository.logInteraction(
                personName = personName,
                notes = notes.takeIf { it.isNotBlank() },
                needs = needs.takeIf { it.isNotBlank() },
                wants = wants.takeIf { it.isNotBlank() },
                timestamp = timestamp,
            )) {
                is LogResult.Success ->
                    _snackMessage.value = "Logged interaction with ${r.person.displayName}"
                is LogResult.Error ->
                    _snackMessage.value = "Error: ${r.message}"
            }
        }
    }

    fun deletePerson(person: Person) {
        viewModelScope.launch {
            repository.deletePerson(person)
            _snackMessage.value = "${person.displayName} deleted"
        }
    }

    fun clearSnack() {
        _snackMessage.value = null
    }
}
