@Suppress("ArrayInDataClass")
data class Certificate(val content: ByteArray, val signature: ByteArray, val publicKey: ByteArray) {
    override fun toString() = "${content.toHexString()}.${signature.toHexString()}.${publicKey.toHexString()}"
}
