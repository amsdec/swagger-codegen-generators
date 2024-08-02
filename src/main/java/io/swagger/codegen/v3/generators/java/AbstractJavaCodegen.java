package io.swagger.codegen.v3.generators.java;

import static io.swagger.codegen.v3.CodegenConstants.HAS_ENUMS_EXT_NAME;
import static io.swagger.codegen.v3.CodegenConstants.IS_ENUM_EXT_NAME;
import static io.swagger.codegen.v3.generators.features.NotNullAnnotationFeatures.NOT_NULL_JACKSON_ANNOTATION;
import static io.swagger.codegen.v3.generators.handlebars.ExtensionHelper.getBooleanValue;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jknack.handlebars.Handlebars;

import io.swagger.codegen.v3.CliOption;
import io.swagger.codegen.v3.CodegenArgument;
import io.swagger.codegen.v3.CodegenConstants;
import io.swagger.codegen.v3.CodegenModel;
import io.swagger.codegen.v3.CodegenOperation;
import io.swagger.codegen.v3.CodegenParameter;
import io.swagger.codegen.v3.CodegenProperty;
import io.swagger.codegen.v3.generators.DefaultCodegenConfig;
import io.swagger.codegen.v3.generators.features.NotNullAnnotationFeatures;
import io.swagger.codegen.v3.generators.handlebars.java.JavaHelper;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.parser.util.SchemaTypeUtil;

public abstract class AbstractJavaCodegen extends DefaultCodegenConfig {

    private static Logger LOGGER = LoggerFactory.getLogger(AbstractJavaCodegen.class);

    public static final String FULL_JAVA_UTIL = "fullJavaUtil";

    public static final String DEFAULT_LIBRARY = "<default>";

    public static final String DATE_LIBRARY = "dateLibrary";

    public static final String JAVA8_MODE = "java8";

    public static final String JAVA11_MODE = "java11";

    public static final String WITH_XML = "withXml";

    public static final String SUPPORT_JAVA6 = "supportJava6";

    public static final String ERROR_ON_UNKNOWN_ENUM = "errorOnUnknownEnum";

    public static final String CHECK_DUPLICATED_MODEL_NAME = "checkDuplicatedModelName";

    public static final String USE_NULLABLE_FOR_NOTNULL = "useNullableForNotNull";

    public static final String WIREMOCK_OPTION = "wiremock";

    public static final String JAKARTA = "jakarta";

    protected String dateLibrary = "joda";

    protected boolean java8Mode = false;

    protected boolean java11Mode = true;

    protected boolean withXml = false;

    protected String invokerPackage = "io.swagger";

    protected String groupId = "io.swagger";

    protected String artifactId = "swagger-java";

    protected String artifactVersion = "1.0.0";

    protected String artifactUrl = "https://github.com/swagger-api/swagger-codegen";

    protected String artifactDescription = "Swagger Java";

    protected String developerName = "Swagger";

    protected String developerEmail = "apiteam@swagger.io";

    protected String developerOrganization = "Swagger";

    protected String developerOrganizationUrl = "http://swagger.io";

    protected String scmConnection = "scm:git:git@github.com:swagger-api/swagger-codegen.git";

    protected String scmDeveloperConnection = "scm:git:git@github.com:swagger-api/swagger-codegen.git";

    protected String scmUrl = "https://github.com/swagger-api/swagger-codegen";

    protected String licenseName = "Unlicense";

    protected String licenseUrl = "http://unlicense.org";

    protected String projectFolder = "src" + File.separator + "main";

    protected String projectTestFolder = "src" + File.separator + "test";

    protected String sourceFolder = this.projectFolder + File.separator + "java";

    protected String testFolder = this.projectTestFolder + File.separator + "java";

    protected String localVariablePrefix = "";

    protected boolean fullJavaUtil;

    protected String javaUtilPrefix = "";

    protected Boolean serializableModel = false;

    protected boolean serializeBigDecimalAsString = false;

    protected String apiDocPath = "docs/";

    protected String modelDocPath = "docs/";

    protected boolean supportJava6 = false;

    protected boolean jakarta = false;

    private NotNullAnnotationFeatures notNullOption;

    protected boolean useNullableForNotNull = true;

    public AbstractJavaCodegen() {
        this.hideGenerationTimestamp = false;
        this.supportsInheritance = true;

        this.setReservedWordsLowerCase(Arrays.asList(
                // used as internal variables, can collide with parameter names
                "localVarPath", "localVarQueryParams", "localVarCollectionQueryParams", "localVarHeaderParams",
                "localVarFormParams", "localVarPostBody", "localVarAccepts", "localVarAccept", "localVarContentTypes",
                "localVarContentType", "localVarAuthNames", "localReturnType", "ApiClient", "ApiException",
                "ApiResponse", "Configuration", "StringUtil",

                // language reserved words
                "abstract", "continue", "for", "new", "switch", "assert", "default", "if", "package", "synchronized",
                "boolean", "do", "goto", "private", "this", "break", "double", "implements", "protected", "throw",
                "byte", "else", "import", "public", "throws", "case", "enum", "instanceof", "return", "transient",
                "catch", "extends", "int", "short", "try", "char", "final", "interface", "static", "void", "class",
                "finally", "long", "strictfp", "volatile", "const", "float", "native", "super", "while", "null"));

        this.languageSpecificPrimitives = new HashSet<>(Arrays.asList("String", "boolean", "Boolean", "Double",
                "Integer", "Long", "Float", "Object", "byte[]"));
        this.instantiationTypes.put("array", "ArrayList");
        this.instantiationTypes.put("map", "HashMap");
        this.typeMapping.put("date", "Date");
        this.typeMapping.put("file", "File");
        this.typeMapping.put("binary", "File");

        this.cliOptions.add(new CliOption(CodegenConstants.MODEL_PACKAGE, CodegenConstants.MODEL_PACKAGE_DESC));
        this.cliOptions.add(new CliOption(CodegenConstants.API_PACKAGE, CodegenConstants.API_PACKAGE_DESC));
        this.cliOptions.add(new CliOption(CodegenConstants.INVOKER_PACKAGE, CodegenConstants.INVOKER_PACKAGE_DESC));
        this.cliOptions.add(new CliOption(CodegenConstants.GROUP_ID, CodegenConstants.GROUP_ID_DESC));
        this.cliOptions.add(new CliOption(CodegenConstants.ARTIFACT_ID, CodegenConstants.ARTIFACT_ID_DESC));
        this.cliOptions.add(new CliOption(CodegenConstants.ARTIFACT_VERSION, CodegenConstants.ARTIFACT_VERSION_DESC));
        this.cliOptions.add(new CliOption(CodegenConstants.ARTIFACT_URL, CodegenConstants.ARTIFACT_URL_DESC));
        this.cliOptions
                .add(new CliOption(CodegenConstants.ARTIFACT_DESCRIPTION, CodegenConstants.ARTIFACT_DESCRIPTION_DESC));
        this.cliOptions.add(new CliOption(CodegenConstants.SCM_CONNECTION, CodegenConstants.SCM_CONNECTION_DESC));
        this.cliOptions.add(new CliOption(CodegenConstants.SCM_DEVELOPER_CONNECTION,
                CodegenConstants.SCM_DEVELOPER_CONNECTION_DESC));
        this.cliOptions.add(new CliOption(CodegenConstants.SCM_URL, CodegenConstants.SCM_URL_DESC));
        this.cliOptions.add(new CliOption(CodegenConstants.DEVELOPER_NAME, CodegenConstants.DEVELOPER_NAME_DESC));
        this.cliOptions.add(new CliOption(CodegenConstants.DEVELOPER_EMAIL, CodegenConstants.DEVELOPER_EMAIL_DESC));
        this.cliOptions.add(
                new CliOption(CodegenConstants.DEVELOPER_ORGANIZATION, CodegenConstants.DEVELOPER_ORGANIZATION_DESC));
        this.cliOptions.add(new CliOption(CodegenConstants.DEVELOPER_ORGANIZATION_URL,
                CodegenConstants.DEVELOPER_ORGANIZATION_URL_DESC));
        this.cliOptions.add(new CliOption(CodegenConstants.LICENSE_NAME, CodegenConstants.LICENSE_NAME_DESC));
        this.cliOptions.add(new CliOption(CodegenConstants.LICENSE_URL, CodegenConstants.LICENSE_URL_DESC));
        this.cliOptions.add(new CliOption(CodegenConstants.SOURCE_FOLDER, CodegenConstants.SOURCE_FOLDER_DESC));
        this.cliOptions.add(
                new CliOption(CodegenConstants.LOCAL_VARIABLE_PREFIX, CodegenConstants.LOCAL_VARIABLE_PREFIX_DESC));
        this.cliOptions.add(
                CliOption.newBoolean(CodegenConstants.SERIALIZABLE_MODEL, CodegenConstants.SERIALIZABLE_MODEL_DESC));
        this.cliOptions.add(CliOption.newBoolean(CodegenConstants.SERIALIZE_BIG_DECIMAL_AS_STRING,
                CodegenConstants.SERIALIZE_BIG_DECIMAL_AS_STRING_DESC));
        this.cliOptions.add(CliOption.newBoolean(FULL_JAVA_UTIL,
                "whether to use fully qualified name for classes under java.util. This option only works for Java API client"));
        this.cliOptions.add(new CliOption(CodegenConstants.HIDE_GENERATION_TIMESTAMP,
                CodegenConstants.HIDE_GENERATION_TIMESTAMP_DESC));
        this.cliOptions.add(CliOption.newBoolean(WITH_XML,
                "whether to include support for application/xml content type and include XML annotations in the model (works with libraries that provide support for JSON and XML)"));
        this.cliOptions.add(CliOption.newBoolean(CodegenConstants.USE_OAS2, CodegenConstants.USE_OAS2_DESC));
        // if (this instanceof NotNullAnnotationFeatures) {
        this.cliOptions.add(CliOption.newBoolean(NOT_NULL_JACKSON_ANNOTATION,
                "adds @JsonInclude(JsonInclude.Include.NON_NULL) annotation to model classes"));
        // }
        final CliOption dateLibrary = new CliOption(DATE_LIBRARY, "Option. Date library to use");
        final Map<String, String> dateOptions = new HashMap<>();
        dateOptions.put("java8",
                "Java 8 native JSR310 (preferred for jdk 1.8+) - note: this also sets \"" + JAVA8_MODE + "\" to true");
        dateOptions.put("java11",
                "Java 11 native JSR384 (preferred for jdk 11+) - note: this also sets \"" + JAVA11_MODE + "\" to true");
        dateOptions.put("threetenbp", "Backport of JSR310 (preferred for jdk < 1.8)");
        dateOptions.put("java8-localdatetime", "Java 8 using LocalDateTime (for legacy app only)");
        dateOptions.put("joda", "Joda (for legacy app only)");
        dateOptions.put("legacy", "Legacy java.util.Date (if you really have a good reason not to use threetenbp");
        dateLibrary.setEnum(dateOptions);
        this.cliOptions.add(dateLibrary);

        final CliOption java8Mode = new CliOption(JAVA8_MODE,
                "Option. Use Java8 classes instead of third party equivalents");
        final Map<String, String> java8ModeOptions = new HashMap<>();
        java8ModeOptions.put("true", "Use Java 8 classes such as Base64");
        java8ModeOptions.put("false", "Various third party libraries as needed");
        java8Mode.setEnum(java8ModeOptions);
        this.cliOptions.add(java8Mode);

        final CliOption java11Mode = new CliOption(JAVA11_MODE,
                "Option. Use Java11 classes instead of third party equivalents");
        final Map<String, String> java11ModeOptions = new HashMap<>();
        java11ModeOptions.put("true", "Use Java 11 classes");
        java11ModeOptions.put("false", "Various third party libraries as needed");
        java11Mode.setEnum(java11ModeOptions);
        this.cliOptions.add(java11Mode);

        this.cliOptions.add(CliOption.newBoolean(CHECK_DUPLICATED_MODEL_NAME,
                "Check if there are duplicated model names (ignoring case)"));

        this.cliOptions.add(CliOption.newBoolean(WIREMOCK_OPTION,
                "Use wiremock to generate endpoint calls to mock on generated tests."));

        this.cliOptions
                .add(CliOption.newBoolean(JAKARTA, "Use Jakarta EE (package jakarta.*) instead of Java EE (javax.*)"));

        final CliOption jeeSpec = CliOption.newBoolean(JAKARTA,
                "Use Jakarta EE (package jakarta.*) instead of Java EE (javax.*)");
        final Map<String, String> jeeSpecModeOptions = new HashMap<>();
        jeeSpecModeOptions.put("true", "Use Jakarta EE (package jakarta.*)");
        jeeSpecModeOptions.put("false", "Use Java EE (javax.*)");
        jeeSpec.setEnum(jeeSpecModeOptions);
        this.cliOptions.add(jeeSpec);

        this.cliOptions.add(CliOption.newBoolean(USE_NULLABLE_FOR_NOTNULL,
                "Add @NotNull depending on `nullable` property instead of `required`"));
    }

    @Override
    public void processOpts() {
        if (this.additionalProperties.containsKey(CodegenConstants.INVOKER_PACKAGE)) {
            this.setInvokerPackage((String) this.additionalProperties.get(CodegenConstants.INVOKER_PACKAGE));
        } else if (this.additionalProperties.containsKey(CodegenConstants.API_PACKAGE)) {
            // guess from api package
            final String derivedInvokerPackage = this
                    .deriveInvokerPackageName((String) this.additionalProperties.get(CodegenConstants.API_PACKAGE));
            this.additionalProperties.put(CodegenConstants.INVOKER_PACKAGE, derivedInvokerPackage);
            this.setInvokerPackage((String) this.additionalProperties.get(CodegenConstants.INVOKER_PACKAGE));
            LOGGER.info("Invoker Package Name, originally not set, is now derived from api package name: "
                    + derivedInvokerPackage);
        } else if (this.additionalProperties.containsKey(CodegenConstants.MODEL_PACKAGE)) {
            // guess from model package
            final String derivedInvokerPackage = this
                    .deriveInvokerPackageName((String) this.additionalProperties.get(CodegenConstants.MODEL_PACKAGE));
            this.additionalProperties.put(CodegenConstants.INVOKER_PACKAGE, derivedInvokerPackage);
            this.setInvokerPackage((String) this.additionalProperties.get(CodegenConstants.INVOKER_PACKAGE));
            LOGGER.info("Invoker Package Name, originally not set, is now derived from model package name: "
                    + derivedInvokerPackage);
        } else if (StringUtils.isNotEmpty(this.invokerPackage)) {
            // not set in additionalProperties, add value from CodegenConfig in order to use it in templates
            this.additionalProperties.put(CodegenConstants.INVOKER_PACKAGE, this.invokerPackage);
        }

        super.processOpts();

        this.modelTemplateFiles.put("model.mustache", ".java");
        this.apiTemplateFiles.put("api.mustache", ".java");
        this.apiTestTemplateFiles.put("api_test.mustache", ".java");
        this.modelDocTemplateFiles.put("model_doc.mustache", ".md");
        this.apiDocTemplateFiles.put("api_doc.mustache", ".md");

        if (this.additionalProperties.containsKey(SUPPORT_JAVA6)) {
            this.setSupportJava6(false); // JAVA 6 not supported
        }
        this.additionalProperties.put(SUPPORT_JAVA6, this.supportJava6);

        if (this.additionalProperties.containsKey(CodegenConstants.GROUP_ID)) {
            this.setGroupId((String) this.additionalProperties.get(CodegenConstants.GROUP_ID));
        } else if (StringUtils.isNotEmpty(this.groupId)) {
            // not set in additionalProperties, add value from CodegenConfig in order to use it in templates
            this.additionalProperties.put(CodegenConstants.GROUP_ID, this.groupId);
        }

        if (this.additionalProperties.containsKey(CodegenConstants.ARTIFACT_ID)) {
            this.setArtifactId((String) this.additionalProperties.get(CodegenConstants.ARTIFACT_ID));
        } else if (StringUtils.isNotEmpty(this.artifactId)) {
            // not set in additionalProperties, add value from CodegenConfig in order to use it in templates
            this.additionalProperties.put(CodegenConstants.ARTIFACT_ID, this.artifactId);
        }

        if (this.additionalProperties.containsKey(CodegenConstants.ARTIFACT_VERSION)) {
            this.setArtifactVersion((String) this.additionalProperties.get(CodegenConstants.ARTIFACT_VERSION));
        } else if (StringUtils.isNotEmpty(this.artifactVersion)) {
            // not set in additionalProperties, add value from CodegenConfig in order to use it in templates
            this.additionalProperties.put(CodegenConstants.ARTIFACT_VERSION, this.artifactVersion);
        }

        if (this.additionalProperties.containsKey(CodegenConstants.ARTIFACT_URL)) {
            this.setArtifactUrl((String) this.additionalProperties.get(CodegenConstants.ARTIFACT_URL));
        } else if (StringUtils.isNoneEmpty(this.artifactUrl)) {
            // not set in additionalProperties, add value from CodegenConfig in order to use it in templates
            this.additionalProperties.put(CodegenConstants.ARTIFACT_URL, this.artifactUrl);
        }

        if (this.additionalProperties.containsKey(CodegenConstants.ARTIFACT_DESCRIPTION)) {
            this.setArtifactDescription((String) this.additionalProperties.get(CodegenConstants.ARTIFACT_DESCRIPTION));
        } else if (StringUtils.isNoneEmpty(this.artifactDescription)) {
            // not set in additionalProperties, add value from CodegenConfig in order to use it in templates
            this.additionalProperties.put(CodegenConstants.ARTIFACT_DESCRIPTION, this.artifactDescription);
        }

        if (this.additionalProperties.containsKey(CodegenConstants.SCM_CONNECTION)) {
            this.setScmConnection((String) this.additionalProperties.get(CodegenConstants.SCM_CONNECTION));
        } else if (StringUtils.isNoneEmpty(this.scmConnection)) {
            // not set in additionalProperties, add value from CodegenConfig in order to use it in templates
            this.additionalProperties.put(CodegenConstants.SCM_CONNECTION, this.scmConnection);
        }

        if (this.additionalProperties.containsKey(CodegenConstants.SCM_DEVELOPER_CONNECTION)) {
            this.setScmDeveloperConnection(
                    (String) this.additionalProperties.get(CodegenConstants.SCM_DEVELOPER_CONNECTION));
        } else if (StringUtils.isNoneEmpty(this.scmDeveloperConnection)) {
            // not set in additionalProperties, add value from CodegenConfig in order to use it in templates
            this.additionalProperties.put(CodegenConstants.SCM_DEVELOPER_CONNECTION, this.scmDeveloperConnection);
        }

        if (this.additionalProperties.containsKey(CodegenConstants.SCM_URL)) {
            this.setScmUrl((String) this.additionalProperties.get(CodegenConstants.SCM_URL));
        } else if (StringUtils.isNoneEmpty(this.scmUrl)) {
            // not set in additionalProperties, add value from CodegenConfig in order to use it in templates
            this.additionalProperties.put(CodegenConstants.SCM_URL, this.scmUrl);
        }

        if (this.additionalProperties.containsKey(CodegenConstants.DEVELOPER_NAME)) {
            this.setDeveloperName((String) this.additionalProperties.get(CodegenConstants.DEVELOPER_NAME));
        } else if (StringUtils.isNoneEmpty(this.developerName)) {
            // not set in additionalProperties, add value from CodegenConfig in order to use it in templates
            this.additionalProperties.put(CodegenConstants.DEVELOPER_NAME, this.developerName);
        }

        if (this.additionalProperties.containsKey(CodegenConstants.DEVELOPER_EMAIL)) {
            this.setDeveloperEmail((String) this.additionalProperties.get(CodegenConstants.DEVELOPER_EMAIL));
        } else if (StringUtils.isNoneEmpty(this.developerEmail)) {
            // not set in additionalProperties, add value from CodegenConfig in order to use it in templates
            this.additionalProperties.put(CodegenConstants.DEVELOPER_EMAIL, this.developerEmail);
        }

        if (this.additionalProperties.containsKey(CodegenConstants.DEVELOPER_ORGANIZATION)) {
            this.setDeveloperOrganization(
                    (String) this.additionalProperties.get(CodegenConstants.DEVELOPER_ORGANIZATION));
        } else if (StringUtils.isNoneEmpty(this.developerOrganization)) {
            // not set in additionalProperties, add value from CodegenConfig in order to use it in templates
            this.additionalProperties.put(CodegenConstants.DEVELOPER_ORGANIZATION, this.developerOrganization);
        }

        if (this.additionalProperties.containsKey(CodegenConstants.DEVELOPER_ORGANIZATION_URL)) {
            this.setDeveloperOrganizationUrl(
                    (String) this.additionalProperties.get(CodegenConstants.DEVELOPER_ORGANIZATION_URL));
        } else if (StringUtils.isNoneEmpty(this.developerOrganizationUrl)) {
            // not set in additionalProperties, add value from CodegenConfig in order to use it in templates
            this.additionalProperties.put(CodegenConstants.DEVELOPER_ORGANIZATION_URL, this.developerOrganizationUrl);
        }

        if (this.additionalProperties.containsKey(CodegenConstants.LICENSE_NAME)) {
            this.setLicenseName((String) this.additionalProperties.get(CodegenConstants.LICENSE_NAME));
        } else if (StringUtils.isNoneEmpty(this.licenseName)) {
            // not set in additionalProperties, add value from CodegenConfig in order to use it in templates
            this.additionalProperties.put(CodegenConstants.LICENSE_NAME, this.licenseName);
        }

        if (this.additionalProperties.containsKey(CodegenConstants.LICENSE_URL)) {
            this.setLicenseUrl((String) this.additionalProperties.get(CodegenConstants.LICENSE_URL));
        } else if (StringUtils.isNoneEmpty(this.licenseUrl)) {
            // not set in additionalProperties, add value from CodegenConfig in order to use it in templates
            this.additionalProperties.put(CodegenConstants.LICENSE_URL, this.licenseUrl);
        }

        if (this.additionalProperties.containsKey(CodegenConstants.SOURCE_FOLDER)) {
            this.setSourceFolder((String) this.additionalProperties.get(CodegenConstants.SOURCE_FOLDER));
        }

        if (this.additionalProperties.containsKey(CodegenConstants.LOCAL_VARIABLE_PREFIX)) {
            this.setLocalVariablePrefix((String) this.additionalProperties.get(CodegenConstants.LOCAL_VARIABLE_PREFIX));
        }

        if (this.additionalProperties.containsKey(CodegenConstants.SERIALIZABLE_MODEL)) {
            this.setSerializableModel(
                    Boolean.valueOf(this.additionalProperties.get(CodegenConstants.SERIALIZABLE_MODEL).toString()));
        }

        if (this.additionalProperties.containsKey(CodegenConstants.LIBRARY)) {
            this.setLibrary((String) this.additionalProperties.get(CodegenConstants.LIBRARY));
        }

        if (this.additionalProperties.containsKey(CodegenConstants.SERIALIZE_BIG_DECIMAL_AS_STRING)) {
            this.setSerializeBigDecimalAsString(Boolean.parseBoolean(
                    this.additionalProperties.get(CodegenConstants.SERIALIZE_BIG_DECIMAL_AS_STRING).toString()));
        }

        // need to put back serializableModel (boolean) into additionalProperties as value in additionalProperties is
        // string
        this.additionalProperties.put(CodegenConstants.SERIALIZABLE_MODEL, this.serializableModel);

        if (this.additionalProperties.containsKey(FULL_JAVA_UTIL)) {
            this.setFullJavaUtil(Boolean.parseBoolean(this.additionalProperties.get(FULL_JAVA_UTIL).toString()));
        }

        if (this.additionalProperties.containsKey(ERROR_ON_UNKNOWN_ENUM)) {
            final boolean errorOnUnknownEnum = Boolean
                    .parseBoolean(this.additionalProperties.get(ERROR_ON_UNKNOWN_ENUM).toString());
            this.additionalProperties.put(ERROR_ON_UNKNOWN_ENUM, errorOnUnknownEnum);
        }

        if (this.additionalProperties.containsKey(WIREMOCK_OPTION)) {
            final boolean useWireMock = this.additionalProperties.get(WIREMOCK_OPTION) != null
                    && Boolean.parseBoolean(this.additionalProperties.get(WIREMOCK_OPTION).toString());
            this.additionalProperties.put(WIREMOCK_OPTION, useWireMock);
        }

        // if (this instanceof NotNullAnnotationFeatures) {
        this.notNullOption = (NotNullAnnotationFeatures) this;
        // if (this.additionalProperties.containsKey(NOT_NULL_JACKSON_ANNOTATION)) {
        this.notNullOption.setNotNullJacksonAnnotation(this.convertPropertyToBoolean(NOT_NULL_JACKSON_ANNOTATION));
        this.writePropertyBack(NOT_NULL_JACKSON_ANNOTATION, this.notNullOption.isNotNullJacksonAnnotation());
        // if (this.notNullOption.isNotNullJacksonAnnotation()) {
        this.importMapping.put("JsonInclude", "com.fasterxml.jackson.annotation.JsonInclude");
        // }
        // }
        // }

        if (this.additionalProperties.containsKey(USE_NULLABLE_FOR_NOTNULL)) {
            this.setUseNullableForNotnull(
                    Boolean.valueOf(this.additionalProperties.get(USE_NULLABLE_FOR_NOTNULL).toString()));
        }
        this.writePropertyBack(USE_NULLABLE_FOR_NOTNULL, this.useNullableForNotNull);

        if (this.fullJavaUtil) {
            this.javaUtilPrefix = "java.util.";
        }
        this.additionalProperties.put(FULL_JAVA_UTIL, this.fullJavaUtil);
        this.additionalProperties.put("javaUtilPrefix", this.javaUtilPrefix);

        if (this.additionalProperties.containsKey(WITH_XML)) {
            this.setWithXml(Boolean.parseBoolean(this.additionalProperties.get(WITH_XML).toString()));
        }
        this.additionalProperties.put(WITH_XML, this.withXml);

        // make api and model doc path available in mustache template
        this.additionalProperties.put("apiDocPath", this.apiDocPath);
        this.additionalProperties.put("modelDocPath", this.modelDocPath);

        this.importMapping.put("List", "java.util.List");

        if (this.fullJavaUtil) {
            this.typeMapping.put("array", "java.util.List");
            this.typeMapping.put("map", "java.util.Map");
            this.typeMapping.put("DateTime", "java.util.Date");
            this.typeMapping.put("UUID", "java.util.UUID");
            this.typeMapping.remove("List");
            this.importMapping.remove("Date");
            this.importMapping.remove("Map");
            this.importMapping.remove("HashMap");
            this.importMapping.remove("Array");
            this.importMapping.remove("ArrayList");
            this.importMapping.remove("List");
            this.importMapping.remove("Set");
            this.importMapping.remove("DateTime");
            this.importMapping.remove("UUID");
            this.instantiationTypes.put("array", "java.util.ArrayList");
            this.instantiationTypes.put("map", "java.util.HashMap");
        }

        this.sanitizeConfig();

        // optional jackson mappings for BigDecimal support
        this.importMapping.put("ToStringSerializer", "com.fasterxml.jackson.databind.ser.std.ToStringSerializer");
        this.importMapping.put("JsonSerialize", "com.fasterxml.jackson.databind.annotation.JsonSerialize");

        // imports for pojos
        if (this.useOas2) {
            this.importMapping.put("ApiModelProperty", "io.swagger.annotations.ApiModelProperty");
            this.importMapping.put("ApiModel", "io.swagger.annotations.ApiModel");
        } else {
            this.importMapping.put("Schema", "io.swagger.v3.oas.annotations.media.Schema");
        }

        this.importMapping.put("JsonProperty", "com.fasterxml.jackson.annotation.JsonProperty");
        this.importMapping.put("JsonSubTypes", "com.fasterxml.jackson.annotation.JsonSubTypes");
        this.importMapping.put("JsonTypeInfo", "com.fasterxml.jackson.annotation.JsonTypeInfo");
        this.importMapping.put("JsonCreator", "com.fasterxml.jackson.annotation.JsonCreator");
        this.importMapping.put("JsonValue", "com.fasterxml.jackson.annotation.JsonValue");
        this.importMapping.put("JsonTypeId", "com.fasterxml.jackson.annotation.JsonTypeId");
        this.importMapping.put("SerializedName", "com.google.gson.annotations.SerializedName");
        this.importMapping.put("TypeAdapter", "com.google.gson.TypeAdapter");
        this.importMapping.put("JsonAdapter", "com.google.gson.annotations.JsonAdapter");
        this.importMapping.put("JsonReader", "com.google.gson.stream.JsonReader");
        this.importMapping.put("JsonWriter", "com.google.gson.stream.JsonWriter");
        this.importMapping.put("IOException", "java.io.IOException");
        this.importMapping.put("Objects", "java.util.Objects");
        this.importMapping.put("StringUtil", this.invokerPackage + ".StringUtil");
        // import JsonCreator if JsonProperty is imported
        // used later in recursive import in postProcessingModels
        this.importMapping.put("com.fasterxml.jackson.annotation.JsonProperty",
                "com.fasterxml.jackson.annotation.JsonCreator");

        this.setJava8Mode(Boolean.parseBoolean(String.valueOf(this.additionalProperties.get(JAVA8_MODE))));
        this.additionalProperties.put(JAVA8_MODE, this.java8Mode);
        this.setJava11Mode(Boolean.parseBoolean(String.valueOf(this.additionalProperties.get(JAVA11_MODE))));
        this.additionalProperties.put(JAVA11_MODE, this.java11Mode);

        if (this.additionalProperties.containsKey(WITH_XML)) {
            this.setWithXml(Boolean.parseBoolean(this.additionalProperties.get(WITH_XML).toString()));
            if (this.withXml) {
                this.additionalProperties.put(WITH_XML, "true");
            }
        }

        if (this.additionalProperties.containsKey(DATE_LIBRARY)) {
            this.setDateLibrary(this.additionalProperties.get("dateLibrary").toString());
        } else if (this.java8Mode) {
            this.setDateLibrary("java8");
        }

        if ("threetenbp".equals(this.dateLibrary)) {
            this.additionalProperties.put("threetenbp", "true");
            this.additionalProperties.put("jsr310", "true");
            this.typeMapping.put("date", "LocalDate");
            this.typeMapping.put("DateTime", "OffsetDateTime");
            this.importMapping.put("LocalDate", "org.threeten.bp.LocalDate");
            this.importMapping.put("OffsetDateTime", "org.threeten.bp.OffsetDateTime");
        } else if ("joda".equals(this.dateLibrary)) {
            this.additionalProperties.put("joda", "true");
            this.typeMapping.put("date", "LocalDate");
            this.typeMapping.put("DateTime", "DateTime");
            this.importMapping.put("LocalDate", "org.joda.time.LocalDate");
            this.importMapping.put("DateTime", "org.joda.time.DateTime");
        } else if (this.dateLibrary.startsWith("java8")) {
            this.additionalProperties.put("java8", true);
            this.additionalProperties.put("jsr310", "true");
            this.typeMapping.put("date", "LocalDate");
            this.importMapping.put("LocalDate", "java.time.LocalDate");
            if ("java8-localdatetime".equals(this.dateLibrary)) {
                this.typeMapping.put("DateTime", "LocalDateTime");
                this.importMapping.put("LocalDateTime", "java.time.LocalDateTime");
            } else {
                this.typeMapping.put("DateTime", "OffsetDateTime");
                this.importMapping.put("OffsetDateTime", "java.time.OffsetDateTime");
            }
        } else if ("legacy".equals(this.dateLibrary)) {
            this.additionalProperties.put("legacyDates", true);
        }

        if (this.additionalProperties.containsKey(JAKARTA)) {
            this.setJakarta(Boolean.parseBoolean(String.valueOf(this.additionalProperties.get(JAKARTA))));
            this.additionalProperties.put(JAKARTA, this.jakarta);
        }
    }

    private void sanitizeConfig() {
        // Sanitize any config options here. We also have to update the additionalProperties because
        // the whole additionalProperties object is injected into the main object passed to the mustache layer

        this.setApiPackage(sanitizePackageName(this.apiPackage));
        if (this.additionalProperties.containsKey(CodegenConstants.API_PACKAGE)) {
            this.additionalProperties.put(CodegenConstants.API_PACKAGE, this.apiPackage);
        }

        this.setModelPackage(sanitizePackageName(this.modelPackage));
        if (this.additionalProperties.containsKey(CodegenConstants.MODEL_PACKAGE)) {
            this.additionalProperties.put(CodegenConstants.MODEL_PACKAGE, this.modelPackage);
        }

        this.setInvokerPackage(sanitizePackageName(this.invokerPackage));
        if (this.additionalProperties.containsKey(CodegenConstants.INVOKER_PACKAGE)) {
            this.additionalProperties.put(CodegenConstants.INVOKER_PACKAGE, this.invokerPackage);
        }
    }

    protected String escapeUnderscore(final String name) {
        // Java 8 discourages naming things _, but Java 9 does not allow it.
        if ("_".equals(name)) {
            return "_u";
        }
        return name;
    }

    @Override
    public String escapeReservedWord(final String name) {
        if (this.reservedWordsMappings().containsKey(name)) {
            return this.reservedWordsMappings().get(name);
        }
        return "_" + name;
    }

    @Override
    public String apiFileFolder() {
        return this.outputFolder + "/" + this.sourceFolder + "/" + this.apiPackage().replace('.', '/');
    }

    @Override
    public String apiTestFileFolder() {
        return this.outputFolder + "/" + this.testFolder + "/" + this.apiPackage().replace('.', '/');
    }

    @Override
    public String modelFileFolder() {
        return this.outputFolder + "/" + this.sourceFolder + "/" + this.modelPackage().replace('.', '/');
    }

    @Override
    public String apiDocFileFolder() {
        return (this.outputFolder + "/" + this.apiDocPath).replace('/', File.separatorChar);
    }

    @Override
    public String modelDocFileFolder() {
        return (this.outputFolder + "/" + this.modelDocPath).replace('/', File.separatorChar);
    }

    @Override
    public String toApiDocFilename(final String name) {
        return this.toApiName(name);
    }

    @Override
    public String toModelDocFilename(final String name) {
        return this.toModelName(name);
    }

    @Override
    public String toApiTestFilename(final String name) {
        return this.toApiName(name) + "Test";
    }

    @Override
    public String toApiName(final String name) {
        if (name.length() == 0) {
            return "DefaultApi";
        }
        return camelize(name);
    }

    @Override
    public String toApiFilename(final String name) {
        return this.toApiName(name);
    }

    @Override
    public String toVarName(String name) {
        // sanitize name
        name = this.sanitizeVarName(name); // FIXME: a parameter should not be assigned. Also declare the methods
                                           // parameters as 'final'.

        if (name.toLowerCase().matches("^_*class$")) {
            return "propertyClass";
        }

        name = this.escapeUnderscore(name);

        // if it's all upper case, do nothing
        if (name.matches("^[A-Z_]*$")) {
            return name;
        }

        if (this.startsWithTwoUppercaseLetters(name)) {
            name = name.substring(0, 2).toLowerCase() + name.substring(2);
        }

        // camelize (lower first character) the variable name
        // pet_id => petId
        name = this.camelizeVarName(name, true);

        // for reserved word or word starting with number, append _
        if (this.isReservedWord(name) || name.matches("^\\d.*")) {
            name = this.escapeReservedWord(name);
        }

        return name;
    }

    public String camelizeVarName(String word, final boolean lowercaseFirstLetter) {
        if (word.startsWith("_") && word.length() > 1 && !"_u".equals(word)) {
            word = "_" + DefaultCodegenConfig.camelize(word, lowercaseFirstLetter);
        } else {
            word = DefaultCodegenConfig.camelize(word, lowercaseFirstLetter);
        }

        if (!word.startsWith("$") || word.length() <= 1) {
            return word;
        }
        final String letter = String.valueOf(word.charAt(1));
        if (!StringUtils.isAllUpperCase(letter)) {
            return word;
        }
        return word.replaceFirst(letter, letter.toLowerCase());
    }

    private boolean startsWithTwoUppercaseLetters(final String name) {
        boolean startsWithTwoUppercaseLetters = false;
        if (name.length() > 1) {
            startsWithTwoUppercaseLetters = name.substring(0, 2).equals(name.substring(0, 2).toUpperCase());
        }
        return startsWithTwoUppercaseLetters;
    }

    @Override
    public String toParamName(final String name) {
        // to avoid conflicts with 'callback' parameter for async call
        if ("callback".equals(name)) {
            return "paramCallback";
        }

        // should be the same as variable name
        return this.toVarName(name);
    }

    @Override
    public String toModelName(final String name) {
        // We need to check if import-mapping has a different model for this class, so we use it
        // instead of the auto-generated one.

        if (!this.getIgnoreImportMapping() && this.importMapping.containsKey(name)) {
            return this.importMapping.get(name);
        }
        final String sanitizedName = this.sanitizeName(name);

        String nameWithPrefixSuffix = sanitizedName;
        if (!StringUtils.isEmpty(this.modelNamePrefix)) {
            // add '_' so that model name can be camelized correctly
            nameWithPrefixSuffix = this.modelNamePrefix + "_" + nameWithPrefixSuffix;
        }

        if (!StringUtils.isEmpty(this.modelNameSuffix)) {
            // add '_' so that model name can be camelized correctly
            nameWithPrefixSuffix = nameWithPrefixSuffix + "_" + this.modelNameSuffix;
        }

        // camelize the model name
        // phone_number => PhoneNumber
        final String camelizedName = camelize(nameWithPrefixSuffix);

        // model name cannot use reserved keyword, e.g. return
        if (this.isReservedWord(camelizedName)) {
            final String modelName = "Model" + camelizedName;
            LOGGER.warn(camelizedName + " (reserved word) cannot be used as model name. Renamed to " + modelName);
            return modelName;
        }

        // model name starts with number
        if (camelizedName.matches("^\\d.*")) {
            final String modelName = "Model" + camelizedName; // e.g. 200Response => Model200Response (after camelize)
            LOGGER.warn(
                    name + " (model name starts with number) cannot be used as model name. Renamed to " + modelName);
            return modelName;
        }

        return camelizedName;
    }

    @Override
    public String toModelFilename(final String name) {
        // should be the same as the model name
        return this.toModelName(name);
    }

    @Override
    public String getTypeDeclaration(final Schema propertySchema) {
        if (propertySchema instanceof ArraySchema) {
            final ArraySchema arraySchema = (ArraySchema) propertySchema;
            final Schema inner = arraySchema.getItems();
            if (inner == null) {
                LOGGER.warn(arraySchema.getName() + "(array property) does not have a proper inner type defined");
                // TODO maybe better defaulting to StringProperty than returning null
                return null;
            }
            return String.format("%s<%s>", this.getSchemaType(propertySchema), this.getTypeDeclaration(inner));
            // return getSwaggerType(propertySchema) + "<" + getTypeDeclaration(inner) + ">";
        }
        if (propertySchema instanceof MapSchema && hasSchemaProperties(propertySchema)) {
            final Schema inner = (Schema) propertySchema.getAdditionalProperties();
            if (inner == null) {
                LOGGER.warn(propertySchema.getName() + "(map property) does not have a proper inner type defined");
                // TODO maybe better defaulting to StringProperty than returning null
                return null;
            }
            return this.getSchemaType(propertySchema) + "<String, " + this.getTypeDeclaration(inner) + ">";
        }
        if (propertySchema instanceof MapSchema && hasTrueAdditionalProperties(propertySchema)) {
            final Schema inner = new ObjectSchema();
            return this.getSchemaType(propertySchema) + "<String, " + this.getTypeDeclaration(inner) + ">";
        }
        return super.getTypeDeclaration(propertySchema);
    }

    @Override
    public String getAlias(final String name) {
        if (this.typeAliases != null && this.typeAliases.containsKey(name)) {
            return this.typeAliases.get(name);
        }
        return name;
    }

    @Override
    public String toDefaultValue(final Schema schema) {
        if (schema instanceof ArraySchema) {
            final ArraySchema arraySchema = (ArraySchema) schema;
            final String pattern;
            if (this.fullJavaUtil) {
                pattern = "new java.util.ArrayList<%s>()";
            } else {
                pattern = "new ArrayList<%s>()";
            }
            if (arraySchema.getItems() == null) {
                return null;
            }

            String typeDeclaration = this.getTypeDeclaration(arraySchema.getItems());
            final Object java8obj = this.additionalProperties.get("java8");
            if (java8obj != null) {
                final Boolean java8 = Boolean.valueOf(java8obj.toString());
                if (java8 != null && java8) {
                    typeDeclaration = "";
                }
            }

            return String.format(pattern, typeDeclaration);
        }
        if (schema instanceof MapSchema && hasSchemaProperties(schema)) {
            final String pattern;
            if (this.fullJavaUtil) {
                pattern = "new java.util.HashMap<%s>()";
            } else {
                pattern = "new HashMap<%s>()";
            }
            if (schema.getAdditionalProperties() == null) {
                return null;
            }

            String typeDeclaration = String.format("String, %s",
                    this.getTypeDeclaration((Schema) schema.getAdditionalProperties()));
            final Object java8obj = this.additionalProperties.get("java8");
            if (java8obj != null) {
                final Boolean java8 = Boolean.valueOf(java8obj.toString());
                if (java8 != null && java8) {
                    typeDeclaration = "";
                }
            }

            return String.format(pattern, typeDeclaration);
        }
        if (schema instanceof MapSchema && hasTrueAdditionalProperties(schema)) {
            final String pattern;
            if (this.fullJavaUtil) {
                pattern = "new java.util.HashMap<%s>()";
            } else {
                pattern = "new HashMap<%s>()";
            }
            if (schema.getAdditionalProperties() == null) {
                return null;
            }
            final Schema inner = new ObjectSchema();
            String typeDeclaration = String.format("String, %s", this.getTypeDeclaration(inner));
            final Object java8obj = this.additionalProperties.get("java8");
            if (java8obj != null) {
                final Boolean java8 = Boolean.valueOf(java8obj.toString());
                if (java8 != null && java8) {
                    typeDeclaration = "";
                }
            }

            return String.format(pattern, typeDeclaration);
        }
        if (schema instanceof IntegerSchema) {
            if (schema.getDefault() != null && SchemaTypeUtil.INTEGER64_FORMAT.equals(schema.getFormat())) {
                return String.format("%sl", schema.getDefault().toString());
            }
        } else if (schema instanceof NumberSchema) {
            if (schema.getDefault() != null) {
                if (schema.getDefault() != null && SchemaTypeUtil.FLOAT_FORMAT.equals(schema.getFormat())) {
                    return String.format("%sf", schema.getDefault().toString());
                }
                if (schema.getDefault() != null && SchemaTypeUtil.DOUBLE_FORMAT.equals(schema.getFormat())) {
                    return String.format("%sd", schema.getDefault().toString());
                } else {
                    return String.format("new BigDecimal(%s)", schema.getDefault().toString());
                }
            }
        } else if ((schema instanceof StringSchema) && (schema.getDefault() != null)) {
            final String _default = schema.getDefault().toString();
            if (schema.getEnum() == null) {
                return String.format("\"%s\"", this.escapeText(_default));
            }
            // convert to enum var name later in postProcessModels
            return _default;
        }
        return super.toDefaultValue(schema);
    }

    @Override
    public void setParameterExampleValue(final CodegenParameter p) {
        String example;

        if (p.defaultValue == null) {
            example = p.example;
        } else {
            example = p.defaultValue;
        }

        String type = p.baseType;
        if (type == null) {
            type = p.dataType;
        }

        if ("String".equals(type)) {
            if (example == null) {
                example = p.paramName + "_example";
            }
            p.testExample = example;
            example = "\"" + this.escapeText(example) + "\"";
        } else if ("Integer".equals(type) || "Short".equals(type)) {
            if (example == null) {
                example = "56";
            }
        } else if ("Long".equals(type)) {
            if (example == null) {
                example = "56";
            }
            p.testExample = example;
            example = example + "L";
        } else if ("Float".equals(type)) {
            if (example == null) {
                example = "3.4";
            }
            p.testExample = example;
            example = example + "F";
        } else if ("Double".equals(type)) {
            example = "3.4";
            p.testExample = example;
            example = example + "D";
        } else if ("Boolean".equals(type)) {
            if (example == null) {
                example = "true";
            }
        } else if ("File".equals(type)) {
            if (example == null) {
                example = "/path/to/file";
            }
            example = "new File(\"" + this.escapeText(example) + "\")";
        } else if ("Date".equals(type)) {
            example = "new Date()";
        } else if (!this.languageSpecificPrimitives.contains(type)) {
            // type is a model class, e.g. User
            example = "new " + type + "()";
        }

        if (p.testExample == null) {
            p.testExample = example;
        }

        if (example == null) {
            example = "null";
        } else if (getBooleanValue(p, CodegenConstants.IS_LIST_CONTAINER_EXT_NAME)) {
            example = "Arrays.asList(" + example + ")";
        } else if (getBooleanValue(p, CodegenConstants.IS_MAP_CONTAINER_EXT_NAME)) {
            example = "new HashMap()";
        }

        p.example = example;
    }

    @Override
    public String toExampleValue(final Schema schemaProperty) {
        if (schemaProperty.getExample() != null) {
            return this.escapeText(schemaProperty.getExample().toString());
        }
        return super.toExampleValue(schemaProperty);
    }

    @Override
    public String getSchemaType(final Schema schema) {
        String schemaType = super.getSchemaType(schema);

        schemaType = this.getAlias(schemaType);

        // don't apply renaming on types from the typeMapping
        if (this.typeMapping.containsKey(schemaType)) {
            return this.typeMapping.get(schemaType);
        }

        if (null == schemaType) {
            if (schema.getName() != null) {
                LOGGER.warn("No Type defined for Property " + schema.getName());
            } else {
                // LOGGER.error("No Type defined.", new Exception());
            }
        }
        return this.toModelName(schemaType);
    }

    @Override
    public String toOperationId(String operationId) {
        // throw exception if method name is empty
        if (StringUtils.isEmpty(operationId)) {
            throw new RuntimeException("Empty method/operation name (operationId) not allowed");
        }

        operationId = camelize(this.sanitizeName(operationId), true);

        // method name cannot use reserved keyword, e.g. return
        if (this.isReservedWord(operationId)) {
            final String newOperationId = camelize("call_" + operationId, true);
            LOGGER.warn(operationId + " (reserved word) cannot be used as method name. Renamed to " + newOperationId);
            return newOperationId;
        }

        return operationId;
    }

    @Override
    public CodegenModel fromModel(final String name, final Schema schema, final Map<String, Schema> allSchemas) {
        CodegenModel codegenModel = super.fromModel(name, schema, allSchemas);
        if (codegenModel.description != null) {
            if (this.useOas2) {
                codegenModel.imports.add("ApiModel");
            } else {
                codegenModel.imports.add("Schema");
            }
        }
        if (codegenModel.discriminator != null && this.additionalProperties.containsKey("jackson")) {
            codegenModel.imports.add("JsonSubTypes");
            codegenModel.imports.add("JsonTypeInfo");
        }
        final boolean hasEnums = getBooleanValue(codegenModel, HAS_ENUMS_EXT_NAME);
        if (allSchemas != null && codegenModel.parentSchema != null && hasEnums) {
            final Schema parentModel = allSchemas.get(codegenModel.parentSchema);
            final CodegenModel parentCodegenModel = super.fromModel(codegenModel.parent, parentModel, allSchemas);
            codegenModel = AbstractJavaCodegen.reconcileInlineEnums(codegenModel, parentCodegenModel);
        }
        // if ((this instanceof NotNullAnnotationFeatures) && (this instanceof NotNullAnnotationFeatures)) {
        this.notNullOption = (NotNullAnnotationFeatures) this;
        // if (this.additionalProperties.containsKey(NOT_NULL_JACKSON_ANNOTATION)
        // && this.notNullOption.isNotNullJacksonAnnotation()) {
        codegenModel.imports.add("JsonInclude");
        // }
        // }
        return codegenModel;
    }

    @Override
    protected void addAdditionPropertiesToCodeGenModel(final CodegenModel codegenModel, final Schema schema) {
        super.addAdditionPropertiesToCodeGenModel(codegenModel, schema);
        this.addVars(codegenModel, schema.getProperties(), schema.getRequired());
    }

    @Override
    public void postProcessModelProperty(final CodegenModel model, final CodegenProperty property) {
        if (this.serializeBigDecimalAsString && "BigDecimal".equals(property.baseType)) {
            // we serialize BigDecimal as `string` to avoid precision loss
            property.vendorExtensions.put("extraAnnotation", "@JsonSerialize(using = ToStringSerializer.class)");

            // this requires some more imports to be added for this model...
            model.imports.add("ToStringSerializer");
            model.imports.add("JsonSerialize");
        }

        if (!this.fullJavaUtil) {
            if ("array".equals(property.containerType)) {
                model.imports.add("ArrayList");
            } else if ("map".equals(property.containerType)) {
                model.imports.add("HashMap");
            }
        }

        final boolean isEnum = getBooleanValue(model, IS_ENUM_EXT_NAME);
        if (!BooleanUtils.toBoolean(isEnum)) {
            // needed by all pojos, but not enums
            if (this.useOas2) {
                model.imports.add("ApiModelProperty");
                model.imports.add("ApiModel");
            } else {
                model.imports.add("Schema");
            }
        }
        if (model.discriminator != null && model.discriminator.getPropertyName().equals(property.baseName)) {
            property.vendorExtensions.put("x-is-discriminator-property", true);
            if (this.additionalProperties.containsKey("jackson")) {
                model.imports.add("JsonTypeId");
            }
        }
    }

    @Override
    protected void fixUpParentAndInterfaces(final CodegenModel codegenModel,
            final Map<String, CodegenModel> allModels) {
        super.fixUpParentAndInterfaces(codegenModel, allModels);
        if (codegenModel.vars == null || codegenModel.vars.isEmpty() || codegenModel.parentModel == null) {
            return;
        }

        for (final CodegenProperty codegenProperty : codegenModel.vars) {
            CodegenModel parentModel = codegenModel.parentModel;

            while (parentModel != null) {
                if (parentModel.vars == null || parentModel.vars.isEmpty()) {
                    parentModel = parentModel.parentModel;
                    continue;
                }
                final boolean hasConflict = parentModel.vars.stream()
                        .anyMatch(parentProperty -> (parentProperty.name.equals(codegenProperty.name)
                                || parentProperty.getGetter().equals(codegenProperty.getGetter())
                                || parentProperty.getSetter().equals(codegenProperty.getSetter())
                                        && !parentProperty.datatype.equals(codegenProperty.datatype)));
                if (hasConflict) {
                    codegenProperty.name = this.toVarName(codegenModel.name + "_" + codegenProperty.name);
                    codegenProperty.nameInCamelCase = camelize(codegenProperty.name, false);
                    codegenProperty.getter = this.toGetter(codegenProperty.name);
                    codegenProperty.setter = this.toSetter(codegenProperty.name);
                    break;
                }
                parentModel = parentModel.parentModel;
            }
        }
    }

    @Override
    public void postProcessParameter(final CodegenParameter parameter) {
    }

    @Override
    public Map<String, Object> postProcessModels(final Map<String, Object> objs) {
        // recursively add import for mapping one type to multiple imports
        final List<Map<String, String>> recursiveImports = (List<Map<String, String>>) objs.get("imports");
        if (recursiveImports == null) {
            return objs;
        }

        final ListIterator<Map<String, String>> listIterator = recursiveImports.listIterator();
        while (listIterator.hasNext()) {
            final String _import = listIterator.next().get("import");
            // if the import package happens to be found in the importMapping (key)
            // add the corresponding import package to the list
            if (this.importMapping.containsKey(_import)) {
                final Map<String, String> newImportMap = new HashMap<>();
                newImportMap.put("import", this.importMapping.get(_import));
                listIterator.add(newImportMap);
            }
        }

        return this.postProcessModelsEnum(objs);
    }

    @Override
    public Map<String, Object> postProcessOperations(final Map<String, Object> objs) {
        // Remove imports of List, ArrayList, Map and HashMap as they are
        // imported in the template already.
        final List<Map<String, String>> imports = (List<Map<String, String>>) objs.get("imports");
        final Pattern pattern = Pattern.compile("java\\.util\\.(List|ArrayList|Map|HashMap)");
        for (final Iterator<Map<String, String>> itr = imports.iterator(); itr.hasNext();) {
            final String _import = itr.next().get("import");
            if (pattern.matcher(_import).matches()) {
                itr.remove();
            }
        }
        return objs;
    }

    @Override
    public void preprocessOpenAPI(final OpenAPI openAPI) {
        super.preprocessOpenAPI(openAPI);
        if (openAPI == null || openAPI.getPaths() == null) {
            return;
        }
        final boolean checkDuplicatedModelName = Boolean
                .parseBoolean(this.additionalProperties.get(CHECK_DUPLICATED_MODEL_NAME) != null
                        ? this.additionalProperties.get(CHECK_DUPLICATED_MODEL_NAME).toString()
                        : "");
        if (checkDuplicatedModelName) {
            this.checkDuplicatedModelNameIgnoringCase(openAPI);
        }

        for (final String pathname : openAPI.getPaths().keySet()) {
            final PathItem pathItem = openAPI.getPaths().get(pathname);

            for (final Operation operation : pathItem.readOperations()) {
                if (operation == null) {
                    continue;
                }
                // only add content-Type if its no a GET-Method
                if (!operation.equals(pathItem.getGet())) {
                    String contentType = this.getContentType(operation.getRequestBody());
                    if (StringUtils.isBlank(contentType)) {
                        contentType = DEFAULT_CONTENT_TYPE;
                    }
                    operation.addExtension("x-contentType", contentType);
                }
                final String accepts = getAccept(operation);
                operation.addExtension("x-accepts", accepts);
            }
        }
    }

    private static String getAccept(final Operation operation) {
        String accepts = null;
        if (operation != null && operation.getResponses() != null && !operation.getResponses().isEmpty()) {
            final StringBuilder mediaTypeBuilder = new StringBuilder();

            responseLoop: for (final ApiResponse response : operation.getResponses().values()) {
                if (response.getContent() == null || response.getContent().isEmpty()) {
                    continue;
                }

                mediaTypeLoop: for (final String mediaTypeKey : response.getContent().keySet()) {
                    if (DEFAULT_CONTENT_TYPE.equalsIgnoreCase(mediaTypeKey)) {
                        accepts = DEFAULT_CONTENT_TYPE;
                        break responseLoop;
                    }
                    if (mediaTypeBuilder.length() > 0) {
                        mediaTypeBuilder.append(",");
                    }
                    mediaTypeBuilder.append(mediaTypeKey);
                }
            }
            if (accepts == null) {
                accepts = mediaTypeBuilder.toString();
            }
        } else {
            accepts = DEFAULT_CONTENT_TYPE;
        }
        return accepts;
    }

    @Override
    protected boolean needToImport(final String type) {
        return super.needToImport(type) && type.indexOf(".") < 0;
    }

    protected void checkDuplicatedModelNameIgnoringCase(final OpenAPI openAPI) {
        final Map<String, Schema> schemas = openAPI.getComponents().getSchemas();
        final Map<String, Map<String, Schema>> schemasRepeated = new HashMap<>();

        for (final String schemaKey : schemas.keySet()) {
            final Schema schema = schemas.get(schemaKey);
            final String lowerKeyDefinition = schemaKey.toLowerCase();

            if (schemasRepeated.containsKey(lowerKeyDefinition)) {
                Map<String, Schema> modelMap = schemasRepeated.get(lowerKeyDefinition);
                if (modelMap == null) {
                    modelMap = new HashMap<>();
                    schemasRepeated.put(lowerKeyDefinition, modelMap);
                }
                modelMap.put(schemaKey, schema);
            } else {
                schemasRepeated.put(lowerKeyDefinition, null);
            }
        }
        for (final String lowerKeyDefinition : schemasRepeated.keySet()) {
            final Map<String, Schema> modelMap = schemasRepeated.get(lowerKeyDefinition);
            if (modelMap == null) {
                continue;
            }
            int index = 1;
            for (final String name : modelMap.keySet()) {
                final Schema schema = modelMap.get(name);
                final String newModelName = name + index;
                schemas.put(newModelName, schema);
                this.replaceDuplicatedInPaths(openAPI.getPaths(), name, newModelName);
                this.replaceDuplicatedInModelProperties(schemas, name, newModelName);
                schemas.remove(name);
                index++;
            }
        }
    }

    protected void replaceDuplicatedInPaths(final Paths paths, final String modelName, final String newModelName) {
        if (paths == null || paths.isEmpty()) {
            return;
        }
        paths.values().stream().flatMap(pathItem -> pathItem.readOperations().stream()).filter(operation -> {
            final RequestBody requestBody = operation.getRequestBody();
            if (requestBody == null || requestBody.getContent() == null || requestBody.getContent().isEmpty()) {
                return false;
            }
            final Optional<MediaType> mediaTypeOptional = requestBody.getContent().values().stream().findAny();
            if (!mediaTypeOptional.isPresent()) {
                return false;
            }
            final MediaType mediaType = mediaTypeOptional.get();
            final Schema schema = mediaType.getSchema();
            if (schema.get$ref() != null) {
                return true;
            }
            return false;
        }).forEach(operation -> {
            final Schema schema = this.getSchemaFromBody(operation.getRequestBody());
            schema.set$ref(schema.get$ref().replace(modelName, newModelName));
        });
        paths.values().stream().flatMap(path -> path.readOperations().stream())
                .flatMap(operation -> operation.getResponses().values().stream()).filter(response -> {
                    if (response.getContent() == null || response.getContent().isEmpty()) {
                        return false;
                    }
                    final Optional<MediaType> mediaTypeOptional = response.getContent().values().stream().findFirst();
                    if (!mediaTypeOptional.isPresent()) {
                        return false;
                    }
                    final MediaType mediaType = mediaTypeOptional.get();
                    final Schema schema = mediaType.getSchema();
                    if (schema.get$ref() != null) {
                        return true;
                    }
                    return false;
                }).forEach(response -> {
                    final Optional<MediaType> mediaTypeOptional = response.getContent().values().stream().findFirst();
                    final Schema schema = mediaTypeOptional.get().getSchema();
                    schema.set$ref(schema.get$ref().replace(modelName, newModelName));
                });
    }

    protected void replaceDuplicatedInModelProperties(final Map<String, Schema> definitions, final String modelName,
            final String newModelName) {
        definitions.values().stream().flatMap(model -> model.getProperties().values().stream())
                .filter(property -> ((Schema) property).get$ref() != null).forEach(property -> {
                    final Schema schema = (Schema) property;
                    schema.set$ref(schema.get$ref().replace(modelName, newModelName));
                });
    }

    @Override
    public String toEnumName(final CodegenProperty property) {
        return this.sanitizeName(camelize(property.name)) + "Enum";
    }

    @Override
    public String toEnumVarName(final String value, final String datatype) {
        if (value.length() == 0) {
            return "EMPTY";
        }

        // for symbol, e.g. $, #
        if (this.getSymbolName(value) != null) {
            return this.getSymbolName(value).toUpperCase();
        }

        // number
        if ("Integer".equals(datatype) || "Long".equals(datatype) || "Float".equals(datatype)
                || "Double".equals(datatype) || "BigDecimal".equals(datatype)) {
            String varName = "NUMBER_" + value;
            varName = varName.replace("-", "MINUS_");
            varName = varName.replace("+", "PLUS_");
            return varName.replace(".", "_DOT_");
        }

        // string
        final String var = value.replaceAll("\\W+", "_").toUpperCase();
        if (var.matches("\\d.*")) {
            return "_" + var;
        }
        return this.escapeUnderscore(var).toUpperCase();
    }

    @Override
    public String toEnumValue(final String value, final String datatype) {
        if (value == null) {
            return null;
        }
        if ("Integer".equals(datatype) || "Double".equals(datatype) || "Boolean".equals(datatype)) {
            return value;
        }
        if ("Long".equals(datatype)) {
            // add l to number, e.g. 2048 => 2048l
            return value + "l";
        }
        if ("Float".equals(datatype)) {
            // add f to number, e.g. 3.14 => 3.14f
            return value + "f";
        }
        if ("BigDecimal".equals(datatype)) {
            return "new BigDecimal(" + this.escapeText(value) + ")";
        }
        return "\"" + this.escapeText(value) + "\"";
    }

    @Override
    public CodegenOperation fromOperation(final String path, final String httpMethod, final Operation operation,
            final Map<String, Schema> schemas, final OpenAPI openAPI) {
        final CodegenOperation op = super.fromOperation(path, httpMethod, operation, schemas, openAPI);
        op.path = this.sanitizePath(op.path);
        return op;
    }

    public String sanitizeVarName(String name) {
        if (name == null) {
            LOGGER.warn("String to be sanitized is null. Default to " + Object.class.getSimpleName());
            return Object.class.getSimpleName();
        }
        if ("$".equals(name)) {
            return "value";
        }
        name = name.replace("[]", StringUtils.EMPTY);
        name = name.replace('[', '_').replace("]", "").replace('(', '_').replace(")", StringUtils.EMPTY)
                .replace('.', '_').replace("@", "_at_").replace('-', '_').replace(' ', '_');

        // remove everything else other than word, number and _
        // $php_variable => php_variable
        if (this.allowUnicodeIdentifiers) { // could be converted to a single line with ?: operator
            name = Pattern.compile("[\\W&&[^$]]", Pattern.UNICODE_CHARACTER_CLASS).matcher(name)
                    .replaceAll(StringUtils.EMPTY);
        } else {
            name = name.replaceAll("[\\W&&[^$]]", StringUtils.EMPTY);
        }
        return name;
    }

    private static CodegenModel reconcileInlineEnums(final CodegenModel codegenModel,
            final CodegenModel parentCodegenModel) {
        // This generator uses inline classes to define enums, which breaks when
        // dealing with models that have subTypes. To clean this up, we will analyze
        // the parent and child models, look for enums that match, and remove
        // them from the child models and leave them in the parent.
        // Because the child models extend the parents, the enums will be available via the parent.

        // Only bother with reconciliation if the parent model has enums.
        final boolean hasEnums = getBooleanValue(parentCodegenModel, HAS_ENUMS_EXT_NAME);
        if (!hasEnums) {
            return codegenModel;
        }

        // Get the properties for the parent and child models
        final List<CodegenProperty> parentModelCodegenProperties = parentCodegenModel.vars;
        final List<CodegenProperty> codegenProperties = codegenModel.vars;

        // Iterate over all of the parent model properties
        boolean removedChildEnum = false;
        for (final CodegenProperty parentModelCodegenPropery : parentModelCodegenProperties) {
            // Look for enums
            boolean isEnum = getBooleanValue(parentModelCodegenPropery, IS_ENUM_EXT_NAME);
            if (isEnum) {
                // Now that we have found an enum in the parent class,
                // and search the child class for the same enum.
                final Iterator<CodegenProperty> iterator = codegenProperties.iterator();
                while (iterator.hasNext()) {
                    final CodegenProperty codegenProperty = iterator.next();
                    isEnum = getBooleanValue(codegenProperty, IS_ENUM_EXT_NAME);
                    // we don't check for the full set of properties as they could be overridden
                    // e.g. in the child; if we used codegenProperty.equals, the result in this
                    // case would be `false` resulting on 2 different enums created on parent and
                    // child classes, used in same method. This means that the child class will use
                    // the enum defined in the parent, loosing any overridden property
                    if (isEnum && isSameEnum(codegenProperty, parentModelCodegenPropery)) {
                        // We found an enum in the child class that is
                        // a duplicate of the one in the parent, so remove it.
                        iterator.remove();
                        removedChildEnum = true;
                    }
                }
            }
        }

        if (removedChildEnum) {
            // If we removed an entry from this model's vars, we need to ensure hasMore is updated
            int count = 0;
            final int numVars = codegenProperties.size();
            for (final CodegenProperty codegenProperty : codegenProperties) {
                count += 1;
                codegenProperty.getVendorExtensions().put(CodegenConstants.HAS_MORE_EXT_NAME,
                        (count < numVars) ? true : false);
            }

            if (!codegenProperties.isEmpty()) {
                codegenModel.getVendorExtensions().put(CodegenConstants.HAS_VARS_EXT_NAME, true);
            } else {
                codegenModel.emptyVars = true;
                codegenModel.getVendorExtensions().put(CodegenConstants.HAS_VARS_EXT_NAME, false);
            }
            codegenModel.getVendorExtensions().put(CodegenConstants.HAS_ENUMS_EXT_NAME, false);

            codegenModel.vars = codegenProperties;
        }
        return codegenModel;

    }

    protected static boolean isSameEnum(final CodegenProperty actual, final CodegenProperty other) {
        if (actual == null && other == null) {
            return true;
        }
        if ((actual.name == null) ? (other.name != null) : !actual.name.equals(other.name)) {
            return false;
        }
        if ((actual.baseName == null) ? (other.baseName != null) : !actual.baseName.equals(other.baseName)) {
            return false;
        }
        if ((actual.datatype == null) ? (other.datatype != null) : !actual.datatype.equals(other.datatype)) {
            return false;
        }
        if ((actual.datatypeWithEnum == null) ? (other.datatypeWithEnum != null)
                : !actual.datatypeWithEnum.equals(other.datatypeWithEnum)) {
            return false;
        }
        if ((actual.baseType == null) ? (other.baseType != null) : !actual.baseType.equals(other.baseType)) {
            return false;
        }
        if (!Objects.equals(actual.enumName, other.enumName)) {
            return false;
        }
        return true;
    }

    private static String sanitizePackageName(String packageName) {
        packageName = packageName.trim(); // FIXME: a parameter should not be assigned. Also declare the methods
                                          // parameters as 'final'.
        packageName = packageName.replaceAll("[^a-zA-Z0-9_\\.]", "_");
        if (packageName == null || packageName.isEmpty()) {
            return "invalidPackageName";
        }
        return packageName;
    }

    public void setInvokerPackage(final String invokerPackage) {
        this.invokerPackage = invokerPackage;
    }

    public void setGroupId(final String groupId) {
        this.groupId = groupId;
    }

    public void setArtifactId(final String artifactId) {
        this.artifactId = artifactId;
    }

    public void setArtifactVersion(final String artifactVersion) {
        this.artifactVersion = artifactVersion;
    }

    public void setArtifactUrl(final String artifactUrl) {
        this.artifactUrl = artifactUrl;
    }

    public void setArtifactDescription(final String artifactDescription) {
        this.artifactDescription = artifactDescription;
    }

    public void setScmConnection(final String scmConnection) {
        this.scmConnection = scmConnection;
    }

    public void setScmDeveloperConnection(final String scmDeveloperConnection) {
        this.scmDeveloperConnection = scmDeveloperConnection;
    }

    public void setScmUrl(final String scmUrl) {
        this.scmUrl = scmUrl;
    }

    public void setUseNullableForNotnull(final Boolean useNullableForNotNull) {
        this.useNullableForNotNull = useNullableForNotNull;
    }

    public void setDeveloperName(final String developerName) {
        this.developerName = developerName;
    }

    public void setDeveloperEmail(final String developerEmail) {
        this.developerEmail = developerEmail;
    }

    public void setDeveloperOrganization(final String developerOrganization) {
        this.developerOrganization = developerOrganization;
    }

    public void setDeveloperOrganizationUrl(final String developerOrganizationUrl) {
        this.developerOrganizationUrl = developerOrganizationUrl;
    }

    public void setLicenseName(final String licenseName) {
        this.licenseName = licenseName;
    }

    public void setLicenseUrl(final String licenseUrl) {
        this.licenseUrl = licenseUrl;
    }

    public void setSourceFolder(final String sourceFolder) {
        this.sourceFolder = sourceFolder;
    }

    public void setTestFolder(final String testFolder) {
        this.testFolder = testFolder;
    }

    public void setLocalVariablePrefix(final String localVariablePrefix) {
        this.localVariablePrefix = localVariablePrefix;
    }

    public void setSerializeBigDecimalAsString(final boolean s) {
        this.serializeBigDecimalAsString = s;
    }

    public void setSerializableModel(final Boolean serializableModel) {
        this.serializableModel = serializableModel;
    }

    private String sanitizePath(final String p) {
        // prefer replace a ", instead of a fuLL URL encode for readability
        return p.replace("\"", "%22");
    }

    public void setFullJavaUtil(final boolean fullJavaUtil) {
        this.fullJavaUtil = fullJavaUtil;
    }

    public void setWithXml(final boolean withXml) {
        this.withXml = withXml;
    }

    public void setDateLibrary(final String library) {
        this.dateLibrary = "joda";
    }

    public void setJava8Mode(final boolean enabled) {
        this.java8Mode = enabled;
    }

    public void setJava11Mode(final boolean java11Mode) {
        this.java11Mode = true;
    }

    public void setJakarta(final boolean jakarta) {
        this.jakarta = jakarta;
    }

    @Override
    public String escapeQuotationMark(final String input) {
        // remove " to avoid code injection
        return input.replace("\"", "");
    }

    @Override
    public String escapeUnsafeCharacters(final String input) {
        return input.replace("*/", "*_/").replace("/*", "/_*");
    }

    /*
     * Derive invoker package name based on the input e.g. foo.bar.model => foo.bar
     * @param input API package/model name
     * @return Derived invoker package name based on API package/model name
     */
    private String deriveInvokerPackageName(final String input) {
        final String[] parts = input.split(Pattern.quote(".")); // Split on period.

        final StringBuilder sb = new StringBuilder();
        String delim = "";
        for (final String p : Arrays.copyOf(parts, parts.length - 1)) {
            sb.append(delim).append(p);
            delim = ".";
        }
        return sb.toString();
    }

    public void setSupportJava6(final boolean value) {
        this.supportJava6 = value;
    }

    @Override
    public String toRegularExpression(final String pattern) {
        return this.escapeText(pattern);
    }

    @Override
    public boolean convertPropertyToBoolean(final String propertyKey) {
        boolean booleanValue = false;
        if (this.additionalProperties.containsKey(propertyKey)) {
            booleanValue = Boolean.parseBoolean(this.additionalProperties.get(propertyKey).toString());
        }

        return booleanValue;
    }

    @Override
    public void writePropertyBack(final String propertyKey, final boolean value) {
        this.additionalProperties.put(propertyKey, value);
    }

    /**
     * Output the Getter name for boolean property, e.g. isActive
     * @param name the name of the property
     * @return getter name based on naming convention
     */
    @Override
    public String toBooleanGetter(final String name) {
        return "is" + this.getterAndSetterCapitalize(name);
    }

    @Override
    public String sanitizeTag(String tag) {
        tag = camelize(underscore(this.sanitizeName(tag)));

        // tag starts with numbers
        if (tag.matches("^\\d.*")) {
            tag = "Class" + tag;
        }
        return tag;
    }

    @Override
    public void addHandlebarHelpers(final Handlebars handlebars) {
        super.addHandlebarHelpers(handlebars);
        handlebars.registerHelpers(new JavaHelper());
    }

    @Override
    public void setLanguageArguments(final List<CodegenArgument> languageArguments) {
        if ((languageArguments != null) && !languageArguments.stream().anyMatch(
                codegenArgument -> CodegenConstants.USE_OAS2_OPTION.equalsIgnoreCase(codegenArgument.getOption())
                        && StringUtils.isNotBlank(codegenArgument.getValue()))) {
            languageArguments.add(new CodegenArgument().option(CodegenConstants.USE_OAS2_OPTION).type("boolean")
                    .value(Boolean.FALSE.toString()));
        }

        super.setLanguageArguments(languageArguments);
    }

    @Override
    public boolean checkAliasModel() {
        return true;
    }
}
