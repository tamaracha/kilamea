package com.github.kilamea.sort

import java.lang.reflect.Field

import com.github.kilamea.util.compareToIgnoreCase

/**
 * A comparator that compares objects based on a specified field and sort order.
 * 
 * @since 0.1.0
 * @property sortField The field to sort by.
 * @property sortOrder The order in which to sort.
 */
internal class FieldComparator<T>(private var sortField: SortField, private var sortOrder: SortOrder) : Comparator<T> {
    /**
     * Compares two objects based on the specified field and sort order.
     * 
     * @param obj1 The first object to compare.
     * @param obj2 The second object to compare.
     * @return A negative integer, zero, or a positive integer as the first argument is less than, equal to, or greater than the second.
     */
    @Suppress("UNCHECKED_CAST")
    override fun compare(obj1: T, obj2: T): Int {
        try {
            var fieldName = sortField.name
            fieldName = fieldName[0].lowercase() + fieldName.substring(1)

            val field: Field = obj1!!::class.java.getDeclaredField(fieldName)
            field.isAccessible = true

            val value1: Any? = field.get(obj1)
            val value2: Any? = field.get(obj2)

            var result = 0

            if (value1 is String && value2 is String) {
                result = value1.compareToIgnoreCase(value2)
            } else if (value1 is Comparable<*> && value2 is Comparable<*>) {
                result = (value1 as Comparable<Any>).compareTo(value2)
            }

            if (sortOrder == SortOrder.Descending) {
                result *= -1
            }

            return result
        } catch (e: NoSuchFieldException) {
            return 0
        } catch (e: IllegalAccessException) {
            return 0
        }
    }

    /**
     * Sets the field by which to sort.
     * 
     * @param sortField The new field to sort by.
     */
    fun setSortField(sortField: SortField) {
        this.sortField = sortField
    }

    /**
     * Sets the order in which to sort.
     * 
     * @param sortOrder The new order in which to sort.
     */
    fun setSortOrder(sortOrder: SortOrder) {
        this.sortOrder = sortOrder
    }

    companion object {
        /**
         * Creates a new FieldComparator with the specified sort field and sort order.
         * 
         * @param T The type of objects compared.
         * @param sortField The field to sort by.
         * @param sortOrder The order in which to sort.
         * @return A new FieldComparator instance.
         */
        fun <T> createComparator(sortField: SortField, sortOrder: SortOrder): FieldComparator<T> {
            return FieldComparator(sortField, sortOrder)
        }
    }
}
