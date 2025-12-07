package com.example.authentication

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.authentication.databinding.ActivityAddMedicationBinding
import com.example.authentication.domain.model.MedicationDraft
import com.example.authentication.reminders.ReminderScheduler
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AddMedicationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddMedicationBinding
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val selectedTimes = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddMedicationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupDatePicker(binding.inputStartDate)
        setupDatePicker(binding.inputEndDate)
        setupTimePicker()

        binding.buttonSave.setOnClickListener {
            val draft = buildDraft() ?: return@setOnClickListener
            val app = application as MedTrackApp
            lifecycleScope.launch {
                val medicationId = app.medicationRepository.createMedication(draft)
                val medication = app.medicationRepository.getMedication(medicationId)
                if (medication != null) {
                    ReminderScheduler(this@AddMedicationActivity).scheduleMedication(medication)
                }
                Toast.makeText(this@AddMedicationActivity, "Medication added", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        binding.buttonCancel.setOnClickListener {
            finish()
        }
    }

    private fun setupTimePicker() {
        binding.inputTimes.isFocusable = false
        binding.inputTimes.isClickable = true
        binding.inputTimes.setOnClickListener { showTimePicker() }
        binding.inputTimes.setOnLongClickListener {
            selectedTimes.clear()
            updateTimesField()
            Toast.makeText(this, "Cleared times", Toast.LENGTH_SHORT).show()
            true
        }
    }

    private fun showTimePicker() {
        val cal = Calendar.getInstance()
        val dialog = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                val formatted24 = String.format(Locale.US, "%02d:%02d", hourOfDay, minute)
                if (!selectedTimes.contains(formatted24)) {
                    selectedTimes.add(formatted24)
                    selectedTimes.sortBy { it }
                    updateTimesField()
                } else {
                    Toast.makeText(this, "Time already added", Toast.LENGTH_SHORT).show()
                }
            },
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            false
        )
        dialog.show()
    }

    private fun updateTimesField() {
        val displayTimes = selectedTimes.mapNotNull { to12Hour(it) }
        binding.inputTimes.setText(displayTimes.joinToString(", "))
    }

    private fun setupDatePicker(target: EditText) {
        target.setOnClickListener { showDatePicker(target) }
    }

    private fun showDatePicker(target: EditText) {
        val calendar = Calendar.getInstance()
        val existingDate = parseDate(target.text.toString().trim())
        if (existingDate != null) {
            calendar.timeInMillis = existingDate
        }

        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                target.setText(dateFormat.format(calendar.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun buildDraft(): MedicationDraft? {
        val name = binding.inputName.text.toString().trim()
        val dosage = binding.inputDosage.text.toString().trim()
        val stomach = binding.inputStomach.text.toString().trim().ifEmpty { "After food" }
        val startDate = parseDate(binding.inputStartDate.text.toString().trim())
        val endDate = binding.inputEndDate.text.toString().trim().takeIf { it.isNotEmpty() }?.let { parseDate(it) }
        val frequencyType = binding.inputFrequency.text.toString().trim().ifEmpty { "daily" }
        val frequencyValue = binding.inputFrequencyValue.text.toString().trim().toIntOrNull()
        val notes = binding.inputNotes.text.toString().trim().ifEmpty { null }
        val selectedDays = binding.inputSelectedDays.text.toString().split(",").mapNotNull { it.trim().toIntOrNull() }.ifEmpty { null }
        val times = selectedTimes.toList()

        if (name.isEmpty() || dosage.isEmpty() || startDate == null || times.isEmpty()) {
            Toast.makeText(this, "Name, dosage, start date, and times are required.", Toast.LENGTH_SHORT).show()
            return null
        }

        if (endDate != null && endDate < startDate) {
            Toast.makeText(this, "End date must be on or after start date.", Toast.LENGTH_SHORT).show()
            return null
        }

        return MedicationDraft(
            name = name,
            dosage = dosage,
            stomachCondition = stomach,
            startDateMillis = startDate,
            endDateMillis = endDate,
            frequencyType = frequencyType,
            frequencyValue = frequencyValue,
            notes = notes,
            selectedDays = selectedDays,
            timesOfDay = times
        )
    }

    private fun parseDate(input: String): Long? {
        if (input.isEmpty()) return null
        return try {
            dateFormat.parse(input)?.time
        } catch (e: Exception) {
            Toast.makeText(this, "Use date format yyyy-MM-dd", Toast.LENGTH_SHORT).show()
            null
        }
    }

    private fun to12Hour(time24: String): String? {
        return try {
            val parts = time24.split(":")
            if (parts.size != 2) return null
            val hour = parts[0].toInt()
            val minute = parts[1].toInt()
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            SimpleDateFormat("h:mm a", Locale.US).format(Date(cal.timeInMillis))
        } catch (_: Exception) {
            null
        }
    }
}
