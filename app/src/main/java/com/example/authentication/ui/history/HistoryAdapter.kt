package com.example.authentication.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.authentication.data.local.entity.IntakeLogEntity
import com.example.authentication.databinding.ItemHistoryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter(
    private var items: List<IntakeLogEntity>
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    private val dateFormat = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.US)

    fun submitList(newItems: List<IntakeLogEntity>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistoryViewHolder(binding, dateFormat)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(items[position])
    }

    class HistoryViewHolder(
        private val binding: ItemHistoryBinding,
        private val dateFormat: SimpleDateFormat
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: IntakeLogEntity) {
            val scheduled = dateFormat.format(Date(item.scheduledTime))
            binding.textHistoryDate.text = scheduled
            binding.textHistoryMed.text = "Medicine ID: ${item.medicationId}"
            val statusText = item.status + (item.takenTime?.let { " at ${dateFormat.format(Date(it))}" } ?: "")
            binding.textHistoryStatus.text = statusText
        }
    }
}
