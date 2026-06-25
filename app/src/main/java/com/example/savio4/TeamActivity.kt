package com.example.savio4

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TeamActivity : AppCompatActivity() {

    private lateinit var missionsContainer: LinearLayout
    private lateinit var loadingBar: ProgressBar
    private lateinit var statusText: TextView
    private var missionsListener: ValueEventListener? = null

    // Jedino ime koje moze kreirati potragu
    private val adminName = "gagi"
    // Kontakt telefon administratora
    private val adminPhone = "+38165203323"

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

        // ─── AKTIVNE POTRAGE ───
        val sep1 = TextView(this)
        sep1.text = t(
            "─── AKTIVNE POTRAGE ───",
            "─── ACTIVE MISSIONS ───",
            "─── АКТИВНЫЕ ОПЕРАЦИИ ───",
            "─── AKTIVE EINSÄTZE ───"
        )
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

        container.addView(title)
        container.addView(nameInfo)
        container.addView(sep1)
        container.addView(loadingBar)
        container.addView(missionsContainer)
        container.addView(statusText)

        // ─── SEKCIJA ZA KREIRANJE — samo admin ───
        if (isAdmin) {
            val sep2 = TextView(this)
            sep2.text = t(
                "─── KREIRAJ NOVU POTRAGU ───",
                "─── CREATE NEW MISSION ───",
                "─── СОЗДАТЬ НОВУЮ ОПЕРАЦИЮ ───",
                "─── NEUE SUCHE ERSTELLEN ───"
            )
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
            missionParams.setMargins(0, 0, 0, 16)
            missionNameInput.layoutParams = missionParams

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

                createMission(missionCode, missionName, rescuerName) { success ->
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
                            createStatus.text = t("Greska. Provjerite internet.", "Error. Check internet.", "Ошибка. Проверьте интернет.", "Fehler. Internet prüfen.")
                        }
                    }
                }
            }

            container.addView(sep2)
            container.addView(missionNameLabel)
            container.addView(missionNameInput)
            container.addView(btnCreate)
            container.addView(createLoadingBar)
            container.addView(createStatus)
        }

        scrollView.addView(container)
        setContentView(scrollView)
        applyWindowInsets()

        loadActiveMissions(rescuerName, isObserver)
    }

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
                    statusText.text = t(
                        "Aktivne potrage (${activeMissions.size}):",
                        "Active missions (${activeMissions.size}):",
                        "Активные операции (${activeMissions.size}):",
                        "Aktive Einsätze (${activeMissions.size}):"
                    )
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
                statusText.text = t("Greska pri ucitavanju.", "Error loading.", "Ошибка загрузки.", "Ladefehler.")
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
        noMissionsText.text = t(
            "Trenutno nema aktivnih potraga.\n\nLista se osvjezava automatski.",
            "No active missions at the moment.\n\nList refreshes automatically.",
            "В данный момент нет активных операций.\n\nСписок обновляется автоматически.",
            "Derzeit keine aktiven Einsätze.\n\nListe wird automatisch aktualisiert."
        )
        noMissionsText.textSize = 14f
        noMissionsText.setTextColor(Color.rgb(180, 180, 180))
        noMissionsText.setPadding(0, 0, 0, 20)

        val contactTitle = TextView(this)
        contactTitle.text = "⚠️ " + t(
            "POTRAGA JOS NIJE POKRENUTA?",
            "MISSION NOT STARTED YET?",
            "ОПЕРАЦИЯ ЕЩЁ НЕ НАЧАТА?",
            "EINSATZ NOCH NICHT GESTARTET?"
        )
        contactTitle.textSize = 15f
        contactTitle.setTextColor(Color.rgb(255, 200, 0))
        contactTitle.typeface = android.graphics.Typeface.DEFAULT_BOLD
        contactTitle.setPadding(0, 0, 0, 10)

        val contactText = TextView(this)
        contactText.text = t(
            "Ako ste primili poziv za potragu ali sesija jos nije kreirana, odmah kontaktirajte administratora sistema telefonskim pozivom. Administrator ce pokrenuti sesiju i vi cete je automatski vidjeti na ovoj listi.",
            "If you received a rescue call but the session hasn't been created yet, immediately contact the system administrator by phone call. The administrator will start the session and you will automatically see it on this list.",
            "Если вы получили вызов на поиск, но сессия ещё не создана, немедленно свяжитесь с администратором системы по телефону. Администратор запустит сессию и вы автоматически увидите её в этом списке.",
            "Wenn Sie einen Rettungsruf erhalten haben, aber die Sitzung noch nicht erstellt wurde, kontaktieren Sie sofort den Systemadministrator per Telefon. Der Administrator startet die Sitzung und Sie sehen sie automatisch in dieser Liste."
        )
        contactText.textSize = 13f
        contactText.setTextColor(Color.rgb(200, 200, 200))
        contactText.setPadding(0, 0, 0, 16)

        val btnCall = Button(this)
        btnCall.text = "📞 " + t(
            "POZOVI ADMINISTRATORA SISTEMA",
            "CALL SYSTEM ADMINISTRATOR",
            "ПОЗВОНИТЬ АДМИНИСТРАТОРУ",
            "SYSTEMADMINISTRATOR ANRUFEN"
        )
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

        statusText.text = t(
            "Nema aktivnih potraga.",
            "No active missions.",
            "Нет активных операций.",
            "Keine aktiven Einsätze."
        )
        statusText.setTextColor(Color.rgb(150, 150, 150))
    }

    private fun addMissionCard(
        code: String,
        name: String,
        coordinator: String,
        createdAt: Long,
        rescuerName: String,
        isObserver: Boolean
    ) {
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
        nameText.textSize = 18f
        nameText.setTextColor(Color.WHITE)
        nameText.typeface = android.graphics.Typeface.DEFAULT_BOLD
        nameText.setPadding(0, 0, 0, 6)

        val codeText = TextView(this)
        codeText.text = t("Kod: $code", "Code: $code", "Код: $code", "Code: $code")
        codeText.textSize = 13f
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

        val timeFormatted = if (createdAt > 0)
            SimpleDateFormat("dd.MM.yyyy. HH:mm", Locale.getDefault()).format(Date(createdAt))
        else "--"

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
                // Posmatrač se ne dodaje u active_rescuers
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
                            Toast.makeText(this, t("Greska. Pokusajte ponovo.", "Error. Try again.", "Ошибка.", "Fehler."), Toast.LENGTH_SHORT).show()
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

        // Učitaj broj spasilaca
        FirebaseDatabase.getInstance().getReference("active_rescuers").child(code).get()
            .addOnSuccessListener { snapshot ->
                val count = snapshot.childrenCount.toInt()
                runOnUiThread {
                    rescuerCountText.text = t(
                        "Spasilaca u akciji: $count",
                        "Rescuers in mission: $count",
                        "Спасателей в операции: $count",
                        "Retter im Einsatz: $count"
                    )
                }
            }
    }

    private fun createMission(missionCode: String, missionName: String, rescuerName: String, callback: (Boolean) -> Unit) {
        val db = FirebaseDatabase.getInstance()
        val missionData = mapOf(
            "name" to missionName,
            "code" to missionCode,
            "coordinator" to rescuerName,
            "createdAt" to System.currentTimeMillis(),
            "active" to true
        )

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

    override fun onDestroy() {
        super.onDestroy()
        missionsListener?.let {
            FirebaseDatabase.getInstance().getReference("missions").removeEventListener(it)
        }
    }

    private fun currentLanguage(): String {
        return getSharedPreferences("savio_prefs", MODE_PRIVATE).getString("language", "sr") ?: "sr"
    }

    private fun t(sr: String, en: String, ru: String, de: String): String {
        return when (currentLanguage()) {
            "en" -> en; "ru" -> ru; "de" -> de; else -> sr
        }
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
