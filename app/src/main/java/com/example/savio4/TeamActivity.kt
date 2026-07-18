package com.example.savio4

import android.content.Intent
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

class TeamActivity : AppCompatActivity() {

    private lateinit var missionsContainer: LinearLayout
    private lateinit var loadingBar: ProgressBar
    private lateinit var statusText: TextView
    private lateinit var sosContainer: LinearLayout
    private var missionsListener: ValueEventListener? = null
    private var sosListener: ValueEventListener? = null

    private val adminName = "gagi"
    private val adminPhone = "+381652013323"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("savio_prefs", MODE_PRIVATE)
        val rescuerName = prefs.getString("teamRescuerName", "") ?: ""
        val isObserver = prefs.getBoolean("teamIsObserver", false)
        val isAdmin = rescuerName.trim().lowercase() == adminName

        val scrollView = ScrollView(this)
        scrollView.setBackgroundColor(Color.rgb(10, 12, 16))

        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(32, 50, 32, 32)
        container.gravity = Gravity.CENTER_HORIZONTAL

        // ─── NASLOV ───
        val title = TextView(this)
        title.text = "SAVIO TEAM"
        title.textSize = 32f
        title.setTextColor(Color.rgb(0, 150, 220))
        title.setPadding(0, 0, 0, 4)

        val roleTag = if (isObserver)
            t("👁️ Posmatrac", "👁️ Observer", "👁️ Наблюдатель", "👁️ Beobachter")
        else if (isAdmin)
            t("🔑 Administrator", "🔑 Administrator", "🔑 Администратор", "🔑 Administrator")
        else
            t("🔵 Spasilac", "🔵 Rescuer", "🔵 Спасатель", "🔵 Retter")

        val nameInfo = TextView(this)
        nameInfo.text = "$roleTag: $rescuerName"
        nameInfo.textSize = 15f
        nameInfo.setTextColor(Color.rgb(0, 150, 220))
        nameInfo.setPadding(0, 0, 0, 32)

        // ─── AKTIVNI SOS SIGNALI (samo admin) ───
        if (isAdmin) {
            val sosSep = TextView(this)
            sosSep.text = "🆘 " + t(
                "─── AKTIVNI SOS SIGNALI ───",
                "─── ACTIVE SOS SIGNALS ───",
                "─── АКТИВНЫЕ SOS СИГНАЛЫ ───",
                "─── AKTIVE SOS-SIGNALE ───"
            )
            sosSep.textSize = 16f
            sosSep.setTextColor(Color.rgb(220, 50, 50))
            sosSep.typeface = android.graphics.Typeface.DEFAULT_BOLD
            sosSep.gravity = Gravity.CENTER
            sosSep.setPadding(0, 0, 0, 12)

            sosContainer = LinearLayout(this)
            sosContainer.orientation = LinearLayout.VERTICAL

            val sosStatus = TextView(this)
            sosStatus.text = t("Učitavam SOS signale...", "Loading SOS signals...", "Загрузка SOS...", "SOS wird geladen...")
            sosStatus.textSize = 13f
            sosStatus.setTextColor(Color.rgb(150, 150, 150))
            sosStatus.gravity = Gravity.CENTER
            sosStatus.setPadding(0, 0, 0, 8)

            val sosSeparator = TextView(this)
            sosSeparator.text = "─────────────────────────────"
            sosSeparator.textSize = 12f
            sosSeparator.setTextColor(Color.rgb(50, 60, 80))
            sosSeparator.gravity = Gravity.CENTER
            val sosSepParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            sosSepParams.setMargins(0, 16, 0, 16)
            sosSeparator.layoutParams = sosSepParams

            container.addView(title)
            container.addView(nameInfo)
            container.addView(sosSep)
            container.addView(sosStatus)
            container.addView(sosContainer)
            container.addView(sosSeparator)

            loadSosSignals(sosContainer, sosStatus, rescuerName)
        } else {
            container.addView(title)
            container.addView(nameInfo)
        }

        // ─── AKTIVNE POTRAGE ───
        val sep1 = TextView(this)
        sep1.text = t("─── AKTIVNE POTRAGE ───", "─── ACTIVE MISSIONS ───", "─── АКТИВНЫЕ ОПЕРАЦИИ ───", "─── AKTIVE EINSÄTZE ───")
        sep1.textSize = 14f
        sep1.setTextColor(Color.rgb(100, 100, 100))
        sep1.gravity = Gravity.CENTER
        sep1.setPadding(0, 0, 0, 16)

        loadingBar = ProgressBar(this)
        val loadingParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        loadingParams.gravity = Gravity.CENTER_HORIZONTAL
        loadingParams.setMargins(0, 8, 0, 8)
        loadingBar.layoutParams = loadingParams

        missionsContainer = LinearLayout(this)
        missionsContainer.orientation = LinearLayout.VERTICAL

        statusText = TextView(this)
        statusText.text = ""
        statusText.textSize = 14f
        statusText.setTextColor(Color.rgb(180, 180, 180))
        statusText.gravity = Gravity.CENTER
        statusText.setPadding(0, 8, 0, 8)

        container.addView(sep1)
        container.addView(loadingBar)
        container.addView(missionsContainer)
        container.addView(statusText)

        // ─── KREIRAJ POTRAGU (samo admin) ───
        if (isAdmin) {
            val sep2 = TextView(this)
            sep2.text = t("─── KREIRAJ NOVU POTRAGU ───", "─── CREATE NEW MISSION ───", "─── СОЗДАТЬ НОВУЮ ОПЕРАЦИЮ ───", "─── NEUE SUCHE ERSTELLEN ───")
            sep2.textSize = 14f
            sep2.setTextColor(Color.rgb(100, 100, 100))
            sep2.gravity = Gravity.CENTER
            sep2.setPadding(0, 32, 0, 16)

            val missionNameLabel = TextView(this)
            missionNameLabel.text = t("Naziv potrage:", "Mission name:", "Название операции:", "Name der Suche:")
            missionNameLabel.textSize = 15f
            missionNameLabel.setTextColor(Color.WHITE)
            missionNameLabel.setPadding(0, 0, 0, 8)

            val missionNameInput = EditText(this)
            missionNameInput.hint = t("Npr. Kopaonik 15.06.2025", "E.g. Mountain search 15.06", "Напр. Гора поиск 15.06", "z.B. Berg Suche 15.06")
            missionNameInput.setTextColor(Color.WHITE)
            missionNameInput.setHintTextColor(Color.rgb(120, 120, 120))
            missionNameInput.textSize = 16f
            val missionParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            missionParams.setMargins(0, 0, 0, 8)
            missionNameInput.layoutParams = missionParams

            // Skriveno polje za SOS koordinate
            val sosLatInput = EditText(this)
            sosLatInput.visibility = android.view.View.GONE
            val sosLonInput = EditText(this)
            sosLonInput.visibility = android.view.View.GONE
            val sosNameInput = EditText(this)
            sosNameInput.visibility = android.view.View.GONE

            val selectedSosInfo = TextView(this)
            selectedSosInfo.text = ""
            selectedSosInfo.textSize = 13f
            selectedSosInfo.setTextColor(Color.rgb(0, 200, 100))
            selectedSosInfo.setPadding(0, 0, 0, 8)

            val btnCreate = Button(this)
            btnCreate.text = t("KREIRAJ POTRAGU", "CREATE MISSION", "СОЗДАТЬ ОПЕРАЦИЮ", "SUCHE ERSTELLEN")
            btnCreate.setTextColor(Color.WHITE)
            btnCreate.setBackgroundColor(Color.rgb(0, 130, 60))
            btnCreate.textSize = 16f
            val createParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            createParams.setMargins(0, 0, 0, 16)
            btnCreate.layoutParams = createParams

            val createStatus = TextView(this)
            createStatus.text = ""
            createStatus.textSize = 14f
            createStatus.setTextColor(Color.RED)
            createStatus.gravity = Gravity.CENTER

            val createLoadingBar = ProgressBar(this)
            createLoadingBar.visibility = android.view.View.GONE

            btnCreate.setOnClickListener {
                val missionName = missionNameInput.text.toString().trim()
                if (missionName.isEmpty()) {
                    createStatus.setTextColor(Color.RED)
                    createStatus.text = t("Molimo unesite naziv potrage.", "Please enter mission name.", "Введите название операции.", "Suchnamen eingeben.")
                    return@setOnClickListener
                }

                val missionCode = "SAVIO-" + SimpleDateFormat("yyyyMMdd-HHmm", Locale.getDefault()).format(Date())
                createStatus.setTextColor(Color.rgb(0, 150, 220))
                createStatus.text = t("Kreiram potragu...", "Creating mission...", "Создание операции...", "Wird erstellt...")
                createLoadingBar.visibility = android.view.View.VISIBLE
                btnCreate.isEnabled = false

                val sosLat = sosLatInput.text.toString().toDoubleOrNull() ?: 0.0
                val sosLon = sosLonInput.text.toString().toDoubleOrNull() ?: 0.0
                val sosPersonName = sosNameInput.text.toString()

                createMission(missionCode, missionName, rescuerName, sosLat, sosLon, sosPersonName) { success ->
                    runOnUiThread {
                        createLoadingBar.visibility = android.view.View.GONE
                        btnCreate.isEnabled = true
                        if (success) {
                            getSharedPreferences("savio_prefs", MODE_PRIVATE).edit()
                                .putString("teamMissionCode", missionCode)
                                .putString("teamMissionName", missionName)
                                .putBoolean("teamIsCoordinator", true)
                                .putBoolean("teamIsObserver", false)
                                .putLong("teamMissionStartTime", System.currentTimeMillis())
                                .apply()
                            startActivity(Intent(this, TeamMapActivity::class.java))
                            finish()
                        } else {
                            createStatus.setTextColor(Color.RED)
                            createStatus.text = t("Greska. Provjerite internet.", "Error. Check internet.", "Ошибка.", "Fehler.")
                        }
                    }
                }
            }

            container.addView(sep2)
            container.addView(missionNameLabel)
            container.addView(missionNameInput)
            container.addView(selectedSosInfo)
            container.addView(sosLatInput)
            container.addView(sosLonInput)
            container.addView(sosNameInput)
            container.addView(btnCreate)
            container.addView(createLoadingBar)
            container.addView(createStatus)
        }

        scrollView.addView(container)
        setContentView(scrollView)
        applyWindowInsets()

        loadActiveMissions(rescuerName, isObserver)

        // Real-time SOS popup — iskaci kada se pojavi novi SOS
        if (isAdmin) {
            startListeningNewSosSignals()
        }
    }

    // ─────────────────────────────────────────────
    // SOS SIGNALI
    // ─────────────────────────────────────────────

    private fun startListeningNewSosSignals() {
        val db = FirebaseDatabase.getInstance()
        val seenSosIds = mutableSetOf<String>()

        // Prvo učitaj postojeće SOS signale da ih ne prikazujemo kao nove
        db.getReference("sos_locations").get().addOnSuccessListener { snapshot ->
            snapshot.children.forEach { seenSosIds.add(it.key ?: "") }

            // Sada slušaj nove
            db.getReference("sos_locations").addChildEventListener(object : com.google.firebase.database.ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val id = snapshot.key ?: return
                    if (id in seenSosIds) return // Već postoji, nije nov
                    seenSosIds.add(id)

                    val active = snapshot.child("active").getValue(Boolean::class.java) ?: false
                    if (!active) return

                    // Preskoci ako je ovo MOJ sopstveni SOS signal
                    val myIncidentId = getSharedPreferences("savio_prefs", MODE_PRIVATE).getString("incidentId", "") ?: ""
                    if (id == myIncidentId) return

                    val name = snapshot.child("name").getValue(String::class.java) ?: t("Nepoznato lice", "Unknown person", "Неизвестное лицо", "Unbekannte Person")
                    val lat = snapshot.child("lat").getValue(Double::class.java) ?: 0.0
                    val lon = snapshot.child("lon").getValue(Double::class.java) ?: 0.0
                    val condition = snapshot.child("condition").getValue(String::class.java) ?: ""
                    val priority = snapshot.child("priority").getValue(String::class.java) ?: ""
                    val time = snapshot.child("time").getValue(String::class.java) ?: ""

                    runOnUiThread {
                        showNewSosPopup(id, name, lat, lon, condition.ifEmpty { priority }, time)
                    }
                }
                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
            })
        }
    }

    private fun showNewSosPopup(incidentId: String, name: String, lat: Double, lon: Double, condition: String, time: String) {
        // Vibracija
        try {
            val vibrator = getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(android.os.VibrationEffect.createWaveform(longArrayOf(0, 500, 200, 500, 200, 500), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 500, 200, 500, 200, 500), -1)
            }
        } catch (_: Exception) {}

        val coordsText = if (lat != 0.0) "\n\n📍 ${"%.5f".format(lat)}, ${"%.5f".format(lon)}" else ""

        AlertDialog.Builder(this)
            .setTitle("🆘🆘🆘 " + t("NOVI SOS SIGNAL!", "NEW SOS SIGNAL!", "НОВЫЙ SOS СИГНАЛ!", "NEUES SOS-SIGNAL!"))
            .setMessage(t(
                "Lice: $name\nStanje: $condition\nVreme: $time$coordsText\n\nDa li želite da kreirate potragu za ovo lice?",
                "Person: $name\nCondition: $condition\nTime: $time$coordsText\n\nDo you want to create a mission for this person?",
                "Лицо: $name\nСостояние: $condition\nВремя: $time$coordsText\n\nСоздать операцию?",
                "Person: $name\nZustand: $condition\nZeit: $time$coordsText\n\nEinsatz erstellen?"
            ))
            .setPositiveButton("🚨 " + t("KREIRAJ POTRAGU", "CREATE MISSION", "СОЗДАТЬ ОПЕРАЦИЮ", "EINSATZ ERSTELLEN")) { _, _ ->
                showCreateMissionForSosDialog(incidentId, name, lat, lon, condition)
            }
            .setNegativeButton(t("ZATVORI", "CLOSE", "ЗАКРЫТЬ", "SCHLIESSEN"), null)
            .setCancelable(false)
            .show()
    }

    private fun loadSosSignals(container: LinearLayout, statusText: TextView, rescuerName: String) {
        val db = FirebaseDatabase.getInstance()
        sosListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                container.removeAllViews()
                val activeSos = mutableListOf<DataSnapshot>()

                snapshot.children.forEach { sosSnapshot ->
                    val active = sosSnapshot.child("active").getValue(Boolean::class.java) ?: false
                    if (active) activeSos.add(sosSnapshot)
                }

                if (activeSos.isEmpty()) {
                    statusText.text = t(
                        "Nema aktivnih SOS signala.",
                        "No active SOS signals.",
                        "Нет активных SOS-сигналов.",
                        "Keine aktiven SOS-Signale."
                    )
                    statusText.setTextColor(Color.rgb(150, 150, 150))
                    return
                }

                statusText.text = t(
                    "🆘 Aktivnih SOS signala: ${activeSos.size}",
                    "🆘 Active SOS signals: ${activeSos.size}",
                    "🆘 Активных SOS сигналов: ${activeSos.size}",
                    "🆘 Aktive SOS-Signale: ${activeSos.size}"
                )
                statusText.setTextColor(Color.rgb(220, 50, 50))

                activeSos.forEach { sosSnapshot ->
                    val incidentId = sosSnapshot.key ?: return@forEach
                    val name = sosSnapshot.child("name").getValue(String::class.java) ?: t("Nepoznato", "Unknown", "Неизвестно", "Unbekannt")
                    val lat = sosSnapshot.child("lat").getValue(Double::class.java) ?: 0.0
                    val lon = sosSnapshot.child("lon").getValue(Double::class.java) ?: 0.0
                    val priority = sosSnapshot.child("priority").getValue(String::class.java) ?: ""
                    val condition = sosSnapshot.child("condition").getValue(String::class.java) ?: ""
                    val time = sosSnapshot.child("time").getValue(String::class.java) ?: ""

                    addSosCard(container, incidentId, name, lat, lon, priority, condition, time)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        db.getReference("sos_locations").addValueEventListener(sosListener!!)
    }

    private fun addSosCard(container: LinearLayout, incidentId: String, name: String, lat: Double, lon: Double, priority: String, condition: String, time: String) {
        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.setPadding(24, 20, 24, 20)

        val cardBg = GradientDrawable()
        cardBg.setColor(Color.rgb(50, 10, 10))
        cardBg.cornerRadius = 16f
        cardBg.setStroke(2, Color.rgb(220, 50, 50))
        card.background = cardBg

        val cardParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        cardParams.setMargins(0, 0, 0, 12)
        card.layoutParams = cardParams

        val nameText = TextView(this)
        nameText.text = "🆘  $name"
        nameText.textSize = 17f
        nameText.setTextColor(Color.WHITE)
        nameText.typeface = android.graphics.Typeface.DEFAULT_BOLD
        nameText.setPadding(0, 0, 0, 4)

        val conditionText = TextView(this)
        conditionText.text = if (condition.isNotEmpty()) condition else priority
        conditionText.textSize = 13f
        conditionText.setTextColor(Color.rgb(255, 150, 150))
        conditionText.setPadding(0, 0, 0, 4)

        val coordText = TextView(this)
        coordText.text = if (lat != 0.0) "📍 ${"%.5f".format(lat)}, ${"%.5f".format(lon)}" else t("Koordinate nedostupne", "Coordinates unavailable", "Координаты недоступны", "Koordinaten nicht verfügbar")
        coordText.textSize = 12f
        coordText.setTextColor(Color.rgb(180, 180, 180))
        coordText.setPadding(0, 0, 0, 4)

        val timeText = TextView(this)
        timeText.text = t("Vreme: $time", "Time: $time", "Время: $time", "Zeit: $time")
        timeText.textSize = 11f
        timeText.setTextColor(Color.rgb(120, 120, 120))
        timeText.setPadding(0, 0, 0, 12)

        val btnCreateForThis = Button(this)
        btnCreateForThis.text = "🚨 " + t(
            "KREIRAJ POTRAGU ZA OVO LICE",
            "CREATE MISSION FOR THIS PERSON",
            "СОЗДАТЬ ОПЕРАЦИЮ ДЛЯ ЭТОГО ЛИЦА",
            "EINSATZ FÜR DIESE PERSON ERSTELLEN"
        )
        btnCreateForThis.setTextColor(Color.WHITE)
        btnCreateForThis.setBackgroundColor(Color.rgb(180, 0, 0))
        btnCreateForThis.textSize = 14f
        btnCreateForThis.setOnClickListener {
            showCreateMissionForSosDialog(incidentId, name, lat, lon, condition)
        }

        card.addView(nameText)
        card.addView(conditionText)
        card.addView(coordText)
        card.addView(timeText)
        card.addView(btnCreateForThis)
        container.addView(card)
    }

    private fun showCreateMissionForSosDialog(incidentId: String, personName: String, lat: Double, lon: Double, condition: String) {
        val input = EditText(this)
        input.setText("Potraga — $personName")
        input.setTextColor(Color.WHITE)
        input.setPadding(16, 16, 16, 16)

        AlertDialog.Builder(this)
            .setTitle("🚨 " + t("KREIRAJ POTRAGU", "CREATE MISSION", "СОЗДАТЬ ОПЕРАЦИЮ", "EINSATZ ERSTELLEN"))
            .setMessage(t(
                "Kreiraćete potragu za:\n$personName\n\nStanje: $condition\nKoordinate: ${"%.5f".format(lat)}, ${"%.5f".format(lon)}\n\nNaziv potrage:",
                "Creating mission for:\n$personName\n\nCondition: $condition\nCoordinates: ${"%.5f".format(lat)}, ${"%.5f".format(lon)}\n\nMission name:",
                "Создание операции для:\n$personName\n\nСостояние: $condition\n\nНазвание:",
                "Einsatz erstellen für:\n$personName\n\nZustand: $condition\n\nEinsatzname:"
            ))
            .setView(input)
            .setPositiveButton(t("KREIRAJ", "CREATE", "СОЗДАТЬ", "ERSTELLEN")) { _, _ ->
                val missionName = input.text.toString().trim().ifEmpty { "Potraga — $personName" }
                val missionCode = "SAVIO-" + SimpleDateFormat("yyyyMMdd-HHmm", Locale.getDefault()).format(Date())
                val rescuerName = getSharedPreferences("savio_prefs", MODE_PRIVATE).getString("teamRescuerName", "") ?: ""

                createMission(missionCode, missionName, rescuerName, lat, lon, personName) { success ->
                    runOnUiThread {
                        if (success) {
                            getSharedPreferences("savio_prefs", MODE_PRIVATE).edit()
                                .putString("teamMissionCode", missionCode)
                                .putString("teamMissionName", missionName)
                                .putBoolean("teamIsCoordinator", true)
                                .putBoolean("teamIsObserver", false)
                                .putLong("teamMissionStartTime", System.currentTimeMillis())
                                .apply()
                            startActivity(Intent(this, TeamMapActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(this, t("Greška. Proverite internet.", "Error. Check internet.", "Ошибка.", "Fehler."), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton(t("ODUSTANI", "CANCEL", "ОТМЕНА", "ABBRECHEN"), null)
            .show()
    }

    // ─────────────────────────────────────────────
    // AKTIVNE POTRAGE
    // ─────────────────────────────────────────────

    private fun loadActiveMissions(rescuerName: String, isObserver: Boolean) {
        loadingBar.visibility = android.view.View.VISIBLE
        statusText.text = t("Ucitavam aktivne potrage...", "Loading active missions...", "Загрузка операций...", "Wird geladen...")
        statusText.setTextColor(Color.rgb(180, 180, 180))

        val db = FirebaseDatabase.getInstance()
        missionsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                loadingBar.visibility = android.view.View.GONE
                missionsContainer.removeAllViews()

                val activeMissions = mutableListOf<DataSnapshot>()
                snapshot.children.forEach { missionSnapshot ->
                    val active = missionSnapshot.child("active").getValue(Boolean::class.java) ?: false
                    if (active) activeMissions.add(missionSnapshot)
                }

                if (activeMissions.isEmpty()) {
                    showNoMissionsInfo()
                } else {
                    statusText.text = t("Aktivne potrage (${activeMissions.size}):", "Active missions (${activeMissions.size}):", "Активные операции (${activeMissions.size}):", "Aktive Einsätze (${activeMissions.size}):")
                    statusText.setTextColor(Color.rgb(0, 200, 100))
                    activeMissions.forEach { missionSnapshot ->
                        val code = missionSnapshot.child("code").getValue(String::class.java) ?: return@forEach
                        val name = missionSnapshot.child("name").getValue(String::class.java) ?: code
                        val coordinator = missionSnapshot.child("coordinator").getValue(String::class.java) ?: ""
                        val createdAt = missionSnapshot.child("createdAt").getValue(Long::class.java) ?: 0L
                        addMissionCard(code, name, coordinator, createdAt, rescuerName, isObserver)
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                loadingBar.visibility = android.view.View.GONE
                statusText.text = t("Greška pri učitavanju.", "Error loading.", "Ошибка загрузки.", "Ladefehler.")
                statusText.setTextColor(Color.RED)
            }
        }
        db.getReference("missions").addValueEventListener(missionsListener!!)
    }

    private fun showNoMissionsInfo() {
        val noMissionsBox = LinearLayout(this)
        noMissionsBox.orientation = LinearLayout.VERTICAL
        noMissionsBox.setPadding(24, 24, 24, 24)

        val bg = GradientDrawable()
        bg.setColor(Color.rgb(20, 25, 35))
        bg.cornerRadius = 16f
        bg.setStroke(2, Color.rgb(60, 70, 90))
        noMissionsBox.background = bg

        val boxParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        boxParams.setMargins(0, 0, 0, 16)
        noMissionsBox.layoutParams = boxParams

        val noMissionsText = TextView(this)
        noMissionsText.text = t("Trenutno nema aktivnih potraga.\n\nLista se osvjezava automatski.", "No active missions.\n\nList refreshes automatically.", "Нет активных операций.", "Keine aktiven Einsätze.")
        noMissionsText.textSize = 14f
        noMissionsText.setTextColor(Color.rgb(180, 180, 180))
        noMissionsText.setPadding(0, 0, 0, 20)

        val contactTitle = TextView(this)
        contactTitle.text = "⚠️ " + t("POTRAGA JOS NIJE POKRENUTA?", "MISSION NOT STARTED YET?", "ОПЕРАЦИЯ ЕЩЁ НЕ НАЧАТА?", "EINSATZ NOCH NICHT GESTARTET?")
        contactTitle.textSize = 15f
        contactTitle.setTextColor(Color.rgb(255, 200, 0))
        contactTitle.typeface = android.graphics.Typeface.DEFAULT_BOLD
        contactTitle.setPadding(0, 0, 0, 10)

        val contactText = TextView(this)
        contactText.text = t(
            "Ako ste primili poziv za potragu ali sesija jos nije kreirana, odmah kontaktirajte administratora sistema telefonskim pozivom.",
            "If you received a rescue call but the session hasn't been created yet, immediately contact the system administrator by phone.",
            "Если вы получили вызов, немедленно свяжитесь с администратором системы по телефону.",
            "Kontaktieren Sie sofort den Systemadministrator per Telefon."
        )
        contactText.textSize = 13f
        contactText.setTextColor(Color.rgb(200, 200, 200))
        contactText.setPadding(0, 0, 0, 16)

        val btnCall = Button(this)
        btnCall.text = "📞 " + t("POZOVI ADMINISTRATORA SISTEMA", "CALL SYSTEM ADMINISTRATOR", "ПОЗВОНИТЬ АДМИНИСТРАТОРУ", "SYSTEMADMINISTRATOR ANRUFEN")
        btnCall.setTextColor(Color.WHITE)
        btnCall.setBackgroundColor(Color.rgb(0, 130, 60))
        btnCall.textSize = 14f
        btnCall.setOnClickListener {
            try {
                val intent = android.content.Intent(android.content.Intent.ACTION_DIAL)
                intent.data = android.net.Uri.parse("tel:$adminPhone")
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, t("Greska pri pozivu.", "Call error.", "Ошибка вызова.", "Anruffehler."), Toast.LENGTH_SHORT).show()
            }
        }

        noMissionsBox.addView(noMissionsText)
        noMissionsBox.addView(contactTitle)
        noMissionsBox.addView(contactText)
        noMissionsBox.addView(btnCall)
        missionsContainer.addView(noMissionsBox)

        statusText.text = t("Nema aktivnih potraga.", "No active missions.", "Нет активных операций.", "Keine aktiven Einsätze.")
        statusText.setTextColor(Color.rgb(150, 150, 150))
    }

    private fun addMissionCard(code: String, name: String, coordinator: String, createdAt: Long, rescuerName: String, isObserver: Boolean) {
        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.setPadding(24, 20, 24, 20)

        val cardBg = GradientDrawable()
        cardBg.setColor(Color.rgb(0, 30, 55))
        cardBg.cornerRadius = 16f
        cardBg.setStroke(2, Color.rgb(0, 120, 200))
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
        codeText.setTextColor(Color.rgb(0, 180, 255))
        codeText.setPadding(0, 0, 0, 4)

        val coordText = TextView(this)
        coordText.text = t("Koordinator: $coordinator", "Coordinator: $coordinator", "Координатор: $coordinator", "Koordinator: $coordinator")
        coordText.textSize = 13f
        coordText.setTextColor(Color.rgb(180, 180, 180))
        coordText.setPadding(0, 0, 0, 4)

        val rescuerCountText = TextView(this)
        rescuerCountText.text = t("Spasilaca: ucitavam...", "Rescuers: loading...", "Спасателей: загрузка...", "Retter: wird geladen...")
        rescuerCountText.textSize = 13f
        rescuerCountText.setTextColor(Color.rgb(150, 150, 150))
        rescuerCountText.setPadding(0, 0, 0, 4)

        val timeFormatted = if (createdAt > 0) SimpleDateFormat("dd.MM.yyyy. HH:mm", Locale.getDefault()).format(Date(createdAt)) else "--"
        val timeText = TextView(this)
        timeText.text = t("Pokrenuto: $timeFormatted", "Started: $timeFormatted", "Начато: $timeFormatted", "Gestartet: $timeFormatted")
        timeText.textSize = 12f
        timeText.setTextColor(Color.rgb(120, 120, 120))
        timeText.setPadding(0, 0, 0, 16)

        val btnJoin = Button(this)
        btnJoin.text = if (isObserver)
            t("👁️  POSMATRAJ POTRAGU", "👁️  OBSERVE MISSION", "👁️  НАБЛЮДАТЬ", "👁️  BEOBACHTEN")
        else
            t("🔵  PRIDRUZI SE POTRAZI", "🔵  JOIN MISSION", "🔵  ПРИСОЕДИНИТЬСЯ", "🔵  BEITRETEN")
        btnJoin.setTextColor(Color.WHITE)
        btnJoin.setBackgroundColor(if (isObserver) Color.rgb(60, 60, 100) else Color.rgb(0, 100, 180))
        btnJoin.textSize = 15f

        btnJoin.setOnClickListener {
            btnJoin.isEnabled = false
            btnJoin.text = t("Povezujem se...", "Connecting...", "Подключение...", "Verbinde...")

            if (isObserver) {
                getSharedPreferences("savio_prefs", MODE_PRIVATE).edit()
                    .putString("teamMissionCode", code)
                    .putString("teamMissionName", name)
                    .putBoolean("teamIsCoordinator", false)
                    .putBoolean("teamIsObserver", true)
                    .apply()
                startActivity(Intent(this, TeamMapActivity::class.java))
                finish()
            } else {
                joinMission(code, rescuerName) { success, missionName ->
                    runOnUiThread {
                        if (success) {
                            getSharedPreferences("savio_prefs", MODE_PRIVATE).edit()
                                .putString("teamMissionCode", code)
                                .putString("teamMissionName", missionName ?: name)
                                .putBoolean("teamIsCoordinator", false)
                                .putBoolean("teamIsObserver", false)
                                .putLong("teamMissionStartTime", System.currentTimeMillis())
                                .apply()
                            startActivity(Intent(this, TeamMapActivity::class.java))
                            finish()
                        } else {
                            btnJoin.isEnabled = true
                            btnJoin.text = t("🔵  PRIDRUZI SE POTRAZI", "🔵  JOIN MISSION", "🔵  ПРИСОЕДИНИТЬСЯ", "🔵  BEITRETEN")
                            Toast.makeText(this, t("Greska.", "Error.", "Ошибка.", "Fehler."), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        card.addView(nameText)
        card.addView(codeText)
        card.addView(coordText)
        card.addView(rescuerCountText)
        card.addView(timeText)
        card.addView(btnJoin)
        missionsContainer.addView(card)

        FirebaseDatabase.getInstance().getReference("active_rescuers").child(code).get()
            .addOnSuccessListener { snapshot ->
                val count = snapshot.childrenCount.toInt()
                runOnUiThread { rescuerCountText.text = t("Spasilaca u akciji: $count", "Rescuers in mission: $count", "Спасателей в операции: $count", "Retter im Einsatz: $count") }
            }
    }

    // ─────────────────────────────────────────────
    // FIREBASE — KREIRANJE I PRIDRUŽIVANJE
    // ─────────────────────────────────────────────

    private fun createMission(missionCode: String, missionName: String, rescuerName: String, sosLat: Double, sosLon: Double, sosPersonName: String, callback: (Boolean) -> Unit) {
        val db = FirebaseDatabase.getInstance()
        val missionData = mutableMapOf<String, Any>(
            "name" to missionName,
            "code" to missionCode,
            "coordinator" to rescuerName,
            "createdAt" to System.currentTimeMillis(),
            "active" to true
        )

        // Dodaj SOS koordinate u misiju ako postoje
        if (sosLat != 0.0 && sosLon != 0.0) {
            missionData["sosLat"] = sosLat
            missionData["sosLon"] = sosLon
            missionData["sosPersonName"] = sosPersonName
        }

        db.getReference("missions").child(missionCode).setValue(missionData)
            .addOnSuccessListener {
                val prefs = getSharedPreferences("savio_prefs", MODE_PRIVATE)
                val rescuerColor = prefs.getInt("teamRescuerColor", Color.rgb(30, 120, 220))
                val rescuerPhone = prefs.getString("teamRescuerPhone", "") ?: ""
                val rescuerData = mapOf(
                    "name" to rescuerName,
                    "color" to rescuerColor,
                    "phone" to rescuerPhone,
                    "lat" to 0.0,
                    "lon" to 0.0,
                    "lastUpdate" to System.currentTimeMillis()
                )
                db.getReference("active_rescuers")
                    .child(missionCode)
                    .child(rescuerName.replace(".", "_").replace("#", "_").replace("$", "_").replace("[", "_").replace("]", "_"))
                    .setValue(rescuerData)
                    .addOnSuccessListener { callback(true) }
                    .addOnFailureListener { callback(false) }
            }
            .addOnFailureListener { callback(false) }
    }

    private fun joinMission(missionCode: String, rescuerName: String, callback: (Boolean, String?) -> Unit) {
        val db = FirebaseDatabase.getInstance()
        db.getReference("missions").child(missionCode).get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) { callback(false, null); return@addOnSuccessListener }
            val active = snapshot.child("active").getValue(Boolean::class.java) ?: false
            if (!active) { callback(false, null); return@addOnSuccessListener }
            val missionName = snapshot.child("name").getValue(String::class.java)
            val prefs = getSharedPreferences("savio_prefs", MODE_PRIVATE)
            val rescuerColor = prefs.getInt("teamRescuerColor", Color.rgb(30, 120, 220))
            val rescuerPhone = prefs.getString("teamRescuerPhone", "") ?: ""
            val rescuerData = mapOf(
                "name" to rescuerName,
                "color" to rescuerColor,
                "phone" to rescuerPhone,
                "lat" to 0.0,
                "lon" to 0.0,
                "lastUpdate" to System.currentTimeMillis()
            )
            db.getReference("active_rescuers")
                .child(missionCode)
                .child(rescuerName.replace(".", "_").replace("#", "_").replace("$", "_").replace("[", "_").replace("]", "_"))
                .setValue(rescuerData)
                .addOnSuccessListener { callback(true, missionName) }
                .addOnFailureListener { callback(false, null) }
        }.addOnFailureListener { callback(false, null) }
    }

    private fun applyWindowInsets() {
        val rootView = window.decorView.findViewById<android.view.View>(android.R.id.content)
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        missionsListener?.let { FirebaseDatabase.getInstance().getReference("missions").removeEventListener(it) }
        sosListener?.let { FirebaseDatabase.getInstance().getReference("sos_locations").removeEventListener(it) }
    }

    private fun currentLanguage(): String {
        return getSharedPreferences("savio_prefs", MODE_PRIVATE).getString("language", "sr") ?: "sr"
    }

    private fun t(sr: String, en: String, ru: String, de: String): String {
        return when (currentLanguage()) { "en" -> en; "ru" -> ru; "de" -> de; else -> sr }
    }
}
