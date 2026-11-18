package com.example.delitrack.frontEnd

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.delitrack.R
import com.example.delitrack.backEnd.data.BatangasLocationGeocoder
import com.example.delitrack.backEnd.data.MapTilerConfig
import com.google.firebase.firestore.FirebaseFirestore
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.util.Locale

class SelectRouteActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var tvRouteTitle: TextView
    private lateinit var tvPickupText: TextView
    private lateinit var tvDestinationText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = packageName

        enableEdgeToEdge()
        setContentView(R.layout.activity_select_route)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<TextView>(R.id.backBtn).setOnClickListener { finish() }

        tvRouteTitle = findViewById(R.id.tvRouteTitle)
        tvPickupText = findViewById(R.id.tvPickupText)
        tvDestinationText = findViewById(R.id.tvDestinationText)

        mapView = findViewById(R.id.mapView)
        mapView = findViewById(R.id.mapView)
        mapView.setTileSource(MapTilerConfig.getDefaultOpenStreetMapSource())
        mapView.setMultiTouchControls(true)

        val deliveryId = intent.getStringExtra("deliveryId")
        if (!deliveryId.isNullOrEmpty()) {
            loadDeliveryDetails(deliveryId)
        } else {
            setupRouteMap("Nasugbu, Batangas", "Lian, Batangas")
        }

        findViewById<Button>(R.id.btnUseThisRoute).setOnClickListener {
            Toast.makeText(this, "Route Selected! Returning to Dashboard", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, DashboardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
    }

    private fun loadDeliveryDetails(deliveryId: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("deliveries").document(deliveryId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val pickup = document.getString("pickupAddress") ?: document.getString("pickupLocation") ?: "Nasugbu, Batangas"
                    val dest = document.getString("destination") ?: document.getString("deliveryAddress") ?: "Lian, Batangas"
                    setupRouteMap(pickup, dest)
                } else {
                    setupRouteMap("Nasugbu, Batangas", "Lian, Batangas")
                }
            }
            .addOnFailureListener {
                setupRouteMap("Nasugbu, Batangas", "Lian, Batangas")
            }
    }

    private fun setupRouteMap(pickupStr: String, destStr: String) {
        tvRouteTitle.text = "$pickupStr ➔ $destStr"
        tvPickupText.text = "Pickup: $pickupStr"
        tvDestinationText.text = "Destination: $destStr"

        // Dynamic Geocoding for any Barangay or Jurisdiction in Nasugbu or Lian, Batangas
        val pickupPoint = BatangasLocationGeocoder.geocodeAddress(pickupStr, isPickup = true)
        val destPoint = BatangasLocationGeocoder.geocodeAddress(destStr, isPickup = false)

        mapView.overlays.clear()

        // 1. Pickup Location Marker (Dynamic Geocoded Location)
        val pickupMarker = Marker(mapView).apply {
            position = pickupPoint
            title = "Pickup: $pickupStr"
            snippet = "Location: (${String.format(Locale.US, "%.4f, %.4f", pickupPoint.latitude, pickupPoint.longitude)})"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        mapView.overlays.add(pickupMarker)

        // 2. Delivery Destination Marker (Dynamic Geocoded Location)
        val destMarker = Marker(mapView).apply {
            position = destPoint
            title = "Destination: $destStr"
            snippet = "Location: (${String.format(Locale.US, "%.4f, %.4f", destPoint.latitude, destPoint.longitude)})"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        mapView.overlays.add(destMarker)

        // 3. Dynamic Route Polyline connecting Pickup and Destination
        val routeLine = Polyline().apply {
            addPoint(pickupPoint)
            val midLat = (pickupPoint.latitude + destPoint.latitude) / 2
            val midLng = (pickupPoint.longitude + destPoint.longitude) / 2
            addPoint(GeoPoint(midLat, midLng))
            addPoint(destPoint)
            outlinePaint.color = Color.parseColor("#0B4F2C")
            outlinePaint.strokeWidth = 10f
        }
        mapView.overlays.add(routeLine)

        // Center map view dynamically between pickup and destination points
        val centerPoint = GeoPoint((pickupPoint.latitude + destPoint.latitude) / 2, (pickupPoint.longitude + destPoint.longitude) / 2)
        mapView.controller.setZoom(12.5)
        mapView.controller.setCenter(centerPoint)
        mapView.invalidate()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }
}
