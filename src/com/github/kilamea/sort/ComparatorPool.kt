package com.github.kilamea.sort

class ComparatorPool {
    private val comparators: MutableMap<Class<*>, Comparator<*>> = HashMap()

    fun <T> initializeComparator(clazz: Class<T>, sortField: SortField, sortOrder: SortOrder) {
        val comparator: Comparator<T> = FieldComparator.createComparator(sortField, sortOrder)
        comparators[clazz] = comparator
    }

    fun <T> changeComparator(clazz: Class<T>, sortField: SortField) {
        val comparator: Comparator<T>? = getComparator(clazz)
        if (comparator != null && comparator is FieldComparator) {
            comparator.setSortField(sortField)
        }
    }

    fun <T> changeComparator(clazz: Class<T>, sortOrder: SortOrder) {
        val comparator: Comparator<T>? = getComparator(clazz)
        if (comparator != null && comparator is FieldComparator) {
            comparator.setSortOrder(sortOrder)
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getComparator(clazz: Class<T>): Comparator<T>? {
        return comparators[clazz] as Comparator<T>?
    }
}
