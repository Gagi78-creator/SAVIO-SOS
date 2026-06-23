package com.example.savio4

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class ProfileActivity : AppCompatActivity() {

    private lateinit var localizedContext: Context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("savio_prefs", MODE_PRIVATE)
        val language = prefs.getString("language", "sr") ?: "sr"
        localizedContext = getLocalizedContext(language)

        val scrollView = ScrollView(this)
        scrollView.setBackgroundColor(Color.rgb(17, 17, 17))

        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(32, 40, 32, 40)

        // ─────────────────────────────────────────────
        // NASLOV
        // ─────────────────────────────────────────────

        val title = TextView(this)
        title.text = localizedContext.getString(R.string.profile_title)
        title.textSize = 28f
        title.setTextColor(Color.WHITE)
        title.setPadding(0, 0, 0, 8)

        // ─────────────────────────────────────────────
        // UPOZORENJE — OBAVEZNA POLJA
        // ─────────────────────────────────────────────

        val warningBox = TextView(this)
        warningBox.text = t(
            "⚠️ Sva polja su OBAVEZNA.\n\nAko nemate poznate bolesti, unesite: Bez poznatih bolesti.\n\nUnošenje lažnih podataka koji mogu uticati na pružanje hitne medicinske pomoći podleže Krivičnom zakoniku Republike Srbije.",
            "⚠️ All fields are MANDATORY.\n\nIf you have no known illnesses, enter: No known illnesses.\n\nProviding false information that may affect emergency medical assistance is subject to criminal law.",
            "⚠️ Все поля ОБЯЗАТЕЛЬНЫ для заполнения.\n\nЕсли у вас нет известных заболеваний, введите: Нет известных заболеваний.\n\nПредоставление ложных данных, способных повлиять на оказание экстренной медицинской помощи, является уголовно наказуемым.",
            "⚠️ Alle Felder sind PFLICHTFELDER.\n\nWenn Sie keine bekannten Krankheiten haben, geben Sie ein: Keine bekannten Erkrankungen.\n\nDie Angabe falscher Daten, die die Notfallversorgung beeinflussen können, ist strafbar."
        )
        warningBox.textSize = 14f
        warningBox.setTextColor(Color.WHITE)
        warningBox.setPadding(24, 24, 24, 24)

        val warningBg = GradientDrawable()
        warningBg.setColor(Color.rgb(80, 20, 0))
        warningBg.cornerRadius = 16f
        warningBg.setStroke(2, Color.RED)
        warningBox.background = warningBg

        val warningParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        warningParams.setMargins(0, 16, 0, 24)
        warningBox.layoutParams = warningParams

        // ─────────────────────────────────────────────
        // POLJA
        // ─────────────────────────────────────────────

        val fullName = buildField(
            hint = t("Ime i prezime *", "Full name *", "Имя и фамилия *", "Vor- und Nachname *"),
            value = prefs.getString("fullName", "") ?: "",
            inputType = android.text.InputType.TYPE_CLASS_TEXT
        )

        val age = buildField(
            hint = t("Godine *", "Age *", "Возраст *", "Alter *"),
            value = prefs.getString("age", "") ?: "",
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        )

        val phone = buildField(
            hint = t("Broj telefona *", "Phone number *", "Номер телефона *", "Telefonnummer *"),
            value = prefs.getString("phone", "") ?: "",
            inputType = android.text.InputType.TYPE_CLASS_PHONE
        )

        val bloodType = buildField(
            hint = t("Krvna grupa *", "Blood type *", "Группа крови *", "Blutgruppe *"),
            value = prefs.getString("bloodType", "") ?: "",
            inputType = android.text.InputType.TYPE_CLASS_TEXT
        )

        val chronicDiseases = buildField(
            hint = t(
                "Hronične bolesti / terapija * (ako nema: Bez poznatih bolesti)",
                "Chronic illnesses / therapy * (if none: No known illnesses)",
                "Хронические болезни / терапия * (если нет: Нет известных заболеваний)",
                "Chronische Erkrankungen / Therapie * (wenn keine: Keine bekannten Erkrankungen)"
            ),
            value = prefs.getString("chronicDiseases", "") ?: "",
            inputType = android.text.InputType.TYPE_CLASS_TEXT,
            minLines = 3
        )

        // ─────────────────────────────────────────────
        // PORUKA O GREŠCI
        // ─────────────────────────────────────────────

        val errorText = TextView(this)
        errorText.text = ""
        errorText.textSize = 14f
        errorText.setTextColor(Color.RED)
        errorText.setPadding(0, 8, 0, 8)

        // ─────────────────────────────────────────────
        // DUGME SAČUVAJ
        // ─────────────────────────────────────────────

        val btnSave = Button(this)
        btnSave.text = localizedContext.getString(R.string.save_profile)

        btnSave.setOnClickListener {
            val name = fullName.text.toString().trim()
            val ageVal = age.text.toString().trim()
            val phoneVal = phone.text.toString().trim()
            val bloodVal = bloodType.text.toString().trim()
            val diseasesVal = chronicDiseases.text.toString().trim()

            // Validacija — sva polja moraju biti popunjena
            val emptyFields = mutableListOf<String>()

            if (name.isEmpty()) emptyFields.add(t("Ime i prezime", "Full name", "Имя и фамилия", "Vor- und Nachname"))
            if (ageVal.isEmpty()) emptyFields.add(t("Godine", "Age", "Возраст", "Alter"))
            if (phoneVal.isEmpty()) emptyFields.add(t("Broj telefona", "Phone number", "Номер телефона", "Telefonnummer"))
            if (bloodVal.isEmpty()) emptyFields.add(t("Krvna grupa", "Blood type", "Группа крови", "Blutgruppe"))
            if (diseasesVal.isEmpty()) emptyFields.add(t("Hronične bolesti / terapija", "Chronic illnesses / therapy", "Хронические болезни", "Chronische Erkrankungen"))

            if (emptyFields.isNotEmpty()) {
                errorText.text = t(
                    "⛔ Molimo popunite sva obavezna polja:\n\n• ${emptyFields.joinToString("\n• ")}",
                    "⛔ Please fill in all mandatory fields:\n\n• ${emptyFields.joinToString("\n• ")}",
                    "⛔ Пожалуйста, заполните все обязательные поля:\n\n• ${emptyFields.joinToString("\n• ")}",
                    "⛔ Bitte füllen Sie alle Pflichtfelder aus:\n\n• ${emptyFields.joinToString("\n• ")}"
                )
                return@setOnClickListener
            }

            // Sve OK — sačuvaj
            errorText.text = ""
            prefs.edit()
                .putString("fullName", name)
                .putString("age", ageVal)
                .putString("phone", phoneVal)
                .putString("bloodType", bloodVal)
                .putString("chronicDiseases", diseasesVal)
                .putBoolean("profileSaved", true)
                .apply()

            Toast.makeText(
                this,
                t("Profil je sačuvan.", "Profile saved.", "Профиль сохранён.", "Profil gespeichert."),
                Toast.LENGTH_SHORT
            ).show()

            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        // ─────────────────────────────────────────────
        // PRAVNO UPOZORENJE — DOLE
        // ─────────────────────────────────────────────

        val legalNote = TextView(this)
        legalNote.text = t(
            "Napomena: Podaci se čuvaju isključivo na Vašem uređaju i koriste se samo u slučaju aktivacije SOS signala.",
            "Note: Data is stored exclusively on your device and is used only in case of SOS signal activation.",
            "Примечание: Данные хранятся исключительно на вашем устройстве и используются только при активации сигнала SOS.",
            "Hinweis: Die Daten werden ausschließlich auf Ihrem Gerät gespeichert und nur bei SOS-Aktivierung verwendet."
        )
        legalNote.textSize = 12f
        legalNote.setTextColor(Color.rgb(140, 140, 140))
        legalNote.setPadding(0, 24, 0, 0)

        container.addView(title)
        container.addView(warningBox)
        container.addView(fullName)
        container.addView(age)
        container.addView(phone)
        container.addView(bloodType)
        container.addView(chronicDiseases)
        container.addView(errorText)
        container.addView(btnSave)
        container.addView(legalNote)

        scrollView.addView(container)
        setContentView(scrollView)
    }

    // ─────────────────────────────────────────────
    // POMOĆNA FUNKCIJA — IZGRADNJA POLJA
    // ─────────────────────────────────────────────

    private fun buildField(
        hint: String,
        value: String,
        inputType: Int,
        minLines: Int = 1
    ): EditText {
        val field = EditText(this)
        field.hint = hint
        field.setText(value)
        field.inputType = inputType
        field.minLines = minLines
        field.setTextColor(Color.WHITE)
        field.setHintTextColor(Color.rgb(140, 140, 140))

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, 0, 0, 16)
        field.layoutParams = params

        return field
    }

    // ─────────────────────────────────────────────
    // LOKALIZACIJA
    // ─────────────────────────────────────────────

    private fun getLocalizedContext(language: String): Context {
        val locale = Locale(language)
        Locale.setDefault(locale)

        val config = android.content.res.Configuration(resources.configuration)
        config.setLocale(locale)

        return createConfigurationContext(config)
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