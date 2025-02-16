package com.example.mobvcviko1.adapters


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment.Companion.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mobvcviko1.R
import com.example.mobvcviko1.Utils.ItemDiffCallback
import com.example.mobvcviko1.data.db.entities.UserEntity



class FeedAdapter(private val onItemClicked: (UserEntity) -> Unit) : RecyclerView.Adapter<FeedAdapter.FeedViewHolder>() {
    private var items: List<UserEntity> = listOf()

    // ViewHolder poskytuje odkazy na zobrazenia v každej položke
    class FeedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImageView: ImageView = itemView.findViewById(R.id.item_image)
        val itemTextView: TextView = itemView.findViewById(R.id.item_text)
    }

    // Táto metóda vytvára nový ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.feed_item, parent, false)
        return FeedViewHolder(view)
    }

    // Táto metóda prepojí dáta s ViewHolderom
    override fun onBindViewHolder(holder: FeedViewHolder, position: Int) {
        val user = items[position]

        val userDetails = "ID: ${user.uid} - ${user.name} (Lat: ${user.lat}, Lon: ${user.lon})"
        holder.itemTextView.text = userDetails

        if(user.photo.isNotEmpty()){
            Glide.with(holder.itemView.context)
                .load(user.photo)
                .placeholder(R.drawable.profile)
                .error(R.drawable.profile)
                .into(holder.profileImageView)
        } else {
            Glide.with(holder.itemView.context)
                .load(R.drawable.profile)
                .into(holder.profileImageView)
        }
        // Set click listener
        holder.itemView.setOnClickListener {
            onItemClicked(user)
        }
    }

    // Vracia počet položiek v zozname
    override fun getItemCount() = items.size

    fun updateItems(newItems: List<UserEntity>) {
        val diffCallback = ItemDiffCallback(items, newItems)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        items = newItems
        diffResult.dispatchUpdatesTo(this)
    }
}

