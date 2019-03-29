package org.springframework.cloud.contract.verifier.spec.openapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.parser.OpenAPIV3Parser;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.contract.verifier.spec.openapi.helper.XContractHelper;
import org.springframework.cloud.contract.verifier.spec.openapi.model.XContract;
import org.springframework.cloud.contract.verifier.spec.openapi.model.XMethod;
import org.springframework.cloud.contract.verifier.spec.openapi.model.XRequestPath;

import java.io.File;
import java.util.*;
import java.util.stream.Stream;

import static org.springframework.cloud.contract.verifier.spec.openapi.helper.XContractHelper.*;

@Slf4j
public class OpenApiConverterHelper {


    public static final OpenApiContractConverter INSTANCE = new OpenApiContractConverter();
    public static final String $_REF = "$ref";
    public static final String DEFAULT_BODY = "default_body";

    private ObjectMapper objectMapper = new ObjectMapper();
    private ObjectWriter jsonObjectWriter = objectMapper.writerWithDefaultPrettyPrinter();


    /**
     *
     * https://github.com/json-path/JsonPath
     * http://dius.github.io/java-faker/
     *
     * **/

    @SneakyThrows
    public OpenAPI fromFile(File file){
        return new OpenAPIV3Parser().read(file.getPath());
    }

    public HashMap<String, XContract> getStringXContractHashMap(OpenAPI spec) {
        HashMap<String,XContract> xContracts = getInitialXContract(spec);
        getXContractRequestBody(spec, xContracts);
        getXContractRequestParams(spec,xContracts);
        getXContractResponseBody(spec,xContracts);
        xContracts.entrySet().stream()
                .forEach(key -> setGenericContactDetails(spec, key.getKey(),key.getValue()));
        return xContracts;
    }



    @SneakyThrows
    private void setGenericContactDetails(OpenAPI spec, String key,XContract contract) {
        setXRequestPathToXContract(spec,key,contract);
        setXResponseToXContract(spec,key,contract);

        //  if Body has $ref try getting the json from #components/schemas/refId.
        //  Add one more key default body and add the json....
        if(contract.getXRequestBody() !=null && contract.getXRequestBody().get($_REF) !=null){
            String pojoKey = getPojoKey(contract.getXRequestBody());
            Schema data =  spec.getComponents().getSchemas().get(pojoKey);
            if(data !=null &&  data.getExample() !=null) {
                contract.getXRequestBody().put(DEFAULT_BODY, data.getExample().toString());
            }
        }

        if(contract.getXResponseBody() !=null && contract.getXResponseBody().get($_REF) !=null){
            String pojoKey = getPojoKey(contract.getXResponseBody());
            Schema data =  spec.getComponents().getSchemas().get(pojoKey);
            if(data !=null && data.getExample() !=null) {
                contract.getXResponseBody().put(DEFAULT_BODY, data.getExample().toString());
            }
        }

        log.info("xcontract  --- {}",jsonObjectWriter.writeValueAsString(contract));
    }

    private String getPojoKey(Map<String, Object> xResponseBody) {
        String pojoKey = xResponseBody.get($_REF).toString();
        int startIndex = pojoKey.lastIndexOf('/')+1 ;
        pojoKey = pojoKey.substring(startIndex);
        return pojoKey;
    }

    private void setXResponseToXContract(OpenAPI spec, String key,XContract contract) {
        XRequestPath xRequestPath = XRequestPath.builder().build();

        Optional found = spec.getPaths().entrySet().stream()
                .map(entry -> pathKey(entry, xRequestPath))
                .map(data -> data.getValue().readOperationsMap())
                .flatMap(operationMap -> operationMap.entrySet().stream())
                .map(operationEntry -> operationKey(operationEntry, xRequestPath))
                .map(operationentry -> operationentry.getValue())
                .filter(isResponseBodyAvailable)
                .map(operation -> operation.getResponses())
                .filter(isResponseEntrySetAvailable)
                .flatMap(responses -> responses.entrySet().stream())
                .map(data -> addCurrentStatus(data,xRequestPath))
                .filter(isExtensionAvailableForResponseValue)
                .map(respnse -> ((ArrayList) respnse.getValue().getExtensions().get(X_CONTRACTS)))
                .flatMap(data -> data.stream())
                .filter(data -> key.equals((((Map) data).get(CONTRACT_ID)).toString()))
                .findFirst();


        if(found.isPresent()){
            contract.getXRequestPath().setStatus(xRequestPath.getStatus());
        }
    }

    private Map.Entry<String, ApiResponse> addCurrentStatus(Map.Entry<String, ApiResponse> data,XRequestPath xRequestPath){
        xRequestPath.setStatus(data.getKey());
        return data;
    }


    private void setXRequestPathToXContract(OpenAPI spec, String key,XContract contract) {
        XRequestPath xRequestPath = XRequestPath.builder().build();

        Optional found = spec.getPaths().entrySet().stream()
                .map(entry -> pathKey(entry, xRequestPath))
                .map(data -> data.getValue().readOperationsMap())
                .flatMap(operationMap -> operationMap.entrySet().stream())
                .map(operationEntry -> operationKey(operationEntry, xRequestPath))
                .map(operationentry -> operationentry.getValue())
                .filter(isExtensionAvailableInOperation)
                .map(operation -> ((ArrayList) operation.getExtensions().get(X_CONTRACTS)))
                .flatMap(data -> data.stream())
                .filter(data -> key.equals((((Map) data).get(CONTRACT_ID)).toString()))
                .findFirst();
        if(found.isPresent()){
            contract.setXRequestPath(xRequestPath);
        }
    }


    private Map.Entry<String, PathItem> pathKey(Map.Entry<String, PathItem> data , XRequestPath value){
        value.setUrlToHit(data.getKey());
        return data;
    }

    private Map.Entry<PathItem.HttpMethod, Operation> operationKey(Map.Entry<PathItem.HttpMethod, Operation> data , XRequestPath value){
        value.setMethod(XMethod.valueOf(data.getKey().name()));
        return data;
    }

    /**
     * This method over the operation model and if request body is present
     * and extracts the contract details if present in the extension and set the
     * {@link XContract} instance appropriately.
     * @param spec spec {@link OpenAPI}
     * @param xContracts {@link XContract}
     */
    private void getXContractResponseBody(OpenAPI spec, HashMap<String, XContract> xContracts) {
        getOperationsFromAGivenSpec(spec)
                .filter(isResponseBodyAvailable)
                .map(operation -> operation.getResponses())
                .filter(isResponseEntrySetAvailable)
                .flatMap(apiResponses -> apiResponses.entrySet().stream())
                .filter(isExtensionAvailableForResponseValue)
                .map(respnse -> ((ArrayList) respnse.getValue().getExtensions().get(X_CONTRACTS)))
                .flatMap(responseContract -> responseContract.stream())
                .forEach(entry -> XContractHelper.fromLinkedHashMapContractResponseBody((LinkedHashMap<String,Object>)entry,xContracts));
    }

    /**
     * This method over the operation model and if request body is present
     * and extracts the contract details if present in the extension and set the
     * {@link XContract} instance appropriately.
     * @param spec spec {@link OpenAPI}
     * @param xContracts {@link XContract}
     */
    private void getXContractRequestParams(OpenAPI spec, HashMap<String, XContract> xContracts) {
        getOperationsFromAGivenSpec(spec)
                .filter(isRequestParameterAvailable)
                .flatMap(operation -> operation.getParameters().stream())
                .filter(isExtensionAvailableForRequestParam)
                .map(parameter -> (ArrayList)parameter.getExtensions().get(X_CONTRACTS))
                .flatMap(paramContracts -> paramContracts.stream())
                .forEach(entry -> XContractHelper.fromLinkedHashMapContractParams((LinkedHashMap<String,Object>)entry,xContracts));

    }

    /**
     * This method over the operation model and if request body is present
     * and extracts the contract details if present in the extension and set the
     * {@link XContract} instance appropriately.
     * @param spec spec {@link OpenAPI}
     * @param xContracts {@link XContract}
     */
    private void getXContractRequestBody(OpenAPI spec, HashMap<String, XContract> xContracts) {
        getOperationsFromAGivenSpec(spec)
                .filter(isRequestBodyAvailable)
                .map(operation -> operation.getRequestBody())
                .filter(isExtensionAvailableForRequestbody)
                .map(body -> (ArrayList)body.getExtensions().get(X_CONTRACTS))
                .flatMap(bodyContracts -> bodyContracts.stream())
                .forEach(entry -> XContractHelper.fromLinkedHashMapContractBody((LinkedHashMap<String,Object>)entry,xContracts));
    }

    /**
     * This method over the operation model and if request body is present
     * and extracts the contract details if present in the extension and set the
     * {@link XContract} instance appropriately.
     * @param spec {@link OpenAPI}
     * @return  xContracts {@link HashMap<String,XContract>}
     */
    public HashMap<String, XContract> getInitialXContract(OpenAPI spec) {
        HashMap<String,XContract> xContractMap = new HashMap<>();

         getOperationsFromAGivenSpec(spec)
                .filter(isExtensionAvailableInOperation)
                .map(operation -> ((ArrayList) operation.getExtensions().get(X_CONTRACTS)))
                .flatMap(data -> data.stream())
                .map(data -> XContractHelper.fromLinkedHashMap((LinkedHashMap<String, Object>) data))
                .forEach(data -> addXContractToMap(((XContract) data),xContractMap));

        return xContractMap;
    }

    private void addXContractToMap(XContract xContract,Map<String,XContract> xContractMap){
        xContractMap.putIfAbsent(xContract.getContractId(),xContract);
    }

    /**
     * Open the operation Stream from the OpenAPI .
     * @param spec {@link OpenAPI}
     * @return {@link Stream<Operation>}
     */
    private Stream<Operation>  getOperationsFromAGivenSpec(OpenAPI spec){
        //dont assign the stream to variable and consume the stream before returning
        return spec.getPaths().entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .map(entry -> entry.getValue().readOperations())
                .flatMap(operations -> operations.stream());
    }


}
