package com.aibot.crm.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aibot.crm.data.db.entities.Interaction
import com.aibot.crm.ui.viewmodel.CrmViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonDetailScreen(
    personId: Long,
    viewModel: CrmViewModel,
    onBack: () -> Unit,
) {
    LaunchedEffect(personId) { viewModel.selectPerson(personId) }

    val person by viewModel.selectedPerson.collectAsState()
    val interactions by viewModel.interactions.collectAsState()
    var showLogDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(person?.displayName ?: "Contact") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showLogDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Log interaction")
            }
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 88.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Contact info card
            person?.let { p ->
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Contact Info", style = MaterialTheme.typography.labelLarge)
                            Spacer(Modifier.height(8.dp))
                            p.email?.let { InfoRow("Email", it) }
                            p.phone?.let { InfoRow("Phone", it) }
                            p.notes?.let { InfoRow("Notes", it) }
                            InfoRow("Total interactions", "${interactions.size}")
                        }
                    }
                }
            }

            // Aggregated needs / wants
            val needsList = interactions.mapNotNull { it.needs }.filter { it.isNotBlank() }.distinct()
            val wantsList = interactions.mapNotNull { it.wants }.filter { it.isNotBlank() }.distinct()
            if (needsList.isNotEmpty() || wantsList.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        ),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Intelligence", style = MaterialTheme.typography.labelLarge)
                            Spacer(Modifier.height(8.dp))
                            if (needsList.isNotEmpty()) {
                                Text("Needs", style = MaterialTheme.typography.labelMedium)
                                needsList.forEach { Text("• $it") }
                                Spacer(Modifier.height(6.dp))
                            }
                            if (wantsList.isNotEmpty()) {
                                Text("Wants", style = MaterialTheme.typography.labelMedium)
                                wantsList.forEach { Text("• $it") }
                            }
                        }
                    }
                }
            }

            // Interaction history header
            item {
                Text(
                    "Interaction History",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            if (interactions.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "No interactions logged yet",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                items(interactions, key = { it.id }) { interaction ->
                    InteractionCard(interaction)
                }
            }
        }
    }

    if (showLogDialog) {
        LogInteractionDialog(
            initialName = person?.displayName ?: "",
            onDismiss = { showLogDialog = false },
            onConfirm = { name, notes, needs, wants ->
                viewModel.logInteraction(name, notes, needs, wants)
                showLogDialog = false
            },
        )
    }
}

@Composable
private fun InteractionCard(interaction: Interaction) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    formatDateTime(interaction.timestamp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            interaction.notes?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
            interaction.needs?.let {
                LabeledChip("Needs", it)
            }
            interaction.wants?.let {
                LabeledChip("Wants", it)
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun LabeledChip(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        SuggestionChip(
            onClick = {},
            label = { Text("$label: $value", style = MaterialTheme.typography.labelSmall) },
        )
    }
}

private fun formatDateTime(ms: Long): String =
    SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault()).format(Date(ms))
