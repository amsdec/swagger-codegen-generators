package io.swagger.codegen.v3.generators.java;

import static io.swagger.codegen.v3.CodegenConstants.IS_ENUM_EXT_NAME;
import static io.swagger.codegen.v3.generators.handlebars.ExtensionHelper.getBooleanValue;
import static java.util.Collections.sort;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.codegen.v3.CliOption;
import io.swagger.codegen.v3.CodegenConstants;
import io.swagger.codegen.v3.CodegenModel;
import io.swagger.codegen.v3.CodegenOperation;
import io.swagger.codegen.v3.CodegenParameter;
import io.swagger.codegen.v3.CodegenProperty;
import io.swagger.codegen.v3.CodegenType;
import io.swagger.codegen.v3.SupportingFile;
import io.swagger.codegen.v3.generators.features.BeanValidationFeatures;
import io.swagger.codegen.v3.generators.features.GzipFeatures;
import io.swagger.codegen.v3.generators.features.NotNullAnnotationFeatures;
import io.swagger.codegen.v3.generators.features.PerformBeanValidationFeatures;
import io.swagger.codegen.v3.generators.util.OpenAPIUtil;

public class JavaClientCodegen extends AbstractJavaCodegen
        implements BeanValidationFeatures, PerformBeanValidationFeatures, GzipFeatures, NotNullAnnotationFeatures {

    static final String MEDIA_TYPE = "mediaType";

    private static final Logger LOGGER = LoggerFactory.getLogger(JavaClientCodegen.class);

    public static final String USE_RX_JAVA = "useRxJava";

    public static final String USE_RX_JAVA2 = "useRxJava2";

    public static final String USE_RX_JAVA3 = "useRxJava3";

    public static final String DO_NOT_USE_RX = "doNotUseRx";

    public static final String USE_PLAY_WS = "usePlayWS";

    public static final String PLAY_VERSION = "playVersion";

    public static final String PARCELABLE_MODEL = "parcelableModel";

    public static final String USE_RUNTIME_EXCEPTION = "useRuntimeException";

    public static final String PLAY_24 = "play24";

    public static final String PLAY_25 = "play25";

    public static final String RETROFIT_1 = "retrofit";

    public static final String RETROFIT_2 = "retrofit2";

    protected String gradleWrapperPackage = "gradle.wrapper";

    protected boolean useRxJava = false;

    protected boolean useRxJava2 = false;

    protected boolean useRxJava3 = false;

    protected boolean doNotUseRx = true; // backwards compatibility for swagger configs that specify neither rx1 nor rx2
                                         // (mustache does not allow for boolean operators so we need this extra field)

    protected boolean usePlayWS = false;

    protected String playVersion = PLAY_25;

    protected boolean parcelableModel = false;

    protected boolean useBeanValidation = false;

    protected boolean performBeanValidation = false;

    protected boolean useGzipFeature = false;

    protected boolean useRuntimeException = false;

    private boolean notNullJacksonAnnotation = true;

    public JavaClientCodegen() {
        this.outputFolder = "generated-code" + File.separator + "java";
        this.invokerPackage = "io.swagger.client";
        this.artifactId = "swagger-java-client";
        this.apiPackage = "io.swagger.client.api";
        this.modelPackage = "io.swagger.client.model";

        this.cliOptions.add(
                CliOption.newBoolean(USE_RX_JAVA, "Whether to use the RxJava adapter with the retrofit2 library."));
        this.cliOptions.add(
                CliOption.newBoolean(USE_RX_JAVA2, "Whether to use the RxJava2 adapter with the retrofit2 library."));
        this.cliOptions.add(
                CliOption.newBoolean(USE_RX_JAVA3, "Whether to use the RxJava3 adapter with the retrofit2 library."));
        this.cliOptions.add(CliOption.newBoolean(PARCELABLE_MODEL,
                "Whether to generate models for Android that implement Parcelable with the okhttp-gson library."));
        this.cliOptions.add(CliOption.newBoolean(USE_PLAY_WS, "Use Play! Async HTTP client (Play WS API)"));
        this.cliOptions.add(CliOption.newString(PLAY_VERSION,
                "Version of Play! Framework (possible values \"play24\", \"play25\")"));
        this.cliOptions.add(CliOption.newBoolean(USE_BEANVALIDATION, "Use BeanValidation API annotations"));
        this.cliOptions.add(CliOption.newBoolean(PERFORM_BEANVALIDATION, "Perform BeanValidation"));
        this.cliOptions.add(CliOption.newBoolean(USE_GZIP_FEATURE, "Send gzip-encoded requests"));
        this.cliOptions.add(CliOption.newBoolean(USE_RUNTIME_EXCEPTION, "Use RuntimeException instead of Exception"));

        this.supportedLibraries.put("jersey1",
                "HTTP client: Jersey client 1.19.4. JSON processing: Jackson 2.10.1. Enable gzip request encoding using '-DuseGzipFeature=true'.");
        this.supportedLibraries.put("feign", "HTTP client: OpenFeign 9.4.0. JSON processing: Jackson 2.10.1");
        this.supportedLibraries.put("jersey2", "HTTP client: Jersey client 2.26. JSON processing: Jackson 2.10.1");
        this.supportedLibraries.put("jersey3", "HTTP client: Jersey client 3.0.10. JSON processing: Jackson 2.10.2");
        this.supportedLibraries.put("okhttp-gson",
                "HTTP client: OkHttp 2.7.5. JSON processing: Gson 2.8.1. Enable Parcelable models on Android using '-DparcelableModel=true'. Enable gzip request encoding using '-DuseGzipFeature=true'.");
        this.supportedLibraries.put("okhttp4-gson",
                "HTTP client: OkHttp 4.10.0. JSON processing: Gson 2.10.1. Enable Parcelable models on Android using '-DparcelableModel=true'. Enable gzip request encoding using '-DuseGzipFeature=true'.");
        this.supportedLibraries.put(RETROFIT_1,
                "HTTP client: OkHttp 2.7.5. JSON processing: Gson 2.3.1 (Retrofit 1.9.0). IMPORTANT NOTE: retrofit1.x is no longer actively maintained so please upgrade to 'retrofit2' instead.");
        this.supportedLibraries.put(RETROFIT_2,
                "HTTP client: OkHttp 3.8.0. JSON processing: Gson 2.6.1 (Retrofit 2.3.0). Enable the RxJava adapter using '-DuseRxJava[2]=true'. (RxJava 1.x or 2.x)");
        this.supportedLibraries.put("resttemplate",
                "HTTP client: Spring RestTemplate 4.3.9-RELEASE. JSON processing: Jackson 2.9.9");
        this.supportedLibraries.put("resteasy",
                "HTTP client: Resteasy client 3.1.3.Final. JSON processing: Jackson 2.9.9");

        final CliOption libraryOption = new CliOption(CodegenConstants.LIBRARY,
                "library template (sub-template) to use");
        libraryOption.setEnum(this.supportedLibraries);
        // set okhttp-gson as the default
        libraryOption.setDefault("okhttp-gson");
        this.cliOptions.add(libraryOption);
        this.setLibrary("okhttp-gson");

    }

    @Override
    public CodegenType getTag() {
        return CodegenType.CLIENT;
    }

    @Override
    public String getName() {
        return "java";
    }

    @Override
    public String getHelp() {
        return "Generates a Java client library.";
    }

    @Override
    public void processOpts() {
        if (RETROFIT_1.equalsIgnoreCase(this.library)) {
            this.dateLibrary = "joda";
        }
        if ("jersey3".equalsIgnoreCase(this.library)) {
            this.dateLibrary = "java8";
            this.additionalProperties.put(JAKARTA, true);
        }

        super.processOpts();

        if (this.additionalProperties.containsKey(USE_RX_JAVA)) {
            this.setUseRxJava(Boolean.parseBoolean(this.additionalProperties.get(USE_RX_JAVA).toString()));
        }
        if (this.additionalProperties.containsKey(USE_RX_JAVA2)) {
            this.setUseRxJava2(Boolean.parseBoolean(this.additionalProperties.get(USE_RX_JAVA2).toString()));
        }
        if (this.additionalProperties.containsKey(USE_RX_JAVA3)) {
            this.setUseRxJava3(Boolean.parseBoolean(this.additionalProperties.get(USE_RX_JAVA3).toString()));
        }

        if (!this.useRxJava && !this.useRxJava2 && !this.useRxJava3) {
            this.additionalProperties.put(DO_NOT_USE_RX, true);
        }
        if (this.additionalProperties.containsKey(USE_PLAY_WS)) {
            this.setUsePlayWS(Boolean.parseBoolean(this.additionalProperties.get(USE_PLAY_WS).toString()));
        }
        this.additionalProperties.put(USE_PLAY_WS, this.usePlayWS);

        if (this.additionalProperties.containsKey(PLAY_VERSION)) {
            this.setPlayVersion(this.additionalProperties.get(PLAY_VERSION).toString());
        }
        this.additionalProperties.put(PLAY_VERSION, this.playVersion);

        if (this.additionalProperties.containsKey(PARCELABLE_MODEL)) {
            this.setParcelableModel(Boolean.parseBoolean(this.additionalProperties.get(PARCELABLE_MODEL).toString()));
        }
        // put the boolean value back to PARCELABLE_MODEL in additionalProperties
        this.additionalProperties.put(PARCELABLE_MODEL, this.parcelableModel);

        if (this.additionalProperties.containsKey(USE_BEANVALIDATION)) {
            this.setUseBeanValidation(this.convertPropertyToBooleanAndWriteBack(USE_BEANVALIDATION));
        }

        if (this.additionalProperties.containsKey(PERFORM_BEANVALIDATION)) {
            this.setPerformBeanValidation(this.convertPropertyToBooleanAndWriteBack(PERFORM_BEANVALIDATION));
        }

        if (this.additionalProperties.containsKey(USE_GZIP_FEATURE)) {
            this.setUseGzipFeature(this.convertPropertyToBooleanAndWriteBack(USE_GZIP_FEATURE));
        }

        if (this.additionalProperties.containsKey(USE_RUNTIME_EXCEPTION)) {
            this.setUseRuntimeException(this.convertPropertyToBooleanAndWriteBack(USE_RUNTIME_EXCEPTION));
        }

        final String invokerFolder = (this.sourceFolder + File.separator + this.invokerPackage).replace(".",
                File.separator);
        final String authFolder = (this.sourceFolder + File.separator + this.invokerPackage + ".auth").replace(".",
                File.separator);
        (this.sourceFolder + File.separator + this.apiPackage).replace(".", File.separator);

        // Common files
        this.writeOptional(this.outputFolder, new SupportingFile("pom.mustache", "", "pom.xml"));
        this.writeOptional(this.outputFolder, new SupportingFile("README.mustache", "", "README.md"));
        if (this.java11Mode) {
            this.writeOptional(this.outputFolder,
                    new SupportingFile("build.gradle.java11.mustache", "", "build.gradle"));
        } else {
            this.writeOptional(this.outputFolder, new SupportingFile("build.gradle.mustache", "", "build.gradle"));
        }
        this.writeOptional(this.outputFolder, new SupportingFile("build.sbt.mustache", "", "build.sbt"));
        this.writeOptional(this.outputFolder, new SupportingFile("settings.gradle.mustache", "", "settings.gradle"));
        this.writeOptional(this.outputFolder,
                new SupportingFile("gradle.properties.mustache", "", "gradle.properties"));
        this.writeOptional(this.outputFolder,
                new SupportingFile("manifest.mustache", this.projectFolder, "AndroidManifest.xml"));
        this.supportingFiles.add(new SupportingFile("travis.mustache", "", ".travis.yml"));
        this.supportingFiles.add(new SupportingFile("ApiClient.mustache", invokerFolder, "ApiClient.java"));
        if (!"resttemplate".equals(this.getLibrary())) {
            this.supportingFiles.add(new SupportingFile("StringUtil.mustache", invokerFolder, "StringUtil.java"));
        }

        this.supportingFiles.add(new SupportingFile("auth/HttpBasicAuth.mustache", authFolder, "HttpBasicAuth.java"));
        this.supportingFiles.add(new SupportingFile("auth/ApiKeyAuth.mustache", authFolder, "ApiKeyAuth.java"));
        this.supportingFiles.add(new SupportingFile("auth/OAuth.mustache", authFolder, "OAuth.java"));
        this.supportingFiles.add(new SupportingFile("auth/OAuthFlow.mustache", authFolder, "OAuthFlow.java"));

        this.supportingFiles.add(new SupportingFile("gradlew.mustache", "", "gradlew"));
        this.supportingFiles.add(new SupportingFile("gradlew.bat.mustache", "", "gradlew.bat"));
        this.supportingFiles.add(new SupportingFile("gradle-wrapper.properties.mustache",
                this.gradleWrapperPackage.replace(".", File.separator), "gradle-wrapper.properties"));
        this.supportingFiles.add(new SupportingFile("gradle-wrapper.jar",
                this.gradleWrapperPackage.replace(".", File.separator), "gradle-wrapper.jar"));
        this.supportingFiles.add(new SupportingFile("git_push.sh.mustache", "", "git_push.sh"));
        this.supportingFiles.add(new SupportingFile("gitignore.mustache", "", ".gitignore"));

        if (this.performBeanValidation) {
            this.supportingFiles.add(new SupportingFile("BeanValidationException.mustache", invokerFolder,
                    "BeanValidationException.java"));
        }

        // TODO: add doc to retrofit1 and feign
        if ("feign".equals(this.getLibrary()) || "retrofit".equals(this.getLibrary())) {
            this.modelDocTemplateFiles.remove("model_doc.mustache");
            this.apiDocTemplateFiles.remove("api_doc.mustache");
        }

        if (!("feign".equals(this.getLibrary()) || "resttemplate".equals(this.getLibrary())
                || this.usesAnyRetrofitLibrary())) {
            this.supportingFiles.add(new SupportingFile("apiException.mustache", invokerFolder, "ApiException.java"));
            this.supportingFiles.add(new SupportingFile("Configuration.mustache", invokerFolder, "Configuration.java"));
            this.supportingFiles.add(new SupportingFile("Pair.mustache", invokerFolder, "Pair.java"));
            this.supportingFiles
                    .add(new SupportingFile("auth/Authentication.mustache", authFolder, "Authentication.java"));
        }

        if ("feign".equals(this.getLibrary())) {
            this.additionalProperties.put("jackson", "true");
            this.supportingFiles.add(new SupportingFile("ParamExpander.mustache", invokerFolder, "ParamExpander.java"));
            this.supportingFiles.add(new SupportingFile("EncodingUtils.mustache", invokerFolder, "EncodingUtils.java"));
        } else if ("okhttp-gson".equals(this.getLibrary()) || "okhttp4-gson".equals(this.getLibrary())
                || StringUtils.isEmpty(this.getLibrary())) {
            // the "okhttp-gson" library template requires "ApiCallback.mustache" for async call
            this.supportingFiles.add(new SupportingFile("ApiCallback.mustache", invokerFolder, "ApiCallback.java"));
            this.supportingFiles.add(new SupportingFile("ApiResponse.mustache", invokerFolder, "ApiResponse.java"));
            this.supportingFiles.add(new SupportingFile("JSON.mustache", invokerFolder, "JSON.java"));
            this.supportingFiles
                    .add(new SupportingFile("ProgressRequestBody.mustache", invokerFolder, "ProgressRequestBody.java"));
            this.supportingFiles.add(
                    new SupportingFile("ProgressResponseBody.mustache", invokerFolder, "ProgressResponseBody.java"));
            this.supportingFiles.add(new SupportingFile("GzipRequestInterceptor.mustache", invokerFolder,
                    "GzipRequestInterceptor.java"));
            this.additionalProperties.put("gson", "true");
        } else if (this.usesAnyRetrofitLibrary()) {
            this.supportingFiles
                    .add(new SupportingFile("auth/OAuthOkHttpClient.mustache", authFolder, "OAuthOkHttpClient.java"));
            this.supportingFiles
                    .add(new SupportingFile("CollectionFormats.mustache", invokerFolder, "CollectionFormats.java"));
            this.additionalProperties.put("gson", "true");
            if ("retrofit2".equals(this.getLibrary()) && !this.usePlayWS) {
                this.supportingFiles.add(new SupportingFile("JSON.mustache", invokerFolder, "JSON.java"));
            }
        } else if ("jersey3".equals(this.getLibrary()) || "jersey2".equals(this.getLibrary())
                || "resteasy".equals(this.getLibrary())) {
            this.supportingFiles.add(new SupportingFile("JSON.mustache", invokerFolder, "JSON.java"));
            this.additionalProperties.put("jackson", "true");
        } else if ("jersey1".equals(this.getLibrary())) {
            this.additionalProperties.put("jackson", "true");
        } else if ("resttemplate".equals(this.getLibrary())) {
            this.additionalProperties.put("jackson", "true");
            this.supportingFiles
                    .add(new SupportingFile("auth/Authentication.mustache", authFolder, "Authentication.java"));
        } else {
            LOGGER.error("Unknown library option (-l/--library): " + this.getLibrary());
        }

        if (this.usePlayWS) {
            // remove unsupported auth
            final Iterator<SupportingFile> iter = this.supportingFiles.iterator();
            while (iter.hasNext()) {
                final SupportingFile sf = iter.next();
                if (sf.templateFile.startsWith("auth/")) {
                    iter.remove();
                }
            }

            this.apiTemplateFiles.remove("api.mustache");

            if (PLAY_24.equals(this.playVersion)) {
                this.additionalProperties.put(PLAY_24, true);
                this.apiTemplateFiles.put("play24/api.mustache", ".java");

                this.supportingFiles
                        .add(new SupportingFile("play24/ApiClient.mustache", invokerFolder, "ApiClient.java"));
                this.supportingFiles.add(new SupportingFile("play24/Play24CallFactory.mustache", invokerFolder,
                        "Play24CallFactory.java"));
                this.supportingFiles.add(new SupportingFile("play24/Play24CallAdapterFactory.mustache", invokerFolder,
                        "Play24CallAdapterFactory.java"));
            } else {
                this.additionalProperties.put(PLAY_25, true);
                this.apiTemplateFiles.put("play25/api.mustache", ".java");

                this.supportingFiles
                        .add(new SupportingFile("play25/ApiClient.mustache", invokerFolder, "ApiClient.java"));
                this.supportingFiles.add(new SupportingFile("play25/Play25CallFactory.mustache", invokerFolder,
                        "Play25CallFactory.java"));
                this.supportingFiles.add(new SupportingFile("play25/Play25CallAdapterFactory.mustache", invokerFolder,
                        "Play25CallAdapterFactory.java"));
                this.additionalProperties.put("java8", "true");
            }

            this.supportingFiles
                    .add(new SupportingFile("play-common/auth/ApiKeyAuth.mustache", authFolder, "ApiKeyAuth.java"));
            this.supportingFiles
                    .add(new SupportingFile("auth/Authentication.mustache", authFolder, "Authentication.java"));
            this.supportingFiles.add(new SupportingFile("Pair.mustache", invokerFolder, "Pair.java"));

            this.additionalProperties.put("jackson", "true");
            this.additionalProperties.remove("gson");
        }

        if (this.additionalProperties.containsKey("jackson")) {
            this.supportingFiles
                    .add(new SupportingFile("RFC3339DateFormat.mustache", invokerFolder, "RFC3339DateFormat.java"));
            if ("threetenbp".equals(this.dateLibrary) && !this.usePlayWS) {
                this.supportingFiles.add(new SupportingFile("CustomInstantDeserializer.mustache", invokerFolder,
                        "CustomInstantDeserializer.java"));
            }
        }
    }

    private boolean usesAnyRetrofitLibrary() {
        return this.getLibrary() != null && this.getLibrary().contains(RETROFIT_1);
    }

    private boolean usesRetrofit2Library() {
        return this.getLibrary() != null && this.getLibrary().contains(RETROFIT_2);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> postProcessOperations(final Map<String, Object> objs) {
        super.postProcessOperations(objs);
        if (this.usesAnyRetrofitLibrary()) {
            final Map<String, Object> operations = (Map<String, Object>) objs.get("operations");
            if (operations != null) {
                final List<CodegenOperation> ops = (List<CodegenOperation>) operations.get("operation");
                for (final CodegenOperation operation : ops) {
                    final boolean hasConsumes = getBooleanValue(operation, CodegenConstants.HAS_CONSUMES_EXT_NAME);
                    if (hasConsumes) {

                        if (isMultipartType(operation.consumes)) {
                            operation.getVendorExtensions().put(CodegenConstants.IS_MULTIPART_EXT_NAME, Boolean.TRUE);
                        } else {
                            operation.prioritizedContentTypes = prioritizeContentTypes(operation.consumes);
                        }
                    }
                    if (operation.returnType == null) {
                        operation.returnType = "Void";
                    }
                    if (this.usesRetrofit2Library() && StringUtils.isNotEmpty(operation.path)
                            && operation.path.startsWith("/")) {
                        operation.path = operation.path.substring(1);
                    }

                    // sorting operation parameters to make sure path params are parsed before query params
                    if (operation.allParams != null) {
                        sort(operation.allParams, (one, another) -> {
                            if (getBooleanValue(one, CodegenConstants.IS_PATH_PARAM_EXT_NAME)
                                    && getBooleanValue(another, CodegenConstants.IS_QUERY_PARAM_EXT_NAME)) {
                                return -1;
                            }
                            if (getBooleanValue(one, CodegenConstants.IS_QUERY_PARAM_EXT_NAME)
                                    && getBooleanValue(another, CodegenConstants.IS_PATH_PARAM_EXT_NAME)) {
                                return 1;
                            }

                            return 0;
                        });
                        final Iterator<CodegenParameter> iterator = operation.allParams.iterator();
                        while (iterator.hasNext()) {
                            final CodegenParameter param = iterator.next();
                            param.getVendorExtensions().put(CodegenConstants.HAS_MORE_EXT_NAME, iterator.hasNext());
                        }
                    }
                }
            }

        }

        // camelize path variables for Feign client
        if ("feign".equals(this.getLibrary())) {
            final Map<String, Object> operations = (Map<String, Object>) objs.get("operations");
            final List<CodegenOperation> operationList = (List<CodegenOperation>) operations.get("operation");
            for (final CodegenOperation op : operationList) {
                final String path = op.path;
                final String[] items = path.split("/", -1);

                for (int i = 0; i < items.length; ++i) {
                    if (items[i].matches("^\\{(.*)\\}$")) { // wrap in {}
                        // camelize path variable
                        items[i] = "{" + camelize(items[i].substring(1, items[i].length() - 1), true) + "}";
                    }
                }
                op.path = StringUtils.join(items, "/");
            }
        }

        return objs;
    }

    @Override
    public String apiFilename(final String templateName, final String tag) {
        return super.apiFilename(templateName, tag);
    }

    /**
     * Prioritizes consumes mime-type list by moving json-vendor and json mime-types up front, but otherwise preserves
     * original consumes definition order. [application/vnd...+json,... application/json, ..as is..]
     * @param consumes consumes mime-type list
     * @return
     */
    static List<Map<String, String>> prioritizeContentTypes(final List<Map<String, String>> consumes) {
        if (consumes.size() <= 1) {
            return consumes;
        }

        final List<Map<String, String>> prioritizedContentTypes = new ArrayList<>(consumes.size());

        final List<Map<String, String>> jsonVendorMimeTypes = new ArrayList<>(consumes.size());
        final List<Map<String, String>> jsonMimeTypes = new ArrayList<>(consumes.size());

        for (final Map<String, String> consume : consumes) {
            if (isJsonVendorMimeType(consume.get(MEDIA_TYPE))) {
                jsonVendorMimeTypes.add(consume);
            } else if (isJsonMimeType(consume.get(MEDIA_TYPE))) {
                jsonMimeTypes.add(consume);
            } else {
                prioritizedContentTypes.add(consume);
            }

            consume.put("hasMore", "true");
        }

        prioritizedContentTypes.addAll(0, jsonMimeTypes);
        prioritizedContentTypes.addAll(0, jsonVendorMimeTypes);

        prioritizedContentTypes.get(prioritizedContentTypes.size() - 1).put("hasMore", null);

        return prioritizedContentTypes;
    }

    private static boolean isMultipartType(final List<Map<String, String>> consumes) {
        final Map<String, String> firstType = consumes.get(0);
        if ((firstType != null) && "multipart/form-data".equals(firstType.get(MEDIA_TYPE))) {
            return true;
        }
        return false;
    }

    @Override
    public void postProcessModelProperty(final CodegenModel model, final CodegenProperty property) {
        super.postProcessModelProperty(model, property);
        final boolean isEnum = getBooleanValue(model, IS_ENUM_EXT_NAME);
        if (!BooleanUtils.toBoolean(isEnum)) {
            // final String lib = getLibrary();
            // Needed imports for Jackson based libraries
            if (this.additionalProperties.containsKey("jackson")) {
                model.imports.add("JsonProperty");
                model.imports.add("JsonValue");
            }
            if (this.additionalProperties.containsKey("gson")) {
                model.imports.add("SerializedName");
                model.imports.add("TypeAdapter");
                model.imports.add("JsonAdapter");
                model.imports.add("JsonReader");
                model.imports.add("JsonWriter");
                model.imports.add("IOException");
            }
        } else { // enum class
            // Needed imports for Jackson's JsonCreator
            if (this.additionalProperties.containsKey("jackson")) {
                model.imports.add("JsonValue");
                model.imports.add("JsonCreator");
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> postProcessAllModels(final Map<String, Object> objs) {
        final Map<String, Object> allProcessedModels = super.postProcessAllModels(objs);
        if (!this.additionalProperties.containsKey("gsonFactoryMethod")) {
            final List<Object> allModels = new ArrayList<>();
            for (final String name : allProcessedModels.keySet()) {
                final Map<String, Object> models = (Map<String, Object>) allProcessedModels.get(name);
                try {
                    allModels.add(((List<Object>) models.get("models")).get(0));
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }
            this.additionalProperties.put("parent", this.modelInheritanceSupportInGson(allModels));
        }
        return allProcessedModels;
    }

    @Override
    public Map<String, Object> postProcessModelsEnum(Map<String, Object> objs) {
        objs = super.postProcessModelsEnum(objs);
        // Needed import for Gson based libraries
        if (this.additionalProperties.containsKey("gson")) {
            final List<Map<String, String>> imports = (List<Map<String, String>>) objs.get("imports");
            final List<Object> models = (List<Object>) objs.get("models");
            for (final Object _mo : models) {
                final Map<String, Object> mo = (Map<String, Object>) _mo;
                final CodegenModel cm = (CodegenModel) mo.get("model");
                // for enum model
                final boolean isEnum = getBooleanValue(cm, IS_ENUM_EXT_NAME);
                if (Boolean.TRUE.equals(isEnum) && cm.allowableValues != null) {
                    cm.imports.add(this.importMapping.get("SerializedName"));
                    final Map<String, String> item = new HashMap<>();
                    item.put("import", this.importMapping.get("SerializedName"));
                    imports.add(item);
                }
            }
        }
        return objs;
    }

    @Override
    public String getArgumentsLocation() {
        return "/arguments/java.yaml";
    }

    @Override
    public String getDefaultTemplateDir() {
        return "Java";
    }

    protected List<Map<String, Object>> modelInheritanceSupportInGson(final List<?> allModels) {
        final Map<CodegenModel, List<CodegenModel>> byParent = new LinkedHashMap<>();
        for (final Object model : allModels) {
            final Map entry = (Map) model;
            final CodegenModel parent = ((CodegenModel) entry.get("model")).parentModel;
            if (null != parent) {
                byParent.computeIfAbsent(parent, k -> new LinkedList<>()).add((CodegenModel) entry.get("model"));
            }
        }
        final List<Map<String, Object>> parentsList = new ArrayList<>();
        for (final Map.Entry<CodegenModel, List<CodegenModel>> parentModelEntry : byParent.entrySet()) {
            final CodegenModel parentModel = parentModelEntry.getKey();
            final List<Map<String, Object>> childrenList = new ArrayList<>();
            final Map<String, Object> parent = new HashMap<>();
            parent.put("classname", parentModel.classname);
            final List<CodegenModel> childrenModels = parentModelEntry.getValue();
            for (final CodegenModel model : childrenModels) {
                final Map<String, Object> child = new HashMap<>();
                child.put("name", model.name);
                child.put("classname", model.classname);
                childrenList.add(child);
            }
            parent.put("children", childrenList);
            parent.put("discriminator", parentModel.discriminator);
            if (parentModel.discriminator != null && parentModel.discriminator.getMapping() != null) {
                parentModel.discriminator.getMapping().replaceAll((key, value) -> OpenAPIUtil.getSimpleRef(value));
            }
            parentsList.add(parent);
        }

        return parentsList;
    }

    public void setUseRxJava(final boolean useRxJava) {
        this.useRxJava = useRxJava;
        this.doNotUseRx = false;
    }

    public void setUseRxJava2(final boolean useRxJava2) {
        this.useRxJava2 = useRxJava2;
        this.doNotUseRx = false;
    }

    public void setUseRxJava3(final boolean useRxJava3) {
        this.useRxJava3 = useRxJava3;
        this.doNotUseRx = false;
    }

    public void setDoNotUseRx(final boolean doNotUseRx) {
        this.doNotUseRx = doNotUseRx;
    }

    public void setUsePlayWS(final boolean usePlayWS) {
        this.usePlayWS = usePlayWS;
    }

    public void setPlayVersion(final String playVersion) {
        this.playVersion = playVersion;
    }

    public void setParcelableModel(final boolean parcelableModel) {
        this.parcelableModel = parcelableModel;
    }

    @Override
    public void setUseBeanValidation(final boolean useBeanValidation) {
        this.useBeanValidation = useBeanValidation;
    }

    @Override
    public void setPerformBeanValidation(final boolean performBeanValidation) {
        this.performBeanValidation = performBeanValidation;
    }

    @Override
    public void setUseGzipFeature(final boolean useGzipFeature) {
        this.useGzipFeature = useGzipFeature;
    }

    public void setUseRuntimeException(final boolean useRuntimeException) {
        this.useRuntimeException = useRuntimeException;
    }

    final private static Pattern JSON_MIME_PATTERN = Pattern.compile("(?i)application\\/json(;.*)?");

    final private static Pattern JSON_VENDOR_MIME_PATTERN = Pattern.compile("(?i)application\\/vnd.(.*)+json(;.*)?");

    /**
     * Check if the given MIME is a JSON MIME. JSON MIME examples: application/json application/json; charset=UTF8
     * APPLICATION/JSON
     */
    static boolean isJsonMimeType(final String mime) {
        return mime != null && (JSON_MIME_PATTERN.matcher(mime).matches());
    }

    /**
     * Check if the given MIME is a JSON Vendor MIME. JSON MIME examples: application/vnd.mycompany+json
     * application/vnd.mycompany.resourceA.version1+json
     */
    static boolean isJsonVendorMimeType(final String mime) {
        return mime != null && JSON_VENDOR_MIME_PATTERN.matcher(mime).matches();
    }

    @Override
    public void setNotNullJacksonAnnotation(final boolean notNullJacksonAnnotation) {
        this.notNullJacksonAnnotation = true;
    }

    @Override
    public boolean isNotNullJacksonAnnotation() {
        return true;
    }
}
