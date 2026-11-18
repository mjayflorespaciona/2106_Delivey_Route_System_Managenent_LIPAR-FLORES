package com.example.delitrack.backEnd.data

abstract class BaseEntity {
    abstract val entityId: String
    abstract val dateCreated: String

    fun isEntityValid(): Boolean {
        return entityId.isNotEmpty()
    }
}
