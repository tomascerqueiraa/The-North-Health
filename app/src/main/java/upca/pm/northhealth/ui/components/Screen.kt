package upca.pm.northhealth.ui.components

sealed class Screen(val route: String, val title: String) {

    data object Login : Screen("login", "Entrar")

    // Ecrãs base (separadores da bottom bar: Hoje, Medicação, Exames e Perfil)
    data object Today : Screen("today", "Hoje")
    data object Meds : Screen("meds", "Medicação")
    data object Exams : Screen("exams", "Exames")
    data object Profile : Screen("profile", "Perfil")

    // Ecrãs de detalhe
    data object AddMed : Screen("add_med", "Novo medicamento")
    data object AddExam : Screen("add_exam", "Nova consulta ou exame")
    data object AlarmSigns : Screen("alarm_signs", "Sinais de alarme")

    data object MedDetail : Screen("med/{medId}", "Medicamento") {
        const val ARG_ID = "medId"
        fun createRoute(id: String) = "med/$id"
    }

    data object ExamDetail : Screen("exam/{examId}", "Preparar exame") {
        const val ARG_ID = "examId"
        fun createRoute(id: String) = "exam/$id"
    }

    data object Reminder : Screen("reminder/{intakeId}", "Lembrete de toma") {
        const val ARG_ID = "intakeId"
        fun createRoute(id: String) = "reminder/$id"
    }
}

/** Lista fora da classe (evita problemas de ordem de inicialização dos objetos). */
val allScreens: List<Screen> = listOf(
    Screen.Login, Screen.Today, Screen.Meds, Screen.Exams, Screen.Profile,
    Screen.AddMed, Screen.AddExam, Screen.AlarmSigns,
    Screen.MedDetail, Screen.ExamDetail, Screen.Reminder
)

val baseScreens: List<Screen> = listOf(Screen.Today, Screen.Meds, Screen.Exams, Screen.Profile)
