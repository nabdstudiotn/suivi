package com.example.ui.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AttendanceRepository
import com.example.data.sync.SyncState
import com.example.domain.model.DailyAttendance
import com.example.domain.model.Worker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class AttendanceUiState(
  val selectedDate: LocalDate = LocalDate.now(ZoneId.of("Africa/Tunis")),
  val workers: List<Worker> = emptyList(),
  val attendanceRecords: Map<String, DailyAttendance> = emptyMap(),
  val syncState: SyncState = SyncState.LocalOnly,
  val activeTab: String = "POINTAGE"
)

class AttendanceViewModel(
  private val repository: AttendanceRepository
) : ViewModel() {

  private val _uiState = MutableStateFlow(AttendanceUiState())
  val uiState: StateFlow<AttendanceUiState> = _uiState.asStateFlow()

  init {
    viewModelScope.launch {
      repository.getAllWorkers().collect { workersList ->
        _uiState.update { it.copy(workers = workersList) }
      }
    }
    viewModelScope.launch {
      repository.syncState.collect { sync ->
        _uiState.update { it.copy(syncState = sync) }
      }
    }
    loadAttendanceForDate(_uiState.value.selectedDate)
  }

  fun onDateSelected(date: LocalDate) {
    _uiState.update { it.copy(selectedDate = date) }
    loadAttendanceForDate(date)
  }

  fun setTab(tab: String) {
    _uiState.update { it.copy(activeTab = tab) }
  }

  private fun loadAttendanceForDate(date: LocalDate) {
    viewModelScope.launch {
      repository.getAttendanceForDate(date).collect { records ->
        val recordMap = records.associateBy { it.workerId }
        _uiState.update { it.copy(attendanceRecords = recordMap) }
      }
    }
  }

  fun saveAttendance(record: DailyAttendance) {
    viewModelScope.launch {
      repository.recordAttendance(record)
    }
  }
}
