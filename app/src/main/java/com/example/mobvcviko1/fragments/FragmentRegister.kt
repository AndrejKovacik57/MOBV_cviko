package com.example.mobvcviko1.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import com.example.mobvcviko1.R
import com.example.mobvcviko1.data.PreferenceData
import com.example.mobvcviko1.data.DataRepository

import com.example.mobvcviko1.databinding.FragmentRegisterBinding
import com.example.mobvcviko1.viewmodels.AuthViewModel
import com.google.android.material.snackbar.Snackbar

class FragmentRegister : Fragment(R.layout.fragment_register) {
    private lateinit var viewModel: AuthViewModel
    private lateinit var binding: FragmentRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(requireActivity(), object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AuthViewModel(DataRepository.getInstance(requireContext())) as T
            }
        })[AuthViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            lifecycleOwner = viewLifecycleOwner
            model = viewModel
        }.also { bnd ->
            viewModel.registrationResult.observe(viewLifecycleOwner) {
                if (it.isNotEmpty()) {
                    Snackbar.make(
                        bnd.submitButtonReg,
                        it,
                        Snackbar.LENGTH_SHORT
                    ).show()
                }

            }
        }
        viewModel.userResult.observe(viewLifecycleOwner) {
            it?.let { user ->
                PreferenceData.getInstance().putUser(requireContext(), user)
                requireView().findNavController().navigate(R.id.action_register_feed)
            } ?: PreferenceData.getInstance().putUser(requireContext(), null)
        }
    }
}