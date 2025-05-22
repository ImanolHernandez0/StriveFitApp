package com.example.strivefitapp.model

import com.google.gson.annotations.SerializedName

data class Exercise(
    val id: String = "",
    val nombre: String = "",
    val tipo: String = "",
    val descripcion: String = "",
    val imagenUrl: String = "",
    val tipoEjercicio: List<String> = listOf()
) {
    companion object {
        val TIPOS = listOf(
            "Pecho",
            "Hombros",
            "Tríceps",
            "Dorsales",
            "Espalda Superior",
            "Bíceps",
            "Aductores",
            "Cuádriceps",
            "Glúteos",
            "Abductores",
            "Gemelos",
            "Isquiotibiales"
        )
    }
} 