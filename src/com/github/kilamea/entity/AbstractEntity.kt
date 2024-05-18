package com.github.kilamea.entity

import java.util.UUID

/**
 * Abstract class that serves as the base for entities with a unique ID.
 * Each entity is assigned a randomly generated UUID upon creation.
 *
 * @since 0.1.0
 * @property id The unique ID of the entity.
 */
abstract class AbstractEntity {
    var id: String

    /**
     * Constructor that creates a new entity with a randomly generated UUID.
     */
    constructor() {
        val uuid = UUID.randomUUID()
        id = uuid.toString()
    }

    /**
     * Compares this entity with another object for equality.
     * Two entities are considered equal if their IDs are the same.
     *
     * @param other The object to be compared with.
     * @return true if the objects are equal, otherwise false.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is AbstractEntity && id == other.id
    }

    /**
     * Computes the hash code of the entity based on its ID.
     *
     * @return The hash code of the entity.
     */
    override fun hashCode(): Int {
        return id.hashCode()
    }

    /**
     * Returns the ID of the entity as a string.
     *
     * @return The ID of the entity.
     */
    override fun toString(): String {
        return id
    }
}
