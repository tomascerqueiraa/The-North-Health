package upca.pm.northhealth.models

/**
 * ATENÇÃO: conteúdo de EXEMPLO para o protótipo.
 * As instruções reais de preparação serão fornecidas e validadas pela ULSBE
 * (RF18 / RNF09) e carregadas no Firebase. Até lá, a app mostra este aviso.
 */
const val CONTENT_DISCLAIMER =
    "Conteúdo de exemplo. As instruções oficiais serão fornecidas e validadas pela ULSBE. " +
        "Segue sempre as indicações que recebeste do hospital."

data class ExamStep(val text: String, val daysBefore: Int)

data class ExamType(val name: String, val emoji: String, val steps: List<ExamStep>)

object ExamCatalog {

    // Passos comuns a todas as consultas e exames
    private val commonSteps = listOf(
        ExamStep("Levar o cartão de utente e um documento de identificação.", 1),
        ExamStep("Levar a lista dos teus medicamentos.", 1),
        ExamStep("Confirmar o local e a hora, e chegar 15 minutos antes.", 1)
    )

    val types: List<ExamType> = listOf(
        ExamType(
            "Consulta", "🩺",
            listOf(ExamStep("Escrever as dúvidas que queres colocar ao médico.", 1))
        ),
        ExamType(
            "Análises ao sangue", "🩸",
            listOf(
                ExamStep("Fazer o jejum indicado pela ULSBE (exemplo: 8 a 12 horas, a confirmar).", 0),
                ExamStep("Perguntar à ULSBE se podes tomar os teus medicamentos de manhã.", 2)
            )
        ),
        ExamType(
            "Ecografia", "🖥️",
            listOf(ExamStep("Seguir a preparação indicada pela ULSBE (jejum ou bexiga cheia, se pedido).", 0))
        ),
        ExamType(
            "TAC", "🧲",
            listOf(
                ExamStep("Confirmar com a ULSBE se precisas de jejum ou de análises recentes.", 2),
                ExamStep("Levar exames anteriores, se os tiveres.", 1)
            )
        ),
        ExamType(
            "Colonoscopia", "🔬",
            listOf(
                ExamStep("Perguntar à ULSBE se deves suspender algum medicamento.", 5),
                ExamStep("Seguir a dieta e a preparação indicadas pela ULSBE.", 3),
                ExamStep("Combinar quem te acompanha no dia do exame.", 2)
            )
        ),
        ExamType("Outro", "📋", emptyList())
    )

    fun find(name: String): ExamType = types.firstOrNull { it.name == name } ?: types.last()

    /** Passos por ordem cronológica (os mais cedo primeiro). A ordem é estável. */
    fun orderedSteps(type: ExamType): List<ExamStep> =
        (type.steps + commonSteps).sortedByDescending { it.daysBefore }
}
