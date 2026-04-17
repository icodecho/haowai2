package evilcode.notification.hwpush

import android.content.Context
import android.content.SharedPreferences

object TokenManager {

    private const val PREFS_NAME = "hwpush_token_prefs"
    private const val KEY_TOKEN = "push_token"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveToken(context: Context, token: String) {
        getPrefs(context).edit().putString(KEY_TOKEN, token).apply()
    }

    fun getToken(context: Context): String {
        return getPrefs(context).getString(KEY_TOKEN, "") ?: ""
    }

    fun clearToken(context: Context) {
        getPrefs(context).edit().remove(KEY_TOKEN).apply()
    }
}
