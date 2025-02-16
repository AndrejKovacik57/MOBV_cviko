package com.example.mobvcviko1.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.example.mobvcviko1.R
import com.example.mobvcviko1.data.PreferenceData
import com.example.mobvcviko1.databinding.FragmentIntroBinding
import com.google.android.material.button.MaterialButton


class FragmentIntro : Fragment(R.layout.fragment_intro) {
    private var binding: FragmentIntroBinding? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentIntroBinding.bind(view).apply {
            lifecycleOwner = viewLifecycleOwner
        }.also { bnd ->
            bnd.login.apply {
                setOnClickListener {
                    it.findNavController().navigate(R.id.action_intro_login)
                }
            }
            bnd.register.apply {
                setOnClickListener {
                    it.findNavController().navigate(R.id.action_intro_signup)
                }
            }
            val user = PreferenceData.getInstance().getUser(requireContext())
            if (user != null) {
                requireView().findNavController().navigate(R.id.action_intro_feed)
            }
        }
    }
    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

}