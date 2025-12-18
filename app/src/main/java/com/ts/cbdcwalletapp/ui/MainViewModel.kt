package com.ts.cbdcwalletapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ts.cbdcwalletapp.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.Response

class MainViewModel(private val repository: Repository, private val tokenManager: TokenManager) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    private val _walletState = MutableStateFlow<WalletState>(WalletState.Idle)
    val walletState: StateFlow<WalletState> = _walletState

    private val _transactionState = MutableStateFlow<TransactionState>(TransactionState.Idle)
    val transactionState: StateFlow<TransactionState> = _transactionState

    fun checkAuth() {
        val token = tokenManager.getToken()
        val user = tokenManager.getUser()
        if (token != null) {
            _authState.value = AuthState.Success(user)
        }
    }

    fun register(request: RegisterRequest) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = repository.register(request)
                if (response.isSuccessful) {
                    _authState.value = AuthState.Registered
                } else {
                    _authState.value = AuthState.Error(response.message())
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun login(request: LoginRequest) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = repository.login(request)
                if (response.isSuccessful && response.body() != null) {
                    val authResponse = response.body()!!
                    tokenManager.saveToken(authResponse.token)
                    tokenManager.saveUser(authResponse.user)
                    _authState.value = AuthState.Success(authResponse.user)
                } else {
                    _authState.value = AuthState.Error(response.message())
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun logout() {
        tokenManager.clearToken()
        _authState.value = AuthState.Idle
        _walletState.value = WalletState.Idle
        _transactionState.value = TransactionState.Idle
    }

    fun getBalance() {
        viewModelScope.launch {
            _walletState.value = WalletState.Loading
            try {
                val response = repository.getBalance()
                if (response.isSuccessful && response.body() != null) {
                    _walletState.value = WalletState.Success(response.body()!!)
                } else {
                    _walletState.value = WalletState.Error(response.message())
                }
            } catch (e: Exception) {
                _walletState.value = WalletState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun loadFunds(amount: Double) {
        viewModelScope.launch {
            _walletState.value = WalletState.Loading
            try {
                val response = repository.loadFunds(LoadRequest(amount))
                if (response.isSuccessful && response.body() != null) {
                    _walletState.value = WalletState.Success(response.body()!!)
                    getTransactions() // Refresh transactions
                } else {
                    _walletState.value = WalletState.Error(response.message())
                }
            } catch (e: Exception) {
                _walletState.value = WalletState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun transferFunds(toPhoneNumber: String, amount: Double) {
        viewModelScope.launch {
            _walletState.value = WalletState.Loading
            try {
                val response = repository.transferFunds(TransferRequest(toPhoneNumber, amount))
                if (response.isSuccessful && response.body() != null) {
                    _walletState.value = WalletState.Success(response.body()!!)
                    getTransactions() // Refresh transactions
                } else {
                    _walletState.value = WalletState.Error(response.message())
                }
            } catch (e: Exception) {
                _walletState.value = WalletState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun getTransactions() {
        viewModelScope.launch {
            _transactionState.value = TransactionState.Loading
            try {
                val response = repository.getTransactions()
                if (response.isSuccessful && response.body() != null) {
                    _transactionState.value = TransactionState.Success(response.body()!!)
                } else {
                    _transactionState.value = TransactionState.Error(response.message())
                }
            } catch (e: Exception) {
                _transactionState.value = TransactionState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Registered : AuthState()
    data class Success(val user: User?) : AuthState()
    data class Error(val message: String) : AuthState()
}

sealed class WalletState {
    object Idle : WalletState()
    object Loading : WalletState()
    data class Success(val wallet: WalletResponse) : WalletState()
    data class Error(val message: String) : WalletState()
}

sealed class TransactionState {
    object Idle : TransactionState()
    object Loading : TransactionState()
    data class Success(val transactions: List<Transaction>) : TransactionState()
    data class Error(val message: String) : TransactionState()
}

class MainViewModelFactory(private val repository: Repository, private val tokenManager: TokenManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository, tokenManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
