package evilcode.notification.hwpush

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class DeeplinkActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_deeplink)
        intent?.let { getIntentData(it) }
    }

    private fun getIntentData(intent: Intent) {
        val uri = intent.data
        if (uri != null) {
            val test = uri.getQueryParameter("test")
            Log.i(TAG, "Deeplink received: uri=$uri, test=$test")
            Toast.makeText(this, "深度链接: $uri", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        getIntentData(intent)
    }

    companion object {
        private const val TAG = "HaoWaiPush"
    }
}
