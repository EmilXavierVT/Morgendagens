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
            em.createNativeQuery("ALTER TABLE IF EXISTS youth_island_invoice_records DROP CONSTRAINT IF EXISTS uk_youth_island_invoice_period_customer")
                    .executeUpdate();
            em.createNativeQuery("ALTER TABLE IF EXISTS youth_island_booking_items ADD COLUMN IF NOT EXISTS unit_price_override NUMERIC(12, 2)")
                    .executeUpdate();
            em.createNativeQuery("ALTER TABLE IF EXISTS youth_island_booking_items ALTER COLUMN menu DROP NOT NULL")
                    .executeUpdate();
            em.createNativeQuery("ALTER TABLE IF EXISTS youth_island_booking_items ALTER COLUMN room DROP NOT NULL")
                    .executeUpdate();
            em.getTransaction().commit();
        }
    }
}
