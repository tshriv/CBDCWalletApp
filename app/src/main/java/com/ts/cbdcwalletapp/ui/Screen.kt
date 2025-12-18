package com.ts.cbdcwalletapp.ui

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Signup : Screen("signup")
    object Dashboard : Screen("dashboard")
}
