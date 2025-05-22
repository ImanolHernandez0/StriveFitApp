package com.example.strivefitapp.view

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ImageView
import android.widget.TableLayout
import android.widget.TableRow
import androidx.appcompat.app.AppCompatActivity
import com.example.strivefitapp.R
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import android.os.Handler
import android.os.Looper
import android.widget.Button
import androidx.appcompat.app.AlertDialog

class EntrenoActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private var secondsElapsed = 0
    private lateinit var handler: Handler
    private lateinit var tvDuracion: TextView
    private var totalKg = 0.0f
    private var totalSeries = 0
    private val allSeries = mutableListOf<MutableList<Map<String, Any>>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_entreno)

        db = FirebaseFirestore.getInstance()
        val ejercicios = intent.getStringArrayListExtra("ejercicios") ?: arrayListOf()
        val container = findViewById<LinearLayout>(R.id.containerEjercicios)
        tvDuracion = findViewById(R.id.tvDuracion)
        handler = Handler(Looper.getMainLooper())
        startTimer()

        val tvVolumen = findViewById<TextView>(R.id.tvVolumen)
        val tvSeries = findViewById<TextView>(R.id.tvSeries)
        allSeries.clear()

        db.collection("rutinas")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { rutinas ->
                val rutinaDoc = rutinas.documents.firstOrNull()
                val ejerciciosRutina = rutinaDoc?.get("ejercicios") as? List<Map<String, Any>>

                for (ejercicioNombre in ejercicios) {
                    val ejercicioView = LayoutInflater.from(this).inflate(R.layout.item_entreno_ejercicio, container, false)
                    val tvNombre = ejercicioView.findViewById<TextView>(R.id.tvNombreEjercicio)
                    val ivEjercicio = ejercicioView.findViewById<ImageView>(R.id.ivEjercicio)
                    val tableSeries = ejercicioView.findViewById<TableLayout>(R.id.tableSeries)
                    val btnAgregarSerie = ejercicioView.findViewById<Button>(R.id.btnAgregarSerie)

                    db.collection("ejercicios")
                        .whereEqualTo("nombre", ejercicioNombre)
                        .get()
                        .addOnSuccessListener { documents ->
                            if (!documents.isEmpty) {
                                val doc = documents.documents[0]
                                tvNombre.text = doc.getString("nombre") ?: ejercicioNombre
                                val imagenUrl = doc.getString("imagenUrl")
                                if (!imagenUrl.isNullOrEmpty()) {
                                    Glide.with(this).load(imagenUrl).into(ivEjercicio)
                                }
                            } else {
                                tvNombre.text = ejercicioNombre
                            }
                        }
                        .addOnFailureListener {
                            tvNombre.text = ejercicioNombre
                        }

                    val seriesList = mutableListOf<Map<String, Any>>()
                    seriesList.add(mapOf("kg" to 0, "reps" to 0))
                    allSeries.add(seriesList)

                    fun renderSeries() {
                        while (tableSeries.childCount > 1) tableSeries.removeViewAt(1)
                        for ((i, serie) in seriesList.withIndex()) {
                            val row = TableRow(this)
                            row.layoutParams = TableLayout.LayoutParams(
                                TableLayout.LayoutParams.MATCH_PARENT,
                                TableLayout.LayoutParams.WRAP_CONTENT
                            )
                            row.addView(makeCell((i + 1).toString()))
                            row.addView(makeCell("-"))

                            val etKg = android.widget.EditText(this)
                            etKg.layoutParams = TableRow.LayoutParams(
                                TableRow.LayoutParams.WRAP_CONTENT,
                                TableRow.LayoutParams.WRAP_CONTENT
                            )
                            etKg.hint = "0"
                            etKg.setHintTextColor(android.graphics.Color.GRAY)
                            etKg.setText(if ((serie["kg"] as? Number)?.toFloat() == 0f) "" else serie["kg"].toString())
                            etKg.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                            etKg.setEms(4)
                            etKg.setTextColor(resources.getColor(R.color.white, null))
                            etKg.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                            etKg.setPadding(4, 4, 4, 4)
                            etKg.setBackgroundResource(android.R.color.transparent)
                            etKg.setOnFocusChangeListener { _, hasFocus ->
                                if (!hasFocus) {
                                    val value = etKg.text.toString().replace(",", ".")
                                    val kg = value.toFloatOrNull() ?: 0f
                                    seriesList[i] = seriesList[i].toMutableMap().apply { put("kg", kg) }
                                }
                            }
                            row.addView(etKg)

                            val etReps = android.widget.EditText(this)
                            etReps.layoutParams = TableRow.LayoutParams(
                                TableRow.LayoutParams.WRAP_CONTENT,
                                TableRow.LayoutParams.WRAP_CONTENT
                            )
                            etReps.hint = "0"
                            etReps.setHintTextColor(android.graphics.Color.GRAY)
                            etReps.setText(if ((serie["reps"] as? Number)?.toInt() == 0) "" else serie["reps"].toString())
                            etReps.inputType = android.text.InputType.TYPE_CLASS_NUMBER
                            etReps.setEms(3)
                            etReps.setTextColor(resources.getColor(R.color.white, null))
                            etReps.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                            etReps.setPadding(4, 4, 4, 4)
                            etReps.setBackgroundResource(android.R.color.transparent)
                            etReps.setOnFocusChangeListener { _, hasFocus ->
                                if (!hasFocus) {
                                    val reps = etReps.text.toString().toIntOrNull() ?: 0
                                    seriesList[i] = seriesList[i].toMutableMap().apply { put("reps", reps) }
                                }
                            }
                            row.addView(etReps)

                            val check = makeCell("\u2713")
                            check.setTextColor(resources.getColor(R.color.white, null))
                            check.isClickable = true
                            check.isFocusable = true
                            var isChecked = serie["checked"] as? Boolean ?: false
                            if (isChecked) {
                                check.setTextColor(android.graphics.Color.GREEN)
                            }
                            check.setOnClickListener {
                                isChecked = !isChecked
                                val kg = etKg.text.toString().replace(",", ".").toFloatOrNull() ?: 0f
                                val reps = etReps.text.toString().toIntOrNull() ?: 0
                                seriesList[i] = seriesList[i].toMutableMap().apply { put("checked", isChecked); put("kg", kg); put("reps", reps) }
                                check.setTextColor(if (isChecked) android.graphics.Color.GREEN else resources.getColor(R.color.white, null))
                                recalculateTotals()
                            }
                            row.addView(check)
                            tableSeries.addView(row)
                        }
                    }

                    renderSeries()

                    btnAgregarSerie.setOnClickListener {
                        seriesList.add(mapOf("kg" to 0, "reps" to 0))
                        renderSeries()
                    }

                    container.addView(ejercicioView)
                }
            }

        val btnTerminar = findViewById<Button>(R.id.btnTerminar)
        btnTerminar.setOnClickListener {
            val dialog = androidx.appcompat.app.AlertDialog.Builder(this, R.style.AlertDialogTheme)
                .setMessage("¿Seguro que quieres terminar?")
                .setNegativeButton("Descartar Entrenamiento") { _, _ ->
                    finish()
                }
                .setPositiveButton("Guardar") { _, _ ->
                    val ejerciciosGuardados = mutableListOf<Map<String, Any>>()
                    for ((idx, seriesList) in allSeries.withIndex()) {
                        val nombreEjercicio = ejercicios.getOrNull(idx) ?: continue
                        val seriesEjercicio = seriesList.map { serie ->
                            mapOf(
                                "kg" to ((serie["kg"] as? Number)?.toFloat() ?: 0f),
                                "reps" to ((serie["reps"] as? Number)?.toInt() ?: 0)
                            )
                        }
                        ejerciciosGuardados.add(
                            mapOf(
                                "nombre" to nombreEjercicio,
                                "series" to seriesEjercicio
                            )
                        )
                    }
                    val sharedPreferences = getSharedPreferences("USER_PREFS", MODE_PRIVATE)
                    val correoUsuario = sharedPreferences.getString("correo", null)
                    val nombreRutina = intent.getStringExtra("nombreRutina") ?: "Entrenamiento libre"
                    val entrenamiento = mapOf(
                        "timestamp" to System.currentTimeMillis(),
                        "duracion" to secondsElapsed,
                        "volumen" to totalKg,
                        "seriesTotales" to totalSeries,
                        "ejercicios" to ejerciciosGuardados,
                        "correo" to (correoUsuario ?: ""),
                        "nombreRutina" to nombreRutina
                    )
                    db.collection("entrenamientos")
                        .add(entrenamiento)
                        .addOnSuccessListener {
                            android.widget.Toast.makeText(this, "Entrenamiento guardado", android.widget.Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        .addOnFailureListener { e ->
                            android.widget.Toast.makeText(this, "Error al guardar: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                        }
                }
                .create()
            dialog.show()
        }
    }

    private fun makeCell(text: String): TextView {
        val tv = TextView(this)
        tv.text = text
        tv.setTextColor(resources.getColor(R.color.white, null))
        tv.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
        tv.setPadding(4, 4, 4, 4)
        tv.layoutParams = TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT
        )
        return tv
    }

    private fun startTimer() {
        handler.post(object : Runnable {
            override fun run() {
                secondsElapsed++
                tvDuracion.text = formatTime(secondsElapsed)
                handler.postDelayed(this, 1000)
            }
        })
    }

    private fun formatTime(seconds: Int): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return String.format("%02d:%02d:%02d", h, m, s)
    }

    private fun recalculateTotals() {
        var newTotalKg = 0.0f
        var newTotalSeries = 0
        for (series in allSeries) {
            for (serie in series) {
                val checked = serie["checked"] as? Boolean ?: false
                val kg = (serie["kg"] as? Number)?.toFloat() ?: 0f
                val reps = (serie["reps"] as? Number)?.toInt() ?: 0
                if (checked) {
                    newTotalKg += kg * reps
                    newTotalSeries += 1
                }
            }
        }
        totalKg = newTotalKg
        totalSeries = newTotalSeries
        val tvVolumen = findViewById<TextView>(R.id.tvVolumen)
        val tvSeries = findViewById<TextView>(R.id.tvSeries)
        tvVolumen.text = String.format("%.2f kg", totalKg)
        tvSeries.text = totalSeries.toString()
    }
} 