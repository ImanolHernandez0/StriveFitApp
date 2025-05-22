package com.example.strivefitapp.view.Inicio

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import coil.load
import com.example.strivefitapp.R
import com.example.strivefitapp.view.UserProfileActivity
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date

class InicioFragment : Fragment() {

    private var _binding: com.example.strivefitapp.databinding.FragmentInicioBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = com.example.strivefitapp.databinding.FragmentInicioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cargarFeed()
    }

    private fun cargarFeed() {
        val containerFeed = binding.containerFeed
        containerFeed.removeAllViews()
        db.collection("entrenamientos")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { entrenos ->
                for (entrenoDoc in entrenos) {
                    val entrenamiento = entrenoDoc.toObject(EntrenamientoFeed::class.java)
                    val correo = entrenamiento.correo
                    db.collection("user")
                        .whereEqualTo("correo", correo)
                        .get()
                        .addOnSuccessListener { users ->
                            val user = users.firstOrNull()
                            val nombreUsuario = user?.getString("usuario") ?: "Usuario"
                            val avatarUrl = user?.getString("avatarUrl")
                            val card = crearCard(entrenamiento, nombreUsuario, avatarUrl, correo)
                            containerFeed.addView(card)
                        }
                        .addOnFailureListener { e ->
                            Log.e("InicioFragment", "Error al cargar usuario: ${e.message}")
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("InicioFragment", "Error al cargar entrenamientos: ${e.message}")
            }
    }

    private fun crearCard(entrenamiento: EntrenamientoFeed, nombreUsuario: String, avatarUrl: String?, correo: String): View {
        val cardView = layoutInflater.inflate(R.layout.item_feed_entrenamiento, binding.containerFeed, false)
        
        // Configurar el nombre de usuario y hacerlo clickeable
        val txtUsuario = cardView.findViewById<TextView>(R.id.txtUsuario)
        txtUsuario.text = nombreUsuario
        txtUsuario.setOnClickListener {
            val intent = Intent(requireContext(), UserProfileActivity::class.java).apply {
                putExtra("correo", correo)
                putExtra("nombreUsuario", nombreUsuario)
            }
            startActivity(intent)
        }

        cardView.findViewById<TextView>(R.id.txtNombreRutina).text = entrenamiento.nombreRutina
        cardView.findViewById<TextView>(R.id.txtFecha).text = SimpleDateFormat("dd/MM/yyyy HH:mm").format(Date(entrenamiento.timestamp))
        cardView.findViewById<TextView>(R.id.txtTiempo).text = "Tiempo: ${entrenamiento.duracion / 60}m ${entrenamiento.duracion % 60}s"
        cardView.findViewById<TextView>(R.id.txtVolumen).text = "Volumen: %.2f kg".format(entrenamiento.volumen)
        cardView.findViewById<TextView>(R.id.txtSeries).text = "Series: ${entrenamiento.seriesTotales}"
        
        val imgAvatar = cardView.findViewById<ImageView>(R.id.imgAvatar)
        if (!avatarUrl.isNullOrEmpty()) {
            imgAvatar.load(avatarUrl) {
                crossfade(true)
                error(R.drawable.default_avatar)
            }
        } else {
            imgAvatar.setImageResource(R.drawable.default_avatar)
        }

        // Ejercicios
        val layoutEjercicios = cardView.findViewById<LinearLayout>(R.id.layoutEjercicios)
        layoutEjercicios.removeAllViews()
        entrenamiento.ejercicios?.forEach { ejercicio ->
            val tv = TextView(requireContext())
            tv.text = "${ejercicio.series.size} series ${ejercicio.nombre}"
            tv.setTextColor(resources.getColor(R.color.white, null))
            tv.textSize = 15f
            layoutEjercicios.addView(tv)
        }
        return cardView
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

data class EntrenamientoFeed(
    val timestamp: Long = 0,
    val duracion: Int = 0,
    val volumen: Double = 0.0,
    val seriesTotales: Int = 0,
    val correo: String = "",
    val nombreRutina: String = "",
    val ejercicios: List<Ejercicio> = listOf()
)

data class Ejercicio(
    val nombre: String = "",
    val series: List<Serie> = listOf()
)

data class Serie(
    val kg: Double = 0.0,
    val reps: Int = 0
)