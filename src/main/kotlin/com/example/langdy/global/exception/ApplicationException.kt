package com.example.langdy.global.exception

abstract class ApplicationException(
    val errorType: ErrorType,
    val customMessage: String? = null,
) : RuntimeException()