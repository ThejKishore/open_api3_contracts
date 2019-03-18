package org.springframework.cloud.contract.verifier.spec.openapi.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data(staticConstructor = "of")
@AllArgsConstructor
@Builder
public class Tuple<A,B> {
    A a;
    B b;
}
