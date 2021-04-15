@file:Suppress("SqlDialectInspection", "SqlNoDataSourceInspection")

import java.security.MessageDigest
import java.sql.Connection
import java.sql.Date
import java.sql.DriverManager

class ServerDatabase(path: String) {
    private val connection: Connection = DriverManager.getConnection("jdbc:sqlite:$path")

    init {
        connection.createStatement().execute(
            """
            CREATE TABLE IF NOT EXISTS `users` (
                `account_id` TEXT PRIMARY KEY,
                `username` TEXT UNIQUE NOT NULL,
                `password` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `balance` INTEGER NOT NULL DEFAULT 0
            );
        """.trimIndent()
        )
        connection.createStatement().execute(
            """
            CREATE TABLE IF NOT EXISTS `transactions` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT,
                `date` INTEGER NOT NULL,
                `from` TEXT NOT NULL,
                `to` TEXT NOT NULL,
                `amount` INTEGER NOT NULL,
                FOREIGN KEY (`from`) REFERENCES `users` (`account_id`),
                FOREIGN KEY (`to`) REFERENCES `users` (`account_id`)
            );
        """.trimIndent()
        )
    }

    fun insertUser(user: User): Boolean = connection.prepareStatement(
        "INSERT INTO `users` (`account_id`, `username`, `password`, `name`) VALUES (?, ?, ?, ?)"
    ).apply {
        setString(1, user.accountId)
        setString(2, user.username)
        setString(
            3, String(
                MessageDigest.getInstance(USER_PASSWD_HASH_ALG).digest(user.password!!.toByteArray())
            )
        )
        setString(4, user.name)
    }.executeUpdate() == 1

    fun checkUserCredentials(user: User): User? = connection.prepareStatement(
        "SELECT `password` FROM `users` WHERE `username` = ?"
    ).apply { setString(1, user.username) }.executeQuery().use {
        return@use if (it.next()) {
            val hashed = String(
                MessageDigest.getInstance(USER_PASSWD_HASH_ALG).digest(user.password!!.toByteArray())
            )
            return@use if (hashed == it.getString("password")) getUser(user.username!!)
            else null
        } else null
    }

    fun getUser(username: String): User? = connection.prepareStatement(
        "SELECT `account_id`, `name`, `balance` FROM `users` WHERE `username` = ?"
    ).apply { setString(1, username) }.executeQuery().use {
        return@use if (it.next()) User(
            accountId = it.getString("account_id"),
            username = username,
            name = it.getString("name"),
            balance = it.getInt("balance")
        ) else null
    }

    fun getUserName(accountId: String): String? = connection.prepareStatement(
        "SELECT `name` FROM `users` WHERE `account_id` = ?"
    ).apply { setString(1, accountId) }.executeQuery().use {
        return@use if (it.next()) it.getString("name")
        else null
    }

    fun addBalance(accountId: String, amount: Int): Int? = connection.prepareStatement(
        "UPDATE `users` SET balance = balance + ? WHERE `account_id` = ?"
    ).apply {
        setInt(1, amount)
        setString(2, accountId)
    }.executeUpdate().let {
        return@let if (it != 1) null
        else connection.prepareStatement(
            "SELECT `balance` FROM `users` WHERE `account_id` = ?"
        ).apply { setString(1, accountId) }.executeQuery().use { resultSet ->
            return@use if (resultSet.next()) resultSet.getInt("balance")
            else null
        }
    }

    fun insertTransaction(transaction: Transaction): Boolean = connection.prepareStatement(
        "INSERT INTO `transactions` (`date`, `from`, `to`, `amount`) VALUES (?, ?, ?, ?)"
    ).apply {
        setLong(1, transaction.date.time)
        setString(2, transaction.from.accountId)
        setString(3, transaction.to.accountId)
        setInt(4, transaction.amount)
    }.executeUpdate().let {
        addBalance(transaction.from.accountId!!, -transaction.amount)
        addBalance(transaction.to.accountId!!, transaction.amount)
        return@let it == 1
    }

    fun getTransactions(accountId: String): List<Transaction> = connection.prepareStatement(
        """
            SELECT `id`, `date`, `from`, user_from.name AS `from_name`, `to`, user_to.name AS `to_name`, `amount`
            FROM `transactions`
            INNER JOIN `users` AS user_from ON user_from.account_id = `from`
            INNER JOIN `users` AS user_to ON user_to.account_id = `to`
            WHERE `from` = ? OR `to` = ?;
        """.trimIndent()
    ).apply {
        setString(1, accountId)
        setString(2, accountId)
    }.executeQuery().use {
        return@use generateSequence {
            return@generateSequence if (it.next()) Transaction(
                it.getInt("id"),
                Date(it.getLong("date")),
                User(
                    accountId = it.getString("from"),
                    name = it.getString("from_name")
                ),
                User(
                    accountId = it.getString("to"),
                    name = it.getString("to_name")
                ),
                it.getInt("amount")
            ) else null
        }.toList()
    }
}
