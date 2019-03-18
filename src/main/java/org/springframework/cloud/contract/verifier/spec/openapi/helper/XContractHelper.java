package org.springframework.cloud.contract.verifier.spec.openapi.helper;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.contract.verifier.spec.openapi.model.*;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;


@Slf4j
public class XContractHelper {

    public static final String X_CONTRACTS = "x-contracts";
    private static final String NAME = "name";
    private static final String PRIORITY = "priority";
    private static final String PATH = "path";
    private static final String TYPE = "type";
    private static final String PREDEFINED = "predefined";
    private static final String IN = "in";
    private static final String VALUE = "value";
    public static final String CONTRACT_ID = "contractId";
    private static final String BODY = "body";
    private static final String MATCHERS = "matchers";
    private static final String PARAMATERS = "paramaters";
    private static final String HEADER = "header";
    private static final String QUERY = "query";

    public static final Predicate<Operation> isRequestParameterAvailable = o -> o.getParameters()!=null;
    public static final Predicate<Operation> isRequestBodyAvailable = o -> o.getRequestBody()!=null;
    public static final Predicate<Operation> isResponseBodyAvailable = o -> o.getResponses()!=null;
    public static final Predicate<Parameter> isExtensionAvailableForRequestParam = p -> p.getExtensions()!=null && p.getExtensions().get(X_CONTRACTS)!=null;
    public static final Predicate<RequestBody> isExtensionAvailableForRequestbody = b -> b.getExtensions()!=null && b.getExtensions().get(X_CONTRACTS)!=null;
    public static final Predicate<Map.Entry<String, ApiResponse>> isExtensionAvailableForResponseValue = r -> r.getValue().getExtensions() !=null && r.getValue().getExtensions().get(X_CONTRACTS) !=null;
    public static final Predicate<ApiResponses> isResponseEntrySetAvailable = apiResponses ->  apiResponses.entrySet()!=null;
    public static final Predicate<Operation> isExtensionAvailableInOperation= o -> o.getExtensions() !=null && o.getExtensions().get(X_CONTRACTS) != null;
    private static final Predicate<Object> isValidObject = s -> (s!=null && !s.toString().trim().isEmpty());
    private static final Predicate<String> isValidString = s -> (s!=null && !s.trim().isEmpty());



    @SneakyThrows
    public static XContract fromLinkedHashMap(LinkedHashMap<String,Object> data){
        Object priority = data.get(PRIORITY);
        String contractId = data.get(CONTRACT_ID).toString();
        String name = data.get(NAME).toString();


        return XContract.builder()
                .contractId(isValidString.test(contractId) ?  contractId : "")
                .name(isValidString.test(name) ?  name : "")
                .priority(Integer.parseInt(isValidObject.test(priority) ?  priority.toString() : "1"))
                .build();
    }



    @SneakyThrows
    public static void fromLinkedHashMapContractBody(LinkedHashMap<String,Object> data, Map<String,XContract> existingContract){
        String xContractId = data.get(CONTRACT_ID).toString();
        XContract xContract = existingContract.get(xContractId);
        xContract.setXRequestBody((LinkedHashMap<String,Object>)data.get(BODY));
        //validate if matcher is available
        if(xContract.getXRequestMatcher() == null ){
            xContract.setXRequestMatcher(XRequestMatcher.builder()
                    .build());
        }
        List<XMatcherDetails> xBodyMatcherDetails = (List<XMatcherDetails>)((List) ((LinkedHashMap<String, Object>) data.get(MATCHERS)).get(BODY)).stream()
                .filter(isBodyParam)
                .map(data1 -> XContractHelper.getXMatcherDetails((LinkedHashMap<String, String>) data1))
                .collect(Collectors.toList());
        xContract.getXRequestMatcher().addPathParamMatchers(collectMatcherDetails(data, BODY, isPathParam));
        xContract.getXRequestMatcher().addRequestParamMatchers(collectMatcherDetails(data, BODY, isRequestParam));
        xContract.getXRequestMatcher().addHeaderMatchers(collectMatcherDetails(data, BODY, isHeaderParam));
        xContract.getXRequestMatcher().setXbody(xBodyMatcherDetails);
    }


    public static Predicate isPathParam =  map -> isPathParam((Map<String,String>) map);
    public static Predicate isHeaderParam =  map -> isHeaderParam((Map<String,String>) map);
    public static Predicate isRequestParam =  map -> isRequestParam((Map<String,String>) map);
    public static Predicate isBodyParam = isPathParam.and(isHeaderParam).and(isRequestParam).negate();

    private static boolean isPathParam(Map<String,String> map){
        String inType = map.get(IN);
        return inType !=null && !inType.trim().isEmpty() && inType.equals(PATH);
    }

    private static boolean isHeaderParam(Map<String,String> map){
        String inType = map.get(IN);
        return inType !=null && !inType.trim().isEmpty() && inType.equals(HEADER);
    }

    private static boolean isRequestParam(Map<String,String> map){
        String inType = map.get(IN);
        return inType !=null && !inType.trim().isEmpty() && inType.equals(QUERY);
    }


    public static XMatcherDetails getXMatcherDetails(LinkedHashMap<String,String> data){
        String name = getValue(data, NAME);
        String path = getValue(data, PATH);
        String type = getValue(data, TYPE);
        String predefined = getValue(data, PREDEFINED);
        String value = getValue(data, VALUE);
        return  XMatcherDetails.builder()
                .name(name)
                .jsonPath(path)
                .type(type)
                .predefined(predefined)
                .value(value)
                .build();
    }

    private static String getValue(LinkedHashMap<String, String> data, String name) {
        return (data.get(name) != null) ? data.get(name) : "";
    }

    public static void fromLinkedHashMapContractParams(LinkedHashMap<String, Object> params, HashMap<String, XContract> xContracts) {
        String xContractId = params.get(CONTRACT_ID).toString();
        XContract xContract = xContracts.get(xContractId);
        if(xContract.getXRequestMatcher() == null ){
            xContract.setXRequestMatcher(XRequestMatcher.builder()
                    .build());
        }
        xContract.getXRequestMatcher().addPathParamMatchers(collectMatcherDetails(params, PARAMATERS, isPathParam));
        xContract.getXRequestMatcher().addRequestParamMatchers(collectMatcherDetails(params, PARAMATERS, isRequestParam));
        xContract.getXRequestMatcher().addHeaderMatchers(collectMatcherDetails(params, PARAMATERS, isHeaderParam));
    }



    private static Set<XMatcherDetails> collectMatcherDetails(LinkedHashMap<String, Object> params, String paramaters, Predicate isRequestParam) {
        return (Set<XMatcherDetails>) ((List) ((LinkedHashMap<String, Object>) params.get(MATCHERS)).get(paramaters)).stream()
                .filter(isRequestParam)
                .map(data1 -> XContractHelper.getXMatcherDetails((LinkedHashMap<String, String>) data1))
                .collect(Collectors.toSet());
    }

    public static void fromLinkedHashMapContractResponseBody(LinkedHashMap<String, Object> data, HashMap<String, XContract> xContracts) {
        String xContractId = data.get(CONTRACT_ID).toString();
        XContract xContract = xContracts.get(xContractId);
        xContract.setXResponseBody((LinkedHashMap<String,Object>)data.get(BODY));
        //validate if matcher is available
        if(xContract.getXResponseMatcher() == null ){
            xContract.setXResponseMatcher(XResponseMatcher.builder()
                    .build());
        }

        List<XMatcherDetails> xMatcherDetails = (List<XMatcherDetails>)((List) ((LinkedHashMap<String, Object>) data.get(MATCHERS)).get(BODY)).stream()
                .map(data1 -> XContractHelper.getXMatcherDetails((LinkedHashMap<String, String>) data1))
                .collect(Collectors.toList());

        xContract.getXResponseMatcher().setXbody(xMatcherDetails);
        xContract.getXResponseMatcher().addHeaderMatchers(collectMatcherDetails(data, BODY, isHeaderParam));
    }
}
