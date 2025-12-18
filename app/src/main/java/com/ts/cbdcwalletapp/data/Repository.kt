package com.ts.cbdcwalletapp.data

import com.ts.cbdcwalletapp.network.ApiService
import retrofit2.Response

class Repository(private val apiService: ApiService) {
    suspend fun register(request: RegisterRequest): Response<Void> = apiService.register(request)
    suspend fun login(request: LoginRequest): Response<AuthResponse> = apiService.login(request)
    suspend fun getBalance(): Response<WalletResponse> = apiService.getBalance()
    suspend fun loadFunds(request: LoadRequest): Response<WalletResponse> = apiService.loadFunds(request)
    suspend fun transferFunds(request: TransferRequest): Response<WalletResponse> = apiService.transferFunds(request)
    suspend fun getTransactions(): Response<List<Transaction>> = apiService.getTransactions()
}
