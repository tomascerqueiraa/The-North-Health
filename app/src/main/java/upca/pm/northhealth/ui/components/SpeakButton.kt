package upca.pm.northhealth.ui.components

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import upca.pm.northhealth.Speaker

/** Cria um Speaker (TextToSpeech) ligado ao ciclo de vida do ecrã. */
@Composable
fun rememberSpeaker(): Speaker {
    val context = LocalContext.current
    val speaker = remember { Speaker(context.applicationContext) }
    DisposableEffect(speaker) {
        onDispose { speaker.shutdown() }
    }
    return speaker
}

/** Botão «Ouvir»: lê o texto em voz alta (RF05, RF13). */
@Composable
fun SpeakButton(text: String, speaker: Speaker, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = { speaker.speak(text) },
        modifier = modifier.heightIn(min = 48.dp)
    ) {
        Text("🔊 Ouvir")
    }
}
