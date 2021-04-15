import org.json.JSONObject
import java.security.*
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.SecretKeySpec

class Client(
    private val serverResponseListener: (Client, Message) -> Unit
) {
    private val server = Server()
    private lateinit var messageEncryptionKey: Key

    private var _user: User? = null
    val user get() = _user

    init {
        verifyServer()
        exchangeEncryptionKey()
    }

    private fun verifyServer() {
        val signature = Signature.getInstance(SERVER_CERTIFICATE_ALGORITHM)
        val certificatePublicKey = KeyFactory.getInstance(SERVER_KEY_ALGORITHM).generatePublic(
            X509EncodedKeySpec(server.certificate.publicKey)
        )
        signature.initVerify(certificatePublicKey)
        signature.update(server.certificate.content)
        if (signature.verify(server.certificate.signature)) println("Server verified")
        else throw IllegalStateException("Server cannot be verified")
    }

    private fun exchangeEncryptionKey() {
        val keyPair = KeyPairGenerator.getInstance(KEY_EXCHANGE_ALGORITHM).run {
            initialize(KEY_EXCHANGE_KEY_SIZE)
            return@run generateKeyPair()
        }
        val serverPublicKey = KeyFactory.getInstance(KEY_EXCHANGE_ALGORITHM).generatePublic(
            X509EncodedKeySpec(server.exchangeEncryptionKey(keyPair.public.encoded))
        )
        val sharedKey = KeyAgreement.getInstance(KEY_EXCHANGE_ALGORITHM).apply {
            init(keyPair.private)
            doPhase(serverPublicKey, true)
        }.generateSecret()
        println("Client encryption key: ${sharedKey.toHexString()}")
        messageEncryptionKey = SecretKeySpec(sharedKey, 0, 16, MESSAGE_ENCRYPTION_ALGORITHM)
    }

    private var signUpUserCache: User? = null
    fun signUp(user: User) {
        signUpUserCache = user.copy()
        sendMessageToServer(Message(Command.SIGN_UP, user.toJSON()))
    }

    fun signIn(user: User) = sendMessageToServer(Message(Command.SIGN_IN, user.toJSON()))

    fun getUser() = _user?.username?.let { sendMessageToServer(Message(Command.GET_USER, it)) }

    fun signOut() {
        _user = null
    }

    fun addBalance(amount: Int) = _user?.accountId?.let {
        sendMessageToServer(
            Message(Command.ADD_BALANCE, JSONObject().put("account_id", it).put("amount", amount))
        )
    }

    fun checkAccountId(accountId: String) = sendMessageToServer(Message(Command.CHECK_ACCOUNT_ID, accountId))

    fun doTransaction(destAccountId: String, amount: Int) = _user?.let {
        sendMessageToServer(
            Message(
                Command.DO_TRANSACTION, Transaction(
                    from = it, to = User(accountId = destAccountId), amount = amount
                ).toJSON()
            )
        )
    }

    fun getTransactions() = _user?.accountId?.let { sendMessageToServer(Message(Command.GET_TRANSACTIONS, it)) }

    private fun sendMessageToServer(message: Message) {
        println("Send to server: $message")
        val cipher = Cipher.getInstance(MESSAGE_ENCRYPTION_KEY_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, messageEncryptionKey)
        val response = server.sendEncryptedData(
            EncryptedData(cipher.doFinal(message.toJSON().toString().toByteArray()), cipher.parameters.encoded)
        )
        println("Server response: $response")
        cipher.init(
            Cipher.DECRYPT_MODE,
            messageEncryptionKey,
            AlgorithmParameters.getInstance(MESSAGE_ENCRYPTION_ALGORITHM).apply { init(response.algorithmParameters) }
        )
        processServerMessage(Message(JSONObject(String(cipher.doFinal(response.data)))))
    }

    private fun processServerMessage(message: Message) {
        println("Process server message: $message")
        when (message.command) {
            Command.SIGN_UP -> {
                _user = if (message.data as Boolean) signUpUserCache!!.copy() else null
                signUpUserCache = null
            }
            Command.SIGN_IN,
            Command.GET_USER -> {
                _user = if (message.data == null) null else User(message.data as JSONObject)
            }
            Command.ADD_BALANCE -> (message.data as? Int)?.let { _user = _user?.copy(balance = it) }
            Command.CHECK_ACCOUNT_ID,
            Command.DO_TRANSACTION,
            Command.GET_TRANSACTIONS -> Unit
        }
        serverResponseListener(this, message)
    }
}
