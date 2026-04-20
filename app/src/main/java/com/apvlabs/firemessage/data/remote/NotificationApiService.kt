package com.apvlabs.firemessage.data.remote

import com.apvlabs.firemessage.data.model.NotificationRequest
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

/**
 * Servicio API para comunicación con backend
 */
interface NotificationApiService {
    
    @POST("send-notification")
    suspend fun sendNotification(@Body request: NotificationRequest): Result<Unit>
    
    companion object {
        private const val BASE_URL = "http://localhost:3000/" // Cambiar a la URL real del backend
        
        fun create(): NotificationApiService {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            
            val client = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()
            
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            
            return retrofit.create(NotificationApiService::class.java)
        }
    }
}
