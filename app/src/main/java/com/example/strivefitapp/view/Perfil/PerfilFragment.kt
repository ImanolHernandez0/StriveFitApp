package com.example.strivefitapp.view.Perfil

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import coil.load
import com.example.strivefitapp.MenuActivity
import com.example.strivefitapp.R
import com.example.strivefitapp.databinding.FragmentPerfilBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class PerfilFragment : Fragment() {

    private var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!
    private val PICK_IMAGE_REQUEST = 1001
    private val PERMISSION_REQUEST_CODE = 1002
    private val storage = FirebaseStorage.getInstance()
    private val storageRef = storage.reference
    private val db = FirebaseFirestore.getInstance()
    private var progressDialog: ProgressDialog? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPerfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        loadUserData()
        loadEntrenamientos()
    }

    override fun onResume() {
        super.onResume()
        loadUserData()
        loadEntrenamientos()
    }

    private fun setupUI() {
        binding.btnEditProfile.setOnClickListener { checkPermissionAndOpenImagePicker() }
        binding.btnLogout.setOnClickListener { handleLogout() }
    }

    private fun checkPermissionAndOpenImagePicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_MEDIA_IMAGES
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                    PERMISSION_REQUEST_CODE
                )
            } else {
                openImagePicker()
            }
        } else {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    PERMISSION_REQUEST_CODE
                )
            } else {
                openImagePicker()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openImagePicker()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Se necesita permiso para acceder a las imágenes",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun loadUserData() {
        val correoUsuario = getCorreoUsuario() ?: return
        showLoading("Cargando datos del usuario...")

        db.collection("user")
            .whereEqualTo("correo", correoUsuario)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    showError("No se encontró el usuario")
                    hideLoading()
                    return@addOnSuccessListener
                }

                val doc = documents.first()
                binding.apply {
                    txtNombre.text = doc.getString("nombre") ?: "No disponible"
                    txtUsuario.text = doc.getString("usuario") ?: "No disponible"
                    txtCorreo.text = doc.getString("correo") ?: "No disponible"
                    val avatarUrl = doc.getString("avatarUrl")
                    if (!avatarUrl.isNullOrEmpty()) loadAvatar(avatarUrl, imageView)
                    else imageView.setImageResource(R.drawable.default_avatar)
                }

                obtenerContadores(correoUsuario)
                hideLoading()
            }
            .addOnFailureListener { e ->
                hideLoading()
                showError("Error al cargar los datos: ${e.message}")
            }
    }

    private fun obtenerContadores(correo: String) {
        // Obtener número de seguidores
        db.collection("seguidores")
            .whereEqualTo("seguidoCorreo", correo)
            .get()
            .addOnSuccessListener { seguidoresSnapshot ->
                val numSeguidores = seguidoresSnapshot.size()
                
                // Obtener número de siguiendo
                db.collection("seguidores")
                    .whereEqualTo("seguidorCorreo", correo)
                    .get()
                    .addOnSuccessListener { siguiendoSnapshot ->
                        val numSiguiendo = siguiendoSnapshot.size()
                        
                        // Actualizar UI con los contadores
                        activity?.runOnUiThread {
                            binding.apply {
                                txtSeguidores.text = "Seguidores"
                                txtSiguiendo.text = "Siguiendo"
                                numeroSeguidores.text = numSeguidores.toString()
                                numeroSiguiendo.text = numSiguiendo.toString()
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("PerfilFragment", "Error al obtener siguiendo: ${e.message}")
                        showError("Error al cargar datos de seguimiento")
                    }
            }
            .addOnFailureListener { e ->
                Log.e("PerfilFragment", "Error al obtener seguidores: ${e.message}")
                showError("Error al cargar datos de seguidores")
            }
    }

    private fun loadEntrenamientos() {
        val correoUsuario = getCorreoUsuario() ?: return
        val container = binding.containerEntrenamientos
        container.removeAllViews()

        showLoading("Cargando entrenamientos...")

        db.collection("entrenamientos")
            .whereEqualTo("correo", correoUsuario)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    showEmptyState()
                    hideLoading()
                    return@addOnSuccessListener
                }

                documents.forEach { document ->
                    try {
                        val entrenamiento = document.toObject(Entrenamiento::class.java)
                        val cardView = createEntrenamientoCard(entrenamiento)
                        container.addView(cardView)
                    } catch (e: Exception) {
                        Log.e("PerfilFragment", "Error al procesar entrenamiento: ${e.message}")
                    }
                }
                hideLoading()
            }
            .addOnFailureListener { e ->
                hideLoading()
                showError("Error al cargar entrenamientos: ${e.message}")
            }
    }

    private fun createEntrenamientoCard(entrenamiento: Entrenamiento): View {
        val cardView = layoutInflater.inflate(R.layout.item_entrenamiento, binding.containerEntrenamientos, false)

        cardView.findViewById<TextView>(R.id.txtFecha).apply {
            text = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm")
                .format(java.util.Date(entrenamiento.timestamp))
        }

        cardView.findViewById<TextView>(R.id.txtNombreRutina).apply {
            text = entrenamiento.nombreRutina.ifEmpty {
                if (entrenamiento.ejercicios.isNotEmpty()) entrenamiento.ejercicios[0].nombre else "Entrenamiento"
            }
        }

        cardView.findViewById<TextView>(R.id.txtTiempo).apply {
            val minutos = entrenamiento.duracion / 60
            val segundos = entrenamiento.duracion % 60
            text = "Tiempo: ${minutos}m ${segundos}s"
        }

        cardView.findViewById<TextView>(R.id.txtVolumen).apply {
            text = "Volumen: %.2f kg".format(entrenamiento.volumen)
        }

        cardView.findViewById<TextView>(R.id.txtSeries).apply {
            text = "Series: ${entrenamiento.seriesTotales}"
        }

        val layoutEjercicios = cardView.findViewById<LinearLayout>(R.id.layoutEjercicios)
        layoutEjercicios.removeAllViews()

        entrenamiento.ejercicios.forEach { ejercicio ->
            TextView(requireContext()).apply {
                text = "${ejercicio.series.size} series ${ejercicio.nombre}"
                setTextColor(resources.getColor(R.color.white, null))
                textSize = 15f
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 4
                }
                layoutEjercicios.addView(this)
            }
        }

        return cardView
    }

    private fun showEmptyState() {
        binding.containerEntrenamientos.apply {
            removeAllViews()
            addView(TextView(requireContext()).apply {
                text = "No hay entrenamientos registrados"
                setTextColor(resources.getColor(R.color.white, null))
                textSize = 16f
                gravity = android.view.Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 32
                }
            })
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    private fun loadAvatar(url: String, imageView: android.widget.ImageView) {
        imageView.load(url) {
            crossfade(true)
            size(200, 200)
            crossfade(300)
            error(R.drawable.default_avatar)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            data.data?.let { uploadImageToFirebase(it) }
        }
    }

    private fun uploadImageToFirebase(imageUri: Uri) {
        val correoUsuario = getCorreoUsuario() ?: return

        showLoading("Subiendo imagen...")
        
        // Usar la referencia correcta al bucket
        val fileName = "avatar_${System.currentTimeMillis()}.jpg"
        val imageRef = storageRef.child("avatars/$fileName")

        // Verificar que la URI de la imagen sea válida
        try {
            requireContext().contentResolver.openInputStream(imageUri)?.use { inputStream ->
                // Verificar que el archivo existe y es accesible
                if (inputStream.available() > 0) {
                    // Crear los metadatos de la imagen
                    val metadata = com.google.firebase.storage.StorageMetadata.Builder()
                        .setContentType("image/jpeg")
                        .setCustomMetadata("userEmail", correoUsuario)
                        .build()

                    // Subir la imagen directamente
                    imageRef.putFile(imageUri, metadata)
                        .addOnSuccessListener {
                            Log.d("PerfilFragment", "Imagen subida exitosamente a: ${imageRef.path}")
                            imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                                Log.d("PerfilFragment", "URL de descarga obtenida: $downloadUri")
                                updateUserAvatar(correoUsuario, downloadUri.toString())
                            }.addOnFailureListener { e ->
                                Log.e("PerfilFragment", "Error al obtener URL de descarga: ${e.message}")
                                hideLoading()
                                showError("Error al obtener URL de la imagen: ${e.message}")
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("PerfilFragment", "Error al subir imagen: ${e.message}")
                            hideLoading()
                            showError("Error al subir la imagen: ${e.message}")
                        }
                        .addOnProgressListener { taskSnapshot ->
                            val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                            Log.d("PerfilFragment", "Progreso de subida: $progress%")
                        }
                } else {
                    Log.e("PerfilFragment", "El archivo de imagen está vacío o no es accesible")
                    hideLoading()
                    showError("No se puede acceder a la imagen seleccionada")
                }
            } ?: run {
                Log.e("PerfilFragment", "No se puede abrir el archivo de imagen")
                hideLoading()
                showError("No se puede abrir la imagen seleccionada")
            }
        } catch (e: Exception) {
            Log.e("PerfilFragment", "Error al procesar la imagen: ${e.message}")
            hideLoading()
            showError("Error al procesar la imagen: ${e.message}")
        }
    }

    private fun updateUserAvatar(correoUsuario: String, avatarUrl: String) {
        db.collection("user")
            .whereEqualTo("correo", correoUsuario)
            .get()
            .addOnSuccessListener { documents ->
                documents.firstOrNull()?.let { document ->
                    document.reference.update("avatarUrl", avatarUrl)
                        .addOnSuccessListener {
                            loadAvatar(avatarUrl, binding.imageView)
                            hideLoading()
                            showSuccess("Avatar actualizado con éxito")
                        }
                        .addOnFailureListener { e ->
                            hideLoading()
                            showError("Error al actualizar el avatar: ${e.message}")
                        }
                }
            }
    }

    private fun handleLogout() {
        activity?.getSharedPreferences("USER_PREFS", AppCompatActivity.MODE_PRIVATE)?.edit()?.clear()?.apply()
        startActivity(Intent(context, MenuActivity::class.java))
        requireActivity().finish()
    }

    private fun getCorreoUsuario(): String? {
        val correo = activity?.getSharedPreferences("USER_PREFS", AppCompatActivity.MODE_PRIVATE)
            ?.getString("correo", null)

        if (correo == null) {
            showError("No se ha encontrado el usuario")
        }

        return correo
    }

    private fun showLoading(message: String) {
        progressDialog?.dismiss()
        progressDialog = ProgressDialog(requireContext()).apply {
            setMessage(message)
            setCancelable(false)
            show()
        }
    }

    private fun hideLoading() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun showSuccess(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        progressDialog?.dismiss()
        _binding = null
    }
}

data class Entrenamiento(
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
