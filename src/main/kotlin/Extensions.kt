import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

private val DATE_FORMAT = SimpleDateFormat("EEEE, d MMM yyyy HH:mm:ss", Locale("id", "ID"))
private val CURRENCY_FORMAt = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

fun ByteArray.toHexString() = joinToString(separator = "") { "%02x".format(it) }
fun Int.formatAsCurrency(): String = CURRENCY_FORMAt.format(this)
fun Date.format(): String = DATE_FORMAT.format(this)