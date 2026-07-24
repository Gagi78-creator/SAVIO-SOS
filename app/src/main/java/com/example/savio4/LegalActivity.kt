package com.example.savio4

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class LegalActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ako je već prihvaćeno — preskoči
        val prefs = getSharedPreferences("savio_prefs", MODE_PRIVATE)
        if (prefs.getBoolean("legalAccepted", false)) {
            goNext()
            return
        }

        buildScreen()
    }

    private fun buildScreen() {
        val prefs = getSharedPreferences("savio_prefs", MODE_PRIVATE)
        val lang = prefs.getString("language", "sr") ?: "sr"

        val scroll = ScrollView(this)
        scroll.setBackgroundColor(Color.rgb(8, 10, 14))

        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(36, 60, 36, 36)

        // ─── LOGO / NAZIV ───────────────────────────────────────────────
        val appName = TextView(this)
        appName.text = "🆘 SAVIO"
        appName.textSize = 48f
        appName.setTextColor(Color.rgb(220, 40, 40))
        appName.typeface = Typeface.DEFAULT_BOLD
        appName.gravity = Gravity.CENTER
        appName.setPadding(0, 0, 0, 4)

        val appSub = TextView(this)
        appSub.text = when (lang) {
            "en" -> "Search and Rescue Coordination App"
            "ru" -> "Приложение координации поисково-спасательных операций"
            "de" -> "Such- und Rettungskoordinations-App"
            else -> "Aplikacija za koordinaciju pretrage i spasavanja"
        }
        appSub.textSize = 13f
        appSub.setTextColor(Color.rgb(150, 150, 160))
        appSub.gravity = Gravity.CENTER
        appSub.setPadding(0, 0, 0, 40)

        // ─── AUTOR ──────────────────────────────────────────────────────
        val authorBox = LinearLayout(this)
        authorBox.orientation = LinearLayout.VERTICAL
        authorBox.setPadding(24, 24, 24, 24)
        val authorBg = android.graphics.drawable.GradientDrawable()
        authorBg.setColor(Color.rgb(15, 20, 35))
        authorBg.cornerRadius = 16f
        authorBg.setStroke(2, Color.rgb(0, 80, 180))
        authorBox.background = authorBg

        val authorLabel = TextView(this)
        authorLabel.text = when (lang) {
            "en" -> "👤 AUTHOR & COPYRIGHT OWNER"
            "ru" -> "👤 АВТОР И ПРАВООБЛАДАТЕЛЬ"
            "de" -> "👤 AUTOR & URHEBERRECHTSINHABER"
            else -> "👤 AUTOR I VLASNIK AUTORSKIH PRAVA"
        }
        authorLabel.textSize = 11f
        authorLabel.setTextColor(Color.rgb(100, 140, 220))
        authorLabel.typeface = Typeface.DEFAULT_BOLD
        authorLabel.letterSpacing = 0.1f

        val authorName = TextView(this)
        authorName.text = "Dragan Živanović — Gagi"
        authorName.textSize = 22f
        authorName.setTextColor(Color.WHITE)
        authorName.typeface = Typeface.DEFAULT_BOLD
        authorName.setPadding(0, 8, 0, 4)

        val authorDetails = TextView(this)
        authorDetails.text = when (lang) {
            "en" -> "Firefighter-Rescuer & Drone Pilot — Serbia\n© 2024–2026 Dragan Živanović. All rights reserved."
            "ru" -> "Пожарный-спасатель и пилот дрона — Сербия\n© 2024–2026 Драган Живанович. Все права защищены."
            "de" -> "Feuerwehr-Retter & Drohnenpilot — Serbien\n© 2024–2026 Dragan Živanović. Alle Rechte vorbehalten."
            else -> "Vatrogasac-spasilac i pilot drona — Srbija\n© 2024–2026 Dragan Živanović. Sva prava zadržana."
        }
        authorDetails.textSize = 13f
        authorDetails.setTextColor(Color.rgb(180, 185, 200))
        authorDetails.setPadding(0, 4, 0, 0)

        authorBox.addView(authorLabel)
        authorBox.addView(authorName)
        authorBox.addView(authorDetails)

        // ─── PRAVNO OBAVEŠTENJE ─────────────────────────────────────────
        val legalBox = LinearLayout(this)
        legalBox.orientation = LinearLayout.VERTICAL
        legalBox.setPadding(24, 24, 24, 24)
        val legalBg = android.graphics.drawable.GradientDrawable()
        legalBg.setColor(Color.rgb(20, 12, 12))
        legalBg.cornerRadius = 16f
        legalBg.setStroke(2, Color.rgb(160, 40, 40))
        legalBox.background = legalBg

        val legalTitle = TextView(this)
        legalTitle.text = when (lang) {
            "en" -> "⚖️ LEGAL NOTICE — INTELLECTUAL PROPERTY"
            "ru" -> "⚖️ ПРАВОВОЕ УВЕДОМЛЕНИЕ — ИНТЕЛЛЕКТУАЛЬНАЯ СОБСТВЕННОСТЬ"
            "de" -> "⚖️ RECHTLICHER HINWEIS — GEISTIGES EIGENTUM"
            else -> "⚖️ PRAVNO OBAVEŠTENJE — INTELEKTUALNA SVOJINA"
        }
        legalTitle.textSize = 12f
        legalTitle.setTextColor(Color.rgb(220, 100, 100))
        legalTitle.typeface = Typeface.DEFAULT_BOLD
        legalTitle.letterSpacing = 0.05f
        legalTitle.setPadding(0, 0, 0, 12)

        val legalText = TextView(this)
        legalText.text = when (lang) {
            "en" -> """
SAVIO is an original software application conceived, designed and developed entirely by Dragan Živanović (Gagi), based on direct field experience in search and rescue operations in Serbia.

PROHIBITED WITHOUT WRITTEN CONSENT:
• Copying, reproducing or distributing the source code
• Presenting this application or its concept as your own work
• Modifying and republishing under a different name
• Commercial use without the author's permission

All development history is publicly documented on GitHub (github.com/Gagi78-creator/SAVIO-SOS) with timestamps proving authorship.

Any unauthorized use, copying, or misrepresentation of this application constitutes a violation of copyright law and may result in criminal and civil liability under Serbian law (Zakon o autorskom i srodnim pravima) and applicable international copyright conventions.

By pressing ACCEPT, you confirm that you have read and understood this notice.
            """.trimIndent()

            "ru" -> """
SAVIO — оригинальное программное приложение, задуманное, спроектированное и разработанное Драганом Живановичем (Гаги) на основе личного опыта поисково-спасательных операций в Сербии.

БЕЗ ПИСЬМЕННОГО СОГЛАСИЯ ЗАПРЕЩЕНО:
• Копирование, воспроизведение или распространение исходного кода
• Представление этого приложения или его концепции как своей работы
• Изменение и повторная публикация под другим именем
• Коммерческое использование без разрешения автора

Вся история разработки публично задокументирована на GitHub с временными метками, подтверждающими авторство.

Любое несанкционированное использование является нарушением авторского права и может повлечь уголовную и гражданскую ответственность.

Нажимая ПРИНИМАЮ, вы подтверждаете, что прочитали и поняли это уведомление.
            """.trimIndent()

            "de" -> """
SAVIO ist eine originelle Softwareanwendung, die vollständig von Dragan Živanović (Gagi) konzipiert, entworfen und entwickelt wurde, basierend auf direkter Felderfahrung bei Such- und Rettungsoperationen in Serbien.

OHNE SCHRIFTLICHE GENEHMIGUNG VERBOTEN:
• Kopieren, Reproduzieren oder Verbreiten des Quellcodes
• Darstellen dieser Anwendung oder ihres Konzepts als eigene Arbeit
• Ändern und erneutes Veröffentlichen unter einem anderen Namen
• Kommerzielle Nutzung ohne Genehmigung des Autors

Die gesamte Entwicklungsgeschichte ist öffentlich auf GitHub dokumentiert mit Zeitstempeln, die die Urheberschaft beweisen.

Jede unbefugte Nutzung stellt eine Verletzung des Urheberrechts dar und kann zu strafrechtlicher und zivilrechtlicher Haftung führen.

Mit dem Drücken von AKZEPTIEREN bestätigen Sie, dass Sie diesen Hinweis gelesen und verstanden haben.
            """.trimIndent()

            else -> """
SAVIO je originalna softverska aplikacija koju je u celosti osmislio, projektovao i razvio Dragan Živanović (Gagi), na osnovu ličnog iskustva u operacijama pretrage i spasavanja u Srbiji.

BEZ PISANOG PRISTANKA ZABRANJENO JE:
• Kopiranje, reprodukovanje ili distribucija izvornog koda
• Predstavljanje ove aplikacije ili njene koncepcije kao svog rada
• Menjanje i ponovno objavljivanje pod drugim imenom
• Komercijalno korišćenje bez dozvole autora

Celokupna istorija razvoja javno je dokumentovana na GitHub platformi (github.com/Gagi78-creator/SAVIO-SOS) sa vremenskim oznakama koje dokazuju autorstvo.

Svako neovlašćeno korišćenje, kopiranje ili prisvajanje ove aplikacije predstavlja povredu autorskog prava i može rezultirati krivičnom i građanskom odgovornošću u skladu sa Zakonom o autorskom i srodnim pravima Republike Srbije i međunarodnim konvencijama o zaštiti autorskih prava.

Pritiskom na PRIHVATAM potvrđujete da ste pročitali i razumeli ovo obaveštenje.
            """.trimIndent()
        }
        legalText.textSize = 13f
        legalText.setTextColor(Color.rgb(200, 200, 210))
        legalText.lineHeight = (legalText.lineHeight * 1.4f).toInt()

        legalBox.addView(legalTitle)
        legalBox.addView(legalText)

        // ─── GITHUB DOKAZ ───────────────────────────────────────────────
        val githubBox = LinearLayout(this)
        githubBox.orientation = LinearLayout.VERTICAL
        githubBox.setPadding(24, 20, 24, 20)
        val githubBg = android.graphics.drawable.GradientDrawable()
        githubBg.setColor(Color.rgb(12, 18, 12))
        githubBg.cornerRadius = 16f
        githubBg.setStroke(2, Color.rgb(40, 140, 40))
        githubBox.background = githubBg

        val githubText = TextView(this)
        githubText.text = when (lang) {
            "en" -> "📋 Public development history:\ngithub.com/Gagi78-creator/SAVIO-SOS\n\nAll commits are timestamped and publicly verifiable."
            "ru" -> "📋 Публичная история разработки:\ngithub.com/Gagi78-creator/SAVIO-SOS\n\nВсе коммиты имеют временные метки и публично верифицируемы."
            "de" -> "📋 Öffentliche Entwicklungsgeschichte:\ngithub.com/Gagi78-creator/SAVIO-SOS\n\nAlle Commits sind zeitgestempelt und öffentlich verifizierbar."
            else -> "📋 Javna istorija razvoja:\ngithub.com/Gagi78-creator/SAVIO-SOS\n\nSvi commit-ovi su vremenski označeni i javno proverljivi."
        }
        githubText.textSize = 13f
        githubText.setTextColor(Color.rgb(100, 200, 100))
        githubBox.addView(githubText)

        // ─── DUGME PRIHVATAM ────────────────────────────────────────────
        val acceptBtn = Button(this)
        acceptBtn.text = when (lang) {
            "en" -> "✅  I ACCEPT"
            "ru" -> "✅  ПРИНИМАЮ"
            "de" -> "✅  AKZEPTIEREN"
            else -> "✅  PRIHVATAM"
        }
        acceptBtn.textSize = 16f
        acceptBtn.setTextColor(Color.WHITE)
        acceptBtn.typeface = Typeface.DEFAULT_BOLD
        val btnBg = android.graphics.drawable.GradientDrawable()
        btnBg.setColor(Color.rgb(0, 130, 60))
        btnBg.cornerRadius = 16f
        acceptBtn.background = btnBg
        val btnParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        btnParams.topMargin = 32
        btnParams.bottomMargin = 16
        acceptBtn.layoutParams = btnParams
        acceptBtn.setPadding(0, 32, 0, 32)

        acceptBtn.setOnClickListener {
            getSharedPreferences("savio_prefs", MODE_PRIVATE)
                .edit()
                .putBoolean("legalAccepted", true)
                .apply()
            goNext()
        }

        // ─── SKLAPANJE ──────────────────────────────────────────────────
        val space1 = Space(this).also { it.layoutParams = LinearLayout.LayoutParams(0, 24) }
        val space2 = Space(this).also { it.layoutParams = LinearLayout.LayoutParams(0, 24) }

        container.addView(appName)
        container.addView(appSub)
        container.addView(authorBox)
        container.addView(space1)
        container.addView(legalBox)
        container.addView(space2)
        container.addView(githubBox)
        container.addView(acceptBtn)

        scroll.addView(container)
        setContentView(scroll)
    }

    private fun goNext() {
        val prefs = getSharedPreferences("savio_prefs", MODE_PRIVATE)
        val onboardingDone = prefs.getBoolean("onboardingDone", false)
        if (onboardingDone) {
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            startActivity(Intent(this, OnboardingActivity::class.java))
        }
        finish()
    }
}
