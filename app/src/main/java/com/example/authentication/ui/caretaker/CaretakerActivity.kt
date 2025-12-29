package com.example.authentication.ui.caretaker

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.authentication.MainActivity
import com.example.authentication.MedTrackApp
import com.example.authentication.R
import com.example.authentication.data.export.PatientDataExporter
import com.example.authentication.data.local.entity.UserEntity
import com.example.authentication.databinding.ActivityCaretakerBinding
import com.example.authentication.reminders.ReminderScheduler
import com.example.authentication.session.UserSessionManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch

class CaretakerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCaretakerBinding
    private lateinit var adapter: CaretakerPatientAdapter
    private var selectedPatient: UserEntity? = null

    // Launcher for Import File
    private val pickImportFileLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            handleImportFile(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCaretakerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val email = UserSessionManager.getEmail(this) ?: ""
        // Keep header simple
        binding.loggedInAs.text = email

        setupPatientList()
        setupFab()
    }

    private fun setupPatientList() {
        adapter = CaretakerPatientAdapter(emptyList()) { patient ->
            showPatientActions(patient)
        }
        binding.patientsRecycler.layoutManager = LinearLayoutManager(this)
        binding.patientsRecycler.adapter = adapter

        loadPatients()
    }

    private fun setupFab() {
        binding.fabAddPatient.setOnClickListener {
            showAddPatientDialog()
        }
    }

    private fun loadPatients() {
        val app = application as MedTrackApp
        lifecycleScope.launch {
            val patients = app.userRepository.getPatients()
            adapter.submitList(patients)
        }
    }

    private fun showAddPatientDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_patient, null)
        val emailInput = dialogView.findViewById<EditText>(R.id.dialogPatientEmail)
        val uidInput = dialogView.findViewById<EditText>(R.id.dialogPatientUid)
        val addButton = dialogView.findViewById<Button>(R.id.dialogAddButton)
        val cancelButton = dialogView.findViewById<Button>(R.id.dialogCancelButton)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        // Make background transparent for cleaner look if needed, or stick to standard dialog style
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        // dialog.window?.setBackgroundDrawableResource(R.drawable.status_chip_background) // Re-using a drawable or default

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        // Initial state: Disabled and Gray
        addButton.isEnabled = false
        addButton.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.LTGRAY)

        val watcher = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val email = emailInput.text.toString().trim()
                val uid = uidInput.text.toString().trim()
                val isValid = email.isNotEmpty() && uid.isNotEmpty()
                
                addButton.isEnabled = isValid
                if (isValid) {
                    // Green color
                    addButton.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#4CAF50"))
                } else {
                    // Gray color
                    addButton.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.LTGRAY)
                }
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        }
        
        emailInput.addTextChangedListener(watcher)
        uidInput.addTextChangedListener(watcher)

        addButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val uid = uidInput.text.toString().trim()

            if (email.isEmpty() || uid.isEmpty()) {
                Toast.makeText(this, getString(R.string.caretaker_add_patient_missing_fields), Toast.LENGTH_SHORT).show()
            } else {
                addPatient(email, uid)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun addPatient(email: String, uid: String) {
        val app = application as MedTrackApp
        lifecycleScope.launch {
            app.userRepository.upsertUser(uid, email, "patient")
            loadPatients()
            Toast.makeText(this@CaretakerActivity, "Patient added", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showPatientActions(patient: UserEntity) {
        selectedPatient = patient
        val sheetDialog = BottomSheetDialog(this)
        val sheetView = LayoutInflater.from(this).inflate(R.layout.sheet_patient_actions, null)

        val nameView = sheetView.findViewById<TextView>(R.id.sheetPatientName)
        nameView.text = patient.email

        sheetView.findViewById<TextView>(R.id.actionViewPatient).setOnClickListener {
            sheetDialog.dismiss()
            openPatientView(patient)
        }

        sheetView.findViewById<TextView>(R.id.actionExportData).setOnClickListener {
            sheetDialog.dismiss()
            exportPatientData(patient)
        }

        sheetView.findViewById<TextView>(R.id.actionImportData).setOnClickListener {
            sheetDialog.dismiss()
            // Launch file picker
            pickImportFileLauncher.launch(arrayOf("application/json", "text/json", "text/*"))
        }

        sheetDialog.setContentView(sheetView)
        sheetDialog.show()
    }

    private fun openPatientView(patient: UserEntity) {
        UserSessionManager.setActivePatient(this, patient.uid)
        startActivity(Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        })
    }

    private fun exportPatientData(patient: UserEntity) {
        val app = application as MedTrackApp
        lifecycleScope.launch {
            val exporter = PatientDataExporter(this@CaretakerActivity, app.database)
            try {
                val result = exporter.exportPatient(patient.uid)
                Toast.makeText(this@CaretakerActivity, "Exported to: ${result.filePath}", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this@CaretakerActivity, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleImportFile(uri: Uri) {
        val patient = selectedPatient ?: return
        val app = application as MedTrackApp
        
        // Take permission
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: SecurityException) { }

        lifecycleScope.launch {
            val exporter = PatientDataExporter(this@CaretakerActivity, app.database)
            try {
                val result = exporter.importPatient(patient.uid, uri)
                
                // Reschedule reminders
                val list = app.database.medicationDao().getAllWithTimesForPatient(patient.uid)
                ReminderScheduler(this@CaretakerActivity).scheduleMedications(list)
                
                Toast.makeText(this@CaretakerActivity, "Imported ${result.medications} meds, ${result.logs} logs.", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this@CaretakerActivity, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}