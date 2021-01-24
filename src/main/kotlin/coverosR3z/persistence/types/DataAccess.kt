package coverosR3z.persistence.types

interface DataAccess<T> {

    /**
     * carry out some write action on the data.
     * @param action a lambda to receive the set of data and do whatever you want with it
     */
    fun <R> actOn(action: (ChangeTrackingSet<T>) -> R) : R

    /**
     * carry out some readonly action on the data.
     * @param action a lambda to receive the set of data and do whatever you want with it
     */
    fun <R> read(action: (Set<T>) -> R) : R
}