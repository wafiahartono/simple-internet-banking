@file:Suppress("FunctionName")

import androidx.compose.desktop.Window
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.svgResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject

fun main() {
    AppWindow()
}

enum class Screen {
    SIGN_IN, SIGN_UP, HOME
}

private fun AppWindow() = Window(
    title = "Internet Banking - CBPWDS",
    size = IntSize(1000, 700),
    resizable = true
) {
    var screen by remember { mutableStateOf(Screen.SIGN_IN) }
    var user by remember { mutableStateOf<User?>(null) }
    var signInInvalidCredentials by remember { mutableStateOf(false) }
    var transactionDestName by remember { mutableStateOf<String?>(null) }
    var transactionDestNameNotFound by remember { mutableStateOf(false) }
    var transactions by remember { mutableStateOf<List<Transaction>>(emptyList()) }
    val client by remember {
        mutableStateOf(Client { client, message ->
            when (message.command) {
                Command.SIGN_UP -> {
                    user = client.user
                    if (message.data as Boolean) screen = Screen.HOME
                }
                Command.SIGN_IN -> {
                    user = if (message.data == null) null else User(message.data as JSONObject)
                    signInInvalidCredentials = message.data == null
                    if (!signInInvalidCredentials) screen = Screen.HOME
                }
                Command.GET_USER -> user = if (message.data == null) null else User(message.data as JSONObject)
                Command.ADD_BALANCE -> user = user?.copy(balance = message.data as? Int)
                Command.CHECK_ACCOUNT_ID -> {
                    transactionDestName = message.data as? String
                    transactionDestNameNotFound = message.data == null
                }
                Command.DO_TRANSACTION -> {
                    client.getUser()
                    client.getTransactions()
                }
                Command.GET_TRANSACTIONS -> transactions = Transaction.toList(message.data as JSONArray)
            }
        })
    }
    MaterialTheme {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (screen) {
                Screen.SIGN_IN -> SignInScreen(
                    onButtonSignInClick = { client.signIn(it) },
                    onButtonSignUpClick = { screen = Screen.SIGN_UP },
                    invalidCredentials = signInInvalidCredentials
                )
                Screen.SIGN_UP -> SignUpScreen(
                    onButtonSignUpClick = { client.signUp(it) },
                    onButtonSignInClick = {
                        signInInvalidCredentials = false
                        screen = Screen.SIGN_IN
                    }
                )
                Screen.HOME -> HomeScreen(
                    user = user,
                    onButtonSignOutClick = {
                        screen = Screen.SIGN_IN
                        user = null
                        client.signOut()
                    },
                    onButtonAddBalanceClick = { client.addBalance(it) },
                    transactionDestName = transactionDestName,
                    transactionDestNameNotFound = transactionDestNameNotFound,
                    onButtonCheckDestAccountIdClick = { client.checkAccountId(it) },
                    onButtonMakeTransactionClick = { accountId, amount ->
                        client.doTransaction(accountId, amount)
                    },
                    transactions = transactions,
                    onButtonRefreshTransactionClick = { client.getTransactions() }
                )
            }
        }
    }
}

@Composable
private fun SignInScreen(
    onButtonSignInClick: (User) -> Unit,
    onButtonSignUpClick: () -> Unit,
    invalidCredentials: Boolean
) {
    Text(
        text = "Internet Banking",
        modifier = Modifier.requiredWidth(320.dp),
        style = MaterialTheme.typography.h4
    )
    Text(
        text = "Sign in as a user",
        modifier = Modifier.requiredWidth(320.dp),
        style = MaterialTheme.typography.subtitle1
    )
    Spacer(Modifier.height(32.dp))
    var username by remember { mutableStateOf("") }
    OutlinedTextField(
        value = username,
        onValueChange = { username = it },
        modifier = Modifier.requiredWidth(320.dp),
        label = { Text(text = "Username") },
    )
    Spacer(Modifier.height(8.dp))
    var password by remember { mutableStateOf("") }
    PasswordOutlinedTextField(
        password = password,
        onValueChange = { password = it },
        isError = invalidCredentials
    )
    Spacer(Modifier.height(16.dp))
    Button(
        onClick = { onButtonSignInClick(User(username = username, password = password)) },
    ) { Text(text = "Sign in") }
    Spacer(Modifier.height(8.dp))
    TextButton(
        onClick = { onButtonSignUpClick() }
    ) {
        Text(
            text = "Not registered? Sign up here",
            style = MaterialTheme.typography.caption
        )
    }
}

@Composable
private fun PasswordOutlinedTextField(
    password: String,
    onValueChange: (String) -> Unit,
    isError: Boolean
) {
    var passwordVisible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = password,
        onValueChange = { onValueChange(it) },
        modifier = Modifier.requiredWidth(320.dp),
        label = { Text(text = "Password") },
        trailingIcon = {
            IconButton(
                onClick = { passwordVisible = !passwordVisible }
            ) {
                Icon(
                    painter = svgResource(
                        if (passwordVisible) "visibility_off_black_24dp.svg" else "visibility_black_24dp.svg"
                    ),
                    contentDescription = "Password visibility toggle"
                )
            }
        },
        isError = isError,
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation()
    )
}

@Composable
private fun SignUpScreen(
    onButtonSignUpClick: (User) -> Unit,
    onButtonSignInClick: () -> Unit
) {
    Text(
        text = "Internet Banking",
        modifier = Modifier.requiredWidth(320.dp),
        style = MaterialTheme.typography.h4
    )
    Text(
        text = "Register an account",
        modifier = Modifier.requiredWidth(320.dp),
        style = MaterialTheme.typography.subtitle1
    )
    Spacer(Modifier.height(32.dp))
    var accountId by remember { mutableStateOf("") }
    OutlinedTextField(
        value = accountId,
        onValueChange = { accountId = it },
        modifier = Modifier.requiredWidth(320.dp),
        label = { Text(text = "Account ID") },
    )
    Spacer(Modifier.height(8.dp))
    var username by remember { mutableStateOf("") }
    OutlinedTextField(
        value = username,
        onValueChange = { username = it },
        modifier = Modifier.requiredWidth(320.dp),
        label = { Text(text = "Username") },
    )
    Spacer(Modifier.height(8.dp))
    var password by remember { mutableStateOf("") }
    PasswordOutlinedTextField(
        password = password,
        onValueChange = { password = it },
        isError = false
    )
    Spacer(Modifier.height(8.dp))
    var name by remember { mutableStateOf("") }
    OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        modifier = Modifier.requiredWidth(320.dp),
        label = { Text(text = "Name") },
    )
    Spacer(Modifier.height(16.dp))
    Button(
        onClick = {
            onButtonSignUpClick(
                User(accountId = accountId, username = username, password = password, name = name)
            )
        },
    ) { Text(text = "Sign up") }
    Spacer(Modifier.height(8.dp))
    TextButton(
        onClick = { onButtonSignInClick() }
    ) {
        Text(
            text = "Have an account? Sign in here",
            style = MaterialTheme.typography.caption
        )
    }
}

@Composable
private fun HomeScreen(
    user: User?,
    onButtonSignOutClick: () -> Unit,
    onButtonAddBalanceClick: (Int) -> Unit,
    transactionDestName: String?,
    transactionDestNameNotFound: Boolean,
    onButtonCheckDestAccountIdClick: (String) -> Unit,
    onButtonMakeTransactionClick: (String, Int) -> Unit,
    transactions: List<Transaction>,
    onButtonRefreshTransactionClick: () -> Unit
) {
    if (user == null) CircularProgressIndicator()
    else {
        Text(
            text = "Welcome, ${user.name}!",
            style = MaterialTheme.typography.h4
        )
        Text(
            text = "${user.username} (${user.accountId})",
            style = MaterialTheme.typography.subtitle1
        )
        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = { onButtonSignOutClick() }
        ) { Text(text = "Sign out") }
        Spacer(Modifier.height(32.dp))
        Row {
            Column {
                AddBalanceSection(user.balance ?: 0) { onButtonAddBalanceClick(it) }
                Spacer(Modifier.height(16.dp))
                MakeTransactionSection(
                    userBalance = user.balance ?: 0,
                    destName = transactionDestName,
                    destNameNotFound = transactionDestNameNotFound,
                    onButtonCheckDestAccountIdClick = { onButtonCheckDestAccountIdClick(it) },
                    onButtonMakeTransactionClick = { destAccountId, amount ->
                        onButtonMakeTransactionClick(destAccountId, amount)
                    }
                )
            }
            Spacer(Modifier.width(32.dp))
            Column {
                TransactionListSection(
                    userAccountId = user.accountId!!,
                    transactions = transactions,
                    onButtonRefreshClick = { onButtonRefreshTransactionClick() }
                )
            }
        }
    }
}

@Composable
private fun AddBalanceSection(
    balance: Int,
    onButtonAddBalanceClick: (Int) -> Unit
) {
    Text(
        text = "Balance",
        style = MaterialTheme.typography.subtitle2
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = "Your account balance is: ${balance.formatAsCurrency()}",
        style = MaterialTheme.typography.body2
    )
    Spacer(Modifier.height(8.dp))
    var amount by remember { mutableStateOf("") }
    AmountTextField(
        onValueChange = { amount = it }
    )
    Spacer(Modifier.height(8.dp))
    Button(
        onClick = { onButtonAddBalanceClick(amount.toInt()) },
        enabled = amount.toIntOrNull() != null
    ) { Text(text = "Add balance") }
}

@Composable
private fun AmountTextField(
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var amount by remember { mutableStateOf("") }
    OutlinedTextField(
        value = amount,
        onValueChange = {
            amount = it
            onValueChange(amount)
        },
        modifier = modifier,
        label = { Text(text = "Amount") },
        isError = if (amount.isNotEmpty()) amount.toIntOrNull() == null else false
    )
}

@Composable
private fun MakeTransactionSection(
    userBalance: Int,
    destName: String?,
    destNameNotFound: Boolean,
    onButtonCheckDestAccountIdClick: (String) -> Unit,
    onButtonMakeTransactionClick: (String, Int) -> Unit
) {
    Text(
        text = "Transaction",
        style = MaterialTheme.typography.subtitle2
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = "Make a transaction to another user",
        style = MaterialTheme.typography.body2
    )
    Spacer(Modifier.height(8.dp))
    var destAccountId by remember { mutableStateOf("") }
    OutlinedTextField(
        value = destAccountId,
        onValueChange = { destAccountId = it },
        label = { Text(text = "Destination account ID") },
        isError = destNameNotFound
    )
    Spacer(Modifier.height(8.dp))
    OutlinedButton(
        onClick = { onButtonCheckDestAccountIdClick(destAccountId) },
    ) { Text("Check account ID") }
    Spacer(Modifier.height(8.dp))
    var amount by remember { mutableStateOf("") }
    if (destName != null && !destNameNotFound) {
        AmountTextField(
            onValueChange = { amount = it }
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "You are about make a transaction to $destName",
            style = MaterialTheme.typography.body2
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                onButtonCheckDestAccountIdClick(destAccountId)
                onButtonMakeTransactionClick(destAccountId, amount.toInt())
            },
            enabled = !destNameNotFound && amount.toIntOrNull()?.let { it <= userBalance } ?: false
        ) { Text(text = "Transact") }
    }
}

@Composable
private fun TransactionListSection(
    userAccountId: String,
    transactions: List<Transaction>,
    onButtonRefreshClick: () -> Unit,
) {
    fun myself(accountId: String) = accountId == userAccountId
    Text(
        text = "Transaction List",
        style = MaterialTheme.typography.subtitle2
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = "Transaction made from or to your account",
        style = MaterialTheme.typography.body2
    )
    Spacer(Modifier.height(8.dp))
    TextButton(
        onClick = { onButtonRefreshClick() }
    ) {
        Text(text = "Refresh")
        Icon(
            imageVector = Icons.Rounded.Refresh,
            contentDescription = null
        )
    }
    Spacer(Modifier.height(8.dp))
    if (transactions.isEmpty()) {
        Text(
            text = "No transaction has been made",
            style = MaterialTheme.typography.caption
        )
        Spacer(Modifier.height(8.dp))
    }
    Column(
        modifier = Modifier.height(600.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        transactions.forEach {
            Surface(
                modifier = Modifier.selectable(false) {},
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, Color.Gray)
            ) {
                val fromMe = myself(it.from.accountId!!)
                val toMe = myself(it.to.accountId!!)
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    Row {
                        Text(
                            text = it.date.format(),
                            style = MaterialTheme.typography.overline
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "From ${if (fromMe) "me" else it.from.name} to ${if (toMe) "me" else it.to.name}",
                            color = MaterialTheme.colors.primary,
                            style = MaterialTheme.typography.overline
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = it.amount.formatAsCurrency(),
                        style = MaterialTheme.typography.body1
                    )
                }
            }
        }
    }
}