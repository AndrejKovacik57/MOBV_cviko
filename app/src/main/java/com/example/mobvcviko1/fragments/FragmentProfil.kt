package com.example.mobvcviko1.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import com.example.mobvcviko1.widgets.bottomBar.BottomBar
import com.example.mobvcviko1.R
import com.example.mobvcviko1.data.PreferenceData
import com.example.mobvcviko1.data.DataRepository
import com.example.mobvcviko1.viewmodels.ProfileViewModel
import com.example.mobvcviko1.databinding.FragmentProfilBinding
import com.google.android.material.snackbar.Snackbar
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory.decodeResource
import android.graphics.Canvas
import android.location.Location
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.signature.ObjectKey
import com.example.mobvcviko1.broadcastReceivers.GeofenceBroadcastReceiver
import com.example.mobvcviko1.viewmodels.AuthViewModel
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotation
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createCircleAnnotationManager
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class FragmentProfil : Fragment(R.layout.fragment_profil) {
    private lateinit var viewModel: ProfileViewModel
    private lateinit var authViewModel: AuthViewModel
    private lateinit var binding: FragmentProfilBinding
    private var selectedPoint: CircleAnnotation? = null
    private var lastLocation: Point? = null
    private lateinit var annotationManager: CircleAnnotationManager
    private val PERMISSIONS_REQUIRED = if (Build.VERSION.SDK_INT >= 33) {
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                Manifest.permission.READ_MEDIA_IMAGES
            )
        }
        else {
            listOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }

    private val onIndicatorPositionChangedListener = OnIndicatorPositionChangedListener {
        Log.d("MapFragment", "poloha je $it")
        refreshLocation(it)
    }
    val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        // Callback is invoked after the user selects a media item or closes the
        // photo picker.
        if (uri != null) {
            Log.d("PhotoPicker", "Selected URI: $uri")
            loadPhoto(uri)
        } else {
            Log.d("PhotoPicker", "No media selected")
        }
    }

    val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {
        }

    fun hasPermissions(context: Context) = PERMISSIONS_REQUIRED.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity(), object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ProfileViewModel(DataRepository.getInstance(requireContext())) as T
            }
        })[ProfileViewModel::class.java]
        authViewModel = ViewModelProvider(requireActivity(), object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AuthViewModel(DataRepository.getInstance(requireContext())) as T
            }
        })[AuthViewModel::class.java]
        viewModel.sharingLocation.postValue(PreferenceData.getInstance().getSharing(requireContext()))

    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfilBinding.inflate(inflater, container, false)
//        binding.mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS) { style ->
//            val markerIconBitmap = rescaleBitmap( getBitmapFromDrawable(requireContext(), R.drawable.map_marker), 60, 60)
//
//            style.addImage("marker-icon", rescaleBitmap(markerIconBitmap, 60, 60))
//        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!hasPermissions(requireContext())) {
            viewModel.sharingLocation.postValue(false)
            requestPermissionsIfNeeded()
        }
        binding.apply {
            lifecycleOwner = viewLifecycleOwner
            model = viewModel
        }.also { bnd ->
            bnd.bottomBar.setActive(BottomBar.PROFILE)
            annotationManager = bnd.mapView.annotations.createCircleAnnotationManager()
            val hasPermission = hasPermissions(requireContext())
            onMapReady(hasPermission)
            bnd.loadProfileBtn.setOnClickListener {
                val user = PreferenceData.getInstance().getUser(requireContext())
                user?.let {
                    viewModel.loadUser(it.id)
                }
            }

            bnd.uploadPictureBtn.setOnClickListener {
                if (hasPermissions(requireContext())) {
                    openGallery()
                } else {
                    requestPermissionsIfNeeded()
                }
            }

            bnd.logoutBtn.setOnClickListener {
                authViewModel.logOutUser()
                PreferenceData.getInstance().clearData(requireContext())
                PreferenceData.getInstance().putUser(requireContext(), null)
                it.findNavController().navigate(R.id.action_profil_intro)
            }
            viewModel.profileResult.observe(viewLifecycleOwner) {
                if (it.isNotEmpty()) {
                    Snackbar.make(
                        bnd.loadProfileBtn,
                        it,
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
            viewModel.userResult.observe(viewLifecycleOwner) { user ->
                user?.photo?.let { photoUri ->
                    val uniqueKey = System.currentTimeMillis().toString() // Generate a unique key (e.g., timestamp)

                    // Assuming `uploaded_image` is the ID of your ImageView
                    Glide.with(this)
                        .load(photoUri) // Load the image URI
                        .signature(ObjectKey(uniqueKey)) // Cache-busting mechanism
                        .placeholder(R.drawable.profile) // Optional: Add a placeholder image
                        .into(bnd.uploadedImage) // Set the ImageView
                }
            }
            viewModel.uploadResult.observe(viewLifecycleOwner) { message ->
                Snackbar.make(binding.uploadPictureBtn, message, Snackbar.LENGTH_SHORT).show()
            }

            bnd.deletePictureBtn.setOnClickListener {
                viewModel.deleteProfilePicture()
            }

            bnd.locationSwitch.isChecked = PreferenceData.getInstance().getSharing(requireContext())
            bnd.changePasswordBtn.setOnClickListener{
                findNavController().navigate(R.id.action_profile_changePassword)
            }
            viewModel.sharingLocation.observe(viewLifecycleOwner) {
                it?.let {
                    if (it) {
                        if (!hasPermissions(requireContext())) {
                            viewModel.sharingLocation.postValue(false)
                            requestPermissionsIfNeeded()
                        } else {
                            PreferenceData.getInstance().putSharing(requireContext(), true)
                            turnOnSharing()

                        }
                    } else {
                        PreferenceData.getInstance().putSharing(requireContext(), false)
                        viewLifecycleOwner.lifecycleScope.launch {
                            viewModel.deleteLocation()
                        }

                        turnOffSharing()
                    }
                }
            }
            bnd.locationSwitch.setOnCheckedChangeListener { _, checked ->
                Log.d("ProfileFragment", "sharing je $checked")
                if (checked) {
                    turnOnSharing()
                } else {
                    viewLifecycleOwner.lifecycleScope.launch {
                        viewModel.deleteLocation()
                    }
                    turnOffSharing()
                }
            }

        }
    }
    private fun requestPermissionsIfNeeded() {
        if (!hasPermissions(requireContext())) {
            for (p in PERMISSIONS_REQUIRED) {
                requestPermissionLauncher.launch(p)
            }
        }
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
    fun openGallery() {
        if (hasPermissions(requireContext())) {
            Log.d("ProfileFragment", "pickPhotoFromGallery invoked")
            Toast.makeText(requireContext(), "Opening gallery...", Toast.LENGTH_SHORT).show()

            lifecycleScope.launch {
                pickMedia.launch(
                    PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.ImageOnly
                    )
                )
            }
        }else {
            requestPermissionsIfNeeded()
        }
    }

    fun inputStreamToFile(
        uri: Uri,
    ): Uri? {
        val resolver = requireContext().applicationContext.contentResolver
        resolver.openInputStream(uri).use { inputStream ->
            var orig = File(requireContext().filesDir, "photo_copied.jpg")
            if (orig.exists()) {
                orig.delete()
            }
            orig = File(requireContext().filesDir, "photo_copied.jpg")

            FileOutputStream(orig).use { fileOutputStream ->
                if (inputStream == null) {
                    Log.d("vybrane", "stream null")
                    return null
                }
                try {
                    Log.d("vybrane", "copied")
                    inputStream.copyTo(fileOutputStream)
                } catch (e: IOException) {
                    e.printStackTrace()
                    return null
                }
            }
            Log.d("vybrane", orig.absolutePath)
            return Uri.fromFile(orig)
        }

    }

    private fun loadPhoto(file: Uri) {
        inputStreamToFile(file)?.let {
            Log.d("vybrane", "vybrane je $it")
            viewModel.uploadPicture(it)
        }

    }


    @SuppressLint("MissingPermission")
    private fun turnOnSharing() {
        Log.d("ProfileFragment", "turnOnSharing")
        if (!hasPermissions(requireContext())) {
            viewModel.sharingLocation.postValue(false)
            binding.locationSwitch.isChecked = false
            for (p in PERMISSIONS_REQUIRED) {
                requestPermissionLauncher.launch(p)
            }
            return
        }
        PreferenceData.getInstance().putSharing(requireContext(), true)

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        fusedLocationClient.lastLocation.addOnSuccessListener(requireActivity()) {
            Log.d("ProfileFragment", "poloha posledna ${it ?: "-"}")
            if (it == null) {
                Log.e("ProfileFragment", "poloha neznama geofence nevytvoreny")
            } else {
                setupGeofence(it)
            }
        }

    }

    private fun turnOffSharing() {
        Log.d("ProfileFragment", "turnOffSharing")
        binding.locationSwitch.isChecked = false
        PreferenceData.getInstance().putSharing(requireContext(), false)
        removeGeofence()
    }

    @SuppressLint("MissingPermission")
    private fun setupGeofence(location: Location) {
        val geofencingClient = LocationServices.getGeofencingClient(requireActivity())
        val geofence = Geofence.Builder()
            .setRequestId("my-geofence")
            .setCircularRegion(location.latitude, location.longitude, 100f) // 100m polomer
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT)
            .build()
        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()
        val intent = Intent(requireActivity(), GeofenceBroadcastReceiver::class.java)
        var geofencePendingIntent: PendingIntent? = null
        geofencePendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getBroadcast(
                requireActivity(),
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
        } else {
            PendingIntent.getBroadcast(
                requireActivity(),
                0,
                intent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_MUTABLE
            )
        }
        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
            addOnSuccessListener {
                // Geofences boli úspešne pridané
                Log.d("ProfileFragment", "geofence vytvoreny")
                viewModel.updateGeofence(location.latitude, location.longitude, 100.0)
            }
            addOnFailureListener {
                // Chyba pri pridaní geofences
                it.printStackTrace()
                binding.locationSwitch.isChecked = false
                PreferenceData.getInstance().putSharing(requireContext(), false)
            }
        }
    }
    private fun removeGeofence() {
        Log.d("ProfileFragment", "geofence zruseny")
        val geofencingClient = LocationServices.getGeofencingClient(requireActivity())
        geofencingClient.removeGeofences(listOf("my-geofence"))
        viewModel.removeGeofence()

        annotationManager.deleteAll()
    }

    private fun rescaleBitmap(defaultBitmap:Bitmap, newWidth: Int, newHeight: Int):Bitmap{
        return Bitmap.createScaledBitmap(defaultBitmap, newWidth, newHeight, true)
    }
    private fun refreshLocation(point: Point) {
        if(!binding.locationSwitch.isChecked){
            return
        }
        Log.d("MapFragmentRefresh", "poloha je $point")

        binding.mapView.getMapboxMap()
            .setCamera(CameraOptions.Builder().center(point).zoom(15.0).build())
        binding.mapView.gestures.focalPoint =
            binding.mapView.getMapboxMap().pixelForCoordinate(point)
        lastLocation = point
        addMarker(point)
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
    private val onMoveListener = object : OnMoveListener {
        override fun onMoveBegin(detector: MoveGestureDetector) {
            onCameraTrackingDismissed()
        }
        override fun onMove(detector: MoveGestureDetector): Boolean {
            return false
        }
        override fun onMoveEnd(detector: MoveGestureDetector) {}
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
    private fun onMapReady(enabled: Boolean) {
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


    private fun onCameraTrackingDismissed() {
        binding.mapView.apply {
            location.removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
            gestures.removeOnMoveListener(onMoveListener)
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