package com.gastrack.app.presentation.viewmodels

import androidx.lifecycle.*
import com.gastrack.app.core.common.Resource
import com.gastrack.app.domain.repository.AuthRepository
import com.gastrack.app.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FormState(val fields: Map<String, String> = emptyMap(), val errors: Map<String, String> = emptyMap(), val busy: Boolean = false, val error: String? = null, val message: String? = null, val submitted: Boolean = false, val acceptedTerms: Boolean = false)
sealed interface AuthEvent { data object SignedIn : AuthEvent }
open class AuthFormViewModel : ViewModel() {
    protected val mutable = MutableStateFlow(FormState())
    val state = mutable.asStateFlow()
    protected val channel = Channel<AuthEvent>(Channel.BUFFERED)
    val events = channel.receiveAsFlow()
    open fun edit(key: String, value: String) { mutable.update { it.copy(fields = it.fields + (key to value), error = null) } }
    fun accept(value: Boolean) { mutable.update { it.copy(acceptedTerms = value) } }
    protected fun <T> submit(action: suspend () -> Resource<T>, success: suspend (T) -> Unit = { }) {
        if (mutable.value.busy) return
        mutable.update { it.copy(busy = true, error = null, message = null, submitted = true) }
        viewModelScope.launch {
            try {
                when (val result = action()) {
                    is Resource.Success -> success(result.data)
                    is Resource.Error -> mutable.update { it.copy(error = result.message) }
                    Resource.Loading -> Unit
                }
            } finally { mutable.update { it.copy(busy = false) } }
        }
    }
}
@HiltViewModel class LoginViewModel @Inject constructor(private val auth: AuthRepository) : AuthFormViewModel() {
    fun login() {
        val fields = state.value.fields
        val errors = buildMap {
            if (!validEmail(fields["email"].orEmpty())) put("email", "Enter a valid email.")
            if (fields["password"].isNullOrEmpty()) put("password", "Password is required.")
        }
        mutable.update { it.copy(errors = errors) }; if (errors.isNotEmpty()) return
        submit({ auth.login(fields.getValue("email"), fields.getValue("password")) }) { channel.send(AuthEvent.SignedIn) }
    }
}
@HiltViewModel class RegistrationViewModel @Inject constructor(private val auth: AuthRepository, private val validate: ValidateRegistration) : AuthFormViewModel() {
    override fun edit(key: String, value: String) {
        val filtered = when (key) {
            "company", "branch" -> value.replace(Regex("[^A-Za-z0-9 .,&'-]"), "").take(if (key == "company") 150 else 100)
            "first", "last" -> value.replace(Regex("[^A-Za-zñÑ' .-]"), "").take(50)
            "address" -> value.take(255)
            "email" -> value.replace(Regex("\\s"), "").take(150)
            else -> value.take(64)
        }
        super.edit(key, filtered)
        if (state.value.submitted) mutable.update { it.copy(errors = validate(it.fields)) }
    }
    fun register() {
        val s = state.value
        val errors = validate(s.fields) + if (!s.acceptedTerms) mapOf("terms" to "Accept the Terms of Service and Privacy Policy.") else emptyMap()
        mutable.update { it.copy(errors = errors, submitted = true) }; if (errors.isNotEmpty()) return
        val f = s.fields
        submit({ auth.register(f.getValue("company"), f.getValue("branch"), f.getValue("address"), f.getValue("first"), f.getValue("last"), f.getValue("email"), f.getValue("password"), f.getValue("confirm")) }) { channel.send(AuthEvent.SignedIn) }
    }
}
@HiltViewModel class PasswordViewModel @Inject constructor(private val auth: AuthRepository) : AuthFormViewModel() {
    fun forgot() {
        val email = state.value.fields["email"].orEmpty()
        if (!validEmail(email)) { mutable.update { it.copy(errors = mapOf("email" to "Enter a valid email.")) }; return }
        submit({ auth.forgot(email) }) { message -> mutable.update { it.copy(message = message) } }
    }
    fun reset(token: String) {
        val password = state.value.fields["password"].orEmpty()
        if (password.length !in 8..64 || password != state.value.fields["confirm"]) {
            mutable.update { it.copy(error = "Use 8–64 characters and matching passwords.") }; return
        }
        submit({
            when (val valid = auth.validateReset(token)) {
                is Resource.Success -> if (valid.data) auth.reset(token, password) else Resource.Error("This reset link is invalid or expired.")
                is Resource.Error -> valid
                Resource.Loading -> Resource.Loading
            }
        }) { message -> mutable.update { it.copy(message = message) } }
    }
}
