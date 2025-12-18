package com.ts.cbdcwalletapp.data

data class LoginRequest(
    val phoneNumber: String,
    val password: String
)

data class RegisterRequest(
    val name: String,
    val phoneNumber: String,
    val password: String
)

data class AuthResponse(
    val token: String,
    val user: User
)

data class User(
    val id: String? = null,        // From login response
    val _id: String? = null,       // From populated MongoDB objects
    val name: String,
    val phoneNumber: String? = null // Optional - not present in transaction user objects
) {
    // Get user ID from whichever field is available
    val userId: String
        get() = id ?: _id ?: ""
}

data class WalletResponse(
    val balance: Double,
    val currency: String
)

data class LoadRequest(
    val amount: Double
)

data class TransferRequest(
    val toPhoneNumber: String,
    val amount: Double
)

data class Transaction(
    val _id: String,
    val fromWallet: WalletInfo?,
    val toWallet: WalletInfo?,
    val amount: Double,
    val type: String,
    val status: String,
    val createdAt: String
)

// User info in wallet - only contains _id and name from backend
data class TransactionUser(
    val _id: String,
    val name: String
)

data class WalletInfo(
    val _id: String,
    val user: TransactionUser?
)
