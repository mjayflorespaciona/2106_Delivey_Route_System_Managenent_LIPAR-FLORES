package com.example.delitrack.backEnd.repository

import com.example.delitrack.backEnd.data.Delivery
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DeliveryRepository {

    private val db by lazy { FirebaseFirestore.getInstance() }

    fun createDeliveryRequest(
        deliveryMap: Map<String, Any>,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val deliveryId = deliveryMap["deliveryId"] as? String ?: "DLV-${System.currentTimeMillis().toString().takeLast(5)}"
        db.collection("deliveries").document(deliveryId)
            .set(deliveryMap)
            .addOnSuccessListener { onSuccess(deliveryId) }
            .addOnFailureListener { onFailure(it) }
    }

    fun updateDeliveryStatus(
        deliveryId: String,
        newStatus: String,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        val timestampStr = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault()).format(Date())
        val historyEntry = mapOf("status" to newStatus, "timestamp" to timestampStr)

        val updates = mapOf(
            "status" to newStatus,
            "lastUpdated" to timestampStr,
            "statusHistory" to FieldValue.arrayUnion(historyEntry)
        )

        db.collection("deliveries").document(deliveryId)
            .update(updates)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun listenToSingleDeliveryTracking(
        deliveryId: String,
        onSnapshot: (Map<String, Any>?) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return db.collection("deliveries").document(deliveryId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                onSnapshot(snapshot?.data)
            }
    }

    fun fetchAllDeliveries(
        onSuccess: (List<Map<String, Any>>) -> Unit,
        onFailure: (Exception) -> Unit = {}
    ) {
        db.collection("deliveries")
            .get()
            .addOnSuccessListener { result ->
                val list = result.documents.mapNotNull { it.data }
                onSuccess(list)
            }
            .addOnFailureListener { onFailure(it) }
    }
}
