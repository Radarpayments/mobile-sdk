package net.payrdr.mobile.payment.sdk.payment.api.dto.response

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

/** A wallet answer's `success`: the gateway writes `true`/`false`, older fixtures and gateways `"true"`/`"false"`. */
internal object WalletSuccessSerializer : KSerializer<Boolean> {
    override val descriptor = PrimitiveSerialDescriptor("WalletSuccess", PrimitiveKind.BOOLEAN)

    override fun deserialize(decoder: Decoder): Boolean {
        val element = (decoder as? JsonDecoder)?.decodeJsonElement() ?: return decoder.decodeBoolean()
        val primitive = element as? JsonPrimitive ?: return false
        return primitive.booleanOrNull ?: primitive.content.equals("true", ignoreCase = true)
    }

    override fun serialize(
        encoder: Encoder,
        value: Boolean,
    ): Unit = encoder.encodeBoolean(value)
}
