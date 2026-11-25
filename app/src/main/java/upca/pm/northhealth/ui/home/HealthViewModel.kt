package upca.pm.northhealth.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import upca.pm.northhealth.FirebaseHelper
import upca.pm.northhealth.ReminderScheduler
import upca.pm.northhealth.models.Appointment
import upca.pm.northhealth.models.ExamCatalog
import upca.pm.northhealth.models.ExamType
import upca.pm.northhealth.models.FORGOTTEN_AFTER_MS
import upca.pm.northhealth.models.Intake
import upca.pm.northhealth.models.IntakeState
import upca.pm.northhealth.models.Medication
import upca.pm.northhealth.models.SNOOZE_MS
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** Todos os dados do utilizador, sincronizados em tempo real. */
data class HealthData(
    val medications: List<Medication> = emptyList(),
    val intakes: List<Intake> = emptyList(),
    val appointments: List<Appointment> = emptyList()
)

sealed interface HealthUiState {
    data object Loading : HealthUiState
    data class Error(val message: String) : HealthUiState
    data class Success(val data: HealthData) : HealthUiState
}

/**
 * ViewModel partilhado por todos os ecrãs (âmbito da Activity).
 * Regista 3 addSnapshotListener (medicamentos, tomas, consultas/exames) e
 * mantém os alarmes locais alinhados com os dados.
 */
class HealthViewModel(application: Application) : AndroidViewModel(application) {

    private val listeners = mutableListOf<ListenerRegistration>()
    private var listeningUid: String? = null

    private var medications: List<Medication>? = null
    private var intakes: List<Intake>? = null
    private var appointments: List<Appointment>? = null
    private var error: String? = null

    private val _uiState = MutableStateFlow<HealthUiState>(HealthUiState.Loading)
    val uiState: StateFlow<HealthUiState> = _uiState.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    // ------------------------------------------------------- Listeners

    fun start(uid: String) {
        if (listeningUid == uid) return
        stop()
        listeningUid = uid

        listeners += FirebaseHelper.listenMedications(
            uid,
            onData = { medications = it; publish() },
            onError = { error = it; publish() }
        )
        listeners += FirebaseHelper.listenIntakes(
            uid,
            onData = { list ->
                intakes = list
                ReminderScheduler.syncIntakes(getApplication(), list)
                publish()
            },
            onError = { error = it; publish() }
        )
        listeners += FirebaseHelper.listenAppointments(
            uid,
            onData = { list ->
                appointments = list
                ReminderScheduler.syncAppointments(getApplication(), list)
                publish()
            },
            onError = { error = it; publish() }
        )
    }

    /** Termina a sessão de dados: remove listeners e alarmes (privacidade). */
    fun stop() {
        listeners.forEach { it.remove() }
        listeners.clear()
        intakes?.let { ReminderScheduler.cancelIntakes(getApplication(), it) }
        appointments?.forEach { ReminderScheduler.cancelAppointment(getApplication(), it) }
        medications = null
        intakes = null
        appointments = null
        error = null
        listeningUid = null
        _uiState.value = HealthUiState.Loading
    }

    fun retry() {
        val uid = listeningUid ?: return
        listeningUid = null
        start(uid)
    }

    private fun publish() {
        error?.let {
            _uiState.value = HealthUiState.Error(it)
            return
        }
        val m = medications
        val i = intakes
        val a = appointments
        if (m != null && i != null && a != null) {
            _uiState.value = HealthUiState.Success(HealthData(m, i, a))
        }
    }

    // ------------------------------------------------------- Medicação

    fun addMedication(
        name: String,
        substance: String,
        form: String,
        dose: String,
        times: List<String>,
        days: Int,
        onDone: () -> Unit
    ) {
        val uid = listeningUid ?: return
        val now = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()

        // Gera uma toma por dia e por hora, só no futuro
        val intakeMaps = mutableListOf<Map<String, Any>>()
        for (day in 0 until days) {
            for (time in times) {
                val at = LocalDateTime.of(today.plusDays(day.toLong()), LocalTime.parse(time))
                    .atZone(zone).toInstant().toEpochMilli()
                if (at > now) {
                    intakeMaps += hashMapOf<String, Any>(
                        "medicationName" to name,
                        "dose" to dose,
                        "scheduledAt" to at,
                        "remindAt" to at,
                        "takenAt" to 0L,
                        "state" to IntakeState.AGENDADA
                    )
                }
            }
        }

        FirebaseHelper.addMedication(
            uid = uid,
            medication = hashMapOf<String, Any>(
                "name" to name,
                "activeSubstance" to substance,
                "form" to form,
                "dose" to dose,
                "times" to times,
                "durationDays" to days,
                "createdAt" to now
            ),
            intakes = intakeMaps,
            onSuccess = onDone,
            onError = { _message.value = it }
        )
    }

    fun deleteMedication(medication: Medication) {
        val uid = listeningUid ?: return
        FirebaseHelper.deleteMedication(uid, medication.id) { _message.value = it }
    }

    /** «Já tomei»: passa a Tomada (ou Tomada com atraso, se passou mais de 60 minutos). */
    fun confirmTaken(intake: Intake) {
        val uid = listeningUid ?: return
        val now = System.currentTimeMillis()
        val state =
            if (now > intake.scheduledAt + FORGOTTEN_AFTER_MS) IntakeState.TOMADA_COM_ATRASO
            else IntakeState.TOMADA
        FirebaseHelper.updateIntake(
            uid, intake.id,
            hashMapOf<String, Any>("state" to state, "takenAt" to now)
        ) { _message.value = it }
    }

    /** «Lembrar daqui»: toma Adiada e novo alarme passados 15 minutos. */
    fun snooze(intake: Intake) {
        val uid = listeningUid ?: return
        FirebaseHelper.updateIntake(
            uid, intake.id,
            hashMapOf<String, Any>(
                "state" to IntakeState.ADIADA,
                "remindAt" to System.currentTimeMillis() + SNOOZE_MS
            )
        ) { _message.value = it }
    }

    /** «Hoje não vou tomar»: toma Dispensada. */
    fun skip(intake: Intake) {
        val uid = listeningUid ?: return
        FirebaseHelper.updateIntake(
            uid, intake.id,
            hashMapOf<String, Any>("state" to IntakeState.DISPENSADA)
        ) { _message.value = it }
    }

    // ------------------------------------------------ Consultas e exames

    fun addAppointment(type: ExamType, place: String, dateTime: Long, onDone: (String) -> Unit) {
        val uid = listeningUid ?: return
        if (dateTime <= System.currentTimeMillis()) {
            _message.value = "A data já passou. Escolhe uma data futura."
            return
        }
        FirebaseHelper.addAppointment(
            uid = uid,
            type = type.name,
            place = place.trim(),
            dateTime = dateTime,
            steps = ExamCatalog.orderedSteps(type),
            onSuccess = onDone,
            onError = { _message.value = it }
        )
    }

    fun deleteAppointment(appointment: Appointment) {
        val uid = listeningUid ?: return
        ReminderScheduler.cancelAppointment(getApplication(), appointment)
        FirebaseHelper.deleteAppointment(uid, appointment.id) { _message.value = it }
    }

    fun clearMessage() {
        _message.value = null
    }

    override fun onCleared() {
        listeners.forEach { it.remove() }
        listeners.clear()
        super.onCleared()
    }
}
