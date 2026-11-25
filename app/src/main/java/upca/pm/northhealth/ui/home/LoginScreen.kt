package upca.pm.northhealth.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import upca.pm.northhealth.ui.components.BigButton

/** Ecrãs 9 e 10: primeira utilização (nome) e privacidade/consentimento. */
@Composable
fun LoginScreen(
    uiState: LoginUiState,
    onLogin: (email: String, password: String) -> Unit,
    onRegister: (name: String, email: String, password: String, consent: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var registerMode by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var consent by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🩺", style = MaterialTheme.typography.headlineMedium)
        Text("The North Health", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            if (registerMode) "Cria a tua conta" else "Entra para ver as tuas tomas e exames",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))

        if (registerMode) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("O teu nome") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
        }
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Palavra-passe") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )

        if (registerMode) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Top) {
                Checkbox(checked = consent, onCheckedChange = { consent = it })
                Text(
                    "Aceito que os meus dados de saúde (medicamentos, tomas e exames) sejam guardados " +
                        "no Firebase, na União Europeia, e que só eu tenho acesso a eles.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }

        uiState.error?.let { error ->
            Spacer(Modifier.height(8.dp))
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.height(16.dp))
        if (uiState.loading) {
            CircularProgressIndicator()
        } else {
            BigButton(
                text = if (registerMode) "Criar conta" else "Entrar",
                onClick = {
                    if (registerMode) onRegister(name, email, password, consent)
                    else onLogin(email, password)
                }
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { registerMode = !registerMode }) {
                Text(if (registerMode) "Já tenho conta" else "Criar conta nova")
            }
        }
    }
}
