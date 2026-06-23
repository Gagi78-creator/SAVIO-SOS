package com.example.savio4

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Telephony
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlin.math.abs

class RescueActivity : AppCompatActivity() {

    private val smsPermissionCode = 301
    private lateinit var status: TextView
    private lateinit var btnTopoMap: Button
    private lateinit var btnOsmMap: Button
    private var lastLat = 0.0
    private var lastLon = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scrollView = ScrollView(this)

        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(32, 50, 32, 32)
        container.setBackgroundColor(Color.rgb(10, 12, 16))

        val title = TextView(this)
        title.text = t(
            "MOD ZA SPASIOCE",
            "RESCUE MODE",
            "РЕЖИМ СПАСАТЕЛЯ",
            "RETTUNGSMODUS"
        )
        title.textSize = 28f
        title.setTextColor(Color.WHITE)

        val info = TextView(this)
        info.text = t(
            "Aplikacija pokušava da pronađe poslednju SOS poruku i automatski otvori Google Maps.",
            "The application tries to find the latest SOS message and automatically open Google Maps.",
            "Приложение пытается найти последнее SOS-сообщение и автоматически открыть Google Maps.",
            "Die Anwendung versucht, die letzte SOS-Nachricht zu finden und automatisch Google Maps zu öffnen."
        )
        info.textSize = 16f
        info.setTextColor(Color.WHITE)
        info.setPadding(0, 0, 0, 16)

        val btnFind = Button(this)
        btnFind.text = t(
            "PRONAĐI POSLEDNJU SOS LOKACIJU",
            "FIND LATEST SOS LOCATION",
            "НАЙТИ ПОСЛЕДНЮЮ SOS-ЛОКАЦИЮ",
            "LETZTEN SOS-STANDORT FINDEN"
        )

        val manualInput = EditText(this)
        manualInput.hint = t(
            "Rezerva: nalepite SOS poruku ili koordinate",
            "Backup: paste SOS message or coordinates",
            "Резервно: вставьте SOS-сообщение или координаты",
            "Reserve: SOS-Nachricht oder Koordinaten einfügen"
        )
        manualInput.minLines = 4
        manualInput.setTextColor(Color.WHITE)
        manualInput.setHintTextColor(Color.LTGRAY)

        val btnManual = Button(this)
        btnManual.text = t(
            "OTVORI RUČNO UNEŠENE KOORDINATE",
            "OPEN MANUALLY ENTERED COORDINATES",
            "ОТКРЫТЬ ВВЕДЁННЫЕ ВРУЧНУЮ КООРДИНАТЫ",
            "MANUELL EINGEGEBENE KOORDINATEN ÖFFNEN"
        )

        status = TextView(this)
        status.text = ""
        status.textSize = 16f
        status.setTextColor(Color.WHITE)
        status.setPadding(0, 8, 0, 24)

        // ─────────────────────────────────────────────
        // OFFLINE KARTE — SEKCIJA
        // ─────────────────────────────────────────────

        val offlineTitle = TextView(this)
        offlineTitle.text = t(
            "🗺️ OFFLINE KARTE ZA SPASIOCE",
            "🗺️ OFFLINE MAPS FOR RESCUERS",
            "🗺️ ОФЛАЙН-КАРТЫ ДЛЯ СПАСАТЕЛЕЙ",
            "🗺️ OFFLINE-KARTEN FÜR RETTER"
        )
        offlineTitle.textSize = 20f
        offlineTitle.setTextColor(Color.rgb(255, 200, 0))
        offlineTitle.setPadding(0, 16, 0, 8)

        val offlineInfo = TextView(this)
        offlineInfo.text = t(
            "⚠️ Google Maps zahteva internet vezu. Na nepristupačnom terenu koristite offline karte koje rade BEZ interneta.",
            "⚠️ Google Maps requires an internet connection. In remote areas, use offline maps that work WITHOUT internet.",
            "⚠️ Google Maps требует подключения к интернету. В труднодоступных районах используйте офлайн-карты, которые работают БЕЗ интернета.",
            "⚠️ Google Maps erfordert eine Internetverbindung. In unzugänglichem Gelände verwenden Sie Offline-Karten, die OHNE Internet funktionieren."
        )
        offlineInfo.textSize = 14f
        offlineInfo.setTextColor(Color.rgb(200, 200, 200))
        offlineInfo.setPadding(0, 0, 0, 16)

        // OsmAnd
        val osmandCard = buildAppCard(
            name = "OsmAnd",
            description = t(
                "Besplatna. Odlična za planine i šume. Podržava GPS bez interneta. Preporučena za GSS.",
                "Free. Excellent for mountains and forests. Supports GPS without internet. Recommended for mountain rescue.",
                "Бесплатная. Отлично подходит для гор и лесов. Поддерживает GPS без интернета.",
                "Kostenlos. Hervorragend für Berge und Wälder. Unterstützt GPS ohne Internet."
            ),
            packageName = "net.osmand",
            color = Color.rgb(0, 100, 60)
        )

        // Locus Map
        val locusCard = buildAppCard(
            name = "Locus Map",
            description = t(
                "Profesionalna navigacija. Koriste je spasilačke službe. Podržava offline karte i tracking.",
                "Professional navigation. Used by rescue services. Supports offline maps and tracking.",
                "Профессиональная навигация. Используется спасательными службами. Поддерживает офлайн-карты.",
                "Professionelle Navigation. Wird von Rettungsdiensten genutzt. Unterstützt Offline-Karten."
            ),
            packageName = "menion.android.locus",
            color = Color.rgb(0, 80, 160)
        )

        // ComAps / Maps.me
        val mapsmeCard = buildAppCard(
            name = "MAPS.ME",
            description = t(
                "Jednostavna i brza. Radi potpuno offline. Dobra za brzu orijentaciju na terenu.",
                "Simple and fast. Works completely offline. Good for quick field orientation.",
                "Простая и быстрая. Работает полностью офлайн. Хороша для быстрой ориентации на местности.",
                "Einfach und schnell. Funktioniert komplett offline. Gut für schnelle Geländeorientierung."
            ),
            packageName = "com.mapswithme.maps.pro",
            color = Color.rgb(100, 50, 0)
        )

        // ─────────────────────────────────────────────
        // LISTENERS
        // ─────────────────────────────────────────────

        btnFind.setOnClickListener {
            if (hasSmsPermission()) {
                findLatestSosSmsAndNavigate()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_SMS),
                    smsPermissionCode
                )
            }
        }

        // OpenTopoMap dugme — vidljivo nakon pronalaska koordinata
        btnTopoMap = Button(this)
        btnTopoMap.text = t(
            "OTVORI TOPOGRAFSKU KARTU",
            "OPEN TOPOGRAPHIC MAP",
            "ОТКРЫТЬ ТОПОГРАФИЧЕСКУЮ КАРТУ",
            "TOPOGRAPHISCHE KARTE ÖFFNEN"
        )
        btnTopoMap.setTextColor(Color.WHITE)
        btnTopoMap.setBackgroundColor(Color.rgb(80, 50, 0))
        btnTopoMap.visibility = android.view.View.GONE

        btnOsmMap = Button(this)
        btnOsmMap.text = t(
            "OTVORI KARTU SA MARKEROM (OSM)",
            "OPEN MAP WITH MARKER (OSM)",
            "ОТКРЫТЬ КАРТУ С МАРКЕРОМ (OSM)",
            "KARTE MIT MARKER ÖFFNEN (OSM)"
        )
        btnOsmMap.setTextColor(Color.WHITE)
        btnOsmMap.setBackgroundColor(Color.rgb(0, 80, 120))
        btnOsmMap.visibility = android.view.View.GONE

        btnManual.setOnClickListener {
            val coordinates = extractCoordinates(manualInput.text.toString())
            if (coordinates == null) {
                status.text = t(
                    "Nisam pronašao koordinate.",
                    "I could not find coordinates.",
                    "Координаты не найдены.",
                    "Keine Koordinaten gefunden."
                )
                btnTopoMap.visibility = android.view.View.GONE
                btnOsmMap.visibility = android.view.View.GONE
            } else {
                lastLat = coordinates.first
                lastLon = coordinates.second
                status.text = buildCoordinateDisplayText(lastLat, lastLon)
                btnTopoMap.visibility = android.view.View.VISIBLE
                btnOsmMap.visibility = android.view.View.VISIBLE
                openGoogleMaps(lastLat, lastLon)
            }
        }

        btnTopoMap.setOnClickListener {
            openTopoMap(lastLat, lastLon)
        }

        btnOsmMap.setOnClickListener {
            openOsmMap(lastLat, lastLon)
        }

        container.addView(title)
        container.addView(info)
        container.addView(btnFind)
        container.addView(manualInput)
        container.addView(btnManual)
        container.addView(status)
        container.addView(btnTopoMap)
        container.addView(btnOsmMap)
        container.addView(offlineTitle)
        container.addView(offlineInfo)
        container.addView(osmandCard)
        container.addView(locusCard)
        container.addView(mapsmeCard)

        scrollView.addView(container)
        setContentView(scrollView)
    }

    // ─────────────────────────────────────────────
    // OFFLINE KARTE — CARD BUILDER
    // ─────────────────────────────────────────────

    private fun buildAppCard(
        name: String,
        description: String,
        packageName: String,
        color: Int
    ): LinearLayout {
        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.setPadding(24, 24, 24, 24)
        card.setBackgroundColor(Color.rgb(20, 25, 32))

        val cardBg = GradientDrawable()
        cardBg.setColor(Color.rgb(20, 25, 32))
        cardBg.cornerRadius = 16f
        cardBg.setStroke(2, color)
        card.background = cardBg

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, 0, 0, 16)
        card.layoutParams = params

        val appName = TextView(this)
        appName.text = name
        appName.textSize = 18f
        appName.setTextColor(color)

        val appDesc = TextView(this)
        appDesc.text = description
        appDesc.textSize = 14f
        appDesc.setTextColor(Color.rgb(200, 200, 200))
        appDesc.setPadding(0, 8, 0, 12)

        val btnInstall = Button(this)
        btnInstall.text = t(
            "OTVORI U GOOGLE PLAY",
            "OPEN IN GOOGLE PLAY",
            "ОТКРЫТЬ В GOOGLE PLAY",
            "IM GOOGLE PLAY ÖFFNEN"
        )
        btnInstall.setTextColor(Color.WHITE)
        btnInstall.setBackgroundColor(color)

        btnInstall.setOnClickListener {
            openPlayStore(packageName)
        }

        card.addView(appName)
        card.addView(appDesc)
        card.addView(btnInstall)

        return card
    }

    private fun openPlayStore(packageName: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
        } catch (e: Exception) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
        }
    }

    // ─────────────────────────────────────────────
    // SMS I NAVIGACIJA
    // ─────────────────────────────────────────────

    private fun hasSmsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun findLatestSosSmsAndNavigate() {
        val cursor: Cursor? = contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            arrayOf(Telephony.Sms.BODY, Telephony.Sms.DATE),
            null,
            null,
            Telephony.Sms.DATE + " DESC"
        )

        cursor?.use {
            var checked = 0

            while (it.moveToNext() && checked < 80) {
                checked++

                val body = it.getString(0) ?: continue

                if (
                    body.contains("SOS", ignoreCase = true) ||
                    body.contains("maps.google.com", ignoreCase = true) ||
                    body.contains("Koordinate", ignoreCase = true)
                ) {
                    val coordinates = extractCoordinates(body)
                    if (coordinates != null) {
                        lastLat = coordinates.first
                        lastLon = coordinates.second

                        status.text = t(
                            "Pronađena SOS lokacija.\n\n",
                            "SOS location found.\n\n",
                            "SOS-локация найдена.\n\n",
                            "SOS-Standort gefunden.\n\n"
                        ) + buildCoordinateDisplayText(lastLat, lastLon)

                        btnTopoMap.visibility = android.view.View.VISIBLE
                        btnOsmMap.visibility = android.view.View.VISIBLE
                        openGoogleMaps(lastLat, lastLon)
                        return
                    }
                }
            }
        }

        status.text = t(
            "Nisam pronašao poslednju SOS poruku sa koordinatama. Koristite ručni unos kao rezervu.",
            "I could not find the latest SOS message with coordinates. Use manual entry as a backup.",
            "Последнее SOS-сообщение с координатами не найдено. Используйте ручной ввод как резерв.",
            "Die letzte SOS-Nachricht mit Koordinaten wurde nicht gefunden. Verwenden Sie die manuelle Eingabe als Reserve."
        )
    }

    private fun extractCoordinates(text: String): Pair<Double, Double>? {
        val regex = Regex("""(-?\d{1,3}\.\d+)\s*,\s*(-?\d{1,3}\.\d+)""")
        val match = regex.find(text) ?: return null

        val lat = match.groupValues[1].toDoubleOrNull() ?: return null
        val lon = match.groupValues[2].toDoubleOrNull() ?: return null

        if (lat !in -90.0..90.0) return null
        if (lon !in -180.0..180.0) return null

        return Pair(lat, lon)
    }

    // ─────────────────────────────────────────────
    // KOORDINATE U VIŠE FORMATA
    // ─────────────────────────────────────────────

    private fun buildCoordinateDisplayText(lat: Double, lon: Double): String {
        val dms = toDms(lat, lon)
        return t(
            "Decimalni format:\n$lat, $lon\n\nDMS format (stepeni/minute/sekunde):\n$dms",
            "Decimal format:\n$lat, $lon\n\nDMS format (degrees/minutes/seconds):\n$dms",
            "Десятичный формат:\n$lat, $lon\n\nФормат DMS (градусы/минуты/секунды):\n$dms",
            "Dezimalformat:\n$lat, $lon\n\nDMS-Format (Grad/Minuten/Sekunden):\n$dms"
        )
    }

    private fun toDms(lat: Double, lon: Double): String {
        fun convert(value: Double): Triple<Int, Int, Double> {
            val absolute = abs(value)
            val degrees = absolute.toInt()
            val minutesDouble = (absolute - degrees) * 60
            val minutes = minutesDouble.toInt()
            val seconds = (minutesDouble - minutes) * 60
            return Triple(degrees, minutes, seconds)
        }

        val (latD, latM, latS) = convert(lat)
        val (lonD, lonM, lonS) = convert(lon)

        val latDir = if (lat >= 0) "N" else "S"
        val lonDir = if (lon >= 0) "E" else "W"

        return "$latD°$latM'${String.format("%.1f", latS)}\" $latDir\n$lonD°$lonM'${String.format("%.1f", lonS)}\" $lonDir"
    }

    private fun openGoogleMaps(lat: Double, lon: Double) {
        val uri = Uri.parse("google.navigation:q=$lat,$lon")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.google.android.apps.maps")

        try {
            startActivity(intent)
        } catch (e: Exception) {
            val webUri = Uri.parse("https://maps.google.com/?q=$lat,$lon")
            startActivity(Intent(Intent.ACTION_VIEW, webUri))
        }
    }

    private fun openTopoMap(lat: Double, lon: Double) {
        val uri = Uri.parse("https://opentopomap.org/#map=15/$lat/$lon")
        startActivity(Intent(Intent.ACTION_VIEW, uri))
    }

    private fun openOsmMap(lat: Double, lon: Double) {
        val uri = Uri.parse("https://www.openstreetmap.org/?mlat=$lat&mlon=$lon&zoom=15")
        startActivity(Intent(Intent.ACTION_VIEW, uri))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (
            requestCode == smsPermissionCode &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            findLatestSosSmsAndNavigate()
        } else {
            status.text = t(
                "Dozvola za čitanje SMS poruka nije odobrena.",
                "Permission to read SMS messages was not granted.",
                "Разрешение на чтение SMS-сообщений не предоставлено.",
                "Die Berechtigung zum Lesen von SMS-Nachrichten wurde nicht erteilt."
            )
        }
    }

    // ─────────────────────────────────────────────
    // JEZICI
    // ─────────────────────────────────────────────

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
