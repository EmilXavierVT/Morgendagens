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
            em.createNativeQuery("ALTER TABLE IF EXISTS youth_island_booking_items ADD COLUMN IF NOT EXISTS serving_date_time TIMESTAMP")
                    .executeUpdate();
            em.createNativeQuery("""
                    UPDATE youth_island_booking_items item
                    SET serving_date_time = booking.eventDate + item.servingTime
                    FROM youth_island_bookings booking
                    WHERE item.booking_id = booking.id AND item.serving_date_time IS NULL
                    """).executeUpdate();
            em.createNativeQuery("ALTER TABLE IF EXISTS youth_island_booking_items ALTER COLUMN menu DROP NOT NULL")
                    .executeUpdate();
            em.createNativeQuery("ALTER TABLE IF EXISTS youth_island_booking_items ALTER COLUMN room DROP NOT NULL")
                    .executeUpdate();
            em.createNativeQuery("ALTER TABLE IF EXISTS youth_island_booking_edits ADD COLUMN IF NOT EXISTS before_snapshot TEXT, ADD COLUMN IF NOT EXISTS after_snapshot TEXT, ADD COLUMN IF NOT EXISTS edited_by_name VARCHAR(512), ADD COLUMN IF NOT EXISTS edited_by_email VARCHAR(255), ADD COLUMN IF NOT EXISTS recorded_at TIMESTAMP WITH TIME ZONE, ADD COLUMN IF NOT EXISTS revision BIGINT")
                    .executeUpdate();
            em.createNativeQuery("CREATE INDEX IF NOT EXISTS idx_youth_booking_edits_booking ON youth_island_booking_edits (booking_id, id)")
                    .executeUpdate();
            em.getTransaction().commit();
        }
    }
}
