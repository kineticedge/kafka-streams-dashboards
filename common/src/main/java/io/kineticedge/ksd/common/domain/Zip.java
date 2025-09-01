package io.kineticedge.ksd.common.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public record Zip(String zip, String city, String state) {
}
