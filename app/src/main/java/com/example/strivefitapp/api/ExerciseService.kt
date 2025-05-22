package com.example.strivefitapp.api

import com.example.strivefitapp.model.Exercise
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.SetOptions
import android.util.Log

class ExerciseService {
    private val db = FirebaseFirestore.getInstance()
    private val exercisesCollection = db.collection("ejercicios")
    private val TAG = "ExerciseService"

    suspend fun addExercise(exercise: Exercise): String = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Adding exercise: ${exercise.nombre}")
            val exerciseData = hashMapOf(
                "nombre" to exercise.nombre,
                "tipo" to exercise.tipo,
                "descripcion" to exercise.descripcion,
                "imagenUrl" to exercise.imagenUrl,
                "tipoEjercicio" to listOf(exercise.tipo),
                "created_at" to Timestamp.now(),
                "updated_at" to Timestamp.now()
            )

            val documentReference = exercisesCollection.add(exerciseData).await()
            Log.d(TAG, "Exercise added successfully with ID: ${documentReference.id}")
            documentReference.id
        } catch (e: Exception) {
            Log.e(TAG, "Error adding exercise ${exercise.nombre}: ${e.message}")
            throw e
        }
    }

    suspend fun updateExercise(id: String, exercise: Exercise): Boolean = withContext(Dispatchers.IO) {
        try {
            val exerciseData = hashMapOf(
                "nombre" to exercise.nombre,
                "tipo" to exercise.tipo,
                "descripcion" to exercise.descripcion,
                "imagenUrl" to exercise.imagenUrl,
                "tipoEjercicio" to listOf(exercise.tipo),
                "keywords" to generateKeywords(exercise.nombre),
                "updated_at" to Timestamp.now()
            )

            exercisesCollection.document(id)
                .set(exerciseData, SetOptions.merge())
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteExercise(id: String): Boolean = withContext(Dispatchers.IO) {
        try {
            exercisesCollection.document(id).delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getExerciseById(id: String): Exercise? = withContext(Dispatchers.IO) {
        try {
            val document = exercisesCollection.document(id).get().await()
            if (document.exists()) {
                document.toObject(Exercise::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun searchExercises(
        query: String? = null,
        tipo: String? = null,
        limit: Long = 20
    ): List<Exercise> = withContext(Dispatchers.IO) {
        try {
            val snapshot = exercisesCollection
                .orderBy("tipo")
                .orderBy("nombre")
                .get()
                .await()

            val exercises = snapshot.toObjects(Exercise::class.java)
            
            if (query.isNullOrBlank()) {
                return@withContext exercises.take(limit.toInt())
            }

            val queryLower = query.lowercase().trim()
            Log.d(TAG, "Filtering exercises with query: $queryLower")
            
            val filteredExercises = exercises.filter { exercise ->
                exercise.nombre.lowercase().contains(queryLower)
            }

            Log.d(TAG, "Found ${filteredExercises.size} exercises matching query")
            filteredExercises.take(limit.toInt())
        } catch (e: Exception) {
            Log.e(TAG, "Error en searchExercises: ${e.message}", e)
            throw e
        }
    }

    suspend fun getInitialExercises(limit: Long = 20): List<Exercise> = withContext(Dispatchers.IO) {
        try {
            val snapshot = exercisesCollection
                .orderBy("tipo")
                .orderBy("nombre")
                .limit(limit)
                .get()
                .await()
            
            snapshot.toObjects(Exercise::class.java)
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun deleteAllExercises() = withContext(Dispatchers.IO) {
        try {
            val snapshot = exercisesCollection.get().await()
            snapshot.documents.forEach { doc ->
                doc.reference.delete().await()
                Log.d(TAG, "Deleted exercise with ID: ${doc.id}")
            }
            Log.d(TAG, "All exercises deleted successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting exercises: ${e.message}")
            throw e
        }
    }

    suspend fun addInitialExercises() = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting to add initial exercises")
        try {
            deleteAllExercises()
            
            val exercises = listOf(
                createExercise(
                    "Pec Deck",
                    "Pecho",
                    "Ejercicio de aislamiento para el pecho usando la máquina pec deck."
                ),
                createExercise(
                    "Press Inclinado Mancuernas",
                    "Pecho",
                    "Press de pecho con mancuernas en banco inclinado."
                ),
                createExercise(
                    "Press Banca Barra",
                    "Pecho",
                    "Press de pecho clásico con barra en banco plano."
                ),
                createExercise(
                    "Press de Pecho Máquina",
                    "Pecho",
                    "Press de pecho en máquina guiada."
                ),
                createExercise(
                    "Elevaciones Laterales Mancuernas",
                    "Hombros",
                    "Elevaciones laterales con mancuernas para desarrollar los deltoides."
                ),
                createExercise(
                    "Elevaciones Laterales Máquina",
                    "Hombros",
                    "Elevaciones laterales en máquina para hombros."
                ),
                createExercise(
                    "Press de Hombros Sentado",
                    "Hombros",
                    "Press militar sentado con barra para hombros."
                ),
                createExercise(
                    "Tríceps con Polea",
                    "Tríceps",
                    "Extensiones de tríceps con polea alta."
                ),
                createExercise(
                    "Máquina para Fondos Sentado",
                    "Tríceps",
                    "Fondos asistidos en máquina para tríceps."
                ),
                createExercise(
                    "Jalón al Pecho",
                    "Dorsales",
                    "Jalones al pecho en polea alta para dorsales."
                ),
                createExercise(
                    "Remo Inclinado con Barra",
                    "Espalda Superior",
                    "Remo con barra en banco inclinado."
                ),
                createExercise(
                    "Remo Sentado con Cable",
                    "Espalda Superior",
                    "Remo con cable en posición sentada."
                ),
                createExercise(
                    "Pullover con Polea",
                    "Dorsales",
                    "Pullover con polea alta para dorsales."
                ),
                createExercise(
                    "Vuelos Posteriores Máquina",
                    "Espalda Superior",
                    "Vuelos posteriores en máquina para deltoides posteriores."
                ),
                createExercise(
                    "Curl Martillo Mancuernas",
                    "Bíceps",
                    "Curl de bíceps estilo martillo con mancuernas."
                ),
                createExercise(
                    "Preacher Curl Máquina",
                    "Bíceps",
                    "Curl de bíceps en banco predicador con máquina."
                ),
                createExercise(
                    "Aducción de Caderas",
                    "Aductores",
                    "Ejercicio de aducción de caderas en máquina."
                ),
                createExercise(
                    "Sentadilla Sumo",
                    "Piernas",
                    "Sentadilla con piernas abiertas y pies apuntando hacia afuera."
                )
            )

            exercises.forEach { exercise ->
                addExercise(exercise)
            }
            
            Log.d(TAG, "Initial exercises added successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error adding initial exercises: ${e.message}")
            throw e
        }
    }

    private fun createExercise(nombre: String, tipo: String, descripcion: String): Exercise {
        return Exercise(
            id = "",
            nombre = nombre,
            tipo = tipo,
            descripcion = descripcion,
            imagenUrl = ""
        )
    }

    private fun generateKeywords(nombre: String): List<String> {
        val words = nombre.lowercase().split(" ")
        val keywords = mutableListOf<String>()
        
        words.forEach { word ->
            if (word.length > 2) {
                keywords.add(word)
            }
        }
        
        return keywords
    }

    companion object {
        @Volatile
        private var instance: ExerciseService? = null

        fun getInstance(): ExerciseService {
            return instance ?: synchronized(this) {
                instance ?: ExerciseService().also { instance = it }
            }
        }
    }
} 