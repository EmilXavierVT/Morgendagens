package app.services.economic;

import app.dto.EconomicProductListResponse;
import app.dto.EconomicProductResponse;
import app.dto.ProductDTO;
import app.exceptions.ApiException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class EconomicProductCatalogService {
    private static final int PAGE_SIZE = 1000;
    private static final int MAX_PAGES = 100;
    private static final Duration DEFAULT_CACHE_DURATION = Duration.ofMinutes(5);

    private final EconomicProductClient client;
    private final Duration cacheDuration;
    private Instant cacheExpiresAt = Instant.EPOCH;
    private List<ProductDTO> cachedProducts = List.of();

    public EconomicProductCatalogService() {
        this(new EconomicProductClient(), DEFAULT_CACHE_DURATION);
    }

    EconomicProductCatalogService(EconomicProductClient client, Duration cacheDuration) {
        this.client = client;
        this.cacheDuration = cacheDuration;
    }

    public synchronized List<ProductDTO> getAvailableProducts() {
        if (!Instant.now().isBefore(cacheExpiresAt)) {
            cachedProducts = fetchAllProducts().stream()
                    .filter(product -> !Boolean.TRUE.equals(product.getBarred()))
                    .map(this::toDto)
                    .toList();
            cacheExpiresAt = Instant.now().plus(cacheDuration);
        }
        return new ArrayList<>(cachedProducts);
    }

    public ProductDTO getByProductNumber(String productNumber) {
        if (productNumber == null || productNumber.isBlank()) {
            throw new ApiException(400, "Product number is required");
        }
        return getAvailableProducts().stream()
                .filter(product -> productNumber.equals(product.getEconomicProductNumber()))
                .findFirst()
                .orElseThrow(() -> new ApiException(404, "Product not found in e-conomic"));
    }

    private List<EconomicProductResponse> fetchAllProducts() {
        List<EconomicProductResponse> products = new ArrayList<>();
        for (int skipPages = 0; skipPages < MAX_PAGES; skipPages++) {
            EconomicProductListResponse response = client.listProducts(PAGE_SIZE, skipPages);
            List<EconomicProductResponse> page = response.getCollection();
            products.addAll(page);
            if (page.size() < PAGE_SIZE) return products;
        }
        throw new ApiException(502, "e-conomic product catalog exceeded the supported page limit");
    }

    private ProductDTO toDto(EconomicProductResponse response) {
        ProductDTO dto = new ProductDTO();
        dto.setName(response.getName());
        dto.setDescription(response.getDescription());
        dto.setPrice(response.getSalesPrice() == null ? 0.0 : response.getSalesPrice().doubleValue());
        dto.setType(EconomicProductCategoryClassifier.classify(response));
        dto.setEconomicProductNumber(response.getProductNumber());
        dto.setEconomicAvailable(!Boolean.TRUE.equals(response.getBarred()));
        dto.setEconomicLastUpdated(response.getLastUpdated());
        if (response.getProductGroup() != null) {
            dto.setEconomicProductGroupNumber(response.getProductGroup().getProductGroupNumber());
            dto.setEconomicProductGroupName(response.getProductGroup().getName());
        }
        dto.setEconomicSelf(response.getSelf());
        dto.setProductInRequestIds(List.of());
        return dto;
    }

    public synchronized ProductDTO updateProduct(String productNumber, ProductDTO update) {
        EconomicProductResponse existing = fetchAllProducts().stream()
                .filter(product -> productNumber.equals(product.getProductNumber()))
                .findFirst()
                .orElseThrow(() -> new ApiException(404, "Product not found in e-conomic"));
        EconomicProductResponse updated = client.updateProduct(existing.getSelf(), update);
        cacheExpiresAt = Instant.EPOCH;
        cachedProducts = List.of();
        return toDto(updated);
    }
}
