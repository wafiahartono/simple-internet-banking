import java.security.KeyPair
import java.security.KeyPairGenerator

const val CA_KEY_PAIR_ALG = "RSA"
const val CA_KEY_PAIR_KEY_SIZE = 4096

val CA_KEY_PAIR: KeyPair = KeyPairGenerator.getInstance(CA_KEY_PAIR_ALG).run {
    initialize(CA_KEY_PAIR_KEY_SIZE)
    return@run generateKeyPair()
}

const val CERT_ALG = "SHA256withRSA"
const val CERT_CONTENT = "Internet Banking"

const val KEY_EXC_ALG = "DH"
const val KEY_EXC_SIZE = 4096

const val SYM_ALG = "AES"
const val SYM_KEY_TRANSF = "AES/CBC/PKCS5Padding"

const val USER_PASSWD_HASH_ALG = "SHA-256"