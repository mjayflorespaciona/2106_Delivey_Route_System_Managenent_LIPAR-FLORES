package com.example.delitrack.backEnd.data

import org.osmdroid.util.GeoPoint
import java.util.Locale

/**
 * Geographic Geocoder containing actual GPS coordinates for all Barangays and Jurisdictions
 * within Nasugbu and Lian, Batangas.
 */
object BatangasLocationGeocoder {

    // Default Fallbacks
    val NASUGBU_TOWN = GeoPoint(14.0754, 120.6311)
    val LIAN_TOWN = GeoPoint(14.0384, 120.6508)

    private val locationRegistry = mapOf(
        // === NASUGBU, BATANGAS JURISDICTIONS & BARANGAYS ===
        "nasugbu" to GeoPoint(14.0754, 120.6311),
        "talangan" to GeoPoint(14.0950, 120.6520),
        "wawa" to GeoPoint(14.0780, 120.6220),
        "bilaran" to GeoPoint(14.0620, 120.6480),
        "tumalim" to GeoPoint(14.1210, 120.6350),
        "banilad" to GeoPoint(14.0880, 120.6610),
        "kay-an" to GeoPoint(14.1020, 120.6280),
        "kay-akpas" to GeoPoint(14.1100, 120.6150),
        "looc" to GeoPoint(14.1620, 120.6120),
        "lumbangan" to GeoPoint(14.0810, 120.6450),
        "catandaan" to GeoPoint(14.0530, 120.6520),
        "poblacion nasugbu" to GeoPoint(14.0754, 120.6311),
        "aga" to GeoPoint(14.1320, 120.6850),
        "balaytigue" to GeoPoint(14.1380, 120.6050),
        "natipuan" to GeoPoint(14.1480, 120.5980),

        // === LIAN, BATANGAS JURISDICTIONS & BARANGAYS ===
        "lian" to GeoPoint(14.0384, 120.6508),
        "binubusan" to GeoPoint(14.0250, 120.6450),
        "balanoy" to GeoPoint(14.0480, 120.6580),
        "bungahan" to GeoPoint(14.0310, 120.6620),
        "canda" to GeoPoint(14.0150, 120.6510),
        "capitolyo" to GeoPoint(14.0390, 120.6480),
        "humayingan" to GeoPoint(14.0110, 120.6680),
        "kapito" to GeoPoint(14.0420, 120.6650),
        "malaruhatan" to GeoPoint(14.0530, 120.6600),
        "matabungkay" to GeoPoint(13.9650, 120.6350),
        "san diego" to GeoPoint(13.9920, 120.6480),
        "poblacion lian" to GeoPoint(14.0384, 120.6508),
        "prenza" to GeoPoint(14.0050, 120.6550),
        "puting kahoy" to GeoPoint(14.0450, 120.6720)
    )

    fun geocodeAddress(address: String?, isPickup: Boolean = true): GeoPoint {
        if (address.isNullOrEmpty()) {
            return if (isPickup) NASUGBU_TOWN else LIAN_TOWN
        }

        val cleaned = address.lowercase(Locale.ROOT)

        // Search registry for matching Barangay or Town keyword
        for ((key, point) in locationRegistry) {
            if (cleaned.contains(key)) {
                return point
            }
        }

        // Default fallbacks based on municipality mention
        return when {
            cleaned.contains("lian") -> LIAN_TOWN
            cleaned.contains("nasugbu") -> NASUGBU_TOWN
            else -> if (isPickup) NASUGBU_TOWN else LIAN_TOWN
        }
    }
}
