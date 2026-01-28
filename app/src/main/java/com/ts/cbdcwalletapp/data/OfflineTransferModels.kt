package com.ts.cbdcwalletapp.data

import com.google.gson.Gson

// Protocol types
enum class TransferStep {
    REQUEST, INIT, ACK, COMMIT, SUCCESS
}

// Base class for parsing type
data class BaseTransferMessage(
    val type: TransferStep
)

// Protocols
// STEP 1: Receiver shows this
data class OfflineRequest(
    val type: TransferStep = TransferStep.REQUEST,
    val to: String // device_id of receiver
)

// STEP 2: Sender scans REQUEST, shows this
/** will check if amount available
 * **/
data class OfflineInit(
    val type: TransferStep = TransferStep.INIT,
    val tx_id: String,
    val from: String, // device_id of sender
    val to: String,   // device_id of receiver
    val amount: Double
)

// STEP 3: Receiver scans INIT, verifies, shows this
data class OfflineAck(
    val type: TransferStep = TransferStep.ACK,
    val tx_id: String,
    val status: String = "OK"
)

// STEP 4: Sender scans ACK, commits (deducts), shows this
data class OfflineCommit(
    val type: TransferStep = TransferStep.COMMIT,
    val tx_id: String,
    val status: String = "DONE"
)

// STEP 5: Receiver scans COMMIT, adds balance, shows this
data class OfflineSuccess(
    val type: TransferStep = TransferStep.SUCCESS,
    val tx_id: String,
    val message: String = "SUCCESS"
)
//Step 6: Sender scans SUCCESS, shows its own SUCCESS Screen
/** after this sender scan SUCCESS and shows its own SUCCESS Screen **/

object TransferProtocol {
    private val gson = Gson()

    fun toJson(data: Any): String {
        return gson.toJson(data)
    }

    fun parseMessage(json: String): BaseTransferMessage? {
        return try {
            gson.fromJson(json, BaseTransferMessage::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun parseRequest(json: String): OfflineRequest? = try { gson.fromJson(json, OfflineRequest::class.java) } catch (e: Exception) { null }
    fun parseInit(json: String): OfflineInit? = try { gson.fromJson(json, OfflineInit::class.java) } catch (e: Exception) { null }
    fun parseAck(json: String): OfflineAck? = try { gson.fromJson(json, OfflineAck::class.java) } catch (e: Exception) { null }
    fun parseCommit(json: String): OfflineCommit? = try { gson.fromJson(json, OfflineCommit::class.java) } catch (e: Exception) { null }
    fun parseSuccess(json: String): OfflineSuccess? = try { gson.fromJson(json, OfflineSuccess::class.java) } catch (e: Exception) { null }
}
