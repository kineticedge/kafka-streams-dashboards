package io.kineticedge.ksd.common.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Zip {
    private String zip;
    private String city;
    private String state;
}
