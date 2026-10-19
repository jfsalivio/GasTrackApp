package com.gastrack.app.data.models

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*

/** Legacy report deployments use numeric IDs; current schemas use prefixed strings. */
object IdentifierSerializer : KSerializer<String> {
    override val descriptor = PrimitiveSerialDescriptor("Identifier", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder): String = (decoder as JsonDecoder).decodeJsonElement().jsonPrimitive.content
    override fun serialize(encoder: Encoder, value: String) = encoder.encodeString(value)
}
