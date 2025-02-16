package com.example.mobvcviko1.data.api.helper

import android.content.Context
import com.example.mobvcviko1.config.AppConfig
import com.example.mobvcviko1.data.PreferenceData
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val context: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
            .newBuilder()
            .addHeader("Accept", "application/json")
            .addHeader("Content-Type", "application/json")

        if (chain.request().url.toUrl().path.contains("/user/create.php", true)
            || chain.request().url.toUrl().path.contains("/user/login.php", true)
        ) {
            //here we do not need a authorization token
        } else if (chain.request().url.toUrl().path.contains("/user/refresh.php", true)) {
            //when refreshing token we need to add our user id
            PreferenceData.getInstance().getUser(context)?.id?.let {
                request.header(
                    "x-user",
                    it
                )
            }
        } else {
            //we add auth token
            val token = PreferenceData.getInstance().getUser(context)?.access
            request.header(
                "Authorization",
                "Bearer $token"
            )
        }

        // add api key to each request
        request.addHeader("x-apikey", AppConfig.API_KEY)

        return chain.proceed(request.build())
    }
}