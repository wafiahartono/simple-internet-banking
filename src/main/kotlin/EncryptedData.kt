@Suppress("ArrayInDataClass")
data class EncryptedData(val data: ByteArray, val algorithmParameters: ByteArray) {
    override fun toString() = "${data.toHexString()}.${algorithmParameters.toHexString()}"
}
