package com.example.savio4

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class RescueLoginActivity : AppCompatActivity() {

    private val rescueUsername = "spasilac"
    private val rescuePassword = "savio112"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Provjeri da li su kredencijali već zapamćeni
        val prefs = getSharedPreferences("savio_prefs", MODE_PRIVATE)
        val savedUsername = prefs.getString("rescue_username", null)
        val savedPassword = prefs.getString("rescue_password", null)

        if (savedUsername == rescueUsername && savedPassword == rescuePassword) {
            // Direktno ulazi bez prikazivanja login ekrana
            startActivity(Intent(this, RescueActivity::class.java))
            finish()
            return
        }

        // Prikaži login ekran
        buildLoginScreen()
    }

    private fun buildLoginScreen() {
        val prefs = getSharedPreferences("savio_prefs", MODE_PRIVATE)

        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(32, 50, 32, 32)
        container.setBackgroundColor(Color.rgb(10, 12, 16))

        val title = TextView(this)
        title.text = t(
            "ZONA ZA SPASIOCE",
            "RESCUE ZONE",
            "ЗОНА СПАСАТЕЛЕЙ",
            "RETTUNGSZONE"
        )
        title.textSize = 28f
        title.setTextColor(Color.WHITE)

        val warning = TextView(this)
        warning.text = t(
            "SAMO ZA OVLAŠĆENA LICA\n\nOvaj deo aplikacije namenjen je spasiocima i osobama koje pružaju pomoć.",
            "AUTHORIZED PERSONNEL ONLY\n\nThis part of the application is intended for rescuers and people providing assistance.",
            "ТОЛЬКО ДЛЯ УПОЛНОМОЧЕННЫХ ЛИЦ\n\nЭта часть приложения предназначена для спасателей и лиц, оказывающих помощь.",
            "NUR FÜR BERECHTIGTE PERSONEN\n\nDieser Bereich der Anwendung ist für Rettungskräfte und helfende Personen vorgesehen."
        )
        warning.textSize = 16f
        warning.setTextColor(Color.LTGRAY)
        warning.setPadding(0, 18, 0, 18)

        val usernameInput = EditText(this)
        usernameInput.hint = t(
            "Korisničko ime",
            "Username",
            "Имя пользователя",
            "Benutzername"
        )
        usernameInput.setTextColor(Color.WHITE)
        usernameInput.setHintTextColor(Color.LTGRAY)

        val passwordInput = EditText(this)
        passwordInput.hint = t(
            "Lozinka",
            "Password",
            "Пароль",
            "Passwort"
        )
        passwordInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        passwordInput.setTextColor(Color.WHITE)
        passwordInput.setHintTextColor(Color.LTGRAY)

        // Checkbox "Zapamti me"
        val rememberMe = CheckBox(this)
        rememberMe.text = t(
            "Zapamti akreditaciju na ovom uređaju",
            "Remember credentials on this device",
            "Запомнить данные на этом устройстве",
            "Zugangsdaten auf diesem Gerät speichern"
        )
        rememberMe.setTextColor(Color.LTGRAY)
        rememberMe.setPadding(0, 12, 0, 12)

        val rememberNote = TextView(this)
        rememberNote.text = t(
            "⚠️ Ako se aplikacija resetuje ili ponovo instalira, akreditacija će biti izbrisana.",
            "⚠️ If the application is reset or reinstalled, credentials will be deleted.",
            "⚠️ При сбросе или переустановке приложения данные будут удалены.",
            "⚠️ Bei einem App-Reset oder einer Neuinstallation werden die Zugangsdaten gelöscht."
        )
        rememberNote.textSize = 12f
        rememberNote.setTextColor(Color.rgb(140, 140, 140))
        rememberNote.setPadding(0, 0, 0, 16)

        val btnLogin = Button(this)
        btnLogin.text = t(
            "ULAZ ZA SPASIOCE",
            "RESCUER LOGIN",
            "ВХОД ДЛЯ СПАСАТЕЛЕЙ",
            "RETTUNGSKRÄFTE LOGIN"
        )
        btnLogin.setTextColor(Color.WHITE)
        btnLogin.setBackgroundColor(Color.rgb(0, 130, 60))

        val btnBack = Button(this)
        btnBack.text = t(
            "NAZAD",
            "BACK",
            "НАЗАД",
            "ZURÜCK"
        )

        val status = TextView(this)
        status.text = ""
        status.textSize = 16f
        status.setTextColor(Color.RED)
        status.setPadding(0, 18, 0, 0)

        btnLogin.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (username == rescueUsername && password == rescuePassword) {
                // Sačuvaj kredencijale ako je checkbox označen
                if (rememberMe.isChecked) {
                    prefs.edit()
                        .putString("rescue_username", username)
                        .putString("rescue_password", password)
                        .apply()
                }

                startActivity(Intent(this, RescueActivity::class.java))
                finish()
            } else {
                status.text = t(
                    "⛔ Pogrešno korisničko ime ili lozinka.",
                    "⛔ Wrong username or password.",
                    "⛔ Неверное имя пользователя или пароль.",
                    "⛔ Falscher Benutzername oder falsches Passwort."
                )
            }
        }

        btnBack.setOnClickListener {
            finish()
        }

        container.addView(title)
        container.addView(warning)
        container.addView(usernameInput)
        container.addView(passwordInput)
        container.addView(rememberMe)
        container.addView(rememberNote)
        container.addView(btnLogin)
        container.addView(btnBack)
        container.addView(status)

        setContentView(container)
    }

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
}