package com.example.savio4

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.database.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.*

class CivilSearchActivity : AppCompatActivity(), LocationListener {

    private lateinit var mapView: MapView
    private lateinit var statusText: TextView
    private lateinit var distanceText: TextView
    private var myMarker: Marker? = null
    private var victimMarker: Marker? = null
    private var routeLine: Polyline? = null
    private var victimLat = 0.0
    private var victimLon = 0.0
    private var mapCenteredOnce = false
    private val handler = Handler(Looper.getMainLooper())
    private val smsPermissionCode = 401

    // Civil podaci
    private var civilName = ""
    private var civilPhone = ""
    private var missionCode = ""
    private var isInMission = false

    // Firebase
    private lateinit var db: FirebaseDatabase
    private val civilMarkers = mutableMapOf<String, Marker>()
    private var civiliListener: ValueEventListener? = null

    // Koordinator kontakt
    private val coordinatorPhone = "+381652013323"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = packageName

        db = FirebaseDatabase.getInstance()

        val mainLayout = LinearLayout(this)
        mainLayout.orientation = LinearLayout.VERTICAL
        mainLayout.setBackgroundColor(Color.rgb(10, 12, 16))

        // ─── HEADER ───
        val header = LinearLayout(this)
        header.orientation = LinearLayout.VERTICAL
        header.setPadding(24, 40, 24, 12)

        val title = TextView(this)
        title.text = "🆘 " + t("POMOZI U POTRAZI", "HELP IN SEARCH", "ПОМОЧЬ В ПОИСКЕ", "SUCHE HELFEN")
        title.textSize = 22f
        title.setTextColor(Color.rgb(220, 100, 0))
        title.typeface = Typeface.DEFAULT_BOLD

        statusText = TextView(this)
        statusText.text = t("Unesite vaše podatke za početak.", "Enter your details to start.", "Введите данные для начала.", "Daten eingeben zum Starten.")
        statusText.textSize = 13f
        statusText.setTextColor(Color.rgb(180, 180, 180))

        distanceText = TextView(this)
        distanceText.text = ""
        distanceText.textSize = 14f
        distanceText.setTextColor(Color.rgb(255, 140, 0))
        distanceText.typeface = Typeface.DEFAULT_BOLD

        val legendText = TextView(this)
        legendText.text = t(
            "🔵 Spasioci   🟡 Civili   🔴 Nestalo lice   ⭐ Vi",
            "🔵 Rescuers   🟡 Civilians   🔴 Missing   ⭐ You",
            "🔵 Спасатели   🟡 Гражданские   🔴 Пострадавший   ⭐ Вы",
            "🔵 Retter   🟡 Zivilisten   🔴 Vermisste   ⭐ Sie"
        )
        legendText.textSize = 11f
        legendText.setTextColor(Color.rgb(150, 150, 150))

        header.addView(title)
        header.addView(statusText)
        header.addView(distanceText)
        header.addView(legendText)

        // ─── MAPA ───
        mapView = MapView(this)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(7.0)
        mapView.controller.setCenter(GeoPoint(44.0, 21.0))
        val mapParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        mapView.layoutParams = mapParams

        // ─── DUGMAD ───
        val buttonsLayout = LinearLayout(this)
        buttonsLayout.orientation = LinearLayout.VERTICAL
        buttonsLayout.setPadding(24, 8, 24, 16)

        val btnJoin = Button(this)
        btnJoin.text = "👤 " + t(
            "UNESITE PODATKE I PRIDRUŽITE SE POTRAZI",
            "ENTER DETAILS AND JOIN SEARCH",
            "ВВЕСТИ ДАННЫЕ И ПРИСОЕДИНИТЬСЯ К ПОИСКУ",
            "DATEN EINGEBEN UND SUCHE BEITRETEN"
        )
        btnJoin.setTextColor(Color.WHITE)
        btnJoin.setBackgroundColor(Color.rgb(150, 80, 0))
        btnJoin.textSize = 14f
        val joinParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        joinParams.setMargins(0, 0, 0, 8)
        btnJoin.layoutParams = joinParams
        btnJoin.setOnClickListener { showJoinDialog() }

        val btnSetVictim = Button(this)
        btnSetVictim.text = "📍 " + t(
            "UNESI LOKACIJU NESTALNOG LICA",
            "ENTER MISSING PERSON LOCATION",
            "ВВЕСТИ МЕСТОПОЛОЖЕНИЕ ПОСТРАДАВШЕГО",
            "STANDORT EINGEBEN"
        )
        btnSetVictim.setTextColor(Color.WHITE)
        btnSetVictim.setBackgroundColor(Color.rgb(180, 0, 0))
        btnSetVictim.textSize = 14f
        val victimBtnParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        victimBtnParams.setMargins(0, 0, 0, 8)
        btnSetVictim.layoutParams = victimBtnParams
        btnSetVictim.setOnClickListener { showVictimLocationDialog() }

        val btnFindSms = Button(this)
        btnFindSms.text = "📱 " + t(
            "UČITAJ IZ SOS PORUKE",
            "LOAD FROM SOS MESSAGE",
            "ЗАГРУЗИТЬ ИЗ SOS-СООБЩЕНИЯ",
            "AUS SOS-NACHRICHT LADEN"
        )
        btnFindSms.setTextColor(Color.WHITE)
        btnFindSms.setBackgroundColor(Color.rgb(0, 100, 60))
        btnFindSms.textSize = 14f
        val smsBtnParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        smsBtnParams.setMargins(0, 0, 0, 8)
        btnFindSms.layoutParams = smsBtnParams
        btnFindSms.setOnClickListener { findSosFromSms() }

        val btnBack = Button(this)
        btnBack.text = t("← NAZAD", "← BACK", "← НАЗАД", "← ZURÜCK")
        btnBack.setTextColor(Color.WHITE)
        btnBack.setBackgroundColor(Color.rgb(50, 50, 70))
        btnBack.textSize = 14f
        btnBack.setOnClickListener {
            if (isInMission) leaveMission()
            finish()
        }

        buttonsLayout.addView(btnJoin)
        buttonsLayout.addView(btnSetVictim)
        buttonsLayout.addView(btnFindSms)
        buttonsLayout.addView(btnBack)

        mainLayout.addView(header)
        mainLayout.addView(mapView)
        mainLayout.addView(buttonsLayout)

        setContentView(mainLayout)
        applyWindowInsets()
        startLocationUpdates()

        // Odmah prikaži dialog za prijavu
        handler.postDelayed({ showJoinDialog() }, 500)
    }

    // ─────────────────────────────────────────────
    // PRIJAVA CIVILA
    // ─────────────────────────────────────────────

    private fun showJoinDialog() {
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(32, 16, 32, 16)

        // Kontakt info
        val contactBox = TextView(this)
        contactBox.text = "ℹ️ " + t(
            "Da biste učestvovali u zvaničnoj potrazi i bili vidljivi spasiocima, potreban vam je KOD POTRAGE.\n\nKod dobijate kontaktiranjem koordinatora:\n📞 $coordinatorPhone\n\nAko nemate kod, možete koristiti aplikaciju samo za praćenje vaše lokacije.",
            "To participate in the official search and be visible to rescuers, you need a SEARCH CODE.\n\nGet the code by contacting the coordinator:\n📞 $coordinatorPhone\n\nWithout a code, you can use the app only to track your location.",
            "Для участия в официальном поиске вам нужен КОД ОПЕРАЦИИ.\n\nПолучите код, связавшись с координатором:\n📞 $coordinatorPhone",
            "Für die offizielle Suche benötigen Sie einen SUCHCODE.\n\nCode erhalten Sie vom Koordinator:\n📞 $coordinatorPhone"
        )
        contactBox.textSize = 13f
        contactBox.setTextColor(Color.WHITE)
        contactBox.setPadding(16, 16, 16, 16)
        val contactBg = GradientDrawable()
        contactBg.setColor(Color.rgb(0, 40, 70))
        contactBg.cornerRadius = 12f
        contactBg.setStroke(1, Color.rgb(0, 100, 180))
        contactBox.background = contactBg
        val contactParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        contactParams.setMargins(0, 0, 0, 16)
        contactBox.layoutParams = contactParams

        val nameInput = EditText(this)
        nameInput.hint = t("Vaše ime (npr. Marko)", "Your name (e.g. Marko)", "Ваше имя (напр. Марко)", "Ihr Name (z.B. Markus)")
        nameInput.setTextColor(Color.WHITE)
        nameInput.setHintTextColor(Color.rgb(120, 120, 120))
        nameInput.textSize = 16f
        nameInput.setText(civilName)
        val nameParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        nameParams.setMargins(0, 0, 0, 12)
        nameInput.layoutParams = nameParams

        val phoneInput = EditText(this)
        phoneInput.hint = t("Vaš broj telefona", "Your phone number", "Ваш номер телефона", "Ihre Telefonnummer")
        phoneInput.setTextColor(Color.WHITE)
        phoneInput.setHintTextColor(Color.rgb(120, 120, 120))
        phoneInput.inputType = android.text.InputType.TYPE_CLASS_PHONE
        phoneInput.textSize = 16f
        phoneInput.setText(civilPhone)
        val phoneParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        phoneParams.setMargins(0, 0, 0, 12)
        phoneInput.layoutParams = phoneParams

        val codeInput = EditText(this)
        codeInput.hint = t("Kod potrage (npr. SAVIO-20260625-1000)", "Search code (e.g. SAVIO-20260625-1000)", "Код операции (напр. SAVIO-20260625-1000)", "Suchcode (z.B. SAVIO-20260625-1000)")
        codeInput.setTextColor(Color.WHITE)
        codeInput.setHintTextColor(Color.rgb(120, 120, 120))
        codeInput.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
        codeInput.textSize = 16f
        codeInput.setText(missionCode)
        val codeParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        codeParams.setMargins(0, 0, 0, 8)
        codeInput.layoutParams = codeParams

        val codeNote = TextView(this)
        codeNote.text = t(
            "Bez koda možete koristiti samo localnu mapu bez vidljivosti spasiocima.",
            "Without a code you can only use the local map without visibility to rescuers.",
            "Без кода вы можете использовать только локальную карту.",
            "Ohne Code nur lokale Karte ohne Sichtbarkeit für Retter."
        )
        codeNote.textSize = 11f
        codeNote.setTextColor(Color.rgb(150, 150, 150))
        codeNote.setPadding(0, 0, 0, 8)

        // Dugme za poziv koordinatoru
        val btnCallCoord = Button(this)
        btnCallCoord.text = "📞 " + t("POZOVI KOORDINATORA", "CALL COORDINATOR", "ПОЗВОНИТЬ КООРДИНАТОРУ", "KOORDINATOR ANRUFEN")
        btnCallCoord.setTextColor(Color.WHITE)
        btnCallCoord.setBackgroundColor(Color.rgb(0, 120, 60))
        btnCallCoord.textSize = 13f
        val callParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        callParams.setMargins(0, 0, 0, 8)
        btnCallCoord.layoutParams = callParams
        btnCallCoord.setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_DIAL, android.net.Uri.parse("tel:$coordinatorPhone")))
            } catch (_: Exception) {}
        }

        layout.addView(contactBox)
        layout.addView(nameInput)
        layout.addView(phoneInput)
        layout.addView(codeInput)
        layout.addView(codeNote)
        layout.addView(btnCallCoord)

        val scrollView = ScrollView(this)
        scrollView.addView(layout)

        AlertDialog.Builder(this)
            .setTitle("👤 " + t("VAŠI PODACI", "YOUR DETAILS", "ВАШИ ДАННЫЕ", "IHRE DATEN"))
            .setView(scrollView)
            .setPositiveButton(t("POTVRDI", "CONFIRM", "ПОДТВЕРДИТЬ", "BESTÄTIGEN")) { _, _ ->
                val name = nameInput.text.toString().trim()
                val phone = phoneInput.text.toString().trim()
                val code = codeInput.text.toString().trim().uppercase()

                if (name.isEmpty()) {
                    Toast.makeText(this, t("Unesite vaše ime.", "Enter your name.", "Введите имя.", "Namen eingeben."), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                civilName = name
                civilPhone = phone

                if (code.isNotEmpty()) {
                    joinMissionWithCode(code)
                } else {
                    statusText.text = "👤 $name — " + t(
                        "Lokalna mapa (bez koda potrage)",
                        "Local map (no search code)",
                        "Локальная карта (без кода)",
                        "Lokale Karte (kein Code)"
                    )
                    statusText.setTextColor(Color.rgb(180, 180, 180))
                    startLocationUpdates()
                }
            }
            .setNegativeButton(t("PRESKOCI", "SKIP", "ПРОПУСТИТЬ", "ÜBERSPRINGEN"), null)
            .show()
    }

    private fun joinMissionWithCode(code: String) {
        db.getReference("missions").child(code).get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                Toast.makeText(this, t(
                    "Potraga sa kodom \"$code\" nije pronađena.",
                    "Search with code \"$code\" not found.",
                    "Операция с кодом \"$code\" не найдена.",
                    "Suche mit Code \"$code\" nicht gefunden."
                ), Toast.LENGTH_LONG).show()
                return@addOnSuccessListener
            }

            val active = snapshot.child("active").getValue(Boolean::class.java) ?: false
            if (!active) {
                Toast.makeText(this, t(
                    "Ova potraga je završena.",
                    "This search has ended.",
                    "Эта операция завершена.",
                    "Diese Suche ist beendet."
                ), Toast.LENGTH_LONG).show()
                return@addOnSuccessListener
            }

            val missionName = snapshot.child("name").getValue(String::class.java) ?: code
            missionCode = code
            isInMission = true

            // Upiši civila u Firebase
            val safeName = civilName.replace(".", "_").replace("#", "_").replace("$", "_").replace("[", "_").replace("]", "_")
            val civilData = mapOf(
                "name" to civilName,
                "phone" to civilPhone,
                "lat" to 0.0,
                "lon" to 0.0,
                "lastUpdate" to System.currentTimeMillis(),
                "lastUpdateTime" to "--:--"
            )
            db.getReference("civil_rescuers").child(missionCode).child(safeName).setValue(civilData)

            statusText.text = "⭐ $civilName — " + t(
                "Pridružen potrazi: $missionName",
                "Joined search: $missionName",
                "Присоединился к операции: $missionName",
                "Suche beigetreten: $missionName"
            )
            statusText.setTextColor(Color.rgb(255, 200, 0))

            // Počni slušati ostale civile
            startListeningCivilians()

            // Učitaj koordinate nestalnog lica iz Firebase
            loadVictimLocation()

            Toast.makeText(this, t(
                "Uspešno pridružen potrazi! Vidljivi ste spasiocima.",
                "Successfully joined search! You are visible to rescuers.",
                "Успешно присоединились! Вы видны спасателям.",
                "Erfolgreich beigetreten! Sie sind für Retter sichtbar."
            ), Toast.LENGTH_LONG).show()

        }.addOnFailureListener {
            Toast.makeText(this, t("Greška. Proverite internet.", "Error. Check internet.", "Ошибка.", "Fehler."), Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadVictimLocation() {
        db.getReference("found_persons").child(missionCode).get().addOnSuccessListener { snapshot ->
            snapshot.children.forEach { findSnapshot ->
                val lat = findSnapshot.child("lat").getValue(Double::class.java) ?: 0.0
                val lon = findSnapshot.child("lon").getValue(Double::class.java) ?: 0.0
                val status = findSnapshot.child("status").getValue(String::class.java) ?: ""
                if (lat != 0.0 && lon != 0.0) {
                    runOnUiThread { setVictimMarker(lat, lon) }
                    return@addOnSuccessListener
                }
            }
        }
    }

    private fun leaveMission() {
        if (missionCode.isEmpty()) return
        val safeName = civilName.replace(".", "_").replace("#", "_").replace("$", "_").replace("[", "_").replace("]", "_")
        db.getReference("civil_rescuers").child(missionCode).child(safeName).removeValue()
        isInMission = false
    }

    private fun startListeningCivilians() {
        if (missionCode.isEmpty()) return
        val ref = db.getReference("civil_rescuers").child(missionCode)
        civiliListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Ukloni stare markere civila (osim mog)
                civilMarkers.forEach { (name, marker) ->
                    if (name != civilName) mapView.overlays.remove(marker)
                }
                civilMarkers.clear()

                snapshot.children.forEach { civilSnapshot ->
                    val name = civilSnapshot.child("name").getValue(String::class.java) ?: return@forEach
                    val lat = civilSnapshot.child("lat").getValue(Double::class.java) ?: 0.0
                    val lon = civilSnapshot.child("lon").getValue(Double::class.java) ?: 0.0
                    val phone = civilSnapshot.child("phone").getValue(String::class.java) ?: ""
                    val lastUpdateTime = civilSnapshot.child("lastUpdateTime").getValue(String::class.java) ?: "--:--"

                    if (name != civilName && lat != 0.0 && lon != 0.0) {
                        addCivilMarker(name, lat, lon, phone, lastUpdateTime)
                    }
                }
                mapView.invalidate()
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(civiliListener!!)
    }

    private fun addCivilMarker(name: String, lat: Double, lon: Double, phone: String, lastUpdateTime: String) {
        val position = GeoPoint(lat, lon)
        val existing = civilMarkers[name]
        if (existing != null) {
            existing.position = position
            return
        }

        val marker = Marker(mapView)
        marker.position = position
        marker.title = "🟡 $name"
        marker.icon = makeCircleBitmap(Color.rgb(220, 180, 0), name.firstOrNull()?.uppercaseChar()?.toString() ?: "C")
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        marker.setOnMarkerClickListener { _, _ ->
            showCivilInfo(name, lat, lon, phone, lastUpdateTime)
            true
        }
        mapView.overlays.add(marker)
        civilMarkers[name] = marker
    }

    private fun showCivilInfo(name: String, lat: Double, lon: Double, phone: String, lastUpdateTime: String) {
        val phoneDisplay = if (phone.isNotEmpty()) phone
        else t("Broj nije dostupan", "Number not available", "Номер недоступен", "Nummer nicht verfügbar")

        val builder = AlertDialog.Builder(this)
            .setTitle("🟡 $name")
            .setMessage(t(
                "Koordinate:\n${"%.5f".format(lat)}, ${"%.5f".format(lon)}\n\nAžurirano: $lastUpdateTime\n\nTelefon: $phoneDisplay",
                "Coordinates:\n${"%.5f".format(lat)}, ${"%.5f".format(lon)}\n\nUpdated: $lastUpdateTime\n\nPhone: $phoneDisplay",
                "Координаты:\n${"%.5f".format(lat)}, ${"%.5f".format(lon)}\n\nОбновлено: $lastUpdateTime\n\nТелефон: $phoneDisplay",
                "Koordinaten:\n${"%.5f".format(lat)}, ${"%.5f".format(lon)}\n\nAktualisiert: $lastUpdateTime\n\nTelefon: $phoneDisplay"
            ))
            .setPositiveButton(t("ZATVORI", "CLOSE", "ЗАКРЫТЬ", "SCHLIESSEN"), null)

        if (phone.isNotEmpty()) {
            builder.setNeutralButton("📞 " + t("POZOVI", "CALL", "ПОЗВОНИТЬ", "ANRUFEN")) { _, _ ->
                try {
                    startActivity(Intent(Intent.ACTION_DIAL, android.net.Uri.parse("tel:$phone")))
                } catch (_: Exception) {}
            }
        }
        builder.show()
    }

    // ─────────────────────────────────────────────
    // LOKACIJA
    // ─────────────────────────────────────────────

    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 201)
            return
        }
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000L, 3f, this)
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000L, 3f, this)
        } catch (_: Exception) {}
    }

    override fun onLocationChanged(location: Location) {
        updateMyMarker(location.latitude, location.longitude)
        updateDistanceAndLine(location.latitude, location.longitude)

        if (!mapCenteredOnce) {
            mapCenteredOnce = true
            mapView.controller.animateTo(GeoPoint(location.latitude, location.longitude))
            mapView.controller.setZoom(14.0)
        }

        // Ažuriraj lokaciju u Firebase ako je u potrazi
        if (isInMission && civilName.isNotEmpty()) {
            updateCivilLocationInFirebase(location)
        }
    }

    private fun updateCivilLocationInFirebase(location: Location) {
        val safeName = civilName.replace(".", "_").replace("#", "_").replace("$", "_").replace("[", "_").replace("]", "_")
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        db.getReference("civil_rescuers").child(missionCode).child(safeName)
            .updateChildren(mapOf(
                "lat" to location.latitude,
                "lon" to location.longitude,
                "lastUpdate" to System.currentTimeMillis(),
                "lastUpdateTime" to time
            ))
    }

    private fun updateMyMarker(lat: Double, lon: Double) {
        val position = GeoPoint(lat, lon)
        if (myMarker != null) {
            myMarker!!.position = position
            mapView.invalidate()
        } else {
            val marker = Marker(mapView)
            marker.position = position
            marker.title = if (civilName.isNotEmpty()) "⭐ $civilName" else t("⭐ Vi", "⭐ You", "⭐ Вы", "⭐ Sie")
            marker.icon = makeCircleBitmap(Color.rgb(255, 200, 0), if (civilName.isNotEmpty()) civilName.first().uppercaseChar().toString() else "V")
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            mapView.overlays.add(marker)
            myMarker = marker
            mapView.invalidate()
        }
    }

    private fun setVictimMarker(lat: Double, lon: Double) {
        victimLat = lat
        victimLon = lon
        val position = GeoPoint(lat, lon)

        if (victimMarker != null) {
            victimMarker!!.position = position
            mapView.invalidate()
        } else {
            val marker = Marker(mapView)
            marker.position = position
            marker.title = t("🔴 Nestalo lice", "🔴 Missing person", "🔴 Пострадавший", "🔴 Vermisste Person")
            marker.icon = makeCircleBitmap(Color.rgb(200, 0, 0), "!")
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            mapView.overlays.add(marker)
            victimMarker = marker
            mapView.invalidate()
        }
        mapView.controller.animateTo(position)
        mapView.controller.setZoom(13.0)
    }

    private fun updateDistanceAndLine(myLat: Double, myLon: Double) {
        if (victimLat == 0.0 && victimLon == 0.0) return

        val distance = calculateDistance(myLat, myLon, victimLat, victimLon)
        val distanceStr = if (distance >= 1000) "${"%.1f".format(distance / 1000)} km" else "${distance.toInt()} m"

        distanceText.text = "📏 " + t(
            "Udaljenost od nestalnog lica: $distanceStr",
            "Distance to missing person: $distanceStr",
            "Расстояние до пострадавшего: $distanceStr",
            "Entfernung zur vermissten Person: $distanceStr"
        )

        routeLine?.let { mapView.overlays.remove(it) }
        val line = Polyline()
        line.setPoints(listOf(GeoPoint(myLat, myLon), GeoPoint(victimLat, victimLon)))
        line.color = Color.argb(180, 255, 100, 0)
        line.width = 4f
        mapView.overlays.add(0, line)
        routeLine = line
        mapView.invalidate()
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    // ─────────────────────────────────────────────
    // LOKACIJA NESTALNOG LICA
    // ─────────────────────────────────────────────

    private fun showVictimLocationDialog() {
        val input = EditText(this)
        input.hint = t(
            "Nalepite koordinate ili Google Maps link\nnpr. 43.9081, 22.2874",
            "Paste coordinates or Google Maps link\ne.g. 43.9081, 22.2874",
            "Вставьте координаты или ссылку\nнапр. 43.9081, 22.2874",
            "Koordinaten oder Link einfügen\nz.B. 43.9081, 22.2874"
        )
        input.minLines = 3
        input.setTextColor(Color.WHITE)
        input.setHintTextColor(Color.rgb(120, 120, 120))
        input.setPadding(16, 16, 16, 16)

        AlertDialog.Builder(this)
            .setTitle("📍 " + t("LOKACIJA NESTALNOG LICA", "MISSING PERSON LOCATION", "МЕСТОПОЛОЖЕНИЕ ПОСТРАДАВШЕГО", "STANDORT"))
            .setView(input)
            .setPositiveButton(t("POSTAVI NA MAPU", "SET ON MAP", "ПОСТАВИТЬ НА КАРТУ", "AUF KARTE SETZEN")) { _, _ ->
                val coords = extractCoordinates(input.text.toString())
                if (coords != null) {
                    setVictimMarker(coords.first, coords.second)
                    Toast.makeText(this, t("Lokacija postavljena!", "Location set!", "Местоположение установлено!", "Standort gesetzt!"), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, t("Nisam pronašao koordinate.", "Could not find coordinates.", "Координаты не найдены.", "Koordinaten nicht gefunden."), Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton(t("ODUSTANI", "CANCEL", "ОТМЕНА", "ABBRECHEN"), null)
            .show()
    }

    private fun findSosFromSms() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_SMS), smsPermissionCode)
            return
        }

        val cursor: Cursor? = contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            arrayOf(Telephony.Sms.BODY, Telephony.Sms.DATE),
            null, null, Telephony.Sms.DATE + " DESC"
        )

        cursor?.use {
            var checked = 0
            while (it.moveToNext() && checked < 80) {
                checked++
                val body = it.getString(0) ?: continue
                if (body.contains("SOS", ignoreCase = true) || body.contains("maps.google.com", ignoreCase = true) || body.contains("Koordinate", ignoreCase = true)) {
                    val coords = extractCoordinates(body)
                    if (coords != null) {
                        setVictimMarker(coords.first, coords.second)
                        Toast.makeText(this, t("SOS lokacija učitana!", "SOS location loaded!", "SOS-локация загружена!", "SOS-Standort geladen!"), Toast.LENGTH_LONG).show()
                        return
                    }
                }
            }
        }

        Toast.makeText(this, t("Nisam pronašao SOS poruku.", "Could not find SOS message.", "SOS-сообщение не найдено.", "SOS-Nachricht nicht gefunden."), Toast.LENGTH_LONG).show()
    }

    private fun extractCoordinates(text: String): Pair<Double, Double>? {
        val regex = Regex("""(-?\d{1,3}\.\d+)\s*,\s*(-?\d{1,3}\.\d+)""")
        val match = regex.find(text) ?: return null
        val lat = match.groupValues[1].toDoubleOrNull() ?: return null
        val lon = match.groupValues[2].toDoubleOrNull() ?: return null
        if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return null
        return Pair(lat, lon)
    }

    // ─────────────────────────────────────────────
    // MARKERI
    // ─────────────────────────────────────────────

    private fun makeCircleBitmap(color: Int, initial: String): BitmapDrawable {
        val size = 80
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.color = color
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4, paint)

        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4, paint)

        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        paint.textSize = 30f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(initial, size / 2f, size / 2f + 10f, paint)

        return BitmapDrawable(resources, bitmap)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            201 -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) startLocationUpdates()
            smsPermissionCode -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) findSosFromSms()
        }
    }

    private fun applyWindowInsets() {
        val rootView = window.decorView.findViewById<View>(android.R.id.content)
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }
    }

    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { super.onPause(); mapView.onPause() }
    override fun onDestroy() {
        super.onDestroy()
        civiliListener?.let {
            if (missionCode.isNotEmpty()) db.getReference("civil_rescuers").child(missionCode).removeEventListener(it)
        }
        if (isInMission) leaveMission()
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationManager.removeUpdates(this)
        mapView.onDetach()
    }

    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
    @Deprecated("Deprecated in Java") override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

    private fun currentLanguage(): String {
        return getSharedPreferences("savio_prefs", MODE_PRIVATE).getString("language", "sr") ?: "sr"
    }

    private fun t(sr: String, en: String, ru: String, de: String): String {
        return when (currentLanguage()) { "en" -> en; "ru" -> ru; "de" -> de; else -> sr }
    }
}
