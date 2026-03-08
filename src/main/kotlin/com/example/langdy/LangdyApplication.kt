package com.example.langdy

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@SpringBootApplication
@EnableJpaAuditing
class LangdyApplication

fun main(args: Array<String>) {
    runApplication<LangdyApplication>(*args)
}
