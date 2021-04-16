import org.json.JSONObject
import java.security.*
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.interfaces.DHPublicKey
import javax.crypto.spec.SecretKeySpec

class Server {
    private val keyPair: KeyPair = KeyPairGenerator.getInstance("RSA").run {
        initialize(4096)
        return@run generateKeyPair()
    }

    val certificate: Certificate = kotlin.run certificate@{
        val content = "simple-internet-banking-server".toByteArray()
        return@certificate Certificate(
            content,
            Signature.getInstance("SHA256withRSA").run signature@{
                initSign(keyPair.private)
                update(content)
                return@signature sign()
            },
            keyPair.public.encoded
        )
    }

    private lateinit var messageEncryptionKey: Key
    private val database = ServerDatabase(".sib-runtime/server.sqlite")

    fun exchangeEncryptionKey(publicKey: ByteArray): ByteArray {
        val clientPublicKey = KeyFactory.getInstance("DH").generatePublic(
            X509EncodedKeySpec(publicKey)
        )
        val keyPair = KeyPairGenerator.getInstance("DH").run {
            initialize((clientPublicKey as DHPublicKey).params)
            return@run generateKeyPair()
        }
        val sharedKey = KeyAgreement.getInstance("DH").apply {
            init(keyPair.private)
            doPhase(clientPublicKey, true)
        }.generateSecret()
        println("Server encryption key: ${sharedKey.toHexString()}")
        messageEncryptionKey = SecretKeySpec(sharedKey, 0, 16, "AES")
        return keyPair.public.encoded
    }

    fun sendEncryptedData(encryptedData: EncryptedData): EncryptedData {
        println("Receive encrypted data from client: $encryptedData")
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            messageEncryptionKey,
            AlgorithmParameters.getInstance("AES").apply { init(encryptedData.algorithmParameters) }
        )
        val responseToSend = processClientMessage(Message(JSONObject(String(cipher.doFinal(encryptedData.data)))))
        cipher.init(Cipher.ENCRYPT_MODE, messageEncryptionKey)
        return EncryptedData(
            cipher.doFinal(responseToSend.toJSON().toString().toByteArray()),
            cipher.parameters.encoded
        )
    }

    private fun processClientMessage(message: Message): Message {
        println("Process client message: $message")
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
