package com.example.mobvcviko1.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mobvcviko1.widgets.bottomBar.BottomBar
import com.example.mobvcviko1.R
import com.example.mobvcviko1.adapters.FeedAdapter
import com.example.mobvcviko1.data.DataRepository
import com.example.mobvcviko1.data.PreferenceData
import com.example.mobvcviko1.databinding.FragmentFeedBinding
import com.example.mobvcviko1.viewmodels.FeedViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton

class FragmentFeed : Fragment() {
    private lateinit var viewModel: FeedViewModel
    private lateinit var binding: FragmentFeedBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity(), object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return FeedViewModel(DataRepository.getInstance(requireContext())) as T
            }
        })[FeedViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFeedBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            lifecycleOwner = viewLifecycleOwner
            model = viewModel
        }.also { bnd ->

            bnd.bottomBar.setActive(BottomBar.FEED)
            bnd.feedRecyclerview.layoutManager = LinearLayoutManager(context)
            val feedAdapter = FeedAdapter{ user ->
                // Navigate to UserInfoFragment with the user details
                val bundle = Bundle().apply {
                    putString("userId", user.uid)
                    putString("userName", user.name)
                    putString("userPhoto", user.photo ?: "")
                    putFloat("userLat", user.lat.toFloat())
                    putFloat("userLon", user.lon.toFloat())
                    putFloat("userRadius", user.radius.toFloat())
                    putString("updated", user.updated)
                }
                findNavController().navigate(R.id.action_feedFragment_to_userInfoFragment, bundle)
            }
            bnd.feedRecyclerview.adapter = feedAdapter

            // Pozorovanie zmeny hodnoty
            viewModel.feed_items.observe(viewLifecycleOwner) { items ->
                Log.d("FeedFragment", "nove hodnoty $items")
                feedAdapter.updateItems(items ?: emptyList())
            }
            bnd.pullRefresh.setOnRefreshListener {
                viewModel.updateItems(requireContext())

            }
            viewModel.loading.observe(viewLifecycleOwner) {
                bnd.pullRefresh.isRefreshing = it
            }

        }
    }
}