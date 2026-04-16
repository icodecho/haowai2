package evilcode.notification.hwpush

data class MessageRecord(
    var id: Long = 0,
    val title: String,
    val content: String,
    val data: String,
    val msgId: String,
    val receiveTime: Long,
    val type: String
)
