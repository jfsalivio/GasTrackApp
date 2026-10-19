package com.gastrack.app.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gastrack.app.presentation.components.*
import com.gastrack.app.presentation.viewmodels.CompletionViewModel

@Composable fun CompletionDialog(id: String, delivery: Boolean, cod: Boolean, close: () -> Unit, completed: () -> Unit) {
    val vm: CompletionViewModel = hiltViewModel(key = "complete:$delivery:$id")
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(id) { vm.load(id, delivery) }
    LaunchedEffect(vm) { vm.events.collect { completed(); close() } }
    AlertDialog(onDismissRequest = { if (!state.busy) close() }, title = { Text(if (delivery) "Complete delivery" else "Complete pickup claim") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.loading || state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            state.error?.let { ErrorPanel(it) }
            if (cod) FormField("Amount collected", state.collected, vm::collected)
            Text("Empty Cylinder Returned?")
            Row { TextButton(onClick = { vm.returned(true) }) { Text(if (state.returned == true) "✓ Yes" else "Yes") }; TextButton(onClick = { vm.returned(false) }) { Text(if (state.returned == false) "✓ No" else "No") } }
            if (state.returned == true) {
                Text("Return warehouse")
                state.options?.warehouses?.forEach { (id, name) -> Row { RadioButton(state.warehouse == id, { vm.warehouse(id) }); Text(name, Modifier.padding(top = 12.dp)) } }
                state.options?.products?.forEach { (id, name) -> FormField("$name quantity", state.quantities[id].orEmpty(), { vm.quantity(id, it) }) }
            }
        }
    }, confirmButton = { TextButton(onClick = { vm.complete(id, delivery, cod) }, enabled = !state.loading && !state.busy && state.options != null) { Text("Complete") } }, dismissButton = { TextButton(onClick = close, enabled = !state.busy) { Text("Cancel") } })
}
