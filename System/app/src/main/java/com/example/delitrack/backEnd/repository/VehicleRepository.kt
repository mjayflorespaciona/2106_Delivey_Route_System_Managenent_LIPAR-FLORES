package com.example.delitrack.backEnd.repository

import com.example.delitrack.backEnd.data.Vehicle
import com.google.firebase.firestore.FirebaseFirestore

object VehicleRepository {

    private val db by lazy { FirebaseFirestore.getInstance() }

    fun checkDuplicatePlate(
        vehicles: List<Vehicle>,
        plateNumber: String,
        currentVehicleId: String? = null
    ): Boolean {
        return vehicles.any { v ->
            v.vehicleId != currentVehicleId && v.plateNumber.equals(plateNumber.trim(), ignoreCase = true)
        }
    }

    fun updateVehicleStatus(
        vehicleId: String,
        newStatus: String,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        db.collection("vehicles").document(vehicleId)
            .update("status", newStatus)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }
}
