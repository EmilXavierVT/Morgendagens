package app.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

final class LegacyProductCatalogMigration {
    private LegacyProductCatalogMigration() {}

    static void migrate(EntityManagerFactory emf) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.createNativeQuery("ALTER TABLE products_in_requests ALTER COLUMN product_id DROP NOT NULL")
                    .executeUpdate();
            em.createNativeQuery("""
                    UPDATE products_in_requests pir
                    SET economic_product_number = COALESCE(pir.economic_product_number, p.economic_product_number, 'legacy-' || p.id),
                        product_name = COALESCE(pir.product_name, p.name),
                        product_description = COALESCE(pir.product_description, p.description),
                        unit_price = COALESCE(pir.unit_price, p.price)
                    FROM products p
                    WHERE pir.product_id = p.id
                    """).executeUpdate();
            em.createNativeQuery("UPDATE products_in_requests SET product_id = NULL WHERE product_id IS NOT NULL")
                    .executeUpdate();
            em.getTransaction().commit();
        }
    }
}
