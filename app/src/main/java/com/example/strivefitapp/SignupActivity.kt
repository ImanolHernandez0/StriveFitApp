package com.example.strivefitapp

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.MotionEvent
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignupActivity : AppCompatActivity() {

    private val TAG = "SignupActivity"
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val btnVolver = findViewById<ImageButton>(R.id.btnVolver2)
        val btnRegistrar = findViewById<Button>(R.id.btnRegistrar)
        val editTextNombre = findViewById<EditText>(R.id.editTextNombre)
        val editTextUsuario = findViewById<EditText>(R.id.editTextNombreUsuario)
        val editTextCorreo = findViewById<EditText>(R.id.editTextCorreo)
        val editTextPasswd = findViewById<EditText>(R.id.editTextPasswd)
        val editTextRepeatPassw = findViewById<EditText>(R.id.editTextRepeatPasswd)
        val checkBox = findViewById<CheckBox>(R.id.checkBox)

        setupPasswordVisibility(editTextPasswd)
        setupPasswordVisibility(editTextRepeatPassw)

        val progressDialog = ProgressDialog(this)

        btnVolver.setOnClickListener {
            finish()
        }

        btnRegistrar.setOnClickListener {
            val nombreS = editTextNombre.text.toString()
            val usernameS = editTextUsuario.text.toString()
            val correoS = editTextCorreo.text.toString()
            val passwdS = editTextPasswd.text.toString()
            val repeatPasswdS = editTextRepeatPassw.text.toString()

            val validationResult = validateInputs(nombreS, usernameS, correoS, passwdS, repeatPasswdS, checkBox)

            if (validationResult != null) {
                Toast.makeText(this, validationResult, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressDialog.setMessage("Registrando usuario...")
            progressDialog.setCancelable(false)
            progressDialog.show()

            db.collection("user")
                .whereEqualTo("usuario", usernameS)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {
                        progressDialog.dismiss()
                        Toast.makeText(this, "El nombre de usuario ya está en uso", Toast.LENGTH_SHORT).show()
                        editTextUsuario.text.clear()
                    } else {
                        auth.createUserWithEmailAndPassword(correoS, passwdS)
                            .addOnCompleteListener(this) { task ->
                                if (task.isSuccessful) {
                                    val user = hashMapOf(
                                        "nombre" to nombreS,
                                        "usuario" to usernameS,
                                        "correo" to correoS,
                                        "uid" to auth.currentUser?.uid,
                                        "numSeguidores" to 0,
                                        "numSiguiendo" to 0
                                    )

                                    db.collection("user")
                                        .document(auth.currentUser?.uid ?: "")
                                        .set(user)
                                        .addOnSuccessListener {
                                            progressDialog.dismiss()
                                            Toast.makeText(this, "Usuario registrado con éxito", Toast.LENGTH_SHORT).show()
                                            val intent = Intent(this, LoginActivity::class.java)
                                            startActivity(intent)
                                            finish()
                                        }
                                        .addOnFailureListener { e ->
                                            progressDialog.dismiss()
                                            Log.w(TAG, "Error al guardar datos en Firestore", e)
                                            Toast.makeText(this, "Error al guardar datos. Inténtalo de nuevo.", Toast.LENGTH_SHORT).show()
                                            auth.currentUser?.delete()
                                        }
                                } else {
                                    progressDialog.dismiss()
                                    val errorMessage = when {
                                        task.exception?.message?.contains("email") == true -> "El correo ya está registrado"
                                        else -> "Error al registrar. Inténtalo de nuevo."
                                    }
                                    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                                    if (errorMessage.contains("correo")) {
                                        editTextCorreo.text.clear()
                                    }
                                }
                            }
                    }
                }
                .addOnFailureListener { e ->
                    progressDialog.dismiss()
                    Log.w(TAG, "Error al verificar el usuario", e)
                    Toast.makeText(this, "Error al verificar el usuario. Inténtalo de nuevo.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupPasswordVisibility(editText: EditText) {
        var isPasswordVisible = false

        editText.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = editText.compoundDrawables[2]
                if (drawableEnd != null && event.rawX >= (editText.right - drawableEnd.bounds.width())) {
                    isPasswordVisible = !isPasswordVisible

                    if (isPasswordVisible) {
                        editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                        editText.setCompoundDrawablesWithIntrinsicBounds(
                            editText.compoundDrawables[0],
                            null,
                            ContextCompat.getDrawable(this, R.drawable.ic_visibility_off),
                            null
                        )
                    } else {
                        editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                        editText.setCompoundDrawablesWithIntrinsicBounds(
                            editText.compoundDrawables[0],
                            null,
                            ContextCompat.getDrawable(this, R.drawable.ic_visibility),
                            null
                        )
                    }

                    editText.setSelection(editText.text.length)

                    return@setOnTouchListener true
                }
            }
            false
        }
    }

    private fun validateInputs(
        nombre: String,
        username: String,
        correo: String,
        passwd: String,
        repeatPasswd: String,
        checkBox: CheckBox
    ): String? {
        if (nombre.isEmpty()) {
            return "Por favor, ingrese su nombre"
        }

        if (username.isEmpty()) {
            return "Por favor, ingrese un nombre de usuario"
        }

        val usernamePattern = "^[A-Za-z0-9]+$".toRegex()
        if (!usernamePattern.matches(username)) {
            return "El nombre de usuario solo puede contener letras y números"
        }

        if (correo.isEmpty()) {
            return "Por favor, ingrese su correo"
        }

        val emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        if (!emailPattern.matches(correo)) {
            return "El correo debe tener el formato: ejemplo@gmail.com"
        }

        if (passwd.isEmpty()) {
            return "Por favor, ingrese su contraseña"
        }

        if (repeatPasswd.isEmpty()) {
            return "Por favor, confirme su contraseña"
        }

        if (passwd != repeatPasswd) {
            return "Las contraseñas no coinciden"
        }

        if (passwd.length < 6) {
            return "La contraseña debe tener al menos 6 caracteres"
        }

        if (!checkBox.isChecked) {
            return "Debe aceptar la política de privacidad"
        }

        return null
    }

    private fun seguir(perfilUserId: String) {
        val currentUser = auth.currentUser ?: return
        
        val seguimiento = hashMapOf(
            "seguidorId" to currentUser.uid,
            "seguidoId" to perfilUserId,
            "timestamp" to System.currentTimeMillis()
        )

        db.runTransaction { transaction ->
            val seguimientoRef = db.collection("seguidores").document()
            transaction.set(seguimientoRef, seguimiento)
        }
    }
}
