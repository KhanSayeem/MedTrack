package com.example.authentication.ui.home

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.authentication.R
import com.example.authentication.databinding.ItemMedicationBinding

class HomeAdapter(
    private var items: List<HomeMedicationItem>,
    private val onTaken: (HomeMedicationItem) -> Unit,
    private val onDelete: (HomeMedicationItem) -> Unit
) : RecyclerView.Adapter<HomeAdapter.MedicationViewHolder>() {

    fun submitList(newItems: List<HomeMedicationItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicationViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemMedicationBinding.inflate(inflater, parent, false)
        return MedicationViewHolder(binding, onTaken, onDelete)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: MedicationViewHolder, position: Int) {
        holder.bind(items[position])
    }

    class MedicationViewHolder(
        private val binding: ItemMedicationBinding,
        private val onTaken: (HomeMedicationItem) -> Unit,
        private val onDelete: (HomeMedicationItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: HomeMedicationItem) {
            binding.textTime.text = item.timeOfDay
            binding.textName.text = item.name
            binding.textDosage.text = item.dosage
            binding.textStomach.text = "Stomach: ${item.stomachCondition}"
            binding.textFrequency.text = "Frequency: ${item.frequency}"
            binding.textDates.text = "Active: ${item.dateRange}"
            binding.textTimes.text = "Times: ${item.timesText}"
            binding.textNotes.text = "Notes: ${item.notes ?: "No notes yet"}"

            val statusLabel = statusLabel(item)
            val statusColor = ContextCompat.getColor(binding.root.context, statusColor(item.status))
            binding.textStatus.text = statusLabel
            binding.textStatus.backgroundTintList = ColorStateList.valueOf(statusColor)

            val isTaken = item.status == HomeMedicationItem.Status.TAKEN
            binding.buttonTaken.isEnabled = !isTaken
            binding.buttonTaken.alpha = if (isTaken) 0.5f else 1f
            binding.buttonTaken.setOnClickListener { onTaken(item) }

            binding.buttonDelete.visibility = if (isTaken) View.VISIBLE else View.GONE
            binding.buttonDelete.setOnClickListener { onDelete(item) }
        }

        private fun statusColor(status: HomeMedicationItem.Status): Int {
            return when (status) {
                HomeMedicationItem.Status.TAKEN -> R.color.status_green
                HomeMedicationItem.Status.DUE_SOON -> R.color.status_yellow
                HomeMedicationItem.Status.MISSED -> R.color.status_red
                HomeMedicationItem.Status.SNOOZED -> R.color.status_yellow
                HomeMedicationItem.Status.SKIPPED -> R.color.status_red
                HomeMedicationItem.Status.SCHEDULED -> R.color.status_blue
            }
        }

        private fun statusLabel(item: HomeMedicationItem): String {
            val context = binding.root.context
            return when (item.status) {
                HomeMedicationItem.Status.TAKEN -> context.getString(R.string.status_taken)
                HomeMedicationItem.Status.DUE_SOON -> context.getString(R.string.status_due)
                HomeMedicationItem.Status.MISSED -> context.getString(R.string.status_missed)
                HomeMedicationItem.Status.SNOOZED -> context.getString(R.string.status_snoozed)
                HomeMedicationItem.Status.SKIPPED -> context.getString(R.string.status_skipped)
                HomeMedicationItem.Status.SCHEDULED -> context.getString(R.string.status_scheduled)
            }
        }
    }
}
