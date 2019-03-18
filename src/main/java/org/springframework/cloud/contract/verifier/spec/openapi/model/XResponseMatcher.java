package org.springframework.cloud.contract.verifier.spec.openapi.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class XResponseMatcher extends XMatcher{

    private String statusCode;
}
