package app.services.economic;

import app.dto.EconomicProductResponse;

import java.util.Locale;

public final class EconomicProductCategoryClassifier {
    private static final int DRINKS = 1;
    private static final int MAINS = 2;
    private static final int SIDES = 3;
    private static final int SWEETS = 4;
    private static final int CLEANING = 42;

    private EconomicProductCategoryClassifier() {
    }

    public static int classify(EconomicProductResponse product) {
        String searchableText = normalize((product.getName() == null ? "" : product.getName()) + " "
                + (product.getDescription() == null ? "" : product.getDescription()));

        if (containsAny(searchableText, "rengoring", "cleaning", "cleaning plan")) {
            return CLEANING;
        }
        if (containsAny(searchableText, "kaffe", "coffee", "juice", "sodavand", "lemonade",
                "drikke", "beverage", "kildevand", "mineralvand", "water", "vin ", "wine", "beer")) {
            return DRINKS;
        }
        if (containsAny(searchableText, "dessert", "kage", "cake", "frugt", "fruit", "snurr",
                "snack", "mysli", "muesli", "cookie", "chokolade", "chocolate")) {
            return SWEETS;
        }
        if (containsAny(searchableText, "tilkob", "tillaeg", "forret", "side", "salat")) {
            return SIDES;
        }
        return MAINS;
    }

    private static boolean containsAny(String value, String... terms) {
        for (String term : terms) {
            if (value.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT)
                .replace('ø', 'o')
                .replace("æ", "ae")
                .replace('å', 'a');
    }
}
