package com.example.delitrack.backEnd.repository

import com.example.delitrack.backEnd.data.Rider
import com.google.firebase.firestore.FirebaseFirestore

object RiderRepository {

    private val db by lazy { FirebaseFirestore.getInstance() }

    fun updateRiderDutyStatus(
        riderId: String,
        newStatus: String,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        db.collection("riders").document(riderId)
            .update("status", newStatus)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun fetchRiderByEmail(
        email: String,
        onSuccess: (Rider?) -> Unit,
        onFailure: (Exception) -> Unit = {}
    ) {
        db.collection("riders")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { query ->
                if (!query.isEmpty) {
                    val doc = query.documents[0]
                    val rider = Rider(
                        riderId = doc.getString("riderId") ?: doc.id,
                        fullName = doc.getString("fullName") ?: "Rider",
                        phone = doc.getString("phone") ?: "",
                        assignedVehicleId = doc.getString("assignedVehicleId"),
                        status = doc.getString("status") ?: "Available",
                        email = doc.getString("email") ?: email
                    )
                    onSuccess(rider)
                } else {
                    onSuccess(null)
                }
            }
            .addOnFailureListener { onFailure(it) }
    }
}
