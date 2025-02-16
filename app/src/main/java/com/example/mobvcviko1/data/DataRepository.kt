package com.example.mobvcviko1.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.mobvcviko1.data.api.model.UserLoginRequest
import com.example.mobvcviko1.data.api.model.UserRegistrationRequest
import com.example.mobvcviko1.data.model.User
import java.io.IOException
import com.example.mobvcviko1.data.api.ApiService
import com.example.mobvcviko1.data.api.PictureApiService
import com.example.mobvcviko1.data.api.model.ChangePasswordRequest
import com.example.mobvcviko1.data.api.model.GeofenceUpdateRequest
import com.example.mobvcviko1.data.db.AppRoomDatabase
import com.example.mobvcviko1.data.db.LocalCache
import com.example.mobvcviko1.data.db.entities.GeofenceEntity
import com.example.mobvcviko1.data.db.entities.UserEntity
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.mindrot.jbcrypt.BCrypt
import java.io.File

class DataRepository private constructor(
    private val service: ApiService,
    private val pictureService: PictureApiService,
    private val cache: LocalCache
) {
    companion object {
        const val TAG = "DataRepository"

        @Volatile
        private var INSTANCE: DataRepository? = null
        private val lock = Any()


        fun getInstance(context: Context): DataRepository =
            INSTANCE ?: synchronized(lock) {
                INSTANCE
                    ?: DataRepository(
                        ApiService.create(context),
                        PictureApiService.create(context),
                        LocalCache(AppRoomDatabase.getInstance(context).appDao())
                    ).also { INSTANCE = it }
            }
    }

    suspend fun apiRegisterUser(
        username: String,
        email: String,
        password: String
    ): Pair<String, User?> {
        if (username.isEmpty()) {
            return Pair("Username can not be empty", null)
        }
        if (email.isEmpty()) {
            return Pair("Email can not be empty", null)
        }
        if (password.isEmpty()) {
            return Pair("Password can not be empty", null)
        }
        try {
            val response = service.registerUser(UserRegistrationRequest(username, email, password))
            if (response.isSuccessful) {
                response.body()?.let { json_response ->
                    return Pair(
                        "",
                        User(
                            username,
                            email,
                            json_response.uid,
                            json_response.access,
                            json_response.refresh
                        )
                    )
                }
            }
            return Pair("Failed to create user", null)
        } catch (ex: IOException) {
            ex.printStackTrace()
            return Pair("Check internet connection. Failed to create user.", null)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return Pair("Fatal error. Failed to create user.", null)
    }

    suspend fun apiLoginUser(username: String, password: String): Pair<String, User?> {
        if (username.isEmpty()) {
            return Pair("Username can not be empty", null)
        }
        if (password.isEmpty()) {
            return Pair("Password can not be empty", null)
        }
        try {
            val response = service.loginUser(UserLoginRequest(username, password))
            if (response.isSuccessful) {
                response.body()?.let { jsonResponse ->
                    if (jsonResponse.uid == "-1") {
                        Log.d("Login", "wrong password or username ")
                        return Pair("Wrong password or username.", null)
                    }
                    return Pair(
                        "",
                        User(
                            username,
                            "",
                            jsonResponse.uid,
                            jsonResponse.access,
                            jsonResponse.refresh
                        )
                    )
                }
            }
            return Pair("Failed to login user", null)
        } catch (ex: IOException) {
            ex.printStackTrace()
            return Pair("Check internet connection. Failed to login user.", null)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return Pair("Fatal error. Failed to login user.", null)
    }
    suspend fun apiGetUser(
        uid: String
    ): Pair<String, User?> {
        try {
            val response = service.getUser(uid)

            if (response.isSuccessful) {
                response.body()?.let {
                    val photoUrl = if (it.photo.isNullOrEmpty()) "" else "https://upload.mcomputing.eu/${it.photo}"
                    return Pair("", User(it.name, "", it.id, "", "", photoUrl))
                }
            }

            return Pair("Failed to load user", null)
        } catch (ex: IOException) {
            ex.printStackTrace()
            return Pair("Check internet connection. Failed to load user.", null)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return Pair("Fatal error. Failed to load user.", null)
    }

    suspend fun apiListGeofence(context: Context?): String {
        try {

            if (!PreferenceData.getInstance().getSharing(context)) {
                cache.deleteUserItems() // Clear the user items from the cache
                return "Geofencing is turned off" // Return nothing
            }
            val response = service.listGeofence()

            if (response.isSuccessful) {
                response.body()?.let { resp ->
                    val users = resp.list.map {
                        // Directly check if photo is null or empty
                        val photoUrl = if (it.photo.isNullOrEmpty()) "" else "https://upload.mcomputing.eu/${it.photo}"
                        UserEntity(
                            it.uid,
                            it.name,
                            it.updated,
                            resp.me.lat,
                            resp.me.lon,
                            it.radius,
                            photoUrl
                        )
                    }

                    cache.insertUserItems(users)

                    return ""
                }
            }
            return "Failed to load users"
        } catch (ex: IOException) {
            ex.printStackTrace()
            return "Check internet connection. Failed to load users."
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return "Fatal error. Failed to load users."
    }
    fun getUsers() = cache.getUsers()

    suspend fun insertGeofence(item: GeofenceEntity) {
        cache.insertGeofence(item)
        try {
            val response =
                service.updateGeofence(GeofenceUpdateRequest(item.lat, item.lon, item.radius))
            if (response.isSuccessful) {
                response.body()?.let {
                    item.uploaded = true
                    cache.insertGeofence(item)
                    return
                }
            }
            return
        } catch (ex: IOException) {
            ex.printStackTrace()
            return
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
    suspend fun removeGeofence() {
        try {
            val response = service.deleteGeofence()
            if (response.isSuccessful) {
                response.body()?.let {
                    return
                }
            }
            return
        } catch (ex: IOException) {
            ex.printStackTrace()
            return
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
    suspend fun apiUploadPhoto(imageUri: Uri): String {
        return try {
            val file = File(imageUri.path ?: "")
            // Create a RequestBody for the file
            val requestBody = file.asRequestBody("image/jpeg".toMediaTypeOrNull())

            // Create the MultipartBody.Part
            val imagePart = MultipartBody.Part.createFormData("image", file.name, requestBody)

            // Call the API to upload the photo
            val response = pictureService.uploadPhoto(imagePart)

            if (response.isSuccessful) {
                // Check if the response's photo field is null or empty
                val photoUrl = if (response.body()?.photo.isNullOrEmpty()) "" else "https://upload.mcomputing.eu/${response.body()?.photo}"
                photoUrl
            } else {
                "Failed to upload photo: ${response.code()} ${response.message()}"
            }
        } catch (ex: IOException) {
            ex.printStackTrace()
            "Check your internet connection and try again."
        } catch (ex: Exception) {
            ex.printStackTrace()
            "An error occurred: ${ex.message}"
        }
    }

    suspend fun apiDeletePhoto(): String {
        return try {
            val response = pictureService.deletePhoto() // API call to delete the photo
            if (response.isSuccessful) {
                ""
            } else {
                "Could not delete picture"
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            "An error occurred: ${ex.message}"
        }
    }
    suspend fun apiGeofenceDeleteLocation(): String {
        try {
            val response = service.deleteGeofence()

            if (response.isSuccessful) {
                return ""
            }

            return "Failed to update location"
        } catch (ex: IOException) {
            ex.printStackTrace()
            return "Check internet connection. Failed to update location"
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return "Fatal error. Failed to update location"
    }
    suspend fun apiChangePassword(
        currentPassword: String,
        newPassword: String
    ): String {
        if (currentPassword.isEmpty()) {
            return "Current password cannot be empty"
        }
        if (newPassword.isEmpty()) {
            return "New password cannot be empty"
        }

        try {
            val response = service.changePassword(ChangePasswordRequest(currentPassword, newPassword))

            if (response.isSuccessful) {
                return response.body()?.status ?: "Password changed successfully"
            }

            return "Failed to change password: ${response.code()} ${response.message()}"
        } catch (ex: IOException) {
            ex.printStackTrace()
            return "Check your internet connection and try again."
        } catch (ex: Exception) {
            ex.printStackTrace()
            return "An error occurred: ${ex.message}"
        }
    }
}