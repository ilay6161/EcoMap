package com.example.ecomapapp.features.feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ecomapapp.data.repository.reports.ReportsRepository
import com.example.ecomapapp.databinding.FragmentFeedBinding
import com.example.ecomapapp.databinding.ItemReportRowBinding
import com.example.ecomapapp.model.Report
import com.squareup.picasso.Picasso
import java.util.concurrent.TimeUnit

class FeedFragment : Fragment() {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!

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

        ReportsRepository.shared.getAllReports().observe(viewLifecycleOwner) { reports ->
            adapter.submitList(reports.sortedByDescending { it.createdAt })
            binding.swipeRefresh.isRefreshing = false
        }

        binding.swipeRefresh.setOnRefreshListener {
            ReportsRepository.shared.refreshReports()
        }
    }

    override fun onResume() {
        super.onResume()
        ReportsRepository.shared.refreshReports()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
            b.rowCategory.text = formatCategory(report.category)
            b.rowTime.text = formatTimeAgo(report.createdAt)

            report.photoUrl?.let { url ->
                Picasso.get().load(url).into(b.rowPhoto)
            }

            b.root.setOnClickListener { onClick(report) }
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

    private fun formatCategory(category: String) = when (category) {
        Report.CATEGORY_LITTER -> "Litter"
        Report.CATEGORY_WATER_LEAK -> "Water Leak"
        Report.CATEGORY_ILLEGAL_DUMPING -> "Illegal Dumping"
        Report.CATEGORY_INFRASTRUCTURE -> "Infrastructure"
        Report.CATEGORY_POLLUTION -> "Pollution"
        else -> "Other"
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
