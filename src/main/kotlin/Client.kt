import org.json.JSONObject
import java.security.AlgorithmParameters
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.SecretKeySpec

class Client(
    private val serverResponseListener: (Client, Message) -> Unit
) {
    private val server = Server()
    private val commKey: ByteArray

    private var _user: User? = null
    val user get() = _user

    init {
        server.certificate.let { certificate ->
            Signature.getInstance(SERVER_CERTIFICATE_ALGORITHM).let {
                it.initVerify(
                    KeyFactory.getInstance(SERVER_KEY_ALGORITHM).generatePublic(
                        X509EncodedKeySpec(certificate.publicKey)
                    )
                )
                it.update(certificate.content)
                if (it.verify(certificate.signature)) println("Server verified")
                else throw IllegalStateException("Server certificate cannot be verified")
            }
        }
        val keyPair = KeyPairGenerator.getInstance(KEY_EXCHANGE_ALGORITHM).run {
            initialize(KEY_EXCHANGE_KEY_SIZE)
            return@run generateKeyPair()
        }
        val serverExcPublicKey = KeyFactory.getInstance(KEY_EXCHANGE_ALGORITHM).generatePublic(
            X509EncodedKeySpec(server.exchangeKey(keyPair.public.encoded))
        )
        commKey = KeyAgreement.getInstance(KEY_EXCHANGE_ALGORITHM).apply {
            init(keyPair.private)
            doPhase(serverExcPublicKey, true)
        }.generateSecret()
        println("Exchanged key with server: ${commKey.toHexString()}")
    }

    private var signUpUser: User? = null
    fun signUp(user: User) {
        signUpUser = user.copy()
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
        println("Client.sendMessageToServer: $message")
        Cipher.getInstance(MESSAGE_ENCRYPTION_KEY_TRANSFORMATION).let {
            val symKey = SecretKeySpec(commKey, 0, 16, MESSAGE_ENCRYPTION_ALGORITHM)
            it.init(Cipher.ENCRYPT_MODE, symKey)
            val response = server.writePayload(
                Payload(it.doFinal(message.toJSON().toString().toByteArray()), it.parameters.encoded)
            )
            println("Client.sendMessageToServer (received response): $response")
            it.init(
                Cipher.DECRYPT_MODE,
                symKey,
                AlgorithmParameters.getInstance(MESSAGE_ENCRYPTION_ALGORITHM).apply { init(response.keyParams) }
            )
            processServerResponse(Message(JSONObject(String(it.doFinal(response.data)))))
        }
    }

    private fun processServerResponse(message: Message) {
        println("Client.processServerResponse: $message")
        when (message.command) {
            Command.SIGN_UP -> {
                _user = if (message.data as Boolean) signUpUser!!.copy() else null
                signUpUser = null
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
