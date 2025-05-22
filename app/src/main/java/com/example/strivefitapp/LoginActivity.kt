package com.example.strivefitapp

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.MotionEvent
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import android.app.ProgressDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val btnVolver = findViewById<ImageButton>(R.id.btnVolver1)
        val btnIniciar = findViewById<Button>(R.id.btnIniciar)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)

        setupPasswordVisibility(etPassword)

        btnVolver.setOnClickListener {
            finish()
        }

        btnIniciar.setOnClickListener {
            val correoS = etEmail.text.toString()
            val passwdS = etPassword.text.toString()

            if (correoS.isEmpty()) {
                Toast.makeText(this, "Por favor, ingrese su correo", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (passwdS.isEmpty()) {
                Toast.makeText(this, "Por favor, ingrese su contraseña", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val progressDialog = ProgressDialog(this)
            progressDialog.setMessage("Iniciando sesión...")
            progressDialog.setCancelable(false)
            progressDialog.show()

            auth.signInWithEmailAndPassword(correoS, passwdS)
                .addOnCompleteListener(this) { task ->
                    progressDialog.dismiss()
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Acceso exitoso", Toast.LENGTH_SHORT).show()

                        val sharedPreferences = getSharedPreferences("USER_PREFS", MODE_PRIVATE)
                        val editor = sharedPreferences.edit()
                        editor.putString("correo", correoS)
                        editor.apply()

                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        val errorMessage = when {
                            task.exception?.message?.contains("password") == true -> "Contraseña incorrecta"
                            task.exception?.message?.contains("user") == true -> "Correo no registrado"
                            else -> "Error al iniciar sesión. Inténtalo de nuevo."
                        }
                        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                        if (errorMessage.contains("Contraseña")) {
                            etPassword.text?.clear()
                        } else if (errorMessage.contains("Correo")) {
                            etEmail.text?.clear()
                            etPassword.text?.clear()
                        }
                    }
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
                            resources.getDrawable(R.drawable.ic_visibility_off, theme),
                            null
                        )
                    } else {
                        editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                        editText.setCompoundDrawablesWithIntrinsicBounds(
                            editText.compoundDrawables[0],
                            null,
                            resources.getDrawable(R.drawable.ic_visibility, theme),
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
}
