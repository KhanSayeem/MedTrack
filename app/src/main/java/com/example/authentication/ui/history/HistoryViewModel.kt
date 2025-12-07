package com.example.authentication.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.authentication.data.repository.IntakeLogRepository
import com.example.authentication.data.local.entity.IntakeLogEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.Calendar

class HistoryViewModel(
    private val intakeLogRepository: IntakeLogRepository
) : ViewModel() {

    private val _logs = MutableStateFlow<List<IntakeLogEntity>>(emptyList())
    val logs: StateFlow<List<IntakeLogEntity>> = _logs.asStateFlow()

    init {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis - 7L * 24 * 60 * 60 * 1000 // past week
        val end = System.currentTimeMillis()
        intakeLogRepository.observeLogsBetween(start, end)
            .onEach { _logs.value = it }
            .launchIn(viewModelScope)
    }

    class Factory(
        private val intakeLogRepository: IntakeLogRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return HistoryViewModel(intakeLogRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
