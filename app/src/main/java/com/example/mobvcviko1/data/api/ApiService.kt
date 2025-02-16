package com.example.mobvcviko1.data.api

import android.content.Context
import com.example.mobvcviko1.data.api.model.LoginResponse
import com.example.mobvcviko1.data.api.model.RefreshTokenRequest
import com.example.mobvcviko1.data.api.model.RefreshTokenResponse
import com.example.mobvcviko1.data.api.model.RegistrationResponse
import com.example.mobvcviko1.data.api.model.UserLoginRequest
import com.example.mobvcviko1.data.api.model.UserRegistrationRequest
import com.example.mobvcviko1.data.api.model.UserResponse
import com.example.mobvcviko1.data.api.helper.AuthInterceptor
import com.example.mobvcviko1.data.api.helper.TokenAuthenticator
import com.example.mobvcviko1.data.api.model.ChangePasswordRequest
import com.example.mobvcviko1.data.api.model.ChangePasswordResponse
import com.example.mobvcviko1.data.api.model.GeofenceListResponse
import com.example.mobvcviko1.data.api.model.GeofenceUpdateRequest
import com.example.mobvcviko1.data.api.model.GeofenceUpdateResponse
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
//    @Headers("x-apikey: $API_KEY")
    @POST("user/create.php")
    suspend fun registerUser(@Body userInfo: UserRegistrationRequest): Response<RegistrationResponse>

//    @Headers("x-apikey: $API_KEY")
    @POST("user/login.php")
    suspend fun loginUser(@Body userInfo: UserLoginRequest): Response<LoginResponse>

    @GET("user/get.php")
    suspend fun getUser(
//        @HeaderMap header: Map<String, String>,
        @Query("id") id: String
    ): Response<UserResponse>

    @POST("user/refresh.php")
    suspend fun refreshToken(
//        @HeaderMap header: Map<String, String>,
        @Body refreshInfo: RefreshTokenRequest
    ): Response<RefreshTokenResponse>

    @POST("user/refresh.php")
    fun refreshTokenBlocking(
        @Body refreshInfo: RefreshTokenRequest
    ): Call<RefreshTokenResponse>

    @GET("geofence/list.php")
    suspend fun listGeofence(): Response<GeofenceListResponse>


    @POST("geofence/update.php")
    suspend fun updateGeofence(@Body body: GeofenceUpdateRequest): Response<GeofenceUpdateResponse>

    @DELETE("geofence/update.php")
    suspend fun deleteGeofence(): Response<GeofenceUpdateResponse>

    @POST("user/password.php")
    suspend fun changePassword(
        @Body changePassword: ChangePasswordRequest
    ): Response<ChangePasswordResponse>

    companion object {
        fun create(context: Context): ApiService {

            val client = OkHttpClient.Builder()
                .addInterceptor(AuthInterceptor(context))
                .authenticator(TokenAuthenticator(context))
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl("https://zadanie.mpage.sk/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return retrofit.create(ApiService::class.java)
        }
    }
}