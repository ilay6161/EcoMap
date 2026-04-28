package com.example.ecomapapp.features.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.ecomapapp.LoginActivity
import com.example.ecomapapp.R
import com.example.ecomapapp.data.models.FirebaseAuthModel
import com.example.ecomapapp.data.models.FirebaseModel
import com.example.ecomapapp.databinding.FragmentProfileBinding
import com.squareup.picasso.Picasso
import com.google.firebase.auth.FirebaseAuth

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val authModel = FirebaseAuthModel()
    private val firebaseModel = FirebaseModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnEdit.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_editProfileFragment)
        }

        binding.cardMyReports.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_myReportsFragment)
        }

        binding.btnSignOut.setOnClickListener {
            authModel.signOut()
            startActivity(Intent(requireContext(), LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
    }

    override fun onResume() {
        super.onResume()
        loadUserData()
    }

    private fun loadUserData() {
        val firebaseUser = FirebaseAuth.getInstance().currentUser ?: return

        binding.tvName.text = firebaseUser.displayName?.takeIf { it.isNotBlank() } ?: "—"
        binding.tvEmail.text = firebaseUser.email ?: "—"

        firebaseUser.photoUrl?.let { uri ->
            loadPhoto(uri.toString())
        }

        // Also fetch fresh data from Firestore (phone, updated photo)
        firebaseModel.getUser(firebaseUser.uid) { user ->
            if (_binding == null) return@getUser
            user?.let {
                if (it.name.isNotBlank()) binding.tvName.text = it.name
                if (!it.photoUrl.isNullOrEmpty()) {
                    loadPhoto(it.photoUrl)
                }
            }
        }
    }

    private fun loadPhoto(url: String) {
        binding.ivProfilePhoto.imageTintList = null
        binding.ivProfilePhoto.setPadding(0, 0, 0, 0)
        Picasso.get()
            .load(url)
            .placeholder(android.R.drawable.ic_menu_my_calendar)
            .into(binding.ivProfilePhoto)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
