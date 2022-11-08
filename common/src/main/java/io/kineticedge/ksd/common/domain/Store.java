package io.kineticedge.ksd.common.domain;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "$type")
public class Store {

    private String storeId;
    private String name;
    private String city;
    private String state;
    private String postalCode;

}
