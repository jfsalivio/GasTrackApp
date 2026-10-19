package com.gastrack.app.domain.entities

data class EditorField(val key: String, val label: String, val required: Boolean = true, val secret: Boolean = false, val choices: List<Pair<String, String>> = emptyList())
data class EditorDefinition(val title: String, val fields: List<EditorField>, val values: Map<String, String>)
