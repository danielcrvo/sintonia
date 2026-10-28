package com.danielcrvo.sintonia.feature.lists.domain

enum class FieldError { Required, InvalidUrl }

enum class FormField { Server, Username, Password, Url, File }

typealias FieldErrors = Map<FormField, FieldError>

object ListFormValidator {
    private val Host = Regex("""^[^\s:@/]+(:\d{1,5})?$""")
    private val Scheme = Regex("""^([a-z][a-z0-9+.-]*)://""", RegexOption.IGNORE_CASE)

    private fun checkAddress(raw: String, schemeRequired: Boolean): FieldError? {
        val value = raw.trim()
        if (value.isEmpty()) return FieldError.Required
        if (value.any { it.isWhitespace() }) return FieldError.InvalidUrl
        val scheme = Scheme.find(value)
        if (scheme == null && schemeRequired) return FieldError.InvalidUrl
        if (scheme != null && !scheme.groupValues[1].equals("http", true) && !scheme.groupValues[1].equals("https", true)) {
            return FieldError.InvalidUrl
        }
        val rest = if (scheme != null) value.substring(scheme.value.length) else value
        val host = rest.split('/', '?', '#').first()
        return if (Host.matches(host)) null else FieldError.InvalidUrl
    }

    private fun required(value: String): FieldError? = if (value.isBlank()) FieldError.Required else null

    fun validateXtream(server: String, username: String, password: String): FieldErrors = buildMap {
        checkAddress(server, schemeRequired = false)?.let { put(FormField.Server, it) }
        required(username)?.let { put(FormField.Username, it) }
        required(password)?.let { put(FormField.Password, it) }
    }

    fun validateLink(url: String): FieldErrors = buildMap {
        checkAddress(url, schemeRequired = true)?.let { put(FormField.Url, it) }
    }

    fun validateFile(hasFile: Boolean): FieldErrors =
        if (hasFile) emptyMap() else mapOf(FormField.File to FieldError.Required)

    fun nameFromServer(address: String): String {
        val withoutScheme = address.trim().replace(Scheme, "")
        val afterUser = if ('@' in withoutScheme) withoutScheme.substringAfterLast('@') else withoutScheme
        return afterUser.split('/', '?', '#').first().replace(Regex(""":\d+$"""), "")
    }

    fun nameFromFile(filename: String): String = filename.replace(Regex("""\.(m3u8?|txt)$""", RegexOption.IGNORE_CASE), "")
}
