package com.example.strivefitapp.view.Rutinas

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.strivefitapp.databinding.FragmentRutinasBinding
import com.example.strivefitapp.databinding.CardRutinaBinding
import com.example.strivefitapp.view.NuevaRutinaActivity
import com.google.firebase.firestore.FirebaseFirestore

class RutinasFragment : Fragment() {

    private var _binding: FragmentRutinasBinding? = null
    private val binding get() = _binding!!
    private val NUEVA_RUTINA_REQUEST = 1001
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRutinasBinding.inflate(inflater, container, false)
        db = FirebaseFirestore.getInstance()

        // Configurar el botón de Nueva Rutina
        binding.btnNuevaRutina.setOnClickListener {
            val intent = Intent(requireContext(), NuevaRutinaActivity::class.java)
            startActivityForResult(intent, NUEVA_RUTINA_REQUEST)
        }

        // Configurar el botón de Entrenamiento Vacío
        binding.cardEntrenamientoVacio.setOnClickListener {
            val intent = Intent(requireContext(), com.example.strivefitapp.view.EntrenoActivity::class.java)
            intent.putStringArrayListExtra("ejercicios", ArrayList())
            intent.putExtra("nombreRutina", "Entrenamiento libre")
            startActivity(intent)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cargarRutinas()
    }

    private fun cargarRutinas() {
        val sharedPreferences = requireContext().getSharedPreferences("USER_PREFS", Activity.MODE_PRIVATE)
        val correoUsuario = sharedPreferences.getString("correo", null)

        if (correoUsuario == null) {
            Toast.makeText(requireContext(), "No se encontró usuario", Toast.LENGTH_SHORT).show()
            return
        }

        // Mostrar Progress Dialog mientras se cargan las rutinas
        val progressDialog = ProgressDialog(requireContext())
        progressDialog.setMessage("Cargando rutinas...")
        progressDialog.setCancelable(false)
        progressDialog.show()

        // Consultar las rutinas del usuario en Firestore
        db.collection("rutinas")
            .whereEqualTo("correo", correoUsuario)
            .get()
            .addOnSuccessListener { querySnapshot ->
                progressDialog.dismiss()
                Log.d("RutinasFragment", "Documentos encontrados: ${querySnapshot.size()}")
                
                // Actualizar el contador de rutinas
                val cantidadRutinas = querySnapshot.size()
                binding.txtMisRutinas.text = "Mis rutinas ($cantidadRutinas)"
                
                // Limpiar el contenedor antes de añadir las nuevas tarjetas
                binding.containerRutinas.removeAllViews()

                if (querySnapshot.isEmpty) {
                    // Si no hay rutinas, mostrar mensaje
                    Toast.makeText(requireContext(), "No tienes rutinas guardadas", Toast.LENGTH_SHORT).show()
                } else {
                    // Crear una tarjeta para cada rutina
                    for (document in querySnapshot.documents) {
                        Log.d("RutinasFragment", "Procesando documento: ${document.id}")
                        
                        val titulo = document.getString("titulo")
                        val ejercicios = document.get("ejercicios") as? List<String>
                        Log.d("RutinasFragment", "Título: $titulo")

                        if (titulo != null) {
                            // Inflar la vista de la tarjeta de rutina
                            val rutinaBinding = CardRutinaBinding.inflate(layoutInflater)
                            
                            // Configurar la tarjeta
                            with(rutinaBinding) {
                                workoutName.text = titulo
                                // Mostrar los ejercicios separados por comas
                                workoutDescription.text = ejercicios?.joinToString(", ") ?: "No hay ejercicios"
                                
                                // Configurar el botón de empezar rutina
                                startButton.setOnClickListener {
                                    val intent = Intent(requireContext(), com.example.strivefitapp.view.EntrenoActivity::class.java)
                                    intent.putStringArrayListExtra("ejercicios", ArrayList(ejercicios ?: listOf()))
                                    intent.putExtra("nombreRutina", titulo)
                                    startActivity(intent)
                                }
                            }
                            
                            // Añadir la tarjeta al contenedor con margen
                            val layoutParams = ViewGroup.MarginLayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            ).apply {
                                bottomMargin = resources.getDimensionPixelSize(com.google.android.material.R.dimen.mtrl_card_spacing)
                            }
                            rutinaBinding.root.layoutParams = layoutParams
                            binding.containerRutinas.addView(rutinaBinding.root)
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Log.e("RutinasFragment", "Error al cargar rutinas", e)
                Toast.makeText(requireContext(), "Error al cargar las rutinas: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == NUEVA_RUTINA_REQUEST && resultCode == Activity.RESULT_OK) {
            cargarRutinas()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
