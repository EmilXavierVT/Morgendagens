package app.services.economic;

import app.dto.CleaningInvoiceLineDTO;
import app.dto.CleaningInvoicePreviewDTO;
import app.dto.CleaningInvoicePreviewDTO.BatchResult;
import app.entities.CleaningAppointment;
import app.entities.CleaningInvoiceRecord;
import app.entities.Product;
import app.exceptions.ApiException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class CleaningInvoiceService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Europe/Copenhagen");
    private static final DateTimeFormatter INVOICE_LINE_DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final EntityManagerFactory emf;
    private final EconomicInvoiceClient invoiceClient;

    public CleaningInvoiceService(EntityManagerFactory emf) {
        this(emf, new EconomicInvoiceClient());
    }

    public CleaningInvoiceService(EntityManagerFactory emf, EconomicInvoiceClient invoiceClient) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.emf = emf;
        this.invoiceClient = invoiceClient;
    }

    public CleaningInvoicePreviewDTO preview(String invoiceMonth, String productNumber) {
        YearMonth month = parseMonth(invoiceMonth);
        try (EntityManager em = emf.createEntityManager()) {
            Product product = requireProduct(em, productNumber);
            return buildPreview(month, product, buildCandidates(em, month, product), null);
        }
    }

    public CleaningInvoicePreviewDTO createInvoices(String invoiceMonth, String productNumber, boolean bookAndSend, boolean force) {
        YearMonth month = parseMonth(invoiceMonth);
        invoiceClient.requireCredentials();

        try (EntityManager em = emf.createEntityManager()) {
            Product product = requireProduct(em, productNumber);
            Map<Integer, Candidate> candidates = buildCandidates(em, month, product);
            int succeeded = 0;
            int failed = 0;
            int skipped = 0;
            for (Candidate candidate : candidates.values()) {
                if (candidate.alreadyInvoiced() && !force) {
                    skipped++;
                    continue;
                }
                try {
                    createOneInvoice(em, month, product, candidate, bookAndSend);
                    succeeded++;
                } catch (ApiException e) {
                    candidate.error(e.getMessage());
                    failed++;
                }
            }
            return buildPreview(month, product, candidates, new BatchResult(succeeded, failed, skipped));
        }
    }

    private CleaningInvoicePreviewDTO buildPreview(YearMonth month, Product product,
                                                   Map<Integer, Candidate> candidates, BatchResult batch) {
        List<CleaningInvoiceLineDTO> lines = candidates.values().stream()
                .sorted(Comparator.comparing(Candidate::customerName, String.CASE_INSENSITIVE_ORDER))
                .map(Candidate::toDto)
                .toList();

        return new CleaningInvoicePreviewDTO(
                month.toString(),
                product.getEconomicProductNumber(),
                product.getName(),
                money(product.getPrice()),
                lines.size(),
                lines.stream().mapToInt(CleaningInvoiceLineDTO::getAppointmentCount).sum(),
                lines.stream().map(CleaningInvoiceLineDTO::getQuantityHours).reduce(BigDecimal.ZERO, BigDecimal::add),
                lines.stream().map(CleaningInvoiceLineDTO::getNetAmount).reduce(BigDecimal.ZERO, BigDecimal::add),
                lines,
                batch
        );
    }

    private void createOneInvoice(EntityManager em, YearMonth month, Product product, Candidate candidate, boolean bookAndSend) {
        EconomicInvoiceClient.DraftInvoiceResult draft = invoiceClient.createCleaningDraftInvoice(
                candidate.customerNumber(),
                product.getEconomicProductNumber(),
                candidate.invoiceLines(),
                LocalDate.now(BUSINESS_ZONE),
                "Cleaning " + month,
                candidate.appointmentCount() + " cleaning visit" + (candidate.appointmentCount() == 1 ? "" : "s")
                        + " in " + month
        );

        Integer draftInvoiceNumber = draft.draftInvoiceNumber();
        if (draftInvoiceNumber == null) {
            throw new ApiException(502, "e-conomic did not return a draft invoice number. Check e-conomic before retrying.");
        }

        if (!bookAndSend || !candidate.alreadyInvoiced()) {
            // Keep a known draft visible if booking fails, so the next batch skips it.
            saveInvoiceRecord(em, month, product, candidate, draftInvoiceNumber, null);
        }
        if (!bookAndSend) return;

        Integer bookedInvoiceNumber;
        try {
            bookedInvoiceNumber = invoiceClient.bookDraftInvoice(draftInvoiceNumber, true).bookedInvoiceNumber();
            if (bookedInvoiceNumber == null) {
                throw new ApiException(502, "e-conomic did not return a booked invoice number");
            }
        } catch (ApiException e) {
            throw new ApiException(e.getCode(), e.getMessage()
                    + ". Check draft #" + draftInvoiceNumber + " in e-conomic before retrying.");
        }
        saveInvoiceRecord(em, month, product, candidate, draftInvoiceNumber, bookedInvoiceNumber);
    }

    private void saveInvoiceRecord(EntityManager em, YearMonth month, Product product, Candidate candidate,
                                   int draftInvoiceNumber, Integer bookedInvoiceNumber) {
        EntityTransaction tx = em.getTransaction();
        tx.begin();
        try {
            CleaningInvoiceRecord record = candidate.invoiceRecord;
            if (record == null) {
                record = new CleaningInvoiceRecord();
                record.setCreatedAt(LocalDateTime.now());
            }
            record.setInvoiceMonth(month.toString());
            record.setEconomicCustomerNumber(candidate.customerNumber());
            record.setEconomicCustomerName(candidate.customerName());
            record.setProductNumber(product.getEconomicProductNumber());
            record.setQuantityHours(candidate.quantityHours());
            record.setUnitNetPrice(candidate.unitNetPrice());
            record.setNetAmount(candidate.netAmount());
            record.setAppointmentCount(candidate.appointmentCount());
            record.setDraftInvoiceNumber(draftInvoiceNumber);
            record.setBookedInvoiceNumber(bookedInvoiceNumber);
            record.setSentByEmail(bookedInvoiceNumber != null);
            record.setStatus(bookedInvoiceNumber != null ? "SENT" : "DRAFT");
            CleaningInvoiceRecord saved = em.merge(record);
            tx.commit();
            candidate.invoiceRecord = saved;
        } catch (RuntimeException e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        }
    }

    private Map<Integer, Candidate> buildCandidates(EntityManager em, YearMonth month, Product product) {
        List<CleaningInvoiceRecord> records = findRecords(em, month.toString(), product.getEconomicProductNumber());
        Map<Integer, CleaningInvoiceRecord> recordsByCustomer = new LinkedHashMap<>();
        records.forEach(record -> recordsByCustomer.put(record.getEconomicCustomerNumber(), record));

        LocalDateTime start = month.atDay(1).atStartOfDay();
        LocalDateTime end = month.plusMonths(1).atDay(1).atStartOfDay();

        List<CleaningAppointment> appointments = em.createQuery("""
                        SELECT appointment
                        FROM CleaningAppointment appointment
                        JOIN FETCH appointment.project project
                        JOIN FETCH project.economicCustomer customer
                        WHERE appointment.appointmentTime >= :start
                          AND appointment.appointmentTime < :end
                          AND appointment.cancellationTime IS NULL
                          AND appointment.vacation = false
                          AND customer.economicAvailable = true
                        ORDER BY customer.name ASC, appointment.appointmentTime ASC
                        """, CleaningAppointment.class)
                .setParameter("start", start)
                .setParameter("end", end)
                .getResultList();

        Map<Integer, Candidate> candidates = new LinkedHashMap<>();
        for (CleaningAppointment appointment : appointments) {
            Integer customerNumber = appointment.getProject().getEconomicCustomer().getCustomerNumber();
            String customerName = appointment.getProject().getEconomicCustomer().getName();
            Candidate candidate = candidates.computeIfAbsent(customerNumber, ignored -> new Candidate(
                    customerNumber,
                    customerName,
                    money(product.getPrice()),
                    recordsByCustomer.get(customerNumber)
            ));
            candidate.addAppointment(appointment.getAppointmentTime().toLocalDate(), appointment.getDurationMinutes());
        }
        return candidates;
    }

    private Product requireProduct(EntityManager em, String productNumber) {
        if (productNumber == null || productNumber.isBlank()) {
            return requireDefaultCleaningProduct(em);
        }

        List<Product> products = em.createQuery("""
                        SELECT product
                        FROM Product product
                        WHERE product.economicProductNumber = :productNumber
                          AND product.economicAvailable = true
                        """, Product.class)
                .setParameter("productNumber", productNumber.trim())
                .getResultList();
        if (products.isEmpty()) {
            throw new ApiException(400, "Choose an available e-conomic product for cleaning invoices");
        }
        Product product = products.get(0);
        if (!Double.isFinite(product.getPrice()) || product.getPrice() < 0) {
            throw new ApiException(400, "The selected product needs a valid sales price");
        }
        if (product.getEconomicProductNumber() == null || product.getEconomicProductNumber().isBlank()) {
            throw new ApiException(400, "The selected product is missing an e-conomic product number");
        }
        return product;
    }

    private Product requireDefaultCleaningProduct(EntityManager em) {
        List<Product> products = em.createQuery("""
                        SELECT product
                        FROM Product product
                        WHERE product.economicAvailable = true
                          AND (
                            lower(product.name) LIKE :cleaning
                            OR lower(product.description) LIKE :cleaning
                            OR lower(product.economicProductGroupName) LIKE :cleaning
                            OR lower(product.name) LIKE :rengoring
                            OR lower(product.description) LIKE :rengoring
                            OR lower(product.economicProductGroupName) LIKE :rengoring
                            OR lower(product.name) LIKE :rengoering
                            OR lower(product.description) LIKE :rengoering
                            OR lower(product.economicProductGroupName) LIKE :rengoering
                          )
                        ORDER BY product.name ASC
                        """, Product.class)
                .setParameter("cleaning", "%cleaning%")
                .setParameter("rengoring", "%rengøring%")
                .setParameter("rengoering", "%rengoring%")
                .getResultList();

        if (products.isEmpty()) {
            throw new ApiException(400, "No available Rengøring product found in e-conomic");
        }
        Product product = products.get(0);
        if (!Double.isFinite(product.getPrice()) || product.getPrice() < 0) {
            throw new ApiException(400, "The Rengøring product needs a valid sales price");
        }
        if (product.getEconomicProductNumber() == null || product.getEconomicProductNumber().isBlank()) {
            throw new ApiException(400, "The Rengøring product is missing an e-conomic product number");
        }
        return product;
    }

    private List<CleaningInvoiceRecord> findRecords(EntityManager em, String invoiceMonth, String productNumber) {
        return em.createQuery("""
                        SELECT record
                        FROM CleaningInvoiceRecord record
                        WHERE record.invoiceMonth = :invoiceMonth
                          AND record.productNumber = :productNumber
                        """, CleaningInvoiceRecord.class)
                .setParameter("invoiceMonth", invoiceMonth)
                .setParameter("productNumber", productNumber)
                .getResultList();
    }

    private YearMonth parseMonth(String invoiceMonth) {
        if (invoiceMonth == null || invoiceMonth.isBlank()) {
            return YearMonth.now().minusMonths(1);
        }
        try {
            return YearMonth.parse(invoiceMonth.trim());
        } catch (DateTimeParseException e) {
            throw new ApiException(400, "invoiceMonth must use YYYY-MM");
        }
    }

    private BigDecimal money(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private static final class Candidate {
        private final Integer customerNumber;
        private final String customerName;
        private final BigDecimal unitNetPrice;
        private CleaningInvoiceRecord invoiceRecord;
        private final Map<LocalDate, Integer> durationMinutesByDate = new LinkedHashMap<>();
        private int appointmentCount;
        private int durationMinutes;
        private String error;

        private Candidate(Integer customerNumber, String customerName, BigDecimal unitNetPrice,
                          CleaningInvoiceRecord existingRecord) {
            this.customerNumber = Objects.requireNonNull(customerNumber);
            this.customerName = customerName == null || customerName.isBlank() ? "Cleaning customer" : customerName;
            this.unitNetPrice = unitNetPrice;
            this.invoiceRecord = existingRecord;
        }

        private void addAppointment(LocalDate serviceDate, int minutes) {
            int safeMinutes = Math.max(0, minutes);
            appointmentCount += 1;
            durationMinutes += safeMinutes;
            durationMinutesByDate.merge(serviceDate, safeMinutes, Integer::sum);
        }

        private Integer customerNumber() { return customerNumber; }
        private String customerName() { return customerName; }
        private int appointmentCount() { return appointmentCount; }
        private int durationMinutes() { return durationMinutes; }
        private BigDecimal unitNetPrice() { return unitNetPrice; }
        private boolean alreadyInvoiced() { return invoiceRecord != null; }
        private void error(String error) { this.error = error; }

        private BigDecimal quantityHours() {
            return BigDecimal.valueOf(durationMinutes)
                    .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        }

        private BigDecimal netAmount() {
            return quantityHours().multiply(unitNetPrice).setScale(2, RoundingMode.HALF_UP);
        }

        private List<EconomicInvoiceClient.InvoiceLine> invoiceLines() {
            return durationMinutesByDate.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(entry -> new EconomicInvoiceClient.InvoiceLine(
                            null,
                            "rengøring " + entry.getKey().format(INVOICE_LINE_DATE_FORMAT),
                            hours(entry.getValue()),
                            unitNetPrice
                    ))
                    .collect(Collectors.toList());
        }

        private CleaningInvoiceLineDTO toDto() {
            return new CleaningInvoiceLineDTO(
                    customerNumber,
                    customerName,
                    appointmentCount,
                    durationMinutes,
                    quantityHours(),
                    unitNetPrice,
                    netAmount(),
                    alreadyInvoiced(),
                    invoiceRecord != null ? invoiceRecord.getStatus() : null,
                    invoiceRecord != null ? invoiceRecord.getDraftInvoiceNumber() : null,
                    invoiceRecord != null ? invoiceRecord.getBookedInvoiceNumber() : null,
                    error
            );
        }

        private BigDecimal hours(int minutes) {
            return BigDecimal.valueOf(minutes)
                    .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        }
    }
}
