package com.example.langdy.domain.repository

import com.example.langdy.domain.entity.Course
import org.springframework.data.jpa.repository.JpaRepository

interface CourseRepository : JpaRepository<Course, Long>
