package com.gastrack.app.domain.entities

data class ReturnOptions(val warehouses: List<Pair<String, String>>, val products: List<Pair<String, String>>)
data class Completion(val id: String, val delivery: Boolean, val requestId: String, val returned: Boolean, val warehouse: String?, val cylinders: Map<String, Int>, val collected: java.math.BigDecimal?)
