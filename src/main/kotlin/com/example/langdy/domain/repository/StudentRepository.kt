package com.example.langdy.domain.repository

import com.example.langdy.domain.entity.Student
import org.springframework.data.jpa.repository.JpaRepository

interface StudentRepository : JpaRepository<Student, Long>
