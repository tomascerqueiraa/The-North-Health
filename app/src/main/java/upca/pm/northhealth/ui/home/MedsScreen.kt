package upca.pm.northhealth.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import upca.pm.northhealth.hourOf
import upca.pm.northhealth.models.Intake
import upca.pm.northhealth.models.Medication
import upca.pm.northhealth.startOfToday
import upca.pm.northhealth.ui.components.RowIntake
import upca.pm.northhealth.ui.components.RowMedication

/** Ecrã 2: Calendário de tomas (por período do dia) e lista de medicamentos. */
@Composable
fun MedsScreen(
    uiState: HealthUiState,
    onRetry: () -> Unit,
    onOpenIntake: (Intake) -> Unit,
    onOpenMedication: (Medication) -> Unit,
    onAddMedication: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        HealthStateHost(uiState, onRetry) { data ->
            val now = System.currentTimeMillis()
            val dayStart = startOfToday()
            val dayEnd = dayStart + 24 * 60 * 60 * 1000L
            val today = data.intakes.filter { it.scheduledAt in dayStart until dayEnd }
            val periods = listOf(
                "Manhã" to today.filter { hourOf(it.scheduledAt) < 12 },
                "Tarde" to today.filter { hourOf(it.scheduledAt) in 12..18 },
                "Noite" to today.filter { hourOf(it.scheduledAt) >= 19 }
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Text("Tomas de hoje", style = MaterialTheme.typography.headlineSmall) }

                if (today.isEmpty()) {
                    item { Text("Não tens tomas para hoje.", style = MaterialTheme.typography.bodyLarge) }
                }
                periods.forEach { (title, list) ->
                    if (list.isNotEmpty()) {
                        item(key = "period-$title") { Text(title, style = MaterialTheme.typography.titleLarge) }
                        items(items = list, key = { "intake-${it.id}" }) { intake ->
                            RowIntake(intake = intake, now = now, onClick = { onOpenIntake(intake) })
                        }
                    }
                }

                item(key = "meds-title") {
                    Text(
                        "Os meus medicamentos",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
                if (data.medications.isEmpty()) {
                    item(key = "meds-empty") {
                        Text(
                            "Ainda não registaste medicamentos.\nToca em + para adicionar o primeiro.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                items(items = data.medications, key = { "med-${it.id}" }) { medication ->
                    RowMedication(medication = medication, onClick = { onOpenMedication(medication) })
                }
            }
        }

        FloatingActionButton(
            onClick = onAddMedication,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(64.dp)
        ) {
            Text("+", style = MaterialTheme.typography.headlineMedium)
        }
    }
}
