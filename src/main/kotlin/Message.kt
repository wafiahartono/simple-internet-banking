import org.json.JSONObject

data class Message(val command: Command, val data: Any?) {
    constructor(json: JSONObject) : this(
        Command.valueOf(json.getString("command")),
        json.opt("data")
    )

    fun toJSON(): JSONObject = JSONObject()
        .put("command", command.toString())
        .putOpt("data", data)
}