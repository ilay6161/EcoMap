package com.example.ecomapapp.features.my_reports

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import com.example.ecomapapp.databinding.DialogConfirmDeleteBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ecomapapp.R
import com.example.ecomapapp.data.repository.reports.ReportsRepository
import com.example.ecomapapp.databinding.FragmentMyReportsBinding
import com.example.ecomapapp.model.Report
import com.google.android.material.snackbar.Snackbar

class MyReportsFragment : Fragment() {

    private var _binding: FragmentMyReportsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: MyReportsViewModel
    private lateinit var adapter: MyReportAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyReportsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[MyReportsViewModel::class.java]

        adapter = MyReportAdapter(
            onItemClick = { report -> navigateToDetail(report) },
            onEditClick = { report -> navigateToEdit(report) },
            onDeleteClick = { report -> confirmDelete(report) }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        binding.progressBar.visibility = View.VISIBLE

        viewModel.reports.observe(viewLifecycleOwner) { reports ->
            binding.progressBar.visibility = View.GONE
            binding.swipeRefresh.isRefreshing = false

            adapter.submitList(reports)

            binding.tvPendingCount.text = reports.count { it.status == Report.STATUS_PENDING }.toString()
            binding.tvVerifiedCount.text = reports.count { it.status == Report.STATUS_VERIFIED }.toString()
            binding.tvResolvedCount.text = reports.count { it.status == Report.STATUS_RESOLVED }.toString()
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refresh()
        }

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    private fun navigateToDetail(report: Report) {
        findNavController().navigate(
            MyReportsFragmentDirections.actionMyReportsFragmentToReportDetailFragment(report.id)
        )
    }

    private fun navigateToEdit(report: Report) {
        findNavController().navigate(
            MyReportsFragmentDirections.actionMyReportsFragmentToEditReportFragment(report.id)
        )
    }

    private fun confirmDelete(report: Report) {
        val dialogBinding = DialogConfirmDeleteBinding.inflate(LayoutInflater.from(requireContext()))
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialogBinding.btnCancel.setOnClickListener { dialog.dismiss() }
        dialogBinding.btnDelete.setOnClickListener {
            dialog.dismiss()
            binding.progressBar.visibility = View.VISIBLE
            ReportsRepository.shared.deleteReport(report) {
                if (_binding == null) return@deleteReport
                binding.progressBar.visibility = View.GONE
                Snackbar.make(binding.root, R.string.report_deleted, Snackbar.LENGTH_SHORT).show()
            }
        }
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
