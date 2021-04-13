@Suppress("ArrayInDataClass")
data class Payload(val data: ByteArray, val keyParams: ByteArray) {
    override fun toString() = "${data.toHexString()}.${keyParams.toHexString()}"
}