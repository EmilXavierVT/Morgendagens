package app.services.economic;

import app.dto.EconomicCustomerListResponse;
import app.dto.EconomicCustomerResponse;
import app.dto.EconomicProductListResponse;
import app.dto.EconomicProductResponse;
import app.dto.ProductDTO;
import app.entities.EconomicCustomer;
import app.entities.Product;
import app.exceptions.ApiException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.NoResultException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

public class EconomicMirrorSyncService {
    private static final Logger logger = LoggerFactory.getLogger(EconomicMirrorSyncService.class);
    private static final int PAGE_SIZE = 1000;
    private static final int MAX_PAGES = 100;

    private final EntityManagerFactory emf;
    private final EconomicProductClient productClient;
    private final EconomicCustomerClient customerClient;

    public EconomicMirrorSyncService(EntityManagerFactory emf) {
        this(emf, new EconomicProductClient(), new EconomicCustomerClient());
    }

    public EconomicMirrorSyncService(EntityManagerFactory emf, EconomicProductClient productClient,
                                     EconomicCustomerClient customerClient) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.emf = emf;
        this.productClient = productClient;
        this.customerClient = customerClient;
    }

    public void syncAllSafely() {
        try {
            SyncResult products = syncProducts();
            SyncResult customers = syncCustomers();
            logger.info("operation=syncEconomicMirror products={} customers={}", products, customers);
        } catch (ApiException e) {
            logger.warn("operation=syncEconomicMirror skipped status={} message={}", e.getCode(), e.getMessage());
        } catch (RuntimeException e) {
            logger.warn("operation=syncEconomicMirror skipped unexpectedError={}", e.getMessage(), e);
        }
    }

    public SyncResult syncProducts() {
        SyncResult result = new SyncResult();
        try (EntityManager em = emf.createEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                for (int skipPages = 0; skipPages < MAX_PAGES; skipPages++) {
                    EconomicProductListResponse response = productClient.listProducts(PAGE_SIZE, skipPages);
                    List<EconomicProductResponse> page = response.getCollection();
                    for (EconomicProductResponse product : page) {
                        if (product == null || isBlank(product.getProductNumber())) continue;
                        upsertProduct(em, product, result);
                    }
                    if (page.size() < PAGE_SIZE) break;
                }
                tx.commit();
            } catch (RuntimeException e) {
                if (tx.isActive()) tx.rollback();
                throw e;
            }
        }
        return result;
    }

    public SyncResult syncCustomers() {
        SyncResult result = new SyncResult();
        try (EntityManager em = emf.createEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                for (int skipPages = 0; skipPages < MAX_PAGES; skipPages++) {
                    EconomicCustomerListResponse response = customerClient.listCustomers(PAGE_SIZE, skipPages);
                    List<EconomicCustomerResponse> page = response.getCollection();
                    for (EconomicCustomerResponse customer : page) {
                        if (customer == null || customer.getCustomerNumber() == null) continue;
                        upsertCustomer(em, customer, result);
                    }
                    if (page.size() < PAGE_SIZE) break;
                }
                tx.commit();
            } catch (RuntimeException e) {
                if (tx.isActive()) tx.rollback();
                throw e;
            }
        }
        return result;
    }

    public void upsertProduct(ProductDTO dto) {
        if (dto == null || isBlank(dto.getEconomicProductNumber())) return;
        EconomicProductResponse response = new EconomicProductResponse();
        response.setProductNumber(dto.getEconomicProductNumber());
        response.setName(dto.getName());
        response.setDescription(dto.getDescription());
        response.setSalesPrice(BigDecimal.valueOf(dto.getPrice()));
        response.setBarred(!dto.isEconomicAvailable());
        response.setLastUpdated(dto.getEconomicLastUpdated());
        response.setSelf(dto.getEconomicSelf());
        if (dto.getEconomicProductGroupNumber() != null || dto.getEconomicProductGroupName() != null) {
            EconomicProductResponse.ProductGroup group = new EconomicProductResponse.ProductGroup();
            group.setProductGroupNumber(dto.getEconomicProductGroupNumber());
            group.setName(dto.getEconomicProductGroupName());
            response.setProductGroup(group);
        }

        try (EntityManager em = emf.createEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                upsertProduct(em, response, new SyncResult());
                tx.commit();
            } catch (RuntimeException e) {
                if (tx.isActive()) tx.rollback();
                throw e;
            }
        }
    }

    private void upsertProduct(EntityManager em, EconomicProductResponse source, SyncResult result) {
        Product product = findProduct(em, source.getProductNumber());
        if (product == null) {
            product = new Product();
            product.setEconomicProductNumber(source.getProductNumber());
            applyProduct(product, source);
            em.persist(product);
            result.created++;
            return;
        }

        if (applyProduct(product, source)) {
            result.updated++;
        } else {
            result.unchanged++;
        }
    }

    private boolean applyProduct(Product target, EconomicProductResponse source) {
        boolean changed = false;
        changed |= setIfChanged(target::getName, target::setName, source.getName());
        changed |= setIfChanged(target::getDescription, target::setDescription, source.getDescription());
        changed |= setDoubleIfChanged(target, source.getSalesPrice());
        changed |= setIntIfChanged(target, EconomicProductCategoryClassifier.classify(source));
        changed |= setIfChanged(target::getEconomicProductNumber, target::setEconomicProductNumber, source.getProductNumber());
        changed |= setIfChanged(target::getEconomicBarred, target::setEconomicBarred, source.getBarred());
        changed |= setBooleanIfChanged(target, !Boolean.TRUE.equals(source.getBarred()));
        changed |= setOffsetDateTimeIfChanged(target::getEconomicLastUpdated, target::setEconomicLastUpdated,
                source.getLastUpdated());
        changed |= setIfChanged(target::getEconomicSelf, target::setEconomicSelf, source.getSelf());
        Integer groupNumber = source.getProductGroup() == null ? null : source.getProductGroup().getProductGroupNumber();
        String groupName = source.getProductGroup() == null ? null : source.getProductGroup().getName();
        changed |= setIfChanged(target::getEconomicProductGroupNumber, target::setEconomicProductGroupNumber, groupNumber);
        changed |= setIfChanged(target::getEconomicProductGroupName, target::setEconomicProductGroupName, groupName);
        return changed;
    }

    private void upsertCustomer(EntityManager em, EconomicCustomerResponse source, SyncResult result) {
        EconomicCustomer customer = findCustomer(em, source.getCustomerNumber());
        if (customer == null) {
            customer = new EconomicCustomer();
            customer.setCustomerNumber(source.getCustomerNumber());
            applyCustomer(customer, source);
            em.persist(customer);
            result.created++;
            return;
        }

        if (applyCustomer(customer, source)) {
            result.updated++;
        } else {
            result.unchanged++;
        }
    }

    private boolean applyCustomer(EconomicCustomer target, EconomicCustomerResponse source) {
        boolean changed = false;
        changed |= setIfChanged(target::getCustomerNumber, target::setCustomerNumber, source.getCustomerNumber());
        changed |= setIfChanged(target::getName, target::setName, source.getName());
        changed |= setIfChanged(target::getEmail, target::setEmail, source.getEmail());
        changed |= setIfChanged(target::getAddress, target::setAddress, source.getAddress());
        changed |= setIfChanged(target::getCity, target::setCity, source.getCity());
        changed |= setIfChanged(target::getZip, target::setZip, source.getZip());
        changed |= setIfChanged(target::getCountry, target::setCountry, source.getCountry());
        changed |= setIfChanged(target::getMobilePhone, target::setMobilePhone, source.getMobilePhone());
        changed |= setIfChanged(target::getTelephoneAndFaxNumber, target::setTelephoneAndFaxNumber, source.getTelephoneAndFaxNumber());
        changed |= setIfChanged(target::getCorporateIdentificationNumber, target::setCorporateIdentificationNumber,
                source.getCorporateIdentificationNumber());
        changed |= setIfChanged(target::getVatNumber, target::setVatNumber, source.getVatNumber());
        changed |= setIfChanged(target::getBalance, target::setBalance, source.getBalance());
        changed |= setIfChanged(target::getBarred, target::setBarred, source.getBarred());
        changed |= setOffsetDateTimeIfChanged(target::getLastUpdated, target::setLastUpdated, source.getLastUpdated());
        changed |= setIfChanged(target::getSelf, target::setSelf, source.getSelf());
        changed |= setIfChanged(target::isEconomicAvailable, target::setEconomicAvailable,
                !Boolean.TRUE.equals(source.getBarred()));
        Integer groupNumber = source.getCustomerGroup() == null ? null : source.getCustomerGroup().getCustomerGroupNumber();
        String groupName = source.getCustomerGroup() == null ? null : source.getCustomerGroup().getName();
        changed |= setIfChanged(target::getCustomerGroupNumber, target::setCustomerGroupNumber, groupNumber);
        changed |= setIfChanged(target::getCustomerGroupName, target::setCustomerGroupName, groupName);
        return changed;
    }

    private Product findProduct(EntityManager em, String productNumber) {
        try {
            return em.createQuery("""
                            SELECT product
                            FROM Product product
                            WHERE product.economicProductNumber = :productNumber
                            """, Product.class)
                    .setParameter("productNumber", productNumber)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    private EconomicCustomer findCustomer(EntityManager em, Integer customerNumber) {
        try {
            return em.createQuery("""
                            SELECT customer
                            FROM EconomicCustomer customer
                            WHERE customer.customerNumber = :customerNumber
                            """, EconomicCustomer.class)
                    .setParameter("customerNumber", customerNumber)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    private boolean setDoubleIfChanged(Product target, BigDecimal sourcePrice) {
        double next = sourcePrice == null ? 0.0 : sourcePrice.doubleValue();
        if (Double.compare(target.getPrice(), next) == 0) return false;
        target.setPrice(next);
        return true;
    }

    private boolean setIntIfChanged(Product target, int next) {
        if (target.getType() == next) return false;
        target.setType(next);
        return true;
    }

    private boolean setBooleanIfChanged(Product target, boolean next) {
        if (target.isEconomicAvailable() == next) return false;
        target.setEconomicAvailable(next);
        return true;
    }

    private <T> boolean setIfChanged(ValueReader<T> reader, ValueWriter<T> writer, T next) {
        if (Objects.equals(reader.get(), next)) return false;
        writer.set(next);
        return true;
    }

    private boolean setOffsetDateTimeIfChanged(ValueReader<OffsetDateTime> reader,
                                               ValueWriter<OffsetDateTime> writer,
                                               OffsetDateTime next) {
        OffsetDateTime current = reader.get();
        if (current == null && next == null) return false;
        if (current != null && next != null && current.toInstant().equals(next.toInstant())) return false;
        writer.set(next);
        return true;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    interface ValueReader<T> {
        T get();
    }

    interface ValueWriter<T> {
        void set(T value);
    }

    public static final class SyncResult {
        private int created;
        private int updated;
        private int unchanged;

        public int getCreated() {
            return created;
        }

        public int getUpdated() {
            return updated;
        }

        public int getUnchanged() {
            return unchanged;
        }

        @Override
        public String toString() {
            return "{created=" + created + ", updated=" + updated + ", unchanged=" + unchanged + "}";
        }
    }
}
