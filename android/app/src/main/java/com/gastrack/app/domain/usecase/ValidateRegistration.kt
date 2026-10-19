package com.gastrack.app.domain.usecase

import javax.inject.Inject

class ValidateRegistration @Inject constructor() {
    operator fun invoke(fields: Map<String, String>): Map<String, String> = buildMap {
        fun check(key: String, valid: Boolean, error: String) { if (!valid) put(key, error) }
        check("company", fields["company"].orEmpty().length in 2..150, "Company name must be 2–150 characters.")
        check("branch", fields["branch"].orEmpty().length in 2..100, "Branch name must be 2–100 characters.")
        check("address", fields["address"].orEmpty().length in 5..255, "Enter a complete address.")
        for (key in listOf("first", "last")) check(key, Regex("^[A-Za-zñÑ' .-]{2,50}$").matches(fields[key].orEmpty()), "Enter a valid name (2–50 characters).")
        check("email", validEmail(fields["email"].orEmpty()), "Enter a valid email.")
        check("password", fields["password"].orEmpty().length in 8..64, "Password must be 8–64 characters.")
        check("confirm", fields["confirm"] == fields["password"], "Passwords must match.")
    }
}
fun validEmail(value: String) = value.length <= 150 && Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(value)
