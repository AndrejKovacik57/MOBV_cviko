package com.example.mobvcviko1.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.mobvcviko1.data.DataRepository
import kotlinx.coroutines.launch
import com.example.mobvcviko1.databinding.FragmentChangePasswordBinding

class FragmentChangePassword: Fragment() {

    private var _binding:FragmentChangePasswordBinding ? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout using view binding
        _binding = FragmentChangePasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set the button's click listener
        binding.changePasswordButton.setOnClickListener {
            handleChangePassword()
        }
    }

    private fun handleChangePassword() {
        val oldPassword = binding.oldPasswordEditText.editText?.text?.toString() ?:""
        val newPassword = binding.newPasswordEditText.editText?.text?.toString() ?:""

        if (oldPassword.isEmpty()) {
            Toast.makeText(context, "Old password cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPassword.isEmpty()) {
            Toast.makeText(context, "New password cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        // Call the change password API using a coroutine
        lifecycleScope.launch {
            try {
                val repository = DataRepository.getInstance(requireContext())
                val result = repository.apiChangePassword(oldPassword, newPassword)

                Toast.makeText(context, "Password changed successfully", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    context,
                    "Failed to change password: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
