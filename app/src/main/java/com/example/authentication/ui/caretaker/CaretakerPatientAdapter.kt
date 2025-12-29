package com.example.authentication.ui.caretaker

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.authentication.data.local.entity.UserEntity
import com.example.authentication.databinding.ItemPatientBinding

class CaretakerPatientAdapter(
    private var patients: List<UserEntity>,
    private val onSelect: (UserEntity) -> Unit
) : RecyclerView.Adapter<CaretakerPatientAdapter.PatientViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatientViewHolder {
        val binding = ItemPatientBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PatientViewHolder(binding)
    }

    override fun getItemCount(): Int = patients.size

    override fun onBindViewHolder(holder: PatientViewHolder, position: Int) {
        holder.bind(patients[position])
    }

    fun submitList(newPatients: List<UserEntity>) {
        patients = newPatients
        notifyDataSetChanged()
    }

    inner class PatientViewHolder(private val binding: ItemPatientBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(patient: UserEntity) {
            binding.patientEmail.text = patient.email
            binding.patientRole.text = patient.role
            binding.root.setOnClickListener { onSelect(patient) }
        }
    }
}
