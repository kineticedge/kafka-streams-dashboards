package io.kineticedge.ksd.common.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

import java.math.BigDecimal;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "$type")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TaxRule(String postalCode, BigDecimal rate) {}
