package com.gastrack.app.presentation.viewmodels

import androidx.lifecycle.*
import com.gastrack.app.core.common.Resource
import com.gastrack.app.domain.entities.EditorDefinition
import com.gastrack.app.domain.repository.ManagementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditorState(val loading: Boolean = true, val definition: EditorDefinition? = null, val values: Map<String, String> = emptyMap(), val errors: Map<String, String> = emptyMap(), val error: String? = null, val busy: Boolean = false)
@HiltViewModel class EditorViewModel @Inject constructor(private val repository: ManagementRepository) : ViewModel() {
    private val mutable = MutableStateFlow(EditorState())
    val state = mutable.asStateFlow()
    private val channel = Channel<String>(Channel.BUFFERED)
    val events = channel.receiveAsFlow()
    fun load(module: String, id: String?) { if (state.value.definition != null) return; viewModelScope.launch {
        when (val result = repository.editor(module, id)) {
            is Resource.Success -> mutable.value = EditorState(false, result.data, result.data.values)
            is Resource.Error -> mutable.value = EditorState(loading = false, error = result.message)
            Resource.Loading -> Unit
        }
    } }
    fun edit(key: String, value: String) { mutable.update { it.copy(values = it.values + (key to value), errors = it.errors - key) } }
    fun save(module: String, id: String?) {
        val s = state.value
        if (s.busy) return
        val errors = s.definition?.fields.orEmpty().filter { it.required && s.values[it.key].isNullOrBlank() }.associate { it.key to "${it.label} is required." }
        if (errors.isNotEmpty()) { mutable.update { it.copy(errors = errors) }; return }
        mutable.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            when (val result = repository.saveRecord(module, id, s.values)) {
                is Resource.Success -> channel.send(result.data)
                is Resource.Error -> mutable.update { it.copy(error = result.message) }
                Resource.Loading -> Unit
            }
            mutable.update { it.copy(busy = false) }
        }
    }
}
