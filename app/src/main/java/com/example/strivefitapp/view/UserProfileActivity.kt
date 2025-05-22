package com.example.strivefitapp.view

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.example.strivefitapp.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Transaction

class UserProfileActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var isFollowing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        val correo = intent.getStringExtra("correo") ?: return
        val nombreUsuario = intent.getStringExtra("nombreUsuario") ?: return

        supportActionBar?.title = nombreUsuario
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        actualizarDatosPerfil(correo)
    }

    override fun onResume() {
        super.onResume()
        val correo = intent.getStringExtra("correo")
        if (correo != null) {
            Log.d("UserProfileActivity", "onResume: Recargando datos para correo: $correo")
            actualizarDatosPerfil(correo)
        }
    }

    private fun actualizarDatosPerfil(correo: String) {
        Log.d("UserProfileActivity", "Obteniendo datos para correo: $correo")
        
        db.collection("user")
            .whereEqualTo("correo", correo)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Log.e("UserProfileActivity", "No se encontró el usuario")
                    return@addOnSuccessListener
                }

                val document = documents.first()
                Log.d("UserProfileActivity", "Documento encontrado: ${document.data}")

                val nombre = document.getString("nombre") ?: "No disponible"
                val usuario = document.getString("usuario") ?: "No disponible"
                val numSeguidores = document.getLong("numSeguidores") ?: 0L
                val numSiguiendo = document.getLong("numSiguiendo") ?: 0L

                Log.d("UserProfileActivity", "Datos obtenidos - Nombre: $nombre, Usuario: $usuario")
                Log.d("UserProfileActivity", "Contadores - Seguidores: $numSeguidores, Siguiendo: $numSiguiendo")

                runOnUiThread {
                    try {
                        findViewById<TextView>(R.id.txtNombre).text = nombre
                        findViewById<TextView>(R.id.txtUsuario).text = usuario
                        findViewById<TextView>(R.id.txtNumSeguidores).text = numSeguidores.toString()
                        findViewById<TextView>(R.id.txtNumSiguiendo).text = numSiguiendo.toString()
                        
                        document.getString("avatarUrl")?.let { url ->
                            findViewById<ImageView>(R.id.imgAvatar).load(url) {
                                crossfade(true)
                                error(R.drawable.default_avatar)
                            }
                        } ?: run {
                            findViewById<ImageView>(R.id.imgAvatar).setImageResource(R.drawable.default_avatar)
                        }
                        
                        configurarBotonSeguir(correo)
                        
                        Log.d("UserProfileActivity", "UI actualizada correctamente")
                    } catch (e: Exception) {
                        Log.e("UserProfileActivity", "Error al actualizar UI: ${e.message}")
                        e.printStackTrace()
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("UserProfileActivity", "Error al obtener datos: ${e.message}")
                Toast.makeText(this, "Error al cargar datos del perfil", Toast.LENGTH_SHORT).show()
            }
    }

    private fun configurarBotonSeguir(perfilCorreo: String) {
        val btnSeguir = findViewById<MaterialButton>(R.id.btnSeguir)
        val currentUser = auth.currentUser

        if (currentUser == null || currentUser.email == perfilCorreo) {
            Log.d("UserProfileActivity", "Ocultando botón seguir: currentUser=${currentUser?.email}, perfilCorreo=$perfilCorreo")
            btnSeguir.visibility = View.GONE
            return
        }

        db.collection("seguidores")
            .whereEqualTo("seguidorCorreo", currentUser.email)
            .whereEqualTo("seguidoCorreo", perfilCorreo)
            .get()
            .addOnSuccessListener { documents ->
                isFollowing = !documents.isEmpty
                Log.d("UserProfileActivity", "Estado de seguimiento: isFollowing=$isFollowing")
                actualizarBotonSeguir(btnSeguir)
            }
            .addOnFailureListener { e ->
                Log.e("UserProfileActivity", "Error al verificar seguimiento: ${e.message}")
            }

        btnSeguir.visibility = View.VISIBLE
        btnSeguir.setOnClickListener {
            if (isFollowing) {
                dejarDeSeguir(perfilCorreo)
            } else {
                seguir(perfilCorreo)
            }
        }
    }

    private fun actualizarBotonSeguir(btnSeguir: MaterialButton) {
        if (isFollowing) {
            btnSeguir.text = "DEJAR DE SEGUIR"
            btnSeguir.setBackgroundColor(resources.getColor(R.color.error, theme))
        } else {
            btnSeguir.text = "SEGUIR"
            btnSeguir.setBackgroundColor(resources.getColor(R.color.primary, theme))
        }
    }

    private fun seguir(perfilCorreo: String) {
        val currentUser = auth.currentUser ?: return
        val currentUserEmail = currentUser.email ?: return
        Log.d("UserProfileActivity", "Iniciando seguimiento: seguidorCorreo=$currentUserEmail, seguidoCorreo=$perfilCorreo")
        
        val seguimiento = hashMapOf(
            "seguidorCorreo" to currentUserEmail,
            "seguidoCorreo" to perfilCorreo,
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("user")
            .whereEqualTo("correo", perfilCorreo)
            .get()
            .addOnSuccessListener { seguidoQuerySnapshot ->
                val seguidoDoc = seguidoQuerySnapshot.documents.firstOrNull()
                if (seguidoDoc == null) {
                    Toast.makeText(this, "No se encontró el usuario a seguir", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                db.collection("user")
                    .whereEqualTo("correo", currentUserEmail)
                    .get()
                    .addOnSuccessListener { seguidorQuerySnapshot ->
                        val seguidorDoc = seguidorQuerySnapshot.documents.firstOrNull()
                        if (seguidorDoc == null) {
                            Toast.makeText(this, "Error al obtener datos del seguidor", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }

                        Log.d("UserProfileActivity", "Documentos encontrados - Seguido: ${seguidoDoc.id}, Seguidor: ${seguidorDoc.id}")

                        db.runTransaction { transaction ->
                            val numSeguidores = (seguidoDoc.getLong("numSeguidores") ?: 0) + 1
                            val numSiguiendo = (seguidorDoc.getLong("numSiguiendo") ?: 0) + 1

                            Log.d("UserProfileActivity", "Nuevos valores - Seguidores: $numSeguidores, Siguiendo: $numSiguiendo")

                            val seguimientoRef = db.collection("seguidores").document()
                            transaction.set(seguimientoRef, seguimiento)
                            transaction.update(seguidoDoc.reference, "numSeguidores", numSeguidores)
                            transaction.update(seguidorDoc.reference, "numSiguiendo", numSiguiendo)

                            mapOf(
                                "numSeguidores" to numSeguidores,
                                "numSiguiendo" to numSiguiendo
                            )
                        }.addOnSuccessListener { nuevosValores ->
                            Log.d("UserProfileActivity", "Transacción completada - Nuevos valores: $nuevosValores")
                            runOnUiThread {
                                findViewById<TextView>(R.id.txtNumSeguidores).text = nuevosValores["numSeguidores"].toString()
                                
                                if (currentUserEmail == perfilCorreo) {
                                    findViewById<TextView>(R.id.txtNumSiguiendo).text = nuevosValores["numSiguiendo"].toString()
                                }
                                
                                isFollowing = true
                                actualizarBotonSeguir(findViewById(R.id.btnSeguir))
                                Toast.makeText(this, "Ahora sigues a este usuario", Toast.LENGTH_SHORT).show()
                                
                                actualizarDatosPerfil(perfilCorreo)
                                
                                if (currentUserEmail == perfilCorreo) {
                                    actualizarDatosPerfil(currentUserEmail)
                                }
                            }
                        }.addOnFailureListener { e ->
                            Log.e("UserProfileActivity", "Error al seguir al usuario: ${e.message}")
                            Toast.makeText(this, "Error al seguir al usuario", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
    }

    private fun dejarDeSeguir(perfilCorreo: String) {
        val currentUser = auth.currentUser ?: return
        val currentUserEmail = currentUser.email ?: return
        
        db.collection("seguidores")
            .whereEqualTo("seguidorCorreo", currentUserEmail)
            .whereEqualTo("seguidoCorreo", perfilCorreo)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(this, "No se encontró la relación de seguimiento", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val seguimientoDoc = documents.first()
                val seguimientoRef = seguimientoDoc.reference

                db.collection("user")
                    .whereEqualTo("correo", perfilCorreo)
                    .get()
                    .addOnSuccessListener { seguidoQuerySnapshot ->
                        val seguidoDoc = seguidoQuerySnapshot.documents.firstOrNull()
                        if (seguidoDoc == null) {
                            Toast.makeText(this, "No se encontró el usuario", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }

                        db.collection("user")
                            .whereEqualTo("correo", currentUserEmail)
                            .get()
                            .addOnSuccessListener { seguidorQuerySnapshot ->
                                val seguidorDoc = seguidorQuerySnapshot.documents.firstOrNull()
                                if (seguidorDoc == null) {
                                    Toast.makeText(this, "Error al obtener datos del seguidor", Toast.LENGTH_SHORT).show()
                                    return@addOnSuccessListener
                                }

                                db.runTransaction { transaction ->
                                    val numSeguidores = (seguidoDoc.getLong("numSeguidores") ?: 0) - 1
                                    val numSiguiendo = (seguidorDoc.getLong("numSiguiendo") ?: 0) - 1

                                    transaction.delete(seguimientoRef)
                                    transaction.update(seguidoDoc.reference, "numSeguidores", numSeguidores)
                                    transaction.update(seguidorDoc.reference, "numSiguiendo", numSiguiendo)

                                    mapOf(
                                        "numSeguidores" to numSeguidores,
                                        "numSiguiendo" to numSiguiendo
                                    )
                                }.addOnSuccessListener { nuevosValores ->
                                    runOnUiThread {
                                        findViewById<TextView>(R.id.txtNumSeguidores).text = nuevosValores["numSeguidores"].toString()
                                        
                                        if (currentUserEmail == perfilCorreo) {
                                            findViewById<TextView>(R.id.txtNumSiguiendo).text = nuevosValores["numSiguiendo"].toString()
                                        }
                                        
                                        isFollowing = false
                                        actualizarBotonSeguir(findViewById(R.id.btnSeguir))
                                        Toast.makeText(this, "Has dejado de seguir a este usuario", Toast.LENGTH_SHORT).show()
                                        
                                        actualizarDatosPerfil(perfilCorreo)
                                        
                                        if (currentUserEmail == perfilCorreo) {
                                            actualizarDatosPerfil(currentUserEmail)
                                        }
                                    }
                                }.addOnFailureListener { e ->
                                    Log.e("UserProfileActivity", "Error al dejar de seguir al usuario: ${e.message}")
                                    Toast.makeText(this, "Error al dejar de seguir al usuario", Toast.LENGTH_SHORT).show()
                                }
                            }
                    }
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 