package com.gastrack.app.data.models

import com.gastrack.app.BuildConfig

fun productImage(raw: String?, name: String): String? {
    val base = BuildConfig.API_BASE_URL.trimEnd('/')
    if (!raw.isNullOrBlank()) {
        val value = raw.trim().replace('\\', '/')
        if (value.startsWith("https://") || value.startsWith("http://") || value.startsWith("data:")) return value
        val path = value.trimStart('/')
        if (path.startsWith("assets/products/")) return "file:///android_asset/$path"
        return "$base/$path"
    }
    val accessories = listOf("hose clamp.*1\\.5" to "acc-lpg-hoseclamp-1.5.png", "hose clamp" to "acc-lpg-hoseclamp.png", "hose" to "acc-lpg-hose.png", "tpa.*regulator" to "acc-tpa-regulator.png", "pol.*regulator" to "acc-pol-regulator.png", "reyna.*stove" to "acc-reyna-gasstove.png")
    accessories.firstOrNull { Regex(it.first, RegexOption.IGNORE_CASE).containsMatchIn(name) }?.let { return "$base/uploads/${it.second}" }
    val size = Regex("(2\\.7|7|11|22|50)\\s*kg", RegexOption.IGNORE_CASE).find(name)?.groupValues?.get(1)
    if (size != null && Regex("gasul|cylinder", RegexOption.IGNORE_CASE).containsMatchIn(name)) {
        val kind = if (name.contains("cylinder", true)) "cylinder" else "gasul"
        val variant = if (size == "11" && name.contains("elite", true)) "elite11" else size
        return "$base/uploads/$kind-${variant}kg.png"
    }
    return null
}
