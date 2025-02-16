package com.example.mobvcviko1.fragments

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.navigation.fragment.findNavController
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.example.mobvcviko1.widgets.bottomBar.BottomBar
import com.example.mobvcviko1.R
import com.example.mobvcviko1.data.DataRepository
import com.example.mobvcviko1.data.db.entities.UserEntity
import com.example.mobvcviko1.databinding.FragmentMapBinding
import com.example.mobvcviko1.viewmodels.FeedViewModel
import com.example.mobvcviko1.viewmodels.ProfileViewModel
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotation
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createCircleAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random


class FragmentMap: Fragment() {
    private lateinit var binding: FragmentMapBinding
    private var selectedPoint: CircleAnnotation? = null
    private var lastLocation: Point? = null
    private lateinit var repository: DataRepository
    private lateinit var viewModel: FeedViewModel
    private lateinit var profileViewModel: ProfileViewModel
    private lateinit var annotationManager: CircleAnnotationManager
    private var pointAnnotationManager: PointAnnotationManager? = null
    private var lastRefreshTime: Long = 0
    private val PERMISSIONS_REQUIRED = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                initLocationComponent()
                addLocationListeners()
                viewModel.updateItems(requireContext())
            }
        }

    fun hasPermissions(context: Context) = PERMISSIONS_REQUIRED.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = DataRepository.getInstance(requireContext())
        viewModel = ViewModelProvider(requireActivity(), object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return FeedViewModel(DataRepository.getInstance(requireContext())) as T
            }
        })[FeedViewModel::class.java]

        profileViewModel = ViewModelProvider(requireActivity(), object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ProfileViewModel(DataRepository.getInstance(requireContext())) as T
            }
        })[ProfileViewModel::class.java]

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMapBinding.inflate(inflater, container, false)
        pointAnnotationManager = binding.mapView.annotations.createPointAnnotationManager()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            lifecycleOwner = viewLifecycleOwner
        }.also { bnd ->
            bnd.bottomBar.setActive(BottomBar.MAP)
            annotationManager = bnd.mapView.annotations.createCircleAnnotationManager()
            val hasPermission = hasPermissions(requireContext())
            onMapReady(hasPermission)

            bnd.myLocation.setOnClickListener {
                if (!hasPermissions(requireContext())) {
                    requestPermissionLauncher.launch(
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                } else {
                    lastLocation?.let { refreshLocation(it) }
                    addLocationListeners()
                }
            }

            // Observe LiveData from ViewModel to add markers based on feed items
            viewModel.feed_items.observe(viewLifecycleOwner) { userEntities ->
                userEntities?.let { _ ->
                    if (lastLocation != null) {
                        lastLocation?.let { refreshLocation(it) }
                    }
                }
            }
            profileViewModel.sharingLocation.observe(viewLifecycleOwner) {it?.let {
                if(!it){

                    annotationManager.deleteAll()
                }
            }}
            pointAnnotationManager?.addClickListener { annotation ->
                // Check the annotation's ID or some property to determine which user was clicked
                val userUid = annotation.iconImage
                val user = viewModel.feed_items.value?.find { it.uid == userUid }

                // If user exists, navigate to the user info fragment
                user?.let {
                    val bundle = Bundle().apply {
                        putString("userId", it.uid)
                        putString("userName", it.name)
                        putString("userPhoto", it.photo ?: "")
                        putFloat("userLat", it.lat.toFloat())
                        putFloat("userLon", it.lon.toFloat())
                        putFloat("userRadius", it.radius.toFloat())
                    }
                    findNavController().navigate(R.id.action_mapFragment_to_userInfoFragment, bundle)
                }

                true // Indicate the event was handled
            }
        }
    }
    private fun onMapReady(enabled: Boolean) {
        viewModel.updateItems(requireContext())

        binding.mapView.getMapboxMap().setCamera(
            CameraOptions.Builder()
                .center(Point.fromLngLat(14.3539484, 49.8001304))
                .zoom(2.0)
                .build()
        )
        binding.mapView.getMapboxMap().loadStyleUri(
            Style.MAPBOX_STREETS
        ) {
            if (enabled) {
                initLocationComponent()
                addLocationListeners()
            }
        }
        binding.mapView.getMapboxMap().addOnMapClickListener {
            if (hasPermissions(requireContext())) {
                onCameraTrackingDismissed()
            }
            true
        }
    }
    private fun initLocationComponent() {
        val locationComponentPlugin = binding.mapView.location
        locationComponentPlugin.updateSettings {
            this.enabled = true
            this.pulsingEnabled = true
        }
    }
    private fun addLocationListeners() {
        binding.mapView.location.addOnIndicatorPositionChangedListener(
            onIndicatorPositionChangedListener
        )
        binding.mapView.gestures.addOnMoveListener(onMoveListener)
    }
    private val onIndicatorPositionChangedListener = OnIndicatorPositionChangedListener {
        Log.d("MapFragment", "poloha je $it")

        refreshLocation(it)
    }
    private fun refreshLocation(point: Point) {
        profileViewModel.sharingLocation.observe(viewLifecycleOwner) { itshare ->
            itshare?.let { share ->
                if (share) {
                    // Handle true case
                    binding.mapView.getMapboxMap()
                        .setCamera(CameraOptions.Builder().center(point).zoom(14.1).build())

                    binding.mapView.gestures.focalPoint =
                        binding.mapView.getMapboxMap().pixelForCoordinate(point)

                    lastLocation = point
                    addMarker(point)

                    val currentTime = System.currentTimeMillis()
                    // Check if 10 seconds have passed since the last refresh
                    if (currentTime - lastRefreshTime >= 5000) {
                        binding.mapView.getMapboxMap()
                            .setCamera(CameraOptions.Builder().center(point).zoom(14.1).build())

                        binding.mapView.gestures.focalPoint =
                            binding.mapView.getMapboxMap().pixelForCoordinate(point)

                        lastLocation = point
                        addMarker(point)

                        if (hasPermissions(requireContext())) {
                            binding.mapView.mapboxMap.loadStyleUri(Style.MAPBOX_STREETS) { style ->
                                viewModel.feed_items.observe(viewLifecycleOwner) { users ->
                                    displayUsersOnMap(users, point.latitude(), point.longitude(), style)
                                }
                            }
                        }
                        lastRefreshTime = currentTime
                    }
                }
            }
        }
    }
    private fun addMarker(point: Point) {
        if (selectedPoint == null) {
            annotationManager.deleteAll()
            val pointAnnotationOptions = CircleAnnotationOptions()
                .withPoint(point)
                .withCircleRadius(100.0)
                .withCircleOpacity(0.2)
                .withCircleColor("#000")
                .withCircleStrokeWidth(2.0)
                .withCircleStrokeColor("#ffffff")
            selectedPoint = annotationManager.create(pointAnnotationOptions)
        } else {
            selectedPoint?.let {
                it.point = point
                annotationManager.update(it)
            }
        }
    }
    private val onMoveListener = object : OnMoveListener {
        override fun onMoveBegin(detector: MoveGestureDetector) {
            onCameraTrackingDismissed()
        }
        override fun onMove(detector: MoveGestureDetector): Boolean {
            return false
        }
        override fun onMoveEnd(detector: MoveGestureDetector) {}
    }
    private fun onCameraTrackingDismissed() {
        binding.mapView.apply {
            location.removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
            gestures.removeOnMoveListener(onMoveListener)
        }
    }
    private fun rescaleBitmap(defaultBitmap:Bitmap, newWidth: Int, newHeight: Int):Bitmap{
        return Bitmap.createScaledBitmap(defaultBitmap, newWidth, newHeight, true)
    }
    fun randomPointWithinRadius(latitude: Double, longitude: Double, radiusMeters: Double): Point {
        // Convert radius from meters to degrees (approximately)
        val radiusInDegrees = radiusMeters / 111320

        // Generate a biased random distance and angle
        // Bias the distance towards the edge of the circle
        val distance = radiusInDegrees * (0.75 + Random.nextDouble() * 0.25)
        val angle = Random.nextDouble() * 2 * Math.PI

        // Calculate the coordinates
        val deltaLat = distance * kotlin.math.cos(angle)
        val deltaLon = distance * kotlin.math.sin(angle) / kotlin.math.cos(Math.toRadians(latitude))

        // New coordinates
        val newLat = latitude + deltaLat
        val newLon = longitude + deltaLon

        return Point.fromLngLat(newLon, newLat)
    }
    private fun addPointAnnotation(point: Point, userUid: String) {
        val pointAnnotationOptions = PointAnnotationOptions()
            .withPoint(point)
            .withIconImage(userUid) // Use the user photo or default icon
            .withIconSize(1.0) // Ensure the size remains consistent

        pointAnnotationManager?.create(pointAnnotationOptions)


    }
    fun getBitmapFromDrawable(context: Context, drawableResId: Int): Bitmap {
        val drawable = ContextCompat.getDrawable(context, drawableResId)
        drawable?.let {
            val width = it.intrinsicWidth
            val height = it.intrinsicHeight
            it.setBounds(0, 0, width, height)

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            it.draw(canvas)
            return bitmap
        }
        throw IllegalArgumentException("Drawable resource ID is invalid")
    }
    private fun generateRandomPointWithinRadius(baseLat: Double, baseLon: Double, radiusInMeters: Double): Point {
        // Convert radius from meters to degrees
        val radiusInDegrees = radiusInMeters / 111000.0

        // Generate random angle and distance
        val angle = Random.nextDouble(0.0, 2 * Math.PI)
        val distance = sqrt(Random.nextDouble(0.0, 1.0)) * radiusInDegrees

        // Calculate the offset
        val latitudeOffset = distance * cos(angle)
        val longitudeOffset = distance * sin(angle) / cos(Math.toRadians(baseLat))

        // Generate the random point
        val randomLatitude = baseLat + latitudeOffset
        val randomLongitude = baseLon + longitudeOffset

        return Point.fromLngLat(randomLongitude, randomLatitude)
    }
    fun displayUsersOnMap(users: List<UserEntity>?, baseLat: Double, baseLon: Double, style: Style) {
        pointAnnotationManager?.deleteAll()
        if (users != null) {
            users.forEach { user ->
                if(user.photo.isNotEmpty()) {
                    Glide.with(requireContext())
                        .asBitmap()
                        .load(user.photo)
                        .into(object : CustomTarget<Bitmap>() {
                            override fun onResourceReady(
                                resource: Bitmap,
                                transition: com.bumptech.glide.request.transition.Transition<in Bitmap>?
                            ) {
                                // Scale and add image to style
                                val markerIconBitmap =
                                    rescaleBitmap(resource, 64, 64) // Optionally scale
                                style.addImage(user.uid, markerIconBitmap)
                            }
                            override fun onLoadCleared(placeholder: Drawable?) {
                                // musi tu byt
                            }
                        })
                }else {
                    // Use a default icon for users without photos
                    val defaultBitmap = BitmapFactory.decodeResource(resources, R.drawable.marker)
                    val markerIconBitmap = rescaleBitmap(defaultBitmap, 60, 60)
                    style.addImage(user.uid, markerIconBitmap)
                }
                addPointAnnotation(generateRandomPointWithinRadius(baseLat, baseLon,100.0), user.uid)

            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.mapView.apply {
            location.removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
            gestures.removeOnMoveListener(onMoveListener)
        }
    }
}