import org.json.JSONObject
import java.security.*
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.interfaces.DHPublicKey
import javax.crypto.spec.SecretKeySpec

class Server {
    private val keyPair: KeyPair = KeyPairGenerator.getInstance(SERVER_KEY_ALGORITHM).run {
        initialize(SERVER_KEY_SIZE)
        return@run generateKeyPair()
    }

    val certificate: Certificate = kotlin.run certificate@{
        val content = "simple-internet-banking-server".toByteArray()
        return@certificate Certificate(
            content,
            Signature.getInstance(SERVER_CERTIFICATE_ALGORITHM).run signature@{
                initSign(keyPair.private)
                update(content)
                return@signature sign()
            },
            keyPair.public.encoded
        )
    }

    private lateinit var commKey: ByteArray
    private val database = ServerDatabase(".sib-runtime/server.sqlite")

    fun exchangeKey(clientKeyEnc: ByteArray): ByteArray {
        val clientExcPublicKey = KeyFactory.getInstance(KEY_EXCHANGE_ALGORITHM).generatePublic(
            X509EncodedKeySpec(clientKeyEnc)
        )
        val keyPair = KeyPairGenerator.getInstance(KEY_EXCHANGE_ALGORITHM).run {
            initialize((clientExcPublicKey as DHPublicKey).params)
            generateKeyPair()
        }
        commKey = KeyAgreement.getInstance(KEY_EXCHANGE_ALGORITHM).apply {
            init(keyPair.private)
            doPhase(clientExcPublicKey, true)
        }.generateSecret()
        println("Exchanged key with client: ${commKey.toHexString()}")
        return keyPair.public.encoded
    }

    fun writePayload(payload: Payload): Payload {
        println("Server.writePayload: $payload")
        Cipher.getInstance(MESSAGE_ENCRYPTION_KEY_TRANSFORMATION).let {
            val symKey = SecretKeySpec(commKey, 0, 16, MESSAGE_ENCRYPTION_ALGORITHM)
            it.init(
                Cipher.DECRYPT_MODE,
                symKey,
                AlgorithmParameters.getInstance(MESSAGE_ENCRYPTION_ALGORITHM).apply { init(payload.keyParams) }
            )
            val result = processClientMessage(Message(JSONObject(String(it.doFinal(payload.data)))))
            it.init(Cipher.ENCRYPT_MODE, symKey)
            return Payload(it.doFinal(result.toJSON().toString().toByteArray()), it.parameters.encoded)
        }
    }

    private fun processClientMessage(message: Message): Message {
        println("Server.processClientMessage: $message")
        return when (message.command) {
            Command.SIGN_UP -> Message(
                Command.SIGN_UP, database.insertUser(User(message.data as JSONObject))
            )
            Command.SIGN_IN -> Message(
                Command.SIGN_IN, database.checkUserCredentials(User(message.data as JSONObject))?.toJSON()
            )
            Command.GET_USER -> Message(
                Command.GET_USER, database.getUser(message.data as String)?.toJSON()
            )
            Command.ADD_BALANCE -> Message(
                Command.ADD_BALANCE, database.addBalance(
                    (message.data as JSONObject).getString("account_id"), message.data.getInt("amount")
                )
            )
            Command.CHECK_ACCOUNT_ID -> Message(
                Command.CHECK_ACCOUNT_ID, database.getUserName(message.data as String)
            )
            Command.DO_TRANSACTION -> Message(
                Command.DO_TRANSACTION, database.insertTransaction(Transaction(message.data as JSONObject))
            )
            Command.GET_TRANSACTIONS -> Message(
                Command.GET_TRANSACTIONS, Transaction.toJSONArray(database.getTransactions(message.data as String))
            )
        }
    }
}
