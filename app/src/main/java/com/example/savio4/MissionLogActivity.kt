package com.example.savio4

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MissionLogActivity : AppCompatActivity() {

    private val adminName = "gagi"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("savio_prefs", MODE_PRIVATE)
        val rescuerName = prefs.getString("teamRescuerName", "") ?: ""
        val isAdmin = rescuerName.trim().lowercase() == adminName

        val scrollView = ScrollView(this)
        scrollView.setBackgroundColor(Color.rgb(10, 12, 16))

        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(32, 50, 32, 32)

        val title = TextView(this)
        title.text = "📋 " + t("IZVEŠTAJI AKCIJA", "MISSION REPORTS", "ОТЧЁТЫ ОБ ОПЕРАЦИЯХ", "EINSATZBERICHTE")
        title.textSize = 26f
        title.setTextColor(Color.WHITE)
        title.typeface = android.graphics.Typeface.DEFAULT_BOLD
        title.setPadding(0, 0, 0, 8)

        val subtitle = TextView(this)
        subtitle.text = t(
            "Pregled intervencija i upravljanje potragama",
            "View interventions and manage missions",
            "Просмотр операций и управление",
            "Einsätze anzeigen und verwalten"
        )
        subtitle.textSize = 14f
        subtitle.setTextColor(Color.rgb(120, 120, 120))
        subtitle.setPadding(0, 0, 0, 24)

        // ─── SEKCIJA AKTIVNIH POTRAGA (samo admin) ───
        val activeSectionTitle = TextView(this)
        activeSectionTitle.text = "🔴 " + t(
            "AKTIVNE POTRAGE",
            "ACTIVE MISSIONS",
            "АКТИВНЫЕ ОПЕРАЦИИ",
            "AKTIVE EINSÄTZE"
        )
        activeSectionTitle.textSize = 18f
        activeSectionTitle.setTextColor(Color.rgb(255, 80, 80))
        activeSectionTitle.typeface = android.graphics.Typeface.DEFAULT_BOLD
        activeSectionTitle.setPadding(0, 0, 0, 12)

        val activeLoadingBar = ProgressBar(this)
        val activeLoadingParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        activeLoadingParams.gravity = Gravity.CENTER_HORIZONTAL
        activeLoadingBar.layoutParams = activeLoadingParams

        val activeContainer = LinearLayout(this)
        activeContainer.orientation = LinearLayout.VERTICAL

        val activeStatusText = TextView(this)
        activeStatusText.text = t("Ucitavam...", "Loading...", "Загрузка...", "Wird geladen...")
        activeStatusText.textSize = 13f
        activeStatusText.setTextColor(Color.rgb(150, 150, 150))
        activeStatusText.gravity = Gravity.CENTER

        // Separator
        val separator = TextView(this)
        separator.text = "─────────────────────────────"
        separator.textSize = 12f
        separator.setTextColor(Color.rgb(50, 60, 80))
        separator.gravity = Gravity.CENTER
        val sepParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        sepParams.setMargins(0, 24, 0, 24)
        separator.layoutParams = sepParams

        // ─── SEKCIJA ZAVRŠENIH POTRAGA ───
        val completedSectionTitle = TextView(this)
        completedSectionTitle.text = "✅ " + t(
            "ZAVRŠENE INTERVENCIJE",
            "COMPLETED INTERVENTIONS",
            "ЗАВЕРШЁННЫЕ ОПЕРАЦИИ",
            "ABGESCHLOSSENE EINSÄTZE"
        )
        completedSectionTitle.textSize = 18f
        completedSectionTitle.setTextColor(Color.rgb(0, 200, 100))
        completedSectionTitle.typeface = android.graphics.Typeface.DEFAULT_BOLD
        completedSectionTitle.setPadding(0, 0, 0, 12)

        val completedLoadingBar = ProgressBar(this)
        val completedLoadingParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        completedLoadingParams.gravity = Gravity.CENTER_HORIZONTAL
        completedLoadingBar.layoutParams = completedLoadingParams

        val completedContainer = LinearLayout(this)
        completedContainer.orientation = LinearLayout.VERTICAL

        val completedStatusText = TextView(this)
        completedStatusText.text = t("Ucitavam...", "Loading...", "Загрузка...", "Wird geladen...")
        completedStatusText.textSize = 13f
        completedStatusText.setTextColor(Color.rgb(150, 150, 150))
        completedStatusText.gravity = Gravity.CENTER

        val btnBack = Button(this)
        btnBack.text = t("← NAZAD", "← BACK", "← НАЗАД", "← ZURÜCK")
        btnBack.setTextColor(Color.WHITE)
        btnBack.setBackgroundColor(Color.rgb(50, 50, 70))
        val backParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        backParams.setMargins(0, 24, 0, 0)
        btnBack.layoutParams = backParams
        btnBack.setOnClickListener { finish() }

        container.addView(title)
        container.addView(subtitle)
        container.addView(activeSectionTitle)
        container.addView(activeLoadingBar)
        container.addView(activeStatusText)
        container.addView(activeContainer)
        container.addView(separator)
        container.addView(completedSectionTitle)
        container.addView(completedLoadingBar)
        container.addView(completedStatusText)
        container.addView(completedContainer)
        container.addView(btnBack)

        scrollView.addView(container)
        setContentView(scrollView)
        applyWindowInsets()

        loadActiveMissions(activeContainer, activeStatusText, activeLoadingBar, isAdmin)
        loadCompletedMissions(completedContainer, completedStatusText, completedLoadingBar, isAdmin)
    }

    // ─────────────────────────────────────────────
    // AKTIVNE POTRAGE
    // ─────────────────────────────────────────────

    private fun loadActiveMissions(
        container: LinearLayout,
        statusText: TextView,
        loadingBar: ProgressBar,
        isAdmin: Boolean
    ) {
        val db = FirebaseDatabase.getInstance()
        db.getReference("missions").get().addOnSuccessListener { snapshot ->
            loadingBar.visibility = android.view.View.GONE
            container.removeAllViews()

            val active = mutableListOf<DataSnapshot>()
            snapshot.children.forEach { missionSnapshot ->
                val isActive = missionSnapshot.child("active").getValue(Boolean::class.java) ?: false
                if (isActive) active.add(missionSnapshot)
            }

            if (active.isEmpty()) {
                statusText.text = t(
                    "Nema aktivnih potraga.",
                    "No active missions.",
                    "Нет активных операций.",
                    "Keine aktiven Einsätze."
                )
                statusText.setTextColor(Color.rgb(150, 150, 150))
                return@addOnSuccessListener
            }

            statusText.text = t(
                "Aktivnih potraga: ${active.size}",
                "Active missions: ${active.size}",
                "Активных операций: ${active.size}",
                "Aktive Einsätze: ${active.size}"
            )
            statusText.setTextColor(Color.rgb(255, 80, 80))

            active.forEach { missionSnapshot ->
                val code = missionSnapshot.child("code").getValue(String::class.java) ?: return@forEach
                val name = missionSnapshot.child("name").getValue(String::class.java) ?: code
                val coordinator = missionSnapshot.child("coordinator").getValue(String::class.java) ?: ""
                val createdAt = missionSnapshot.child("createdAt").getValue(Long::class.java) ?: 0L
                addActiveMissionCard(container, code, name, coordinator, createdAt, isAdmin, db)
            }
        }.addOnFailureListener {
            loadingBar.visibility = android.view.View.GONE
            statusText.text = t("Greška pri učitavanju.", "Error loading.", "Ошибка.", "Fehler.")
            statusText.setTextColor(Color.RED)
        }
    }

    private fun addActiveMissionCard(
        container: LinearLayout,
        code: String,
        name: String,
        coordinator: String,
        createdAt: Long,
        isAdmin: Boolean,
        db: FirebaseDatabase
    ) {
        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.setPadding(24, 20, 24, 20)

        val cardBg = GradientDrawable()
        cardBg.setColor(Color.rgb(40, 10, 10))
        cardBg.cornerRadius = 16f
        cardBg.setStroke(2, Color.rgb(200, 50, 50))
        card.background = cardBg

        val cardParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        cardParams.setMargins(0, 0, 0, 16)
        card.layoutParams = cardParams

        val nameText = TextView(this)
        nameText.text = "🔴  $name"
        nameText.textSize = 17f
        nameText.setTextColor(Color.WHITE)
        nameText.typeface = android.graphics.Typeface.DEFAULT_BOLD
        nameText.setPadding(0, 0, 0, 6)

        val codeText = TextView(this)
        codeText.text = t("Kod: $code", "Code: $code", "Код: $code", "Code: $code")
        codeText.textSize = 12f
        codeText.setTextColor(Color.rgb(200, 100, 100))
        codeText.setPadding(0, 0, 0, 4)

        val coordText = TextView(this)
        coordText.text = t("Koordinator: $coordinator", "Coordinator: $coordinator", "Координатор: $coordinator", "Koordinator: $coordinator")
        coordText.textSize = 13f
        coordText.setTextColor(Color.rgb(200, 180, 180))
        coordText.setPadding(0, 0, 0, 4)

        val startTime = if (createdAt > 0)
            SimpleDateFormat("dd.MM.yyyy. HH:mm", Locale.getDefault()).format(Date(createdAt))
        else "--"

        val timeText = TextView(this)
        timeText.text = t("Pokrenuto: $startTime", "Started: $startTime", "Начато: $startTime", "Gestartet: $startTime")
        timeText.textSize = 12f
        timeText.setTextColor(Color.rgb(150, 120, 120))
        timeText.setPadding(0, 0, 0, 16)

        card.addView(nameText)
        card.addView(codeText)
        card.addView(coordText)
        card.addView(timeText)

        // Samo admin može prisilno zatvoriti
        if (isAdmin) {
            val btnForceClose = Button(this)
            btnForceClose.text = "⛔  " + t(
                "PRISILNO ZATVORI POTRAGU",
                "FORCE CLOSE MISSION",
                "ПРИНУДИТЕЛЬНО ЗАВЕРШИТЬ",
                "EINSATZ ERZWINGEN SCHLIESSEN"
            )
            btnForceClose.setTextColor(Color.WHITE)
            btnForceClose.setBackgroundColor(Color.rgb(150, 0, 0))
            btnForceClose.textSize = 14f

            val closeParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            closeParams.setMargins(0, 0, 0, 8)
            btnForceClose.layoutParams = closeParams

            btnForceClose.setOnClickListener {
                showForceCloseConfirmation(code, name, card, container, db)
            }

            val btnDelete = Button(this)
            btnDelete.text = "🗑️  " + t("OBRIŠI POTPUNO", "DELETE COMPLETELY", "УДАЛИТЬ ПОЛНОСТЬЮ", "VOLLSTÄNDIG LÖSCHEN")
            btnDelete.setTextColor(Color.WHITE)
            btnDelete.setBackgroundColor(Color.rgb(80, 0, 0))
            btnDelete.textSize = 14f

            btnDelete.setOnClickListener {
                showDeleteConfirmation(code, name, card, container, db)
            }

            card.addView(btnForceClose)
            card.addView(btnDelete)
        }

        container.addView(card)
    }

    private fun showForceCloseConfirmation(code: String, name: String, card: LinearLayout, container: LinearLayout, db: FirebaseDatabase) {
        AlertDialog.Builder(this)
            .setTitle("⛔ " + t("PRISILNO ZATVARANJE", "FORCE CLOSE", "ПРИНУДИТЕЛЬНОЕ ЗАКРЫТИЕ", "ERZWUNGENES SCHLIESSEN"))
            .setMessage(t(
                "Potraga \"$name\" će biti zatvorena.\n\nLog intervencije će biti sačuvan.\nSvi spasioci će biti uklonjeni sa mape.\n\nOva akcija se ne može poništiti.",
                "Mission \"$name\" will be closed.\n\nIntervention log will be saved.\nAll rescuers will be removed from map.\n\nThis action cannot be undone.",
                "Операция \"$name\" будет закрыта.\n\nЖурнал будет сохранён.\n\nЭто действие нельзя отменить.",
                "Einsatz \"$name\" wird geschlossen.\n\nProtokoll wird gespeichert.\n\nDiese Aktion kann nicht rückgängig gemacht werden."
            ))
            .setPositiveButton(t("ZATVORI POTRAGU", "CLOSE MISSION", "ЗАКРЫТЬ ОПЕРАЦИЮ", "EINSATZ SCHLIESSEN")) { _, _ ->
                forceCloseMission(code, name, card, container, db)
            }
            .setNegativeButton(t("ODUSTANI", "CANCEL", "ОТМЕНА", "ABBRECHEN"), null)
            .show()
    }

    private fun forceCloseMission(code: String, name: String, card: LinearLayout, container: LinearLayout, db: FirebaseDatabase) {
        val endTime = System.currentTimeMillis()

        // Sačuvaj log pre zatvaranja
        db.getReference("active_rescuers").child(code).get().addOnSuccessListener { rescuersSnapshot ->
            val rescuersList = mutableListOf<Map<String, String>>()
            rescuersSnapshot.children.forEach { r ->
                val rName = r.child("name").getValue(String::class.java) ?: ""
                val rPhone = r.child("phone").getValue(String::class.java) ?: ""
                if (rName.isNotEmpty()) rescuersList.add(mapOf("name" to rName, "phone" to rPhone))
            }

            val logData = mapOf(
                "missionCode" to code,
                "missionName" to name,
                "endedAt" to endTime,
                "result" to t(
                    "POTRAGA PRISILNO ZATVORENA OD STRANE ADMINISTRATORA",
                    "MISSION FORCE CLOSED BY ADMINISTRATOR",
                    "ОПЕРАЦИЯ ПРИНУДИТЕЛЬНО ЗАКРЫТА АДМИНИСТРАТОРОМ",
                    "EINSATZ VOM ADMINISTRATOR ZWANGSWEISE GESCHLOSSEN"
                ),
                "rescuers" to rescuersList,
                "finds" to emptyList<Any>()
            )

            db.getReference("mission_logs").child(code).setValue(logData)
            db.getReference("missions").child(code).child("active").setValue(false)
            db.getReference("missions").child(code).child("endedAt").setValue(endTime)
            db.getReference("active_rescuers").child(code).removeValue()

            container.removeView(card)

            Toast.makeText(this, t(
                "Potraga \"$name\" je zatvorena.",
                "Mission \"$name\" has been closed.",
                "Операция \"$name\" закрыта.",
                "Einsatz \"$name\" geschlossen."
            ), Toast.LENGTH_LONG).show()

            // Osvježi listu završenih
            recreate()
        }
    }

    // ─────────────────────────────────────────────
    // ZAVRŠENE POTRAGE
    // ─────────────────────────────────────────────

    private fun loadCompletedMissions(
        container: LinearLayout,
        statusText: TextView,
        loadingBar: ProgressBar,
        isAdmin: Boolean
    ) {
        val db = FirebaseDatabase.getInstance()
        db.getReference("missions").get().addOnSuccessListener { snapshot ->
            loadingBar.visibility = android.view.View.GONE
            container.removeAllViews()

            val completed = mutableListOf<DataSnapshot>()
            snapshot.children.forEach { missionSnapshot ->
                val isActive = missionSnapshot.child("active").getValue(Boolean::class.java) ?: true
                if (!isActive) completed.add(missionSnapshot)
            }

            if (completed.isEmpty()) {
                statusText.text = t(
                    "Nema završenih intervencija.",
                    "No completed interventions.",
                    "Нет завершённых операций.",
                    "Keine abgeschlossenen Einsätze."
                )
                statusText.setTextColor(Color.rgb(150, 150, 150))
                return@addOnSuccessListener
            }

            statusText.text = t(
                "Završenih intervencija: ${completed.size}",
                "Completed: ${completed.size}",
                "Завершённых: ${completed.size}",
                "Abgeschlossen: ${completed.size}"
            )
            statusText.setTextColor(Color.rgb(0, 200, 100))

            completed.sortedByDescending {
                it.child("createdAt").getValue(Long::class.java) ?: 0L
            }.forEach { missionSnapshot ->
                val code = missionSnapshot.child("code").getValue(String::class.java) ?: return@forEach
                val name = missionSnapshot.child("name").getValue(String::class.java) ?: code
                val coordinator = missionSnapshot.child("coordinator").getValue(String::class.java) ?: ""
                val createdAt = missionSnapshot.child("createdAt").getValue(Long::class.java) ?: 0L
                val endedAt = missionSnapshot.child("endedAt").getValue(Long::class.java) ?: 0L
                addCompletedMissionCard(container, code, name, coordinator, createdAt, endedAt, isAdmin, db)
            }
        }.addOnFailureListener {
            loadingBar.visibility = android.view.View.GONE
            statusText.text = t("Greška.", "Error.", "Ошибка.", "Fehler.")
            statusText.setTextColor(Color.RED)
        }
    }

    private fun addCompletedMissionCard(
        container: LinearLayout,
        code: String,
        name: String,
        coordinator: String,
        createdAt: Long,
        endedAt: Long,
        isAdmin: Boolean,
        db: FirebaseDatabase
    ) {
        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.setPadding(24, 20, 24, 20)

        val cardBg = GradientDrawable()
        cardBg.setColor(Color.rgb(15, 20, 30))
        cardBg.cornerRadius = 16f
        cardBg.setStroke(2, Color.rgb(50, 60, 80))
        card.background = cardBg

        val cardParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        cardParams.setMargins(0, 0, 0, 16)
        card.layoutParams = cardParams

        val nameText = TextView(this)
        nameText.text = "📋  $name"
        nameText.textSize = 17f
        nameText.setTextColor(Color.WHITE)
        nameText.typeface = android.graphics.Typeface.DEFAULT_BOLD
        nameText.setPadding(0, 0, 0, 6)

        val codeText = TextView(this)
        codeText.text = t("Kod: $code", "Code: $code", "Код: $code", "Code: $code")
        codeText.textSize = 12f
        codeText.setTextColor(Color.rgb(100, 150, 200))
        codeText.setPadding(0, 0, 0, 4)

        val coordText = TextView(this)
        coordText.text = t("Koordinator: $coordinator", "Coordinator: $coordinator", "Координатор: $coordinator", "Koordinator: $coordinator")
        coordText.textSize = 13f
        coordText.setTextColor(Color.rgb(160, 160, 160))
        coordText.setPadding(0, 0, 0, 4)

        val startTime = if (createdAt > 0) SimpleDateFormat("dd.MM.yyyy. HH:mm", Locale.getDefault()).format(Date(createdAt)) else "--"
        val endTime = if (endedAt > 0) SimpleDateFormat("dd.MM.yyyy. HH:mm", Locale.getDefault()).format(Date(endedAt)) else "--"

        val timeText = TextView(this)
        timeText.text = t("Pokrenuto: $startTime\nZavršeno: $endTime", "Started: $startTime\nEnded: $endTime", "Начато: $startTime\nЗавершено: $endTime", "Gestartet: $startTime\nBeendet: $endTime")
        timeText.textSize = 12f
        timeText.setTextColor(Color.rgb(120, 120, 120))
        timeText.setPadding(0, 0, 0, 16)

        val btnView = Button(this)
        btnView.text = t("📄 PREGLEDAJ IZVEŠTAJ", "📄 VIEW REPORT", "📄 ПРОСМОТРЕТЬ ОТЧЁТ", "📄 BERICHT ANZEIGEN")
        btnView.setTextColor(Color.WHITE)
        btnView.setBackgroundColor(Color.rgb(60, 80, 130))
        btnView.textSize = 14f
        val viewParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        viewParams.setMargins(0, 0, 0, if (isAdmin) 8 else 0)
        btnView.layoutParams = viewParams
        btnView.setOnClickListener { showMissionReport(code, name, coordinator, startTime, endTime, db) }

        card.addView(nameText)
        card.addView(codeText)
        card.addView(coordText)
        card.addView(timeText)
        card.addView(btnView)

        if (isAdmin) {
            val btnDelete = Button(this)
            btnDelete.text = "🗑️  " + t("OBRIŠI", "DELETE", "УДАЛИТЬ", "LÖSCHEN")
            btnDelete.setTextColor(Color.WHITE)
            btnDelete.setBackgroundColor(Color.rgb(100, 0, 0))
            btnDelete.textSize = 14f
            btnDelete.setOnClickListener { showDeleteConfirmation(code, name, card, container, db) }
            card.addView(btnDelete)
        }

        container.addView(card)
    }

    // ─────────────────────────────────────────────
    // PREGLED IZVEŠTAJA
    // ─────────────────────────────────────────────

    private fun showMissionReport(code: String, name: String, coordinator: String, startTime: String, endTime: String, db: FirebaseDatabase) {
        val scrollView = ScrollView(this)
        scrollView.setBackgroundColor(Color.rgb(10, 12, 16))

        val reportContainer = LinearLayout(this)
        reportContainer.orientation = LinearLayout.VERTICAL
        reportContainer.setPadding(32, 32, 32, 32)

        val loadingBar = ProgressBar(this)
        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        lp.gravity = Gravity.CENTER_HORIZONTAL
        loadingBar.layoutParams = lp
        reportContainer.addView(loadingBar)

        scrollView.addView(reportContainer)

        val dialog = AlertDialog.Builder(this)
            .setTitle("📋 $name")
            .setView(scrollView)
            .setPositiveButton(t("ZATVORI", "CLOSE", "ЗАКРЫТЬ", "SCHLIESSEN"), null)
            .create()
        dialog.show()

        var reportText = t("IZVEŠTAJ INTERVENCIJE\n\n", "INTERVENTION REPORT\n\n", "ОТЧЁТ ОБ ОПЕРАЦИИ\n\n", "EINSATZBERICHT\n\n")
        reportText += "Naziv: $name\nKod: $code\nKoordinator: $coordinator\nPokrenuto: $startTime\nZavršeno: $endTime\n\n"

        db.getReference("mission_logs").child(code).get().addOnSuccessListener { logSnapshot ->
            reportContainer.removeAllViews()

            if (logSnapshot.exists()) {
                val rescuers = logSnapshot.child("rescuers")
                if (rescuers.exists()) {
                    reportText += t("─── SPASIOCI ───\n", "─── RESCUERS ───\n", "─── СПАСАТЕЛИ ───\n", "─── RETTER ───\n")
                    rescuers.children.forEach { r ->
                        val rName = r.child("name").getValue(String::class.java) ?: ""
                        val rPhone = r.child("phone").getValue(String::class.java) ?: "--"
                        reportText += "• $rName  |  $rPhone\n"
                    }
                    reportText += "\n"
                }

                val finds = logSnapshot.child("finds")
                if (finds.exists() && finds.childrenCount > 0) {
                    reportText += t("─── NALAZI ───\n", "─── FINDS ───\n", "─── НАХОДКИ ───\n", "─── FUNDE ───\n")
                    finds.children.forEach { f ->
                        val fRescuer = f.child("rescuer").getValue(String::class.java) ?: ""
                        val fStatus = f.child("status").getValue(String::class.java) ?: ""
                        val fTime = f.child("time").getValue(String::class.java) ?: ""
                        val fLat = f.child("lat").getValue(Double::class.java) ?: 0.0
                        val fLon = f.child("lon").getValue(Double::class.java) ?: 0.0
                        reportText += "• $fRescuer — $fStatus ($fTime)\n"
                        if (fLat != 0.0) reportText += "  ${"%.5f".format(fLat)}, ${"%.5f".format(fLon)}\n"
                    }
                    reportText += "\n"
                }

                val result = logSnapshot.child("result").getValue(String::class.java)
                    ?: t("Nije zabilježen", "Not recorded", "Не зафиксирован", "Nicht aufgezeichnet")
                reportText += t("─── ZAKLJUČAK ───\n$result", "─── RESULT ───\n$result", "─── РЕЗУЛЬТАТ ───\n$result", "─── ERGEBNIS ───\n$result")
            } else {
                reportText += t(
                    "Detaljan log nije dostupan za ovu intervenciju.",
                    "Detailed log not available for this intervention.",
                    "Подробный журнал недоступен.",
                    "Detailliertes Protokoll nicht verfügbar."
                )
            }

            val reportTextView = TextView(this)
            reportTextView.text = reportText
            reportTextView.textSize = 13f
            reportTextView.setTextColor(Color.WHITE)
            reportTextView.setBackgroundColor(Color.rgb(10, 12, 16))
            reportContainer.addView(reportTextView)

        }.addOnFailureListener {
            reportContainer.removeAllViews()
            val errorText = TextView(this)
            errorText.text = t("Greška pri učitavanju loga.", "Error loading log.", "Ошибка загрузки.", "Ladefehler.")
            errorText.setTextColor(Color.RED)
            reportContainer.addView(errorText)
        }
    }

    // ─────────────────────────────────────────────
    // BRISANJE
    // ─────────────────────────────────────────────

    private fun showDeleteConfirmation(code: String, name: String, card: LinearLayout, container: LinearLayout, db: FirebaseDatabase) {
        AlertDialog.Builder(this)
            .setTitle("🗑️ " + t("OBRIŠI POTPUNO", "DELETE COMPLETELY", "УДАЛИТЬ ПОЛНОСТЬЮ", "VOLLSTÄNDIG LÖSCHEN"))
            .setMessage(t(
                "Da li ste sigurni?\n\n\"$name\"\n\nSvi podaci, log i chat ove potrage biće trajno obrisani.",
                "Are you sure?\n\n\"$name\"\n\nAll data, log and chat will be permanently deleted.",
                "Вы уверены?\n\n\"$name\"\n\nВсе данные будут удалены навсегда.",
                "Sind Sie sicher?\n\n\"$name\"\n\nAlle Daten werden dauerhaft gelöscht."
            ))
            .setPositiveButton(t("OBRIŠI", "DELETE", "УДАЛИТЬ", "LÖSCHEN")) { _, _ ->
                db.getReference("missions").child(code).removeValue()
                db.getReference("active_rescuers").child(code).removeValue()
                db.getReference("found_persons").child(code).removeValue()
                db.getReference("mission_logs").child(code).removeValue()
                db.getReference("mission_chat").child(code).removeValue()
                container.removeView(card)
                Toast.makeText(this, t("Obrisano.", "Deleted.", "Удалено.", "Gelöscht."), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(t("ODUSTANI", "CANCEL", "ОТМЕНА", "ABBRECHEN"), null)
            .show()
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
