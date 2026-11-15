package com.example.delitrack.frontEnd

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.delitrack.R
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReportsFragment : Fragment() {

    private lateinit var tvTotalRequests: TextView
    private lateinit var tvCompletedDeliveries: TextView
    private lateinit var tvPendingDeliveries: TextView
    private lateinit var tvFailedDeliveries: TextView
    private lateinit var tvCancelledDeliveries: TextView
    private lateinit var tvRiderAssignments: TextView
    private lateinit var tvVehicleUsage: TextView

    private lateinit var tvDeliveredPercent: TextView
    private lateinit var tvInTransitPercent: TextView
    private lateinit var tvIssuesPercent: TextView

    private lateinit var tvDateFilter: TextView
    private lateinit var searchReportIdField: EditText
    private lateinit var containerDateRecords: LinearLayout
    private lateinit var reportsPrintContainer: LinearLayout

    private var currentFilterOption = "All Time"
    private var searchQuery = ""
    private val rawDeliveries = mutableListOf<Map<String, Any>>()
    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_reports, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvTotalRequests = view.findViewById(R.id.tvTotalRequests)
        tvCompletedDeliveries = view.findViewById(R.id.tvCompletedDeliveries)
        tvPendingDeliveries = view.findViewById(R.id.tvPendingDeliveries)
        tvFailedDeliveries = view.findViewById(R.id.tvFailedDeliveries)
        tvCancelledDeliveries = view.findViewById(R.id.tvCancelledDeliveries)
        tvRiderAssignments = view.findViewById(R.id.tvRiderAssignments)
        tvVehicleUsage = view.findViewById(R.id.tvVehicleUsage)

        tvDeliveredPercent = view.findViewById(R.id.tvDeliveredPercent)
        tvInTransitPercent = view.findViewById(R.id.tvInTransitPercent)
        tvIssuesPercent = view.findViewById(R.id.tvIssuesPercent)

        tvDateFilter = view.findViewById(R.id.tvDateFilter)
        searchReportIdField = view.findViewById(R.id.searchReportIdField)
        containerDateRecords = view.findViewById(R.id.containerDateRecords)
        reportsPrintContainer = view.findViewById(R.id.reportsPrintContainer)

        tvDateFilter.setOnClickListener {
            showDateFilterDialog()
        }

        searchReportIdField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString()?.trim() ?: ""
                filterAndRenderReportData()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        view.findViewById<Button>(R.id.btnExportPdf)?.setOnClickListener {
            exportReportToPdf()
        }

        view.findViewById<Button>(R.id.btnExportCsv)?.setOnClickListener {
            exportReportToCsv()
        }

        loadReportDataFromFirestore()
    }

    private fun showDateFilterDialog() {
        val options = arrayOf("All Time", "Today", "This Week", "This Month")
        AlertDialog.Builder(requireContext())
            .setTitle("Select Date Range Filter")
            .setItems(options) { _, which ->
                currentFilterOption = options[which]
                tvDateFilter.text = "$currentFilterOption ▾"
                filterAndRenderReportData()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadReportDataFromFirestore() {
        db.collection("deliveries")
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded || snapshot == null) return@addOnSuccessListener
                rawDeliveries.clear()
                for (doc in snapshot.documents) {
                    val data = doc.data ?: continue
                    rawDeliveries.add(data)
                }

                // Also query vehicle usage
                db.collection("vehicles").whereEqualTo("status", "In Use")
                    .get()
                    .addOnSuccessListener { vSnap ->
                        if (!isAdded) return@addOnSuccessListener
                        val inUseCount = vSnap.size()
                        tvVehicleUsage.text = inUseCount.toString()
                    }

                filterAndRenderReportData()
            }
    }

    private fun filterAndRenderReportData() {
        if (!isAdded) return

        var total = 0
        var completed = 0
        var pending = 0
        var failed = 0
        var cancelled = 0
        var inTransit = 0
        var riderJobs = 0

        containerDateRecords.removeAllViews()

        val filtered = rawDeliveries.filter { data ->
            val deliveryId = (data["deliveryId"] as? String ?: "").lowercase()
            val dateStr = data["dateCreated"] as? String ?: "Today"
            val matchesSearch = searchQuery.isEmpty() || deliveryId.contains(searchQuery.lowercase())

            val matchesDate = when (currentFilterOption) {
                "Today" -> dateStr.equals("Today", ignoreCase = true) || dateStr.contains(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
                "This Week" -> true
                "This Month" -> true
                else -> true
            }

            matchesSearch && matchesDate
        }

        for (data in filtered) {
            total++
            val status = data["status"] as? String ?: "Pending"
            val deliveryId = data["deliveryId"] as? String ?: "DLV-XXXX"
            val receiver = data["receiverName"] as? String ?: "N/A"
            val pickup = data["pickupLocation"] as? String ?: data["pickupAddress"] as? String ?: "Nasugbu"
            val dest = data["destination"] as? String ?: data["deliveryAddress"] as? String ?: "Lian"
            val date = data["dateCreated"] as? String ?: "Today"
            val rider = data["assignedRiderName"] as? String ?: ""

            if (rider.isNotEmpty()) riderJobs++

            when (status.lowercase()) {
                "pending" -> pending++
                "delivered", "completed", "delivered & confirmed" -> completed++
                "in transit", "dispatched", "active", "assigned to rider", "assigned", "preparing", "ready for pickup" -> inTransit++
                "confirmed", "acknowledged" -> pending++
                else -> {
                    if (status.lowercase().contains("failed")) failed++
                    else if (status.lowercase().contains("cancelled") || status.lowercase().contains("issue")) cancelled++
                    else pending++
                }
            }

            // Render audit item card
            val itemCard = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundResource(android.R.drawable.dialog_holo_light_frame)
                setPadding(20, 16, 20, 16)
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                lp.setMargins(0, 0, 0, 10)
                layoutParams = lp

                setOnClickListener {
                    val intent = Intent(requireContext(), DeliveryTrackingActivity::class.java)
                    intent.putExtra("deliveryId", deliveryId)
                    startActivity(intent)
                }
            }

            val row1 = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL }
            val tvId = TextView(requireContext()).apply {
                text = "$deliveryId • $date"
                setTextColor(Color.parseColor("#111111"))
                setTypeface(null, Typeface.BOLD)
                textSize = 14f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val tvSt = TextView(requireContext()).apply {
                text = status
                textSize = 11f
                setTypeface(null, Typeface.BOLD)
                setPadding(12, 4, 12, 4)
                if (status.lowercase().contains("delivered") || status.lowercase().contains("completed")) {
                    setBackgroundColor(Color.parseColor("#DCFCE7"))
                    setTextColor(Color.parseColor("#16A34A"))
                } else if (status.lowercase().contains("failed") || status.lowercase().contains("cancelled")) {
                    setBackgroundColor(Color.parseColor("#FEE2E2"))
                    setTextColor(Color.parseColor("#DC2626"))
                } else {
                    setBackgroundColor(Color.parseColor("#E0F2FE"))
                    setTextColor(Color.parseColor("#0284C7"))
                }
            }

            row1.addView(tvId)
            row1.addView(tvSt)
            itemCard.addView(row1)

            val tvInfo = TextView(requireContext()).apply {
                text = "Receiver: $receiver | Route: $pickup ➔ $dest"
                setTextColor(Color.parseColor("#555555"))
                textSize = 12f
                setPadding(0, 4, 0, 0)
            }
            itemCard.addView(tvInfo)

            containerDateRecords.addView(itemCard)
        }

        // Render Summary Numbers
        tvTotalRequests.text = total.toString()
        tvCompletedDeliveries.text = completed.toString()
        tvPendingDeliveries.text = pending.toString()
        tvFailedDeliveries.text = failed.toString()
        tvCancelledDeliveries.text = cancelled.toString()
        tvRiderAssignments.text = riderJobs.toString()

        // Render Summary Percentages
        val pctDelivered = if (total > 0) (completed * 100) / total else 0
        val pctInTransit = if (total > 0) (inTransit * 100) / total else 0
        val pctIssues = if (total > 0) ((failed + cancelled) * 100) / total else 0

        tvDeliveredPercent.text = "$completed ($pctDelivered%)"
        tvInTransitPercent.text = "$inTransit ($pctInTransit%)"
        tvIssuesPercent.text = "${failed + cancelled} ($pctIssues%)"
    }

    private fun exportReportToPdf() {
        val printManager = requireContext().getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager != null) {
            val jobName = "RoutGo_Delivery_Report_${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}"
            val printAdapter = object : PrintDocumentAdapter() {
                override fun onLayout(
                    oldAttributes: PrintAttributes?,
                    newAttributes: PrintAttributes?,
                    cancellationSignal: CancellationSignal?,
                    callback: LayoutResultCallback?,
                    extras: Bundle?
                ) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback?.onLayoutCancelled()
                        return
                    }
                    val info = PrintDocumentInfo.Builder(jobName)
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .setPageCount(1)
                        .build()
                    callback?.onLayoutFinished(info, true)
                }

                override fun onWrite(
                    pages: Array<out PageRange>?,
                    destination: ParcelFileDescriptor?,
                    cancellationSignal: CancellationSignal?,
                    callback: WriteResultCallback?
                ) {
                    try {
                        val pdfDocument = PdfDocument()
                        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
                        val page = pdfDocument.startPage(pageInfo)

                        val canvas = page.canvas
                        reportsPrintContainer.draw(canvas)
                        pdfDocument.finishPage(page)

                        destination?.let {
                            pdfDocument.writeTo(FileOutputStream(it.fileDescriptor))
                        }
                        pdfDocument.close()
                        callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                    } catch (e: Exception) {
                        callback?.onWriteFailed(e.message)
                    }
                }
            }
            printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
            Toast.makeText(requireContext(), "Opening Print / Save PDF Manager...", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "PDF service unavailable on this device", Toast.LENGTH_SHORT).show()
        }
    }

    private fun exportReportToCsv() {
        try {
            val csvHeader = "Tracking ID,Date,Status,Receiver,Pickup,Destination\n"
            val csvRows = rawDeliveries.joinToString("\n") { data ->
                val id = data["deliveryId"] as? String ?: ""
                val date = data["dateCreated"] as? String ?: ""
                val status = data["status"] as? String ?: ""
                val receiver = (data["receiverName"] as? String ?: "").replace(",", " ")
                val pickup = (data["pickupAddress"] as? String ?: data["pickupLocation"] as? String ?: "").replace(",", " ")
                val dest = (data["destination"] as? String ?: data["deliveryAddress"] as? String ?: "").replace(",", " ")

                "$id,$date,$status,$receiver,$pickup,$dest"
            }

            val csvContent = csvHeader + csvRows
            val cacheFile = File(requireContext().cacheDir, "RoutGo_Delivery_Report.csv")
            FileOutputStream(cacheFile).use { it.write(csvContent.toByteArray()) }

            Toast.makeText(requireContext(), "CSV Audit File Exported Successfully! (${rawDeliveries.size} records)", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "CSV Export Failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
