package com.abhinav.otapulse.util

data class Region(
    val name: String,
    val code: String,
    val nvid: String,
    val serverCode: String
) {
    override fun toString(): String = "$name ($code)"
}