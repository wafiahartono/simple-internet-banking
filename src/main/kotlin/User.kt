import org.json.JSONObject

data class User(
    val accountId: String? = null,
    val username: String? = null,
    val password: String? = null,
    val name: String? = null,
    val balance: Int? = null
) {
    constructor(json: JSONObject) : this(
        json.opt("account_id") as? String,
        json.opt("username") as? String,
        json.opt("password") as? String,
        json.opt("name") as? String,
        json.opt("balance") as? Int
    )

    fun toJSON(): JSONObject = JSONObject()
        .putOpt("account_id", accountId)
        .putOpt("username", username)
        .putOpt("password", password)
        .putOpt("name", name)
        .putOpt("balance", balance)
}
