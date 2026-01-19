package com.ts.cbdcwalletapp.ui

import android.Manifest
import android.annotation.SuppressLint
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.ts.cbdcwalletapp.data.*
import com.ts.cbdcwalletapp.utils.QRUtils
import java.util.UUID
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun OfflineTransferScreen(
    navController: NavController,
    viewModel: MainViewModel,
    currentUser: User?
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    if (cameraPermissionState.status.isGranted) {
        OfflineTransferContent(navController, viewModel, currentUser)
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Camera permission is needed for offline transfer",
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                Text("Grant Permission")
            }
        }
    }
}

@Composable
fun OfflineTransferContent(
    navController: NavController,
    viewModel: MainViewModel,
    currentUser: User?
) {
    val context = LocalContext.current
    var isReceiver by remember { mutableStateOf(false) }
    var isSender by remember { mutableStateOf(false) }
    var currentStep by remember { mutableStateOf(TransferStep.REQUEST) }
    var statusMessage by remember { mutableStateOf("Select Role") }
    var amountToSend by remember { mutableStateOf("") }

    // State to hold the QR content to display
    var qrContent by remember { mutableStateOf<String?>(null) }

    // State to act as a lock for processing scanned codes
    var isProcessingScan by remember { mutableStateOf(false) }

    // Transaction details
    var transactionId by remember { mutableStateOf("") }
    var otherDeviceId by remember { mutableStateOf("") }
    var transferAmount by remember { mutableStateOf(0.0) }

    val walletState by viewModel.walletState.collectAsState()

    // Helper to reset
    fun reset() {
        isReceiver = false
        isSender = false
        currentStep = TransferStep.REQUEST
        statusMessage = "Select Role"
        qrContent = null
        isProcessingScan = false
        transactionId = ""
        otherDeviceId = ""
        transferAmount = 0.0
        amountToSend = ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "Offline Transfer",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (!isReceiver && !isSender) {
            // Role Selection
            Text(
                "Select your role for this transaction",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Amount Input for potential Sender
            OutlinedTextField(
                value = amountToSend,
                onValueChange = {
                    if (it.all { char -> char.isDigit() || char == '.' }) amountToSend = it
                },
                label = { Text("Amount to Send ($)") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = {
                    val amt = amountToSend.toDoubleOrNull()
                    if (amt != null && amt > 0) {
                        transferAmount = amt
                        isSender = true
                        statusMessage = "Scanning for Receiver..."
                        currentStep = TransferStep.REQUEST // Sender starts by scanning REQUEST
                    } else {
                        Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT)
                            .show()
                    }
                }) {
                    Text("Send Money")
                }

                Button(onClick = {
                    isReceiver = true
                    // Generate REQUEST QR
                    val req = OfflineRequest(
                        to = currentUser?.phoneNumber ?: currentUser?.userId ?: "unknown"
                    )
                    qrContent = TransferProtocol.toJson(req)
                    statusMessage = "Show QR to Sender"
                    currentStep = TransferStep.REQUEST // Receiver starts by showing REQUEST
                }) {
                    Text("Receive Money")
                }
            }
        } else if (currentStep == TransferStep.SUCCESS) {
            // SUCCESS UI
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🎉", style = MaterialTheme.typography.displayLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Transaction Complete!",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isSender) "Sent $$transferAmount successfully." else "Received $$transferAmount successfully.",
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(modifier = Modifier.height(32.dp))

                // If Receiver, show SUCCESS QR so Sender can also see success
                if (isReceiver) {
                    qrContent?.let { content ->
                        val bitmap = QRUtils.generateQRCode(content)
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Success QR",
                                modifier = Modifier.size(200.dp)
                            )
                            Text("Show this to Sender to confirm")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = { reset() }) {
                    Text("Start New Transaction")
                }
            }
        } else {
            // Active Transaction UI

            // Active Transaction UI - Split Screen
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Half: QR Code Display + Status
                Card(
                    modifier = Modifier
                        .weight(0.6f)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (isSender) "My Role: Sender ($$transferAmount)" else "My Role: Receiver",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        if (qrContent != null) {
                            val bitmap = QRUtils.generateQRCode(qrContent!!)
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "QR Code",
                                    modifier = Modifier
                                        .size(280.dp) // Increased size for better scanning
                                        .padding(8.dp)
                                )
                                Text(
                                    text = statusMessage,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier.size(280.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                            Text(
                                text = statusMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Bottom Half: Camera Preview
                Card(
                    modifier = Modifier
                        .weight(0.4f)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        var lastScannedDebug by remember { mutableStateOf("") }

                        CameraPreview(
                            onCodeScanned = { scannedData ->
                                lastScannedDebug = scannedData // Show debug info

                                if (isProcessingScan) return@CameraPreview

                                val message = TransferProtocol.parseMessage(scannedData)
                                    ?: return@CameraPreview

                                // STRICT ROLE VALIDATION
                                // Sender should ONLY parse: REQUEST, ACK, SUCCESS
                                // Receiver should ONLY parse: INIT, COMMIT

                                val isValidScan = if (isSender) {
                                    message.type == TransferStep.REQUEST || message.type == TransferStep.ACK || message.type == TransferStep.SUCCESS
                                } else {
                                    message.type == TransferStep.INIT || message.type == TransferStep.COMMIT
                                }

                                if (!isValidScan) return@CameraPreview

                                when {
                                    // SENDER: Use Step 1 (Scan REQUEST), Generate Step 2 (Show INIT)
                                    isSender && currentStep == TransferStep.REQUEST && message.type == TransferStep.REQUEST -> {
                                        isProcessingScan = true
                                        val req = TransferProtocol.parseRequest(scannedData)
                                        if (req != null) {
                                            otherDeviceId = req.to

                                            // Check balance
                                            val currentBalance =
                                                (walletState as? WalletState.Success)?.wallet?.balance
                                                    ?: 0.0
                                            if (currentBalance >= transferAmount) {
                                                transactionId = UUID.randomUUID().toString()
                                                val initMsg = OfflineInit(
                                                    tx_id = transactionId,
                                                    from = currentUser?.phoneNumber
                                                        ?: currentUser?.userId ?: "sender",
                                                    to = otherDeviceId,
                                                    amount = transferAmount
                                                )
                                                qrContent = TransferProtocol.toJson(initMsg)
                                                statusMessage = "Initializing..."
                                                currentStep = TransferStep.INIT
                                                isProcessingScan = false
                                            } else {
                                                // Handle Error
                                                isProcessingScan = false
                                            }
                                        } else {
                                            isProcessingScan = false
                                        }
                                    }

                                    // RECEIVER: Use Step 2 (Scan INIT), Generate Step 3 (Show ACK)
                                    isReceiver && currentStep == TransferStep.REQUEST && message.type == TransferStep.INIT -> {
                                        isProcessingScan = true
                                        val initMsg = TransferProtocol.parseInit(scannedData)
                                        if (initMsg != null && initMsg.to == (currentUser?.phoneNumber
                                                ?: currentUser?.userId ?: "unknown")
                                        ) {
                                            transactionId = initMsg.tx_id
                                            transferAmount = initMsg.amount
                                            otherDeviceId = initMsg.from

                                            val ackMsg = OfflineAck(tx_id = transactionId)
                                            qrContent = TransferProtocol.toJson(ackMsg)
                                            statusMessage = "Confirming..."
                                            currentStep = TransferStep.ACK
                                            isProcessingScan = false
                                        } else {
                                            isProcessingScan = false
                                        }
                                    }

                                    // SENDER: Use Step 3 (Scan ACK), Generate Step 4 (Show COMMIT) & Deduct
                                    isSender && currentStep == TransferStep.INIT && message.type == TransferStep.ACK -> {
                                        isProcessingScan = true
                                        val ackMsg = TransferProtocol.parseAck(scannedData)
                                        if (ackMsg != null && ackMsg.tx_id == transactionId) {
                                            viewModel.deductBalance(transferAmount)

                                            val commitMsg = OfflineCommit(tx_id = transactionId)
                                            qrContent = TransferProtocol.toJson(commitMsg)
                                            statusMessage = "Finalizing..."
                                            currentStep = TransferStep.COMMIT
                                            isProcessingScan = false
                                        } else {
                                            isProcessingScan = false
                                        }
                                    }

                                    // RECEIVER: Use Step 4 (Scan COMMIT), Generate Final & Add
                                    isReceiver && currentStep == TransferStep.ACK && message.type == TransferStep.COMMIT -> {
                                        isProcessingScan = true
                                        val commitMsg = TransferProtocol.parseCommit(scannedData)
                                        if (commitMsg != null && commitMsg.tx_id == transactionId) {
                                            viewModel.addBalance(transferAmount)

                                            // Make Success QR
                                            val successMsg = OfflineSuccess(tx_id = transactionId)
                                            qrContent = TransferProtocol.toJson(successMsg)
                                            statusMessage = "Success!"
                                            currentStep = TransferStep.SUCCESS
                                            isProcessingScan =
                                                false // Receiver stops scanning, just shows SUCCESS QR
                                        } else {
                                            isProcessingScan = false
                                        }
                                    }

                                    // SENDER: Use Step 5 (Scan SUCCESS), Show Final Screen
                                    isSender && currentStep == TransferStep.COMMIT && message.type == TransferStep.SUCCESS -> {
                                        isProcessingScan = true
                                        val successMsg = TransferProtocol.parseSuccess(scannedData)
                                        if (successMsg != null && successMsg.tx_id == transactionId) {
                                            statusMessage = "Transfer Complete!"
                                            qrContent = null
                                            currentStep = TransferStep.SUCCESS
                                            isProcessingScan = false
                                        } else {
                                            isProcessingScan = false
                                        }
                                    }
                                }
                            }
                        )

                        // Scanner Overlay
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(200.dp)
                                .border(
                                    2.dp,
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(12.dp)
                                )
                        )

                        // Phase Indicator overlay on Camera
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.TopStart
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "Scan: ${
                                            if (isSender) when (currentStep) {
                                                TransferStep.REQUEST -> "Receiver's Request"
                                                TransferStep.INIT -> "Scan ACK"
                                                TransferStep.COMMIT -> "Scan Final Confirmation"
                                                else -> "Complete"
                                            } else when (currentStep) {
                                                TransferStep.REQUEST -> "Sender's Init"
                                                TransferStep.ACK -> "Sender's Commit"
                                                else -> "Complete"
                                            }
                                        }",
                                        color = MaterialTheme.colorScheme.onSecondary,
                                        modifier = Modifier.padding(
                                            horizontal = 8.dp,
                                            vertical = 4.dp
                                        ),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }

                                // DEBUG OVERLAY
                                if (lastScannedDebug.isNotEmpty()) {
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.6f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "Last: ${lastScannedDebug.take(20)}...",
                                            color = Color.Green,
                                            modifier = Modifier.padding(
                                                horizontal = 8.dp,
                                                vertical = 4.dp
                                            ),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { reset() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Cancel Transaction")
            }
        }
    }
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@SuppressLint("RestrictedApi") // Suppress for ImageAnalysis usage if needed, or proper Camerax handling
@Composable
fun CameraPreview(
    onCodeScanned: (String) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setTargetResolution(
                        android.util.Size(
                            1280,
                            720
                        )
                    ) // Request HD resolution for better dense QR detection
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val image = InputImage.fromMediaImage(
                                    mediaImage,
                                    imageProxy.imageInfo.rotationDegrees
                                )
                                val scanner = BarcodeScanning.getClient()
                                scanner.process(image)
                                    .addOnSuccessListener { barcodes ->
                                        for (barcode in barcodes) {
                                            barcode.rawValue?.let { value ->
                                                onCodeScanned(value)
                                            }
                                        }
                                    }
                                    .addOnCompleteListener {
                                        imageProxy.close()
                                    }
                            } else {
                                imageProxy.close()
                            }
                        }
                    }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_FRONT_CAMERA, // USER REQUESTED FRONT CAMERA
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    Log.e("CameraPreview", "Binding failed", e)
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}
