package org.springframework.cloud.contract.verifier.spec.openapi.helper;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.contract.verifier.spec.openapi.model.*;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;


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

    private static final Predicate<Object> isValidObject = s -> !ObjectUtils.isEmpty(s);
    private static final Predicate<String> isValidString = s -> !ObjectUtils.isEmpty(s);

    private XContractHelper() {}

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
        Map<String,Object> body = (LinkedHashMap<String,Object>)data.get(BODY);
        xContract.setXRequestBody(body);
        //validate if matcher is available
        if(xContract.getXRequestMatcher() == null ){
            xContract.setXRequestMatcher(XRequestMatcher.builder()
                    .build());
        }
        extractRequestMatcherDetails(data, xContract);
    }


    private static Predicate isPathParam =  map -> isPathParam((Map<String,String>) map);
    private static Predicate isHeaderParam =  map -> isHeaderParam((Map<String,String>) map);
    private static Predicate isRequestParam =  map -> isRequestParam((Map<String,String>) map);
    private static Predicate isBodyParam = map -> isBodyParam((Map<String,String>) map);

    private static boolean isPathParam(Map<String,String> map){
        String inType = map.get(IN);
        return inType !=null && !inType.trim().isEmpty() && inType.equals(PATH);
    }

    private static boolean isBodyParam(Map<String,String> map){
        String inType = map.get(IN);
        return inType !=null && !inType.trim().isEmpty() && inType.equals(BODY);
    }

    private static boolean isHeaderParam(Map<String,String> map){
        String inType = map.get(IN);
        return inType !=null && !inType.trim().isEmpty() && inType.equals(HEADER);
    }

    private static boolean isRequestParam(Map<String,String> map){
        String inType = map.get(IN);
        return inType !=null && !inType.trim().isEmpty() && inType.equals(QUERY);
    }


    public static Tuple<String,XMatcherDetails> getXMatcherDetails(LinkedHashMap<String,String> data){
        String name = getValue(data, NAME);
        String path = getValue(data, PATH);
        String type = getValue(data, TYPE);
        String predefined = getValue(data, PREDEFINED);
        String value = getValue(data, VALUE);

        String key = isValidString.test(name) ? name :  path;

        return new Tuple<>(key,XMatcherDetails.builder()
                .name(name)
                .jsonPath(path)
                .type(type)
                .predefined(predefined)
                .value(value)
                .build());
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
        extractRequestMatcherDetails(params, xContract);
    }

    private static void extractRequestMatcherDetails(LinkedHashMap<String, Object> params, XContract xContract) {
        //An X-Contract details can set in any place within body , parameters , header ,cookie

        xContract.getXRequestMatcher().addBodyMatchers(collectMatcherDetails(params, BODY, isBodyParam));
        xContract.getXRequestMatcher().addBodyMatchers(collectMatcherDetails(params, PARAMATERS, isBodyParam));
        xContract.getXRequestMatcher().addBodyMatchers(collectMatcherDetails(params, HEADER, isBodyParam));


        xContract.getXRequestMatcher().addPathParamMatchers(collectMatcherDetails(params, BODY, isPathParam));
        xContract.getXRequestMatcher().addPathParamMatchers(collectMatcherDetails(params, PARAMATERS, isPathParam));
        xContract.getXRequestMatcher().addPathParamMatchers(collectMatcherDetails(params, HEADER, isPathParam));

        xContract.getXRequestMatcher().addRequestParamMatchers(collectMatcherDetails(params, BODY, isRequestParam));
        xContract.getXRequestMatcher().addRequestParamMatchers(collectMatcherDetails(params, PARAMATERS, isRequestParam));
        xContract.getXRequestMatcher().addRequestParamMatchers(collectMatcherDetails(params, HEADER, isRequestParam));

        xContract.getXRequestMatcher().addHeaderMatchers(collectMatcherDetails(params, BODY, isHeaderParam));
        xContract.getXRequestMatcher().addHeaderMatchers(collectMatcherDetails(params, PARAMATERS, isHeaderParam));
        xContract.getXRequestMatcher().addHeaderMatchers(collectMatcherDetails(params, HEADER, isHeaderParam));
    }



    private static Stream isRequestMatcherAvailable(LinkedHashMap<String, Object> params, String paramaters){
        LinkedHashMap<String, Object> paramMatcher = (LinkedHashMap<String, Object>) params.get(MATCHERS);
        if(paramMatcher !=null) {
            Object data = paramMatcher.get(paramaters);
            if (data instanceof List) {
                return ((List) data).stream();
            } else {
                return Stream.empty();
            }
        } else{
            return Stream.empty();
        }
    }

    private static Map<String,XMatcherDetails> collectMatcherDetails(LinkedHashMap<String, Object> params, String paramaters, Predicate predicate) {
        return (Map<String,XMatcherDetails>) isRequestMatcherAvailable(params,paramaters)
                .filter(predicate)
                .map(data1 -> XContractHelper.getXMatcherDetails((LinkedHashMap<String, String>) data1))
                .collect(Collectors.toMap(t -> ((Tuple<String,XMatcherDetails>) t).getA() , t -> ((Tuple<String,XMatcherDetails>) t).getB() ));
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

        xContract.getXResponseMatcher().addBodyMatchers(collectMatcherDetails(data, BODY, isBodyParam));
        xContract.getXResponseMatcher().addBodyMatchers(collectMatcherDetails(data, HEADER, isBodyParam));

        xContract.getXResponseMatcher().addHeaderMatchers(collectMatcherDetails(data, BODY, isHeaderParam));
        xContract.getXResponseMatcher().addHeaderMatchers(collectMatcherDetails(data, HEADER, isHeaderParam));
    }
}
