package io.kineticedge.ksd.common.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "$type")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Product(
        String sku,
        String modelNumber,
        String description,
        BigDecimal price,
        Map<String, Object> attributes
) {

  public Product(String sku, String modelNumber, String description, BigDecimal price, Map<String, Object> attributes) {
    this.sku = Objects.requireNonNull(sku, "sku");
    this.modelNumber = Objects.requireNonNull(modelNumber, "modelNumber");
    this.description = description;
    this.price = Objects.requireNonNull(price, "price");
    this.attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
  }

}


