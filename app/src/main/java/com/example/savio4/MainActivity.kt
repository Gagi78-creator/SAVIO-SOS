package com.example.savio4

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.speech.tts.TextToSpeech
import android.telephony.SmsManager
import android.view.Gravity
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var mainContainer: LinearLayout
    private lateinit var status: TextView
    private var tts: TextToSpeech? = null

    private val sosNumbers = listOf(
        "+381652013323",
        "+381691604996"
    )

    private val permissionRequestCode = 101
    private val deniedPermissionsMap = mutableMapOf<String, Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mainContainer = findViewById(R.id.mainContainer)
        mainContainer.removeAllViews()

        requestNeededPermissions()
        requestBatteryOptimizationExemption()
        buildHomeScreen()
        applyWindowInsets()
    }

    // ─────────────────────────────────────────────
    // DOZVOLE
    // ─────────────────────────────────────────────

    private fun requestNeededPermissions() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.SEND_SMS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.POST_NOTIFICATIONS
        )
        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), permissionRequestCode)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != permissionRequestCode) return

        val deniedPermissions = permissions.filterIndexed { index, _ ->
            grantResults[index] != PackageManager.PERMISSION_GRANTED
        }
        if (deniedPermissions.isEmpty()) return

        deniedPermissions.forEach { permission ->
            deniedPermissionsMap[permission] = (deniedPermissionsMap[permission] ?: 0) + 1
        }

        val shouldOpenSettings = deniedPermissions.any { permission ->
            (deniedPermissionsMap[permission] ?: 0) >= 2
        }

        if (shouldOpenSettings) {
            showOpenSettingsDialog(deniedPermissions)
        } else {
            showPermissionRationaleDialog(deniedPermissions)
        }
    }

    private fun showPermissionRationaleDialog(deniedPermissions: List<String>) {
        val permissionNames = deniedPermissions.joinToString("\n") { "• " + localizePermissionName(it) }
        AlertDialog.Builder(this)
            .setTitle(t("⚠️ POTREBNE DOZVOLE", "⚠️ REQUIRED PERMISSIONS", "⚠️ НЕОБХОДИМЫЕ РАЗРЕШЕНИЯ", "⚠️ ERFORDERLICHE BERECHTIGUNGEN"))
            .setMessage(t(
                "Aplikacija NEĆE ispravno raditi bez sledećih dozvola:\n\n$permissionNames\n\nMolimo odobrite dozvole.",
                "The application will NOT work correctly without:\n\n$permissionNames\n\nPlease grant the permissions.",
                "Приложение НЕ будет работать без:\n\n$permissionNames\n\nПожалуйста, предоставьте разрешения.",
                "Die Anwendung funktioniert NICHT ohne:\n\n$permissionNames\n\nBitte erteilen Sie die Berechtigungen."
            ))
            .setPositiveButton(t("ODOBRI", "GRANT", "РАЗРЕШИТЬ", "ERLAUBEN")) { _, _ -> requestNeededPermissions() }
            .setNegativeButton(t("ZATVORI", "CLOSE", "ЗАКРЫТЬ", "SCHLIESSEN"), null)
            .setCancelable(false)
            .show()
    }

    private fun showOpenSettingsDialog(deniedPermissions: List<String>) {
        val permissionNames = deniedPermissions.joinToString("\n") { "• " + localizePermissionName(it) }
        AlertDialog.Builder(this)
            .setTitle(t("⚠️ DOZVOLE NISU ODOBRENE", "⚠️ PERMISSIONS NOT GRANTED", "⚠️ РАЗРЕШЕНИЯ НЕ ПРЕДОСТАВЛЕНЫ", "⚠️ BERECHTIGUNGEN NICHT ERTEILT"))
            .setMessage(t(
                "Sledeće dozvole nisu odobrene:\n\n$permissionNames\n\nMolimo otvorite podešavanja i ručno odobrite dozvole.",
                "The following permissions were not granted:\n\n$permissionNames\n\nPlease open settings and manually grant permissions.",
                "Следующие разрешения не предоставлены:\n\n$permissionNames\n\nОткройте настройки и предоставьте разрешения.",
                "Folgende Berechtigungen nicht erteilt:\n\n$permissionNames\n\nBitte Einstellungen öffnen."
            ))
            .setPositiveButton(t("OTVORI PODEŠAVANJA", "OPEN SETTINGS", "ОТКРЫТЬ НАСТРОЙКИ", "EINSTELLUNGEN ÖFFNEN")) { _, _ ->
                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
            .setNegativeButton(t("ZATVORI", "CLOSE", "ЗАКРЫТЬ", "SCHLIESSEN"), null)
            .setCancelable(false)
            .show()
    }

    private fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                AlertDialog.Builder(this)
                    .setTitle(t("⚠️ OPTIMIZACIJA BATERIJE", "⚠️ BATTERY OPTIMIZATION", "⚠️ ОПТИМИЗАЦИЯ БАТАРЕИ", "⚠️ AKKUOPTIMIERUNG"))
                    .setMessage(t(
                        "Android može automatski ugasiti praćenje lokacije.\n\nDa bi SOS nadzor radio neprekidno, isključite optimizaciju baterije za ovu aplikaciju.\n\nPritisnite DOZVOLI na sledećem ekranu.",
                        "Android may stop location monitoring to save battery.\n\nTo keep SOS monitoring running, disable battery optimization for this app.\n\nPress ALLOW on the next screen.",
                        "Android может остановить мониторинг местоположения.\n\nНажмите РАЗРЕШИТЬ на следующем экране.",
                        "Android kann die Standortüberwachung stoppen.\n\nBitte ZULASSEN auf dem nächsten Bildschirm drücken."
                    ))
                    .setPositiveButton(t("NASTAVI", "CONTINUE", "ПРОДОЛЖИТЬ", "WEITER")) { _, _ ->
                        try {
                            val intent = Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                            intent.data = Uri.parse("package:$packageName")
                            startActivity(intent)
                        } catch (e: Exception) {
                            try {
                                startActivity(Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                            } catch (_: Exception) {}
                        }
                    }
                    .setNegativeButton(t("PRESKOCI", "SKIP", "ПРОПУСТИТЬ", "ÜBERSPRINGEN"), null)
                    .setCancelable(false)
                    .show()
            }
        }
    }

    private fun localizePermissionName(permission: String): String {
        return when (permission) {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION -> t("GPS lokacija", "GPS location", "GPS местоположение", "GPS-Standort")
            Manifest.permission.SEND_SMS -> t("Slanje SMS poruka", "Send SMS messages", "Отправка SMS", "SMS senden")
            Manifest.permission.CALL_PHONE -> t("Telefonski pozivi", "Phone calls", "Телефонные звонки", "Telefonanrufe")
            Manifest.permission.POST_NOTIFICATIONS -> t("Obaveštenja", "Notifications", "Уведомления", "Benachrichtigungen")
            else -> permission.substringAfterLast(".")
        }
    }

    // ─────────────────────────────────────────────
    // IZGRADNJA EKRANA
    // ─────────────────────────────────────────────

    private fun buildHomeScreen() {
        mainContainer.setBackgroundColor(Color.rgb(10, 12, 16))
        mainContainer.gravity = Gravity.CENTER_HORIZONTAL

        val prefs = getSharedPreferences("savio_prefs", MODE_PRIVATE)
        val isSosActive = prefs.getBoolean("sosActive", false)

        val logo = ImageView(this)
        logo.setImageResource(R.drawable.savio_logo)
        val logoParams = LinearLayout.LayoutParams(260, 260)
        logoParams.gravity = Gravity.CENTER_HORIZONTAL
        logoParams.setMargins(0, 20, 0, 10)
        logo.layoutParams = logoParams

        val title = TextView(this)
        title.text = "SAVIO SOS v1.3"
        title.textSize = 30f
        title.setTextColor(Color.WHITE)
        title.gravity = Gravity.CENTER
        title.setPadding(0, 30, 0, 8)

        val subtitle = TextView(this)
        subtitle.text = t(
            "POLICIJA  •  VATROGASCI-SPASIOCI  •  GORSKA SLUŽBA SPASAVANJA",
            "POLICE  •  FIREFIGHTERS-RESCUERS  •  MOUNTAIN RESCUE SERVICE",
            "ПОЛИЦИЯ  •  ПОЖАРНЫЕ-СПАСАТЕЛИ  •  ГОРНОСПАСАТЕЛЬНАЯ СЛУЖБА",
            "POLIZEI  •  FEUERWEHR-RETTER  •  BERGRETTUNGSDIENST"
        )
        subtitle.textSize = 12f
        subtitle.setTextColor(Color.rgb(120, 120, 120))
        subtitle.gravity = Gravity.CENTER
        subtitle.setPadding(0, 0, 0, 24)

        val btnEditProfile = Button(this)
        btnEditProfile.text = t("IZMENI PROFIL", "EDIT PROFILE", "ИЗМЕНИТЬ ПРОФИЛЬ", "PROFIL BEARBEITEN")

        val btnSettings = Button(this)
        btnSettings.text = t("PODEŠAVANJA", "SETTINGS", "НАСТРОЙКИ", "EINSTELLUNGEN")

        val tacticalAdvice = TextView(this)
        tacticalAdvice.text = t(
            "TAKTIČKI SAVET ZA TEREN\n\nNakon upućivanja SOS signala ne napuštajte mesto slanja.\nOstanite pozicionirani na tački sa koje je poslat SOS signal kako bi vas spasilački timovi lakše i brže pronašli.\n\nLokaciju menjajte samo ako postoji neposredna opasnost po život.",
            "TACTICAL ADVICE FOR THE FIELD\n\nAfter sending the SOS signal do not leave the location.\nStay positioned at the point from which the SOS signal was sent so rescue teams can find you faster.\n\nChange location only if there is immediate danger to life.",
            "ТАКТИЧЕСКИЙ СОВЕТ\n\nПосле отправки SOS-сигнала не покидайте место отправки.\nОставайтесь на точке, откуда был отправлен сигнал, чтобы спасатели могли быстрее вас найти.\n\nМеняйте местоположение только при непосредственной угрозе жизни.",
            "TAKTISCHER RATSCHLAG\n\nNach dem Senden des SOS-Signals den Ort nicht verlassen.\nBleiben Sie an der Stelle, von der das Signal gesendet wurde, damit Rettungsteams Sie schneller finden.\n\nÄndern Sie den Standort nur bei unmittelbarer Lebensgefahr."
        )
        tacticalAdvice.textSize = 16f
        tacticalAdvice.setTextColor(Color.WHITE)
        tacticalAdvice.setPadding(24, 24, 24, 24)
        tacticalAdvice.background = roundedBackground(Color.rgb(25, 30, 38), 28)

        val btnSos = TextView(this)
        btnSos.text = t("SOS\nCENTAR ZA\nUGROŽENE", "SOS\nCENTER FOR\nTHOSE IN DANGER", "SOS\nЦЕНТР\nПОМОЩИ", "SOS\nZENTRUM FÜR\nGEFÄHRDETE")
        btnSos.textSize = 22f
        btnSos.setTextColor(Color.WHITE)
        btnSos.gravity = Gravity.CENTER
        btnSos.setPadding(20, 20, 20, 20)
        btnSos.background = circleBackground(Color.rgb(190, 0, 0))
        val sosParams = LinearLayout.LayoutParams(420, 420)
        sosParams.gravity = Gravity.CENTER_HORIZONTAL
        sosParams.setMargins(0, 34, 0, 20)
        btnSos.layoutParams = sosParams

        val btnDeactivateSos = Button(this)
        btnDeactivateSos.text = "⚠ " + t("DEAKTIVIRAJ SOS", "DEACTIVATE SOS", "ОТКЛЮЧИТЬ SOS", "SOS DEAKTIVIEREN")
        btnDeactivateSos.setTextColor(Color.BLACK)
        btnDeactivateSos.setBackgroundColor(Color.rgb(255, 215, 0))

        if (isSosActive) {
            val blinkAnimation = AlphaAnimation(1.0f, 0.25f)
            blinkAnimation.duration = 500
            blinkAnimation.repeatMode = Animation.REVERSE
            blinkAnimation.repeatCount = Animation.INFINITE
            btnDeactivateSos.startAnimation(blinkAnimation)
        }

        // ─── ZONA ZA SPASIOCE — otvara međuekran ───
        val btnRescueZone = Button(this)
        btnRescueZone.text = t(
            "ZONA ZA SPASIOCE",
            "RESCUE ZONE",
            "ЗОНА СПАСАТЕЛЕЙ",
            "RETTUNGSZONE"
        )
        btnRescueZone.setTextColor(Color.WHITE)
        btnRescueZone.setBackgroundColor(Color.rgb(0, 130, 60))

        val btnSurvival = Button(this)
        btnSurvival.text = "🛟 " + t("SAVETI ZA PREŽIVLJAVANJE", "SURVIVAL TIPS", "СОВЕТЫ ПО ВЫЖИВАНИЮ", "ÜBERLEBENSTIPPS")
        btnSurvival.textSize = 16f
        btnSurvival.setTextColor(Color.WHITE)
        btnSurvival.setBackgroundColor(Color.rgb(0, 70, 140))

        status = TextView(this)
        status.text = if (isSosActive) {
            buildActiveStatusText(
                prefs.getString("incidentId", unknownText()) ?: unknownText(),
                prefs.getString("incidentPriority", unknownText()) ?: unknownText(),
                prefs.getString("incidentCondition", unknownText()) ?: unknownText()
            )
        } else {
            t("Sistem spreman.", "System ready.", "Система готова.", "System bereit.")
        }
        status.textSize = 16f
        status.setTextColor(Color.WHITE)
        status.setPadding(24, 24, 24, 24)
        status.background = roundedBackground(
            if (isSosActive) Color.rgb(80, 0, 0) else Color.rgb(18, 22, 28), 24
        )

        btnEditProfile.setOnClickListener { startActivity(Intent(this, ProfileActivity::class.java)) }
        btnSettings.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }

        btnSos.setOnClickListener {
            val active = getSharedPreferences("savio_prefs", MODE_PRIVATE).getBoolean("sosActive", false)
            if (active) {
                status.text = t(
                    "SOS je već aktivan. Ako želite da ga zaustavite, koristite dugme DEAKTIVIRAJ SOS.",
                    "SOS is already active. To stop it, use the DEACTIVATE SOS button.",
                    "SOS уже активен. Для отключения используйте кнопку ОТКЛЮЧИТЬ SOS.",
                    "SOS ist bereits aktiv. Zum Stoppen verwenden Sie die Schaltfläche SOS DEAKTIVIEREN."
                )
            } else {
                val teamMissionCode = getSharedPreferences("savio_prefs", MODE_PRIVATE).getString("teamMissionCode", null)
                if (!teamMissionCode.isNullOrEmpty()) {
                    android.app.AlertDialog.Builder(this)
                        .setTitle(t("UPOZORENJE", "WARNING", "ПРЕДУПРЕЖДЕНИЕ", "WARNUNG"))
                        .setMessage(t(
                            "Trenutno ste u aktivnoj spasilackoj akciji: $teamMissionCode\n\nAktivacija SOS-a ce vas prijaviti kao ugrozenu osobu.\n\nNastavite samo ako ste i sami dosli u opasnost.",
                            "You are in an active rescue mission: $teamMissionCode\n\nActivating SOS will register you as a person in danger.\n\nContinue only if you are in danger.",
                            "Вы участвуете в операции: $teamMissionCode\n\nПродолжайте только если вы сами в опасности.",
                            "Sie nehmen an einem Einsatz teil: $teamMissionCode\n\nFahren Sie nur fort, wenn Sie selbst in Gefahr sind."
                        ))
                        .setPositiveButton(t("AKTIVIRAJ SOS", "ACTIVATE SOS", "АКТИВИРОВАТЬ SOS", "SOS AKTIVIEREN")) { _, _ -> showEmergencyLevelDialog() }
                        .setNegativeButton(t("ODUSTANI", "CANCEL", "ОТМЕНА", "ABBRECHEN"), null)
                        .show()
                } else {
                    showEmergencyLevelDialog()
                }
            }
        }

        btnDeactivateSos.setOnClickListener {
            val active = getSharedPreferences("savio_prefs", MODE_PRIVATE).getBoolean("sosActive", false)
            if (active) showDeactivateConfirmation()
            else status.text = t(
                "SOS režim trenutno nije aktivan.",
                "SOS mode is currently not active.",
                "Режим SOS в данный момент не активен.",
                "SOS-Modus ist derzeit nicht aktiv."
            )
        }

        // ─── ZONA ZA SPASIOCE → međuekran ───
        btnRescueZone.setOnClickListener {
            showRescueZoneDialog()
        }

        btnSurvival.setOnClickListener { showSurvivalTips() }

        mainContainer.addView(logo)
        mainContainer.addView(title)
        mainContainer.addView(subtitle)
        mainContainer.addView(btnEditProfile)
        mainContainer.addView(btnSettings)
        mainContainer.addView(tacticalAdvice)
        mainContainer.addView(btnSos)
        mainContainer.addView(btnDeactivateSos)
        mainContainer.addView(btnRescueZone)
        mainContainer.addView(btnSurvival)
        mainContainer.addView(status)
    }

    // ─────────────────────────────────────────────
    // MEĐUEKRAN — ZONA ZA SPASIOCE
    // ─────────────────────────────────────────────

    private fun showRescueZoneDialog() {
        val dialogLayout = LinearLayout(this)
        dialogLayout.orientation = LinearLayout.VERTICAL
        dialogLayout.setPadding(32, 32, 32, 16)
        dialogLayout.setBackgroundColor(Color.rgb(10, 12, 16))

        val infoText = TextView(this)
        infoText.text = t(
            "Odaberite režim rada:",
            "Select operating mode:",
            "Выберите режим работы:",
            "Betriebsmodus auswählen:"
        )
        infoText.textSize = 15f
        infoText.setTextColor(Color.rgb(180, 180, 180))
        infoText.setPadding(0, 0, 0, 24)

        // ─── INDIVIDUALNI REŽIM ───
        val btnIndividual = Button(this)
        btnIndividual.text = "👤  " + t(
            "INDIVIDUALNI REŽIM\nSamostan rad spasioca",
            "INDIVIDUAL MODE\nIndependent rescue work",
            "ИНДИВИДУАЛЬНЫЙ РЕЖИМ\nСамостоятельная работа",
            "INDIVIDUELLER MODUS\nSelbstständige Arbeit"
        )
        btnIndividual.setTextColor(Color.WHITE)
        btnIndividual.setBackgroundColor(Color.rgb(0, 130, 60))
        btnIndividual.textSize = 14f
        val individualParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        individualParams.setMargins(0, 0, 0, 16)
        btnIndividual.layoutParams = individualParams

        // ─── TIMSKA POTRAGA ───
        val btnTeam = Button(this)
        btnTeam.text = "👥  " + t(
            "TIMSKA POTRAGA\nGrupna koordinacija i mapa",
            "TEAM MISSION\nGroup coordination and map",
            "КОМАНДНАЯ ОПЕРАЦИЯ\nГрупповая координация и карта",
            "TEAMEINSATZ\nGruppenkoordination und Karte"
        )
        btnTeam.setTextColor(Color.WHITE)
        btnTeam.setBackgroundColor(Color.rgb(0, 100, 180))
        btnTeam.textSize = 14f
        val teamParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        teamParams.setMargins(0, 0, 0, 16)
        btnTeam.layoutParams = teamParams

        // ─── IZVEŠTAJI AKCIJA ───
        val btnReports = Button(this)
        btnReports.text = "📋  " + t(
            "IZVEŠTAJI AKCIJA\nPregled završenih intervencija",
            "MISSION REPORTS\nView completed interventions",
            "ОТЧЁТЫ ОБ ОПЕРАЦИЯХ\nПросмотр завершённых операций",
            "EINSATZBERICHTE\nAbgeschlossene Einsätze anzeigen"
        )
        btnReports.setTextColor(Color.WHITE)
        btnReports.setBackgroundColor(Color.rgb(80, 50, 120))
        btnReports.textSize = 14f
        val reportsParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        reportsParams.setMargins(0, 0, 0, 8)
        btnReports.layoutParams = reportsParams

        dialogLayout.addView(infoText)
        dialogLayout.addView(btnIndividual)
        dialogLayout.addView(btnTeam)
        dialogLayout.addView(btnReports)

        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle(t("ZONA ZA SPASIOCE", "RESCUE ZONE", "ЗОНА СПАСАТЕЛЕЙ", "RETTUNGSZONE"))
            .setView(dialogLayout)
            .setNegativeButton(t("ODUSTANI", "CANCEL", "ОТМЕНА", "ABBRECHEN"), null)
            .create()

        btnIndividual.setOnClickListener {
            dialog.dismiss()
            startActivity(Intent(this, RescueLoginActivity::class.java))
        }

        btnTeam.setOnClickListener {
            dialog.dismiss()
            startActivity(Intent(this, TeamLoginActivity::class.java))
        }

        btnReports.setOnClickListener {
            dialog.dismiss()
            startActivity(Intent(this, MissionLogActivity::class.java))
        }

        dialog.show()
    }

    // ─────────────────────────────────────────────
    // SOS DIJALOZI
    // ─────────────────────────────────────────────

    private fun showEmergencyLevelDialog() {
        val displayOptions = emergencyConditionDisplayOptions()
        val internalConditions = arrayOf(
            "ŽIVOTNO UGROŽEN", "POVREĐEN", "IZGUBLJEN",
            "ZAROBLJEN / NE MOGU DA SE KREĆEM", "POTREBNA MI JE POMOĆ"
        )

        AlertDialog.Builder(this)
            .setTitle(t("ODABERITE STANJE", "SELECT CONDITION", "ВЫБЕРИТЕ СОСТОЯНИЕ", "ZUSTAND AUSWÄHLEN"))
            .setItems(displayOptions) { _, which ->
                val condition = internalConditions[which]
                val priority = when (which) {
                    0 -> "CRVENI - ŽIVOTNO UGROŽEN"
                    1 -> "NARANDŽASTI - POVREĐEN"
                    2 -> "ŽUTI - IZGUBLJEN"
                    3 -> "CRVENI - NE MOŽE DA SE KREĆE"
                    else -> "ŽUTI - POTREBNA POMOĆ"
                }
                showSosConfirmation(priority, condition)
            }
            .setNegativeButton(t("ODUSTANI", "CANCEL", "ОТМЕНА", "ABBRECHEN"), null)
            .show()
    }

    private fun showSurvivalTips() {
        val scrollView = ScrollView(this)
        val textView = TextView(this)
        textView.text = survivalTipsText()
        textView.textSize = 16f
        textView.setTextColor(Color.WHITE)
        textView.setPadding(32, 32, 32, 32)
        scrollView.setBackgroundColor(Color.rgb(10, 12, 16))
        scrollView.addView(textView)

        AlertDialog.Builder(this)
            .setTitle(t("Saveti za preživljavanje", "Survival tips", "Советы по выживанию", "Überlebenstipps"))
            .setView(scrollView)
            .setPositiveButton(t("ZATVORI", "CLOSE", "ЗАКРЫТЬ", "SCHLIESSEN"), null)
            .show()
    }

    private fun showSosConfirmation(priority: String, condition: String) {
        val localizedCondition = localizeCondition(condition)
        val localizedPriority = localizePriority(priority)

        AlertDialog.Builder(this)
            .setTitle(t("POTVRDA SOS AKTIVACIJE", "CONFIRM SOS ACTIVATION", "ПОДТВЕРЖДЕНИЕ АКТИВАЦИИ SOS", "SOS-AKTIVIERUNG BESTÄTIGEN"))
            .setMessage(t(
                "Odabrano stanje:\n$localizedCondition\n\nPrioritet:\n$localizedPriority\n\nAko nastavite, aplikacija će:\n• poslati GPS koordinate\n• poslati SMS na definisane brojeve\n• pokrenuti poziv ka primarnom broju\n• pokrenuti nadzor pomeranja preko 50 metara\n\nNastavite samo ako postoji stvarna potreba.",
                "Selected condition:\n$localizedCondition\n\nPriority:\n$localizedPriority\n\nIf you continue, the app will:\n• send GPS coordinates\n• send SMS to defined numbers\n• start a call to primary number\n• start movement monitoring over 50 meters\n\nContinue only if there is real need.",
                "Выбранное состояние:\n$localizedCondition\n\nПриоритет:\n$localizedPriority\n\nПриложение выполнит:\n• отправит GPS-координаты\n• отправит SMS\n• выполнит звонок\n• запустит контроль перемещения\n\nПродолжайте только при реальной необходимости.",
                "Ausgewählter Zustand:\n$localizedCondition\n\nPriorität:\n$localizedPriority\n\nDie App wird:\n• GPS-Koordinaten senden\n• SMS senden\n• Anruf starten\n• Bewegungsüberwachung starten\n\nNur bei echter Notwendigkeit fortfahren."
            ))
            .setPositiveButton(t("AKTIVIRAJ SOS", "ACTIVATE SOS", "АКТИВИРОВАТЬ SOS", "SOS AKTIVIEREN")) { _, _ -> activateSos(priority, condition) }
            .setNegativeButton(t("ODUSTANI", "CANCEL", "ОТМЕНА", "ABBRECHEN"), null)
            .show()
    }

    private fun showDeactivateConfirmation() {
        val reasons = arrayOf(
            t("SPASEN SAM — spasioci su me pronašli", "RESCUED — rescuers found me", "СПАСЁН — спасатели меня нашли", "GERETTET — Rettungskräfte haben mich gefunden"),
            t("PRONAŠAO SAM IZLAZ SAM", "FOUND EXIT ON MY OWN", "НАШЁЛ ВЫХОД САМОСТОЯТЕЛЬНО", "AUSWEG SELBST GEFUNDEN"),
            t("STIGAO SAM U NASELJENO MESTO", "REACHED POPULATED AREA", "ДОБРАЛСЯ ДО НАСЕЛЁННОГО ПУНКТА", "BESIEDELTES GEBIET ERREICHT"),
            t("OPASNOST JE PROŠLA", "DANGER HAS PASSED", "ОПАСНОСТЬ МИНОВАЛА", "GEFAHR IST VORBEI"),
            t("BIO JE LAŽNI ALARM", "FALSE ALARM", "ЛОЖНАЯ ТРЕВОГА", "FEHLALARM"),
            t("DRUGI RAZLOG", "OTHER REASON", "ДРУГАЯ ПРИЧИНА", "ANDERER GRUND")
        )

        AlertDialog.Builder(this)
            .setTitle(t("RAZLOG DEAKTIVACIJE SOS-a", "REASON FOR SOS DEACTIVATION", "ПРИЧИНА ОТКЛЮЧЕНИЯ SOS", "GRUND DER SOS-DEAKTIVIERUNG"))
            .setItems(reasons) { _, which ->
                if (which == 5) showCustomReasonDialog()
                else showFinalDeactivateConfirmation(reasons[which])
            }
            .setNegativeButton(t("ODUSTANI", "CANCEL", "ОТМЕНА", "ABBRECHEN"), null)
            .show()
    }

    private fun showCustomReasonDialog() {
        val input = android.widget.EditText(this)
        input.hint = t("Unesite razlog...", "Enter reason...", "Введите причину...", "Grund eingeben...")
        input.setTextColor(android.graphics.Color.WHITE)
        input.setPadding(32, 16, 32, 16)

        AlertDialog.Builder(this)
            .setTitle(t("UNESITE RAZLOG", "ENTER REASON", "ВВЕДИТЕ ПРИЧИНУ", "GRUND EINGEBEN"))
            .setView(input)
            .setPositiveButton(t("POTVRDI", "CONFIRM", "ПОДТВЕРДИТЬ", "BESTÄTIGEN")) { _, _ ->
                val reason = input.text.toString().trim().ifEmpty {
                    t("Nije naveden razlog", "No reason provided", "Причина не указана", "Kein Grund angegeben")
                }
                showFinalDeactivateConfirmation(reason)
            }
            .setNegativeButton(t("ODUSTANI", "CANCEL", "ОТМЕНА", "ABBRECHEN"), null)
            .show()
    }

    private fun showFinalDeactivateConfirmation(reason: String) {
        val prefs = getSharedPreferences("savio_prefs", MODE_PRIVATE)
        val fullName = prefs.getString("fullName", unknownText()) ?: unknownText()

        AlertDialog.Builder(this)
            .setTitle(t("POTVRDA DEAKTIVACIJE", "CONFIRM DEACTIVATION", "ПОДТВЕРЖДЕНИЕ ОТКЛЮЧЕНИЯ", "DEAKTIVIERUNG BESTÄTIGEN"))
            .setMessage(t(
                "Razlog: $reason\n\nKorisnik: $fullName\n\nPoruka o deaktivaciji biće poslata spasiocima.\n\nNastavite?",
                "Reason: $reason\n\nUser: $fullName\n\nA deactivation message will be sent to rescuers.\n\nContinue?",
                "Причина: $reason\n\nПользователь: $fullName\n\nСообщение об отключении будет отправлено.\n\nПродолжить?",
                "Grund: $reason\n\nBenutzer: $fullName\n\nEine Deaktivierungsnachricht wird gesendet.\n\nFortfahren?"
            ))
            .setPositiveButton(t("DEAKTIVIRAJ", "DEACTIVATE", "ОТКЛЮЧИТЬ", "DEAKTIVIEREN")) { _, _ -> deactivateSos(reason) }
            .setNegativeButton(t("ODUSTANI", "CANCEL", "ОТМЕНА", "ABBRECHEN"), null)
            .show()
    }

    // ─────────────────────────────────────────────
    // SOS LOGIKA
    // ─────────────────────────────────────────────

    private fun activateSos(priority: String, condition: String) {
        if (!hasRequiredPermissions()) {
            status.text = t(
                "Nedostaju dozvole za GPS, SMS ili poziv.",
                "Missing permissions for GPS, SMS or call.",
                "Отсутствуют разрешения для GPS, SMS или звонка.",
                "Fehlende Berechtigungen für GPS, SMS oder Anruf."
            )
            requestNeededPermissions()
            return
        }

        status.text = t("Tražim GPS lokaciju...", "Getting GPS location...", "Получение GPS-координат...", "GPS-Standort wird ermittelt...")

        getFreshLocation { location ->
            val incidentId = generateIncidentId()

            getSharedPreferences("savio_prefs", MODE_PRIVATE).edit()
                .putString("incidentId", incidentId)
                .putString("incidentPriority", priority)
                .putString("incidentCondition", condition)
                .apply()

            val message = buildSosMessage(location, incidentId, priority, condition)
            sosNumbers.forEach { number -> sendSms(number, message) }

            if (location != null) {
                saveSosActive(location, incidentId, priority, condition)
                startLocationMonitor(location, incidentId, priority, condition)
            } else {
                getSharedPreferences("savio_prefs", MODE_PRIVATE).edit()
                    .putBoolean("sosActive", true)
                    .putString("incidentId", incidentId)
                    .putString("incidentPriority", priority)
                    .putString("incidentCondition", condition)
                    .apply()
                status.text = t(
                    "SOS poruke su poslate, ali početna GPS lokacija nije dostupna za nadzor kretanja.",
                    "SOS messages sent, but initial GPS location is not available for movement monitoring.",
                    "SOS-сообщения отправлены, но начальное GPS-местоположение недоступно.",
                    "SOS-Nachrichten gesendet, aber anfänglicher GPS-Standort nicht verfügbar."
                )
            }

            callPrimaryNumber()
            status.text = buildSosActivatedStatusText(incidentId, priority, condition)
            mainContainer.removeAllViews()
            buildHomeScreen()
        }
    }

    private fun deactivateSos(reason: String) {
        if (!hasRequiredPermissions()) {
            status.text = t(
                "Nedostaju dozvole za SMS ili GPS.",
                "Missing permissions for SMS or GPS.",
                "Отсутствуют разрешения для SMS или GPS.",
                "Fehlende Berechtigungen für SMS oder GPS."
            )
            requestNeededPermissions()
            return
        }

        stopService(Intent(this, LocationMonitorService::class.java))

        val location = getLastKnownLocation()
        val prefs = getSharedPreferences("savio_prefs", MODE_PRIVATE)
        val incidentId = prefs.getString("incidentId", "Nepoznat") ?: "Nepoznat"
        val priority = prefs.getString("incidentPriority", "Nepoznat") ?: "Nepoznat"
        val condition = prefs.getString("incidentCondition", "Nepoznato") ?: "Nepoznato"

        val message = buildCancelMessage(location, incidentId, priority, condition, reason)
        sosNumbers.forEach { number -> sendSms(number, message) }

        prefs.edit()
            .putBoolean("sosActive", false)
            .remove("sosStartLat").remove("sosStartLon")
            .remove("incidentId").remove("incidentPriority").remove("incidentCondition")
            .apply()

        status.text = buildSosDeactivatedStatusText(incidentId)
        mainContainer.removeAllViews()
        buildHomeScreen()
    }

    private fun saveSosActive(location: Location, incidentId: String, priority: String, condition: String) {
        getSharedPreferences("savio_prefs", MODE_PRIVATE).edit()
            .putBoolean("sosActive", true)
            .putString("incidentId", incidentId)
            .putString("incidentPriority", priority)
            .putString("incidentCondition", condition)
            .putString("sosStartLat", location.latitude.toString())
            .putString("sosStartLon", location.longitude.toString())
            .apply()
    }

    private fun startLocationMonitor(startLocation: Location, incidentId: String, priority: String, condition: String) {
        val intent = Intent(this, LocationMonitorService::class.java)
        intent.putExtra("start_lat", startLocation.latitude)
        intent.putExtra("start_lon", startLocation.longitude)
        intent.putExtra("incident_id", incidentId)
        intent.putExtra("incident_priority", priority)
        intent.putExtra("incident_condition", condition)
        intent.putStringArrayListExtra("sos_numbers", ArrayList(sosNumbers))
        startService(intent)
    }

    private fun hasRequiredPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
    }

    private fun getFreshLocation(callback: (Location?) -> Unit) {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                callback(getLastKnownLocation()); return
            }
            var called = false
            val listener = object : android.location.LocationListener {
                override fun onLocationChanged(location: Location) {
                    if (!called) { called = true; locationManager.removeUpdates(this); callback(location) }
                }
                override fun onProviderDisabled(provider: String) {}
                override fun onProviderEnabled(provider: String) {}
                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
            }
            val timeoutRunnable = Runnable {
                if (!called) { called = true; try { locationManager.removeUpdates(listener) } catch (_: Exception) {}; callback(getLastKnownLocation()) }
            }
            val handler = android.os.Handler(mainLooper)
            handler.postDelayed(timeoutRunnable, 8000L)
            var requested = false
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) { locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, listener, mainLooper); requested = true }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) { locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, listener, mainLooper); requested = true }
            if (!requested) { handler.removeCallbacks(timeoutRunnable); called = true; callback(getLastKnownLocation()) }
        } catch (e: Exception) { callback(getLastKnownLocation()) }
    }

    private fun getLastKnownLocation(): Location? {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val gpsLocation = try { locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) } catch (e: SecurityException) { null }
        val networkLocation = try { locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) } catch (e: SecurityException) { null }
        return gpsLocation ?: networkLocation
    }

    // ─────────────────────────────────────────────
    // PORUKE
    // ─────────────────────────────────────────────

    private fun buildSosMessage(location: Location?, incidentId: String, priority: String, condition: String): String {
        val prefs = getSharedPreferences("savio_prefs", MODE_PRIVATE)
        val fullName = prefs.getString("fullName", "Nije uneto")
        val age = prefs.getString("age", "Nije uneto")
        val phone = prefs.getString("phone", "Nije uneto")
        val bloodType = prefs.getString("bloodType", "Nije uneto")
        val chronicDiseases = prefs.getString("chronicDiseases", "Nije uneto")
        val time = SimpleDateFormat("dd.MM.yyyy. HH:mm:ss", Locale.getDefault()).format(Date())
        val battery = getBatteryPercent()
        val locationText = if (location != null) {
            "Lokacija:\nhttps://maps.google.com/?q=${location.latitude},${location.longitude}\n\nKoordinate:\n${location.latitude}, ${location.longitude}"
        } else "Lokacija trenutno nije dostupna."

        return "SOS UPOZORENJE!\n\nIncident ID: $incidentId\n\nPRIORITET: $priority\nSTANJE: $condition\n\nIme i prezime: $fullName\nGodine: $age\nTelefon: $phone\nKrvna grupa: $bloodType\nHronicne bolesti: $chronicDiseases\n\nVreme aktivacije: $time\nBaterija: $battery%\n\n$locationText\n\nNapomena: Osoba je upozorena da ne napusta lokaciju."
    }

    private fun buildCancelMessage(location: Location?, incidentId: String, priority: String, condition: String, reason: String): String {
        val time = SimpleDateFormat("dd.MM.yyyy. HH:mm:ss", Locale.getDefault()).format(Date())
        val battery = getBatteryPercent()
        val prefs = getSharedPreferences("savio_prefs", MODE_PRIVATE)
        val fullName = prefs.getString("fullName", "Nije uneto") ?: "Nije uneto"
        val locationText = if (location != null)
            "Poslednja lokacija:\nhttps://maps.google.com/?q=${location.latitude},${location.longitude}"
        else "Poslednja lokacija nije dostupna."

        return "SOS OTKAZAN\n\nIncident ID: $incidentId\n\nRAZLOG: $reason\n\nIme: $fullName\nPRIORITET: $priority\nSTANJE: $condition\n\nVreme deaktivacije: $time\nBaterija: $battery%\n\n$locationText"
    }

    // ─────────────────────────────────────────────
    // POMOĆNE FUNKCIJE
    // ─────────────────────────────────────────────

    private fun generateIncidentId(): String {
        val time = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(Date())
        return "SAVIO-$time"
    }

    private fun getBatteryPercent(): Int {
        val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        if (level < 0 || scale <= 0) return -1
        return (level * 100) / scale
    }

    private fun sendSms(number: String, message: String) {
        try {
            val smsManager = SmsManager.getDefault()
            val parts = smsManager.divideMessage(message)
            smsManager.sendMultipartTextMessage(number, null, parts, null, null)
        } catch (e: Exception) {
            Toast.makeText(this, "SMS greška za $number: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun callPrimaryNumber() {
        try {
            val intent = Intent(Intent.ACTION_CALL)
            intent.data = Uri.parse("tel:${sosNumbers.first()}")
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Greška pri pozivu: ${e.message}", Toast.LENGTH_LONG).show()
        }
        Handler(Looper.getMainLooper()).postDelayed({ startTtsMessage() }, 2000L)
    }

    private fun startTtsMessage() {
        val prefs = getSharedPreferences("savio_prefs", MODE_PRIVATE)
        val fullName = prefs.getString("fullName", "Nepoznato") ?: "Nepoznato"
        val phone = prefs.getString("phone", "Nepoznato") ?: "Nepoznato"
        val bloodType = prefs.getString("bloodType", "Nepoznato") ?: "Nepoznato"
        val chronicDiseases = prefs.getString("chronicDiseases", "Nepoznato") ?: "Nepoznato"
        val location = getLastKnownLocation()
        val gpsText = if (location != null) {
            val lat = location.latitude.toString().replace(".", " tačka ")
            val lon = location.longitude.toString().replace(".", " tačka ")
            "GPS koordinate su: $lat, $lon."
        } else "GPS koordinate trenutno nisu dostupne."

        val phoneSpoken = phone.map { c ->
            when (c) {
                '0' -> "nula"; '1' -> "jedan"; '2' -> "dva"; '3' -> "tri"; '4' -> "četiri"
                '5' -> "pet"; '6' -> "šest"; '7' -> "sedam"; '8' -> "osam"; '9' -> "devet"
                '+' -> "plus"; else -> c.toString()
            }
        }.joinToString(" ")

        val ttsMessage = "Pažnja! SOS uzbuna! Ako možete da govorite, jednostavno progovorite i bićete čuti. Ovo je automatska poruka aplikacije SAVIO SOS. Osoba je aktivirala SOS signal. Ime i prezime: $fullName. Broj telefona: $phoneSpoken. Krvna grupa: $bloodType. Hronične bolesti: $chronicDiseases. $gpsText GPS koordinate su poslati i SMS porukom. Poruka će biti ponovljena još dva puta."

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val langResult = tts?.setLanguage(Locale("sr"))
                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.language = Locale.getDefault()
                }
                tts?.setSpeechRate(0.85f)
                repeatTtsMessage(ttsMessage, 3)
            }
        }
    }

    private fun repeatTtsMessage(message: String, timesLeft: Int) {
        if (timesLeft <= 0) { tts?.stop(); tts?.shutdown(); tts = null; return }
        tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "SOS_TTS_$timesLeft")
        val estimatedDuration = (message.length / 100.0 * 6000).toLong() + 5000L
        Handler(Looper.getMainLooper()).postDelayed({ repeatTtsMessage(message, timesLeft - 1) }, estimatedDuration)
    }

    override fun onDestroy() { tts?.stop(); tts?.shutdown(); tts = null; super.onDestroy() }

    private fun applyWindowInsets() {
        val rootView = window.decorView.findViewById<android.view.View>(android.R.id.content)
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }
    }

    private fun roundedBackground(color: Int, radius: Int): GradientDrawable {
        val drawable = GradientDrawable()
        drawable.setColor(color)
        drawable.cornerRadius = radius.toFloat()
        return drawable
    }

    private fun circleBackground(color: Int): GradientDrawable {
        val drawable = GradientDrawable()
        drawable.shape = GradientDrawable.OVAL
        drawable.setColor(color)
        drawable.setStroke(8, Color.WHITE)
        return drawable
    }

    // ─────────────────────────────────────────────
    // JEZICI
    // ─────────────────────────────────────────────

    private fun currentLanguage(): String {
        return getSharedPreferences("savio_prefs", MODE_PRIVATE).getString("language", "sr") ?: "sr"
    }

    private fun t(sr: String, en: String, ru: String, de: String): String {
        return when (currentLanguage()) { "en" -> en; "ru" -> ru; "de" -> de; else -> sr }
    }

    private fun unknownText(): String = t("Nepoznat", "Unknown", "Неизвестно", "Unbekannt")

    private fun emergencyConditionDisplayOptions(): Array<String> {
        return arrayOf(
            t("ŽIVOTNO UGROŽEN", "LIFE THREATENED", "УГРОЗА ЖИЗНИ", "LEBENSGEFAHR"),
            t("POVREĐEN", "INJURED", "РАНЕН / ТРАВМИРОВАН", "VERLETZT"),
            t("IZGUBLJEN", "LOST", "ПОТЕРЯЛСЯ", "VERIRRT"),
            t("ZAROBLJEN / NE MOGU DA SE KREĆEM", "TRAPPED / CANNOT MOVE", "ЗАБЛОКИРОВАН / НЕ МОГУ ДВИГАТЬСЯ", "EINGESCHLOSSEN / KANN MICH NICHT BEWEGEN"),
            t("POTREBNA MI JE POMOĆ", "I NEED HELP", "НУЖНА ПОМОЩЬ", "ICH BRAUCHE HILFE")
        )
    }

    private fun localizeCondition(condition: String): String {
        return when (condition) {
            "ŽIVOTNO UGROŽEN" -> t("ŽIVOTNO UGROŽEN", "LIFE THREATENED", "УГРОЗА ЖИЗНИ", "LEBENSGEFAHR")
            "POVREĐEN" -> t("POVREĐEN", "INJURED", "РАНЕН / ТРАВМИРОВАН", "VERLETZT")
            "IZGUBLJEN" -> t("IZGUBLJEN", "LOST", "ПОТЕРЯЛСЯ", "VERIRRT")
            "ZAROBLJEN / NE MOGU DA SE KREĆEM" -> t("ZAROBLJEN / NE MOGU DA SE KREĆEM", "TRAPPED / CANNOT MOVE", "ЗАБЛОКИРОВАН / НЕ МОГУ ДВИГАТЬСЯ", "EINGESCHLOSSEN / KANN MICH NICHT BEWEGEN")
            "POTREBNA MI JE POMOĆ" -> t("POTREBNA MI JE POMOĆ", "I NEED HELP", "НУЖНА ПОМОЩЬ", "ICH BRAUCHE HILFE")
            else -> condition
        }
    }

    private fun localizePriority(priority: String): String {
        return when (priority) {
            "CRVENI - ŽIVOTNO UGROŽEN" -> t("CRVENI - ŽIVOTNO UGROŽEN", "RED - LIFE THREATENED", "КРАСНЫЙ - УГРОЗА ЖИЗНИ", "ROT - LEBENSGEFAHR")
            "NARANDŽASTI - POVREĐEN" -> t("NARANDŽASTI - POVREĐEN", "ORANGE - INJURED", "ОРАНЖЕВЫЙ - ТРАВМА", "ORANGE - VERLETZT")
            "ŽUTI - IZGUBLJEN" -> t("ŽUTI - IZGUBLJEN", "YELLOW - LOST", "ЖЁЛТЫЙ - ПОТЕРЯЛСЯ", "GELB - VERIRRT")
            "CRVENI - NE MOŽE DA SE KREĆE" -> t("CRVENI - NE MOŽE DA SE KREĆE", "RED - CANNOT MOVE", "КРАСНЫЙ - НЕ МОЖЕТ ДВИГАТЬСЯ", "ROT - KANN SICH NICHT BEWEGEN")
            "ŽUTI - POTREBNA POMOĆ" -> t("ŽUTI - POTREBNA POMOĆ", "YELLOW - HELP NEEDED", "ЖЁЛТЫЙ - НУЖНА ПОМОЩЬ", "GELB - HILFE BENÖTIGT")
            else -> priority
        }
    }

    private fun buildActiveStatusText(incidentId: String, priority: String, condition: String): String {
        return "${t("SOS JE AKTIVAN", "SOS IS ACTIVE", "SOS АКТИВЕН", "SOS IST AKTIV")}\n\nIncident ID: $incidentId\n${t("Prioritet", "Priority", "Приоритет", "Priorität")}: ${localizePriority(priority)}\n${t("Stanje", "Condition", "Состояние", "Zustand")}: ${localizeCondition(condition)}\n\n${t("Nadzor lokacije je uključen.", "Location monitoring is active.", "Контроль местоположения включён.", "Standortüberwachung ist aktiv.")}\n${t("Ne menjajte lokaciju osim ako ste životno ugroženi.", "Do not change location unless your life is in danger.", "Не меняйте местоположение если вашей жизни ничего не угрожает.", "Ändern Sie Ihren Standort nicht, es sei denn, Ihr Leben ist in Gefahr.")}"
    }

    private fun buildSosActivatedStatusText(incidentId: String, priority: String, condition: String): String {
        return t(
            "SOS AKTIVIRAN\n\nIncident ID: $incidentId\nPrioritet: ${localizePriority(priority)}\nStanje: ${localizeCondition(condition)}\n\nPoruke su poslate.\nPoziv je pokrenut.\nNadzor pomeranja je aktivan.\n\nNe menjajte lokaciju osim ako ste životno ugroženi.",
            "SOS ACTIVATED\n\nIncident ID: $incidentId\nPriority: ${localizePriority(priority)}\nCondition: ${localizeCondition(condition)}\n\nMessages sent.\nCall started.\nMovement monitoring active.\n\nDo not change location unless your life is in danger.",
            "SOS АКТИВИРОВАН\n\nIncident ID: $incidentId\nПриоритет: ${localizePriority(priority)}\nСостояние: ${localizeCondition(condition)}\n\nСообщения отправлены.\nЗвонок выполнен.\nКонтроль активирован.",
            "SOS AKTIVIERT\n\nIncident ID: $incidentId\nPriorität: ${localizePriority(priority)}\nZustand: ${localizeCondition(condition)}\n\nNachrichten gesendet.\nAnruf gestartet.\nBewegungsüberwachung aktiv."
        )
    }

    private fun buildSosDeactivatedStatusText(incidentId: String): String {
        return t(
            "SOS DEAKTIVIRAN\n\nIncident ID: $incidentId\n\nNadzor lokacije je zaustavljen.\nPoruka o otkazivanju poslata je na definisane brojeve.",
            "SOS DEACTIVATED\n\nIncident ID: $incidentId\n\nLocation monitoring stopped.\nCancellation message sent.",
            "SOS ОТКЛЮЧЕН\n\nIncident ID: $incidentId\n\nКонтроль остановлен.\nСообщение об отмене отправлено.",
            "SOS DEAKTIVIERT\n\nIncident ID: $incidentId\n\nStandortüberwachung gestoppt.\nAbbruchnachricht gesendet."
        )
    }

    private fun survivalTipsText(): String {
        return t(
            "SAVETI ZA PREŽIVLJAVANJE\n\nŠUMA:\n• Ostanite na jednom mestu ako ste već poslali SOS.\n• Napravite vidljiv znak od grana, odeće ili kamenja.\n• Ne trošite bateriju bez potrebe.\n\nSNEG:\n• Zaštitite glavu, šake i stopala.\n• Ne sedite direktno na sneg.\n• Napravite zaklon od vetra.\n\nVODA:\n• Ne trošite snagu nepotrebnim plivanjem.\n• Pokušajte da plutate i smirite disanje.\n\nSUNCE I VRUĆINA:\n• Sklonite se u hlad.\n• Pokrijte glavu.\n• Štedite vodu.\n\nMEDICINSKI SAVET:\n• Ako krvarite, pritisnite ranu čistom tkaninom.\n• Ako sumnjate na prelom, ne pomerajte povređeni deo.\n• Ostanite mirni i dišite polako.\n\nBATERIJA:\n• Smanjite osvetljenje ekrana.\n• Ne zatvarajte aplikaciju ako je SOS aktivan.",
            "SURVIVAL TIPS\n\nFOREST:\n• Stay in one place if you have already sent SOS.\n• Make a visible sign using branches, clothing or stones.\n• Do not waste battery.\n\nSNOW:\n• Protect your head, hands and feet.\n• Do not sit directly on snow.\n• Make wind shelter.\n\nWATER:\n• Do not waste strength swimming unnecessarily.\n• Try to float and calm your breathing.\n\nSUN AND HEAT:\n• Move into shade.\n• Cover your head.\n• Save water.\n\nMEDICAL:\n• If bleeding, press wound with clean cloth.\n• If fracture suspected, do not move injured part.\n• Stay calm and breathe slowly.\n\nBATTERY:\n• Reduce screen brightness.\n• Do not close app while SOS is active.",
            "СОВЕТЫ ПО ВЫЖИВАНИЮ\n\nЛЕС:\n• Оставайтесь на месте если отправили SOS.\n• Сделайте знак из веток или одежды.\n\nСНЕГ:\n• Защитите голову, руки и ноги.\n• Не садитесь на снег.\n\nВОДА:\n• Держитесь на воде, успокойте дыхание.\n\nСОЛНЦЕ:\n• Перейдите в тень.\n• Экономьте воду.\n\nМЕДИЦИНА:\n• При кровотечении прижмите рану.\n• При переломе не двигайте повреждённую часть.",
            "ÜBERLEBENSTIPPS\n\nWALD:\n• Bleiben Sie an einem Ort wenn SOS gesendet.\n• Machen Sie ein sichtbares Zeichen.\n\nSCHNEE:\n• Schützen Sie Kopf, Hände und Füße.\n• Bauen Sie Windschutz.\n\nWASSER:\n• Versuchen Sie zu treiben.\n\nSONNE:\n• Gehen Sie in den Schatten.\n• Wasser sparen.\n\nMEDIZIN:\n• Bei Blutung Wunde drücken.\n• Bei Bruch nicht bewegen."
        )
    }
}
