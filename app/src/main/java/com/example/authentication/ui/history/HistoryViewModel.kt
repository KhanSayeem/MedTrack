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

class HistoryViewModel(
    private val intakeLogRepository: IntakeLogRepository,
    private val patientId: String
) : ViewModel() {

    private val _logs = MutableStateFlow<List<IntakeLogEntity>>(emptyList())
    val logs: StateFlow<List<IntakeLogEntity>> = _logs.asStateFlow()

    init {
        intakeLogRepository.observeAllLogs(patientId)
            .onEach { _logs.value = it }
            .launchIn(viewModelScope)
    }

    class Factory(
        private val intakeLogRepository: IntakeLogRepository,
        private val patientId: String
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return HistoryViewModel(intakeLogRepository, patientId) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
