package com.example.savio4

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.telephony.TelephonyManager
import android.text.InputType
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.database.FirebaseDatabase

class TeamLoginActivity : AppCompatActivity() {

    private val teamUsername = "spasilac"
    private val teamPassword = "savio112"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("savio_prefs", MODE_PRIVATE)

        // ─── PROVJERA ZAPAMĆENIH KREDENCIJALA ───
        val savedUsername = prefs.getString("team_username", null)
        val savedPassword = prefs.getString("team_password", null)

        if (savedUsername == teamUsername && savedPassword == teamPassword) {
            checkActiveSessionAndProceed(prefs)
            return
        }

        buildLoginScreen(prefs)
    }

    private fun checkActiveSessionAndProceed(prefs: android.content.SharedPreferences) {
        val existingMissionCode = prefs.getString("teamMissionCode", null)
        val existingName = prefs.getString("teamRescuerName", null)

        if (!existingMissionCode.isNullOrEmpty() && !existingName.isNullOrEmpty()) {
            val db = FirebaseDatabase.getInstance()
            db.getReference("missions").child(existingMissionCode).child("active")
                .get().addOnSuccessListener { snapshot ->
                    val isActive = snapshot.getValue(Boolean::class.java) ?: false
                    if (isActive) {
                        startActivity(Intent(this, TeamMapActivity::class.java))
                        finish()
                    } else {
                        prefs.edit()
                            .remove("teamMissionCode").remove("teamMissionName")
                            .remove("teamIsCoordinator").remove("teamIsObserver")
                            .apply()
                        buildNameScreen(prefs)
                    }
                }.addOnFailureListener {
                    buildNameScreen(prefs)
                }
        } else {
            buildNameScreen(prefs)
        }
    }

    // ─────────────────────────────────────────────
    // LOGIN EKRAN
    // ─────────────────────────────────────────────

    private fun buildLoginScreen(prefs: android.content.SharedPreferences) {
        val scrollView = ScrollView(this)
        scrollView.setBackgroundColor(Color.rgb(10, 12, 16))

        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(32, 50, 32, 32)

        val title = TextView(this)
        title.text = t("ZONA ZA SPASIOCE", "RESCUE ZONE", "ЗОНА СПАСАТЕЛЕЙ", "RETTUNGSZONE")
        title.textSize = 28f
        title.setTextColor(Color.WHITE)
        title.setPadding(0, 0, 0, 8)

        val subtitle = TextView(this)
        subtitle.text = t(
            "SAMO ZA OVLAŠĆENA LICA\n\nOvaj deo aplikacije namenjen je spasiocima i osobama koje pružaju pomoć.",
            "AUTHORIZED PERSONNEL ONLY\n\nThis part is intended for rescuers and people providing assistance.",
            "ТОЛЬКО ДЛЯ УПОЛНОМОЧЕННЫХ ЛИЦ\n\nЭта часть предназначена для спасателей.",
            "NUR FÜR BERECHTIGTE PERSONEN\n\nDieser Bereich ist für Rettungskräfte vorgesehen."
        )
        subtitle.textSize = 15f
        subtitle.setTextColor(Color.LTGRAY)
        subtitle.setPadding(0, 0, 0, 24)

        val usernameInput = EditText(this)
        usernameInput.hint = t("Korisničko ime", "Username", "Имя пользователя", "Benutzername")
        usernameInput.setTextColor(Color.WHITE)
        usernameInput.setHintTextColor(Color.LTGRAY)

        val passwordInput = EditText(this)
        passwordInput.hint = t("Lozinka", "Password", "Пароль", "Passwort")
        passwordInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        passwordInput.setTextColor(Color.WHITE)
        passwordInput.setHintTextColor(Color.LTGRAY)

        val rememberMe = CheckBox(this)
        rememberMe.text = t(
            "Zapamti akreditaciju na ovom uređaju",
            "Remember credentials on this device",
            "Запомнить данные на этом устройстве",
            "Zugangsdaten auf diesem Gerät speichern"
        )
        rememberMe.setTextColor(Color.LTGRAY)
        rememberMe.setPadding(0, 12, 0, 4)

        val rememberNote = TextView(this)
        rememberNote.text = t(
            "⚠️ Ako se aplikacija resetuje ili ponovo instalira, akreditacija će biti izbrisana.",
            "⚠️ If the app is reset or reinstalled, credentials will be deleted.",
            "⚠️ При сбросе или переустановке данные будут удалены.",
            "⚠️ Bei App-Reset werden die Zugangsdaten gelöscht."
        )
        rememberNote.textSize = 12f
        rememberNote.setTextColor(Color.rgb(140, 140, 140))
        rememberNote.setPadding(0, 0, 0, 16)

        val btnLogin = Button(this)
        btnLogin.text = t("ULAZ ZA SPASIOCE", "RESCUER LOGIN", "ВХОД ДЛЯ СПАСАТЕЛЕЙ", "RETTUNGSKRÄFTE LOGIN")
        btnLogin.setTextColor(Color.WHITE)
        btnLogin.setBackgroundColor(Color.rgb(0, 100, 180))

        val btnBack = Button(this)
        btnBack.text = t("NAZAD", "BACK", "НАЗАД", "ZURÜCK")

        val status = TextView(this)
        status.text = ""
        status.textSize = 16f
        status.setTextColor(Color.RED)
        status.setPadding(0, 18, 0, 0)

        btnLogin.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (username == teamUsername && password == teamPassword) {
                if (rememberMe.isChecked) {
                    prefs.edit()
                        .putString("team_username", username)
                        .putString("team_password", password)
                        .apply()
                }
                buildNameScreen(prefs)
            } else {
                status.text = t(
                    "⛔ Pogrešno korisničko ime ili lozinka.",
                    "⛔ Wrong username or password.",
                    "⛔ Неверное имя пользователя или пароль.",
                    "⛔ Falscher Benutzername oder Passwort."
                )
            }
        }

        btnBack.setOnClickListener { finish() }

        container.addView(title)
        container.addView(subtitle)
        container.addView(usernameInput)
        container.addView(passwordInput)
        container.addView(rememberMe)
        container.addView(rememberNote)
        container.addView(btnLogin)
        container.addView(btnBack)
        container.addView(status)

        scrollView.addView(container)
        setContentView(scrollView)
        applyWindowInsets()
    }

    // ─────────────────────────────────────────────
    // EKRAN SA IMENOM I BROJEM
    // ─────────────────────────────────────────────

    private fun buildNameScreen(prefs: android.content.SharedPreferences) {
        val gpsOk = isGpsEnabled()
        val internetOk = isInternetAvailable()

        val scrollView = ScrollView(this)
        scrollView.setBackgroundColor(Color.rgb(10, 12, 16))

        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(32, 50, 32, 32)

        val title = TextView(this)
        title.text = "SAVIO TEAM"
        title.textSize = 32f
        title.setTextColor(Color.rgb(0, 150, 220))
        title.setPadding(0, 0, 0, 8)

        val subtitle = TextView(this)
        subtitle.text = t(
            "Koordinacija spasilacke ekipe",
            "Rescue team coordination",
            "Координация спасательной группы",
            "Koordination des Rettungsteams"
        )
        subtitle.textSize = 16f
        subtitle.setTextColor(Color.rgb(120, 120, 120))
        subtitle.setPadding(0, 0, 0, 24)

        if (!gpsOk || !internetOk) {
            buildTechWarningScreen(container, title, subtitle, gpsOk, internetOk)
        } else {
            buildFormScreen(container, title, subtitle, prefs)
        }

        scrollView.addView(container)
        setContentView(scrollView)
    }

    private fun buildTechWarningScreen(
        container: LinearLayout,
        title: TextView,
        subtitle: TextView,
        gpsOk: Boolean,
        internetOk: Boolean
    ) {
        val gpsStatus = if (gpsOk)
            t("GPS: UKLJUCEN ✓", "GPS: ON ✓", "GPS: ВКЛ ✓", "GPS: EIN ✓")
        else
            t("GPS: ISKLJUCEN ✗", "GPS: OFF ✗", "GPS: ВЫКЛ ✗", "GPS: AUS ✗")

        val internetStatus = if (internetOk)
            t("Internet: DOSTUPAN ✓", "Internet: AVAILABLE ✓", "Интернет: ДОСТУПЕН ✓", "Internet: VERFÜGBAR ✓")
        else
            t("Internet: NIJE DOSTUPAN ✗", "Internet: NOT AVAILABLE ✗", "Интернет: НЕДОСТУПЕН ✗", "Internet: NICHT VERFÜGBAR ✗")

        val warningBox = TextView(this)
        warningBox.text = t(
            "TEHNICKI ZAHTJEVI NISU ISPUNJENI\n\n$gpsStatus\n$internetStatus\n\nUkljucite GPS i internet prije nastavka.",
            "TECHNICAL REQUIREMENTS NOT MET\n\n$gpsStatus\n$internetStatus\n\nEnable GPS and internet before continuing.",
            "ТЕХНИЧЕСКИЕ ТРЕБОВАНИЯ НЕ ВЫПОЛНЕНЫ\n\n$gpsStatus\n$internetStatus",
            "ANFORDERUNGEN NICHT ERFÜLLT\n\n$gpsStatus\n$internetStatus"
        )
        warningBox.textSize = 14f
        warningBox.setTextColor(Color.WHITE)
        warningBox.setPadding(24, 24, 24, 24)

        val bg = GradientDrawable()
        bg.setColor(Color.rgb(80, 0, 0))
        bg.cornerRadius = 16f
        bg.setStroke(2, Color.RED)
        warningBox.background = bg

        val warningParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        warningParams.setMargins(0, 0, 0, 16)
        warningBox.layoutParams = warningParams

        val btnSettings = Button(this)
        btnSettings.text = t("OTVORI PODESAVANJA", "OPEN SETTINGS", "ОТКРЫТЬ НАСТРОЙКИ", "EINSTELLUNGEN ÖFFNEN")
        btnSettings.setTextColor(Color.WHITE)
        btnSettings.setBackgroundColor(Color.rgb(150, 0, 0))
        val sp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        sp.setMargins(0, 0, 0, 8)
        btnSettings.layoutParams = sp
        btnSettings.setOnClickListener { startActivity(Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS)) }

        val btnRefresh = Button(this)
        btnRefresh.text = t("PROVJERI PONOVO", "CHECK AGAIN", "ПРОВЕРИТЬ СНОВА", "ERNEUT PRÜFEN")
        btnRefresh.setTextColor(Color.WHITE)
        btnRefresh.setBackgroundColor(Color.rgb(0, 80, 160))
        btnRefresh.setOnClickListener { recreate() }

        container.addView(title)
        container.addView(subtitle)
        container.addView(warningBox)
        container.addView(btnSettings)
        container.addView(btnRefresh)
    }

    private fun buildFormScreen(
        container: LinearLayout,
        title: TextView,
        subtitle: TextView,
        prefs: android.content.SharedPreferences
    ) {
        val techOkBox = TextView(this)
        techOkBox.text = t(
            "GPS: UKLJUCEN ✓\nInternet: DOSTUPAN ✓\n\nVasa lokacija ce biti vidljiva ostalim spasiocima.",
            "GPS: ON ✓\nInternet: AVAILABLE ✓\n\nYour location will be visible to other rescuers.",
            "GPS: ВКЛ ✓\nИнтернет: ДОСТУПЕН ✓\n\nВаше местоположение будет видно другим спасателям.",
            "GPS: EIN ✓\nInternet: VERFÜGBAR ✓\n\nIhr Standort ist für andere Retter sichtbar."
        )
        techOkBox.textSize = 14f
        techOkBox.setTextColor(Color.WHITE)
        techOkBox.setPadding(24, 20, 24, 20)
        val okBg = GradientDrawable()
        okBg.setColor(Color.rgb(0, 50, 20))
        okBg.cornerRadius = 16f
        okBg.setStroke(2, Color.rgb(0, 180, 80))
        techOkBox.background = okBg
        val okParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        okParams.setMargins(0, 0, 0, 24)
        techOkBox.layoutParams = okParams

        // ─── IME ───
        val nameLabel = TextView(this)
        nameLabel.text = t("Vase ime za ovu akciju:", "Your name for this mission:", "Ваше имя для этой операции:", "Ihr Name:")
        nameLabel.textSize = 16f
        nameLabel.setTextColor(Color.WHITE)
        nameLabel.setPadding(0, 0, 0, 8)

        val nameInput = EditText(this)
        nameInput.hint = t("Npr. Nemanja Zajecar", "E.g. John Belgrade", "Напр. Иван Белград", "z.B. Klaus Berlin")
        nameInput.setTextColor(Color.WHITE)
        nameInput.setHintTextColor(Color.rgb(120, 120, 120))
        nameInput.textSize = 18f
        nameInput.setText(prefs.getString("teamRescuerName", "") ?: "")
        val nameParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        nameParams.setMargins(0, 0, 0, 24)
        nameInput.layoutParams = nameParams

        // ─── BROJ TELEFONA — automatski + ručni unos ───
        val phoneLabel = TextView(this)
        phoneLabel.text = t(
            "Vaš broj telefona:",
            "Your phone number:",
            "Ваш номер телефона:",
            "Ihre Telefonnummer:"
        )
        phoneLabel.textSize = 16f
        phoneLabel.setTextColor(Color.WHITE)
        phoneLabel.setPadding(0, 0, 0, 8)

        val phoneInput = EditText(this)
        phoneInput.inputType = android.text.InputType.TYPE_CLASS_PHONE
        phoneInput.setTextColor(Color.WHITE)
        phoneInput.setHintTextColor(Color.rgb(120, 120, 120))
        phoneInput.textSize = 16f

        // Pokušaj automatskog čitanja broja sa SIM kartice
        val simNumber = getSimPhoneNumber()
        val savedPhone = prefs.getString("teamRescuerPhone", "") ?: ""

        if (simNumber.isNotEmpty()) {
            // SIM broj uspešno pročitan
            phoneInput.setText(simNumber)
            val phoneNote = TextView(this)
            phoneNote.text = "✓ " + t(
                "Broj automatski preuzet sa SIM kartice. Možete ga promijeniti ako je potrebno.",
                "Number automatically read from SIM card. You can change it if needed.",
                "Номер автоматически получен с SIM-карты. Можно изменить при необходимости.",
                "Nummer automatisch von der SIM-Karte gelesen. Bei Bedarf änderbar."
            )
            phoneNote.textSize = 12f
            phoneNote.setTextColor(Color.rgb(0, 180, 80))
            phoneNote.setPadding(0, 4, 0, 0)

            val phoneParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            phoneParams.setMargins(0, 0, 0, 4)
            phoneInput.layoutParams = phoneParams

            val noteParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            noteParams.setMargins(0, 0, 0, 24)
            phoneNote.layoutParams = noteParams

            container.addView(title)
            container.addView(subtitle)
            container.addView(techOkBox)
            container.addView(nameLabel)
            container.addView(nameInput)
            container.addView(phoneLabel)
            container.addView(phoneInput)
            container.addView(phoneNote)

        } else {
            // SIM broj nije dostupan — ručni unos
            if (savedPhone.isNotEmpty()) {
                phoneInput.setText(savedPhone)
            } else {
                phoneInput.hint = t(
                    "Npr. +381641234567 (unesite ručno)",
                    "E.g. +381641234567 (enter manually)",
                    "Напр. +381641234567 (введите вручную)",
                    "z.B. +381641234567 (manuell eingeben)"
                )
            }

            val phoneNote = TextView(this)
            phoneNote.text = "ℹ️ " + t(
                "Broj nije automatski dostupan na ovom uređaju. Unesite ga ručno.",
                "Number not automatically available on this device. Please enter manually.",
                "Номер недоступен автоматически. Введите вручную.",
                "Nummer auf diesem Gerät nicht automatisch verfügbar. Bitte manuell eingeben."
            )
            phoneNote.textSize = 12f
            phoneNote.setTextColor(Color.rgb(180, 150, 0))
            phoneNote.setPadding(0, 4, 0, 0)

            val phoneParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            phoneParams.setMargins(0, 0, 0, 4)
            phoneInput.layoutParams = phoneParams

            val noteParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            noteParams.setMargins(0, 0, 0, 24)
            phoneNote.layoutParams = noteParams

            container.addView(title)
            container.addView(subtitle)
            container.addView(techOkBox)
            container.addView(nameLabel)
            container.addView(nameInput)
            container.addView(phoneLabel)
            container.addView(phoneInput)
            container.addView(phoneNote)
        }

        // ─── ULOGA ───
        val roleLabel = TextView(this)
        roleLabel.text = t("Vasa uloga u potrazi:", "Your role in the mission:", "Ваша роль:", "Ihre Rolle:")
        roleLabel.textSize = 15f
        roleLabel.setTextColor(Color.WHITE)
        roleLabel.setPadding(0, 0, 0, 8)

        val roleSpinner = Spinner(this)
        val roles = arrayOf(
            t("🔵  Spasilac — aktivni ucesnik potrage", "🔵  Rescuer — active participant", "🔵  Спасатель — активный участник", "🔵  Retter — aktiver Teilnehmer"),
            t("👁️  Posmatrac — vidim mapu, ne ucestvujem", "👁️  Observer — map view only", "👁️  Наблюдатель — только карта", "👁️  Beobachter — nur Karte")
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, roles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        roleSpinner.adapter = adapter
        val roleParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        roleParams.setMargins(0, 0, 0, 16)
        roleSpinner.layoutParams = roleParams

        val colorInfo = TextView(this)
        colorInfo.text = t(
            "Spasilac: plava tacka na mapi.\nPosmatrac: nije vidljiv na mapi.\nKlik na plavu tacku — vidite ime i broj spasioca.",
            "Rescuer: blue marker on map.\nObserver: not visible on map.\nTap blue marker — see rescuer name and phone.",
            "Спасатель: синяя точка на карте.\nНаблюдатель: не виден на карте.\nНажмите синюю точку — имя и телефон спасателя.",
            "Retter: blauer Marker.\nBeobachter: nicht sichtbar.\nMarker antippen — Name und Telefon des Retters."
        )
        colorInfo.textSize = 13f
        colorInfo.setTextColor(Color.rgb(150, 150, 150))
        colorInfo.setPadding(0, 0, 0, 24)

        val status = TextView(this)
        status.text = ""
        status.textSize = 14f
        status.setTextColor(Color.RED)
        status.gravity = Gravity.CENTER

        val loadingBar = ProgressBar(this)
        loadingBar.visibility = android.view.View.GONE

        val btnContinue = Button(this)
        btnContinue.text = t("NASTAVI", "CONTINUE", "ПРОДОЛЖИТЬ", "WEITER")
        btnContinue.setTextColor(Color.WHITE)
        btnContinue.setBackgroundColor(Color.rgb(0, 100, 180))
        btnContinue.textSize = 18f

        btnContinue.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val phone = phoneInput.text.toString().trim()
            val isObserver = roleSpinner.selectedItemPosition == 1

            if (name.isEmpty()) {
                status.text = t("Molimo unesite vase ime.", "Please enter your name.", "Введите имя.", "Namen eingeben.")
                return@setOnClickListener
            }
            if (name.length < 3) {
                status.text = t("Ime mora imati najmanje 3 slova.", "Name must be at least 3 characters.", "Минимум 3 символа.", "Mindestens 3 Zeichen.")
                return@setOnClickListener
            }

            if (isObserver) {
                prefs.edit()
                    .putString("teamRescuerName", name)
                    .putString("teamRescuerPhone", phone)
                    .putBoolean("teamIsObserver", true)
                    .putInt("teamRescuerColor", Color.rgb(30, 120, 220))
                    .apply()
                startActivity(Intent(this, TeamActivity::class.java))
                finish()
                return@setOnClickListener
            }

            status.text = t("Provjera...", "Checking...", "Проверка...", "Wird geprüft...")
            status.setTextColor(Color.rgb(0, 150, 220))
            loadingBar.visibility = android.view.View.VISIBLE
            btnContinue.isEnabled = false

            checkNameAvailability(name) { available ->
                runOnUiThread {
                    loadingBar.visibility = android.view.View.GONE
                    btnContinue.isEnabled = true

                    if (available) {
                        prefs.edit()
                            .putString("teamRescuerName", name)
                            .putString("teamRescuerPhone", phone)
                            .putBoolean("teamIsObserver", false)
                            .putInt("teamRescuerColor", Color.rgb(30, 120, 220))
                            .apply()
                        startActivity(Intent(this, TeamActivity::class.java))
                        finish()
                    } else {
                        status.setTextColor(Color.RED)
                        status.text = t(
                            "Ime \"$name\" je vec zauzeto. Dodajte grad:\nnpr. \"$name Zajecar\"",
                            "Name \"$name\" is taken. Add city:\ne.g. \"$name Belgrade\"",
                            "Имя \"$name\" занято. Добавьте город.",
                            "Name \"$name\" vergeben. Stadt hinzufügen."
                        )
                    }
                }
            }
        }

        container.addView(roleLabel)
        container.addView(roleSpinner)
        container.addView(colorInfo)
        container.addView(loadingBar)
        container.addView(status)
        container.addView(btnContinue)
    }

    // ─────────────────────────────────────────────
    // ČITANJE BROJA SA SIM KARTICE
    // ─────────────────────────────────────────────

    private fun getSimPhoneNumber(): String {
        return try {
            // Provjeri dozvolu READ_PHONE_STATE
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
                return ""
            }

            val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val number = telephonyManager.line1Number ?: ""

            // Provjeri da li je broj validan
            if (number.isNotEmpty() && number != "null" && number.length >= 6) {
                // Normalizuj format — dodaj + ako nedostaje
                if (number.startsWith("0")) {
                    "+381${number.substring(1)}"
                } else {
                    number
                }
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    // ─────────────────────────────────────────────
    // POMOĆNE FUNKCIJE
    // ─────────────────────────────────────────────

    private fun isGpsEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun checkNameAvailability(name: String, callback: (Boolean) -> Unit) {
        FirebaseDatabase.getInstance().getReference("active_rescuers").get()
            .addOnSuccessListener { snapshot ->
                var nameTaken = false
                snapshot.children.forEach { actionSnapshot ->
                    actionSnapshot.children.forEach { rescuerSnapshot ->
                        val rescuerName = rescuerSnapshot.child("name").getValue(String::class.java)
                        if (rescuerName?.equals(name, ignoreCase = true) == true) nameTaken = true
                    }
                }
                callback(!nameTaken)
            }.addOnFailureListener { callback(true) }
    }

    private fun currentLanguage(): String {
        return getSharedPreferences("savio_prefs", MODE_PRIVATE).getString("language", "sr") ?: "sr"
    }

    private fun t(sr: String, en: String, ru: String, de: String): String {
        return when (currentLanguage()) { "en" -> en; "ru" -> ru; "de" -> de; else -> sr }
    }

    private fun applyWindowInsets() {
        val rootView = window.decorView.findViewById<android.view.View>(android.R.id.content)
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }
    }
}
