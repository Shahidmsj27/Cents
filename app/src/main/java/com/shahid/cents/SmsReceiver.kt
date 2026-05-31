package com.shahid.cents

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val app = context.applicationContext as CentsApp
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                messages.forEach { message ->
                    app.repository.processSms(
                        sender = message.displayOriginatingAddress.orEmpty(),
                        body = message.displayMessageBody.orEmpty(),
                        timestamp = message.timestampMillis
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
