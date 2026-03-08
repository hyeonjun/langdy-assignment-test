package com.example.langdy.domain.entity

import com.example.langdy.domain.base.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.CascadeType
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "students")
class Student(
    @Column(nullable = false)
    var name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var os: Os,
) : BaseEntity() {

    @OneToMany(mappedBy = "student", cascade = [CascadeType.ALL], orphanRemoval = true)
    val lessons: MutableList<Lesson> = mutableListOf()

    enum class Os {
        IOS, ANDROID
    }
}
