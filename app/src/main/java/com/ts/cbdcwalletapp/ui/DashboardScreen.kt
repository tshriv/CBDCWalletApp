package com.ts.cbdcwalletapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ts.cbdcwalletapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavController, viewModel: MainViewModel) {
    val walletState by viewModel.walletState.collectAsState()
    val transactionState by viewModel.transactionState.collectAsState()
    val authState by viewModel.authState.collectAsState()
    val currentUserId = (authState as? AuthState.Success)?.user?.userId
    var showLoadDialog by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.getBalance()
        viewModel.getTransactions()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CBDC Wallet", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = {
                        viewModel.logout()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Dashboard.route) { inclusive = true }
                        }
                    }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // Balance Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        // Greeting with username
                        val userName = (authState as? AuthState.Success)?.user?.name ?: "User"
                        Text(
                            text = "Hello, $userName",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Total Balance",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (walletState is WalletState.Success) {
                            val wallet = (walletState as WalletState.Success).wallet
                            val currency = wallet.currency ?: "CBDC" // Fix: Handle null currency
                            Text(
                                text = "$currency ${wallet.balance}",
                                style = MaterialTheme.typography.displayMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        } else if (walletState is WalletState.Loading) {
                            CircularProgressIndicator(color = Color.White)
                        } else {
                            Text(text = "Error loading balance", color = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ActionButton(
                    text = "Load Funds",
                    icon = Icons.Default.Add,
                    onClick = { showLoadDialog = true }
                )
                ActionButton(
                    text = "Transfer",
                    icon = Icons.Default.ArrowForward,
                    onClick = { showTransferDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Recent Transactions
            Text(
                text = "Recent Transactions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (transactionState is TransactionState.Success) {
                val transactions = (transactionState as TransactionState.Success).transactions
                if (transactions.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No recent transactions", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(transactions) { transaction ->
                            TransactionItem(transaction, currentUserId)
                        }
                    }
                }
            } else if (transactionState is TransactionState.Loading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (transactionState is TransactionState.Error) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Error: ${(transactionState as TransactionState.Error).message}",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    if (showLoadDialog) {
        LoadFundsDialog(
            onDismiss = { showLoadDialog = false },
            onConfirm = { amount ->
                viewModel.loadFunds(amount)
                showLoadDialog = false
            }
        )
    }

    if (showTransferDialog) {
        TransferFundsDialog(
            onDismiss = { showTransferDialog = false },
            onConfirm = { phone, amount ->
                viewModel.transferFunds(phone, amount)
                showTransferDialog = false
            }
        )
    }
}

@Composable
fun ActionButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledIconButton(
            onClick = onClick,
            modifier = Modifier.size(64.dp),
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Icon(icon, contentDescription = text, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun TransactionItem(transaction: com.ts.cbdcwalletapp.data.Transaction, currentUserId: String?) {
    val isDeposit = transaction.type == "DEPOSIT"
    
    // Check if current user is the SENDER (fromWallet) - means money was SENT OUT (negative)
    val isSentByMe = transaction.fromWallet?.user?._id == currentUserId
    // Check if current user is the RECEIVER (toWallet) - means money was RECEIVED (positive)
    val isReceivedByMe = isDeposit || (transaction.toWallet?.user?._id == currentUserId && !isSentByMe)
    
    val title = when {
        isDeposit -> "Load Funds"
        isSentByMe -> "Sent to ${transaction.toWallet?.user?.name ?: "Unknown"}"
        isReceivedByMe -> "Received from ${transaction.fromWallet?.user?.name ?: "Unknown"}"
        else -> transaction.type
    }

    val amountColor = if (isReceivedByMe) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
    val amountSign = if (isReceivedByMe) "+" else "-"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = transaction.createdAt.take(10), // Simple date formatting
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$amountSign ${transaction.amount}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
                Text(
                    text = transaction.status,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (transaction.status == "COMPLETED") MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun LoadFundsDialog(onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var amount by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Load Funds") },
        text = {
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = {
                amount.toDoubleOrNull()?.let { onConfirm(it) }
            }) {
                Text("Load")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun TransferFundsDialog(onDismiss: () -> Unit, onConfirm: (String, Double) -> Unit) {
    var phoneNumber by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Transfer Funds") },
        text = {
            Column {
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Recipient Phone") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val amt = amount.toDoubleOrNull()
                if (phoneNumber.isNotBlank() && amt != null) {
                    onConfirm(phoneNumber, amt)
                }
            }) {
                Text("Transfer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
