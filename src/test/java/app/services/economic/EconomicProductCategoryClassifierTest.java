package app.services.economic;

import app.dto.EconomicProductResponse;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EconomicProductCategoryClassifierTest {
    @ParameterizedTest
    @CsvSource({
            "Kaffe og juice, 1",
            "Social middag, 2",
            "Ekstra forret med fisk, 3",
            "Tilkob BMO, 3",
            "Dessert og kage, 4",
            "Rengoringsaftale, 42"
    })
    void classifiesNewEconomicProducts(String name, int expectedType) {
        EconomicProductResponse product = new EconomicProductResponse();
        product.setName(name);

        assertEquals(expectedType, EconomicProductCategoryClassifier.classify(product));
    }
}
