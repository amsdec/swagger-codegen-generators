package io.swagger.codegen.v3.generators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.codegen.v3.CodegenConstants;
import io.swagger.codegen.v3.CodegenContent;
import io.swagger.codegen.v3.CodegenOperation;
import io.swagger.codegen.v3.CodegenParameter;
import io.swagger.codegen.v3.generators.util.OpenAPIUtil;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.CookieParameter;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.PathParameter;
import io.swagger.v3.oas.models.parameters.QueryParameter;

public class OperationParameters {

    private static Logger LOGGER = LoggerFactory.getLogger(OperationParameters.class);

    private CodegenParameter bodyParam = null;

    private final List<CodegenParameter> allParams = new ArrayList<>();

    private final List<CodegenParameter> bodyParams = new ArrayList<>();

    private final List<CodegenParameter> pathParams = new ArrayList<>();

    private final List<CodegenParameter> queryParams = new ArrayList<>();

    private final List<CodegenParameter> headerParams = new ArrayList<>();

    private final List<CodegenParameter> cookieParams = new ArrayList<>();

    private final List<CodegenParameter> formParams = new ArrayList<>();

    private final List<CodegenParameter> requiredParams = new ArrayList<>();

    private final List<CodegenContent> codegenContents = new ArrayList<>();

    public void setBodyParam(final CodegenParameter bodyParam) {
        this.bodyParam = bodyParam;
    }

    public CodegenParameter getBodyParam() {
        return this.bodyParam;
    }

    public List<CodegenParameter> getAllParams() {
        return this.allParams;
    }

    public List<CodegenParameter> getBodyParams() {
        return this.bodyParams;
    }

    public List<CodegenParameter> getPathParams() {
        return this.pathParams;
    }

    public List<CodegenParameter> getQueryParams() {
        return this.queryParams;
    }

    public List<CodegenParameter> getHeaderParams() {
        return this.headerParams;
    }

    public List<CodegenParameter> getCookieParams() {
        return this.cookieParams;
    }

    public List<CodegenParameter> getFormParams() {
        return this.formParams;
    }

    public List<CodegenParameter> getRequiredParams() {
        return this.requiredParams;
    }

    public List<CodegenContent> getCodegenContents() {
        return this.codegenContents;
    }

    public void addAllParams(final CodegenParameter codegenParameter) {
        this.allParams.add(codegenParameter);
    }

    public void addBodyParams(final CodegenParameter codegenParameter) {
        this.bodyParams.add(codegenParameter);
    }

    public void addPathParams(final CodegenParameter codegenParameter) {
        this.pathParams.add(codegenParameter);
    }

    public void addQueryParams(final CodegenParameter codegenParameter) {
        this.queryParams.add(codegenParameter);
    }

    public void addHeaderParams(final CodegenParameter codegenParameter) {
        this.headerParams.add(codegenParameter);
    }

    public void addCookieParams(final CodegenParameter codegenParameter) {
        this.cookieParams.add(codegenParameter);
    }

    public void addFormParam(final CodegenParameter codegenParameter) {
        this.formParams.add(codegenParameter);
    }

    public void addRequiredParam(final CodegenParameter codegenParameter) {
        this.requiredParams.add(codegenParameter);
    }

    public void addCodegenContents(final CodegenContent codegenContent) {
        this.codegenContents.add(codegenContent);
    }

    public void addParameters(final Parameter parameter, final CodegenParameter codegenParameter) {
        this.allParams.add(codegenParameter);

        if (parameter instanceof QueryParameter || "query".equalsIgnoreCase(parameter.getIn())) {
            this.queryParams.add(codegenParameter.copy());
        } else if (parameter instanceof PathParameter || "path".equalsIgnoreCase(parameter.getIn())) {
            this.pathParams.add(codegenParameter.copy());
        } else if (parameter instanceof HeaderParameter || "header".equalsIgnoreCase(parameter.getIn())) {
            this.headerParams.add(codegenParameter.copy());
        } else if (parameter instanceof CookieParameter || "cookie".equalsIgnoreCase(parameter.getIn())) {
            this.cookieParams.add(codegenParameter.copy());
        }
        if (codegenParameter.required) {
            this.requiredParams.add(codegenParameter.copy());
        }
    }

    public void addHasMore(final CodegenOperation codegenOperation) {
        codegenOperation.allParams = addHasMore(this.allParams);
        codegenOperation.bodyParams = addHasMore(this.bodyParams);
        codegenOperation.pathParams = addHasMore(this.pathParams);
        codegenOperation.queryParams = addHasMore(this.queryParams);
        codegenOperation.headerParams = addHasMore(this.headerParams);
        codegenOperation.cookieParams = addHasMore(this.cookieParams);
        codegenOperation.formParams = addHasMore(this.formParams);
        codegenOperation.requiredParams = addHasMore(this.requiredParams);
    }

    private static final List<String> ELEMENTS = Arrays.asList("pdPTenantKey", "pdPTenantSecret", "tenantId");

    public void sortRequiredAllParams() {
        Collections.sort(this.allParams, (one, another) -> {
            final int oneIndex = ELEMENTS.indexOf(one.getParamName());
            final int anotherIndex = ELEMENTS.indexOf(another.getParamName());
            if (oneIndex == -1 && anotherIndex == -1) {
                // LOGGER.info("{} {} {}", one.getParamName(), one.getIsPathParam(), one.getIsQueryParam());
                // LOGGER.info("{} {} {}", another.getParamName(), another.getIsPathParam(), another.getIsQueryParam());
                // LOGGER.info(another.toString());
                if (Boolean.TRUE.equals(one.getIsPathParam()) && Boolean.TRUE.equals(!another.getIsPathParam())) {
                    return -1;
                }
                if (Boolean.TRUE.equals(!one.getIsPathParam()) && Boolean.TRUE.equals(another.getIsPathParam())) {
                    return 1;
                }
                return 0;
            }
            if (oneIndex != -1 && anotherIndex == -1) {
                return -1;
            }
            if (oneIndex == -1 && anotherIndex != -1) {
                return 1;
            }

            if (oneIndex != -1 && anotherIndex != -1) {
                return oneIndex < anotherIndex ? -1 : 1;
            }

            // if ("pdPTenantSecret".equals(one.getParamName())) {
            // return -1;
            // }
            // if ("tenantId".equals(one.getParamName())) {
            // return -1;
            // }
            // if (one.required == another.required) {
            // return 0;
            // }
            // if (one.required) {
            // return -1;
            // }
            return 1;
        });
    }

    public void parseNestedObjects(final String name, Schema schema, final Set<String> imports,
            final DefaultCodegenConfig codegenConfig, final OpenAPI openAPI) {
        schema = OpenAPIUtil.getRefSchemaIfExists(schema, openAPI);
        if (schema == null || !this.isObjectWithProperties(schema)) {
            return;
        }
        final Map<String, Schema> properties = schema.getProperties();
        for (final String key : properties.keySet()) {
            Schema property = properties.get(key);
            property = OpenAPIUtil.getRefSchemaIfExists(property, openAPI);
            boolean required;
            if (schema.getRequired() == null || schema.getRequired().isEmpty()) {
                required = false;
            } else {
                required = schema.getRequired().stream()
                        .anyMatch(propertyName -> key.equalsIgnoreCase(propertyName.toString()));
            }
            final String parameterName;
            if (property instanceof ArraySchema) {
                parameterName = String.format("%s[%s][]", name, key);
            } else {
                parameterName = String.format("%s[%s]", name, key);
            }
            if (this.isObjectWithProperties(property)) {
                this.parseNestedObjects(parameterName, property, imports, codegenConfig, openAPI);
                continue;
            }
            final Parameter queryParameter = new QueryParameter().name(parameterName).required(required)
                    .schema(property);
            final CodegenParameter codegenParameter = codegenConfig.fromParameter(queryParameter, imports);
            this.addParameters(queryParameter, codegenParameter);
        }
    }

    public static List<CodegenParameter> addHasMore(final List<CodegenParameter> codegenParameters) {
        if (codegenParameters == null || codegenParameters.isEmpty()) {
            return codegenParameters;
        }
        for (int i = 0; i < codegenParameters.size(); i++) {
            codegenParameters.get(i).secondaryParam = i > 0;
            codegenParameters.get(i).getVendorExtensions().put(CodegenConstants.HAS_MORE_EXT_NAME,
                    i < codegenParameters.size() - 1);
        }
        return codegenParameters;
    }

    private boolean isObjectWithProperties(final Schema schema) {
        return ("object".equalsIgnoreCase(schema.getType()) || schema.getType() == null)
                && schema.getProperties() != null && !schema.getProperties().isEmpty();
    }
}
