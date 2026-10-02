package app.services.economic;

import app.dto.EconomicProductListResponse;
import app.dto.EconomicProductResponse;
import app.dto.ProductDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EconomicProductCatalogServiceTest {
    @Test
    void cachesProductsInMemoryAndFiltersBarredProducts() {
        StubProductClient client = new StubProductClient();
        EconomicProductCatalogService service = new EconomicProductCatalogService(client, Duration.ofMinutes(5));

        List<ProductDTO> first = service.getAvailableProducts();
        List<ProductDTO> second = service.getAvailableProducts();

        assertEquals(1, first.size());
        assertEquals("FOOD-1", first.get(0).getEconomicProductNumber());
        assertEquals("FOOD-1", service.getByProductNumber("FOOD-1").getEconomicProductNumber());
        assertEquals(2026, first.get(0).getEconomicLastUpdated().getYear());
        assertEquals(first, second);
        assertEquals(1, client.calls);
    }

    private static class StubProductClient extends EconomicProductClient {
        private int calls;

        StubProductClient() {
            super(HttpClient.newHttpClient(), "http://localhost", "app-secret", "agreement-token");
        }

        @Override
        public EconomicProductListResponse listProducts(int pageSize, int skipPages) {
            calls++;
            EconomicProductListResponse response = new EconomicProductListResponse();
            if (skipPages > 0) return response;

            EconomicProductResponse available = product("FOOD-1", false);
            EconomicProductResponse barred = product("OLD-1", true);
            response.setCollection(List.of(available, barred));
            return response;
        }

        private EconomicProductResponse product(String number, boolean barred) {
            EconomicProductResponse product = new EconomicProductResponse();
            product.setProductNumber(number);
            product.setName(number);
            product.setDescription("Description");
            product.setSalesPrice(BigDecimal.valueOf(125));
            product.setBarred(barred);
            product.setLastUpdated(OffsetDateTime.parse("2026-05-01T10:15:30+02:00"));
            product.setSelf("http://localhost/products/" + number);
            return product;
        }
    }
}
