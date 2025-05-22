package com.example.strivefitapp.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.strivefitapp.R
import com.example.strivefitapp.api.ExerciseService
import com.example.strivefitapp.databinding.ActivitySearchBinding
import com.example.strivefitapp.model.Exercise
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchBinding
    private val selectedExercises = mutableSetOf<Exercise>()
    private val exerciseService = ExerciseService.getInstance()
    private val TAG = "SearchActivity"
    private var searchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d(TAG, "SearchActivity created")

        setupListeners()
        loadInitialExercises()
    }

    private fun setupListeners() {
        // Botón Cancelar
        binding.btnCancel.setOnClickListener {
            finish()
        }

        // Botón Crear
        binding.btnSave.setOnClickListener {
            if (selectedExercises.isNotEmpty()) {
                // Devolver los ejercicios seleccionados a la actividad anterior
                val resultIntent = Intent()
                resultIntent.putStringArrayListExtra("ejercicios_seleccionados", 
                    ArrayList(selectedExercises.map { it.nombre }))
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            } else {
                Toast.makeText(this, "Por favor, selecciona al menos un ejercicio", Toast.LENGTH_SHORT).show()
            }
        }

        // Configurar el buscador con TextWatcher
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                // Cancelar búsqueda anterior si existe
                searchJob?.cancel()
                
                // Iniciar nueva búsqueda con delay
                searchJob = lifecycleScope.launch {
                    delay(300) // Esperar 300ms para evitar muchas búsquedas seguidas
                    searchExercises(s?.toString() ?: "")
                }
            }
        })
    }

    private fun loadInitialExercises() {
        Log.d(TAG, "Loading initial exercises")
        lifecycleScope.launch {
            try {
                showLoading(true)
                var exercises = exerciseService.getInitialExercises()
                Log.d(TAG, "Loaded ${exercises.size} exercises")
                
                if (exercises.isEmpty()) {
                    Log.d(TAG, "No exercises found, adding initial exercises")
                    exerciseService.addInitialExercises()
                    exercises = exerciseService.getInitialExercises()
                    Log.d(TAG, "Added and loaded ${exercises.size} exercises")
                }

                if (exercises.isNotEmpty()) {
                    displayExercises(exercises)
                } else {
                    Log.d(TAG, "Still no exercises found after adding them")
                    showErrorMessage("No se encontraron ejercicios")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al cargar ejercicios: ${e.message}", e)
                showErrorMessage("Error al cargar ejercicios: ${e.message}")
            } finally {
                showLoading(false)
            }
        }
    }

    private fun searchExercises(query: String) {
        Log.d(TAG, "Searching exercises with query: $query")
        if (query.isEmpty()) {
            loadInitialExercises()
            return
        }

        lifecycleScope.launch {
            try {
                showLoading(true)
                val exercises = exerciseService.searchExercises(query = query)
                Log.d(TAG, "Found ${exercises.size} exercises for query: $query")
                
                if (exercises.isNotEmpty()) {
                    displayExercises(exercises)
                } else {
                    showErrorMessage("No se encontraron ejercicios para '$query'")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error en la búsqueda: ${e.message}", e)
                showErrorMessage("Error en la búsqueda: ${e.message}")
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.exercisesContainer.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showErrorMessage(message: String) {
        Log.d(TAG, "Showing error message: $message")
        binding.exercisesContainer.removeAllViews()
        
        val errorView = TextView(this).apply {
            text = message
            textSize = 16f
            setTextColor(getColor(R.color.text_secondary))
            setPadding(32, 32, 32, 32)
        }
        binding.exercisesContainer.addView(errorView)
    }

    private fun displayExercises(exercises: List<Exercise>) {
        Log.d(TAG, "Displaying ${exercises.size} exercises")
        binding.exercisesContainer.removeAllViews()
        
        if (exercises.isEmpty()) {
            showErrorMessage("No se encontraron ejercicios")
            return
        }

        exercises.forEach { exercise ->
            Log.d(TAG, "Adding exercise card for: ${exercise.nombre}")
            addExerciseCard(exercise)
        }
    }

    private fun addExerciseCard(exercise: Exercise) {
        val card = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(16, 8, 16, 8)
            }
            radius = resources.getDimension(R.dimen.card_corner_radius)
            setCardBackgroundColor(getColor(R.color.surface_dark))
            strokeColor = getColor(R.color.divider)
            strokeWidth = resources.getDimensionPixelSize(R.dimen.card_stroke_width)
            elevation = resources.getDimension(R.dimen.card_elevation)
        }

        // Contenido de la card
        val content = LayoutInflater.from(this).inflate(R.layout.item_exercise, card, false)
        
        // Configurar los elementos de la card
        content.findViewById<TextView>(R.id.exerciseName).text = exercise.nombre
        content.findViewById<TextView>(R.id.exerciseMuscle).text = exercise.tipo
        content.findViewById<TextView>(R.id.exerciseDescription).text = exercise.descripcion
        
        // Cargar la imagen según el tipo
        val imageView = content.findViewById<android.widget.ImageView>(R.id.exerciseImage)
        when {
            exercise.tipo.equals("Pecho", ignoreCase = true) -> {
                imageView.setImageResource(R.drawable.pecho_icon)
            }
            exercise.tipo.equals("Bíceps", ignoreCase = true) -> {
                imageView.setImageResource(R.drawable.biceps_icon)
            }
            exercise.tipo.equals("Hombros", ignoreCase = true) -> {
                imageView.setImageResource(R.drawable.hombros_icon)
            }
            exercise.tipo.equals("Dorsales", ignoreCase = true) -> {
                imageView.setImageResource(R.drawable.dorsales_icon)
            }
            exercise.tipo.equals("Espalda Superior", ignoreCase = true) -> {
                imageView.setImageResource(R.drawable.espalda_superior_icon)
            }
            exercise.tipo.equals("Tríceps", ignoreCase = true) -> {
                imageView.setImageResource(R.drawable.triceps_icon)
            }
            else -> {
                imageView.setImageResource(R.drawable.placeholder_exercise)
            }
        }

        // Hacer la card clickeable
        card.addView(content)
        card.setOnClickListener {
            if (selectedExercises.contains(exercise)) {
                selectedExercises.remove(exercise)
                card.strokeColor = getColor(R.color.divider)
            } else {
                selectedExercises.add(exercise)
                card.strokeColor = getColor(R.color.primary)
            }
            updateSelectedCount()
        }

        // Añadir la card al contenedor
        binding.exercisesContainer.addView(card)
    }

    private fun updateSelectedCount() {
        binding.btnSave.text = "Guardar (${selectedExercises.size})"
    }

    companion object {
        fun createIntent(context: android.content.Context): android.content.Intent {
            return android.content.Intent(context, SearchActivity::class.java)
        }
    }
} 