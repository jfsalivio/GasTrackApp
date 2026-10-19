package com.gastrack.app.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gastrack.app.presentation.components.*
import com.gastrack.app.presentation.viewmodels.EditorViewModel

@Composable fun EditorDialog(module: String, id: String?, instance: String, close: () -> Unit, saved: () -> Unit) {
    val vm: EditorViewModel = hiltViewModel(key = "editor:$module:$id:$instance")
    val s by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(vm) { vm.load(module, id) }
    LaunchedEffect(vm) { vm.events.collect { Toast.makeText(context, it, Toast.LENGTH_LONG).show(); saved(); close() } }
    AlertDialog(onDismissRequest = { if (!s.busy) close() }, title = { Text(s.definition?.title ?: "Loading…") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (s.loading || s.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            s.error?.let { ErrorPanel(it) }
            s.definition?.fields?.forEach { field ->
                if (module == "purchase-orders" && field.key.startsWith("qty:") && !field.key.startsWith("qty:${s.values["supplierId"]}:")) return@forEach
                if (field.choices.isNotEmpty()) {
                    Text(field.label, style = MaterialTheme.typography.titleSmall)
                    field.choices.forEach { (value, label) -> Row { RadioButton(s.values[field.key] == value, { vm.edit(field.key, value) }, enabled = !s.busy); Text(label, Modifier.padding(top = 12.dp)) } }
                    s.errors[field.key]?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                } else FormField(field.label, s.values[field.key].orEmpty(), { vm.edit(field.key, it) }, s.errors[field.key], field.secret, !s.busy)
            }
        }
    }, confirmButton = { TextButton(onClick = { vm.save(module, id) }, enabled = !s.loading && !s.busy && s.definition != null) { Text("Save") } }, dismissButton = { TextButton(onClick = close, enabled = !s.busy) { Text("Cancel") } })
}
