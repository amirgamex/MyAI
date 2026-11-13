package com.myai.app.ui.memory

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.myai.app.data.db.AppDatabase
import com.myai.app.data.db.MemoryEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class MemoryUiState(
    val memories: List<MemoryEntity> = emptyList(),
    val isLoading: Boolean = false,
    val editingId: String? = null,
    val editKey: String = "",
    val editValue: String = "",
    val showAddDialog: Boolean = false
)

class MemoryViewModel(application: Application) : AndroidViewModel(application) {

    private val chatDao = AppDatabase.getInstance(application).chatDao()

    private val _uiState = MutableStateFlow(MemoryUiState())
    val uiState: StateFlow<MemoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            chatDao.getAllMemories().collect { list ->
                _uiState.update { it.copy(memories = list, isLoading = false) }
            }
        }
    }

    fun startAdd() {
        _uiState.update {
            it.copy(
                showAddDialog = true,
                editingId = null,
                editKey = "",
                editValue = ""
            )
        }
    }

    fun startEdit(memory: MemoryEntity) {
        _uiState.update {
            it.copy(
                showAddDialog = true,
                editingId = memory.id,
                editKey = memory.key,
                editValue = memory.value
            )
        }
    }

    fun cancelEdit() {
        _uiState.update {
            it.copy(showAddDialog = false, editingId = null, editKey = "", editValue = "")
        }
    }

    fun onKeyChange(value: String) {
        _uiState.update { it.copy(editKey = value) }
    }

    fun onValueChange(value: String) {
        _uiState.update { it.copy(editValue = value) }
    }

    fun saveMemory() {
        val state = _uiState.value
        val key = state.editKey.trim()
        val value = state.editValue.trim()
        if (key.isEmpty() || value.isEmpty()) return

        viewModelScope.launch {
            val entity = MemoryEntity(
                id = state.editingId ?: UUID.randomUUID().toString(),
                key = key,
                value = value,
                updatedAt = System.currentTimeMillis()
            )
            chatDao.upsertMemory(entity)
            _uiState.update {
                it.copy(showAddDialog = false, editingId = null, editKey = "", editValue = "")
            }
        }
    }

    fun deleteMemory(id: String) {
        viewModelScope.launch {
            chatDao.deleteMemory(id)
        }
    }

    fun clearAllMemories() {
        viewModelScope.launch {
            chatDao.clearAllMemories()
        }
    }
}
