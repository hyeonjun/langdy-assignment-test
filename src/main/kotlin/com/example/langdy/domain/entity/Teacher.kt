package com.example.langdy.domain.entity

import com.example.langdy.domain.base.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.CascadeType
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "teachers")
class Teacher(
    @Column(nullable = false)
    var name: String,
) : BaseEntity() {

    @OneToMany(mappedBy = "teacher", cascade = [CascadeType.ALL], orphanRemoval = true)
    val lessons: MutableList<Lesson> = mutableListOf()
}
