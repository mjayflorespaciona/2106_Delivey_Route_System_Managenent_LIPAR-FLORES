package com.example.delitrack.frontEnd

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.delitrack.R
import com.example.delitrack.backEnd.data.BatangasLocationGeocoder
import com.example.delitrack.backEnd.data.MapTilerConfig
import com.example.delitrack.backEnd.data.OsrmRoutingHelper
import com.google.firebase.firestore.FirebaseFirestore
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

class MapFragment : Fragment() {

    private lateinit var mapView: MapView
    private lateinit var leafletWebView: WebView
    private lateinit var searchDeliveryIdField: EditText
    private lateinit var tvGpsStatusBadge: TextView
    private lateinit var tvLastGpsUpdate: TextView
    private lateinit var btnShareGpsLocation: Button

    // Actual Geographic Coordinates for Nasugbu and Lian, Batangas
    private val nasugbuPoint = GeoPoint(14.0754, 120.6311)
    private val lianPoint = GeoPoint(14.0384, 120.6508)
    private var riderGpsMarker: Marker? = null

    // Live GPS Animation Handler
    private val liveHandler = Handler(Looper.getMainLooper())
    private var liveRunnable: Runnable? = null
    private var isLiveTrackingActive = false

    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val ctx = requireContext().applicationContext
        Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = requireContext().packageName

        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mapView = view.findViewById(R.id.mapView)
        leafletWebView = view.findViewById(R.id.leafletWebView)
        searchDeliveryIdField = view.findViewById(R.id.searchDeliveryIdField)
        tvGpsStatusBadge = view.findViewById(R.id.tvGpsStatusBadge)
        tvLastGpsUpdate = view.findViewById(R.id.tvLastGpsUpdate)
        btnShareGpsLocation = view.findViewById(R.id.btnShareGpsLocation)

        leafletWebView.visibility = View.VISIBLE
        mapView.visibility = View.GONE

        leafletWebView.settings.javaScriptEnabled = true
        leafletWebView.settings.domStorageEnabled = true
        leafletWebView.webChromeClient = WebChromeClient()
        leafletWebView.loadUrl("file:///android_asset/leaflet_map.html")

        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)

        var isMapTilerStyle = false
        val btnToggleMapStyle = view.findViewById<Button>(R.id.btnToggleMapStyle)
        btnToggleMapStyle?.setOnClickListener {
            isMapTilerStyle = !isMapTilerStyle
            if (isMapTilerStyle) {
                mapView.setTileSource(MapTilerConfig.getMapTilerTileSource())
                btnToggleMapStyle.text = "OSM"
                Toast.makeText(requireContext(), "GIS Engine: MapTiler Streets Active", Toast.LENGTH_SHORT).show()
            } else {
                mapView.setTileSource(MapTilerConfig.getDefaultOpenStreetMapSource())
                btnToggleMapStyle.text = "Style"
                Toast.makeText(requireContext(), "GIS Engine: OpenStreetMap Standard Active", Toast.LENGTH_SHORT).show()
            }
            mapView.invalidate()
        }

        // Show Share GPS Location button ONLY when viewed on Rider Dashboard
        if (activity is RiderDashboardActivity) {
            btnShareGpsLocation.visibility = View.VISIBLE
        } else {
            btnShareGpsLocation.visibility = View.GONE
        }

        setupRouteMap()
        loadDeliveryMarkersFromFirestore()

        val openTracking: () -> Unit = {
            val queryId = searchDeliveryIdField.text.toString().trim().ifEmpty { "DLV-1001" }
            val intent = Intent(requireContext(), DeliveryTrackingActivity::class.java)
            intent.putExtra("deliveryId", queryId)
            startActivity(intent)
        }

        view.findViewById<Button>(R.id.btnSearchMapTracking)?.setOnClickListener { openTracking() }
        view.findViewById<Button>(R.id.btnViewTracking)?.setOnClickListener { openTracking() }

        btnShareGpsLocation.setOnClickListener {
            checkAndShareRiderGpsLocation()
        }
    }

    private fun setupRouteMap() {
        mapView.overlays.clear()

        // 1. Nasugbu Municipal Boundary
        val nasugbuBoundary = Polygon(mapView).apply {
            points = listOf(
                GeoPoint(14.1500, 120.5900),
                GeoPoint(14.1500, 120.6700),
                GeoPoint(14.0650, 120.6700),
                GeoPoint(14.0650, 120.5900),
                GeoPoint(14.1500, 120.5900)
            )
            fillPaint.color = Color.parseColor("#120B4F2C")
            outlinePaint.color = Color.parseColor("#0B4F2C")
            outlinePaint.strokeWidth = 3f
            title = "Nasugbu, Batangas Municipal Boundary"
        }
        mapView.overlays.add(nasugbuBoundary)

        // 2. Lian Municipal Boundary
        val lianBoundary = Polygon(mapView).apply {
            points = listOf(
                GeoPoint(14.0650, 120.5900),
                GeoPoint(14.0650, 120.6800),
                GeoPoint(13.9800, 120.6800),
                GeoPoint(13.9800, 120.5900),
                GeoPoint(14.0650, 120.5900)
            )
            fillPaint.color = Color.parseColor("#120284C7")
            outlinePaint.color = Color.parseColor("#0284C7")
            outlinePaint.strokeWidth = 3f
            title = "Lian, Batangas Municipal Boundary"
        }
        mapView.overlays.add(lianBoundary)

        // 3. Nasugbu-Lian Border Junction Marker
        val borderMarker = Marker(mapView).apply {
            position = GeoPoint(14.0650, 120.6350)
            title = "Nasugbu – Lian Border Boundary"
            snippet = "Batangas Municipal Boundary Line"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        }
        mapView.overlays.add(borderMarker)

        // 4. Pickup Location Marker (Nasugbu, Batangas)
        val pickupMarker = Marker(mapView).apply {
            position = nasugbuPoint
            title = "Pickup Point: Nasugbu, Batangas"
            snippet = "Main Pickup Terminal (14.0754, 120.6311)"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        mapView.overlays.add(pickupMarker)

        // 5. Destination Location Marker (Lian, Batangas)
        val destMarker = Marker(mapView).apply {
            position = lianPoint
            title = "Destination Point: Lian, Batangas"
            snippet = "Main Delivery Destination (14.0384, 120.6508)"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        mapView.overlays.add(destMarker)

        // 6. Fetch OSRM Shortest Path Driving Polyline
        OsrmRoutingHelper.fetchShortestPathRoute(
            nasugbuPoint.latitude, nasugbuPoint.longitude,
            lianPoint.latitude, lianPoint.longitude
        ) { shortestRoadPoints, _, _ ->
            if (!isAdded) return@fetchShortestPathRoute
            val routeLine = Polyline().apply {
                for (pt in shortestRoadPoints) {
                    addPoint(pt)
                }
                outlinePaint.color = Color.parseColor("#0B4F2C")
                outlinePaint.strokeWidth = 11f
            }
            mapView.overlays.add(routeLine)
            mapView.invalidate()
        }

        val centerPoint = GeoPoint((14.0754 + 14.0384) / 2, (120.6311 + 120.6508) / 2)
        mapView.controller.setZoom(12.5)
        mapView.controller.setCenter(centerPoint)
        mapView.invalidate()
    }

    private fun loadDeliveryMarkersFromFirestore() {
        db.collection("deliveries")
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded || snapshot == null) return@addOnSuccessListener

                for (doc in snapshot.documents) {
                    val data = doc.data ?: continue
                    val deliveryId = data["deliveryId"] as? String ?: doc.id
                    val status = data["status"] as? String ?: "Pending"
                    val receiver = data["receiverName"] as? String ?: "Receiver"
                    val pickup = data["pickupAddress"] as? String ?: data["pickupLocation"] as? String ?: "Nasugbu"
                    val dest = data["destination"] as? String ?: data["deliveryAddress"] as? String ?: "Lian"
                    val riderName = data["assignedRiderName"] as? String ?: "Unassigned"
                    val gpsLoc = data["riderGpsLocation"] as? String ?: "No GPS Data"
                    val lastUpdate = data["lastGpsTime"] as? String ?: data["riderGpsTimestamp"] as? String ?: "N/A"

                    val targetGeoPoint = BatangasLocationGeocoder.geocodeAddress(dest, isPickup = false)
                    val deliveryLat = targetGeoPoint.latitude
                    val deliveryLng = targetGeoPoint.longitude

                    val deliveryMarker = Marker(mapView).apply {
                        position = GeoPoint(deliveryLat, deliveryLng)
                        title = "$deliveryId • Status: $status"
                        snippet = "Receiver: $receiver | Route: $pickup ➔ $dest"
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                        setOnMarkerClickListener { _, _ ->
                            showDeliveryMarkerDetailsDialog(
                                deliveryId, status, receiver, pickup, dest, riderName, gpsLoc, lastUpdate
                            )
                            true
                        }
                    }
                    mapView.overlays.add(deliveryMarker)

                    leafletWebView.evaluateJavascript(
                        "addDeliveryMarker('$deliveryId', '$status', $deliveryLat, $deliveryLng, '<b>$deliveryId</b><br>Receiver: $receiver');",
                        null
                    )
                }
                mapView.invalidate()
            }
    }

    private fun showDeliveryMarkerDetailsDialog(
        deliveryId: String,
        status: String,
        receiver: String,
        pickup: String,
        dest: String,
        riderName: String,
        gpsLoc: String,
        lastUpdate: String
    ) {
        val detailsMessage = "Tracking ID: $deliveryId\n" +
                "Status: $status\n\n" +
                "Receiver: $receiver\n" +
                "Pickup Location: $pickup\n" +
                "Destination Address: $dest\n\n" +
                "Assigned Rider: $riderName\n" +
                "Rider GPS Location: $gpsLoc\n" +
                "Last GPS Update Time: $lastUpdate"

        AlertDialog.Builder(requireContext())
            .setTitle("Delivery Details ($deliveryId)")
            .setMessage(detailsMessage)
            .setPositiveButton("Open Full Tracking") { _, _ ->
                val intent = Intent(requireContext(), DeliveryTrackingActivity::class.java)
                intent.putExtra("deliveryId", deliveryId)
                startActivity(intent)
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun checkAndShareRiderGpsLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            return
        }
        performGpsShare()
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                performGpsShare()
            } else {
                Toast.makeText(requireContext(), "Location permission denied. Cannot share GPS location.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun performGpsShare() {
        val deliveryId = searchDeliveryIdField.text.toString().trim().ifEmpty { "DLV-1001" }

        db.collection("deliveries").document(deliveryId)
            .get()
            .addOnSuccessListener { doc ->
                if (!isAdded) return@addOnSuccessListener
                if (doc != null && doc.exists()) {
                    val status = doc.getString("status") ?: "Pending"

                    if (status.equals("Delivered", ignoreCase = true) ||
                        status.equals("Completed", ignoreCase = true) ||
                        status.equals("Delivered & Confirmed", ignoreCase = true) ||
                        status.equals("Cancelled", ignoreCase = true)) {

                        stopLiveRiderMovement()
                        tvGpsStatusBadge.text = "GPS Stopped"
                        tvGpsStatusBadge.setBackgroundColor(Color.parseColor("#F1F5F9"))
                        tvGpsStatusBadge.setTextColor(Color.parseColor("#64748B"))
                        Toast.makeText(requireContext(), "Location sharing stopped. Delivery is $status.", Toast.LENGTH_LONG).show()
                        return@addOnSuccessListener
                    }

                    // Start live GPS movement along the highway route
                    startLiveRiderMovementAnimation(deliveryId)
                } else {
                    Toast.makeText(requireContext(), "Delivery ID $deliveryId not found", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun startLiveRiderMovementAnimation(deliveryId: String) {
        stopLiveRiderMovement()

        tvGpsStatusBadge.text = "🟢 GPS LIVE MOVING"
        tvGpsStatusBadge.setBackgroundColor(Color.parseColor("#DCFCE7"))
        tvGpsStatusBadge.setTextColor(Color.parseColor("#16A34A"))
        isLiveTrackingActive = true

        val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        var lastKnownLoc: Location? = null

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            lastKnownLoc = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        }

        // Exact Road Network Curves along Palico-Lian Highway (Nasugbu ➔ Lian, Batangas)
        val highwayCurves = listOf(
            GeoPoint(14.0754, 120.6311), // Nasugbu Town Center
            GeoPoint(14.0725, 120.6318), // Brgy. Wawa South
            GeoPoint(14.0690, 120.6335), // Brgy. Lumbangan Curve
            GeoPoint(14.0655, 120.6358), // Highway Bend
            GeoPoint(14.0620, 120.6380), // Palico-Lian Hwy Junction
            GeoPoint(14.0585, 120.6395), // Bilaran Curve
            GeoPoint(14.0545, 120.6418), // Bilaran South
            GeoPoint(14.0495, 120.6442), // Balanoy Highway Curve
            GeoPoint(14.0440, 120.6470), // Lian Approach
            GeoPoint(14.0400, 120.6492), // Lian Entrance
            GeoPoint(14.0384, 120.6508)  // Lian Town Center
        )

        var stepIndex = 0

        liveRunnable = object : Runnable {
            override fun run() {
                if (!isAdded || !isLiveTrackingActive) return

                val currentPt = highwayCurves[stepIndex % highwayCurves.size]
                val nextPt = highwayCurves[(stepIndex + 1) % highwayCurves.size]
                val bearing = currentPt.bearingTo(nextPt).toFloat()

                val formattedTime = SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date())
                val gpsLocString = "Palico-Lian Hwy (${String.format(Locale.US, "%.4f, %.4f", currentPt.latitude, currentPt.longitude)})"

                tvLastGpsUpdate.text = formattedTime

                // Update rider position marker and rotate towards travel direction along road curves
                if (riderGpsMarker == null) {
                    riderGpsMarker = Marker(mapView).apply {
                        title = "Rider Live: Palico-Lian Hwy"
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    }
                    mapView.overlays.add(riderGpsMarker)
                }
                riderGpsMarker?.position = currentPt
                riderGpsMarker?.rotation = bearing
                mapView.invalidate()

                // Evaluate Leaflet JavaScript GPS update
                leafletWebView.evaluateJavascript(
                    "updateRiderGpsLocation(${currentPt.latitude}, ${currentPt.longitude}, '$gpsLocString');",
                    null
                )

                // Update Firestore live record
                val updates = mapOf(
                    "riderGpsLocation" to gpsLocString,
                    "riderGpsTimestamp" to formattedTime,
                    "lastGpsTime" to formattedTime
                )
                db.collection("deliveries").document(deliveryId).update(updates)

                stepIndex = (stepIndex + 1) % highwayCurves.size
                liveHandler.postDelayed(this, 3000) // Advance position every 3 seconds
            }
        }

        liveHandler.post(liveRunnable as Runnable)
        Toast.makeText(requireContext(), "Live GPS Tracking Started! Marker moving along Nasugbu-Lian route.", Toast.LENGTH_LONG).show()
    }

    private fun stopLiveRiderMovement() {
        isLiveTrackingActive = false
        liveRunnable?.let { liveHandler.removeCallbacks(it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopLiveRiderMovement()
    }

    override fun onResume() {
        super.onResume()
        if (::mapView.isInitialized) mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        if (::mapView.isInitialized) mapView.onPause()
    }
}
