package com.gastrack.app.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Alignment
import com.gastrack.app.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gastrack.app.presentation.components.*
import com.gastrack.app.presentation.viewmodels.*

@Composable private fun AuthForm(title: String, state: FormState, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Image(painterResource(R.drawable.gastrack_logo), "GasTrack", Modifier.height(72.dp).fillMaxWidth(), alignment = Alignment.CenterStart)
        Text(title, style = MaterialTheme.typography.headlineSmall)
        state.error?.let { ErrorPanel(it) }
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        content()
    }
}
@Composable fun LoginScreen(vm: LoginViewModel, register: () -> Unit, forgot: () -> Unit, signedIn: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(vm) { vm.events.collect { if (it == AuthEvent.SignedIn) signedIn() } }
    AuthForm("Welcome back", state) {
        FormField("Email", state.fields["email"].orEmpty(), { vm.edit("email", it) }, state.errors["email"], enabled = !state.busy)
        FormField("Password", state.fields["password"].orEmpty(), { vm.edit("password", it) }, state.errors["password"], password = true, enabled = !state.busy)
        Button(onClick = vm::login, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("Sign in") }
        TextButton(onClick = forgot) { Text("Forgot password?") }
        TextButton(onClick = register) { Text("Register your company") }
    }
}
@Composable fun RegistrationScreen(vm: RegistrationViewModel, back: () -> Unit, signedIn: () -> Unit, legal: (String) -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(vm) { vm.events.collect { if (it == AuthEvent.SignedIn) signedIn() } }
    AuthForm("Register your company", state) {
        listOf("company" to "Company name", "branch" to "Branch name", "address" to "Complete address", "first" to "First name", "last" to "Last name", "email" to "Email", "password" to "Password", "confirm" to "Confirm password").forEach { (key, label) ->
            FormField(label, state.fields[key].orEmpty(), { vm.edit(key, it) }, state.errors[key], password = key in setOf("password", "confirm"), enabled = !state.busy)
        }
        Row { Checkbox(state.acceptedTerms, vm::accept); Text("I agree to the Terms of Service and Privacy Policy.", Modifier.padding(top = 12.dp)) }
        state.errors["terms"]?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Row { TextButton(onClick = { legal("terms-of-service.txt") }) { Text("Terms") }; TextButton(onClick = { legal("privacy-policy.txt") }) { Text("Privacy") } }
        Button(onClick = vm::register, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("Create account") }
        TextButton(onClick = back) { Text("Back to sign in") }
    }
}
@Composable fun PasswordScreen(vm: PasswordViewModel, token: String?, back: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    AuthForm(if (token == null) "Forgot password" else "Reset password", state) {
        if (token == null) FormField("Email", state.fields["email"].orEmpty(), { vm.edit("email", it) }, state.errors["email"])
        else {
            FormField("New password", state.fields["password"].orEmpty(), { vm.edit("password", it) }, password = true)
            FormField("Confirm password", state.fields["confirm"].orEmpty(), { vm.edit("confirm", it) }, password = true)
        }
        Button(onClick = { if (token == null) vm.forgot() else vm.reset(token) }, enabled = !state.busy) { Text(if (token == null) "Send reset link" else "Reset password") }
        TextButton(onClick = back) { Text("Back to sign in") }
    }
}
