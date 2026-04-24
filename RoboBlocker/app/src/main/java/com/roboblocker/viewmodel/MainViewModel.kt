package com.roboblocker.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.roboblocker.App
import com.roboblocker.data.db.BlockReason
import com.roboblocker.data.db.BlockedNumber
import com.roboblocker.data.repository.BlockerRepository
import com.roboblocker.utils.startOfToday
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repo: BlockerRepository = App.instance.repository

    val allBlockedNumbers = repo.allBlockedNumbers
    val blockedCount      = repo.blockedCount
    val recentCallLogs    = repo.recentCallLogs
    val totalBlocked      = repo.totalBlocked
    val blockedToday      = repo.blockedToday(startOfToday())

    private val _isServiceActive = MutableLiveData(false)
    val isServiceActive: LiveData<Boolean> = _isServiceActive

    private val _operationResult = MutableLiveData<String?>()
    val operationResult: LiveData<String?> = _operationResult

    fun setServiceActive(active: Boolean) { _isServiceActive.value = active }

    fun addNumber(number: String, label: String = "", isPattern: Boolean = false) {
        viewModelScope.launch {
            if (number.isBlank()) {
                _operationResult.value = "Número inválido"
                return@launch
            }
            repo.addBlockedNumber(
                BlockedNumber(
                    number = number.trim(),
                    label = label.trim(),
                    reason = BlockReason.MANUAL,
                    isPattern = isPattern
                )
            )
            _operationResult.value = "Número adicionado: $number"
        }
    }

    fun removeNumber(number: BlockedNumber) {
        viewModelScope.launch {
            repo.removeBlockedNumber(number)
            _operationResult.value = "Número removido"
        }
    }

    fun importNumbers(rawList: String) {
        viewModelScope.launch {
            val numbers = rawList.lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() && it.any { c -> c.isDigit() } }
            repo.importNumbers(numbers)
            _operationResult.value = "${numbers.size} números importados"
        }
    }

    suspend fun exportCsv(): String {
        val all = repo.exportAll()
        return buildString {
            appendLine("numero,label,motivo,adicionado,vezes_bloqueado")
            all.forEach { n ->
                appendLine("${n.number},${n.label},${n.reason},${n.addedAt},${n.timesBlocked}")
            }
        }
    }

    fun clearBlocklist() {
        viewModelScope.launch {
            repo.clearAll()
            _operationResult.value = "Lista negra limpa"
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            repo.clearLogs()
            _operationResult.value = "Histórico limpo"
        }
    }

    fun clearResult() { _operationResult.value = null }
}
