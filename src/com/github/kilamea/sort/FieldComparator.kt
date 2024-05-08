package com.github.kilamea.sort

import java.lang.reflect.Field

internal class FieldComparator<T>(private var sortField: SortField, private var sortOrder: SortOrder) : Comparator<T> {
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
                result = value1.compareTo(value2, ignoreCase = true)
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

    fun setSortField(sortField: SortField) {
        this.sortField = sortField
    }

    fun setSortOrder(sortOrder: SortOrder) {
        this.sortOrder = sortOrder
    }

    companion object {
        fun <T> createComparator(sortField: SortField, sortOrder: SortOrder): FieldComparator<T> {
            return FieldComparator(sortField, sortOrder)
        }
    }
}
