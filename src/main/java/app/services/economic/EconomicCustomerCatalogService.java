package app.services.economic;

import app.dto.EconomicCustomerListResponse;
import app.dto.EconomicCustomerResponse;
import app.exceptions.ApiException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class EconomicCustomerCatalogService {
    private static final int PAGE_SIZE = 1000;
    private static final int MAX_PAGES = 100;
    private static final Duration DEFAULT_CACHE_DURATION = Duration.ofMinutes(5);

    private final EconomicCustomerClient client;
    private final Duration cacheDuration;
    private Instant cacheExpiresAt = Instant.EPOCH;
    private List<EconomicCustomerResponse> cachedCustomers = List.of();

    public EconomicCustomerCatalogService() {
        this(new EconomicCustomerClient(), DEFAULT_CACHE_DURATION);
    }

    EconomicCustomerCatalogService(EconomicCustomerClient client, Duration cacheDuration) {
        this.client = client;
        this.cacheDuration = cacheDuration;
    }

    public synchronized List<EconomicCustomerResponse> getCustomers() {
        if (!Instant.now().isBefore(cacheExpiresAt)) {
            cachedCustomers = fetchAllCustomers().stream()
                    .filter(customer -> !Boolean.TRUE.equals(customer.getBarred()))
                    .toList();
            cacheExpiresAt = Instant.now().plus(cacheDuration);
        }
        return new ArrayList<>(cachedCustomers);
    }

    private List<EconomicCustomerResponse> fetchAllCustomers() {
        List<EconomicCustomerResponse> customers = new ArrayList<>();
        for (int skipPages = 0; skipPages < MAX_PAGES; skipPages++) {
            EconomicCustomerListResponse response = client.listCustomers(PAGE_SIZE, skipPages);
            List<EconomicCustomerResponse> page = response.getCollection();
            customers.addAll(page);
            if (page.size() < PAGE_SIZE) return customers;
        }
        throw new ApiException(502, "e-conomic customer catalog exceeded the supported page limit");
    }
}
