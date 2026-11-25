package upca.pm.northhealth

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import upca.pm.northhealth.models.Appointment
import upca.pm.northhealth.models.ChecklistItem
import upca.pm.northhealth.models.ExamCatalog
import upca.pm.northhealth.models.ExamStep
import upca.pm.northhealth.models.Intake
import upca.pm.northhealth.models.Medication
import upca.pm.northhealth.models.isPending
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

// =====================================================================
//  Formatação de datas e horas (português europeu)
// =====================================================================

private val PT = Locale("pt", "PT")
private val zone: ZoneId get() = ZoneId.systemDefault()

fun formatTime(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(zone).format(DateTimeFormatter.ofPattern("HH:mm"))

fun formatDate(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(zone).format(DateTimeFormatter.ofPattern("d 'de' MMMM", PT))

fun formatDateTime(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(zone).format(DateTimeFormatter.ofPattern("d 'de' MMMM 'às' HH:mm", PT))

/** "2026-11-30" -> "30 de novembro de 2026" */
fun formatIsoDate(iso: String): String =
    LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", PT))

/** Junta uma data ISO ("2026-11-30") e uma hora ("08:30") em milissegundos. */
fun toMillis(isoDate: String, hhmm: String): Long =
    LocalDateTime.of(LocalDate.parse(isoDate), LocalTime.parse(hhmm))
        .atZone(zone).toInstant().toEpochMilli()

fun formatHm(hour: Int, minute: Int): String = String.format(Locale.US, "%02d:%02d", hour, minute)

fun startOfToday(): Long = LocalDate.now().atStartOfDay(zone).toInstant().toEpochMilli()

fun hourOf(millis: Long): Int = Instant.ofEpochMilli(millis).atZone(zone).hour

fun daysUntil(millis: Long): Long =
    ChronoUnit.DAYS.between(LocalDate.now(), Instant.ofEpochMilli(millis).atZone(zone).toLocalDate())

fun countdownText(millis: Long): String = when (val days = daysUntil(millis)) {
    in Long.MIN_VALUE..-1L -> "Já passou"
    0L -> "É hoje"
    1L -> "Falta 1 dia"
    else -> "Faltam $days dias"
}

fun deadlineText(dateTime: Long, daysBefore: Int): String {
    if (daysBefore == 0) return "No próprio dia"
    val date = Instant.ofEpochMilli(dateTime).atZone(zone).toLocalDate().minusDays(daysBefore.toLong())
    return "Até " + date.format(DateTimeFormatter.ofPattern("d 'de' MMMM", PT))
}

// =====================================================================
//  Firebase (Authentication + Cloud Firestore)
//
//  users/{uid}                                     perfil
//  users/{uid}/medications/{id}                    medicamentos
//  users/{uid}/intakes/{id}                        tomas
//  users/{uid}/appointments/{id}                   consultas e exames
//  users/{uid}/appointments/{id}/checklist/{id}    passos da preparação
//
//  O Firestore guarda cache offline: as escritas funcionam sem Internet e
//  são sincronizadas depois (RNF04). Por isso não esperamos pela
//  confirmação do servidor para continuar.
// =====================================================================

object FirebaseHelper {

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    val currentUid: String? get() = auth.currentUser?.uid
    val currentEmail: String? get() = auth.currentUser?.email
    val currentName: String? get() = auth.currentUser?.displayName

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    // ---------------------------------------------------------------- Auth

    fun login(email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        auth.signInWithEmailAndPassword(email.trim().lowercase(), password)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.toMessage()) }
    }

    /** Cria a conta, guarda o nome e regista a data do consentimento (RGPD). */
    fun register(
        name: String,
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val cleanEmail = email.trim().lowercase()
        auth.createUserWithEmailAndPassword(cleanEmail, password)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user == null) {
                    onError("Não foi possível criar a conta.")
                    return@addOnSuccessListener
                }
                val request = UserProfileChangeRequest.Builder().setDisplayName(name.trim()).build()
                user.updateProfile(request).addOnCompleteListener {
                    userDoc(user.uid)
                        .set(
                            hashMapOf<String, Any>(
                                "name" to name.trim(),
                                "email" to cleanEmail,
                                "consentAt" to System.currentTimeMillis()
                            )
                        )
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener { onError(it.toMessage()) }
                }
            }
            .addOnFailureListener {
                if (it is FirebaseAuthInvalidCredentialsException) onError("O email não é válido.")
                else onError(it.toMessage())
            }
    }

    fun logout() {
        auth.signOut()
    }

    // ----------------------------------------------------------- Listeners
    // addSnapshotListener: a UI atualiza sozinha sempre que os dados mudam.

    fun listenMedications(
        uid: String,
        onData: (List<Medication>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration =
        userDoc(uid).collection("medications").orderBy("createdAt")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error.toMessage())
                    return@addSnapshotListener
                }
                onData(snapshot?.documents?.mapNotNull { it.toObject(Medication::class.java) }.orEmpty())
            }

    fun listenIntakes(
        uid: String,
        onData: (List<Intake>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration =
        userDoc(uid).collection("intakes").orderBy("scheduledAt")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error.toMessage())
                    return@addSnapshotListener
                }
                onData(snapshot?.documents?.mapNotNull { it.toObject(Intake::class.java) }.orEmpty())
            }

    fun listenAppointments(
        uid: String,
        onData: (List<Appointment>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration =
        userDoc(uid).collection("appointments").orderBy("dateTime")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error.toMessage())
                    return@addSnapshotListener
                }
                onData(snapshot?.documents?.mapNotNull { it.toObject(Appointment::class.java) }.orEmpty())
            }

    fun listenChecklist(
        uid: String,
        appointmentId: String,
        onData: (List<ChecklistItem>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration =
        userDoc(uid).collection("appointments").document(appointmentId)
            .collection("checklist").orderBy("order")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error.toMessage())
                    return@addSnapshotListener
                }
                onData(snapshot?.documents?.mapNotNull { it.toObject(ChecklistItem::class.java) }.orEmpty())
            }

    // ---------------------------------------------------------- Medicação

    /** Cria o medicamento e todas as tomas previstas num único batch. */
    fun addMedication(
        uid: String,
        medication: Map<String, Any>,
        intakes: List<Map<String, Any>>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val medRef = userDoc(uid).collection("medications").document()
        val batch = db.batch()
        batch.set(medRef, medication)
        intakes.forEach { data ->
            batch.set(
                userDoc(uid).collection("intakes").document(),
                data + ("medicationId" to medRef.id)
            )
        }
        batch.commit().addOnFailureListener { onError(it.toMessage()) }
        onSuccess()
    }

    /** Apaga o medicamento e as respetivas tomas. */
    fun deleteMedication(uid: String, medicationId: String, onError: (String) -> Unit) {
        val medRef = userDoc(uid).collection("medications").document(medicationId)
        userDoc(uid).collection("intakes").whereEqualTo("medicationId", medicationId).get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()
                snapshot.documents.forEach { batch.delete(it.reference) }
                batch.delete(medRef)
                batch.commit().addOnFailureListener { onError(it.toMessage()) }
            }
            .addOnFailureListener { onError(it.toMessage()) }
    }

    /** Atualiza apenas alguns campos de uma toma (estado, hora real, próximo alarme). */
    fun updateIntake(uid: String, intakeId: String, fields: Map<String, Any>, onError: (String) -> Unit) {
        userDoc(uid).collection("intakes").document(intakeId).update(fields)
            .addOnFailureListener { onError(it.toMessage()) }
    }

    // ------------------------------------------------- Consultas e exames

    /** Cria a marcação e a checklist de preparação (um documento por passo). */
    fun addAppointment(
        uid: String,
        type: String,
        place: String,
        dateTime: Long,
        steps: List<ExamStep>,
        onSuccess: (appointmentId: String) -> Unit,
        onError: (String) -> Unit
    ) {
        val apptRef = userDoc(uid).collection("appointments").document()
        val batch = db.batch()
        batch.set(
            apptRef,
            hashMapOf<String, Any>(
                "type" to type,
                "place" to place,
                "dateTime" to dateTime,
                "totalSteps" to steps.size,
                "doneSteps" to 0,
                "createdAt" to System.currentTimeMillis()
            )
        )
        steps.forEachIndexed { index, step ->
            batch.set(
                apptRef.collection("checklist").document(),
                hashMapOf<String, Any>(
                    "text" to step.text,
                    "daysBefore" to step.daysBefore,
                    "done" to false,
                    "order" to index
                )
            )
        }
        batch.commit().addOnFailureListener { onError(it.toMessage()) }
        onSuccess(apptRef.id)
    }

    fun deleteAppointment(uid: String, appointmentId: String, onError: (String) -> Unit) {
        val apptRef = userDoc(uid).collection("appointments").document(appointmentId)
        apptRef.collection("checklist").get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()
                snapshot.documents.forEach { batch.delete(it.reference) }
                batch.delete(apptRef)
                batch.commit().addOnFailureListener { onError(it.toMessage()) }
            }
            .addOnFailureListener { onError(it.toMessage()) }
    }

    /** Marca/desmarca um passo e mantém o contador de progresso da marcação. */
    fun toggleChecklistItem(
        uid: String,
        appointmentId: String,
        itemId: String,
        done: Boolean,
        onError: (String) -> Unit
    ) {
        val apptRef = userDoc(uid).collection("appointments").document(appointmentId)
        val batch = db.batch()
        batch.update(apptRef.collection("checklist").document(itemId), "done", done)
        batch.update(apptRef, "doneSteps", FieldValue.increment(if (done) 1L else -1L))
        batch.commit().addOnFailureListener { onError(it.toMessage()) }
    }

    // --------------------------------------------------------------- Erros

    private fun Exception.toMessage(): String = when (this) {
        is FirebaseAuthInvalidUserException,
        is FirebaseAuthInvalidCredentialsException -> "Email ou palavra-passe incorretos."
        is FirebaseAuthUserCollisionException -> "Já existe uma conta com este email."
        is FirebaseAuthWeakPasswordException -> "A palavra-passe é demasiado fraca (mínimo 6 caracteres)."
        is FirebaseNetworkException -> "Sem ligação à internet."
        is FirebaseFirestoreException -> when (code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                "Sem permissão para esta operação (verifica as regras do Firestore)."
            FirebaseFirestoreException.Code.UNAVAILABLE -> "Sem ligação ao servidor."
            else -> "Erro do Firestore (${code.name})."
        }
        else -> message ?: "Ocorreu um erro inesperado."
    }
}

// =====================================================================
//  Lembretes locais (AlarmManager + notificações)
//  Funcionam com a app fechada e sem Internet (RNF03, RNF04).
// =====================================================================

const val EXTRA_KIND = "kind"
const val EXTRA_ID = "id"
const val EXTRA_TITLE = "title"
const val EXTRA_TEXT = "text"
const val KIND_INTAKE = "intake"
const val KIND_EXAM = "exam"

private const val CHANNEL_ID = "reminders"
private const val ACTION_REMINDER = "upca.pm.northhealth.REMINDER"
private const val TWO_HOURS_MS = 2 * 60 * 60 * 1000L

object ReminderScheduler {

    private fun alarmManager(context: Context) =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun intakeCode(intakeId: String) = "intake:$intakeId".hashCode()
    private fun examCode(appointmentId: String, index: Int) = "exam:$appointmentId:$index".hashCode()

    /** Android 12+: alarmes exatos precisam de autorização do utilizador. */
    fun canScheduleExact(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager(context).canScheduleExactAlarms()

    private fun schedule(
        context: Context,
        code: Int,
        triggerAt: Long,
        kind: String,
        id: String,
        title: String,
        text: String
    ) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_REMINDER
            putExtra(EXTRA_KIND, kind)
            putExtra(EXTRA_ID, id)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_TEXT, text)
        }
        val pending = PendingIntent.getBroadcast(
            context, code, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val am = alarmManager(context)
        try {
            if (canScheduleExact(context)) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            }
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    private fun cancel(context: Context, code: Int) {
        val intent = Intent(context, ReminderReceiver::class.java).apply { action = ACTION_REMINDER }
        val pending = PendingIntent.getBroadcast(
            context, code, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pending != null) {
            alarmManager(context).cancel(pending)
            pending.cancel()
        }
    }

    /** Alinha os alarmes com o estado atual das tomas: só as pendentes têm alarme. */
    fun syncIntakes(context: Context, intakes: List<Intake>) {
        val now = System.currentTimeMillis()
        intakes.forEach { intake ->
            val code = intakeCode(intake.id)
            if (intake.isPending() && intake.remindAt > now) {
                schedule(
                    context, code, intake.remindAt, KIND_INTAKE, intake.id,
                    "💊 Hora da toma", "${intake.medicationName} · ${intake.dose}"
                )
            } else {
                cancel(context, code)
            }
        }
    }

    fun cancelIntakes(context: Context, intakes: List<Intake>) {
        intakes.forEach { cancel(context, intakeCode(it.id)) }
    }

    /** Lembretes dos passos de preparação (calculados a partir do tipo de exame). */
    fun syncAppointments(context: Context, appointments: List<Appointment>) {
        val now = System.currentTimeMillis()
        appointments.forEach { appt ->
            if (appt.dateTime <= now) {
                cancelAppointment(context, appt)
                return@forEach
            }
            val steps = ExamCatalog.orderedSteps(ExamCatalog.find(appt.type))
            val examDate = Instant.ofEpochMilli(appt.dateTime).atZone(zone).toLocalDate()
            steps.forEachIndexed { index, step ->
                var at = examDate.minusDays(step.daysBefore.toLong())
                    .atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
                if (at >= appt.dateTime) at = appt.dateTime - TWO_HOURS_MS
                if (at > now) {
                    schedule(context, examCode(appt.id, index), at, KIND_EXAM, appt.id, "📋 Preparar: ${appt.type}", step.text)
                } else {
                    cancel(context, examCode(appt.id, index))
                }
            }
            // Lembrete final, 2 horas antes
            val finalAt = appt.dateTime - TWO_HOURS_MS
            if (finalAt > now) {
                schedule(
                    context, examCode(appt.id, steps.size), finalAt, KIND_EXAM, appt.id,
                    "🩺 Hoje tens: ${appt.type}",
                    "Às ${formatTime(appt.dateTime)}, em ${appt.place.ifBlank { "local a confirmar" }}. Leva os documentos."
                )
            }
        }
    }

    fun cancelAppointment(context: Context, appointment: Appointment) {
        val steps = ExamCatalog.orderedSteps(ExamCatalog.find(appointment.type))
        for (index in 0..steps.size) cancel(context, examCode(appointment.id, index))
    }

    @SuppressLint("MissingPermission")
    fun showNotification(context: Context, kind: String, id: String, title: String, text: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Lembretes", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Lembretes de tomas, consultas e exames"
            }
        )

        val open = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(EXTRA_KIND, kind)
            putExtra(EXTRA_ID, id)
        }
        val content = PendingIntent.getActivity(
            context, "open:$kind:$id".hashCode(), open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(content)
            .build()
        NotificationManagerCompat.from(context).notify("$kind:$id".hashCode(), notification)
    }
}

/** Recebe o alarme e mostra a notificação (mesmo com a app fechada). */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val kind = intent.getStringExtra(EXTRA_KIND) ?: return
        val id = intent.getStringExtra(EXTRA_ID) ?: return
        ReminderScheduler.showNotification(
            context, kind, id,
            intent.getStringExtra(EXTRA_TITLE).orEmpty(),
            intent.getStringExtra(EXTRA_TEXT).orEmpty()
        )
    }
}

// =====================================================================
//  Leitura por voz (Android TextToSpeech)
// =====================================================================

class Speaker(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context, this)
    private var ready = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("pt", "PT")
            ready = true
        }
    }

    fun speak(text: String) {
        if (ready) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "north-health")
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
    }
}
