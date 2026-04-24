package com.roboblocker.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.roboblocker.R
import com.roboblocker.data.db.CallAction
import com.roboblocker.data.db.CallLogEntry
import com.roboblocker.utils.toRelativeTime

class CallLogAdapter(
    private val onBlock: (CallLogEntry) -> Unit
) : ListAdapter<CallLogEntry, CallLogAdapter.VH>(DIFF) {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val ivAction: ImageView   = view.findViewById(R.id.iv_action)
        val tvNumber: TextView    = view.findViewById(R.id.tv_log_number)
        val tvReason: TextView    = view.findViewById(R.id.tv_log_reason)
        val tvTime: TextView      = view.findViewById(R.id.tv_log_time)
        val tvConfidence: TextView = view.findViewById(R.id.tv_confidence)
        val tvCategory: TextView  = view.findViewById(R.id.tv_category)
        val btnBlock: MaterialButton = view.findViewById(R.id.btn_block_now)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_call_log, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        val ctx  = holder.itemView.context

        holder.tvNumber.text = if (item.number.length > 4) item.number else "Número oculto"
        holder.tvReason.text = item.reason.take(60)
        holder.tvTime.text   = item.timestamp.toRelativeTime()

        when (item.action) {
            CallAction.BLOCKED -> {
                holder.ivAction.setImageResource(R.drawable.ic_block)
                holder.ivAction.setColorFilter(ContextCompat.getColor(ctx, R.color.red_accent))
                holder.btnBlock.visibility = View.GONE
            }
            CallAction.ALLOWED -> {
                holder.ivAction.setImageResource(R.drawable.ic_call_allowed)
                holder.ivAction.setColorFilter(ContextCompat.getColor(ctx, R.color.green_accent))
                holder.btnBlock.visibility = View.VISIBLE
                holder.btnBlock.setOnClickListener { onBlock(item) }
            }
            CallAction.WHITELISTED -> {
                holder.ivAction.setImageResource(R.drawable.ic_whitelist)
                holder.ivAction.setColorFilter(ContextCompat.getColor(ctx, R.color.blue_accent))
                holder.btnBlock.visibility = View.GONE
            }
        }

        if (item.aiConfidence > 0f) {
            holder.tvConfidence.text = "Confiança IA: ${(item.aiConfidence * 100).toInt()}%"
            holder.tvConfidence.visibility = View.VISIBLE
        } else {
            holder.tvConfidence.visibility = View.GONE
        }

        if (item.spamCategory.isNotEmpty() && item.spamCategory != "UNKNOWN") {
            holder.tvCategory.text = categoryLabel(item.spamCategory)
            holder.tvCategory.visibility = View.VISIBLE
        } else {
            holder.tvCategory.visibility = View.GONE
        }
    }

    private fun categoryLabel(cat: String) = when (cat) {
        "TELEMARKETING"   -> "📞 Telemarketing"
        "SCAM"            -> "⚠️ Fraude"
        "ROBOCALL"        -> "🤖 Robocall"
        "FREQUENCY_ABUSE" -> "⚡ Spam"
        "PATTERN_MATCH"   -> "🔍 Padrão"
        else              -> cat.lowercase().replaceFirstChar { it.uppercase() }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<CallLogEntry>() {
            override fun areItemsTheSame(a: CallLogEntry, b: CallLogEntry) = a.id == b.id
            override fun areContentsTheSame(a: CallLogEntry, b: CallLogEntry) = a == b
        }
    }
}
