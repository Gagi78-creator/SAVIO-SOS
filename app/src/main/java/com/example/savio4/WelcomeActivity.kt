package com.example.savio4

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

class WelcomeActivity : AppCompatActivity() {

    private var selectedLanguage = "sr"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("savio_prefs", MODE_PRIVATE)
        val savedLanguage = prefs.getString("language", "sr") ?: "sr"
        selectedLanguage = savedLanguage

        applyAppLanguage(savedLanguage)

        val legalAccepted = prefs.getBoolean("legalAccepted", false)
        val profileSaved = prefs.getBoolean("profileSaved", false)

        if (legalAccepted && profileSaved) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        if (legalAccepted && !profileSaved) {
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
            return
        }

        val scrollView = ScrollView(this)
        scrollView.setBackgroundColor(Color.rgb(17, 17, 17))

        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(32, 40, 32, 40)

        val title = TextView(this)
        title.text = "SAVIO SOS v1.4"
        title.textSize = 28f
        title.setTextColor(Color.WHITE)
        title.gravity = android.view.Gravity.CENTER

        val languageTitle = TextView(this)
        languageTitle.textSize = 18f
        languageTitle.setTextColor(Color.WHITE)

        val radioGroup = RadioGroup(this)

        val sr = RadioButton(this)
        sr.id = View.generateViewId()
        sr.text = "Srpski"
        sr.setTextColor(Color.WHITE)

        val en = RadioButton(this)
        en.id = View.generateViewId()
        en.text = "English"
        en.setTextColor(Color.WHITE)

        val ru = RadioButton(this)
        ru.id = View.generateViewId()
        ru.text = "Русский"
        ru.setTextColor(Color.WHITE)

        val de = RadioButton(this)
        de.id = View.generateViewId()
        de.text = "Deutsch"
        de.setTextColor(Color.WHITE)

        radioGroup.addView(sr)
        radioGroup.addView(en)
        radioGroup.addView(ru)
        radioGroup.addView(de)

        val warning = TextView(this)
        warning.textSize = 16f
        warning.setTextColor(Color.WHITE)
        warning.setPadding(0, 24, 0, 24)

        val checkBox = CheckBox(this)
        checkBox.setTextColor(Color.WHITE)

        val btnContinue = Button(this)
        btnContinue.isEnabled = false

        // iOS napomena — prikazuje se na odabranom jeziku
        val iosNote = TextView(this)
        iosNote.setPadding(24, 16, 24, 16)
        iosNote.textSize = 14f
        iosNote.setTextColor(Color.WHITE)
        val iosBg = android.graphics.drawable.GradientDrawable()
        iosBg.setColor(Color.rgb(50, 35, 0))
        iosBg.cornerRadius = 16f
        iosBg.setStroke(2, Color.rgb(255, 165, 0))
        iosNote.background = iosBg
        val iosParams = android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        )
        iosParams.setMargins(0, 0, 0, 16)
        iosNote.layoutParams = iosParams

        fun updateLanguageTexts(language: String) {
            selectedLanguage = language

            when (language) {
                "en" -> {
                    languageTitle.text = "\nChoose language"

                    warning.text = """
                        LEGAL WARNING

                        Activating SOS mode inside this application starts emergency notification of predefined contacts and sends your geographic coordinates.

                        Intentional false or unjustified activation of SOS mode may cause unnecessary engagement of police, firefighters, rescue services or other emergency resources and may lead to legal responsibility under applicable law.

                        The user confirms that SOS mode will be used only in real danger or when there is a justified need for urgent notification.

                        ⚠️ IMPORTANT LIMITATION
                        SAVIO SOS works only if the phone has a GSM signal. In areas without GSM coverage, the application CANNOT guarantee delivery of the SOS signal. For use in completely isolated areas, we recommend a satellite device (Garmin inReach, SPOT X).
                    """.trimIndent()

                    checkBox.text = "I have read and accept the warning"
                    btnContinue.text = "CONTINUE"
                    iosNote.text = "📱 AVAILABILITY\nSAVIO SOS is currently available for Android devices only. An iOS version is not available."
                }

                "ru" -> {
                    languageTitle.text = "\nВыберите язык"

                    warning.text = """
                        ПРАВОВОЕ ПРЕДУПРЕЖДЕНИЕ

                        Активация режима SOS в приложении запускает экстренное уведомление заранее заданных контактов и отправку ваших географических координат.

                        Умышленная ложная или необоснованная активация режима SOS может привести к ненужному привлечению полиции, пожарных, спасательных служб и других экстренных ресурсов, а также к юридической ответственности в соответствии с действующим законодательством.

                        Пользователь подтверждает, что режим SOS будет использоваться только при реальной опасности или при обоснованной необходимости срочного уведомления.

                        ⚠️ ВАЖНОЕ ОГРАНИЧЕНИЕ
                        SAVIO SOS работает только при наличии GSM-сигнала. В районах без GSM-покрытия приложение НЕ МОЖЕТ гарантировать доставку SOS-сигнала. Для работы в полностью изолированных районах рекомендуем спутниковые устройства (Garmin inReach, SPOT X).
                    """.trimIndent()

                    checkBox.text = "Я прочитал(а) и принимаю предупреждение"
                    btnContinue.text = "ПРОДОЛЖИТЬ"
                    iosNote.text = "📱 ДОСТУПНОСТЬ\nSAVIO SOS в настоящее время доступен только для устройств Android. Версия для iOS недоступна."
                }

                "de" -> {
                    languageTitle.text = "\nSprache auswählen"

                    warning.text = """
                        RECHTLICHER HINWEIS

                        Die Aktivierung des SOS-Modus in dieser Anwendung startet eine Notfallbenachrichtigung der vordefinierten Kontakte und übermittelt Ihre geografischen Koordinaten.

                        Eine vorsätzliche falsche oder unbegründete Aktivierung des SOS-Modus kann zu einem unnötigen Einsatz von Polizei, Feuerwehr, Rettungsdiensten oder anderen Notfallressourcen führen und rechtliche Folgen nach geltendem Recht haben.

                        Der Benutzer bestätigt, dass der SOS-Modus nur bei tatsächlicher Gefahr oder bei einem berechtigten Bedarf an einer dringenden Benachrichtigung verwendet wird.

                        ⚠️ WICHTIGE EINSCHRÄNKUNG
                        SAVIO SOS funktioniert nur, wenn das Telefon ein GSM-Signal hat. In Gebieten ohne GSM-Abdeckung KANN die Anwendung die Zustellung des SOS-Signals NICHT garantieren. Für den Einsatz in völlig abgelegenen Gebieten empfehlen wir ein Satellitengerät (Garmin inReach, SPOT X).
                    """.trimIndent()

                    checkBox.text = "Ich habe den Hinweis gelesen und akzeptiere ihn"
                    btnContinue.text = "FORTFAHREN"
                    iosNote.text = "📱 VERFÜGBARKEIT\nSAVIO SOS ist derzeit nur für Android-Geräte verfügbar. Eine iOS-Version ist nicht verfügbar."
                }

                else -> {
                    languageTitle.text = "\nIzaberite jezik"

                    warning.text = """
                        PRAVNO UPOZORENJE

                        Aktivacija SOS režima unutar aplikacije pokreće direktno uzbunjivanje unapred definisanih kontakata i hitan prenos vaših geografskih koordinata.

                        Namerno lažno ili neosnovano aktiviranje SOS režima sa namerom izazivanja panike ili bespotrebnog angažovanja resursa policije, vatrogasaca, spasilačkih službi i drugih hitnih službi može podlegati krivičnoj odgovornosti u skladu sa zakonima Republike Srbije.

                        Korisnik potvrđuje da SOS režim koristi isključivo u stvarnoj opasnosti ili kada postoji opravdan razlog za hitno obaveštavanje.

                        ⚠️ VAŽNO OGRANIČENJE
                        SAVIO SOS funkcioniše samo ako telefon ima GSM signal. Na terenima bez GSM pokrivenosti aplikacija NE MOŽE garantovati isporuku SOS signala. Za rad na potpuno izolovanim terenima preporučujemo satelitski uređaj (Garmin inReach, SPOT X).
                    """.trimIndent()

                    checkBox.text = "Pročitao/la sam i prihvatam upozorenje"
                    btnContinue.text = "NASTAVI"
                    iosNote.text = "📱 DOSTUPNOST APLIKACIJE\nSAVIO SOS je trenutno dostupan isključivo za Android uređaje. iOS verzija nije dostupna."
                }
            }
        }

        checkBox.setOnCheckedChangeListener { _, isChecked ->
            btnContinue.isEnabled = isChecked
        }

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val language = when (checkedId) {
                en.id -> "en"
                ru.id -> "ru"
                de.id -> "de"
                sr.id -> "sr"
                else -> "sr"
            }

            checkBox.isChecked = false
            btnContinue.isEnabled = false
            updateLanguageTexts(language)
        }

        btnContinue.setOnClickListener {
            prefs.edit()
                .putBoolean("legalAccepted", true)
                .putString("language", selectedLanguage)
                .apply()

            applyAppLanguage(selectedLanguage)

            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }

        updateLanguageTexts(selectedLanguage)

        when (selectedLanguage) {
            "en" -> radioGroup.check(en.id)
            "ru" -> radioGroup.check(ru.id)
            "de" -> radioGroup.check(de.id)
            else -> radioGroup.check(sr.id)
        }

        container.addView(title)
        container.addView(languageTitle)
        container.addView(radioGroup)
        container.addView(warning)
        container.addView(iosNote)
        container.addView(checkBox)
        container.addView(btnContinue)

        scrollView.addView(container)
        setContentView(scrollView)
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
}
