package com.example.savio4

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

class SettingsActivity : AppCompatActivity() {

    private var selectedLanguage = "sr"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("savio_prefs", MODE_PRIVATE)
        selectedLanguage = prefs.getString("language", "sr") ?: "sr"

        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(32, 50, 32, 32)
        container.setBackgroundColor(Color.rgb(10, 12, 16))

        val title = TextView(this)
        title.text = t("PODEŠAVANJA", "SETTINGS", "НАСТРОЙКИ", "EINSTELLUNGEN")
        title.textSize = 28f
        title.setTextColor(Color.WHITE)

        val btnReadiness = Button(this)
        btnReadiness.text = t(
            "✅ PROVERA SPREMNOSTI",
            "✅ READINESS CHECK",
            "✅ ПРОВЕРКА ГОТОВНОСТИ",
            "✅ BEREITSCHAFTSPRÜFUNG"
        )
        btnReadiness.textSize = 18f
        btnReadiness.setTextColor(Color.WHITE)
        btnReadiness.setBackgroundColor(Color.rgb(0, 120, 200))

        btnReadiness.setOnClickListener {
            startActivity(Intent(this, ReadinessActivity::class.java))
        }

        val languageLabel = TextView(this)
        languageLabel.text = t("Jezik aplikacije", "Application language", "Язык приложения", "Sprache der Anwendung")
        languageLabel.textSize = 18f
        languageLabel.setTextColor(Color.WHITE)
        languageLabel.setPadding(0, 24, 0, 8)

        val radioGroup = RadioGroup(this)

        val sr = RadioButton(this)
        sr.text = "Srpski"
        sr.setTextColor(Color.WHITE)

        val en = RadioButton(this)
        en.text = "English"
        en.setTextColor(Color.WHITE)

        val ru = RadioButton(this)
        ru.text = "Русский"
        ru.setTextColor(Color.WHITE)

        val de = RadioButton(this)
        de.text = "Deutsch"
        de.setTextColor(Color.WHITE)

        radioGroup.addView(sr)
        radioGroup.addView(en)
        radioGroup.addView(ru)
        radioGroup.addView(de)

        when (selectedLanguage) {
            "en" -> en.isChecked = true
            "ru" -> ru.isChecked = true
            "de" -> de.isChecked = true
            else -> sr.isChecked = true
        }

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            selectedLanguage = when (checkedId) {
                en.id -> "en"
                ru.id -> "ru"
                de.id -> "de"
                else -> "sr"
            }
        }

        val btnSaveLanguage = Button(this)
        btnSaveLanguage.text = t("SAČUVAJ JEZIK", "SAVE LANGUAGE", "СОХРАНИТЬ ЯЗЫК", "SPRACHE SPEICHERN")

        btnSaveLanguage.setOnClickListener {
            prefs.edit()
                .putString("language", selectedLanguage)
                .apply()

            applyAppLanguage(selectedLanguage)

            Toast.makeText(
                this,
                t("Jezik je sačuvan.", "Language saved.", "Язык сохранён.", "Sprache gespeichert."),
                Toast.LENGTH_SHORT
            ).show()

            recreate()
        }

        val btnEditProfile = Button(this)
        btnEditProfile.text = t("IZMENI PROFIL", "EDIT PROFILE", "РЕДАКТИРОВАТЬ ПРОФИЛЬ", "PROFIL BEARBEITEN")

        btnEditProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        val version = TextView(this)
        version.text = t(
            "Verzija aplikacije: SAVIO SOS v1.4 TEST",
            "Application version: SAVIO SOS v1.4 TEST",
            "Версия приложения: SAVIO SOS v1.4 TEST",
            "App-Version: SAVIO SOS v1.4 TEST"
        )
        version.textSize = 16f
        version.setTextColor(Color.LTGRAY)
        version.setPadding(0, 24, 0, 24)

        val btnReset = Button(this)
        btnReset.text = t("RESET APLIKACIJE", "RESET APPLICATION", "СБРОС ПРИЛОЖЕНИЯ", "APP ZURÜCKSETZEN")

        btnReset.setOnClickListener {
            prefs.edit().clear().apply()
            applyAppLanguage("sr")

            Toast.makeText(
                this,
                t(
                    "Aplikacija je resetovana.",
                    "Application has been reset.",
                    "Приложение сброшено.",
                    "App wurde zurückgesetzt."
                ),
                Toast.LENGTH_LONG
            ).show()

            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
        }

        val btnBack = Button(this)
        btnBack.text = t("NAZAD", "BACK", "НАЗАД", "ZURÜCK")

        btnBack.setOnClickListener {
            finish()
        }

        container.addView(title)
        container.addView(btnReadiness)
        container.addView(languageLabel)
        container.addView(radioGroup)
        container.addView(btnSaveLanguage)
        container.addView(btnEditProfile)
        container.addView(version)
        container.addView(btnReset)
        container.addView(btnBack)

        setContentView(container)
    }

    private fun applyAppLanguage(language: String) {
        val localeTag = when (language) {
            "en" -> "en"
            "ru" -> "ru"
            "de" -> "de"
            else -> ""
        }

        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(localeTag)
        )
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
