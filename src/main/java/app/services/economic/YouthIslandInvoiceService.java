package app.services.economic;

import app.dto.YouthIslandInvoiceEventDTO;
import app.dto.YouthIslandInvoiceLineDTO;
import app.dto.YouthIslandInvoicePreviewDTO;
import app.entities.EconomicCustomer;
import app.entities.Product;
import app.entities.YouthIslandBooking;
import app.entities.YouthIslandBookingItem;
import app.entities.YouthIslandInvoiceRecord;
import app.exceptions.ApiException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class YouthIslandInvoiceService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Europe/Copenhagen");
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final String STANDARD_EVENT_CUSTOMER_NAME = "ungdomsø event";
    private static final String LEGACY_STANDARD_EVENT_CUSTOMER_NAME = "ungdomsøen event #1";
    private static final BigDecimal EVENT_DISCOUNT_PERCENTAGE = BigDecimal.valueOf(33);
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final EntityManagerFactory emf;
    private final EconomicInvoiceClient invoiceClient;

    public YouthIslandInvoiceService(EntityManagerFactory emf) {
        this(emf, new EconomicInvoiceClient());
    }

    public YouthIslandInvoiceService(EntityManagerFactory emf, EconomicInvoiceClient invoiceClient) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.emf = emf;
        this.invoiceClient = invoiceClient;
    }

    public YouthIslandInvoicePreviewDTO preview(String startDate, Integer customerNumber, boolean applyEventDiscount) {
        LocalDate periodStart = parseStartDate(startDate);
        LocalDate periodEnd = periodStart.plusDays(7);

        try (EntityManager em = emf.createEntityManager()) {
            EconomicCustomer customer = requireCustomer(em, customerNumber);
            BatchCandidate batch = buildBatch(em, periodStart, periodEnd, customer, eventDiscount(applyEventDiscount));
            return batch.toDto();
        }
    }

    public YouthIslandInvoicePreviewDTO createInvoice(String startDate, Integer customerNumber, boolean applyEventDiscount,
                                                      boolean bookAndSend, boolean force) {
        LocalDate periodStart = parseStartDate(startDate);
        LocalDate periodEnd = periodStart.plusDays(7);

        try (EntityManager em = emf.createEntityManager()) {
            EconomicCustomer customer = requireCustomer(em, customerNumber);
            BatchCandidate batch = buildBatch(em, periodStart, periodEnd, customer, eventDiscount(applyEventDiscount));

            if (batch.events().isEmpty()) {
                batch.error("No billable Youth Island bookings found for this week.");
                return batch.toDto();
            }

            for (EventCandidate event : batch.events()) {
                if (event.alreadyInvoiced() && !force) continue;
                if (event.lines().isEmpty()) {
                    event.error("No billable booking lines found for this event.");
                    continue;
                }
                try {
                    createOneEventInvoice(em, batch, event, bookAndSend);
                } catch (ApiException e) {
                    event.error(e.getMessage());
                }
            }

            return batch.toDto();
        }
    }

    private void createOneEventInvoice(EntityManager em, BatchCandidate batch, EventCandidate event, boolean bookAndSend) {
        EconomicInvoiceClient.DraftInvoiceResult draft = invoiceClient.createDraftInvoice(
                batch.customerNumber(),
                event.economicLines(),
                batch.invoiceDate(),
                event.eventName(),
                "Ungdomsøen booking " + event.spectraReservationNumber()
                        + " on " + event.eventDate().format(DISPLAY_DATE)
        );

        Integer bookedInvoiceNumber = null;
        if (bookAndSend && draft.draftInvoiceNumber() != null) {
            EconomicInvoiceClient.BookedInvoiceResult booked = invoiceClient.bookDraftInvoice(draft.draftInvoiceNumber(), true);
            bookedInvoiceNumber = booked.bookedInvoiceNumber();
        }

        EntityTransaction tx = em.getTransaction();
        tx.begin();
        try {
            YouthIslandInvoiceRecord record = event.existingRecord();
            if (record == null) {
                record = new YouthIslandInvoiceRecord();
                record.setCreatedAt(LocalDateTime.now(BUSINESS_ZONE));
            }
            record.setPeriodStart(batch.periodStart());
            record.setPeriodEnd(batch.periodEnd());
            record.setInvoiceDate(batch.invoiceDate());
            record.setBookingId(event.bookingId());
            record.setEventName(event.eventName());
            record.setEconomicCustomerNumber(batch.customerNumber());
            record.setEconomicCustomerName(batch.customerName());
            record.setBookingCount(1);
            record.setLineCount(event.lineCount());
            record.setTotalGuests(event.totalGuests());
            record.setNetAmount(event.netAmount());
            record.setDraftInvoiceNumber(draft.draftInvoiceNumber());
            record.setBookedInvoiceNumber(bookedInvoiceNumber);
            record.setSentByEmail(bookAndSend);
            record.setStatus(bookAndSend ? "SENT" : "DRAFT");
            YouthIslandInvoiceRecord saved = em.merge(record);
            tx.commit();
            event.existingRecord(saved);
        } catch (RuntimeException e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        }
    }

    private BatchCandidate buildBatch(EntityManager em, LocalDate periodStart, LocalDate periodEnd,
                                      EconomicCustomer customer, BigDecimal discountPercentage) {
        LocalDate invoiceDate = LocalDate.now(BUSINESS_ZONE);
        List<YouthIslandBooking> bookings = em.createQuery("""
                        SELECT DISTINCT booking
                        FROM YouthIslandBooking booking
                        LEFT JOIN FETCH booking.items item
                        WHERE booking.eventDate >= :periodStart
                          AND booking.eventDate < :periodEnd
                        ORDER BY booking.eventDate ASC, booking.id ASC
                        """, YouthIslandBooking.class)
                .setParameter("periodStart", periodStart)
                .setParameter("periodEnd", periodEnd)
                .getResultList();

        Set<Long> bookingIds = bookings.stream()
                .map(YouthIslandBooking::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, YouthIslandInvoiceRecord> recordsByBooking = findRecords(em, bookingIds, customer.getCustomerNumber());

        Set<String> productNumbers = bookings.stream()
                .flatMap(booking -> booking.getItems().stream())
                .map(YouthIslandBookingItem::getProductNumber)
                .filter(productNumber -> productNumber != null && !productNumber.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, Product> productsByNumber = findProducts(em, productNumbers);

        BatchCandidate batch = new BatchCandidate(periodStart, periodEnd, invoiceDate, customer);
        for (YouthIslandBooking booking : bookings) {
            EventCandidate event = new EventCandidate(booking, recordsByBooking.get(booking.getId()), discountPercentage);
            for (YouthIslandBookingItem item : booking.getItems().stream()
                    .sorted(Comparator.comparing(YouthIslandBookingItem::getServingTime))
                    .toList()) {
                Product product = productsByNumber.get(item.getProductNumber());
                if (product == null) {
                    throw new ApiException(400, "Missing available e-conomic product " + item.getProductNumber());
                }
                event.addLine(item, money(product.getPrice()));
            }
            batch.addEvent(event);
        }
        return batch;
    }

    private Map<String, Product> findProducts(EntityManager em, Set<String> productNumbers) {
        if (productNumbers.isEmpty()) return Map.of();

        List<Product> products = em.createQuery("""
                        SELECT product
                        FROM Product product
                        WHERE product.economicProductNumber IN :productNumbers
                          AND product.economicAvailable = true
                        """, Product.class)
                .setParameter("productNumbers", productNumbers)
                .getResultList();

        Map<String, Product> byNumber = new LinkedHashMap<>();
        for (Product product : products) {
            if (!Double.isFinite(product.getPrice()) || product.getPrice() < 0) {
                throw new ApiException(400, "Product " + product.getEconomicProductNumber() + " needs a valid sales price");
            }
            byNumber.put(product.getEconomicProductNumber(), product);
        }
        return byNumber;
    }

    private EconomicCustomer requireCustomer(EntityManager em, Integer customerNumber) {
        if (customerNumber != null) {
            EconomicCustomer customer = em.createQuery("""
                            SELECT customer
                            FROM EconomicCustomer customer
                            WHERE customer.customerNumber = :customerNumber
                              AND customer.economicAvailable = true
                            """, EconomicCustomer.class)
                    .setParameter("customerNumber", customerNumber)
                    .getResultStream()
                    .findFirst()
                    .orElse(null);
            if (customer == null || !isYouthIslandEventCustomer(customer)) {
                throw new ApiException(400, "Choose an available Ungdomsøen event e-conomic customer for Youth Island invoicing");
            }
            return customer;
        }

        List<EconomicCustomer> matches = em.createQuery("""
                        SELECT customer
                        FROM EconomicCustomer customer
                        WHERE customer.economicAvailable = true
                          AND lower(customer.name) LIKE :event
                          AND (
                            lower(customer.name) LIKE :ungdom
                            OR lower(customer.name) LIKE :ungdomAscii
                            OR lower(customer.name) LIKE :youthIsland
                          )
                        ORDER BY
                          CASE
                            WHEN lower(trim(customer.name)) = :standardExact THEN 0
                            WHEN lower(trim(customer.name)) = :legacyStandardExact THEN 1
                            ELSE 2
                          END,
                          customer.customerNumber ASC
                        """, EconomicCustomer.class)
                .setParameter("standardExact", STANDARD_EVENT_CUSTOMER_NAME)
                .setParameter("legacyStandardExact", LEGACY_STANDARD_EVENT_CUSTOMER_NAME)
                .setParameter("event", "%event%")
                .setParameter("ungdom", "%ungdomsø%")
                .setParameter("ungdomAscii", "%ungdomso%")
                .setParameter("youthIsland", "%youth island%")
                .getResultList();

        if (matches.isEmpty()) {
            throw new ApiException(400, "Choose the Ungdomsøen Event #1 e-conomic customer before previewing invoices");
        }
        return matches.get(0);
    }

    private boolean isYouthIslandEventCustomer(EconomicCustomer customer) {
        String name = customer.getName() == null ? "" : customer.getName().trim().toLowerCase();
        boolean youthIsland = name.contains("ungdomsø") || name.contains("ungdomso") || name.contains("youth island");
        return youthIsland && name.contains("event");
    }

    private Map<Long, YouthIslandInvoiceRecord> findRecords(EntityManager em, Set<Long> bookingIds, Integer customerNumber) {
        if (bookingIds.isEmpty()) return Map.of();

        List<YouthIslandInvoiceRecord> records = em.createQuery("""
                        SELECT record
                        FROM YouthIslandInvoiceRecord record
                        WHERE record.bookingId IN :bookingIds
                          AND record.economicCustomerNumber = :customerNumber
                        """, YouthIslandInvoiceRecord.class)
                .setParameter("bookingIds", bookingIds)
                .setParameter("customerNumber", customerNumber)
                .getResultList();

        Map<Long, YouthIslandInvoiceRecord> byBooking = new LinkedHashMap<>();
        for (YouthIslandInvoiceRecord record : records) {
            byBooking.put(record.getBookingId(), record);
        }
        return byBooking;
    }

    private LocalDate parseStartDate(String startDate) {
        if (startDate == null || startDate.isBlank()) return LocalDate.now(BUSINESS_ZONE);
        try {
            return LocalDate.parse(startDate.trim());
        } catch (DateTimeParseException e) {
            throw new ApiException(400, "startDate must use YYYY-MM-DD");
        }
    }

    private BigDecimal money(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal eventDiscount(boolean applyEventDiscount) {
        return applyEventDiscount ? EVENT_DISCOUNT_PERCENTAGE : BigDecimal.ZERO;
    }

    private static final class BatchCandidate {
        private final LocalDate periodStart;
        private final LocalDate periodEnd;
        private final LocalDate invoiceDate;
        private final EconomicCustomer customer;
        private final List<EventCandidate> events = new ArrayList<>();
        private String error;

        private BatchCandidate(LocalDate periodStart, LocalDate periodEnd, LocalDate invoiceDate, EconomicCustomer customer) {
            this.periodStart = Objects.requireNonNull(periodStart);
            this.periodEnd = Objects.requireNonNull(periodEnd);
            this.invoiceDate = Objects.requireNonNull(invoiceDate);
            this.customer = Objects.requireNonNull(customer);
        }

        private void addEvent(EventCandidate event) { events.add(event); }
        private void error(String error) { this.error = error; }
        private List<EventCandidate> events() { return events; }
        private LocalDate periodStart() { return periodStart; }
        private LocalDate periodEnd() { return periodEnd; }
        private LocalDate invoiceDate() { return invoiceDate; }
        private Integer customerNumber() { return customer.getCustomerNumber(); }
        private String customerName() { return customer.getName() == null || customer.getName().isBlank() ? "Ungdomsøen Event #1" : customer.getName(); }

        private YouthIslandInvoicePreviewDTO toDto() {
            List<YouthIslandInvoiceEventDTO> invoiceDtos = events.stream().map(EventCandidate::toDto).toList();
            List<YouthIslandInvoiceLineDTO> allLines = events.stream().flatMap(event -> event.lines().stream()).toList();
            int invoiceCount = (int) events.stream().filter(event -> !event.alreadyInvoiced()).count();
            return new YouthIslandInvoicePreviewDTO(
                    periodStart,
                    periodEnd,
                    invoiceDate,
                    customerNumber(),
                    customerName(),
                    events.size(),
                    invoiceCount,
                    allLines.size(),
                    allLines.stream().mapToInt(YouthIslandInvoiceLineDTO::getGuestCount).sum(),
                    allLines.stream().map(YouthIslandInvoiceLineDTO::getNetAmount).reduce(BigDecimal.ZERO, BigDecimal::add),
                    events.stream().allMatch(EventCandidate::alreadyInvoiced) && !events.isEmpty(),
                    null,
                    null,
                    null,
                    error,
                    allLines,
                    invoiceDtos
            );
        }
    }

    private static final class EventCandidate {
        private final YouthIslandBooking booking;
        private final List<YouthIslandInvoiceLineDTO> lines = new ArrayList<>();
        private final BigDecimal discountPercentage;
        private YouthIslandInvoiceRecord existingRecord;
        private String error;

        private EventCandidate(YouthIslandBooking booking, YouthIslandInvoiceRecord existingRecord, BigDecimal discountPercentage) {
            this.booking = Objects.requireNonNull(booking);
            this.existingRecord = existingRecord;
            this.discountPercentage = Objects.requireNonNull(discountPercentage);
        }

        private void addLine(YouthIslandBookingItem item, BigDecimal unitNetPrice) {
            BigDecimal quantity = BigDecimal.valueOf(item.getGuestCount());
            BigDecimal lineNetAmount = quantity.multiply(unitNetPrice)
                    .multiply(ONE_HUNDRED.subtract(discountPercentage))
                    .divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
            lines.add(new YouthIslandInvoiceLineDTO(
                    booking.getId(),
                    booking.getEventDate(),
                    item.getServingTime(),
                    booking.getSpectraReservationNumber(),
                    item.getProductNumber(),
                    item.getProductName(),
                    item.getRoom(),
                    item.getGuestCount(),
                    unitNetPrice,
                    discountPercentage,
                    lineNetAmount
            ));
        }

        private List<EconomicInvoiceClient.InvoiceLine> economicLines() {
            return lines.stream()
                    .map(line -> new EconomicInvoiceClient.InvoiceLine(
                            line.getProductNumber(),
                            invoiceLineDescription(line),
                            BigDecimal.valueOf(line.getGuestCount()),
                            line.getUnitNetPrice(),
                            line.getDiscountPercentage()
                    ))
                    .toList();
        }

        private String invoiceLineDescription(YouthIslandInvoiceLineDTO line) {
            String description = line.getProductName() + " " + line.getEventDate().format(DISPLAY_DATE)
                    + " kl. " + line.getServingTime().format(DISPLAY_TIME)
                    + " - " + line.getRoom();
            return description.length() <= 255 ? description : description.substring(0, 255);
        }

        private YouthIslandInvoiceEventDTO toDto() {
            return new YouthIslandInvoiceEventDTO(
                    bookingId(),
                    eventName(),
                    eventDate(),
                    spectraReservationNumber(),
                    lineCount(),
                    totalGuests(),
                    netAmount(),
                    alreadyInvoiced(),
                    existingRecord != null ? existingRecord.getStatus() : null,
                    existingRecord != null ? existingRecord.getDraftInvoiceNumber() : null,
                    existingRecord != null ? existingRecord.getBookedInvoiceNumber() : null,
                    error,
                    lines
            );
        }

        private Long bookingId() { return booking.getId(); }
        private String eventName() { return booking.getCustomerName() == null || booking.getCustomerName().isBlank() ? "Ungdomsøen event" : booking.getCustomerName(); }
        private LocalDate eventDate() { return booking.getEventDate(); }
        private String spectraReservationNumber() { return booking.getSpectraReservationNumber(); }
        private List<YouthIslandInvoiceLineDTO> lines() { return lines; }
        private YouthIslandInvoiceRecord existingRecord() { return existingRecord; }
        private void existingRecord(YouthIslandInvoiceRecord record) { this.existingRecord = record; }
        private void error(String error) { this.error = error; }
        private boolean alreadyInvoiced() { return existingRecord != null; }
        private int lineCount() { return lines.size(); }
        private int totalGuests() { return lines.stream().mapToInt(YouthIslandInvoiceLineDTO::getGuestCount).sum(); }
        private BigDecimal netAmount() {
            return lines.stream().map(YouthIslandInvoiceLineDTO::getNetAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }
}
