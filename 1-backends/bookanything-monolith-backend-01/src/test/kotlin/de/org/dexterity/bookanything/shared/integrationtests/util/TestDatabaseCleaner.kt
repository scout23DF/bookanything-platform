package de.org.dexterity.bookanything.shared.integrationtests.util

import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class TestDatabaseCleaner {

    @Autowired
    private lateinit var entityManager: EntityManager

    @Transactional
    fun cleanAndResetDatabase() {
        entityManager.createNativeQuery("DELETE FROM tb_address").executeUpdate()
        entityManager.createNativeQuery("DELETE FROM tb_geo_location").executeUpdate()
        entityManager.createNativeQuery("DELETE FROM tb_localizable_place").executeUpdate()

        // Reset ID sequences for a clean state in each test
        entityManager.createNativeQuery("ALTER SEQUENCE tb_address_id_seq RESTART WITH 1").executeUpdate()
        entityManager.createNativeQuery("ALTER SEQUENCE tb_geo_location_id_seq RESTART WITH 1").executeUpdate()
    }
}
