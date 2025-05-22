package com.example.strivefitapp.api

import com.example.strivefitapp.model.Exercise
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface ExerciseApi {
    companion object {
        // Valores válidos para los parámetros
        val VALID_TYPES = listOf("cardio", "strength", "stretching", "plyometrics")
        val VALID_MUSCLES = listOf("abdominals", "abductors", "adductors", "biceps", "calves", 
            "chest", "forearms", "glutes", "hamstrings", "lats", "lower_back", "middle_back",
            "neck", "quadriceps", "traps", "triceps")
        val VALID_DIFFICULTIES = listOf("beginner", "intermediate", "expert")
    }

    @Headers(
        "X-Api-Key: 96gWn0H82vm9xneyxsAP5w==WeY0ANxWCjYyfJc7" // API key de ejemplo, reemplaza con tu propia API key
    )
    @GET("v1/exercises")
    suspend fun searchExercises(
        @Query("name") name: String? = null,
        @Query("type") type: String? = null,
        @Query("muscle") muscle: String? = null,
        @Query("difficulty") difficulty: String? = null
    ): Response<List<Exercise>>
} 