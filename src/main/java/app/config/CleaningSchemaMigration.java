package app.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

final class CleaningSchemaMigration {
    private CleaningSchemaMigration() {}

    static void migrate(EntityManagerFactory emf) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.createNativeQuery("ALTER TABLE cleaning_appointments ALTER COLUMN cleaning_client_id DROP NOT NULL")
                    .executeUpdate();
            em.getTransaction().commit();
        }
    }
}
