package com.example.ecomapapp.features.feed

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import com.example.ecomapapp.R
import com.example.ecomapapp.databinding.FragmentFeedBinding
import com.example.ecomapapp.databinding.ItemReportRowBinding
import com.example.ecomapapp.model.Report
import com.squareup.picasso.Picasso
import java.util.concurrent.TimeUnit

class FeedFragment : Fragment() {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FeedViewModel by viewModels()
    private var isFirstLoad = true
    private val chipViews = LinkedHashMap<String?, TextView>()

    private val adapter = ReportAdapter { report ->
        val action = FeedFragmentDirections.actionFeedFragmentToReportDetailFragment(report.id)
        findNavController().navigate(action)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.reportsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.reportsRecyclerView.adapter = adapter

        isFirstLoad = true
        binding.progressBar.visibility = View.VISIBLE

        viewModel.filteredReports.observe(viewLifecycleOwner) { reports ->
            adapter.submitList(reports)
            binding.swipeRefresh.isRefreshing = false
            if (isFirstLoad) {
                binding.progressBar.visibility = View.GONE
                isFirstLoad = false
            }
        }

        buildFilterChips()

        viewModel.selectedCategory.observe(viewLifecycleOwner) { selected ->
            updateChipStyles(selected)
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refresh()
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                viewModel.searchQuery.value = s?.toString() ?: ""
            }
        })
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun buildFilterChips() {
        val categories = listOf(
            null to "All",
            Report.CATEGORY_LITTER to "Litter",
            Report.CATEGORY_WATER_LEAK to "Water Leak",
            Report.CATEGORY_ILLEGAL_DUMPING to "Dumping",
            Report.CATEGORY_INFRASTRUCTURE to "Infrastructure",
            Report.CATEGORY_POLLUTION to "Pollution",
            Report.CATEGORY_OTHER to "Other"
        )

        chipViews.clear()
        binding.chipContainer.removeAllViews()

        val chipHeight = (32 * resources.displayMetrics.density).toInt()
        val chipPadH = (14 * resources.displayMetrics.density).toInt()
        val chipMarginEnd = (8 * resources.displayMetrics.density).toInt()

        categories.forEach { (category, label) ->
            val chip = TextView(requireContext()).apply {
                text = label
                textSize = 13f
                gravity = Gravity.CENTER
                setPadding(chipPadH, 0, chipPadH, 0)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, chipHeight
                ).also { it.marginEnd = chipMarginEnd }
                setOnClickListener { viewModel.selectedCategory.value = category }
            }
            chipViews[category] = chip
            binding.chipContainer.addView(chip)
        }

        updateChipStyles(viewModel.selectedCategory.value)
    }

    private fun updateChipStyles(selected: String?) {
        chipViews.forEach { (category, chip) ->
            val isSelected = category == selected
            chip.setBackgroundResource(
                if (isSelected) R.drawable.bg_chip_selected else R.drawable.bg_chip_unselected
            )
            chip.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (isSelected) R.color.white else R.color.eco_text_hint
                )
            )
        }
    }
}

class ReportAdapter(
    private val onClick: (Report) -> Unit
) : ListAdapter<Report, ReportAdapter.ViewHolder>(DIFF_CALLBACK) {

    inner class ViewHolder(private val b: ItemReportRowBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(report: Report) {
            b.rowTitle.text = report.title
            b.rowLocation.text = report.locationName
            b.rowTime.text = formatTimeAgo(report.createdAt)
            b.root.setOnClickListener { onClick(report) }

            bindCategoryBadge(report)
            bindStatusBadge(report)
            bindThumbnail(report)
        }

        private fun bindCategoryBadge(report: Report) {
            val (iconRes, circleColorRes, categoryLabel) = when (report.category) {
                Report.CATEGORY_LITTER ->
                    Triple(R.drawable.ic_category_litter, R.color.eco_pin_litter, "Litter")
                Report.CATEGORY_WATER_LEAK ->
                    Triple(R.drawable.ic_category_water_leak, R.color.eco_pin_leak, "Leak")
                Report.CATEGORY_ILLEGAL_DUMPING ->
                    Triple(R.drawable.ic_category_dumping, R.color.eco_pin_dumping, "Dumping")
                Report.CATEGORY_INFRASTRUCTURE ->
                    Triple(R.drawable.ic_category_infrastructure, R.color.eco_dark_green, "Infrastructure")
                Report.CATEGORY_POLLUTION ->
                    Triple(R.drawable.ic_category_pollution, R.color.eco_medium_green, "Pollution")
                else ->
                    Triple(R.drawable.ic_category_other, R.color.eco_text_hint, "Other")
            }
            b.rowCategoryIcon.setImageResource(iconRes)
            b.rowCategoryIcon.imageTintList =
                ColorStateList.valueOf(ContextCompat.getColor(b.root.context, android.R.color.white))
            ViewCompat.setBackgroundTintList(
                b.rowCategoryIcon,
                ColorStateList.valueOf(ContextCompat.getColor(b.root.context, circleColorRes))
            )
            b.rowCategory.text = categoryLabel
        }

        private fun bindStatusBadge(report: Report) {
            val (bgColorRes, statusText) = when (report.status) {
                Report.STATUS_VERIFIED -> R.color.eco_status_verified to "Verified"
                Report.STATUS_RESOLVED -> R.color.eco_status_resolved to "Resolved"
                else -> R.color.eco_status_pending to "Pending"
            }
            b.rowStatus.text = statusText
            ViewCompat.setBackgroundTintList(
                b.rowStatus,
                ColorStateList.valueOf(ContextCompat.getColor(b.root.context, bgColorRes))
            )
        }

        private fun bindThumbnail(report: Report) {
            val (iconRes, iconColorRes) = when (report.category) {
                Report.CATEGORY_LITTER ->
                    R.drawable.ic_category_litter to R.color.eco_pin_litter
                Report.CATEGORY_WATER_LEAK ->
                    R.drawable.ic_category_water_leak to R.color.eco_pin_leak
                Report.CATEGORY_ILLEGAL_DUMPING ->
                    R.drawable.ic_category_dumping to R.color.eco_pin_dumping
                Report.CATEGORY_INFRASTRUCTURE ->
                    R.drawable.ic_category_infrastructure to R.color.eco_dark_green
                Report.CATEGORY_POLLUTION ->
                    R.drawable.ic_category_pollution to R.color.eco_medium_green
                else ->
                    R.drawable.ic_category_other to R.color.eco_text_hint
            }

            if (!report.photoUrl.isNullOrBlank()) {
                b.rowPhoto.imageTintList = null
                b.rowPhoto.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                b.rowPhoto.setPadding(0, 0, 0, 0)
                Picasso.get()
                    .load(report.photoUrl)
                    .placeholder(iconRes)
                    .into(b.rowPhoto)
            } else {
                val pad = (b.root.context.resources.displayMetrics.density * 12).toInt()
                b.rowPhoto.setPadding(pad, pad, pad, pad)
                b.rowPhoto.scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE
                b.rowPhoto.setImageResource(iconRes)
                b.rowPhoto.imageTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(b.root.context, iconColorRes))
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemReportRowBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private fun formatTimeAgo(createdAt: Long?): String {
        if (createdAt == null) return ""
        val diff = System.currentTimeMillis() - createdAt
        return when {
            diff < TimeUnit.MINUTES.toMillis(1) -> "just now"
            diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)}m ago"
            diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)}h ago"
            else -> "${TimeUnit.MILLISECONDS.toDays(diff)}d ago"
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Report>() {
            override fun areItemsTheSame(old: Report, new: Report) = old.id == new.id
            override fun areContentsTheSame(old: Report, new: Report) = old == new
        }
    }
}
