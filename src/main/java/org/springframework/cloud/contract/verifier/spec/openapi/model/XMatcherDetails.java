package org.springframework.cloud.contract.verifier.spec.openapi.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class XMatcherDetails {

    private String jsonPath;
    private String name;
    private String type;
    private String value;
    private String predefined;

}
