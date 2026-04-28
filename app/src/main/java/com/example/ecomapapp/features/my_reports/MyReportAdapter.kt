package com.example.ecomapapp.features.my_reports

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ecomapapp.R
import com.example.ecomapapp.databinding.ItemMyReportRowBinding
import com.example.ecomapapp.model.Report
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MyReportAdapter(
    private val onEditClick: (Report) -> Unit,
    private val onDeleteClick: (Report) -> Unit
) : ListAdapter<Report, MyReportAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val binding: ItemMyReportRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(report: Report) {
            binding.tvTitle.text = report.title
            binding.tvLocation.text = report.locationName.ifEmpty { "—" }
            binding.tvTime.text = formatTimeAgo(report.createdAt)

            val (label, color) = when (report.status) {
                Report.STATUS_VERIFIED -> Pair(
                    "Verified",
                    ContextCompat.getColor(binding.root.context, R.color.eco_status_verified)
                )
                Report.STATUS_RESOLVED -> Pair(
                    "Resolved",
                    ContextCompat.getColor(binding.root.context, R.color.eco_status_resolved)
                )
                else -> Pair(
                    "Pending",
                    ContextCompat.getColor(binding.root.context, R.color.eco_status_pending)
                )
            }
            binding.tvStatus.text = label
            binding.tvStatus.backgroundTintList = ColorStateList.valueOf(color)

            if (!report.photoUrl.isNullOrEmpty()) {
                binding.ivThumbnail.setImageDrawable(null)
                Picasso.get()
                    .load(report.photoUrl)
                    .placeholder(R.drawable.ic_category_other)
                    .error(R.drawable.ic_category_other)
                    .fit()
                    .centerCrop()
                    .into(binding.ivThumbnail)
            } else {
                binding.ivThumbnail.setImageResource(R.drawable.ic_category_other)
            }

            binding.btnEdit.setOnClickListener { onEditClick(report) }
            binding.btnDelete.setOnClickListener { onDeleteClick(report) }
        }

        private fun formatTimeAgo(timestamp: Long?): String {
            if (timestamp == null) return ""
            val diff = System.currentTimeMillis() - timestamp
            return when {
                diff < 60_000L -> "Just now"
                diff < 3_600_000L -> "${diff / 60_000}m ago"
                diff < 86_400_000L -> "${diff / 3_600_000}h ago"
                diff < 2_592_000_000L -> "${diff / 86_400_000}d ago"
                else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMyReportRowBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<Report>() {
        override fun areItemsTheSame(oldItem: Report, newItem: Report) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Report, newItem: Report) = oldItem == newItem
    }
}
