package com.gastrack.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

private val colors = lightColorScheme(primary = Color(0xFF1D6BF3), background = Color(0xFFF3F5F8), surface = Color.White, onSurface = Color(0xFF111827), error = Color(0xFFDC2626))
@Composable fun GasTrackTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, shapes = Shapes(medium = RoundedCornerShape(16.dp)), content = content)
}
@Composable fun FormField(label: String, value: String, onChange: (String) -> Unit, error: String? = null, password: Boolean = false, enabled: Boolean = true) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(value, onChange, modifier = Modifier.fillMaxWidth(), label = { Text(label) }, singleLine = true,
        enabled = enabled, isError = error != null,
        supportingText = if (error != null) ({ Text(error) }) else null,
        visualTransformation = if (password && !visible) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = if (password) ({ TextButton(onClick = { visible = !visible }) { Text(if (visible) "Hide" else "Show") } }) else null)
}
@Composable fun ErrorPanel(message: String, retry: (() -> Unit)? = null) {
    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) { Text(message); if (retry != null) TextButton(onClick = retry) { Text("Retry") } }
    }
}
@Composable fun RecordCard(title: String, subtitle: String, detail: String = "", actions: @Composable RowScope.() -> Unit = {}) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            if (detail.isNotBlank()) Text(detail, style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), content = actions)
        }
    }
}
@Composable fun LoadingPanel(label: String = "Loading…") {
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LinearProgressIndicator(Modifier.fillMaxWidth())
        Text(label)
        repeat(3) { Box(Modifier.fillMaxWidth().height(64.dp).background(Color(0xFFE5E7EB), RoundedCornerShape(12.dp))) }
    }
}
@Composable fun ProductImage(url: String?, name: String) {
    if (url != null) AsyncImage(model = url, contentDescription = name, modifier = Modifier.fillMaxWidth().height(128.dp), contentScale = ContentScale.Fit)
    else Text("No image available", style = MaterialTheme.typography.bodySmall)
}
