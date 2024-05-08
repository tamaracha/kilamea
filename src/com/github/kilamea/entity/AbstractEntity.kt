package com.github.kilamea.entity

import java.util.UUID

abstract class AbstractEntity {
    var id: String

    constructor() {
        val uuid = UUID.randomUUID()
        id = uuid.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is AbstractEntity && id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return id
    }
}
