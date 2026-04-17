package evilcode.notification.hwpush

import android.Manifest
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.huawei.hms.aaid.HmsInstanceId
import com.huawei.hms.common.ApiException
import com.huawei.hms.push.HmsMessaging
import evilcode.notification.hwpush.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var pushToken: String = ""
    private var isTokenVisible = false
    private val handler = Handler(Looper.getMainLooper())
    private val tokenHideRunnable = Runnable {
        isTokenVisible = false
        updateTokenDisplay()
    }

    private val pushReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.getStringExtra("method")) {
                "onNewToken" -> {
                    val token = intent.getStringExtra("token") ?: ""
                    if (token.isNotEmpty()) {
                        pushToken = token
                        TokenManager.saveToken(this@MainActivity, token)
                        updateTokenDisplay()
                        appendLog("收到新Token: ${maskToken(token)}")
                    }
                }
                "onMessageReceived" -> {
                    val msg = intent.getStringExtra("msg") ?: ""
                    val title = intent.getStringExtra("title") ?: ""
                    val body = intent.getStringExtra("body") ?: ""
                    val data = intent.getStringExtra("data") ?: ""
                    val msgId = intent.getStringExtra("msgId") ?: ""
                    val isDataMessage = intent.getBooleanExtra("isDataMessage", false)
                    appendLog(msg)

                    if (isDataMessage) {
                        val record = MessageRecord(
                            title = title,
                            content = body,
                            data = data,
                            msgId = msgId,
                            receiveTime = System.currentTimeMillis(),
                            type = "透传消息"
                        )
                        MessageRecordManager.addRecord(this@MainActivity, record)
                    }
                }
                "onMessageSent" -> {
                    val msg = intent.getStringExtra("msg") ?: ""
                    appendLog(msg)
                }
                "onSendError" -> {
                    val msg = intent.getStringExtra("msg") ?: ""
                    appendLog(msg)
                }
                "onTokenError" -> {
                    val msg = intent.getStringExtra("msg") ?: ""
                    appendLog(msg)
                }
            }
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(this, "需要通知权限才能接收推送消息", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        NotificationHelper.createChannel(this)

        pushToken = TokenManager.getToken(this)

        requestNotificationPermission()
        setupViews()
        registerPushReceiver()
        handleNotificationClick(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleNotificationClick(it) }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun setupViews() {
        binding.btnGetToken.setOnClickListener { getToken() }
        binding.btnDeleteToken.setOnClickListener { deleteToken() }
        binding.btnShowToken.setOnClickListener { toggleTokenVisibility() }
        binding.btnCopyToken.setOnClickListener { copyToken() }
        binding.btnTurnOnPush.setOnClickListener { turnOnPush() }
        binding.btnTurnOffPush.setOnClickListener { turnOffPush() }
        binding.btnAutoInitOn.setOnClickListener { setAutoInitEnabled(true) }
        binding.btnAutoInitOff.setOnClickListener { setAutoInitEnabled(false) }
        binding.btnIsAutoInit.setOnClickListener { checkAutoInit() }
        binding.btnSendMessage.setOnClickListener { sendTestMessage() }
        binding.btnViewRecords.setOnClickListener { openMessageRecords() }
        binding.btnClearLog.setOnClickListener {
            binding.tvLog.text = ""
        }

        updateTokenDisplay()
        updatePushButtonState()
    }

    private fun registerPushReceiver() {
        val filter = IntentFilter(MyPushService.ACTION_PUSH_EVENT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(pushReceiver, filter, 2)
        } else {
            registerReceiver(pushReceiver, filter)
        }
    }

    private fun handleNotificationClick(intent: Intent) {
        if (intent.getBooleanExtra("from_notification", false)) {
            intent.removeExtra("from_notification")
            return
        }

        val cached = NotificationMessageCache.consume()
        if (cached != null) {
            val record = MessageRecord(
                title = cached.first,
                content = cached.second,
                data = cached.third,
                msgId = "",
                receiveTime = System.currentTimeMillis(),
                type = "通知消息"
            )
            MessageRecordManager.addRecord(this, record)
            appendLog("通知消息已保存到记录: ${cached.first.ifEmpty { "通知消息" }}")
        }
    }

    private fun getToken() {
        appendLog("开始获取Token...")
        lifecycleScope.launch {
            try {
                val token = withContext(Dispatchers.IO) {
                    HmsInstanceId.getInstance(this@MainActivity).getToken(APP_ID, "HCM")
                }
                if (!token.isNullOrEmpty()) {
                    pushToken = token
                    TokenManager.saveToken(this@MainActivity, token)
                    updateTokenDisplay()
                    appendLog("获取Token成功")
                } else {
                    appendLog("获取Token为空，请等待onNewToken回调")
                }
            } catch (e: ApiException) {
                Log.e(TAG, "getToken failed", e)
                appendLog("获取Token失败: ${e.message}")
            }
        }
    }

    private fun deleteToken() {
        AlertDialog.Builder(this)
            .setTitle("注销Token")
            .setMessage("确定要注销推送Token吗？注销后将无法接收推送消息。")
            .setPositiveButton("确定") { _, _ ->
                appendLog("开始注销Token...")
                lifecycleScope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            HmsInstanceId.getInstance(this@MainActivity).deleteToken(APP_ID, "HCM")
                        }
                        pushToken = ""
                        TokenManager.clearToken(this@MainActivity)
                        updateTokenDisplay()
                        appendLog("注销Token成功")
                    } catch (e: ApiException) {
                        Log.e(TAG, "deleteToken failed", e)
                        appendLog("注销Token失败: ${e.message}")
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun toggleTokenVisibility() {
        if (pushToken.isEmpty()) {
            Toast.makeText(this, "请先获取Token", Toast.LENGTH_SHORT).show()
            return
        }
        if (isTokenVisible) {
            isTokenVisible = false
            handler.removeCallbacks(tokenHideRunnable)
            updateTokenDisplay()
        } else {
            isTokenVisible = true
            updateTokenDisplay()
            handler.postDelayed(tokenHideRunnable, 10000)
        }
    }

    private fun copyToken() {
        if (pushToken.isEmpty()) {
            Toast.makeText(this, "请先获取Token", Toast.LENGTH_SHORT).show()
            return
        }
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Push Token", pushToken)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Token已复制到剪贴板", Toast.LENGTH_SHORT).show()
    }

    private fun maskToken(token: String): String {
        if (token.length <= 8) return "********"
        return token.take(4) + "****" + token.takeLast(4)
    }

    private fun updateTokenDisplay() {
        if (pushToken.isEmpty()) {
            binding.tvToken.text = "尚未获取Token"
            binding.btnShowToken.visibility = View.GONE
            binding.btnCopyToken.visibility = View.GONE
        } else {
            binding.tvToken.text = if (isTokenVisible) pushToken else maskToken(pushToken)
            binding.btnShowToken.visibility = View.VISIBLE
            binding.btnCopyToken.visibility = View.VISIBLE
            binding.btnShowToken.text = if (isTokenVisible) "隐藏TOKEN" else "显示TOKEN"
        }
    }

    private fun turnOnPush() {
        appendLog("开启通知栏消息...")
        lifecycleScope.launch {
            try {
                HmsMessaging.getInstance(this@MainActivity).turnOnPush().await()
                isPushEnabled = true
                appendLog("开启通知栏消息成功")
                updatePushButtonState()
            } catch (e: Exception) {
                appendLog("开启通知栏消息失败: ${e.message}")
            }
        }
    }

    private fun turnOffPush() {
        appendLog("关闭通知栏消息...")
        lifecycleScope.launch {
            try {
                HmsMessaging.getInstance(this@MainActivity).turnOffPush().await()
                isPushEnabled = false
                appendLog("关闭通知栏消息成功")
                updatePushButtonState()
            } catch (e: Exception) {
                appendLog("关闭通知栏消息失败: ${e.message}")
            }
        }
    }

    private var isPushEnabled = true

    private fun updatePushButtonState() {
        binding.btnTurnOnPush.isEnabled = !isPushEnabled
        binding.btnTurnOffPush.isEnabled = isPushEnabled
    }

    private fun setAutoInitEnabled(enable: Boolean) {
        HmsMessaging.getInstance(this).isAutoInitEnabled = enable
        appendLog("自动初始化已${if (enable) "开启" else "关闭"}")
        Toast.makeText(this, "自动初始化已${if (enable) "开启" else "关闭"}", Toast.LENGTH_SHORT).show()
    }

    private fun checkAutoInit() {
        val isEnabled = HmsMessaging.getInstance(this).isAutoInitEnabled
        appendLog("自动初始化状态: $isEnabled")
    }

    private fun sendTestMessage() {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = android.net.Uri.parse("pushscheme://evilcode.notification.hwpush/deeplink?test=true")
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val intentUri = intent.toUri(Intent.URI_INTENT_SCHEME)
        appendLog("Intent URI: $intentUri")
        appendLog("提示：请通过AppGallery Connect发送测试消息")
    }

    private fun openMessageRecords() {
        val intent = Intent(this, MessageRecordActivity::class.java)
        startActivity(intent)
    }

    private fun appendLog(log: String) {
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val timeStr = timeFormat.format(Date())
        binding.tvLog.append("[$timeStr] $log\n")
        binding.svLog.post { binding.svLog.fullScroll(View.FOCUS_DOWN) }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(tokenHideRunnable)
        try {
            unregisterReceiver(pushReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "unregisterReceiver failed", e)
        }
    }

    companion object {
        private const val TAG = "HaoWaiPush"
        private const val APP_ID = "117395995"
    }
}
