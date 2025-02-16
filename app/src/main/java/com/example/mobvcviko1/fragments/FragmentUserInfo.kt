package com.example.mobvcviko1.fragments

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.example.mobvcviko1.R
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotation
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createCircleAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager

class FragmentUserInfo : Fragment() {

    private lateinit var mapView: MapView
    private var circleAnnotationManager: CircleAnnotationManager? = null
    private var geofenceCircle: CircleAnnotation? = null
    private var pointAnnotationManager: PointAnnotationManager? = null
    private var markerIconBitmap: Bitmap? = null // Store the marker icon bitmap
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_user, container, false)

        // Retrieve arguments passed to the fragment
        val userId = arguments?.getString("userId")
        val userName = arguments?.getString("userName")
        val userPhoto = arguments?.getString("userPhoto")
        val userLat = arguments?.getFloat("userLat", 0.0F)
        val userLon = arguments?.getFloat("userLon", 0.0F)
        val userRadius = arguments?.getFloat("userRadius", 100.0F)
        val updated = arguments?.getString("updated")

        // Initialize UI components
        val profileImageView = view.findViewById<ImageView>(R.id.user_info_image)
        val nameTextView = view.findViewById<TextView>(R.id.username_profile)
        mapView = view.findViewById(R.id.map_view)

        // Load user photo using Glide
        if (!userPhoto.isNullOrEmpty()) {
            Glide.with(requireContext())
                .load(userPhoto)
                .placeholder(R.drawable.profile)
                .error(R.drawable.profile)
                .into(profileImageView)
        } else {
            profileImageView.setImageResource(R.drawable.profile)
        }

        // Set user details text
        nameTextView.text = "$userName"


        userLat?.let { lat ->
            userLon?.let { lon ->
                val point = Point.fromLngLat(lon.toDouble(), lat.toDouble())
                addGeofence(point, userRadius?.toDouble() ?: 100.0)
                addMarker(point) // Now the marker will use the user photo
            }
        }

        return view
    }

    private fun addGeofence(point: Point, radius: Double) {
        // Initialize annotation manager if not already initialized
        if (circleAnnotationManager == null) {
            circleAnnotationManager = mapView.annotations.createCircleAnnotationManager()
        }

        // Set camera to the user's location
        mapView.getMapboxMap().setCamera(
            CameraOptions.Builder()
                .center(point)
                .zoom(14.0)
                .build()
        )

        // Clear any existing annotations
        circleAnnotationManager?.deleteAll()

        // Add geofence circle annotation
        val circleOptions = CircleAnnotationOptions()
            .withPoint(point)
            .withCircleRadius(radius) // Circle radius
            .withCircleColor("#000") // Red color
            .withCircleOpacity(0.3) // Semi-transparent
            .withCircleStrokeWidth(2.0)
            .withCircleStrokeColor("#FFFFFF") // White border

        geofenceCircle = circleAnnotationManager?.create(circleOptions)
    }

    private fun addMarker(point: Point) {
        // Initialize PointAnnotationManager if not already initialized
        if (pointAnnotationManager == null) {
            pointAnnotationManager = mapView.annotations.createPointAnnotationManager()
        }

        // Clear any existing markers
        pointAnnotationManager?.deleteAll()

        // Add a marker at the user's location
        val pointAnnotationOptions = PointAnnotationOptions()
            .withPoint(point)
            .withIconImage("user-photo-icon") // Use the user photo icon
            .withIconSize(1.0) // Ensure the size remains consistent
        pointAnnotationManager?.create(pointAnnotationOptions)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        circleAnnotationManager?.let {
            it.deleteAll()
            it.onDestroy()
        }
        pointAnnotationManager?.let {
            it.deleteAll()
            it.onDestroy()
        }
        mapView.onDestroy()
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }
}