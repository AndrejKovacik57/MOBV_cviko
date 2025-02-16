package com.example.mobvcviko1.viewmodels

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobvcviko1.data.DataRepository
import com.example.mobvcviko1.data.db.entities.GeofenceEntity
import com.example.mobvcviko1.data.model.User
import kotlinx.coroutines.launch

    class ProfileViewModel(private val dataRepository: DataRepository) : ViewModel() {
    private val _profileResult = MutableLiveData<String>()
    val profileResult: LiveData<String> get() = _profileResult

    private val _userResult = MutableLiveData<User?>()

    private val _uploadResult = MutableLiveData<String>()
    val uploadResult: LiveData<String> get() = _uploadResult

    private val _deleteResult = MutableLiveData<String>()
    val deleteResult: LiveData<String> get() = _uploadResult

    val userResult: LiveData<User?> get() = _userResult
    val sharingLocation = MutableLiveData<Boolean?>(null)

    fun loadUser(uid: String) {
        viewModelScope.launch {
            val result = dataRepository.apiGetUser(uid)
            _profileResult.postValue(result.first ?: "")
            _userResult.postValue(result.second)
        }
    }
    fun updateGeofence(lat: Double, lon: Double, radius: Double) {
        viewModelScope.launch {
            dataRepository.insertGeofence(GeofenceEntity(lat, lon, radius))
        }
    }
    fun removeGeofence() {
        viewModelScope.launch {
            dataRepository.removeGeofence()
        }
    }
    suspend fun deleteLocation() {

        val result = dataRepository.apiGeofenceDeleteLocation()

    }
    fun uploadPicture(imageUri: Uri) {
        viewModelScope.launch {
            // Call the repository method to upload the photo
            val result = dataRepository.apiUploadPhoto(imageUri)

            _userResult.value?.let { user ->
                val updatedUser = user.copy(photo = result) // Update the photo URI
                _userResult.postValue(updatedUser)
            }
            _uploadResult.postValue("Photo uploaded successfully")
        }
    }
    fun deleteProfilePicture() {
        viewModelScope.launch {
            val result = dataRepository.apiDeletePhoto() // Assume this deletes the photo on the server
            if (result.isEmpty()) { // If deletion was successful
                _userResult.value?.let { user ->
                    val updatedUser = user.copy(photo = result) // Set photo to null
                    _userResult.postValue(updatedUser)
                }
                _deleteResult.postValue("Photo deleted successfully")
            } else {
                _deleteResult.postValue("Failed to delete photo")
            }
        }
    }

}