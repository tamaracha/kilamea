package com.github.kilamea.sort

/**
 * A pool for managing comparators associated with different classes.
 * 
 * @since 0.1.0
 * @property comparators A map storing comparators for different classes.
 */
class ComparatorPool {
    private val comparators: MutableMap<Class<*>, Comparator<*>> = HashMap()

    /**
     * Initializes a comparator for the specified class using the given sort field and sort order.
     * 
     * @param T The type of objects compared.
     * @param clazz The class for which the comparator is to be initialized.
     * @param sortField The field to sort by.
     * @param sortOrder The order in which to sort.
     */
    fun <T> initializeComparator(clazz: Class<T>, sortField: SortField, sortOrder: SortOrder) {
        val comparator: Comparator<T> = FieldComparator.createComparator(sortField, sortOrder)
        comparators[clazz] = comparator
    }

    /**
     * Changes the sort field of the comparator for the specified class.
     * 
     * @param T The type of objects compared.
     * @param clazz The class for which the comparator's sort field is to be changed.
     * @param sortField The new field to sort by.
     */
    fun <T> changeComparator(clazz: Class<T>, sortField: SortField) {
        val comparator: Comparator<T>? = getComparator(clazz)
        if (comparator != null && comparator is FieldComparator) {
            comparator.setSortField(sortField)
        }
    }

    /**
     * Changes the sort order of the comparator for the specified class.
     * 
     * @param T The type of objects compared.
     * @param clazz The class for which the comparator's sort order is to be changed.
     * @param sortOrder The new order in which to sort.
     */
    fun <T> changeComparator(clazz: Class<T>, sortOrder: SortOrder) {
        val comparator: Comparator<T>? = getComparator(clazz)
        if (comparator != null && comparator is FieldComparator) {
            comparator.setSortOrder(sortOrder)
        }
    }

    /**
     * Retrieves the comparator for the specified class.
     * 
     * @param T The type of objects compared.
     * @param clazz The class for which to retrieve the comparator.
     * @return The comparator for the specified class, or null if none is found.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getComparator(clazz: Class<T>): Comparator<T>? {
        return comparators[clazz] as Comparator<T>?
    }
}
