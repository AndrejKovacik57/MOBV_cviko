package com.example.mobvcviko1.data.api

import android.content.Context
import com.example.mobvcviko1.data.api.helper.AuthInterceptor
import com.example.mobvcviko1.data.api.helper.TokenAuthenticator
import com.example.mobvcviko1.data.api.model.PhotoDeleteResponse
import com.example.mobvcviko1.data.api.model.PhotoUploadResponse
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.DELETE
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface PictureApiService {
    @Multipart
    @POST("photo.php")
    suspend fun uploadPhoto(
        @Part image: MultipartBody.Part
    ): Response<PhotoUploadResponse>

    // Reuse this for deleting a picture
    @DELETE("photo.php")
    suspend fun deletePhoto(): Response<PhotoDeleteResponse>
    companion object {
        fun create(context: Context): PictureApiService {

            val client = OkHttpClient.Builder()
                .addInterceptor(AuthInterceptor(context))
                .authenticator(TokenAuthenticator(context))
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl("https://upload.mcomputing.eu/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return retrofit.create(PictureApiService::class.java)
        }
    }
}