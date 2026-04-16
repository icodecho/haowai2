package evilcode.notification.hwpush

import android.content.Intent
import android.util.Log
import com.huawei.hms.push.HmsMessageService
import com.huawei.hms.push.RemoteMessage
import com.huawei.hms.push.SendException

class MyPushService : HmsMessageService() {

    override fun onNewToken(token: String?) {
        Log.i(TAG, "onNewToken: $token")
        if (!token.isNullOrEmpty()) {
            val intent = Intent(ACTION_PUSH_EVENT)
            intent.setPackage(packageName)
            intent.putExtra("method", "onNewToken")
            intent.putExtra("token", token)
            sendBroadcast(intent)
        }
    }

    override fun onMessageReceived(message: RemoteMessage?) {
        Log.i(TAG, "onMessageReceived called")
        if (message == null) {
            Log.e(TAG, "Received message entity is null!")
            return
        }

        val data = message.data
        val msgId = message.messageId ?: ""
        val notification = message.notification
        val title = notification?.title ?: ""
        val body = notification?.body ?: ""

        Log.i(TAG, "getData: $data, msgId: $msgId, title: $title, body: $body")

        val isDataMessage = title.isEmpty() && body.isEmpty() && data.isNotEmpty()

        if (isDataMessage) {
            NotificationHelper.showDataMessageNotification(this, title, body, data, msgId)
        } else {
            NotificationMessageCache.put(title, body, data)
        }

        val intent = Intent(ACTION_PUSH_EVENT)
        intent.setPackage(packageName)
        intent.putExtra("method", "onMessageReceived")
        intent.putExtra("msg", "收到消息: id=$msgId, data=$data")
        intent.putExtra("title", title)
        intent.putExtra("body", body)
        intent.putExtra("data", data)
        intent.putExtra("msgId", msgId)
        intent.putExtra("isDataMessage", isDataMessage)
        sendBroadcast(intent)
    }

    override fun onMessageSent(msgId: String?) {
        Log.i(TAG, "onMessageSent: $msgId")
        val intent = Intent(ACTION_PUSH_EVENT)
        intent.setPackage(packageName)
        intent.putExtra("method", "onMessageSent")
        intent.putExtra("msg", "消息发送成功: $msgId")
        sendBroadcast(intent)
    }

    override fun onSendError(msgId: String?, exception: Exception?) {
        Log.e(TAG, "onSendError: $msgId, error: $exception")
        val intent = Intent(ACTION_PUSH_EVENT)
        intent.setPackage(packageName)
        intent.putExtra("method", "onSendError")
        intent.putExtra("msg", "消息发送失败: $msgId, error: ${exception?.message}")
        sendBroadcast(intent)
    }

    override fun onTokenError(e: Exception) {
        Log.e(TAG, "onTokenError: $e")
        val intent = Intent(ACTION_PUSH_EVENT)
        intent.setPackage(packageName)
        intent.putExtra("method", "onTokenError")
        intent.putExtra("msg", "Token错误: ${e.message}")
        sendBroadcast(intent)
    }

    companion object {
        private const val TAG = "HaoWaiPush"
        const val ACTION_PUSH_EVENT = "evilcode.notification.hwpush.ACTION_PUSH_EVENT"
    }
}
