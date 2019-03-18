package org.springframework.cloud.contract.verifier.spec.openapi.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class XRequestPath {

    private String urlToHit;
    private XMethod method;
    private String status;
}
