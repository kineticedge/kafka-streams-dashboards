package io.kineticedge.ksd.common.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "$type")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Store(
        String storeId,
        String name,
        String city,
        String state,
        String postalCode
) {

  public Store(String storeId, String name, String city, String state, String postalCode) {
    this.storeId = storeId;
    this.name = name;
    this.city = city;
    this.state = state;
    this.postalCode = postalCode;
  }

}
