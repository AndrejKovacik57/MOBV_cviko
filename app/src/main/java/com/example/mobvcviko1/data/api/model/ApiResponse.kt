package com.example.mobvcviko1.data.api.model

data class RegistrationResponse(val uid: String, val access: String, val refresh: String)
data class LoginResponse(val uid: String, val access: String, val refresh: String)
data class UserResponse(val id: String, val name: String, val photo: String)
data class RefreshTokenResponse(val uid: String, val access: String, val refresh: String)

data class GeofenceListResponse(
    val me: GeofenceMeResponse,
    val list: List<GeofenceUserResponse>
)

data class GeofenceUserResponse(
    val uid: String,
    val radius: Double,
    val updated: String,
    val name: String,
    val photo: String
)


data class GeofenceMeResponse(
    val uid: String,
    val lat: Double,
    val lon: Double,
    val radius: Double
)

data class GeofenceUpdateResponse(val success: String)
data class PhotoUploadResponse(
    val id: String,
    val name: String,
    val photo: String
)

data class PhotoDeleteResponse(
    val id: String,
    val name: String,
    val photo: String
)
data class ChangePasswordResponse(
    val status: String
)
