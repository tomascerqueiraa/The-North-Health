# The North Health - app Android (Kotlin + Jetpack Compose + Firebase)

Primeiro incremento da proposta (secção 15): Preparador de Consultas/Exames + Gestor de Medicação.

## Antes de compilar (obrigatório)
1. Firebase Console -> criar projeto (região UE para o Firestore).
2. Adicionar app Android com o package **upca.pm.northhealth**.
3. Descarregar o **google-services.json** para `app/google-services.json`.
4. Authentication -> Sign-in method -> ativar **Email/Password**.
5. Firestore Database -> criar base de dados.
6. Firestore -> Rules -> colar `firestore.rules` e publicar.
7. Gradle 8.9 + JDK 21 (Settings -> Build Tools -> Gradle). Sync e Run num telemóvel/emulador com Android 8+ (minSdk 26).

## Estrutura
MainActivity.kt   navegação (Scaffold, NavHost, bottom bar com 4 separadores)
Helpers.kt        Firebase (Auth/Firestore), alarmes locais, notificações, voz (TTS), datas
models/           Medication, Intake, Appointment, ChecklistItem, ExamCatalog
ui/components/    Screen, StateViews, BigButton, SpeakButton, Row*, Dialogs, MessageToast
ui/home/          ViewModels e ecrãs
ui/theme/         Color, Type, Theme (letra ajustável e alto contraste)
