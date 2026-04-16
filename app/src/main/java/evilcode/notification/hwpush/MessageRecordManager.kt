package evilcode.notification.hwpush

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object MessageRecordManager {

    private const val PREFS_NAME = "hwpush_message_records"
    private const val KEY_RECORDS = "message_records"
    private const val KEY_COUNTER = "record_counter"

    private val gson = Gson()

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun addRecord(context: Context, record: MessageRecord) {
        val records = getRecords(context).toMutableList()
        record.id = getAndIncrementId(context)
        records.add(0, record)
        saveRecords(context, records)
    }

    fun getRecords(context: Context): List<MessageRecord> {
        val json = getPrefs(context).getString(KEY_RECORDS, null) ?: return emptyList()
        val type = object : TypeToken<List<MessageRecord>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun deleteRecord(context: Context, id: Long) {
        val records = getRecords(context).toMutableList()
        records.removeAll { it.id == id }
        saveRecords(context, records)
    }

    fun deleteRecords(context: Context, ids: List<Long>) {
        val records = getRecords(context).toMutableList()
        records.removeAll { it.id in ids }
        saveRecords(context, records)
    }

    fun clearAll(context: Context) {
        saveRecords(context, emptyList())
    }

    private fun saveRecords(context: Context, records: List<MessageRecord>) {
        val json = gson.toJson(records)
        getPrefs(context).edit().putString(KEY_RECORDS, json).apply()
    }

    private fun getAndIncrementId(context: Context): Long {
        val prefs = getPrefs(context)
        val id = prefs.getLong(KEY_COUNTER, 0) + 1
        prefs.edit().putLong(KEY_COUNTER, id).apply()
        return id
    }
}
