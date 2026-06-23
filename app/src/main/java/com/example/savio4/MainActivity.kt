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
        "+381652013323"
    )

    private val permissionRequestCode = 101

    // Prati koliko puta je korisnik odbio dozvole
    private val deniedPermissionsMap = mutableMapOf<String, Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mainContainer = findViewById(R.id.mainContainer)
        mainContainer.removeAllViews()

        requestNeededPermissions()
        requestBatteryOptimizationExemption()
        buildHomeScreen()
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
            ActivityCompat.requestPermissions(
                this,
                missingPermissions.toTypedArray(),
                permissionRequestCode
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode != permissionRequestCode) return

        val deniedPermissions = permissions.filterIndexed { index, _ ->
            grantResults[index] != PackageManager.PERMISSION_GRANTED
        }

        if (deniedPermissions.isEmpty()) return

        // Ažuriraj brojač odbijanja za svaku dozvolu
        deniedPermissions.forEach { permission ->
            deniedPermissionsMap[permission] = (deniedPermissionsMap[permission] ?: 0) + 1
        }

        // Provjeri da li je neka dozvola odbijena više od jednom → otvori podešavanja
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
        val permissionNames = deniedPermissions.joinToString("\n") { permission ->
            "• " + localizePermissionName(permission)
        }

        AlertDialog.Builder(this)
            .setTitle(
                t(
                    "⚠️ POTREBNE DOZVOLE",
                    "⚠️ REQUIRED PERMISSIONS",
                    "⚠️ НЕОБХОДИМЫЕ РАЗРЕШЕНИЯ",
                    "⚠️ ERFORDERLICHE BERECHTIGUNGEN"
                )
            )
            .setMessage(
                t(
                    "Aplikacija NEĆE ispravno raditi bez sledećih dozvola:\n\n$permissionNames\n\nBez ovih dozvola nije moguće poslati SOS signal, GPS lokaciju ni pozvati hitne kontakte.\n\nMolimo odobrite dozvole.",
                    "The application will NOT work correctly without the following permissions:\n\n$permissionNames\n\nWithout these permissions it is not possible to send SOS signal, GPS location or call emergency contacts.\n\nPlease grant the permissions.",
                    "Приложение НЕ будет работать без следующих разрешений:\n\n$permissionNames\n\nБез этих разрешений невозможно отправить сигнал SOS, GPS-координаты или позвонить экстренным контактам.\n\nПожалуйста, предоставьте разрешения.",
                    "Die Anwendung wird NICHT korrekt funktionieren ohne folgende Berechtigungen:\n\n$permissionNames\n\nOhne diese Berechtigungen ist es nicht möglich, ein SOS-Signal, GPS-Standort zu senden oder Notfallkontakte anzurufen.\n\nBitte erteilen Sie die Berechtigungen."
                )
            )
            .setPositiveButton(
                t("ODOBRI", "GRANT", "РАЗРЕШИТЬ", "ERLAUBEN")
            ) { _, _ ->
                requestNeededPermissions()
            }
            .setNegativeButton(
                t("ZATVORI", "CLOSE", "ЗАКРЫТЬ", "SCHLIESSEN"),
                null
            )
            .setCancelable(false)
            .show()
    }

    private fun showOpenSettingsDialog(deniedPermissions: List<String>) {
        val permissionNames = deniedPermissions.joinToString("\n") { permission ->
            "• " + localizePermissionName(permission)
        }

        AlertDialog.Builder(this)
            .setTitle(
                t(
                    "⚠️ DOZVOLE NISU ODOBRENE",
                    "⚠️ PERMISSIONS NOT GRANTED",
                    "⚠️ РАЗРЕШЕНИЯ НЕ ПРЕДОСТАВЛЕНЫ",
                    "⚠️ BERECHTIGUNGEN NICHT ERTEILT"
                )
            )
            .setMessage(
                t(
                    "Sledeće dozvole nisu odobrene:\n\n$permissionNames\n\nAplikacija NEĆE moći da pošalje SOS signal bez ovih dozvola.\n\nMolimo otvorite podešavanja i ručno odobrite dozvole.",
                    "The following permissions were not granted:\n\n$permissionNames\n\nThe application will NOT be able to send SOS signal without these permissions.\n\nPlease open settings and manually grant the permissions.",
                    "Следующие разрешения не были предоставлены:\n\n$permissionNames\n\nПриложение НЕ сможет отправить сигнал SOS без этих разрешений.\n\nПожалуйста, откройте настройки и вручную предоставьте разрешения.",
                    "Folgende Berechtigungen wurden nicht erteilt:\n\n$permissionNames\n\nDie Anwendung kann KEIN SOS-Signal ohne diese Berechtigungen senden.\n\nBitte öffnen Sie die Einstellungen und erteilen Sie die Berechtigungen manuell."
                )
            )
            .setPositiveButton(
                t("OTVORI PODEŠAVANJA", "OPEN SETTINGS", "ОТКРЫТЬ НАСТРОЙКИ", "EINSTELLUNGEN ÖFFNEN")
            ) { _, _ ->
                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
            .setNegativeButton(
                t("ZATVORI", "CLOSE", "ЗАКРЫТЬ", "SCHLIESSEN"),
                null
            )
            .setCancelable(false)
            .show()
    }

    private fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                AlertDialog.Builder(this)
                    .setTitle(
                        t(
                            "⚠️ OPTIMIZACIJA BATERIJE",
                            "⚠️ BATTERY OPTIMIZATION",
                            "⚠️ ОПТИМИЗАЦИЯ БАТАРЕИ",
                            "⚠️ AKKUOPTIMIERUNG"
                        )
                    )
                    .setMessage(
                        t(
                            "Android može automatski ugasiti praćenje lokacije radi uštede baterije.\n\nDa bi SOS nadzor radio neprekidno, potrebno je isključiti optimizaciju baterije za ovu aplikaciju.\n\nMolimo pritisnite DOZVOLI na sledećem ekranu.",
                            "Android may automatically stop location monitoring to save battery.\n\nTo keep SOS monitoring running continuously, battery optimization must be disabled for this app.\n\nPlease press ALLOW on the next screen.",
                            "Android может автоматически остановить мониторинг местоположения для экономии батареи.\n\nЧтобы мониторинг SOS работал непрерывно, необходимо отключить оптимизацию батареи для этого приложения.\n\nПожалуйста, нажмите РАЗРЕШИТЬ на следующем экране.",
                            "Android kann die Standortüberwachung automatisch stoppen, um den Akku zu schonen.\n\nDamit die SOS-Überwachung ununterbrochen funktioniert, muss die Akku-Optimierung für diese App deaktiviert werden.\n\nBitte drücken Sie auf dem nächsten Bildschirm ZULASSEN."
                        )
                    )
                    .setPositiveButton(
                        t("NASTAVI", "CONTINUE", "ПРОДОЛЖИТЬ", "WEITER")
                    ) { _, _ ->
                        try {
                            val intent = Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                            intent.data = Uri.parse("package:$packageName")
                            startActivity(intent)
                        } catch (e: Exception) {
                            // Ako direktan intent ne radi, otvori opšta podešavanja baterije
                            try {
                                val intent = Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                startActivity(intent)
                            } catch (_: Exception) {}
                        }
                    }
                    .setNegativeButton(
                        t("PRESKOCI", "SKIP", "ПРОПУСТИТЬ", "ÜBERSPRINGEN"),
                        null
                    )
                    .setCancelable(false)
                    .show()
            }
        }
    }

    private fun localizePermissionName(permission: String): String {
        return when (permission) {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION ->
                t("GPS lokacija", "GPS location", "GPS местоположение", "GPS-Standort")

            Manifest.permission.SEND_SMS ->
                t("Slanje SMS poruka", "Send SMS messages", "Отправка SMS", "SMS senden")

            Manifest.permission.CALL_PHONE ->
                t("Telefonski pozivi", "Phone calls", "Телефонные звонки", "Telefonanrufe")

            Manifest.permission.POST_NOTIFICATIONS ->
                t("Obaveštenja", "Notifications", "Уведомления", "Benachrichtigungen")

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
        subtitle.text = getString(R.string.subtitle_services)
        subtitle.textSize = 12f
        subtitle.setTextColor(Color.rgb(120, 120, 120))
        subtitle.gravity = Gravity.CENTER
        subtitle.setPadding(0, 0, 0, 24)

        val btnEditProfile = Button(this)
        btnEditProfile.text = getString(R.string.edit_profile)

        val btnSettings = Button(this)
        btnSettings.text = t("PODEŠAVANJA", "SETTINGS", "НАСТРОЙКИ", "EINSTELLUNGEN")

        val tacticalAdvice = TextView(this)
        tacticalAdvice.text = getString(R.string.tactical_advice)
        tacticalAdvice.textSize = 16f
        tacticalAdvice.setTextColor(Color.WHITE)
        tacticalAdvice.setPadding(24, 24, 24, 24)
        tacticalAdvice.background = roundedBackground(Color.rgb(25, 30, 38), 28)

        val btnSos = TextView(this)
        btnSos.text = getString(R.string.sos_center)
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
        btnDeactivateSos.text = "⚠ " + getString(R.string.deactivate_sos)
        btnDeactivateSos.setTextColor(Color.BLACK)
        btnDeactivateSos.setBackgroundColor(Color.rgb(255, 215, 0))

        if (isSosActive) {
            val blinkAnimation = AlphaAnimation(1.0f, 0.25f)
            blinkAnimation.duration = 500
            blinkAnimation.repeatMode = Animation.REVERSE
            blinkAnimation.repeatCount = Animation.INFINITE
            btnDeactivateSos.startAnimation(blinkAnimation)
        }

        val btnNavigate = Button(this)
        btnNavigate.text = t(
            "ZONA ZA SPASIOCE",
            "RESCUE ZONE",
            "ЗОНА СПАСАТЕЛЕЙ",
            "RETTUNGSZONE"
        )
        btnNavigate.setTextColor(Color.WHITE)
        btnNavigate.setBackgroundColor(Color.rgb(0, 130, 60))

        val btnSurvival = Button(this)
        btnSurvival.text = "🛟 " + getString(R.string.survival_tips)
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
            getString(R.string.system_ready)
        }
        status.textSize = 16f
        status.setTextColor(Color.WHITE)
        status.setPadding(24, 24, 24, 24)
        status.background = roundedBackground(
            if (isSosActive) Color.rgb(80, 0, 0) else Color.rgb(18, 22, 28),
            24
        )

        btnEditProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        btnSos.setOnClickListener {
            val active = getSharedPreferences("savio_prefs", MODE_PRIVATE)
                .getBoolean("sosActive", false)

            if (active) {
                status.text = getString(R.string.sos_already_active)
            } else {
                showEmergencyLevelDialog()
            }
        }

        btnDeactivateSos.setOnClickListener {
            val active = getSharedPreferences("savio_prefs", MODE_PRIVATE)
                .getBoolean("sosActive", false)

            if (active) {
                showDeactivateConfirmation()
            } else {
                status.text = getString(R.string.sos_not_active)
            }
        }

        btnNavigate.setOnClickListener {
            startActivity(Intent(this, RescueLoginActivity::class.java))
        }

        btnSurvival.setOnClickListener {
            showSurvivalTips()
        }

        mainContainer.addView(logo)
        mainContainer.addView(title)
        mainContainer.addView(subtitle)
        mainContainer.addView(btnEditProfile)
        mainContainer.addView(btnSettings)
        mainContainer.addView(tacticalAdvice)
        mainContainer.addView(btnSos)
        mainContainer.addView(btnDeactivateSos)
        mainContainer.addView(btnNavigate)
        mainContainer.addView(btnSurvival)
        mainContainer.addView(status)
    }

    // ─────────────────────────────────────────────
    // SOS DIJALOZI
    // ─────────────────────────────────────────────

    private fun showEmergencyLevelDialog() {
        val displayOptions = emergencyConditionDisplayOptions()
        val internalConditions = arrayOf(
            "ŽIVOTNO UGROŽEN",
            "POVREĐEN",
            "IZGUBLJEN",
            "ZAROBLJEN / NE MOGU DA SE KREĆEM",
            "POTREBNA MI JE POMOĆ"
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
            .setMessage(
                t(
                    """
                    Odabrano stanje:
                    $localizedCondition

                    Prioritet:
                    $localizedPriority

                    Ako nastavite, aplikacija će aktivirati SOS režim:

                    • poslati GPS koordinate
                    • poslati podatke iz profila
                    • poslati SMS na definisane brojeve
                    • pokrenuti poziv ka primarnom broju
                    • pokrenuti nadzor pomeranja preko 50 metara

                    Nastavite samo ako postoji stvarna potreba za hitnim obaveštavanjem.
                    """.trimIndent(),
                    """
                    Selected condition:
                    $localizedCondition

                    Priority:
                    $localizedPriority

                    If you continue, the application will activate SOS mode:

                    • send GPS coordinates
                    • send profile data
                    • send SMS to defined numbers
                    • start a call to the primary number
                    • start movement monitoring over 50 meters

                    Continue only if there is a real need for emergency notification.
                    """.trimIndent(),
                    """
                    Выбранное состояние:
                    $localizedCondition

                    Приоритет:
                    $localizedPriority

                    Если вы продолжите, приложение активирует режим SOS:

                    • отправит GPS-координаты
                    • отправит данные профиля
                    • отправит SMS на заданные номера
                    • выполнит звонок на основной номер
                    • запустит контроль перемещения более 50 метров

                    Продолжайте только при реальной необходимости экстренного уведомления.
                    """.trimIndent(),
                    """
                    Ausgewählter Zustand:
                    $localizedCondition

                    Priorität:
                    $localizedPriority

                    Wenn Sie fortfahren, aktiviert die Anwendung den SOS-Modus:

                    • GPS-Koordinaten senden
                    • Profildaten senden
                    • SMS an festgelegte Nummern senden
                    • Anruf an die primäre Nummer starten
                    • Bewegungsüberwachung über 50 Meter starten

                    Fahren Sie nur fort, wenn wirklich eine Notfallbenachrichtigung erforderlich ist.
                    """.trimIndent()
                )
            )
            .setPositiveButton(t("AKTIVIRAJ SOS", "ACTIVATE SOS", "АКТИВИРОВАТЬ SOS", "SOS AKTIVIEREN")) { _, _ ->
                activateSos(priority, condition)
            }
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
            .setTitle(t(
                "RAZLOG DEAKTIVACIJE SOS-a",
                "REASON FOR SOS DEACTIVATION",
                "ПРИЧИНА ОТКЛЮЧЕНИЯ SOS",
                "GRUND DER SOS-DEAKTIVIERUNG"
            ))
            .setItems(reasons) { _, which ->
                val selectedReason = reasons[which]

                if (which == 5) {
                    // Drugi razlog — slobodan unos
                    showCustomReasonDialog()
                } else {
                    showFinalDeactivateConfirmation(selectedReason)
                }
            }
            .setNegativeButton(t("ODUSTANI", "CANCEL", "ОТМЕНА", "ABBRECHEN"), null)
            .show()
    }

    private fun showCustomReasonDialog() {
        val input = android.widget.EditText(this)
        input.hint = t(
            "Unesite razlog...",
            "Enter reason...",
            "Введите причину...",
            "Grund eingeben..."
        )
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
            .setMessage(
                t(
                    "Razlog: $reason\n\nKorisnik: $fullName\n\nPoruka o deaktivaciji biće poslata spasiocima.\n\nNastavite?",
                    "Reason: $reason\n\nUser: $fullName\n\nA deactivation message will be sent to rescuers.\n\nContinue?",
                    "Причина: $reason\n\nПользователь: $fullName\n\nСообщение об отключении будет отправлено спасателям.\n\nПродолжить?",
                    "Grund: $reason\n\nBenutzer: $fullName\n\nEine Deaktivierungsnachricht wird an die Rettungskräfte gesendet.\n\nFortfahren?"
                )
            )
            .setPositiveButton(t("DEAKTIVIRAJ", "DEACTIVATE", "ОТКЛЮЧИТЬ", "DEAKTIVIEREN")) { _, _ ->
                deactivateSos(reason)
            }
            .setNegativeButton(t("ODUSTANI", "CANCEL", "ОТМЕНА", "ABBRECHEN"), null)
            .show()
    }

    // ─────────────────────────────────────────────
    // SOS LOGIKA
    // ─────────────────────────────────────────────

    private fun activateSos(priority: String, condition: String) {
        if (!hasRequiredPermissions()) {
            status.text = getString(R.string.missing_permissions)
            requestNeededPermissions()
            return
        }

        // Prikaži poruku da se čeka GPS
        status.text = t(
            "Tražim GPS lokaciju...",
            "Getting GPS location...",
            "Получение GPS-координат...",
            "GPS-Standort wird ermittelt..."
        )

        // Traži svježu lokaciju, pa tek onda aktiviraj SOS
        getFreshLocation { location ->
            val incidentId = generateIncidentId()

            getSharedPreferences("savio_prefs", MODE_PRIVATE)
                .edit()
                .putString("incidentId", incidentId)
                .putString("incidentPriority", priority)
                .putString("incidentCondition", condition)
                .apply()

            val message = buildSosMessage(location, incidentId, priority, condition)

            sosNumbers.forEach { number ->
                sendSms(number, message)
            }

            if (location != null) {
                saveSosActive(location, incidentId, priority, condition)
                startLocationMonitor(location, incidentId, priority, condition)
            } else {
                getSharedPreferences("savio_prefs", MODE_PRIVATE)
                    .edit()
                    .putBoolean("sosActive", true)
                    .putString("incidentId", incidentId)
                    .putString("incidentPriority", priority)
                    .putString("incidentCondition", condition)
                    .apply()

                status.text = getString(R.string.sos_sent_no_location)
            }

            callPrimaryNumber()

            status.text = buildSosActivatedStatusText(incidentId, priority, condition)

            mainContainer.removeAllViews()
            buildHomeScreen()
        }
    }

    private fun deactivateSos(reason: String) {
        if (!hasRequiredPermissions()) {
            status.text = getString(R.string.missing_sms_gps_permissions)
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

        sosNumbers.forEach { number ->
            sendSms(number, message)
        }

        prefs.edit()
            .putBoolean("sosActive", false)
            .remove("sosStartLat")
            .remove("sosStartLon")
            .remove("incidentId")
            .remove("incidentPriority")
            .remove("incidentCondition")
            .apply()

        status.text = buildSosDeactivatedStatusText(incidentId)

        mainContainer.removeAllViews()
        buildHomeScreen()
    }

    private fun saveSosActive(
        location: Location,
        incidentId: String,
        priority: String,
        condition: String
    ) {
        getSharedPreferences("savio_prefs", MODE_PRIVATE)
            .edit()
            .putBoolean("sosActive", true)
            .putString("incidentId", incidentId)
            .putString("incidentPriority", priority)
            .putString("incidentCondition", condition)
            .putString("sosStartLat", location.latitude.toString())
            .putString("sosStartLon", location.longitude.toString())
            .apply()
    }

    private fun startLocationMonitor(
        startLocation: Location,
        incidentId: String,
        priority: String,
        condition: String
    ) {
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

    // Traži svježu GPS lokaciju — timeout 8 sekundi, pa padne na staru
    private fun getFreshLocation(callback: (Location?) -> Unit) {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
                callback(getLastKnownLocation())
                return
            }

            var called = false

            val listener = object : android.location.LocationListener {
                override fun onLocationChanged(location: Location) {
                    if (!called) {
                        called = true
                        locationManager.removeUpdates(this)
                        callback(location)
                    }
                }
                override fun onProviderDisabled(provider: String) {}
                override fun onProviderEnabled(provider: String) {}
                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
            }

            // Timeout — ako GPS ne odgovori za 8 sekundi, koristi staru lokaciju
            val timeoutRunnable = Runnable {
                if (!called) {
                    called = true
                    try { locationManager.removeUpdates(listener) } catch (_: Exception) {}
                    callback(getLastKnownLocation())
                }
            }

            val handler = android.os.Handler(mainLooper)
            handler.postDelayed(timeoutRunnable, 8000L)

            // Pokušaj GPS prvo, pa Network
            var requested = false
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, listener, mainLooper)
                requested = true
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, listener, mainLooper)
                requested = true
            }

            if (!requested) {
                handler.removeCallbacks(timeoutRunnable)
                called = true
                callback(getLastKnownLocation())
            }

        } catch (e: Exception) {
            callback(getLastKnownLocation())
        }
    }

    private fun getLastKnownLocation(): Location? {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val gpsLocation = try {
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        } catch (e: SecurityException) {
            null
        }

        val networkLocation = try {
            locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        } catch (e: SecurityException) {
            null
        }

        return gpsLocation ?: networkLocation
    }

    // ─────────────────────────────────────────────
    // PORUKE
    // ─────────────────────────────────────────────

    private fun buildSosMessage(
        location: Location?,
        incidentId: String,
        priority: String,
        condition: String
    ): String {
        val prefs = getSharedPreferences("savio_prefs", MODE_PRIVATE)

        val fullName = prefs.getString("fullName", "Nije uneto")
        val age = prefs.getString("age", "Nije uneto")
        val phone = prefs.getString("phone", "Nije uneto")
        val bloodType = prefs.getString("bloodType", "Nije uneto")
        val chronicDiseases = prefs.getString("chronicDiseases", "Nije uneto")

        val time = SimpleDateFormat("dd.MM.yyyy. HH:mm:ss", Locale.getDefault()).format(Date())
        val battery = getBatteryPercent()

        val locationText = if (location != null) {
            val lat = location.latitude
            val lon = location.longitude
            """
            Lokacija:
            https://maps.google.com/?q=$lat,$lon

            Koordinate:
            $lat, $lon
            """.trimIndent()
        } else {
            "Lokacija trenutno nije dostupna."
        }

        return """
            SOS UPOZORENJE!

            Incident ID: $incidentId

            PRIORITET: $priority
            STANJE: $condition

            Potrebna je hitna pomoć.

            Ime i prezime: $fullName
            Godine: $age
            Telefon korisnika: $phone
            Krvna grupa: $bloodType
            Hronične bolesti / terapija: $chronicDiseases

            Vreme aktivacije: $time
            Baterija uređaja: $battery%

            $locationText

            Napomena:
            Osoba je upozorena da ne napušta lokaciju osim ako je životno ugrožena.
        """.trimIndent()
    }

    private fun buildCancelMessage(
        location: Location?,
        incidentId: String,
        priority: String,
        condition: String,
        reason: String
    ): String {
        val time = SimpleDateFormat("dd.MM.yyyy. HH:mm:ss", Locale.getDefault()).format(Date())
        val battery = getBatteryPercent()

        val prefs = getSharedPreferences("savio_prefs", MODE_PRIVATE)
        val fullName = prefs.getString("fullName", "Nije uneto") ?: "Nije uneto"
        val age = prefs.getString("age", "Nije uneto") ?: "Nije uneto"
        val phone = prefs.getString("phone", "Nije uneto") ?: "Nije uneto"
        val bloodType = prefs.getString("bloodType", "Nije uneto") ?: "Nije uneto"

        val locationText = if (location != null) {
            val lat = location.latitude
            val lon = location.longitude
            """
            Poslednja poznata lokacija:
            https://maps.google.com/?q=$lat,$lon

            Koordinate:
            $lat, $lon
            """.trimIndent()
        } else {
            "Poslednja poznata lokacija trenutno nije dostupna."
        }

        return """
            SOS OTKAZAN

            Incident ID: $incidentId

            RAZLOG DEAKTIVACIJE:
            $reason

            PODACI O LICU:
            Ime i prezime: $fullName
            Godine: $age
            Telefon: $phone
            Krvna grupa: $bloodType

            PRIORITET: $priority
            STANJE: $condition

            Vreme deaktivacije: $time
            Baterija uređaja: $battery%

            $locationText
        """.trimIndent()
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

        // Nakon što se poziv uspostavi, pokreni TTS poruku
        Handler(Looper.getMainLooper()).postDelayed({
            startTtsMessage()
        }, 2000L) // Čekaj 2 sekunde da se poziv uspostavi
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
        } else {
            "GPS koordinate trenutno nisu dostupne."
        }

        // Broj telefona izgovoren cifra po cifra
        val phoneSpoken = phone.map { c ->
            when (c) {
                '0' -> "nula"
                '1' -> "jedan"
                '2' -> "dva"
                '3' -> "tri"
                '4' -> "četiri"
                '5' -> "pet"
                '6' -> "šest"
                '7' -> "sedam"
                '8' -> "osam"
                '9' -> "devet"
                '+' -> "plus"
                else -> c.toString()
            }
        }.joinToString(" ")

        val ttsMessage = """
            Pažnja! SOS uzbuna!
            Ako možete da govorite, jednostavno progovorite i bićete čuti.
            Ovo je automatska poruka aplikacije SAVIO SOS.
            Osoba je aktivirala SOS signal i možda nije u stanju da govori.
            Ime i prezime: $fullName.
            Broj telefona pozivaoca: $phoneSpoken.
            Krvna grupa: $bloodType.
            Hronične bolesti i terapija: $chronicDiseases.
            $gpsText
            GPS koordinate i lokacija su poslati i SMS porukom.
            Poruka će biti ponovljena još dva puta.
        """.trimIndent()

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val langResult = tts?.setLanguage(Locale("sr"))
                if (langResult == TextToSpeech.LANG_MISSING_DATA ||
                    langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.language = Locale.getDefault()
                }

                tts?.setSpeechRate(0.85f)
                repeatTtsMessage(ttsMessage, 3)
            }
        }
    }

    private fun repeatTtsMessage(message: String, timesLeft: Int) {
        if (timesLeft <= 0) {
            tts?.stop()
            tts?.shutdown()
            tts = null
            return
        }

        tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "SOS_TTS_$timesLeft")

        // Procijeni trajanje poruke (~6 sekundi po 100 znakova) + 5 sekundi pauza
        val estimatedDuration = (message.length / 100.0 * 6000).toLong() + 5000L

        Handler(Looper.getMainLooper()).postDelayed({
            repeatTtsMessage(message, timesLeft - 1)
        }, estimatedDuration)
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        super.onDestroy()
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
        return getSharedPreferences("savio_prefs", MODE_PRIVATE)
            .getString("language", "sr") ?: "sr"
    }

    private fun t(sr: String, en: String, ru: String, de: String): String {
        return when (currentLanguage()) {
            "en" -> en
            "ru" -> ru
            "de" -> de
            else -> sr
        }
    }

    private fun unknownText(): String {
        return t("Nepoznat", "Unknown", "Неизвестно", "Unbekannt")
    }

    // ─────────────────────────────────────────────
    // LOKALIZACIJA STANJA I PRIORITETA
    // ─────────────────────────────────────────────

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

    // ─────────────────────────────────────────────
    // TEKSTOVI STATUSA
    // ─────────────────────────────────────────────

    private fun buildActiveStatusText(
        incidentId: String,
        priority: String,
        condition: String
    ): String {
        return """
            ${t("SOS JE AKTIVAN", "SOS IS ACTIVE", "SOS АКТИВЕН", "SOS IST AKTIV")}

            Incident ID: $incidentId
            ${t("Prioritet", "Priority", "Приоритет", "Priorität")}: ${localizePriority(priority)}
            ${t("Stanje", "Condition", "Состояние", "Zustand")}: ${localizeCondition(condition)}

            ${getString(R.string.location_monitor_active)}
            ${getString(R.string.do_not_move_warning)}
        """.trimIndent()
    }

    private fun buildSosActivatedStatusText(
        incidentId: String,
        priority: String,
        condition: String
    ): String {
        return t(
            """
            SOS AKTIVIRAN

            Incident ID: $incidentId
            Prioritet: ${localizePriority(priority)}
            Stanje: ${localizeCondition(condition)}

            Poruke su poslate na definisane brojeve.
            Pokrenut je poziv ka primarnom broju.
            Nadzor pomeranja je aktiviran.

            Ne menjajte lokaciju osim ako ste životno ugroženi.
            """.trimIndent(),
            """
            SOS ACTIVATED

            Incident ID: $incidentId
            Priority: ${localizePriority(priority)}
            Condition: ${localizeCondition(condition)}

            Messages were sent to the defined numbers.
            A call to the primary number was started.
            Movement monitoring has been activated.

            Do not change location unless your life is in danger.
            """.trimIndent(),
            """
            SOS АКТИВИРОВАН

            Incident ID: $incidentId
            Приоритет: ${localizePriority(priority)}
            Состояние: ${localizeCondition(condition)}

            Сообщения отправлены на заданные номера.
            Запущен звонок на основной номер.
            Контроль перемещения активирован.

            Не меняйте местоположение, если вашей жизни ничего не угрожает.
            """.trimIndent(),
            """
            SOS AKTIVIERT

            Incident ID: $incidentId
            Priorität: ${localizePriority(priority)}
            Zustand: ${localizeCondition(condition)}

            Nachrichten wurden an die festgelegten Nummern gesendet.
            Ein Anruf an die primäre Nummer wurde gestartet.
            Bewegungsüberwachung wurde aktiviert.

            Ändern Sie Ihren Standort nicht, es sei denn, Ihr Leben ist in Gefahr.
            """.trimIndent()
        )
    }

    private fun buildSosDeactivatedStatusText(incidentId: String): String {
        return t(
            """
            SOS DEAKTIVIRAN

            Incident ID: $incidentId

            Nadzor lokacije je zaustavljen.
            Poruka o otkazivanju SOS režima poslata je na definisane brojeve.
            """.trimIndent(),
            """
            SOS DEACTIVATED

            Incident ID: $incidentId

            Location monitoring has been stopped.
            A cancellation message was sent to the defined numbers.
            """.trimIndent(),
            """
            SOS ОТКЛЮЧЕН

            Incident ID: $incidentId

            Контроль местоположения остановлен.
            Сообщение об отмене SOS отправлено на заданные номера.
            """.trimIndent(),
            """
            SOS DEAKTIVIERT

            Incident ID: $incidentId

            Standortüberwachung wurde gestoppt.
            Eine Abbruchnachricht wurde an die festgelegten Nummern gesendet.
            """.trimIndent()
        )
    }

    // ─────────────────────────────────────────────
    // SAVETI ZA PREŽIVLJAVANJE
    // ─────────────────────────────────────────────

    private fun survivalTipsText(): String {
        return t(
            """
            SAVETI ZA PREŽIVLJAVANJE

            ŠUMA:
            • Ostanite na jednom mestu ako ste već poslali SOS.
            • Napravite vidljiv znak od grana, odeće ili kamenja.
            • Ne trošite bateriju bez potrebe.
            • Ako čujete spasioce, odgovarajte kratkim i jasnim signalima.

            SNEG:
            • Zaštitite glavu, šake i stopala.
            • Ne sedite direktno na sneg.
            • Napravite zaklon od vetra.
            • Ne krećite se bez potrebe ako ste iscrpljeni.

            VODA:
            • Ne trošite snagu nepotrebnim plivanjem.
            • Pokušajte da plutate i smirite disanje.
            • Ako ste blizu obale, krećite se ukoso ka njoj.

            SUNCE I VRUĆINA:
            • Sklonite se u hlad.
            • Pokrijte glavu.
            • Štedite vodu.
            • Ne izlažite se direktnom suncu bez potrebe.

            MEDICINSKI SAVET:
            • Ako krvarite, pritisnite ranu čistom tkaninom.
            • Ako sumnjate na prelom, ne pomerajte povređeni deo.
            • Ostanite mirni i dišite polako.

            BATERIJA:
            • Smanjite osvetljenje ekrana.
            • Ne koristite kameru bez potrebe.
            • Ne zatvarajte aplikaciju ako je SOS aktivan.
            • Ostanite dostupni za poziv.
            """.trimIndent(),
            """
            SURVIVAL TIPS

            FOREST:
            • Stay in one place if you have already sent SOS.
            • Make a visible sign using branches, clothing or stones.
            • Do not waste battery unnecessarily.
            • If you hear rescuers, respond with short and clear signals.

            SNOW:
            • Protect your head, hands and feet.
            • Do not sit directly on snow.
            • Make shelter from the wind.
            • Do not move unnecessarily if you are exhausted.

            WATER:
            • Do not waste strength by swimming unnecessarily.
            • Try to float and calm your breathing.
            • If you are near the shore, move diagonally toward it.

            SUN AND HEAT:
            • Move into shade.
            • Cover your head.
            • Save water.
            • Do not expose yourself to direct sun unless necessary.

            MEDICAL ADVICE:
            • If you are bleeding, press the wound with clean cloth.
            • If you suspect a fracture, do not move the injured part.
            • Stay calm and breathe slowly.

            BATTERY:
            • Reduce screen brightness.
            • Do not use the camera unnecessarily.
            • Do not close the application while SOS is active.
            • Stay available for a call.
            """.trimIndent(),
            """
            СОВЕТЫ ПО ВЫЖИВАНИЮ

            ЛЕС:
            • Оставайтесь на одном месте, если уже отправили SOS.
            • Сделайте заметный знак из веток, одежды или камней.
            • Не расходуйте батарею без необходимости.
            • Если слышите спасателей, отвечайте короткими и ясными сигналами.

            СНЕГ:
            • Защитите голову, руки и ноги.
            • Не садитесь прямо на снег.
            • Сделайте укрытие от ветра.
            • Не двигайтесь без необходимости, если вы истощены.

            ВОДА:
            • Не тратьте силы на лишнее плавание.
            • Попробуйте держаться на воде и успокоить дыхание.
            • Если берег рядом, двигайтесь к нему по диагонали.

            СОЛНЦЕ И ЖАРА:
            • Перейдите в тень.
            • Накройте голову.
            • Экономьте воду.
            • Не находитесь под прямым солнцем без необходимости.

            МЕДИЦИНСКИЙ СОВЕТ:
            • При кровотечении прижмите рану чистой тканью.
            • При подозрении на перелом не двигайте повреждённую часть.
            • Сохраняйте спокойствие и дышите медленно.

            БАТАРЕЯ:
            • Уменьшите яркость экрана.
            • Не используйте камеру без необходимости.
            • Не закрывайте приложение, если SOS активен.
            • Оставайтесь доступными для звонка.
            """.trimIndent(),
            """
            ÜBERLEBENSTIPPS

            WALD:
            • Bleiben Sie an einem Ort, wenn Sie bereits SOS gesendet haben.
            • Machen Sie ein sichtbares Zeichen aus Ästen, Kleidung oder Steinen.
            • Verschwenden Sie den Akku nicht unnötig.
            • Wenn Sie Rettungskräfte hören, antworten Sie mit kurzen und klaren Signalen.

            SCHNEE:
            • Schützen Sie Kopf, Hände und Füße.
            • Setzen Sie sich nicht direkt auf Schnee.
            • Bauen Sie einen Windschutz.
            • Bewegen Sie sich nicht unnötig, wenn Sie erschöpft sind.

            WASSER:
            • Verschwenden Sie keine Kraft durch unnötiges Schwimmen.
            • Versuchen Sie zu treiben und ruhig zu atmen.
            • Wenn das Ufer nahe ist, bewegen Sie sich schräg darauf zu.

            SONNE UND HITZE:
            • Gehen Sie in den Schatten.
            • Bedecken Sie Ihren Kopf.
            • Sparen Sie Wasser.
            • Setzen Sie sich nicht unnötig direkter Sonne aus.

            MEDIZINISCHER HINWEIS:
            • Wenn Sie bluten, drücken Sie die Wunde mit einem sauberen Tuch.
            • Bei Verdacht auf einen Bruch bewegen Sie den verletzten Teil nicht.
            • Bleiben Sie ruhig und atmen Sie langsam.

            AKKU:
            • Reduzieren Sie die Bildschirmhelligkeit.
            • Verwenden Sie die Kamera nicht unnötig.
            • Schließen Sie die Anwendung nicht, wenn SOS aktiv ist.
            • Bleiben Sie für einen Anruf erreichbar.
            """.trimIndent()
        )
    }
}
