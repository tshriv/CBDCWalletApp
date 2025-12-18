package com.ts.cbdcwalletapp.network

import com.ts.cbdcwalletapp.data.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<Void>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @GET("api/wallet/balance")
    suspend fun getBalance(): Response<WalletResponse>

    @POST("api/wallet/load")
    suspend fun loadFunds(@Body request: LoadRequest): Response<WalletResponse>

    @POST("api/wallet/transfer")
    suspend fun transferFunds(@Body request: TransferRequest): Response<WalletResponse>

    @GET("api/wallet/transactions")
    suspend fun getTransactions(): Response<List<Transaction>>
}
