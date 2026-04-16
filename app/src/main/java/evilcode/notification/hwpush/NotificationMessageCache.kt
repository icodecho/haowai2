package evilcode.notification.hwpush

object NotificationMessageCache {
    private var cached: Triple<String, String, String>? = null

    fun put(title: String, body: String, data: String) {
        cached = Triple(title, body, data)
    }

    fun consume(): Triple<String, String, String>? {
        val value = cached
        cached = null
        return value
    }
}
