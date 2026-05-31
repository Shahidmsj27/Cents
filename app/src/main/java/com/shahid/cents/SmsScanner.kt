package com.shahid.cents

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SmsScanner(
    private val context: Context,
    private val repository: TransactionRepository
) {
    suspend fun scanRange(startMillis: Long, endMillis: Long): ScanResult = withContext(Dispatchers.IO) {
        var scanned = 0
        var parsed = 0
        var imported = 0
        try {
            val uri = Uri.parse("content://sms")
            val projection = arrayOf("address", "body", "date", "type")
            val selection = "date >= ? AND date < ?"
            val selectionArgs = arrayOf(startMillis.toString(), endMillis.toString())
            context.contentResolver.query(uri, projection, selection, selectionArgs, "date DESC")?.use { cursor ->
                val senderIndex = cursor.getColumnIndexOrThrow("address")
                val bodyIndex = cursor.getColumnIndexOrThrow("body")
                val dateIndex = cursor.getColumnIndexOrThrow("date")
                val typeIndex = cursor.getColumnIndexOrThrow("type")
                while (cursor.moveToNext()) {
                    val messageType = cursor.getInt(typeIndex)
                    if (messageType != 1) continue
                    scanned++
                    val sender = cursor.getString(senderIndex).orEmpty()
                    val body = cursor.getString(bodyIndex).orEmpty()
                    val date = cursor.getLong(dateIndex)
                    when (repository.processSms(sender, body, date)) {
                        ProcessResult.Imported -> {
                            parsed++
                            imported++
                        }
                        ProcessResult.Duplicate -> parsed++
                        ProcessResult.NotTransaction -> Unit
                    }
                }
            }
            Log.i("CentsSmsScanner", "Scanned=$scanned parsed=$parsed imported=$imported")
            ScanResult(scanned = scanned, parsed = parsed, imported = imported)
        } catch (error: SecurityException) {
            Log.e("CentsSmsScanner", "Missing SMS permission", error)
            ScanResult(scanned = scanned, parsed = parsed, imported = imported, error = "SMS permission is missing or blocked")
        } catch (error: Throwable) {
            Log.e("CentsSmsScanner", "Failed to scan SMS", error)
            ScanResult(scanned = scanned, parsed = parsed, imported = imported, error = error.message ?: error.javaClass.simpleName)
        }
    }
}
