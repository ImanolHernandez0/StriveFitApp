package com.example.strivefitapp.api

import android.util.Log
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "https://api.api-ninjas.com/"
    private const val TIMEOUT_SECONDS = 30L
    private const val TAG = "RetrofitClient"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // Interceptor personalizado para depurar las solicitudes
    private val debugInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        
        // Log de la solicitud
        Log.d(TAG, "Realizando solicitud a: ${originalRequest.url}")
        Log.d(TAG, "Método: ${originalRequest.method}")
        Log.d(TAG, "Headers: ${originalRequest.headers}")
        
        try {
            val response = chain.proceed(originalRequest)
            
            // Log de la respuesta
            Log.d(TAG, "Código de respuesta: ${response.code}")
            if (!response.isSuccessful) {
                Log.e(TAG, "Error en la respuesta: ${response.body?.string()}")
            }
            
            response
        } catch (e: Exception) {
            Log.e(TAG, "Error en la solicitud: ${e.message}", e)
            throw e
        }
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(debugInterceptor)
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(client)
        .build()

    val exerciseApi: ExerciseApi = retrofit.create(ExerciseApi::class.java)
} 