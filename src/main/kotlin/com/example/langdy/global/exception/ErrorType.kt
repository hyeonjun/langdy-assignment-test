package com.example.langdy.global.exception

interface ErrorType {
    val status: Int
    val errorCode: String

    fun prefix(): String
    fun messageCode(): String = "${prefix()}.${errorCode}"
}