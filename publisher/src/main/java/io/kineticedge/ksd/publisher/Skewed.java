package io.kineticedge.ksd.publisher;

import java.math.BigDecimal;

public record Skewed(String attributeName, String attributeValue, BigDecimal percentage) {
}
