package com.example.authentication.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.authentication.MedTrackApp
import com.example.authentication.databinding.FragmentHomeBinding
import com.example.authentication.reminders.ReminderScheduler
import com.example.authentication.session.UserSessionManager
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: HomeAdapter

    private val viewModel: HomeViewModel by viewModels {
        val app = requireActivity().application as MedTrackApp
        val patientId = UserSessionManager.getActivePatient(requireContext()) ?: UserSessionManager.getUid(requireContext()) ?: ""
        HomeViewModel.Factory(app.medicationRepository, app.intakeLogRepository, patientId)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUidHeader()

        adapter = HomeAdapter(
            emptyList(),
            onTaken = { item -> viewModel.markTaken(item) },
            onDelete = { item ->
                viewLifecycleOwner.lifecycleScope.launch {
                    viewModel.deleteMedication(
                        item,
                        ReminderScheduler(requireContext().applicationContext)
                    )
                }
            }
        )
        binding.recyclerMedications.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerMedications.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.medications.collect { items ->
                    adapter.submitList(items)
                    binding.emptyState.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun setupUidHeader() {
        val myUid = UserSessionManager.getUid(requireContext())
        val activePatientUid = UserSessionManager.getActivePatient(requireContext())
        val role = UserSessionManager.getRole(requireContext())

        // Only show "My ID" if the user is a patient viewing their own data
        if (role == "patient" && (activePatientUid == null || activePatientUid == myUid)) {
            binding.uidContainer.visibility = View.VISIBLE
            binding.textUserUid.text = myUid
            binding.btnCopyUid.setOnClickListener {
                copyToClipboard(myUid ?: "")
            }
        } else {
            binding.uidContainer.visibility = View.GONE
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Patient UID", text)
        clipboard.setPrimaryClip(clip)
        android.widget.Toast.makeText(requireContext(), "ID copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = HomeFragment()
    }
}
