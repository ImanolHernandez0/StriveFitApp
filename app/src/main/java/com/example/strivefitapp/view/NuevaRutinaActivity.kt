package com.example.strivefitapp.view

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.strivefitapp.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.material.card.MaterialCardView

class NuevaRutinaActivity : AppCompatActivity() {

    private lateinit var edtTituloRutina: EditText
    private lateinit var btnGuardar: TextView
    private lateinit var btnCancelar: TextView
    private lateinit var btnAgregar: Button
    private lateinit var ejerciciosContainer: LinearLayout
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var db: FirebaseFirestore
    private val SEARCH_EXERCISE_REQUEST = 1002
    private val TAG = "NuevaRutinaActivity"

    private val ejerciciosSeleccionados = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nueva_rutina)

        initViews()
        db = FirebaseFirestore.getInstance()
        setupListeners()
    }

    private fun initViews() {
        edtTituloRutina = findViewById(R.id.edtTituloRutina)
        btnGuardar = findViewById(R.id.btnGuardar)
        btnCancelar = findViewById(R.id.btnCancelar)
        btnAgregar = findViewById(R.id.btnAgregar)
        ejerciciosContainer = findViewById(R.id.ejerciciosContainer)
        layoutEmptyState = findViewById(R.id.layoutEmptyState)
    }

    private fun setupListeners() {
        btnAgregar.setOnClickListener {
            val intent = Intent(this, SearchActivity::class.java)
            startActivityForResult(intent, SEARCH_EXERCISE_REQUEST)
        }

        btnGuardar.setOnClickListener {
            guardarRutina()
        }

        btnCancelar.setOnClickListener {
            finish()
        }
    }

    private fun guardarRutina() {
        val titulo = edtTituloRutina.text.toString().trim()

        if (titulo.isEmpty()) {
            edtTituloRutina.error = "Introduce un título"
            return
        }

        if (ejerciciosSeleccionados.isEmpty()) {
            Toast.makeText(this, "Agrega al menos un ejercicio", Toast.LENGTH_SHORT).show()
            return
        }

        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Guardando rutina...")
        progressDialog.setCancelable(false)
        progressDialog.show()

        val sharedPreferences = getSharedPreferences("USER_PREFS", MODE_PRIVATE)
        val correoUsuario = sharedPreferences.getString("correo", null)

        if (correoUsuario == null) {
            progressDialog.dismiss()
            Toast.makeText(this, "Error: No se encontró el usuario", Toast.LENGTH_SHORT).show()
            return
        }

        val rutina = hashMapOf(
            "titulo" to titulo,
            "descripcion" to "- Ejercicios personalizados",
            "timestamp" to System.currentTimeMillis(),
            "correo" to correoUsuario,
            "ejercicios" to ejerciciosSeleccionados
        )

        db.collection("rutinas")
            .add(rutina)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(this, "Rutina guardada correctamente", Toast.LENGTH_SHORT).show()

                val resultIntent = Intent()
                resultIntent.putExtra("tituloRutina", titulo)
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Log.e(TAG, "Error al guardar la rutina: ${e.message}", e)
                Toast.makeText(this, "Error al guardar la rutina: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SEARCH_EXERCISE_REQUEST && resultCode == Activity.RESULT_OK) {
            val ejerciciosSeleccionados = data?.getStringArrayListExtra("ejercicios_seleccionados")
            if (ejerciciosSeleccionados != null) {
                ejerciciosSeleccionados.forEach { ejercicio ->
                    if (!this.ejerciciosSeleccionados.contains(ejercicio)) {
                        this.ejerciciosSeleccionados.add(ejercicio)
                        agregarEjercicioView(ejercicio)
                    }
                }
                
                actualizarUIEjercicios()
            }
        }
    }
    
    private fun agregarEjercicioView(nombreEjercicio: String) {
        if (ejerciciosSeleccionados.size == 1) {
            layoutEmptyState.visibility = View.GONE
        }

        val card = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(8, 4, 8, 4)
            }
            radius = resources.getDimension(R.dimen.card_corner_radius)
            setCardBackgroundColor(getColor(R.color.surface_dark))
            cardElevation = resources.getDimension(R.dimen.card_elevation)
        }

        val content = LayoutInflater.from(this)
            .inflate(R.layout.item_ejercicio_seleccionado, card, false)

        content.findViewById<TextView>(R.id.tvNombreEjercicio).text = nombreEjercicio

        content.findViewById<ImageButton>(R.id.btnDelete).setOnClickListener {
            ejerciciosContainer.removeView(card)
            ejerciciosSeleccionados.remove(nombreEjercicio)
            actualizarUIEjercicios()

            if (ejerciciosSeleccionados.isEmpty()) {
                layoutEmptyState.visibility = View.VISIBLE
            }
        }

        card.addView(content)
        ejerciciosContainer.addView(card)
    }
    
    private fun actualizarUIEjercicios() {
        if (ejerciciosSeleccionados.isEmpty()) {
            btnAgregar.text = "＋ Agregar ejercicio"
        } else {
            btnAgregar.text = "＋ Agregar ejercicio (${ejerciciosSeleccionados.size})"
        }
    }
}