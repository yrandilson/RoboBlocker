package com.roboblocker.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.roboblocker.R
import com.roboblocker.data.db.BlockReason
import com.roboblocker.data.db.BlockedNumber
import com.roboblocker.utils.toFormattedDate

class BlockedNumberAdapter(
    private val onDelete: (BlockedNumber) -> Unit,
    private val onLongClick: (BlockedNumber) -> Unit
) : ListAdapter<BlockedNumber, BlockedNumberAdapter.VH>(DIFF) {

    private var fullList: List<BlockedNumber> = emptyList()

    override fun submitList(list: List<BlockedNumber>?) {
        fullList = list ?: emptyList()
        super.submitList(list)
    }

    fun filter(query: String) {
        val filtered = if (query.isBlank()) fullList
        else fullList.filter {
            it.number.contains(query) || it.label.contains(query, ignoreCase = true)
        }
        super.submitList(filtered)
    }

    fun getItem(position: Int): BlockedNumber = getItem(position)

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvNumber: TextView    = view.findViewById(R.id.tv_number)
        val tvLabel: TextView     = view.findViewById(R.id.tv_label)
        val tvReason: TextView    = view.findViewById(R.id.tv_reason)
        val tvDate: TextView      = view.findViewById(R.id.tv_date)
        val tvCount: TextView     = view.findViewById(R.id.tv_block_count)
        val tvPattern: TextView   = view.findViewById(R.id.tv_pattern_badge)
        val btnDelete: ImageButton = view.findViewById(R.id.btn_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_blocked_number, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.tvNumber.text  = item.number
        holder.tvLabel.text   = item.label.ifEmpty { "Sem rótulo" }
        holder.tvReason.text  = reasonLabel(item.reason)
        holder.tvDate.text    = item.addedAt.toFormattedDate()
        holder.tvCount.text   = "${item.timesBlocked}x bloqueado"
        holder.tvPattern.visibility = if (item.isPattern) View.VISIBLE else View.GONE
        holder.btnDelete.setOnClickListener { onDelete(item) }
        holder.itemView.setOnLongClickListener { onLongClick(item); true }
    }

    private fun reasonLabel(reason: BlockReason) = when (reason) {
        BlockReason.MANUAL           -> "Manual"
        BlockReason.AI_DETECTED      -> "🤖 IA"
        BlockReason.PATTERN_MATCH    -> "🔍 Padrão"
        BlockReason.FREQUENCY_ABUSE  -> "⚡ Frequência"
        BlockReason.IMPORTED         -> "📥 Importado"
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<BlockedNumber>() {
            override fun areItemsTheSame(a: BlockedNumber, b: BlockedNumber) = a.id == b.id
            override fun areContentsTheSame(a: BlockedNumber, b: BlockedNumber) = a == b
        }
    }
}
