package com.example.mobvcviko1.data.api.model

data class UserRegistrationRequest(val name: String, val email: String, val password: String)
data class UserLoginRequest(val name: String, val password: String)
data class RefreshTokenRequest(val refresh: String)
data class GeofenceUpdateRequest(val lat: Double, val lon: Double, val radius: Double)
data class ChangePasswordRequest(val old_password: String, val new_password: String)