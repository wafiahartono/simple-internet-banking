import org.json.JSONArray
import org.json.JSONObject
import java.util.*

data class Transaction(
    val id: Int? = null,
    val date: Date = Date(),
    val from: User,
    val to: User,
    val amount: Int
) {
    companion object {
        fun toList(array: JSONArray) = array.map { Transaction(it as JSONObject) }
        fun toJSONArray(list: List<Transaction>) = JSONArray(list.size).apply {
            list.forEachIndexed { i, t -> this.put(i, t.toJSON()) }
        }
    }

    constructor(json: JSONObject) : this(
        json.opt("id") as? Int,
        Date(json.getLong("date")),
        User(json.getJSONObject("from")),
        User(json.getJSONObject("to")),
        json.getInt("amount")
    )

    fun toJSON(): JSONObject = JSONObject()
        .putOpt("id", id)
        .put("date", date.time)
        .put("from", from.toJSON())
        .put("to", to.toJSON())
        .put("amount", amount)
}