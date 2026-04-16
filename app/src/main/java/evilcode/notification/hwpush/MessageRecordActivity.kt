package evilcode.notification.hwpush

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import evilcode.notification.hwpush.databinding.ActivityMessageRecordBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessageRecordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMessageRecordBinding
    private lateinit var adapter: MessageRecordAdapter
    private var records: MutableList<MessageRecord> = mutableListOf()
    private var isSelectionMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMessageRecordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "消息记录"

        setupRecyclerView()
        handleNotificationIntent(intent)
        loadRecords()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleNotificationIntent(it) }
    }

    private fun handleNotificationIntent(intent: Intent) {
        val notifTitle = intent.getStringExtra("notif_title")
        val notifBody = intent.getStringExtra("notif_body")
        val notifData = intent.getStringExtra("notif_data")
        val notifType = intent.getStringExtra("notif_type") ?: "通知消息"
        if (!notifTitle.isNullOrEmpty() || !notifBody.isNullOrEmpty() || !notifData.isNullOrEmpty()) {
            if (notifType != "透传消息") {
                val record = MessageRecord(
                    title = notifTitle ?: "",
                    content = notifBody ?: "",
                    data = notifData ?: "",
                    msgId = "",
                    receiveTime = System.currentTimeMillis(),
                    type = notifType
                )
                MessageRecordManager.addRecord(this, record)
            }
            intent.removeExtra("notif_title")
            intent.removeExtra("notif_body")
            intent.removeExtra("notif_data")
            intent.removeExtra("notif_type")
        }
    }

    private fun setupRecyclerView() {
        adapter = MessageRecordAdapter(records, object : MessageRecordAdapter.OnItemClickListener {
            override fun onItemClick(record: MessageRecord) {
                if (isSelectionMode) {
                    toggleSelection(record)
                } else {
                    showRecordDetail(record)
                }
            }

            override fun onItemLongClick(record: MessageRecord) {
                if (!isSelectionMode) {
                    enterSelectionMode()
                }
                toggleSelection(record)
            }

            override fun onCopyClick(record: MessageRecord) {
                copyRecord(record)
            }

            override fun onDeleteClick(record: MessageRecord) {
                deleteRecord(record)
            }
        })

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.addItemDecoration(
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        )
    }

    private fun loadRecords() {
        records.clear()
        records.addAll(MessageRecordManager.getRecords(this))
        adapter.notifyDataSetChanged()
        updateEmptyView()
    }

    private fun updateEmptyView() {
        if (records.isEmpty()) {
            binding.tvEmpty.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
        } else {
            binding.tvEmpty.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
        }
    }

    private fun showRecordDetail(record: MessageRecord) {
        val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val timeStr = timeFormat.format(Date(record.receiveTime))

        val dialogView = layoutInflater.inflate(R.layout.dialog_message_detail, null)
        val tvDetail = dialogView.findViewById<android.widget.TextView>(R.id.tv_detail)
        val btnCopyTitle = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_copy_title)
        val btnCopyContent = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_copy_content)
        val btnCopyData = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_copy_data)
        val btnCopyMsgId = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_copy_msg_id)

        val detail = """
            类型: ${record.type}
            标题: ${record.title}
            内容: ${record.content}
            数据: ${record.data}
            消息ID: ${record.msgId}
            接收时间: $timeStr
        """.trimIndent()
        tvDetail.text = detail

        val dialog = AlertDialog.Builder(this)
            .setTitle("消息详情")
            .setView(dialogView)
            .setPositiveButton("关闭", null)
            .create()

        btnCopyTitle.setOnClickListener { copyText("标题", record.title) }
        btnCopyContent.setOnClickListener { copyText("内容", record.content) }
        btnCopyData.setOnClickListener { copyText("数据", record.data) }
        btnCopyMsgId.setOnClickListener { copyText("消息ID", record.msgId) }

        dialog.show()
    }

    private fun copyText(label: String, text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "${label}已复制", Toast.LENGTH_SHORT).show()
    }

    private fun copyRecord(record: MessageRecord) {
        val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val timeStr = timeFormat.format(Date(record.receiveTime))
        val text = "[${record.type}] ${record.title}\n${record.content}\n数据: ${record.data}\n时间: $timeStr"
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("消息记录", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
    }

    private fun deleteRecord(record: MessageRecord) {
        AlertDialog.Builder(this)
            .setTitle("删除记录")
            .setMessage("确定要删除这条消息记录吗？")
            .setPositiveButton("删除") { _, _ ->
                MessageRecordManager.deleteRecord(this, record.id)
                loadRecords()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun enterSelectionMode() {
        isSelectionMode = true
        adapter.setSelectionMode(true)
        binding.toolbar.title = "选择记录"
        invalidateOptionsMenu()
    }

    private fun exitSelectionMode() {
        isSelectionMode = false
        adapter.setSelectionMode(false)
        adapter.clearSelections()
        binding.toolbar.title = "消息记录"
        invalidateOptionsMenu()
    }

    private fun toggleSelection(record: MessageRecord) {
        adapter.toggleSelection(record)
        val selectedCount = adapter.getSelectedCount()
        if (selectedCount == 0) {
            exitSelectionMode()
        } else {
            binding.toolbar.title = "已选择 $selectedCount 项"
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_message_record, menu)
        menu.findItem(R.id.action_delete_selected)?.isVisible = isSelectionMode
        menu.findItem(R.id.action_clear_all)?.isVisible = !isSelectionMode && records.isNotEmpty()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                if (isSelectionMode) {
                    exitSelectionMode()
                } else {
                    finish()
                }
                true
            }
            R.id.action_delete_selected -> {
                val selectedIds = adapter.getSelectedIds()
                if (selectedIds.isNotEmpty()) {
                    AlertDialog.Builder(this)
                        .setTitle("删除记录")
                        .setMessage("确定要删除选中的 ${selectedIds.size} 条记录吗？")
                        .setPositiveButton("删除") { _, _ ->
                            MessageRecordManager.deleteRecords(this, selectedIds)
                            exitSelectionMode()
                            loadRecords()
                        }
                        .setNegativeButton("取消", null)
                        .show()
                }
                true
            }
            R.id.action_clear_all -> {
                AlertDialog.Builder(this)
                    .setTitle("清空记录")
                    .setMessage("确定要清空所有消息记录吗？")
                    .setPositiveButton("清空") { _, _ ->
                        MessageRecordManager.clearAll(this)
                        loadRecords()
                    }
                    .setNegativeButton("取消", null)
                    .show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        loadRecords()
    }

    override fun onBackPressed() {
        if (isSelectionMode) {
            exitSelectionMode()
        } else {
            super.onBackPressed()
        }
    }
}
