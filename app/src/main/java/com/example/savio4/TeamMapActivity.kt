package com.example.savio4

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManager
import android.view.Gravity
import android.view.View
import android.view.animation.TranslateAnimation
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.database.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TeamMapActivity : AppCompatActivity(), LocationListener {

    private lateinit var db: FirebaseDatabase
    private lateinit var missionCode: String
    private lateinit var rescuerName: String
    private var isCoordinator = false
    private var isObserver = false

    private lateinit var statusText: TextView
    private lateinit var rescuersContainer: LinearLayout
    private lateinit var mapView: MapView

    // Chat panel
    private lateinit var chatPanel: LinearLayout
    private lateinit var chatMessagesContainer: LinearLayout
    private lateinit var chatScrollView: ScrollView
    private var chatVisible = false
    private var chatListener: ChildEventListener? = null

    // Sektor crtanje
    private var drawingSectorMode = false
    private val sectorPoints = mutableListOf<GeoPoint>()
    private var sectorPolyline: Polyline? = null
    private lateinit var btnDrawSector: Button
    private lateinit var btnFinishSector: Button
    private lateinit var sectorDrawingInfo: TextView

    // Sektori na mapi
    private val sectorPolygons = mutableMapOf<String, Polygon>()

    // Moj sektor
    private var mySectorId: String? = null
    private var mySectorPoints: List<GeoPoint> = emptyList()
    private var mySectorName: String = ""
    private var wasInsideSector = true

    private var lastLocation: Location? = null
    private val updateIntervalMs = 10_000L
    private val handler = Handler(Looper.getMainLooper())
    private var rescuersListener: ValueEventListener? = null
    private var mapCenteredOnce = false

    private val rescuerMarkers = mutableMapOf<String, Marker>()
    private val foundMarkers = mutableMapOf<String, Marker>()
    private val foundConfirmations = mutableMapOf<String, MutableSet<String>>()
    private val rescuerPhones = mutableListOf<String>()

    // Boje sektora
    private val sectorColors = listOf(
        Color.argb(80, 0, 150, 255),
        Color.argb(80, 0, 200, 100),
        Color.argb(80, 255, 180, 0),
        Color.argb(80, 200, 50, 200),
        Color.argb(80, 255, 100, 0),
        Color.argb(80, 0, 200, 200)
    )
    private var colorIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = packageName

        val prefs = getSharedPreferences("savio_prefs", MODE_PRIVATE)
        missionCode = prefs.getString("teamMissionCode", "") ?: ""
        rescuerName = prefs.getString("teamRescuerName", "") ?: ""
        isCoordinator = prefs.getBoolean("teamIsCoordinator", false)
        isObserver = prefs.getBoolean("teamIsObserver", false)
        val missionName = prefs.getString("teamMissionName", missionCode) ?: missionCode

        db = FirebaseDatabase.getInstance()

        val rootLayout = android.widget.FrameLayout(this)
        rootLayout.setBackgroundColor(Color.rgb(10, 12, 16))

        val mainLayout = LinearLayout(this)
        mainLayout.orientation = LinearLayout.VERTICAL
        mainLayout.setBackgroundColor(Color.rgb(10, 12, 16))
        mainLayout.layoutParams = android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT
        )

        // ─── HEADER ───
        val header = LinearLayout(this)
        header.orientation = LinearLayout.VERTICAL
        header.setPadding(24, 40, 24, 12)

        val title = TextView(this)
        title.text = "SAVIO POTRAGA"
        title.textSize = 22f
        title.setTextColor(Color.rgb(0, 150, 220))
        title.typeface = Typeface.DEFAULT_BOLD

        val missionInfo = TextView(this)
        missionInfo.text = t("Akcija: $missionName  |  Kod: $missionCode", "Mission: $missionName  |  Code: $missionCode", "Операция: $missionName  |  Код: $missionCode", "Einsatz: $missionName  |  Code: $missionCode")
        missionInfo.textSize = 12f
        missionInfo.setTextColor(Color.rgb(100, 100, 100))

        statusText = TextView(this)
        statusText.text = if (isObserver)
            t("👁️ Posmatrac — pratite potragu uzivo", "👁️ Observer — watching live", "👁️ Наблюдатель", "👁️ Beobachter")
        else t("Trazim GPS lokaciju...", "Getting GPS...", "Получение GPS...", "GPS wird ermittelt...")
        statusText.textSize = 13f
        statusText.setTextColor(if (isObserver) Color.rgb(150, 150, 255) else Color.rgb(255, 180, 0))

        val legendText = TextView(this)
        legendText.text = t(
            "🔵 Spasioci   🔴 Nestalo lice   ⭐ Vi  —  kliknite marker za detalje",
            "🔵 Rescuers   🔴 Missing   ⭐ You  —  tap marker for details",
            "🔵 Спасатели   🔴 Пострадавший   ⭐ Вы",
            "🔵 Retter   🔴 Vermisste   ⭐ Sie"
        )
        legendText.textSize = 11f
        legendText.setTextColor(Color.rgb(150, 150, 150))
        legendText.setPadding(0, 4, 0, 0)

        header.addView(title)
        header.addView(missionInfo)
        header.addView(statusText)
        header.addView(legendText)

        // ─── SEKTOR INFO (vidljiv kad je spasilac u sektoru) ───
        sectorDrawingInfo = TextView(this)
        sectorDrawingInfo.textSize = 13f
        sectorDrawingInfo.setTextColor(Color.WHITE)
        sectorDrawingInfo.setPadding(24, 8, 24, 8)
        sectorDrawingInfo.visibility = View.GONE
        sectorDrawingInfo.setBackgroundColor(Color.rgb(0, 60, 0))

        // ─── OSM MAPA ───
        mapView = MapView(this)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(7.0)
        mapView.controller.setCenter(GeoPoint(44.0, 21.0))
        val mapParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        mapView.layoutParams = mapParams

        // Tap listener za crtanje sektora
        mapView.overlays.add(object : org.osmdroid.views.overlay.Overlay() {
            override fun onSingleTapConfirmed(e: android.view.MotionEvent, mapView: MapView): Boolean {
                if (drawingSectorMode && isCoordinator) {
                    val projection = mapView.projection
                    val geoPoint = projection.fromPixels(e.x.toInt(), e.y.toInt())
                    addSectorPoint(GeoPoint(geoPoint.latitude, geoPoint.longitude))
                    return true
                }
                return false
            }
        })

        // ─── LISTA SPASILACA ───
        val scrollView = ScrollView(this)
        val scrollParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(160))
        scrollView.layoutParams = scrollParams

        rescuersContainer = LinearLayout(this)
        rescuersContainer.orientation = LinearLayout.VERTICAL
        rescuersContainer.setPadding(24, 8, 24, 8)
        scrollView.addView(rescuersContainer)

        // ─── DUGMAD ───
        val buttonsLayout = LinearLayout(this)
        buttonsLayout.orientation = LinearLayout.VERTICAL
        buttonsLayout.setPadding(24, 8, 24, 16)

        // Red 1 — CHAT + SEKTOR (samo koordinator)
        val topRow = LinearLayout(this)
        topRow.orientation = LinearLayout.HORIZONTAL
        topRow.setPadding(0, 0, 0, 8)

        val btnChat = Button(this)
        btnChat.text = "💬 " + t("CHAT", "CHAT", "ЧАТ", "CHAT")
        btnChat.setTextColor(Color.WHITE)
        btnChat.setBackgroundColor(Color.rgb(60, 60, 120))
        btnChat.textSize = 13f
        val chatBtnParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        chatBtnParams.setMargins(0, 0, 8, 0)
        btnChat.layoutParams = chatBtnParams
        btnChat.setOnClickListener { toggleChatPanel() }
        topRow.addView(btnChat)

        if (isCoordinator) {
            btnDrawSector = Button(this)
            btnDrawSector.text = "🗺️ " + t("SEKTOR", "SECTOR", "СЕКТОР", "SEKTOR")
            btnDrawSector.setTextColor(Color.WHITE)
            btnDrawSector.setBackgroundColor(Color.rgb(0, 100, 80))
            btnDrawSector.textSize = 13f
            btnDrawSector.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            btnDrawSector.setOnClickListener { startDrawingSector() }
            topRow.addView(btnDrawSector)

            btnFinishSector = Button(this)
            btnFinishSector.text = "✓ " + t("ZAVRŠI", "FINISH", "ГОТОВО", "FERTIG")
            btnFinishSector.setTextColor(Color.WHITE)
            btnFinishSector.setBackgroundColor(Color.rgb(0, 150, 50))
            btnFinishSector.textSize = 13f
            btnFinishSector.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            btnFinishSector.visibility = View.GONE
            btnFinishSector.setOnClickListener { finishDrawingSector() }
            topRow.addView(btnFinishSector)
        }

        buttonsLayout.addView(topRow)

        if (isObserver) {
            val observerInfo = TextView(this)
            observerInfo.text = "👁️ " + t("Posmatrate potragu.", "Observing mission.", "Наблюдение.", "Beobachten.")
            observerInfo.textSize = 12f
            observerInfo.setTextColor(Color.rgb(150, 150, 255))
            val btnLeaveObserver = Button(this)
            btnLeaveObserver.text = t("IZLAZ", "EXIT", "ВЫХОД", "BEENDEN")
            btnLeaveObserver.setTextColor(Color.WHITE)
            btnLeaveObserver.setBackgroundColor(Color.rgb(60, 60, 80))
            btnLeaveObserver.textSize = 13f
            val bottomRowObs = LinearLayout(this)
            bottomRowObs.orientation = LinearLayout.HORIZONTAL
            bottomRowObs.addView(observerInfo.apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f)
            })
            bottomRowObs.addView(btnLeaveObserver.apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            btnLeaveObserver.setOnClickListener { clearTeamPrefs(); finish() }
            buttonsLayout.addView(bottomRowObs)
        } else {
            val bottomRow = LinearLayout(this)
            bottomRow.orientation = LinearLayout.HORIZONTAL

            val btnFound = Button(this)
            btnFound.text = "🚨 " + t("PRONASAO", "FOUND", "НАШЕЛ", "GEFUNDEN")
            btnFound.setTextColor(Color.WHITE)
            btnFound.setBackgroundColor(Color.rgb(180, 50, 0))
            btnFound.textSize = 13f
            val foundBtnParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            foundBtnParams.setMargins(0, 0, 8, 0)
            btnFound.layoutParams = foundBtnParams
            btnFound.setOnClickListener { showFoundPersonDialog() }

            val btnSosHelp = Button(this)
            btnSosHelp.text = "🆘 " + t("TREBAM POMOĆ", "NEED HELP", "НУЖНА ПОМОЩЬ", "HILFE")
            btnSosHelp.setTextColor(Color.WHITE)
            btnSosHelp.setBackgroundColor(Color.rgb(160, 0, 160))
            btnSosHelp.textSize = 13f
            val sosBtnParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            sosBtnParams.setMargins(0, 0, 8, 0)
            btnSosHelp.layoutParams = sosBtnParams
            btnSosHelp.setOnClickListener { showRescuerSosDialog() }

            bottomRow.addView(btnFound)
            bottomRow.addView(btnSosHelp)

            if (isCoordinator) {
                val btnEnd = Button(this)
                btnEnd.text = t("ZAVRSI", "END", "ЗАВЕРШИТЬ", "BEENDEN")
                btnEnd.setTextColor(Color.BLACK)
                btnEnd.setBackgroundColor(Color.rgb(255, 200, 0))
                btnEnd.textSize = 13f
                btnEnd.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                btnEnd.setOnClickListener { showEndMissionDialog() }
                bottomRow.addView(btnEnd)
            } else {
                val btnLeave = Button(this)
                btnLeave.text = t("NAPUSTI", "LEAVE", "ПОКИНУТЬ", "VERLASSEN")
                btnLeave.setTextColor(Color.WHITE)
                btnLeave.setBackgroundColor(Color.rgb(100, 0, 0))
                btnLeave.textSize = 13f
                btnLeave.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                btnLeave.setOnClickListener { showLeaveMissionDialog() }
                bottomRow.addView(btnLeave)
            }
            buttonsLayout.addView(bottomRow)
        }

        mainLayout.addView(header)
        mainLayout.addView(sectorDrawingInfo)
        mainLayout.addView(mapView)
        mainLayout.addView(scrollView)
        mainLayout.addView(buttonsLayout)

        chatPanel = buildChatPanel()
        val chatParams = android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT, dpToPx(380)
        )
        chatParams.gravity = Gravity.BOTTOM
        chatPanel.layoutParams = chatParams
        chatPanel.visibility = View.GONE

        rootLayout.addView(mainLayout)
        rootLayout.addView(chatPanel)

        setContentView(rootLayout)
        applyWindowInsets()

        if (!isObserver) startLocationUpdates()
        startListeningRescuers()
        startListeningFoundPersons()
        startListeningFoundConfirmations()
        startListeningChat()
        startListeningRescuerSos()
        startListeningSectors()
        loadMySector()
    }

    // ─────────────────────────────────────────────
    // SEKTORI — CRTANJE
    // ─────────────────────────────────────────────

    private fun startDrawingSector() {
        drawingSectorMode = true
        sectorPoints.clear()
        sectorPolyline?.let { mapView.overlays.remove(it) }
        sectorPolyline = null

        btnDrawSector.visibility = View.GONE
        btnFinishSector.visibility = View.VISIBLE

        sectorDrawingInfo.text = "🗺️ " + t(
            "Režim crtanja sektora — tapujte tačke na mapi. Minimalno 3 tačke. Pritisnite ZAVRŠI kada završite.",
            "Sector drawing mode — tap points on map. Minimum 3 points. Press FINISH when done.",
            "Режим рисования сектора — нажимайте точки на карте. Минимум 3 точки.",
            "Sektor-Zeichenmodus — Punkte auf der Karte antippen. Mindestens 3 Punkte."
        )
        sectorDrawingInfo.visibility = View.VISIBLE

        Toast.makeText(this, t(
            "Tapujte tačke na mapi da nacrtate sektor",
            "Tap points on map to draw sector",
            "Нажимайте точки на карте для рисования сектора",
            "Tippen Sie Punkte auf die Karte"
        ), Toast.LENGTH_LONG).show()
    }

    private fun addSectorPoint(point: GeoPoint) {
        sectorPoints.add(point)

        // Ažuriraj liniju na mapi
        sectorPolyline?.let { mapView.overlays.remove(it) }

        val polyline = Polyline()
        val displayPoints = sectorPoints.toMutableList()
        if (sectorPoints.size >= 3) displayPoints.add(sectorPoints[0]) // Zatvori poligon
        polyline.setPoints(displayPoints)
        polyline.color = Color.rgb(0, 200, 255)
        polyline.width = 4f
        mapView.overlays.add(polyline)
        sectorPolyline = polyline
        mapView.invalidate()

        sectorDrawingInfo.text = "🗺️ " + t(
            "Tačaka: ${sectorPoints.size} — ${if (sectorPoints.size >= 3) "Možete završiti ili dodati još tačaka" else "Dodajte još ${3 - sectorPoints.size} tačke"}",
            "Points: ${sectorPoints.size} — ${if (sectorPoints.size >= 3) "You can finish or add more points" else "Add ${3 - sectorPoints.size} more points"}",
            "Точек: ${sectorPoints.size} — ${if (sectorPoints.size >= 3) "Можно завершить или добавить точки" else "Добавьте ещё ${3 - sectorPoints.size} точки"}",
            "Punkte: ${sectorPoints.size} — ${if (sectorPoints.size >= 3) "Sie können beenden oder weitere Punkte hinzufügen" else "Fügen Sie ${3 - sectorPoints.size} Punkte hinzu"}"
        )
    }

    private fun finishDrawingSector() {
        if (sectorPoints.size < 3) {
            Toast.makeText(this, t(
                "Minimalno 3 tačke su potrebne za sektor.",
                "Minimum 3 points needed for sector.",
                "Необходимо минимум 3 точки.",
                "Mindestens 3 Punkte erforderlich."
            ), Toast.LENGTH_SHORT).show()
            return
        }

        // Pitaj za naziv sektora
        val input = EditText(this)
        input.hint = t("Npr. Sektor A — severna padina", "E.g. Sector A — north slope", "Напр. Сектор А — северный склон", "z.B. Sektor A — Nordhang")
        input.setTextColor(Color.WHITE)
        input.setPadding(32, 16, 32, 16)

        AlertDialog.Builder(this)
            .setTitle("🗺️ " + t("NAZIV SEKTORA", "SECTOR NAME", "НАЗВАНИЕ СЕКТОРА", "SEKTOR-NAME"))
            .setView(input)
            .setPositiveButton(t("SAČUVAJ SEKTOR", "SAVE SECTOR", "СОХРАНИТЬ СЕКТОР", "SEKTOR SPEICHERN")) { _, _ ->
                val sectorName = input.text.toString().trim().ifEmpty {
                    t("Sektor ${sectorPolygons.size + 1}", "Sector ${sectorPolygons.size + 1}", "Сектор ${sectorPolygons.size + 1}", "Sektor ${sectorPolygons.size + 1}")
                }
                saveSectorToFirebase(sectorName, sectorPoints.toList())
                cancelDrawingSector()
            }
            .setNegativeButton(t("ODUSTANI", "CANCEL", "ОТМЕНА", "ABBRECHEN")) { _, _ ->
                cancelDrawingSector()
            }
            .show()
    }

    private fun cancelDrawingSector() {
        drawingSectorMode = false
        sectorPoints.clear()
        sectorPolyline?.let { mapView.overlays.remove(it) }
        sectorPolyline = null
        btnDrawSector.visibility = View.VISIBLE
        btnFinishSector.visibility = View.GONE
        sectorDrawingInfo.visibility = View.GONE
        mapView.invalidate()
    }

    private fun saveSectorToFirebase(name: String, points: List<GeoPoint>) {
        val sectorId = "sector_${System.currentTimeMillis()}"
        val color = sectorColors[colorIndex % sectorColors.size]
        colorIndex++

        val pointsData = points.map { mapOf("lat" to it.latitude, "lon" to it.longitude) }

        val sectorData = mapOf(
            "id" to sectorId,
            "name" to name,
            "color" to color,
            "points" to pointsData,
            "assignedTo" to "",
            "createdBy" to rescuerName,
            "timestamp" to System.currentTimeMillis()
        )

        db.getReference("sectors").child(missionCode).child(sectorId).setValue(sectorData)
            .addOnSuccessListener {
                Toast.makeText(this, t(
                    "Sektor \"$name\" je kreiran!",
                    "Sector \"$name\" created!",
                    "Сектор \"$name\" создан!",
                    "Sektor \"$name\" erstellt!"
                ), Toast.LENGTH_SHORT).show()
                // Odmah ponudi dodelu sektora
                showAssignSectorDialog(sectorId, name)
            }
    }

    private fun startListeningSectors() {
        db.getReference("sectors").child(missionCode)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Ukloni stare poligone
                    sectorPolygons.values.forEach { mapView.overlays.remove(it) }
                    sectorPolygons.clear()

                    snapshot.children.forEach { sectorSnapshot ->
                        val sectorId = sectorSnapshot.child("id").getValue(String::class.java) ?: return@forEach
                        val name = sectorSnapshot.child("name").getValue(String::class.java) ?: ""
                        val color = sectorSnapshot.child("color").getValue(Int::class.java) ?: Color.argb(80, 0, 150, 255)
                        val assignedTo = sectorSnapshot.child("assignedTo").getValue(String::class.java) ?: ""

                        val points = mutableListOf<GeoPoint>()
                        sectorSnapshot.child("points").children.forEach { pointSnapshot ->
                            val lat = pointSnapshot.child("lat").getValue(Double::class.java) ?: return@forEach
                            val lon = pointSnapshot.child("lon").getValue(Double::class.java) ?: return@forEach
                            points.add(GeoPoint(lat, lon))
                        }

                        if (points.size >= 3) {
                            runOnUiThread { drawSectorOnMap(sectorId, name, color, points, assignedTo) }
                        }
                    }
                    mapView.invalidate()
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun drawSectorOnMap(sectorId: String, name: String, color: Int, points: List<GeoPoint>, assignedTo: String) {
        val polygon = Polygon()
        polygon.points = points
        polygon.fillColor = color
        polygon.strokeColor = Color.argb(200, Color.red(color) * 2, Color.green(color) * 2, Color.blue(color) * 2)
        polygon.strokeWidth = 3f
        polygon.title = if (assignedTo.isNotEmpty()) "$name → $assignedTo" else name

        // Klik na sektor — koordinator može dodeliti
        polygon.setOnClickListener { _, _, _ ->
            if (isCoordinator) {
                showSectorOptionsDialog(sectorId, name, assignedTo)
            } else {
                Toast.makeText(this, t(
                    "Sektor: $name${if (assignedTo.isNotEmpty()) "\nDodeljen: $assignedTo" else ""}",
                    "Sector: $name${if (assignedTo.isNotEmpty()) "\nAssigned: $assignedTo" else ""}",
                    "Сектор: $name${if (assignedTo.isNotEmpty()) "\nНазначен: $assignedTo" else ""}",
                    "Sektor: $name${if (assignedTo.isNotEmpty()) "\nZugewiesen: $assignedTo" else ""}"
                ), Toast.LENGTH_SHORT).show()
            }
            true
        }

        mapView.overlays.add(0, polygon) // Dodaj ispod markera
        sectorPolygons[sectorId] = polygon
    }

    private fun showSectorOptionsDialog(sectorId: String, name: String, assignedTo: String) {
        val options = arrayOf(
            t("👤 Dodeli spasiocu", "👤 Assign to rescuer", "👤 Назначить спасателю", "👤 Retter zuweisen"),
            t("🗑️ Obriši sektor", "🗑️ Delete sector", "🗑️ Удалить сектор", "🗑️ Sektor löschen")
        )

        AlertDialog.Builder(this)
            .setTitle("🗺️ $name")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showAssignSectorDialog(sectorId, name)
                    1 -> showDeleteSectorDialog(sectorId, name)
                }
            }
            .setNegativeButton(t("ZATVORI", "CLOSE", "ЗАКРЫТЬ", "SCHLIESSEN"), null)
            .show()
    }

    private fun showAssignSectorDialog(sectorId: String, sectorName: String) {
        db.getReference("active_rescuers").child(missionCode).get().addOnSuccessListener { snapshot ->
            val rescuerNames = mutableListOf<String>()
            snapshot.children.forEach { r ->
                val name = r.child("name").getValue(String::class.java) ?: return@forEach
                rescuerNames.add(name)
            }

            if (rescuerNames.isEmpty()) {
                Toast.makeText(this, t("Nema aktivnih spasilaca.", "No active rescuers.", "Нет активных спасателей.", "Keine aktiven Retter."), Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }

            AlertDialog.Builder(this)
                .setTitle(t("Dodeli sektor: $sectorName", "Assign sector: $sectorName", "Назначить сектор: $sectorName", "Sektor zuweisen: $sectorName"))
                .setItems(rescuerNames.toTypedArray()) { _, which ->
                    val selectedRescuer = rescuerNames[which]
                    assignSectorToRescuer(sectorId, sectorName, selectedRescuer)
                }
                .setNegativeButton(t("ODUSTANI", "CANCEL", "ОТМЕНА", "ABBRECHEN"), null)
                .show()
        }
    }

    private fun assignSectorToRescuer(sectorId: String, sectorName: String, targetRescuer: String) {
        db.getReference("sectors").child(missionCode).child(sectorId)
            .child("assignedTo").setValue(targetRescuer)
            .addOnSuccessListener {
                // Obavesti spasioca u chatu
                sendChatMessage(t(
                    "🗺️ Spasilac $targetRescuer je dodeljen sektoru: $sectorName",
                    "🗺️ Rescuer $targetRescuer assigned to sector: $sectorName",
                    "🗺️ Спасатель $targetRescuer назначен на сектор: $sectorName",
                    "🗺️ Retter $targetRescuer dem Sektor zugewiesen: $sectorName"
                ))

                // Ažuriraj Firebase profil spasioca sa sektorom
                val safeName = targetRescuer.replace(".", "_").replace("#", "_")
                    .replace("$", "_").replace("[", "_").replace("]", "_")
                db.getReference("active_rescuers").child(missionCode).child(safeName)
                    .child("assignedSector").setValue(sectorName)

                Toast.makeText(this, t(
                    "Sektor \"$sectorName\" dodeljen spasiocu $targetRescuer",
                    "Sector \"$sectorName\" assigned to $targetRescuer",
                    "Сектор \"$sectorName\" назначен $targetRescuer",
                    "Sektor \"$sectorName\" $targetRescuer zugewiesen"
                ), Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDeleteSectorDialog(sectorId: String, name: String) {
        AlertDialog.Builder(this)
            .setTitle(t("Obriši sektor", "Delete sector", "Удалить сектор", "Sektor löschen"))
            .setMessage(t("Da li ste sigurni da želite da obrišete sektor \"$name\"?", "Are you sure you want to delete sector \"$name\"?", "Вы уверены, что хотите удалить сектор \"$name\"?", "Möchten Sie den Sektor \"$name\" wirklich löschen?"))
            .setPositiveButton(t("OBRIŠI", "DELETE", "УДАЛИТЬ", "LÖSCHEN")) { _, _ ->
                db.getReference("sectors").child(missionCode).child(sectorId).removeValue()
            }
            .setNegativeButton(t("ODUSTANI", "CANCEL", "ОТМЕНА", "ABBRECHEN"), null)
            .show()
    }

    private fun loadMySector() {
        if (isObserver) return
        db.getReference("sectors").child(missionCode)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    mySectorId = null
                    mySectorPoints = emptyList()
                    mySectorName = ""

                    snapshot.children.forEach { sectorSnapshot ->
                        val assignedTo = sectorSnapshot.child("assignedTo").getValue(String::class.java) ?: ""
                        if (assignedTo == rescuerName) {
                            mySectorId = sectorSnapshot.child("id").getValue(String::class.java)
                            mySectorName = sectorSnapshot.child("name").getValue(String::class.java) ?: ""
                            val points = mutableListOf<GeoPoint>()
                            sectorSnapshot.child("points").children.forEach { p ->
                                val lat = p.child("lat").getValue(Double::class.java) ?: return@forEach
                                val lon = p.child("lon").getValue(Double::class.java) ?: return@forEach
                                points.add(GeoPoint(lat, lon))
                            }
                            mySectorPoints = points
                        }
                    }

                    runOnUiThread {
                        if (mySectorId != null) {
                            sectorDrawingInfo.text = "🗺️ " + t(
                                "Vaš sektor: $mySectorName",
                                "Your sector: $mySectorName",
                                "Ваш сектор: $mySectorName",
                                "Ihr Sektor: $mySectorName"
                            )
                            sectorDrawingInfo.setBackgroundColor(Color.rgb(0, 60, 0))
                            sectorDrawingInfo.visibility = View.VISIBLE
                        } else {
                            if (!isCoordinator) sectorDrawingInfo.visibility = View.GONE
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun checkSectorBoundary(location: Location) {
        if (mySectorPoints.size < 3 || isObserver) return

        val isInside = isPointInPolygon(GeoPoint(location.latitude, location.longitude), mySectorPoints)

        if (wasInsideSector && !isInside) {
            // Upravo napustio sektor
            wasInsideSector = false
            runOnUiThread { showSectorExitWarning() }
        } else if (!wasInsideSector && isInside) {
            // Vratio se u sektor
            wasInsideSector = true
            runOnUiThread {
                sectorDrawingInfo.text = "🗺️ " + t(
                    "Vaš sektor: $mySectorName — ste unutar sektora",
                    "Your sector: $mySectorName — you are inside the sector",
                    "Ваш сектор: $mySectorName — вы внутри сектора",
                    "Ihr Sektor: $mySectorName — Sie sind im Sektor"
                )
                sectorDrawingInfo.setBackgroundColor(Color.rgb(0, 60, 0))
            }
        }
    }

    private fun showSectorExitWarning() {
        // Zvučno upozorenje
        try {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(android.os.VibrationEffect.createWaveform(longArrayOf(0, 300, 100, 300), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 300, 100, 300), -1)
            }
        } catch (_: Exception) {}

        // Vizuelno upozorenje u header-u
        sectorDrawingInfo.text = "⚠️ " + t(
            "NAPUSTILI STE SEKTOR: $mySectorName!",
            "YOU LEFT YOUR SECTOR: $mySectorName!",
            "ВЫ ПОКИНУЛИ СЕКТОР: $mySectorName!",
            "SIE HABEN SEKTOR VERLASSEN: $mySectorName!"
        )
        sectorDrawingInfo.setBackgroundColor(Color.rgb(150, 0, 0))
        sectorDrawingInfo.visibility = View.VISIBLE

        AlertDialog.Builder(this)
            .setTitle("⚠️ " + t("NAPUSTILI STE SEKTOR", "YOU LEFT YOUR SECTOR", "ВЫ ПОКИНУЛИ СЕКТОР", "SEKTOR VERLASSEN"))
            .setMessage(t(
                "Napustili ste vaš sektor: $mySectorName\n\nDa li nastavljate u susedni sektor ili se vraćate?",
                "You left your sector: $mySectorName\n\nAre you continuing to an adjacent sector or returning?",
                "Вы покинули свой сектор: $mySectorName\n\nВы продолжаете в соседний сектор или возвращаетесь?",
                "Sie haben Ihren Sektor verlassen: $mySectorName\n\nGehen Sie in einen benachbarten Sektor oder kehren Sie zurück?"
            ))
            .setPositiveButton(t("IDEM U SUSEDNI SEKTOR", "GOING TO ADJACENT SECTOR", "ИДУ В СОСЕДНИЙ СЕКТОР", "GEHE IN NACHBARSEKTOR")) { _, _ ->
                // Obavesti koordinatora u chatu
                sendChatMessage("⚠️ " + t(
                    "Spasilac $rescuerName je napustio sektor $mySectorName i prelazi u susedni sektor.",
                    "Rescuer $rescuerName left sector $mySectorName and is moving to adjacent sector.",
                    "Спасатель $rescuerName покинул сектор $mySectorName и переходит в соседний.",
                    "Retter $rescuerName hat Sektor $mySectorName verlassen und wechselt."
                ))
                sectorDrawingInfo.text = "🗺️ " + t(
                    "Van sektora $mySectorName — prelaz u susedni",
                    "Outside sector $mySectorName — moving to adjacent",
                    "Вне сектора $mySectorName — переход",
                    "Außerhalb Sektor $mySectorName — Wechsel"
                )
                sectorDrawingInfo.setBackgroundColor(Color.rgb(100, 80, 0))
            }
            .setNegativeButton(t("VRAĆAM SE U SEKTOR", "RETURNING TO SECTOR", "ВОЗВРАЩАЮСЬ В СЕКТОР", "KEHRE ZURÜCK")) { _, _ ->
                sectorDrawingInfo.text = "🗺️ " + t(
                    "Vraćate se u sektor: $mySectorName",
                    "Returning to sector: $mySectorName",
                    "Возвращаетесь в сектор: $mySectorName",
                    "Kehren zurück zu Sektor: $mySectorName"
                )
                sectorDrawingInfo.setBackgroundColor(Color.rgb(0, 60, 0))
            }
            .setCancelable(false)
            .show()
    }

    // Point-in-polygon algoritam (Ray casting)
    private fun isPointInPolygon(point: GeoPoint, polygon: List<GeoPoint>): Boolean {
        var inside = false
        var j = polygon.size - 1
        for (i in polygon.indices) {
            val xi = polygon[i].longitude; val yi = polygon[i].latitude
            val xj = polygon[j].longitude; val yj = polygon[j].latitude
            val intersect = ((yi > point.latitude) != (yj > point.latitude)) &&
                    (point.longitude < (xj - xi) * (point.latitude - yi) / (yj - yi) + xi)
            if (intersect) inside = !inside
            j = i
        }
        return inside
    }

    // ─────────────────────────────────────────────
    // CHAT PANEL
    // ─────────────────────────────────────────────

    private fun buildChatPanel(): LinearLayout {
        val panel = LinearLayout(this)
        panel.orientation = LinearLayout.VERTICAL

        val panelBg = GradientDrawable()
        panelBg.setColor(Color.rgb(15, 18, 28))
        panelBg.cornerRadii = floatArrayOf(24f, 24f, 24f, 24f, 0f, 0f, 0f, 0f)
        panelBg.setStroke(2, Color.rgb(0, 100, 180))
        panel.background = panelBg

        val chatHeader = LinearLayout(this)
        chatHeader.orientation = LinearLayout.HORIZONTAL
        chatHeader.gravity = Gravity.CENTER_VERTICAL
        chatHeader.setPadding(24, 16, 16, 16)
        chatHeader.setBackgroundColor(Color.rgb(0, 60, 120))

        val chatTitle = TextView(this)
        chatTitle.text = "💬 " + t("CHAT EKIPE", "TEAM CHAT", "ЧАТ КОМАНДЫ", "TEAM-CHAT")
        chatTitle.textSize = 16f
        chatTitle.setTextColor(Color.WHITE)
        chatTitle.typeface = Typeface.DEFAULT_BOLD
        chatTitle.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

        val btnCloseChat = Button(this)
        btnCloseChat.text = "✕"
        btnCloseChat.textSize = 16f
        btnCloseChat.setTextColor(Color.WHITE)
        btnCloseChat.setBackgroundColor(Color.TRANSPARENT)
        btnCloseChat.setOnClickListener { toggleChatPanel() }

        chatHeader.addView(chatTitle)
        chatHeader.addView(btnCloseChat)

        chatScrollView = ScrollView(this)
        chatScrollView.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        chatScrollView.setBackgroundColor(Color.rgb(10, 12, 20))

        chatMessagesContainer = LinearLayout(this)
        chatMessagesContainer.orientation = LinearLayout.VERTICAL
        chatMessagesContainer.setPadding(16, 8, 16, 8)
        chatScrollView.addView(chatMessagesContainer)

        val inputLayout = LinearLayout(this)
        inputLayout.orientation = LinearLayout.HORIZONTAL
        inputLayout.gravity = Gravity.CENTER_VERTICAL
        inputLayout.setPadding(16, 8, 16, 16)
        inputLayout.setBackgroundColor(Color.rgb(15, 18, 28))

        val messageInput = EditText(this)
        messageInput.hint = t("Napišite poruku...", "Write a message...", "Написать сообщение...", "Nachricht schreiben...")
        messageInput.setTextColor(Color.WHITE)
        messageInput.setHintTextColor(Color.rgb(100, 100, 100))
        messageInput.textSize = 14f
        messageInput.setBackgroundColor(Color.rgb(25, 30, 45))
        messageInput.setPadding(16, 12, 16, 12)
        val inputParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        inputParams.setMargins(0, 0, 8, 0)
        messageInput.layoutParams = inputParams

        val btnSend = Button(this)
        btnSend.text = "▶"
        btnSend.textSize = 18f
        btnSend.setTextColor(Color.WHITE)
        btnSend.setBackgroundColor(Color.rgb(0, 100, 180))
        btnSend.layoutParams = LinearLayout.LayoutParams(dpToPx(52), dpToPx(52))

        btnSend.setOnClickListener {
            val text = messageInput.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener
            sendChatMessage(text)
            messageInput.setText("")
        }

        messageInput.setOnEditorActionListener { _, _, _ ->
            val text = messageInput.text.toString().trim()
            if (text.isNotEmpty()) { sendChatMessage(text); messageInput.setText("") }
            true
        }

        inputLayout.addView(messageInput)
        inputLayout.addView(btnSend)

        panel.addView(chatHeader)
        panel.addView(chatScrollView)
        panel.addView(inputLayout)

        return panel
    }

    private fun toggleChatPanel() {
        if (chatVisible) {
            val anim = TranslateAnimation(0f, 0f, 0f, chatPanel.height.toFloat())
            anim.duration = 300
            anim.fillAfter = true
            chatPanel.startAnimation(anim)
            handler.postDelayed({ chatPanel.visibility = View.GONE }, 300)
            chatVisible = false
        } else {
            chatPanel.visibility = View.VISIBLE
            val anim = TranslateAnimation(0f, 0f, chatPanel.height.toFloat(), 0f)
            anim.duration = 300
            chatPanel.startAnimation(anim)
            chatVisible = true
            handler.postDelayed({ chatScrollView.fullScroll(View.FOCUS_DOWN) }, 350)
        }
    }

    private fun sendChatMessage(text: String) {
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val messageData = mapOf(
            "sender" to rescuerName,
            "text" to text,
            "time" to time,
            "timestamp" to System.currentTimeMillis()
        )
        db.getReference("mission_chat").child(missionCode).push().setValue(messageData)
    }

    private fun startListeningChat() {
        val ref = db.getReference("mission_chat").child(missionCode)
        chatListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val sender = snapshot.child("sender").getValue(String::class.java) ?: return
                val text = snapshot.child("text").getValue(String::class.java) ?: return
                val time = snapshot.child("time").getValue(String::class.java) ?: ""
                runOnUiThread { addChatMessage(sender, text, time) }
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addChildEventListener(chatListener!!)
    }

    private fun addChatMessage(sender: String, text: String, time: String) {
        val isMe = sender == rescuerName
        val messageLayout = LinearLayout(this)
        messageLayout.orientation = LinearLayout.VERTICAL
        messageLayout.gravity = if (isMe) Gravity.END else Gravity.START
        val msgParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        msgParams.setMargins(0, 4, 0, 4)
        messageLayout.layoutParams = msgParams

        if (!isMe) {
            val senderText = TextView(this)
            senderText.text = sender
            senderText.textSize = 11f
            senderText.setTextColor(Color.rgb(0, 150, 220))
            senderText.setPadding(12, 0, 0, 2)
            messageLayout.addView(senderText)
        }

        val bubble = TextView(this)
        bubble.text = "$text\n$time"
        bubble.textSize = 14f
        bubble.setTextColor(Color.WHITE)
        bubble.setPadding(16, 10, 16, 10)

        val bubbleBg = GradientDrawable()
        bubbleBg.setColor(if (isMe) Color.rgb(0, 80, 160) else Color.rgb(30, 35, 50))
        bubbleBg.cornerRadius = 16f
        bubble.background = bubbleBg

        val bubbleParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        bubbleParams.gravity = if (isMe) Gravity.END else Gravity.START
        bubbleParams.setMargins(if (isMe) dpToPx(60) else 0, 0, if (isMe) 0 else dpToPx(60), 0)
        bubble.layoutParams = bubbleParams

        messageLayout.addView(bubble)
        chatMessagesContainer.addView(messageLayout)
        handler.postDelayed({ chatScrollView.fullScroll(View.FOCUS_DOWN) }, 100)
    }

    // ─────────────────────────────────────────────
    // MARKERI NA MAPI
    // ─────────────────────────────────────────────

    private fun makeRescuerBitmap(name: String, isMe: Boolean, signalStatus: Int = 0): BitmapDrawable {
        val size = 80
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.color = when {
            isMe -> Color.rgb(255, 200, 0)
            signalStatus == 2 -> Color.rgb(100, 100, 100)
            signalStatus == 1 -> Color.rgb(200, 130, 0)
            else -> Color.rgb(30, 120, 220)
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4, paint)

        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4, paint)

        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        paint.textSize = 30f
        paint.textAlign = Paint.Align.CENTER
        val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        canvas.drawText(initial, size / 2f, size / 2f + 10f, paint)

        return BitmapDrawable(resources, bitmap)
    }

    private fun makeMissingPersonBitmap(): BitmapDrawable {
        val size = 80
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.color = Color.rgb(200, 0, 0)
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4, paint)
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4, paint)
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        paint.textSize = 36f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("!", size / 2f, size / 2f + 13f, paint)

        return BitmapDrawable(resources, bitmap)
    }

    private fun makeSosBitmap(name: String): BitmapDrawable {
        val size = 80
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.color = Color.rgb(220, 0, 0)
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4, paint)
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4, paint)
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        paint.textSize = 28f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("SOS", size / 2f, size / 2f + 10f, paint)

        return BitmapDrawable(resources, bitmap)
    }

    private fun updateRescuerOnMap(name: String, lat: Double, lon: Double, lastUpdateTime: String, phone: String = "", battery: Int = -1, signalStatus: Int = 0) {
        if (lat == 0.0 && lon == 0.0) return
        val position = GeoPoint(lat, lon)
        val isMe = name == rescuerName

        runOnUiThread {
            val existing = rescuerMarkers[name]
            if (existing != null) {
                existing.position = position
                existing.icon = makeRescuerBitmap(name, isMe, signalStatus)
                existing.setOnMarkerClickListener { _, _ ->
                    showRescuerInfo(name, lat, lon, lastUpdateTime, phone, battery, signalStatus, isMe)
                    true
                }
                mapView.invalidate()
            } else {
                val marker = Marker(mapView)
                marker.position = position
                marker.title = if (isMe) "$name ⭐" else name
                marker.icon = makeRescuerBitmap(name, isMe, signalStatus)
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                marker.setOnMarkerClickListener { _, _ ->
                    showRescuerInfo(name, lat, lon, lastUpdateTime, phone, battery, signalStatus, isMe)
                    true
                }
                mapView.overlays.add(marker)
                rescuerMarkers[name] = marker
                mapView.invalidate()
            }

            if (isMe && !mapCenteredOnce) {
                mapCenteredOnce = true
                mapView.controller.animateTo(position)
                mapView.controller.setZoom(14.0)
            }
        }
    }

    private fun addMissingPersonMarker(findId: String, lat: Double, lon: Double, status: String, rescuer: String, time: String) {
        if (lat == 0.0 && lon == 0.0) return
        val position = GeoPoint(lat, lon)

        runOnUiThread {
            val existing = foundMarkers[findId]
            if (existing != null) {
                existing.title = "🔴 $status"
                mapView.invalidate()
                return@runOnUiThread
            }

            val marker = Marker(mapView)
            marker.position = position
            marker.title = "🔴 $status"
            marker.icon = makeMissingPersonBitmap()
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            marker.setOnMarkerClickListener { _, _ ->
                showMissingPersonInfo(status, rescuer, lat, lon, time)
                true
            }
            mapView.overlays.add(marker)
            foundMarkers[findId] = marker
            mapView.controller.animateTo(position)
            mapView.controller.setZoom(15.0)
            mapView.invalidate()
        }
    }

    private fun showRescuerInfo(name: String, lat: Double, lon: Double, lastUpdate: String, phone: String, battery: Int, signalStatus: Int, isMe: Boolean) {
        val title = if (isMe) "⭐ " + t("Vi ($name)", "You ($name)", "Вы ($name)", "Sie ($name)") else "🔵 $name"

        val phoneDisplay = if (phone.isNotEmpty()) phone
        else t("Broj nije dostupan", "Number not available", "Номер недоступен", "Nummer nicht verfügbar")

        val batteryDisplay = if (battery >= 0) "$battery%" else "--"
        val batteryLine = t("Baterija: $batteryDisplay", "Battery: $batteryDisplay", "Батарея: $batteryDisplay", "Akku: $batteryDisplay")

        val signalLine = when (signalStatus) {
            1 -> t("⚠️ Slab signal", "⚠️ Weak signal", "⚠️ Слабый сигнал", "⚠️ Schwaches Signal")
            2 -> t("🔴 Signal izgubljen", "🔴 Signal lost", "🔴 Сигнал потерян", "🔴 Signal verloren")
            else -> ""
        }

        val message = t(
            "Koordinate:\n${"%.5f".format(lat)}, ${"%.5f".format(lon)}\n\nPoslednje azuriranje: $lastUpdate\n\nBroj telefona: $phoneDisplay\n$batteryLine${if (signalLine.isNotEmpty()) "\n$signalLine" else ""}",
            "Coordinates:\n${"%.5f".format(lat)}, ${"%.5f".format(lon)}\n\nLast update: $lastUpdate\n\nPhone: $phoneDisplay\n$batteryLine${if (signalLine.isNotEmpty()) "\n$signalLine" else ""}",
            "Координаты:\n${"%.5f".format(lat)}, ${"%.5f".format(lon)}\n\nОбновлено: $lastUpdate\n\nТелефон: $phoneDisplay\n$batteryLine${if (signalLine.isNotEmpty()) "\n$signalLine" else ""}",
            "Koordinaten:\n${"%.5f".format(lat)}, ${"%.5f".format(lon)}\n\nUpdate: $lastUpdate\n\nTelefon: $phoneDisplay\n$batteryLine${if (signalLine.isNotEmpty()) "\n$signalLine" else ""}"
        )

        val builder = AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(t("ZATVORI", "CLOSE", "ЗАКРЫТЬ", "SCHLIESSEN"), null)

        if (!isMe && phone.isNotEmpty()) {
            builder.setNeutralButton("📞 " + t("POZOVI", "CALL", "ПОЗВОНИТЬ", "ANRUFEN")) { _, _ ->
                try {
                    val intent = Intent(Intent.ACTION_DIAL)
                    intent.data = android.net.Uri.parse("tel:$phone")
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, t("Greška.", "Error.", "Ошибка.", "Fehler."), Toast.LENGTH_SHORT).show()
                }
            }
        }
        builder.show()
    }

    private fun showMissingPersonInfo(status: String, rescuer: String, lat: Double, lon: Double, time: String) {
        AlertDialog.Builder(this)
            .setTitle("🔴 " + t("NESTALO LICE", "MISSING PERSON", "ПОСТРАДАВШИЙ", "VERMISSTE PERSON"))
            .setMessage(t(
                "Status: $status\n\nPronasao: $rescuer\nVrijeme: $time\n\nKoordinate:\n${"%.5f".format(lat)}, ${"%.5f".format(lon)}",
                "Status: $status\n\nFound by: $rescuer\nTime: $time\n\nCoordinates:\n${"%.5f".format(lat)}, ${"%.5f".format(lon)}",
                "Статус: $status\n\nНашёл: $rescuer\nВремя: $time\n\nКоординаты:\n${"%.5f".format(lat)}, ${"%.5f".format(lon)}",
                "Status: $status\n\nGefunden: $rescuer\nZeit: $time\n\nKoordinaten:\n${"%.5f".format(lat)}, ${"%.5f".format(lon)}"
            ))
            .setPositiveButton(t("ZATVORI", "CLOSE", "ЗАКРЫТЬ", "SCHLIESSEN"), null)
            .show()
    }

    // ─────────────────────────────────────────────
    // LOKACIJA
    // ─────────────────────────────────────────────

    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) return
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, updateIntervalMs, 5f, this)
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, updateIntervalMs, 5f, this)
        } catch (_: Exception) {}
    }

    override fun onLocationChanged(location: Location) {
        if (isObserver) return
        lastLocation = location
        statusText.text = t("GPS aktivan — lokacija se dijeli u realnom vremenu", "GPS active — location shared in real time", "GPS активен — местоположение передается", "GPS aktiv — Standort wird geteilt")
        statusText.setTextColor(Color.rgb(0, 200, 100))
        updateLocationOnFirebase(location)
        checkSectorBoundary(location)
    }

    private fun getBatteryPercent(): Int {
        val intent = registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
        if (level < 0 || scale <= 0) return -1
        return (level * 100) / scale
    }

    private fun updateLocationOnFirebase(location: Location) {
        if (isObserver) return
        val safeName = rescuerName.replace(".", "_").replace("#", "_").replace("$", "_").replace("[", "_").replace("]", "_")
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val battery = getBatteryPercent()
        db.getReference("active_rescuers").child(missionCode).child(safeName)
            .updateChildren(mapOf(
                "lat" to location.latitude,
                "lon" to location.longitude,
                "lastUpdate" to System.currentTimeMillis(),
                "lastUpdateTime" to time,
                "battery" to battery
            ))
    }

    // ─────────────────────────────────────────────
    // FIREBASE — SPASIOCI
    // ─────────────────────────────────────────────

    private fun startListeningRescuers() {
        val ref = db.getReference("active_rescuers").child(missionCode)
        rescuersListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                rescuersContainer.removeAllViews()
                rescuerPhones.clear()

                if (!snapshot.exists() || snapshot.childrenCount == 0L) {
                    val emptyText = TextView(this@TeamMapActivity)
                    emptyText.text = t("Nema aktivnih spasilaca.", "No active rescuers.", "Нет активных спасателей.", "Keine aktiven Retter.")
                    emptyText.textSize = 14f
                    emptyText.setTextColor(Color.rgb(120, 120, 120))
                    rescuersContainer.addView(emptyText)
                    return
                }

                snapshot.children.forEach { rescuerSnapshot ->
                    val name = rescuerSnapshot.child("name").getValue(String::class.java) ?: return@forEach
                    val lat = rescuerSnapshot.child("lat").getValue(Double::class.java) ?: 0.0
                    val lon = rescuerSnapshot.child("lon").getValue(Double::class.java) ?: 0.0
                    val lastUpdateTime = rescuerSnapshot.child("lastUpdateTime").getValue(String::class.java) ?: "--:--"
                    val lastUpdateTs = rescuerSnapshot.child("lastUpdate").getValue(Long::class.java) ?: 0L
                    val phone = rescuerSnapshot.child("phone").getValue(String::class.java) ?: ""
                    val battery = rescuerSnapshot.child("battery").getValue(Int::class.java) ?: -1
                    val assignedSector = rescuerSnapshot.child("assignedSector").getValue(String::class.java) ?: ""
                    if (phone.isNotEmpty()) rescuerPhones.add(phone)

                    val timeSinceUpdate = if (lastUpdateTs > 0) System.currentTimeMillis() - lastUpdateTs else 0L
                    val signalStatus = when {
                        lastUpdateTs == 0L -> 0
                        timeSinceUpdate > 1_800_000L -> 2
                        timeSinceUpdate > 600_000L -> 1
                        else -> 0
                    }

                    val isMe = name == rescuerName
                    addRescuerCard(name, lat, lon, lastUpdateTime, phone, battery, signalStatus, assignedSector, isMe)
                    updateRescuerOnMap(name, lat, lon, lastUpdateTime, phone, battery, signalStatus)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(rescuersListener!!)
    }

    private fun addRescuerCard(name: String, lat: Double, lon: Double, lastUpdateTime: String, phone: String, battery: Int, signalStatus: Int, assignedSector: String, isMe: Boolean) {
        val card = LinearLayout(this)
        card.orientation = LinearLayout.HORIZONTAL
        card.gravity = Gravity.CENTER_VERTICAL
        card.setPadding(16, 10, 16, 10)

        val cardBg = GradientDrawable()
        cardBg.setColor(when {
            isMe -> Color.rgb(0, 40, 80)
            signalStatus == 2 -> Color.rgb(30, 30, 30)
            signalStatus == 1 -> Color.rgb(40, 25, 0)
            else -> Color.rgb(18, 22, 28)
        })
        cardBg.cornerRadius = 12f
        cardBg.setStroke(2, when {
            isMe -> Color.rgb(0, 150, 220)
            signalStatus == 2 -> Color.rgb(100, 100, 100)
            signalStatus == 1 -> Color.rgb(200, 130, 0)
            else -> Color.rgb(40, 50, 60)
        })
        card.background = cardBg

        val cardParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        cardParams.setMargins(0, 0, 0, 6)
        card.layoutParams = cardParams

        val dot = TextView(this)
        dot.text = when {
            isMe -> "⭐"; signalStatus == 2 -> "⚫"; signalStatus == 1 -> "🟠"; else -> "🔵"
        }
        dot.textSize = 16f
        dot.setPadding(0, 0, 10, 0)

        val infoCol = LinearLayout(this)
        infoCol.orientation = LinearLayout.VERTICAL
        infoCol.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

        val nameText = TextView(this)
        nameText.text = if (isMe) "$name ${t("(Vi)", "(You)", "(Вы)", "(Sie)")}" else name
        nameText.textSize = 14f
        nameText.setTextColor(when { isMe -> Color.rgb(0, 180, 255); signalStatus == 2 -> Color.rgb(150, 150, 150); signalStatus == 1 -> Color.rgb(255, 180, 0); else -> Color.WHITE })
        nameText.typeface = Typeface.DEFAULT_BOLD

        val coordText = TextView(this)
        coordText.text = if (lat == 0.0 && lon == 0.0) t("Lokacija se ucitava...", "Loading...", "Загрузка...", "Wird geladen...") else "%.4f, %.4f".format(lat, lon)
        coordText.textSize = 10f
        coordText.setTextColor(Color.rgb(150, 150, 150))

        val timeText = TextView(this)
        timeText.text = t("Azurirano: $lastUpdateTime", "Updated: $lastUpdateTime", "Обновлено: $lastUpdateTime", "Aktualisiert: $lastUpdateTime")
        timeText.textSize = 9f
        timeText.setTextColor(Color.rgb(100, 100, 100))

        infoCol.addView(nameText)
        infoCol.addView(coordText)
        infoCol.addView(timeText)

        // Sektor info
        if (assignedSector.isNotEmpty()) {
            val sectorText = TextView(this)
            sectorText.text = "🗺️ $assignedSector"
            sectorText.textSize = 10f
            sectorText.setTextColor(Color.rgb(0, 200, 150))
            infoCol.addView(sectorText)
        }

        // Signal status
        if (signalStatus > 0) {
            val signalText = TextView(this)
            signalText.text = when (signalStatus) {
                1 -> "⚠️ " + t("Slab signal", "Weak signal", "Слабый сигнал", "Schwaches Signal")
                else -> "🔴 " + t("Signal izgubljen", "Signal lost", "Сигнал потерян", "Signal verloren")
            }
            signalText.setTextColor(if (signalStatus == 1) Color.rgb(255, 180, 0) else Color.rgb(220, 50, 50))
            signalText.textSize = 10f
            infoCol.addView(signalText)
        }

        // Baterija
        val batteryText = TextView(this)
        if (battery >= 0) {
            val batteryColor = when { battery >= 50 -> Color.rgb(0, 200, 100); battery >= 20 -> Color.rgb(255, 180, 0); else -> Color.rgb(220, 0, 0) }
            batteryText.text = "🔋 $battery%"
            batteryText.setTextColor(batteryColor)
        } else {
            batteryText.text = "🔋 --"
            batteryText.setTextColor(Color.rgb(100, 100, 100))
        }
        batteryText.textSize = 11f
        infoCol.addView(batteryText)

        card.addView(dot)
        card.addView(infoCol)

        if (lat != 0.0 && lon != 0.0) {
            val btnCenter = Button(this)
            btnCenter.text = "📍"
            btnCenter.textSize = 14f
            btnCenter.setBackgroundColor(Color.TRANSPARENT)
            btnCenter.setTextColor(Color.rgb(0, 150, 220))
            btnCenter.setOnClickListener {
                mapView.controller.animateTo(GeoPoint(lat, lon))
                mapView.controller.setZoom(15.0)
            }
            card.addView(btnCenter)
        }

        rescuersContainer.addView(card)
    }

    // ─────────────────────────────────────────────
    // DVOSTRUKA VERIFIKACIJA NALAZA
    // ─────────────────────────────────────────────

    private fun showFoundPersonDialog() {
        val statusOptions = arrayOf(
            t("ZIVA I ZDRAVA", "ALIVE AND WELL", "ЖИВ И ЗДОРОВ", "LEBT UND IST WOHLAUF"),
            t("ZIVA — POTREBNA MEDICINSKA POMOC", "ALIVE — MEDICAL HELP NEEDED", "ЖИВ — НУЖНА МЕДПОМОЩЬ", "LEBT — MEDIZINISCHE HILFE NÖTIG"),
            t("ZIVA — TESKE POVREDE", "ALIVE — SERIOUS INJURIES", "ЖИВ — ТЯЖЁЛЫЕ ТРАВМЫ", "LEBT — SCHWERE VERLETZUNGEN"),
            t("NEMA ZNAKOVA ZIVOTA", "NO SIGNS OF LIFE", "НЕТ ПРИЗНАКОВ ЖИЗНИ", "KEINE LEBENSZEICHEN")
        )
        var selectedStatus = statusOptions[0]

        AlertDialog.Builder(this)
            .setTitle(t("STATUS PRONADJENOG LICA", "FOUND PERSON STATUS", "СТАТУС НАЙДЕННОГО", "STATUS"))
            .setSingleChoiceItems(statusOptions, 0) { _, which -> selectedStatus = statusOptions[which] }
            .setPositiveButton(t("PRIJAVI NALAZ", "REPORT FIND", "СООБЩИТЬ", "MELDEN")) { _, _ -> submitFoundReport(selectedStatus) }
            .setNegativeButton(t("ODUSTANI", "CANCEL", "ОТМЕНА", "ABBRECHEN"), null)
            .show()
    }

    private fun submitFoundReport(status: String) {
        val lat = lastLocation?.latitude ?: 0.0
        val lon = lastLocation?.longitude ?: 0.0
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val findId = "find_${rescuerName.replace(" ", "_")}_${System.currentTimeMillis()}"

        val report = mapOf(
            "findId" to findId, "rescuer" to rescuerName, "status" to status,
            "lat" to lat, "lon" to lon, "time" to time,
            "timestamp" to System.currentTimeMillis(),
            "confirmations" to mapOf(rescuerName to true), "confirmed" to false
        )

        db.getReference("found_persons").child(missionCode).child(findId).setValue(report)
            .addOnSuccessListener {
                addMissingPersonMarker(findId, lat, lon, "⏳ $status", rescuerName, time)
                sendChatMessage("🚨 " + t(
                    "PRIJAVLJUJEM NALAZ LICA! Status: $status — ${"%.4f".format(lat)}, ${"%.4f".format(lon)}",
                    "REPORTING FIND! Status: $status — ${"%.4f".format(lat)}, ${"%.4f".format(lon)}",
                    "СООБЩАЮ О НАХОДКЕ! Статус: $status",
                    "MELDE FUND! Status: $status"
                ))
                Toast.makeText(this, t("Nalaz prijavljen! Ceka se potvrda.", "Find reported! Waiting for confirmation.", "Находка зарегистрирована!", "Fund gemeldet!"), Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener { Toast.makeText(this, t("Greska.", "Error.", "Ошибка.", "Fehler."), Toast.LENGTH_SHORT).show() }
    }

    private fun startListeningFoundPersons() {
        db.getReference("found_persons").child(missionCode)
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) { processFoundPersonSnapshot(snapshot) }
                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) { processFoundPersonSnapshot(snapshot) }
                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun processFoundPersonSnapshot(snapshot: DataSnapshot) {
        val findId = snapshot.child("findId").getValue(String::class.java) ?: snapshot.key ?: return
        val reportingRescuer = snapshot.child("rescuer").getValue(String::class.java) ?: return
        val status = snapshot.child("status").getValue(String::class.java) ?: return
        val lat = snapshot.child("lat").getValue(Double::class.java) ?: 0.0
        val lon = snapshot.child("lon").getValue(Double::class.java) ?: 0.0
        val time = snapshot.child("time").getValue(String::class.java) ?: ""
        val alreadyConfirmed = snapshot.child("confirmed").getValue(Boolean::class.java) ?: false

        val confirmations = mutableSetOf<String>()
        snapshot.child("confirmations").children.forEach { child -> child.key?.let { confirmations.add(it) } }
        foundConfirmations[findId] = confirmations

        runOnUiThread {
            if (alreadyConfirmed) { addMissingPersonMarker(findId, lat, lon, status, reportingRescuer, time); return@runOnUiThread }
            addMissingPersonMarker(findId, lat, lon, "⏳ $status", reportingRescuer, time)
            if (reportingRescuer != rescuerName && !isObserver && !confirmations.contains(rescuerName)) {
                showConfirmationRequest(findId, reportingRescuer, status, lat, lon, time, confirmations.size)
            }
        }
    }

    private fun startListeningFoundConfirmations() {
        db.getReference("found_persons").child(missionCode)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.children.forEach { findSnapshot ->
                        val findId = findSnapshot.child("findId").getValue(String::class.java) ?: findSnapshot.key ?: return@forEach
                        val alreadyConfirmed = findSnapshot.child("confirmed").getValue(Boolean::class.java) ?: false
                        if (alreadyConfirmed) return@forEach

                        val confirmations = mutableSetOf<String>()
                        findSnapshot.child("confirmations").children.forEach { c -> c.key?.let { confirmations.add(it) } }

                        if (confirmations.size >= 2) {
                            val status = findSnapshot.child("status").getValue(String::class.java) ?: ""
                            val lat = findSnapshot.child("lat").getValue(Double::class.java) ?: 0.0
                            val lon = findSnapshot.child("lon").getValue(Double::class.java) ?: 0.0
                            val time = findSnapshot.child("time").getValue(String::class.java) ?: ""
                            val rescuer = findSnapshot.child("rescuer").getValue(String::class.java) ?: ""

                            db.getReference("found_persons").child(missionCode).child(findId).child("confirmed").setValue(true)

                            runOnUiThread {
                                if (isCoordinator) showMissionSuccessDialog(status, rescuer, lat, lon, time)
                                else showConfirmedNotification(status, rescuer, lat, lon, time)
                            }
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun showConfirmationRequest(findId: String, reportingRescuer: String, status: String, lat: Double, lon: Double, time: String, currentConfirmations: Int) {
        AlertDialog.Builder(this)
            .setTitle("🚨 " + t("NALAZ LICA — POTVRDA", "PERSON FOUND — CONFIRM", "НАХОДКА — ПОДТВЕРЖДЕНИЕ", "FUND — BESTÄTIGUNG"))
            .setMessage(t(
                "Spasilac $reportingRescuer je prijavio pronalazak!\n\nStatus: $status\nVrijeme: $time\nPotvrde: $currentConfirmations/2\n\nDa li potvrdujete?",
                "Rescuer $reportingRescuer reported a find!\n\nStatus: $status\nTime: $time\nConfirmations: $currentConfirmations/2\n\nDo you confirm?",
                "Спасатель $reportingRescuer сообщил о находке!\n\nСтатус: $status\nВремя: $time\nПодтверждений: $currentConfirmations/2",
                "Retter $reportingRescuer meldete Fund!\n\nStatus: $status\nZeit: $time\nBestätigungen: $currentConfirmations/2"
            ))
            .setPositiveButton(t("POTVRDITE", "CONFIRM", "ПОДТВЕРДИТЬ", "BESTÄTIGEN")) { _, _ -> addMyConfirmation(findId) }
            .setNegativeButton(t("NISAM SIGURAN", "NOT SURE", "НЕ УВЕРЕН", "NICHT SICHER"), null)
            .setCancelable(false)
            .show()
    }

    private fun addMyConfirmation(findId: String) {
        val safeRescuerName = rescuerName.replace(".", "_").replace("#", "_").replace("$", "_").replace("[", "_").replace("]", "_")
        db.getReference("found_persons").child(missionCode).child(findId).child("confirmations").child(safeRescuerName).setValue(true)
            .addOnSuccessListener { Toast.makeText(this, t("Potvrda zabilježena!", "Confirmed!", "Подтверждено!", "Bestätigt!"), Toast.LENGTH_SHORT).show() }
    }

    private fun showMissionSuccessDialog(status: String, rescuer: String, lat: Double, lon: Double, time: String) {
        AlertDialog.Builder(this)
            .setTitle("✅ " + t("POTRAGA USPJESNA", "MISSION SUCCESSFUL", "ОПЕРАЦИЯ УСПЕШНА", "ERFOLGREICH"))
            .setMessage(t(
                "Lice pronadjeno i potvrdjeno!\n\nStatus: $status\nPronasao: $rescuer\nVrijeme: $time\n\nZavrsiti i poslati SMS?",
                "Person found and confirmed!\n\nStatus: $status\nFound by: $rescuer\nTime: $time\n\nEnd and send SMS?",
                "Найден и подтверждён!\n\nСтатус: $status\nНашёл: $rescuer\nВремя: $time",
                "Gefunden und bestätigt!\n\nStatus: $status\nGefunden: $rescuer\nZeit: $time"
            ))
            .setPositiveButton(t("ZAVRSI I POŠALJI SMS", "END & SEND SMS", "ЗАВЕРШИТЬ И SMS", "BEENDEN & SMS")) { _, _ ->
                saveMissionLog(status, rescuer, lat, lon, time, true)
                endMissionSuccess(status, rescuer, lat, lon, time)
            }
            .setNegativeButton(t("CEKAJ", "WAIT", "ПОДОЖДИТЕ", "WARTEN"), null)
            .setCancelable(false)
            .show()
    }

    private fun showConfirmedNotification(status: String, rescuer: String, lat: Double, lon: Double, time: String) {
        AlertDialog.Builder(this)
            .setTitle("✅ " + t("LICE POTVRDJENO", "PERSON CONFIRMED", "ПОДТВЕРЖДЕНО", "BESTÄTIGT"))
            .setMessage(t("Potraga uspjesno zavrsena!\n\nStatus: $status\nPronasao: $rescuer\nVrijeme: $time", "Mission completed!\n\nStatus: $status\nFound by: $rescuer\nTime: $time", "Операция завершена!\n\nСтатус: $status", "Einsatz abgeschlossen!\n\nStatus: $status"))
            .setPositiveButton(t("ZATVORI", "CLOSE", "ЗАКРЫТЬ", "SCHLIESSEN"), null)
            .setCancelable(false)
            .show()
    }

    // ─────────────────────────────────────────────
    // SOS UNUTAR POTRAGE
    // ─────────────────────────────────────────────

    private fun showRescuerSosDialog() {
        AlertDialog.Builder(this)
            .setTitle("🆘 " + t("TREBAM POMOĆ", "I NEED HELP", "НУЖНА ПОМОЩЬ", "ICH BRAUCHE HILFE"))
            .setMessage(t(
                "Da li ste sigurni da trebate pomoć kolega?\n\nSvi spasioci odmah dobijaju alarm sa vašom lokacijom.\n\nKoristite ovo samo u stvarnoj opasnosti!",
                "Are you sure you need help?\n\nAll rescuers receive an alert with your location.\n\nUse only in real danger!",
                "Вы уверены, что вам нужна помощь?\n\nВсе спасатели получат сигнал тревоги.\n\nТолько при реальной опасности!",
                "Sind Sie sicher?\n\nAlle Retter erhalten einen Alarm.\n\nNur bei echter Gefahr!"
            ))
            .setPositiveButton("🆘 " + t("POŠALJI ALARM", "SEND ALERT", "ОТПРАВИТЬ СИГНАЛ", "ALARM SENDEN")) { _, _ -> sendRescuerSosAlert() }
            .setNegativeButton(t("ODUSTANI", "CANCEL", "ОТМЕНА", "ABBRECHEN"), null)
            .show()
    }

    private fun sendRescuerSosAlert() {
        val lat = lastLocation?.latitude ?: 0.0
        val lon = lastLocation?.longitude ?: 0.0
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

        val sosData = mapOf("rescuer" to rescuerName, "lat" to lat, "lon" to lon, "time" to time, "timestamp" to System.currentTimeMillis())
        db.getReference("rescuer_sos").child(missionCode)
            .child(rescuerName.replace(".", "_").replace("#", "_").replace("$", "_").replace("[", "_").replace("]", "_"))
            .setValue(sosData)

        sendChatMessage("🆘🆘🆘 " + t(
            "SPASILAC $rescuerName TREBA HITNU POMOĆ! Lokacija: ${"%.4f".format(lat)}, ${"%.4f".format(lon)} — $time",
            "RESCUER $rescuerName NEEDS URGENT HELP! Location: ${"%.4f".format(lat)}, ${"%.4f".format(lon)} — $time",
            "СПАСАТЕЛЬ $rescuerName СРОЧНО НУЖДАЕТСЯ В ПОМОЩИ! ${"%.4f".format(lat)}, ${"%.4f".format(lon)} — $time",
            "RETTER $rescuerName BRAUCHT DRINGEND HILFE! ${"%.4f".format(lat)}, ${"%.4f".format(lon)} — $time"
        ))

        if (lat != 0.0 && lon != 0.0) {
            runOnUiThread {
                val position = GeoPoint(lat, lon)
                val existing = rescuerMarkers[rescuerName]
                val sosIcon = makeSosBitmap(rescuerName)
                if (existing != null) { existing.icon = sosIcon; mapView.invalidate() }
                else {
                    val marker = Marker(mapView)
                    marker.position = position
                    marker.title = "🆘 $rescuerName"
                    marker.icon = sosIcon
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    mapView.overlays.add(marker)
                    rescuerMarkers[rescuerName] = marker
                    mapView.invalidate()
                }
                mapView.controller.animateTo(position)
                mapView.controller.setZoom(15.0)
            }
        }

        Toast.makeText(this, t("Alarm poslan!", "Alert sent!", "Сигнал отправлен!", "Alarm gesendet!"), Toast.LENGTH_LONG).show()
    }

    private fun startListeningRescuerSos() {
        db.getReference("rescuer_sos").child(missionCode)
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val rescuer = snapshot.child("rescuer").getValue(String::class.java) ?: return
                    if (rescuer == rescuerName) return
                    val lat = snapshot.child("lat").getValue(Double::class.java) ?: 0.0
                    val lon = snapshot.child("lon").getValue(Double::class.java) ?: 0.0
                    val time = snapshot.child("time").getValue(String::class.java) ?: ""
                    runOnUiThread { showRescuerSosNotification(rescuer, lat, lon, time) }
                }
                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun showRescuerSosNotification(rescuer: String, lat: Double, lon: Double, time: String) {
        try {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(android.os.VibrationEffect.createWaveform(longArrayOf(0, 500, 200, 500, 200, 500), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 500, 200, 500, 200, 500), -1)
            }
        } catch (_: Exception) {}

        AlertDialog.Builder(this)
            .setTitle("🆘🆘🆘 " + t("SPASILAC TREBA POMOĆ!", "RESCUER NEEDS HELP!", "СПАСАТЕЛЬ НУЖДАЕТСЯ В ПОМОЩИ!", "RETTER BRAUCHT HILFE!"))
            .setMessage(t(
                "HITNO!\n\nSpasilac $rescuer je aktivirao alarm!\n\nVreme: $time\nKoordinate: ${"%.4f".format(lat)}, ${"%.4f".format(lon)}\n\nOdmah se uputite ka njegovoj lokaciji!",
                "URGENT!\n\nRescuer $rescuer activated alarm!\n\nTime: $time\nCoordinates: ${"%.4f".format(lat)}, ${"%.4f".format(lon)}\n\nHead to their location immediately!",
                "СРОЧНО!\n\nСпасатель $rescuer активировал сигнал!\n\nВремя: $time\nКоординаты: ${"%.4f".format(lat)}, ${"%.4f".format(lon)}",
                "DRINGEND!\n\nRetter $rescuer hat Alarm ausgelöst!\n\nZeit: $time\nKoordinaten: ${"%.4f".format(lat)}, ${"%.4f".format(lon)}"
            ))
            .setPositiveButton(t("CENTRIRAJ NA MAPI", "CENTER ON MAP", "НА КАРТЕ", "AUF KARTE")) { _, _ ->
                if (lat != 0.0 && lon != 0.0) { mapView.controller.animateTo(GeoPoint(lat, lon)); mapView.controller.setZoom(15.0) }
            }
            .setNegativeButton(t("ZATVORI", "CLOSE", "ЗАКРЫТЬ", "SCHLIESSEN"), null)
            .setCancelable(false)
            .show()
    }

    // ─────────────────────────────────────────────
    // LOG I PDF
    // ─────────────────────────────────────────────

    private fun saveMissionLog(status: String, rescuer: String, lat: Double, lon: Double, time: String, found: Boolean) {
        val missionName = getSharedPreferences("savio_prefs", MODE_PRIVATE).getString("teamMissionName", missionCode) ?: missionCode

        db.getReference("active_rescuers").child(missionCode).get().addOnSuccessListener { rescuersSnapshot ->
            val rescuersList = mutableListOf<Map<String, String>>()
            rescuersSnapshot.children.forEach { r ->
                val rName = r.child("name").getValue(String::class.java) ?: ""
                val rPhone = r.child("phone").getValue(String::class.java) ?: ""
                if (rName.isNotEmpty()) rescuersList.add(mapOf("name" to rName, "phone" to rPhone))
            }

            db.getReference("found_persons").child(missionCode).get().addOnSuccessListener { findsSnapshot ->
                val findsList = mutableListOf<Map<String, Any>>()
                findsSnapshot.children.forEach { f ->
                    findsList.add(mapOf(
                        "rescuer" to (f.child("rescuer").getValue(String::class.java) ?: ""),
                        "status" to (f.child("status").getValue(String::class.java) ?: ""),
                        "time" to (f.child("time").getValue(String::class.java) ?: ""),
                        "lat" to (f.child("lat").getValue(Double::class.java) ?: 0.0),
                        "lon" to (f.child("lon").getValue(Double::class.java) ?: 0.0)
                    ))
                }

                db.getReference("mission_chat").child(missionCode).get().addOnSuccessListener { chatSnapshot ->
                    val chatList = mutableListOf<Map<String, String>>()
                    chatSnapshot.children.forEach { msg ->
                        val sender = msg.child("sender").getValue(String::class.java) ?: ""
                        val text = msg.child("text").getValue(String::class.java) ?: ""
                        val msgTime = msg.child("time").getValue(String::class.java) ?: ""
                        if (text.isNotEmpty()) chatList.add(mapOf("sender" to sender, "text" to text, "time" to msgTime))
                    }

                    val endTime = System.currentTimeMillis()
                    val startTime = getSharedPreferences("savio_prefs", MODE_PRIVATE).getLong("teamMissionStartTime", endTime)

                    val result = if (found)
                        t("LICE PRONADJENO — Status: $status — Pronasao: $rescuer — Koordinate: ${"%.5f".format(lat)}, ${"%.5f".format(lon)}", "PERSON FOUND — Status: $status — Found by: $rescuer", "ЧЕЛОВЕК НАЙДЕН — Статус: $status", "PERSON GEFUNDEN — Status: $status")
                    else
                        t("POTRAGA ZAVRŠENA BEZ NALAZA", "MISSION ENDED WITHOUT FIND", "ОПЕРАЦИЯ ЗАВЕРШЕНА БЕЗ НАХОДКИ", "EINSATZ OHNE FUND BEENDET")

                    val logData = mapOf("missionCode" to missionCode, "missionName" to missionName, "endedAt" to endTime, "result" to result, "rescuers" to rescuersList, "finds" to findsList)

                    db.getReference("mission_logs").child(missionCode).setValue(logData)
                    db.getReference("missions").child(missionCode).child("endedAt").setValue(endTime)

                    generateMissionPdf(missionName, missionCode, rescuerName, startTime, endTime, rescuersList, findsList, chatList, result)
                }
            }
        }
    }

    private fun generateMissionPdf(missionName: String, missionCode: String, coordinator: String, startTime: Long, endTime: Long, rescuersList: List<Map<String, String>>, findsList: List<Map<String, Any>>, chatList: List<Map<String, String>>, result: String) {
        try {
            val pdfDocument = android.graphics.pdf.PdfDocument()
            val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            val paintTitle = Paint().apply { textSize = 22f; color = Color.rgb(0, 100, 180); typeface = Typeface.DEFAULT_BOLD }
            val paintHeader = Paint().apply { textSize = 14f; color = Color.BLACK; typeface = Typeface.DEFAULT_BOLD }
            val paintText = Paint().apply { textSize = 12f; color = Color.BLACK }
            val paintGray = Paint().apply { textSize = 11f; color = Color.rgb(100, 100, 100) }
            val paintLine = Paint().apply { color = Color.rgb(200, 200, 200); strokeWidth = 1f }

            val dateFormat = SimpleDateFormat("dd.MM.yyyy. HH:mm", Locale.getDefault())
            val startFormatted = dateFormat.format(Date(startTime))
            val endFormatted = dateFormat.format(Date(endTime))
            val durationMin = ((endTime - startTime) / 60000).toInt()
            val durationText = if (durationMin >= 60) "${durationMin / 60}h ${durationMin % 60}min" else "${durationMin}min"

            var y = 60f
            val leftMargin = 40f
            val pageWidth = 595f

            fun checkPage() {
                if (y > 800f) {
                    pdfDocument.finishPage(page)
                    val newPageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, pdfDocument.pages.size + 1).create()
                    page = pdfDocument.startPage(newPageInfo)
                    canvas = page.canvas
                    y = 40f
                }
            }

            canvas.drawText("SAVIO SOS — IZVEŠTAJ INTERVENCIJE", leftMargin, y, paintTitle); y += 8f
            canvas.drawLine(leftMargin, y, pageWidth - leftMargin, y, paintLine); y += 24f
            canvas.drawText("Naziv akcije: $missionName", leftMargin, y, paintHeader); y += 20f
            canvas.drawText("Kod: $missionCode", leftMargin, y, paintText); y += 18f
            canvas.drawText("Koordinator: $coordinator", leftMargin, y, paintText); y += 18f
            canvas.drawText("Početak: $startFormatted", leftMargin, y, paintText); y += 18f
            canvas.drawText("Završetak: $endFormatted", leftMargin, y, paintText); y += 18f
            canvas.drawText("Trajanje: $durationText", leftMargin, y, paintText); y += 8f
            canvas.drawLine(leftMargin, y, pageWidth - leftMargin, y, paintLine); y += 20f

            canvas.drawText("SPASIOCI (${rescuersList.size})", leftMargin, y, paintHeader); y += 18f
            rescuersList.forEach { r ->
                checkPage()
                canvas.drawText("•  ${r["name"] ?: ""}     Tel: ${r["phone"] ?: "--"}", leftMargin + 10f, y, paintText); y += 16f
            }
            y += 8f; canvas.drawLine(leftMargin, y, pageWidth - leftMargin, y, paintLine); y += 20f

            canvas.drawText("NALAZI (${findsList.size})", leftMargin, y, paintHeader); y += 18f
            if (findsList.isEmpty()) { canvas.drawText("Nema prijavljenih nalaza.", leftMargin + 10f, y, paintGray); y += 16f }
            else findsList.forEach { f ->
                checkPage()
                canvas.drawText("•  ${f["rescuer"]} — ${f["status"]} (${f["time"]})", leftMargin + 10f, y, paintText); y += 16f
                val fLat = f["lat"] as? Double ?: 0.0
                val fLon = f["lon"] as? Double ?: 0.0
                if (fLat != 0.0) { canvas.drawText("   ${"%.5f".format(fLat)}, ${"%.5f".format(fLon)}", leftMargin + 10f, y, paintGray); y += 14f }
            }
            y += 8f; canvas.drawLine(leftMargin, y, pageWidth - leftMargin, y, paintLine); y += 20f

            canvas.drawText("CHAT EKIPE (${chatList.size} poruka)", leftMargin, y, paintHeader); y += 18f
            if (chatList.isEmpty()) { canvas.drawText("Nema chat poruka.", leftMargin + 10f, y, paintGray); y += 16f }
            else chatList.forEach { msg ->
                checkPage()
                canvas.drawText("[${msg["time"]}] ${msg["sender"]}:", leftMargin + 10f, y, paintGray); y += 14f
                val text = msg["text"] ?: ""
                val words = text.split(" ")
                var line = ""
                words.forEach { word ->
                    val testLine = if (line.isEmpty()) word else "$line $word"
                    if (paintText.measureText(testLine) > pageWidth - leftMargin - 30f) {
                        canvas.drawText(line, leftMargin + 20f, y, paintText); y += 14f; line = word; checkPage()
                    } else line = testLine
                }
                if (line.isNotEmpty()) { canvas.drawText(line, leftMargin + 20f, y, paintText); y += 14f }
                y += 4f
            }
            y += 8f; canvas.drawLine(leftMargin, y, pageWidth - leftMargin, y, paintLine); y += 20f

            canvas.drawText("ZAKLJUČAK", leftMargin, y, paintHeader); y += 18f
            canvas.drawText(result, leftMargin + 10f, y, paintText)
            canvas.drawText("Generisano: ${dateFormat.format(Date())}  |  SAVIO SOS  |  Dragan Živanović — Gagi", leftMargin, 820f, paintGray)

            pdfDocument.finishPage(page)

            val fileName = "SAVIO_${missionCode}_izvestaj.pdf"
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.Downloads.MIME_TYPE, "application/pdf")
                    put(android.provider.MediaStore.Downloads.IS_PENDING, 1)
                }
                val uri = contentResolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    contentResolver.openOutputStream(uri)?.use { pdfDocument.writeTo(it) }
                    contentValues.clear()
                    contentValues.put(android.provider.MediaStore.Downloads.IS_PENDING, 0)
                    contentResolver.update(uri, contentValues, null, null)
                }
                pdfDocument.close()
            } else {
                val pdfFile = java.io.File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), fileName)
                java.io.FileOutputStream(pdfFile).use { pdfDocument.writeTo(it) }
                pdfDocument.close()
            }

            runOnUiThread {
                Toast.makeText(this, t("PDF sačuvan u Downloads: $fileName", "PDF saved to Downloads: $fileName", "PDF сохранён: $fileName", "PDF gespeichert: $fileName"), Toast.LENGTH_LONG).show()
            }

        } catch (e: Exception) {
            runOnUiThread { Toast.makeText(this, t("Greška pri generisanju PDF-a.", "Error generating PDF.", "Ошибка PDF.", "PDF-Fehler."), Toast.LENGTH_SHORT).show() }
        }
    }

    // ─────────────────────────────────────────────
    // ZAVRŠETAK POTRAGE + SMS
    // ─────────────────────────────────────────────

    private fun endMissionSuccess(status: String, rescuer: String, lat: Double, lon: Double, time: String) {
        val missionName = getSharedPreferences("savio_prefs", MODE_PRIVATE).getString("teamMissionName", missionCode) ?: missionCode
        val smsText = t(
            "SAVIO POTRAGA — USPJESNO ZAVRSENA\n\nAkcija: $missionName\nKod: $missionCode\n\nLice PRONADJENO!\nStatus: $status\nPronasao: $rescuer\nVrijeme: $time\n\nIzvestaj: Zona za spasioce → Izvestaji akcija",
            "SAVIO MISSION — COMPLETED\n\nMission: $missionName\nCode: $missionCode\n\nPerson FOUND!\nStatus: $status\nFound by: $rescuer\nTime: $time",
            "SAVIO — ОПЕРАЦИЯ ЗАВЕРШЕНА\n\nОперация: $missionName\nКод: $missionCode\n\nЧеловек НАЙДЕН!\nСтатус: $status",
            "SAVIO — EINSATZ ERFOLGREICH\n\nEinsatz: $missionName\nCode: $missionCode\n\nPerson GEFUNDEN!\nStatus: $status"
        )
        sendSmsToAllRescuers(smsText)
        endMission()
    }

    private fun sendSmsToAllRescuers(message: String) {
        try {
            val smsManager = SmsManager.getDefault()
            val parts = smsManager.divideMessage(message)
            rescuerPhones.forEach { phone -> try { smsManager.sendMultipartTextMessage(phone, null, parts, null, null) } catch (_: Exception) {} }
        } catch (_: Exception) {}
    }

    private fun showEndMissionDialog() {
        AlertDialog.Builder(this)
            .setTitle(t("ZAVRSI POTRAGU", "END MISSION", "ЗАВЕРШИТЬ", "BEENDEN"))
            .setMessage(t("Zavrsavate potragu bez nalaza?\n\nSvi spasioci ce dobiti SMS.", "Ending without finding the person?\n\nAll rescuers will receive SMS.", "Завершаете без находки?\n\nВсе получат SMS.", "Ohne Fund beenden?\n\nAlle erhalten SMS."))
            .setPositiveButton(t("ZAVRSI BEZ NALAZA", "END WITHOUT FIND", "ЗАВЕРШИТЬ", "BEENDEN")) { _, _ ->
                saveMissionLog("", "", 0.0, 0.0, "", false)
                endMissionNoFind()
            }
            .setNegativeButton(t("ODUSTANI", "CANCEL", "ОТМЕНА", "ABBRECHEN"), null)
            .show()
    }

    private fun endMissionNoFind() {
        val missionName = getSharedPreferences("savio_prefs", MODE_PRIVATE).getString("teamMissionName", missionCode) ?: missionCode
        val smsText = t(
            "SAVIO POTRAGA — ZAVRSENA\n\nAkcija: $missionName\nKod: $missionCode\n\nLice NIJE pronadjeno.\n\nHvala svim spasiocima.",
            "SAVIO MISSION — ENDED\n\nMission: $missionName\nCode: $missionCode\n\nPerson NOT found.",
            "SAVIO — ОПЕРАЦИЯ ЗАВЕРШЕНА\n\nОперация: $missionName\nЧеловек НЕ найден.",
            "SAVIO — EINSATZ BEENDET\n\nEinsatz: $missionName\nPerson NICHT gefunden."
        )
        sendSmsToAllRescuers(smsText)
        endMission()
    }

    private fun endMission() {
        db.getReference("missions").child(missionCode).child("active").setValue(false)
        db.getReference("active_rescuers").child(missionCode).removeValue()
        db.getReference("sectors").child(missionCode).removeValue()
        clearTeamPrefs()
        Toast.makeText(this, t("Potraga je završena.", "Mission ended.", "Операция завершена.", "Einsatz beendet."), Toast.LENGTH_LONG).show()
        finish()
    }

    private fun showLeaveMissionDialog() {
        AlertDialog.Builder(this)
            .setTitle(t("NAPUSTI POTRAGU", "LEAVE MISSION", "ПОКИНУТЬ", "VERLASSEN"))
            .setMessage(t("Da li ste sigurni?", "Are you sure?", "Вы уверены?", "Sind Sie sicher?"))
            .setPositiveButton(t("NAPUSTI", "LEAVE", "ПОКИНУТЬ", "VERLASSEN")) { _, _ -> leaveMission() }
            .setNegativeButton(t("ODUSTANI", "CANCEL", "ОТМЕНА", "ABBRECHEN"), null)
            .show()
    }

    private fun leaveMission() {
        val safeName = rescuerName.replace(".", "_").replace("#", "_").replace("$", "_").replace("[", "_").replace("]", "_")
        db.getReference("active_rescuers").child(missionCode).child(safeName).removeValue()
        clearTeamPrefs()
        finish()
    }

    private fun clearTeamPrefs() {
        getSharedPreferences("savio_prefs", MODE_PRIVATE).edit()
            .remove("teamMissionCode").remove("teamMissionName")
            .remove("teamIsCoordinator").remove("teamIsObserver")
            .remove("teamMissionStartTime").apply()
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { super.onPause(); mapView.onPause() }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        rescuersListener?.let { db.getReference("active_rescuers").child(missionCode).removeEventListener(it) }
        chatListener?.let { db.getReference("mission_chat").child(missionCode).removeEventListener(it) }
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationManager.removeUpdates(this)
        mapView.onDetach()
    }

    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
    @Deprecated("Deprecated in Java") override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

    private fun applyWindowInsets() {
        val rootView = window.decorView.findViewById<android.view.View>(android.R.id.content)
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }
    }

    private fun currentLanguage(): String {
        return getSharedPreferences("savio_prefs", MODE_PRIVATE).getString("language", "sr") ?: "sr"
    }

    private fun t(sr: String, en: String, ru: String, de: String): String {
        return when (currentLanguage()) { "en" -> en; "ru" -> ru; "de" -> de; else -> sr }
    }
}
