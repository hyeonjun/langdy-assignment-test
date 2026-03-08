package com.example.langdy

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync

@EnableAsync
@SpringBootApplication
class LangdyApplication

fun main(args: Array<String>) {
    runApplication<LangdyApplication>(*args)
}
