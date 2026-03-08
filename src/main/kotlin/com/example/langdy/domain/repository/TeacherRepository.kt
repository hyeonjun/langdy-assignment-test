package com.example.langdy.domain.repository

import com.example.langdy.domain.entity.Teacher
import org.springframework.data.jpa.repository.JpaRepository

interface TeacherRepository : JpaRepository<Teacher, Long>
