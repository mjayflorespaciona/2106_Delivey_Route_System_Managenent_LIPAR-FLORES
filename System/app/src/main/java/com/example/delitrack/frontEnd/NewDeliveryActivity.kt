package com.example.delitrack.frontEnd

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.delitrack.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class NewDeliveryActivity : AppCompatActivity() {

    private var isSubmitting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_new_delivery)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val backBtn = findViewById<TextView>(R.id.backBtn)

        // Sender Fields
        val senderNameField = findViewById<EditText>(R.id.senderNameField)
        val senderContactField = findViewById<EditText>(R.id.senderContactField)
        val pickupAddressField = findViewById<EditText>(R.id.pickupAddressField)

        // Receiver Fields
        val receiverNameField = findViewById<EditText>(R.id.receiverNameField)
        val receiverContactField = findViewById<EditText>(R.id.receiverContactField)
        val deliveryAddressField = findViewById<EditText>(R.id.deliveryAddressField)

        // Package Fields
        val packageDescField = findViewById<EditText>(R.id.packageDescField)
        val radioLightweight = findViewById<RadioButton>(R.id.radioLightweight)
        val radioHeavy = findViewById<RadioButton>(R.id.radioHeavy)
        val radioBulky = findViewById<RadioButton>(R.id.radioBulky)
        val packageWeightField = findViewById<EditText>(R.id.packageWeightField)
        val numberOfPackagesField = findViewById<EditText>(R.id.numberOfPackagesField)
        val specialHandlingField = findViewById<EditText>(R.id.specialHandlingField)

        val btnCreateDelivery = findViewById<Button>(R.id.btnCreateDelivery)

        // Auto-fill Sender Name, Contact, and Pickup Address from Customer profile
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            FirebaseFirestore.getInstance().collection("users").document(currentUser.uid)
                .get()
                .addOnSuccessListener { doc ->
                    if (!isFinishing && doc != null && doc.exists()) {
                        val name = doc.getString("fullName") ?: ""
                        val phone = doc.getString("phone") ?: doc.getString("contactNumber") ?: ""
                        val addr = doc.getString("address") ?: doc.getString("deliveryAddress") ?: "Nasugbu, Batangas"

                        if (name.isNotEmpty() && senderNameField.text.isEmpty()) {
                            senderNameField.setText(name)
                        }
                        if (phone.isNotEmpty() && senderContactField.text.isEmpty()) {
                            senderContactField.setText(phone)
                        }
                        if (pickupAddressField.text.isEmpty()) {
                            pickupAddressField.setText(if (addr.contains("Nasugbu")) addr else "$addr, Nasugbu, Batangas")
                        }
                    }
                }
        }

        backBtn.setOnClickListener { finish() }

        btnCreateDelivery.setOnClickListener {
            if (isSubmitting) return@setOnClickListener

            // Extract Sender Values
            val senderName = senderNameField.text.toString().trim()
            val senderContact = senderContactField.text.toString().trim()
            val pickupAddress = pickupAddressField.text.toString().trim()

            // Extract Receiver Values
            val receiverName = receiverNameField.text.toString().trim()
            val receiverContact = receiverContactField.text.toString().trim()
            val deliveryAddress = deliveryAddressField.text.toString().trim()

            // Extract Package Values
            val packageDesc = packageDescField.text.toString().trim()
            val packageClassification = when {
                radioHeavy.isChecked -> "Heavy"
                radioBulky.isChecked -> "Bulky"
                else -> "Lightweight"
            }
            val weightStr = packageWeightField.text.toString().trim()
            val numPackagesStr = numberOfPackagesField.text.toString().trim()
            val specialHandling = specialHandlingField.text.toString().trim()

            // Field Validation
            if (senderName.isEmpty() || senderContact.isEmpty() || pickupAddress.isEmpty() ||
                receiverName.isEmpty() || receiverContact.isEmpty() || deliveryAddress.isEmpty() ||
                packageDesc.isEmpty() || weightStr.isEmpty()) {
                Toast.makeText(this, "Please fill in all required fields (*)", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val weightKg = weightStr.toDoubleOrNull() ?: 1.0
            val numPackages = numPackagesStr.toIntOrNull() ?: 1

            // Prevent duplicate submission
            isSubmitting = true
            btnCreateDelivery.isEnabled = false

            val deliveryId = "DLV-${System.currentTimeMillis().toString().takeLast(5)}"
            val db = FirebaseFirestore.getInstance()

            val deliveryMap = hashMapOf(
                "deliveryId" to deliveryId,
                // Sender Information
                "senderName" to senderName,
                "senderContact" to senderContact,
                "pickupAddress" to pickupAddress,
                "pickupLocation" to pickupAddress,
                // Receiver Information
                "receiverName" to receiverName,
                "receiverContact" to receiverContact,
                "deliveryAddress" to deliveryAddress,
                "destination" to deliveryAddress,
                // Customer Mapping for queries
                "customerName" to senderName,
                "contactNumber" to senderContact,
                // Package Information
                "packageDescription" to packageDesc,
                "packageType" to packageClassification,
                "packageWeightKg" to weightKg,
                "numberOfPackages" to numPackages,
                "packageDimensions" to "Standard Parcel",
                "specialHandling" to specialHandling,
                "deliveryNotes" to specialHandling,
                // Route Information
                "routeName" to "Nasugbu - Lian Route",
                // Initial status set to PENDING
                "status" to "Pending",
                "dateCreated" to "Today",
                "timestamp" to System.currentTimeMillis(),
                "customerId" to (currentUser?.uid ?: "default_customer"),
                "customerEmail" to (currentUser?.email ?: "")
            )

            db.collection("deliveries").document(deliveryId)
                .set(deliveryMap)
                .addOnSuccessListener {
                    isSubmitting = false
                    btnCreateDelivery.isEnabled = true
                    Toast.makeText(this, "Delivery Request $deliveryId Created! Generating Receipt...", Toast.LENGTH_LONG).show()

                    val intent = Intent(this, DeliveryReceiptActivity::class.java)
                    intent.putExtra("deliveryId", deliveryId)
                    startActivity(intent)
                    finish()
                }
                .addOnFailureListener { e ->
                    isSubmitting = false
                    btnCreateDelivery.isEnabled = true
                    Toast.makeText(this, "Failed to submit request: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }
}
