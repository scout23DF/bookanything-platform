package de.org.dexterity.bookanything.dom01geolocation.domain.ports

/** Access to the corporate MinIO buckets (source files of the imports and detail report copies). */
interface CorporateStoragePort {
    fun exists(bucket: String, key: String): Boolean

    /** Deletes an object; a missing object is not an error. */
    fun delete(bucket: String, key: String)
}
