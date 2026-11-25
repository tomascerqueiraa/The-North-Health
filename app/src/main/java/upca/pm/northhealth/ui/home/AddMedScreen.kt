package upca.pm.northhealth.ui.home

import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import upca.pm.northhealth.formatHm
import upca.pm.northhealth.models.Medication
import upca.pm.northhealth.ui.components.BigButton
import upca.pm.northhealth.ui.components.ButtonKind
import upca.pm.northhealth.ui.components.ConfirmDialog

private val FORMS = listOf("Comprimido", "Cápsula", "Líquido", "Injeção", "Outro")

/** Ecrã 12: Novo medicamento (nome, forma, dose e horas). */
@Composable
fun AddMedScreen(
    existing: List<Medication>,
    onSave: (name: String, substance: String, form: String, dose: String, times: List<String>, days: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var name by rememberSaveable { mutableStateOf("") }
    var substance by rememberSaveable { mutableStateOf("") }
    var formIndex by rememberSaveable { mutableStateOf(0) }
    var dose by rememberSaveable { mutableStateOf("") }
    var timesText by rememberSaveable { mutableStateOf("08:00") } // "08:00,20:00"
    var daysText by rememberSaveable { mutableStateOf("7") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var duplicate by remember { mutableStateOf<Medication?>(null) }

    val times = timesText.split(",").filter { it.isNotBlank() }.sorted()

    fun save() {
        onSave(
            name.trim(), substance.trim(), FORMS[formIndex], dose.trim(),
            times, daysText.toIntOrNull() ?: 7
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nome do medicamento") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = substance,
            onValueChange = { substance = it },
            label = { Text("Substância ativa (opcional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Text("Forma", style = MaterialTheme.typography.titleMedium)
        FORMS.forEachIndexed { index, form ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { formIndex = index }
                    .heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = formIndex == index, onClick = { formIndex = index })
                Text(form, style = MaterialTheme.typography.bodyLarge)
            }
        }

        OutlinedTextField(
            value = dose,
            onValueChange = { dose = it },
            label = { Text("Dose (ex.: 1 comprimido)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Text("Horas da toma", style = MaterialTheme.typography.titleMedium)
        times.forEach { time ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("⏰ $time", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = { timesText = (times - time).joinToString(",") }) {
                    Text("Remover")
                }
            }
        }
        BigButton(
            text = "+ Adicionar hora",
            kind = ButtonKind.Outlined,
            onClick = {
                TimePickerDialog(context, { _, hour, minute ->
                    val picked = formatHm(hour, minute)
                    if (picked !in times) timesText = (times + picked).joinToString(",")
                }, 8, 0, true).show()
            }
        )

        OutlinedTextField(
            value = daysText,
            onValueChange = { value -> daysText = value.filter { it.isDigit() }.take(2) },
            label = { Text("Durante quantos dias? (1 a 30)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        BigButton(
            text = "Guardar",
            onClick = {
                val days = daysText.toIntOrNull() ?: 0
                when {
                    name.isBlank() -> error = "Escreve o nome do medicamento."
                    dose.isBlank() -> error = "Escreve a dose."
                    times.isEmpty() -> error = "Adiciona pelo menos uma hora."
                    days !in 1..30 -> error = "Escolhe entre 1 e 30 dias."
                    else -> {
                        error = null
                        // RF19: avisar se já existe outro medicamento com a mesma substância ativa
                        val same = existing.firstOrNull {
                            substance.isNotBlank() && it.activeSubstance.equals(substance.trim(), ignoreCase = true)
                        }
                        if (same != null) duplicate = same else save()
                    }
                }
            }
        )
    }

    duplicate?.let { other ->
        ConfirmDialog(
            title = "Atenção",
            text = "Já tens o medicamento ${other.name} com a mesma substância ativa (${other.activeSubstance}). " +
                "Tomar os dois pode ser perigoso. Queres guardar mesmo assim?",
            confirmText = "Guardar",
            onConfirm = {
                duplicate = null
                save()
            },
            onDismiss = { duplicate = null }
        )
    }
}
