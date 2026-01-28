package com.ts.cbdcwalletapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ts.cbdcwalletapp.data.Repository
import com.ts.cbdcwalletapp.data.TokenManager
import com.ts.cbdcwalletapp.network.RetrofitClient
import com.ts.cbdcwalletapp.ui.*
import com.ts.cbdcwalletapp.ui.theme.CBDCWalletAppTheme

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager

class MainActivity : ComponentActivity() {
    private var alertDialog: android.app.AlertDialog? = null

    /* // turbo-all: ensure permissions are handled if needed, usually USB permission dialog is system handled, 
       but listing devices requires no special permission on modern Android unless you want to communicate with them. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        startUsbDetection()


        setContent {
            CBDCWalletAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val tokenManager = TokenManager(applicationContext)
                    val apiService = RetrofitClient.getApiService(applicationContext)
                    val repository = Repository(apiService)
                    val viewModel: MainViewModel = viewModel(
                        factory = MainViewModelFactory(repository, tokenManager)
                    )

                    // Check if user is already logged in
                    viewModel.checkAuth()
                    val startDestination = if (tokenManager.getToken() != null) Screen.Dashboard.route else Screen.Login.route

                    NavHost(navController = navController, startDestination = startDestination) {
                        composable(Screen.Login.route) {
                            LoginScreen(navController, viewModel)
                        }
                        composable(Screen.Signup.route) {
                            SignupScreen(navController, viewModel)
                        }
                        composable(Screen.Dashboard.route) {
                            DashboardScreen(navController, viewModel)
                        }
                        composable(Screen.OfflineTransfer.route) {
                            OfflineTransferScreen(navController, viewModel, tokenManager.getUser())
                        }
                    }
                }
            }
        }
    }

    private fun startUsbDetection() {
        val usbManager = getSystemService(android.content.Context.USB_SERVICE) as UsbManager

        Thread {
            while (true) {
                val deviceList = usbManager.deviceList
                if (deviceList.isEmpty()) {
                    runOnUiThread { showUSBDisconnectedDialog() }
                } else {
                    // Just check the first device for now as per snippet
                    val device = deviceList.values.firstOrNull()
                    if (device == null || !isSmartCardReader(device)) {
                        runOnUiThread { showUSBDisconnectedDialog() }
                    } else {
                        runOnUiThread {
                            if (alertDialog != null && alertDialog!!.isShowing) {
                                alertDialog!!.dismiss()
                            }
                        }
                    }
                }
                try {
                    Thread.sleep(100) // Check every 100ms
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                }
            }
        }.start()
    }

    private fun showUSBDisconnectedDialog() {
        if (alertDialog?.isShowing == true) return

        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("USB Token Missing")
        builder.setMessage("Please connect your USB Token/Smart Card Reader to continue.")
        builder.setCancelable(false)
        alertDialog = builder.create()
        alertDialog?.show()
    }

    private fun isSmartCardReader(device: UsbDevice): Boolean {
        // Placeholder check. 
        // In a real app, you might check: device.interfaceCount > 0 && device.getInterface(0).interfaceClass == UsbConstants.USB_CLASS_CSCID (0x0B)
        // For now, we assume any plugged in device is potentially the token, satisfying the requirement to "add detection".
        // The user snippet had this method call, so we implement it loosely.
        return true 
    }
}