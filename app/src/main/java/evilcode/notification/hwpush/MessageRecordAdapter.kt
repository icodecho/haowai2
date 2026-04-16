package evilcode.notification.hwpush

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessageRecordAdapter(
    private val records: MutableList<MessageRecord>,
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<MessageRecordAdapter.ViewHolder>() {

    private var isSelectionMode = false
    private val selectedIds = mutableSetOf<Long>()

    interface OnItemClickListener {
        fun onItemClick(record: MessageRecord)
        fun onItemLongClick(record: MessageRecord)
        fun onCopyClick(record: MessageRecord)
        fun onDeleteClick(record: MessageRecord)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tv_record_title)
        val tvContent: TextView = view.findViewById(R.id.tv_record_content)
        val tvTime: TextView = view.findViewById(R.id.tv_record_time)
        val tvType: TextView = view.findViewById(R.id.tv_record_type)
        val btnCopy: ImageButton = view.findViewById(R.id.btn_copy)
        val btnDelete: ImageButton = view.findViewById(R.id.btn_delete)
        val checkBox: CheckBox = view.findViewById(R.id.checkbox)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message_record, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = records[position]
        val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val timeStr = timeFormat.format(Date(record.receiveTime))

        holder.tvTitle.text = record.title.ifEmpty { "无标题" }
        holder.tvContent.text = record.content.ifEmpty { record.data }
        holder.tvTime.text = timeStr
        holder.tvType.text = record.type

        holder.checkBox.visibility = if (isSelectionMode) View.VISIBLE else View.GONE
        holder.checkBox.isChecked = selectedIds.contains(record.id)
        holder.btnCopy.visibility = if (isSelectionMode) View.GONE else View.VISIBLE
        holder.btnDelete.visibility = if (isSelectionMode) View.GONE else View.VISIBLE

        holder.itemView.setOnClickListener {
            if (isSelectionMode) {
                listener.onItemClick(record)
            } else {
                listener.onItemClick(record)
            }
        }

        holder.itemView.setOnLongClickListener {
            listener.onItemLongClick(record)
            true
        }

        holder.checkBox.setOnClickListener {
            toggleSelection(record)
        }

        holder.btnCopy.setOnClickListener {
            listener.onCopyClick(record)
        }

        holder.btnDelete.setOnClickListener {
            listener.onDeleteClick(record)
        }
    }

    override fun getItemCount(): Int = records.size

    fun setSelectionMode(enabled: Boolean) {
        isSelectionMode = enabled
        if (!enabled) {
            selectedIds.clear()
        }
        notifyDataSetChanged()
    }

    fun toggleSelection(record: MessageRecord) {
        if (selectedIds.contains(record.id)) {
            selectedIds.remove(record.id)
        } else {
            selectedIds.add(record.id)
        }
        notifyDataSetChanged()
    }

    fun clearSelections() {
        selectedIds.clear()
        notifyDataSetChanged()
    }

    fun getSelectedCount(): Int = selectedIds.size

    fun getSelectedIds(): List<Long> = selectedIds.toList()
}
