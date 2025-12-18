package com.ts.cbdcwalletapp.data

import android.content.Context
import android.content.SharedPreferences

class TokenManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    fun saveToken(token: String) {
        prefs.edit().putString("jwt_token", token).apply()
    }

    fun getToken(): String? {
        return prefs.getString("jwt_token", null)
    }

    fun saveUser(user: User) {
        prefs.edit()
            .putString("user_id", user.userId)
            .putString("user_name", user.name)
            .putString("user_phone", user.phoneNumber)
            .apply()
    }

    fun getUser(): User? {
        val id = prefs.getString("user_id", null)
        val name = prefs.getString("user_name", null)
        val phone = prefs.getString("user_phone", null)

        return if (id != null && name != null && phone != null) {
            User(id = id, name = name, phoneNumber = phone)
        } else {
            null
        }
    }

    fun clearToken() {
        prefs.edit().clear().apply()
    }
}
