package io.kineticedge.ksd.common.domain;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "$type")
public class TaxRule {

    private String postalCode;
    private BigDecimal rate;

}
