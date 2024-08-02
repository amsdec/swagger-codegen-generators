package io.swagger.codegen.v3.generators;

import static io.swagger.codegen.v3.CodegenConstants.HAS_ONLY_READ_ONLY_EXT_NAME;
import static io.swagger.codegen.v3.CodegenConstants.HAS_OPTIONAL_EXT_NAME;
import static io.swagger.codegen.v3.CodegenConstants.HAS_REQUIRED_EXT_NAME;
import static io.swagger.codegen.v3.CodegenConstants.IS_ARRAY_MODEL_EXT_NAME;
import static io.swagger.codegen.v3.CodegenConstants.IS_CONTAINER_EXT_NAME;
import static io.swagger.codegen.v3.CodegenConstants.IS_ENUM_EXT_NAME;
import static io.swagger.codegen.v3.generators.CodegenHelper.getDefaultIncludes;
import static io.swagger.codegen.v3.generators.CodegenHelper.getImportMappings;
import static io.swagger.codegen.v3.generators.CodegenHelper.getTypeMappings;
import static io.swagger.codegen.v3.generators.CodegenHelper.initalizeSpecialCharacterMapping;
import static io.swagger.codegen.v3.generators.handlebars.ExtensionHelper.getBooleanValue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.jknack.handlebars.Handlebars;
import com.samskivert.mustache.Mustache;

import io.swagger.codegen.v3.CliOption;
import io.swagger.codegen.v3.CodegenArgument;
import io.swagger.codegen.v3.CodegenConfig;
import io.swagger.codegen.v3.CodegenConstants;
import io.swagger.codegen.v3.CodegenContent;
import io.swagger.codegen.v3.CodegenModel;
import io.swagger.codegen.v3.CodegenModelFactory;
import io.swagger.codegen.v3.CodegenModelType;
import io.swagger.codegen.v3.CodegenOperation;
import io.swagger.codegen.v3.CodegenParameter;
import io.swagger.codegen.v3.CodegenProperty;
import io.swagger.codegen.v3.CodegenResponse;
import io.swagger.codegen.v3.CodegenSecurity;
import io.swagger.codegen.v3.ISchemaHandler;
import io.swagger.codegen.v3.SupportingFile;
import io.swagger.codegen.v3.generators.examples.ExampleGenerator;
import io.swagger.codegen.v3.generators.handlebars.BaseItemsHelper;
import io.swagger.codegen.v3.generators.handlebars.BracesHelper;
import io.swagger.codegen.v3.generators.handlebars.HasHelper;
import io.swagger.codegen.v3.generators.handlebars.HasNotHelper;
import io.swagger.codegen.v3.generators.handlebars.IsHelper;
import io.swagger.codegen.v3.generators.handlebars.IsNotHelper;
import io.swagger.codegen.v3.generators.handlebars.NotEmptyHelper;
import io.swagger.codegen.v3.generators.handlebars.StringUtilHelper;
import io.swagger.codegen.v3.generators.util.OpenAPIUtil;
import io.swagger.codegen.v3.templates.HandlebarTemplateEngine;
import io.swagger.codegen.v3.templates.MustacheTemplateEngine;
import io.swagger.codegen.v3.templates.TemplateEngine;
import io.swagger.codegen.v3.utils.ModelUtils;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BinarySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.ByteArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.DateSchema;
import io.swagger.v3.oas.models.media.DateTimeSchema;
import io.swagger.v3.oas.models.media.EmailSchema;
import io.swagger.v3.oas.models.media.FileSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.media.UUIDSchema;
import io.swagger.v3.oas.models.parameters.CookieParameter;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.PathParameter;
import io.swagger.v3.oas.models.parameters.QueryParameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.parser.util.SchemaTypeUtil;

public abstract class DefaultCodegenConfig implements CodegenConfig {

    protected static final Logger LOGGER = LoggerFactory.getLogger(DefaultCodegenConfig.class);

    public static final String DEFAULT_CONTENT_TYPE = "application/json";

    public static final String REQUEST_BODY_NAME = "body";

    public static final String DEFAULT_TEMPLATE_DIR = "handlebars";

    public static final String IS_NULLABLE_FALSE = "x-nullable-false";

    public static final String IS_NULLABLE_TRUE = "x-nullable-true";

    protected OpenAPI openAPI;

    protected OpenAPI unflattenedOpenAPI;

    protected String inputSpec;

    protected String inputURL;

    protected String outputFolder = StringUtils.EMPTY;

    protected Set<String> defaultIncludes = new HashSet<>();

    protected Map<String, String> typeMapping = new HashMap<>();

    protected Map<String, String> instantiationTypes = new HashMap<>();

    protected Set<String> reservedWords = new HashSet<>();

    protected Set<String> languageSpecificPrimitives = new HashSet<>();

    protected Map<String, String> importMapping = new HashMap<>();

    protected String modelPackage = StringUtils.EMPTY;

    protected String apiPackage = StringUtils.EMPTY;

    protected String fileSuffix;

    protected String modelNamePrefix = StringUtils.EMPTY;

    protected String modelNameSuffix = StringUtils.EMPTY;

    protected String testPackage = StringUtils.EMPTY;

    protected Map<String, String> apiTemplateFiles = new HashMap<>();

    protected Map<String, String> modelTemplateFiles = new HashMap<>();

    protected Map<String, String> apiTestTemplateFiles = new HashMap<>();

    protected Map<String, String> modelTestTemplateFiles = new HashMap<>();

    protected Map<String, String> apiDocTemplateFiles = new HashMap<>();

    protected Map<String, String> modelDocTemplateFiles = new HashMap<>();

    protected Map<String, String> reservedWordsMappings = new HashMap<>();

    protected String templateDir;

    protected String customTemplateDir;

    protected String templateVersion;

    protected String embeddedTemplateDir;

    protected String commonTemplateDir = "_common";

    protected Map<String, Object> additionalProperties = new HashMap<>();

    protected Map<String, Object> vendorExtensions = new HashMap<>();

    protected List<SupportingFile> supportingFiles = new ArrayList<>();

    protected List<CliOption> cliOptions = new ArrayList<>();

    protected List<CodegenArgument> languageArguments;

    protected boolean skipOverwrite;

    protected boolean removeOperationIdPrefix;

    protected boolean supportsInheritance;

    protected boolean supportsMixins;

    protected Map<String, String> supportedLibraries = new LinkedHashMap<>();

    protected String library;

    protected Boolean sortParamsByRequiredFlag = false;

    protected Boolean ensureUniqueParams = true;

    protected Boolean allowUnicodeIdentifiers = false;

    protected String gitUserId, gitRepoId, releaseNote, gitRepoBaseURL;

    protected String httpUserAgent;

    protected Boolean hideGenerationTimestamp = true;

    protected TemplateEngine templateEngine = new HandlebarTemplateEngine(this);

    // How to encode special characters like $
    // They are translated to words like "Dollar" and prefixed with '
    // Then translated back during JSON encoding and decoding
    protected Map<String, String> specialCharReplacements = new HashMap<>();

    // When a model is an alias for a simple type
    protected Map<String, String> typeAliases = null;

    protected String ignoreFilePathOverride;

    protected boolean useOas2 = false;

    protected boolean ignoreImportMapping;

    @Override
    public List<CliOption> cliOptions() {
        return this.cliOptions;
    }

    @Override
    public void processOpts() {
        if (this.additionalProperties.containsKey(CodegenConstants.TEMPLATE_DIR)) {
            this.customTemplateDir = this.additionalProperties.get(CodegenConstants.TEMPLATE_DIR).toString();
        }
        this.embeddedTemplateDir = this.templateDir = this.getTemplateDir();

        if (this.additionalProperties.get(CodegenConstants.IGNORE_IMPORT_MAPPING_OPTION) != null) {
            this.setIgnoreImportMapping(Boolean.parseBoolean(
                    this.additionalProperties.get(CodegenConstants.IGNORE_IMPORT_MAPPING_OPTION).toString()));
        } else {
            this.setIgnoreImportMapping(this.defaultIgnoreImportMappingOption());
        }

        if (this.additionalProperties.containsKey(CodegenConstants.TEMPLATE_VERSION)) {
            this.setTemplateVersion((String) this.additionalProperties.get(CodegenConstants.TEMPLATE_VERSION));
        }

        if (this.additionalProperties.containsKey(CodegenConstants.MODEL_PACKAGE)) {
            this.setModelPackage((String) this.additionalProperties.get(CodegenConstants.MODEL_PACKAGE));
        } else if (StringUtils.isNotEmpty(this.modelPackage)) {
            // not set in additionalProperties, add value from CodegenConfig in order to use it in templates
            this.additionalProperties.put(CodegenConstants.MODEL_PACKAGE, this.modelPackage);
        }

        if (this.additionalProperties.containsKey(CodegenConstants.API_PACKAGE)) {
            this.setApiPackage((String) this.additionalProperties.get(CodegenConstants.API_PACKAGE));
        } else if (StringUtils.isNotEmpty(this.apiPackage)) {
            // not set in additionalProperties, add value from CodegenConfig in order to use it in templates
            this.additionalProperties.put(CodegenConstants.API_PACKAGE, this.apiPackage);
        }

        if (this.additionalProperties.containsKey(CodegenConstants.SORT_PARAMS_BY_REQUIRED_FLAG)) {
            this.setSortParamsByRequiredFlag(Boolean
                    .valueOf(this.additionalProperties.get(CodegenConstants.SORT_PARAMS_BY_REQUIRED_FLAG).toString()));
        } else if (this.sortParamsByRequiredFlag != null) {
            // not set in additionalProperties, add value from CodegenConfig in order to use it in templates
            this.additionalProperties.put(CodegenConstants.SORT_PARAMS_BY_REQUIRED_FLAG, this.sortParamsByRequiredFlag);
        }

        if (this.additionalProperties.containsKey(CodegenConstants.ENSURE_UNIQUE_PARAMS)) {
            this.setEnsureUniqueParams(
                    Boolean.valueOf(this.additionalProperties.get(CodegenConstants.ENSURE_UNIQUE_PARAMS).toString()));
        }

        if (this.additionalProperties.containsKey(CodegenConstants.ALLOW_UNICODE_IDENTIFIERS)) {
            this.setAllowUnicodeIdentifiers(Boolean
                    .valueOf(this.additionalProperties.get(CodegenConstants.ALLOW_UNICODE_IDENTIFIERS).toString()));
        }

        if (this.additionalProperties.containsKey(CodegenConstants.MODEL_NAME_PREFIX)) {
            this.setModelNamePrefix((String) this.additionalProperties.get(CodegenConstants.MODEL_NAME_PREFIX));
        }

        if (this.additionalProperties.containsKey(CodegenConstants.MODEL_NAME_SUFFIX)) {
            this.setModelNameSuffix((String) this.additionalProperties.get(CodegenConstants.MODEL_NAME_SUFFIX));
        }

        if (this.additionalProperties.containsKey(CodegenConstants.REMOVE_OPERATION_ID_PREFIX)) {
            this.setRemoveOperationIdPrefix(Boolean.parseBoolean(
                    this.additionalProperties.get(CodegenConstants.REMOVE_OPERATION_ID_PREFIX).toString()));
        }

        if (this.additionalProperties.containsKey(CodegenConstants.HIDE_GENERATION_TIMESTAMP)) {
            this.setHideGenerationTimestamp(Boolean
                    .valueOf(this.additionalProperties.get(CodegenConstants.HIDE_GENERATION_TIMESTAMP).toString()));
        } else if (this.hideGenerationTimestamp != null) {
            // not set in additionalProperties, add value from CodegenConfig in order to use it in templates
            this.additionalProperties.put(CodegenConstants.HIDE_GENERATION_TIMESTAMP, this.hideGenerationTimestamp);
        }

        if (this.additionalProperties.containsKey(CodegenConstants.USE_OAS2)) {
            this.setUseOas2(Boolean.parseBoolean(this.additionalProperties.get(CodegenConstants.USE_OAS2).toString()));
        }

        this.setTemplateEngine();
    }

    @Override
    public Map<String, Object> postProcessAllModels(final Map<String, Object> processedModels) {
        // Index all CodegenModels by model name.
        final Map<String, CodegenModel> allModels = new HashMap<>();
        for (final Map.Entry<String, Object> entry : processedModels.entrySet()) {
            final String modelName = this.toModelName(entry.getKey());
            final Map<String, Object> inner = (Map<String, Object>) entry.getValue();
            final List<Map<String, Object>> models = (List<Map<String, Object>>) inner.get("models");
            for (final Map<String, Object> mo : models) {
                final CodegenModel codegenModel = (CodegenModel) mo.get("model");
                allModels.put(modelName, codegenModel);
            }
        }
        this.postProcessAllCodegenModels(allModels);
        return processedModels;
    }

    protected void postProcessAllCodegenModels(final Map<String, CodegenModel> allModels) {
        if (this.supportsInheritance) {
            for (final String name : allModels.keySet()) {
                final CodegenModel codegenModel = allModels.get(name);
                this.fixUpParentAndInterfaces(codegenModel, allModels);
            }
        }
    }

    /**
     * Fix up all parent and interface CodegenModel references.
     * @param allModels
     */
    protected void fixUpParentAndInterfaces(final CodegenModel codegenModel,
            final Map<String, CodegenModel> allModels) {
        if (codegenModel.parent != null) {
            codegenModel.parentModel = allModels.get(codegenModel.parent);
        }
        if (codegenModel.interfaces != null && !codegenModel.interfaces.isEmpty()) {
            codegenModel.interfaceModels = new ArrayList<>(codegenModel.interfaces.size());
            for (final String intf : codegenModel.interfaces) {
                final CodegenModel intfModel = allModels.get(intf);
                if (intfModel != null) {
                    codegenModel.interfaceModels.add(intfModel);
                }
            }
        }
        CodegenModel parent = codegenModel.parentModel;
        // if a discriminator exists on the parent, don't add this child to the inheritance hierarchy
        // TODO Determine what to do if the parent discriminator name == the grandparent discriminator name
        while (parent != null) {
            if (parent.children == null) {
                parent.children = new ArrayList<>();
            }
            parent.children.add(codegenModel);
            if (parent.discriminator == null) {
                parent = allModels.get(parent.parent);
            } else {
                parent = null;
            }
        }
    }

    // override with any special post-processing
    @Override
    @SuppressWarnings("static-method")
    public Map<String, Object> postProcessModels(final Map<String, Object> objs) {
        return objs;
    }

    /**
     * post process enum defined in model's properties
     * @param objs Map of models
     * @return maps of models with better enum support
     */
    public Map<String, Object> postProcessModelsEnum(final Map<String, Object> objs) {
        this.processModelEnums(objs);
        return objs;
    }

    public void processModelEnums(final Map<String, Object> objs) {
        final List<Object> models = (List<Object>) objs.get("models");
        for (final Object _mo : models) {
            final Map<String, Object> mo = (Map<String, Object>) _mo;
            final CodegenModel cm = (CodegenModel) mo.get("model");

            // for enum model
            final boolean isEnum = getBooleanValue(cm, IS_ENUM_EXT_NAME);
            if (Boolean.TRUE.equals(isEnum) && cm.allowableValues != null) {
                final Map<String, Object> allowableValues = cm.allowableValues;
                final List<Object> values = (List<Object>) allowableValues.get("values");
                final List<Map<String, String>> enumVars = new ArrayList<>();
                final String commonPrefix = this.findCommonPrefixOfVars(values);
                final int truncateIdx = commonPrefix.length();
                for (final Object value : values) {
                    final Map<String, String> enumVar = new HashMap<>();
                    final String enumName = this.findEnumName(truncateIdx, value);
                    enumVar.put("name", this.toEnumVarName(enumName, cm.dataType));
                    if (value == null) {
                        enumVar.put("value", this.toEnumValue(null, cm.dataType));
                    } else {
                        enumVar.put("value", this.toEnumValue(value.toString(), cm.dataType));
                    }
                    enumVars.add(enumVar);
                }
                cm.allowableValues.put("enumVars", enumVars);
            }
            this.updateCodegenModelEnumVars(cm);
        }
    }

    public boolean isPrimivite(final String datatype) {
        return "number".equalsIgnoreCase(datatype) || "integer".equalsIgnoreCase(datatype)
                || "boolean".equalsIgnoreCase(datatype);
    }

    /**
     * update codegen property enum with proper naming convention and handling of numbers, special characters
     * @param codegenModel
     */
    protected void updateCodegenModelEnumVars(final CodegenModel codegenModel) {
        for (final CodegenProperty var : codegenModel.vars) {
            this.updateCodegenPropertyEnum(var);
        }
    }

    private String findEnumName(final int truncateIdx, final Object value) {
        if (value == null) {
            return "null";
        }
        String enumName;
        if (truncateIdx == 0) {
            enumName = value.toString();
        } else {
            enumName = value.toString().substring(truncateIdx);
            if ("".equals(enumName)) {
                enumName = value.toString();
            }
        }
        return enumName;
    }

    /**
     * Returns the common prefix of variables for enum naming if two or more variables are present.
     * @param vars List of variable names
     * @return the common prefix for naming
     */
    public String findCommonPrefixOfVars(final List<Object> vars) {
        if (vars.size() > 1) {
            try {
                final String[] listStr = vars.toArray(new String[vars.size()]);
                final String prefix = StringUtils.getCommonPrefix(listStr);
                // exclude trailing characters that should be part of a valid variable
                // e.g. ["status-on", "status-off"] => "status-" (not "status-o")
                return prefix.replaceAll("[a-zA-Z0-9]+\\z", "");
            } catch (final ArrayStoreException e) {
                // do nothing, just return default value
            }
        }
        return "";
    }

    /**
     * Return the enum default value in the language specified format
     * @param value enum variable name
     * @param datatype data type
     * @return the default value for the enum
     */
    public String toEnumDefaultValue(final String value, final String datatype) {
        return datatype + "." + value;
    }

    /**
     * Return the enum value in the language specified format e.g. status becomes "status"
     * @param value enum variable name
     * @param datatype data type
     * @return the sanitized value for enum
     */
    public String toEnumValue(final String value, final String datatype) {
        if (value == null) {
            return null;
        }
        if ("number".equalsIgnoreCase(datatype)) {
            return value;
        }
        return "\"" + this.escapeText(value) + "\"";
    }

    /**
     * Return the sanitized variable name for enum
     * @param value enum variable name
     * @param datatype data type
     * @return the sanitized variable name for enum
     */
    public String toEnumVarName(final String value, final String datatype) {
        return ModelUtils.toEnumVarName(value);
    }

    // override with any special post-processing
    @Override
    @SuppressWarnings("static-method")
    public Map<String, Object> postProcessOperations(final Map<String, Object> objs) {
        return objs;
    }

    // override with any special post-processing
    @Override
    @SuppressWarnings("static-method")
    public Map<String, Object> postProcessOperationsWithModels(final Map<String, Object> objs,
            final List<Object> allModels) {
        return objs;
    }

    // override with any special post-processing
    @Override
    @SuppressWarnings("static-method")
    public Map<String, Object> postProcessSupportingFileData(final Map<String, Object> objs) {
        return objs;
    }

    // override to post-process any model properties
    @Override
    @SuppressWarnings("unused")
    public void postProcessModelProperty(final CodegenModel model, final CodegenProperty property) {
    }

    // override to post-process any parameters
    @Override
    @SuppressWarnings("unused")
    public void postProcessParameter(final CodegenParameter parameter) {
    }

    @Override
    public void preprocessOpenAPI(final OpenAPI openAPI) {
        this.openAPI = openAPI;
    }

    @Override
    public void processOpenAPI(final OpenAPI openAPI) {
    }

    @Override
    public Mustache.Compiler processCompiler(final Mustache.Compiler compiler) {
        return compiler;
    }

    @Override
    public TemplateEngine getTemplateEngine() {
        return this.templateEngine;
    }

    // override with any special text escaping logic
    @Override
    @SuppressWarnings("static-method")
    public String escapeText(final String input) {
        if (input == null) {
            return input;
        }

        // remove \t, \n, \r
        // replace \ with \\
        // replace " with \"
        // outter unescape to retain the original multi-byte characters
        // finally escalate characters avoiding code injection
        return this.escapeUnsafeCharacters(
                StringEscapeUtils.unescapeJava(StringEscapeUtils.escapeJava(input).replace("\\/", "/"))
                        .replaceAll("[\\t\\n\\r]", " ").replace("\\", "\\\\").replace("\"", "\\\""));
    }

    /**
     * override with any special text escaping logic to handle unsafe characters so as to avoid code injection
     * @param input String to be cleaned up
     * @return string with unsafe characters removed or escaped
     */
    @Override
    public String escapeUnsafeCharacters(final String input) {
        LOGGER.warn("escapeUnsafeCharacters should be overridden in the code generator with proper logic to escape "
                + "unsafe characters");
        // doing nothing by default and code generator should implement
        // the logic to prevent code injection
        // later we'll make this method abstract to make sure
        // code generator implements this method
        return input;
    }

    /**
     * Escape single and/or double quote to avoid code injection
     * @param input String to be cleaned up
     * @return string with quotation mark removed or escaped
     */
    @Override
    public String escapeQuotationMark(final String input) {
        LOGGER.warn("escapeQuotationMark should be overridden in the code generator with proper logic to escape "
                + "single/double quote");
        return input.replace("\"", "\\\"");
    }

    @Override
    public Set<String> defaultIncludes() {
        return this.defaultIncludes;
    }

    @Override
    public Map<String, String> typeMapping() {
        return this.typeMapping;
    }

    @Override
    public Map<String, String> instantiationTypes() {
        return this.instantiationTypes;
    }

    @Override
    public Set<String> reservedWords() {
        return this.reservedWords;
    }

    @Override
    public Set<String> languageSpecificPrimitives() {
        return this.languageSpecificPrimitives;
    }

    @Override
    public Map<String, String> importMapping() {
        return this.importMapping;
    }

    @Override
    public String testPackage() {
        return this.testPackage;
    }

    @Override
    public String modelPackage() {
        return this.modelPackage;
    }

    @Override
    public String apiPackage() {
        return this.apiPackage;
    }

    @Override
    public String fileSuffix() {
        return this.fileSuffix;
    }

    @Override
    public String templateDir() {
        return this.templateDir;
    }

    @Override
    public String embeddedTemplateDir() {
        if (this.embeddedTemplateDir != null) {
            return this.embeddedTemplateDir;
        }
        return this.templateDir;
    }

    @Override
    public String customTemplateDir() {
        return this.customTemplateDir;
    }

    @Override
    public String getCommonTemplateDir() {
        return this.commonTemplateDir;
    }

    public void setCommonTemplateDir(final String commonTemplateDir) {
        this.commonTemplateDir = commonTemplateDir;
    }

    @Override
    public Map<String, String> apiDocTemplateFiles() {
        return this.apiDocTemplateFiles;
    }

    @Override
    public Map<String, String> modelDocTemplateFiles() {
        return this.modelDocTemplateFiles;
    }

    @Override
    public Map<String, String> reservedWordsMappings() {
        return this.reservedWordsMappings;
    }

    @Override
    public Map<String, String> apiTestTemplateFiles() {
        return this.apiTestTemplateFiles;
    }

    @Override
    public Map<String, String> modelTestTemplateFiles() {
        return this.modelTestTemplateFiles;
    }

    @Override
    public Map<String, String> apiTemplateFiles() {
        return this.apiTemplateFiles;
    }

    @Override
    public Map<String, String> modelTemplateFiles() {
        return this.modelTemplateFiles;
    }

    @Override
    public String apiFileFolder() {
        return this.outputFolder + File.separator + this.apiPackage().replace('.', File.separatorChar);
    }

    @Override
    public String modelFileFolder() {
        return this.outputFolder + File.separator + this.modelPackage().replace('.', File.separatorChar);
    }

    @Override
    public String apiTestFileFolder() {
        return this.outputFolder + File.separator + this.testPackage().replace('.', File.separatorChar);
    }

    @Override
    public String modelTestFileFolder() {
        return this.outputFolder + File.separator + this.testPackage().replace('.', File.separatorChar);
    }

    @Override
    public String apiDocFileFolder() {
        return this.outputFolder;
    }

    @Override
    public String modelDocFileFolder() {
        return this.outputFolder;
    }

    @Override
    public Map<String, Object> additionalProperties() {
        return this.additionalProperties;
    }

    @Override
    public Map<String, Object> vendorExtensions() {
        return this.vendorExtensions;
    }

    @Override
    public List<SupportingFile> supportingFiles() {
        return this.supportingFiles;
    }

    @Override
    public String outputFolder() {
        return this.outputFolder;
    }

    @Override
    public void setOutputDir(final String dir) {
        this.outputFolder = dir;
    }

    @Override
    public String getOutputDir() {
        return this.outputFolder();
    }

    @Override
    public String getInputSpec() {
        return this.inputSpec;
    }

    @Override
    public void setInputSpec(final String inputSpec) {
        this.inputSpec = inputSpec;
    }

    @Override
    public String getInputURL() {
        return this.inputURL;
    }

    @Override
    public void setInputURL(final String inputURL) {
        this.inputURL = inputURL;
    }

    public void setTemplateDir(final String templateDir) {
        this.templateDir = templateDir;
    }

    @Override
    public String getTemplateVersion() {
        return this.templateVersion;
    }

    public void setTemplateVersion(final String templateVersion) {
        this.templateVersion = templateVersion;
    }

    public void setModelPackage(final String modelPackage) {
        this.modelPackage = modelPackage;
    }

    public void setModelNamePrefix(final String modelNamePrefix) {
        this.modelNamePrefix = modelNamePrefix;
    }

    public void setModelNameSuffix(final String modelNameSuffix) {
        this.modelNameSuffix = modelNameSuffix;
    }

    public void setApiPackage(final String apiPackage) {
        this.apiPackage = apiPackage;
    }

    public Boolean getSortParamsByRequiredFlag() {
        return false;
    }

    public void setSortParamsByRequiredFlag(final Boolean sortParamsByRequiredFlag) {
        this.sortParamsByRequiredFlag = false;
    }

    public void setEnsureUniqueParams(final Boolean ensureUniqueParams) {
        this.ensureUniqueParams = ensureUniqueParams;
    }

    public void setAllowUnicodeIdentifiers(final Boolean allowUnicodeIdentifiers) {
        this.allowUnicodeIdentifiers = allowUnicodeIdentifiers;
    }

    /**
     * Return the regular expression/JSON schema pattern
     * (http://json-schema.org/latest/json-schema-validation.html#anchor33)
     * @param pattern the pattern (regular expression)
     * @return properly-escaped pattern
     */
    public String toRegularExpression(final String pattern) {
        return this.addRegularExpressionDelimiter(this.escapeText(pattern));
    }

    /**
     * Return the file name of the Api Test
     * @param name the file name of the Api
     * @return the file name of the Api
     */
    @Override
    public String toApiFilename(final String name) {
        return this.toApiName(name);
    }

    /**
     * Return the file name of the Api Documentation
     * @param name the file name of the Api
     * @return the file name of the Api
     */
    @Override
    public String toApiDocFilename(final String name) {
        return this.toApiName(name);
    }

    /**
     * Return the file name of the Api Test
     * @param name the file name of the Api
     * @return the file name of the Api
     */
    @Override
    public String toApiTestFilename(final String name) {
        return this.toApiName(name) + "Test";
    }

    /**
     * Return the variable name in the Api
     * @param name the varible name of the Api
     * @return the snake-cased variable name
     */
    @Override
    public String toApiVarName(final String name) {
        return this.snakeCase(name);
    }

    /**
     * Return the capitalized file name of the model
     * @param name the model name
     * @return the file name of the model
     */
    @Override
    public String toModelFilename(final String name) {
        return this.initialCaps(name);
    }

    /**
     * Return the capitalized file name of the model test
     * @param name the model name
     * @return the file name of the model
     */
    @Override
    public String toModelTestFilename(final String name) {
        return this.initialCaps(name) + "Test";
    }

    /**
     * Return the capitalized file name of the model documentation
     * @param name the model name
     * @return the file name of the model
     */
    @Override
    public String toModelDocFilename(final String name) {
        return this.initialCaps(name);
    }

    /**
     * Return the operation ID (method name)
     * @param operationId operation ID
     * @return the sanitized method name
     */
    @SuppressWarnings("static-method")
    public String toOperationId(final String operationId) {
        // throw exception if method name is empty
        if (StringUtils.isEmpty(operationId)) {
            throw new RuntimeException("Empty method name (operationId) not allowed");
        }

        return operationId;
    }

    /**
     * Return the variable name by removing invalid characters and proper escaping if it's a reserved word.
     * @param name the variable name
     * @return the sanitized variable name
     */
    public String toVarName(final String name) {
        if (this.reservedWords.contains(name)) {
            return this.escapeReservedWord(name);
        }
        return name;
    }

    /**
     * Return the parameter name by removing invalid characters and proper escaping if it's a reserved word.
     * @param name Codegen property object
     * @return the sanitized parameter name
     */
    @Override
    public String toParamName(String name) {
        name = this.removeNonNameElementToCamelCase(name); // FIXME: a parameter should not be assigned. Also declare
                                                           // the methods parameters as 'final'.
        if (this.reservedWords.contains(name)) {
            return this.escapeReservedWord(name);
        }
        return name;
    }

    /**
     * Return the Enum name (e.g. StatusEnum given 'status')
     * @param property Codegen property
     * @return the Enum name
     */
    @SuppressWarnings("static-method")
    public String toEnumName(final CodegenProperty property) {
        return StringUtils.capitalize(property.name) + "Enum";
    }

    /**
     * Return the escaped name of the reserved word
     * @param name the name to be escaped
     * @return the escaped reserved word throws Runtime exception as reserved word is not allowed (default behavior)
     */
    @Override
    @SuppressWarnings("static-method")
    public String escapeReservedWord(final String name) {
        throw new RuntimeException("reserved word " + name + " not allowed");
    }

    /**
     * Return the fully-qualified "Model" name for import
     * @param name the name of the "Model"
     * @return the fully-qualified "Model" name for import
     */
    @Override
    public String toModelImport(final String name) {
        if ("".equals(this.modelPackage())) {
            return name;
        }
        return this.modelPackage() + "." + name;
    }

    /**
     * Return the fully-qualified "Api" name for import
     * @param name the name of the "Api"
     * @return the fully-qualified "Api" name for import
     */
    @Override
    public String toApiImport(final String name) {
        return this.apiPackage() + "." + name;
    }

    /**
     * Default constructor. This method will map between Swagger type and language-specified type, as well as mapping
     * between Swagger type and the corresponding import statement for the language. This will also add some language
     * specified CLI options, if any. returns string presentation of the example path (it's a constructor)
     */
    public DefaultCodegenConfig() {
        this.defaultIncludes = getDefaultIncludes();

        this.typeMapping = getTypeMappings();

        this.instantiationTypes = new HashMap<>();

        this.reservedWords = new HashSet<>();

        this.importMapping = getImportMappings();

        // we've used the .swagger-codegen-ignore approach as
        // suppportingFiles can be cleared by code generator that extends
        // the default codegen, leaving the commented code below for
        // future reference
        // supportingFiles.add(new GlobalSupportingFile("LICENSE", "LICENSE"));

        this.cliOptions
                .add(CliOption
                        .newBoolean(CodegenConstants.SORT_PARAMS_BY_REQUIRED_FLAG,
                                CodegenConstants.SORT_PARAMS_BY_REQUIRED_FLAG_DESC)
                        .defaultValue(Boolean.TRUE.toString()));
        this.cliOptions.add(
                CliOption.newBoolean(CodegenConstants.ENSURE_UNIQUE_PARAMS, CodegenConstants.ENSURE_UNIQUE_PARAMS_DESC)
                        .defaultValue(Boolean.TRUE.toString()));

        // name formatting options
        this.cliOptions.add(CliOption
                .newBoolean(CodegenConstants.ALLOW_UNICODE_IDENTIFIERS, CodegenConstants.ALLOW_UNICODE_IDENTIFIERS_DESC)
                .defaultValue(Boolean.FALSE.toString()));

        // initialize special character mapping
        initalizeSpecialCharacterMapping(this.specialCharReplacements);
    }

    /**
     * Return the symbol name of a symbol
     * @param input Symbol (e.g. $)
     * @return Symbol name (e.g. Dollar)
     */
    protected String getSymbolName(final String input) {
        return this.specialCharReplacements.get(input);
    }

    /**
     * Return the example path
     * @param path the path of the operation
     * @param operation Swagger operation object
     * @return string presentation of the example path
     */
    @Override
    @SuppressWarnings("static-method")
    public String generateExamplePath(final String path, final Operation operation) {
        final StringBuilder sb = new StringBuilder();
        sb.append(path);

        if (operation.getParameters() != null) {
            int count = 0;

            for (final Parameter param : operation.getParameters()) {
                if (param instanceof QueryParameter) {
                    final StringBuilder paramPart = new StringBuilder();
                    final QueryParameter queryParameter = (QueryParameter) param;

                    if (count == 0) {
                        paramPart.append("?");
                    } else {
                        paramPart.append(",");
                    }
                    count += 1;
                    if (!param.getRequired()) {
                        paramPart.append("[");
                    }
                    paramPart.append(param.getName()).append("=");
                    paramPart.append("{");

                    if (queryParameter.getStyle() != null) {
                        paramPart.append(param.getName()).append("1");
                        if (Parameter.StyleEnum.FORM.equals(queryParameter.getStyle())) {
                            if (queryParameter.getExplode() != null && queryParameter.getExplode()) {
                                paramPart.append(",");
                            } else {
                                paramPart.append("&").append(param.getName()).append("=");
                                paramPart.append(param.getName()).append("2");
                            }
                        } else if (Parameter.StyleEnum.PIPEDELIMITED.equals(queryParameter.getStyle())) {
                            paramPart.append("|");
                        } else if (Parameter.StyleEnum.SPACEDELIMITED.equals(queryParameter.getStyle())) {
                            paramPart.append("%20");
                        }
                    } else {
                        paramPart.append(param.getName());
                    }

                    paramPart.append("}");
                    if (!param.getRequired()) {
                        paramPart.append("]");
                    }
                    sb.append(paramPart.toString());
                }
            }
        }

        return sb.toString();
    }

    /**
     * Return the instantiation type of the property, especially for map and array
     * @param property Swagger property object
     * @return string presentation of the instantiation type of the property
     */
    public String toInstantiationType(final Schema property) {
        if (property instanceof MapSchema && hasSchemaProperties(property)) {
            final Schema additionalProperties = (Schema) property.getAdditionalProperties();
            final String type = additionalProperties.getType();
            if (null == type) {
                LOGGER.error("No Type defined for Additional Property " + additionalProperties + "\n" //
                        + "\tIn Property: " + property);
            }
            final String inner = this.getSchemaType(additionalProperties);
            return this.instantiationTypes.get("map") + "<String, " + inner + ">";
        }
        if (property instanceof MapSchema && hasTrueAdditionalProperties(property)) {
            final String inner = this.getSchemaType(new ObjectSchema());
            return this.instantiationTypes.get("map") + "<String, " + inner + ">";
        }
        if (property instanceof ArraySchema) {
            final ArraySchema arraySchema = (ArraySchema) property;
            final String inner = this.getSchemaType(arraySchema.getItems());
            return this.instantiationTypes.get("array") + "<" + inner + ">";
        }
        return null;
    }

    /**
     * Return the example value of the parameter.
     * @param p Swagger property object
     */
    public void setParameterExampleValue(final CodegenParameter p) {

    }

    /**
     * Return the example value of the property
     * @param property Schema property object
     * @return string presentation of the example value of the property
     */
    public String toExampleValue(final Schema property) {
        return String.valueOf(property.getExample());
    }

    /**
     * Return the default value of the property
     * @param property Schema property object
     * @return string presentation of the default value of the property
     */
    @SuppressWarnings("static-method")
    public String toDefaultValue(final Schema property) {
        return String.valueOf(property.getDefault());
    }

    /**
     * Return the property initialized from a data object Useful for initialization with a plain object in Javascript
     * @param name Name of the property object
     * @param property openAPI schema object
     * @return string presentation of the default value of the property
     */
    @SuppressWarnings("static-method")
    public String toDefaultValueWithParam(final String name, final Schema property) {
        return " = data." + name + ";";
    }

    /**
     * returns the swagger type for the property
     * @param property Schema property object
     * @return string presentation of the type
     **/
    @SuppressWarnings("static-method")
    public String getSchemaType(final Schema property) {
        String datatype = null;

        if (StringUtils.isNotBlank(property.get$ref())) {
            try {
                datatype = property.get$ref();
                if (datatype.indexOf("#/components/schemas/") == 0) {
                    return datatype.substring("#/components/schemas/".length());
                }
            } catch (final Exception e) {
                LOGGER.warn("Error obtaining the datatype from ref:" + property + ". Datatype default to Object");
                datatype = "Object";
                LOGGER.error(e.getMessage(), e);
            }
            return datatype;
        }

        return getTypeOfSchema(property);
    }

    private static String getTypeOfSchema(final Schema schema) {
        if (schema instanceof StringSchema && "number".equals(schema.getFormat())) {
            return "BigDecimal";
        }
        if (schema instanceof ByteArraySchema) {
            return "ByteArray";
        }
        if (schema instanceof BinarySchema) {
            return SchemaTypeUtil.BINARY_FORMAT;
        }
        if (schema instanceof FileSchema) {
            return SchemaTypeUtil.BINARY_FORMAT;
        }
        if (schema instanceof BooleanSchema) {
            return SchemaTypeUtil.BOOLEAN_TYPE;
        }
        if (schema instanceof DateSchema) {
            return SchemaTypeUtil.DATE_FORMAT;
        }
        if (schema instanceof DateTimeSchema) {
            return "DateTime";
        }
        if (schema instanceof NumberSchema) {
            if (SchemaTypeUtil.FLOAT_FORMAT.equals(schema.getFormat())) {
                return SchemaTypeUtil.FLOAT_FORMAT;
            }
            if (SchemaTypeUtil.DOUBLE_FORMAT.equals(schema.getFormat())) {
                return SchemaTypeUtil.DOUBLE_FORMAT;
            }
            return "BigDecimal";
        }
        if (schema instanceof IntegerSchema) {
            if (SchemaTypeUtil.INTEGER64_FORMAT.equals(schema.getFormat())) {
                return "long";
            }
            return "integer";
        }
        if (schema instanceof MapSchema) {
            return "map";
        }
        if (schema instanceof ObjectSchema) {
            return "object";
        }
        if (schema instanceof UUIDSchema) {
            return "UUID";
        }
        if (schema instanceof StringSchema) {
            return "string";
        }
        if (schema instanceof ComposedSchema && schema.getExtensions() != null
                && schema.getExtensions().containsKey("x-model-name")) {
            return schema.getExtensions().get("x-model-name").toString();

        }
        if (schema != null) {
            if (SchemaTypeUtil.OBJECT_TYPE.equals(schema.getType())
                    && (hasSchemaProperties(schema) || hasTrueAdditionalProperties(schema))) {
                return "map";
            }
            if (schema.getType() == null && schema.getProperties() != null && !schema.getProperties().isEmpty()) {
                return "object";
            }
            return schema.getType();
        }
        return null;
    }

    /**
     * Return the snake-case of the string
     * @param name string to be snake-cased
     * @return snake-cased string
     */
    @SuppressWarnings("static-method")
    public String snakeCase(final String name) {
        return (name.length() > 0) ? (Character.toLowerCase(name.charAt(0)) + name.substring(1)) : "";
    }

    /**
     * Capitalize the string
     * @param name string to be capitalized
     * @return capitalized string
     */
    @SuppressWarnings("static-method")
    public String initialCaps(final String name) {
        return StringUtils.capitalize(name);
    }

    /**
     * Output the type declaration of a given name
     * @param name name
     * @return a string presentation of the type
     */
    @Override
    @SuppressWarnings("static-method")
    public String getTypeDeclaration(final String name) {
        return name;
    }

    /**
     * Output the type declaration of the property
     * @param schema Schema Property object
     * @return a string presentation of the property type
     */
    @Override
    public String getTypeDeclaration(final Schema schema) {
        final String schemaType = this.getSchemaType(schema);
        if (this.typeMapping.containsKey(schemaType)) {
            return this.typeMapping.get(schemaType);
        }
        return schemaType;
    }

    /**
     * Determine the type alias for the given type if it exists. This feature is only used for Java, because the
     * language does not have a aliasing mechanism of its own.
     * @param name The type name.
     * @return The alias of the given type, if it exists. If there is no alias for this type, then returns the input
     *         type name.
     */
    public String getAlias(final String name) {
        return name;
    }

    /**
     * Output the Getter name for boolean property, e.g. getActive
     * @param name the name of the property
     * @return getter name based on naming convention
     */
    @Override
    public String toBooleanGetter(final String name) {
        return "get" + this.getterAndSetterCapitalize(name);
    }

    /**
     * Output the Getter name, e.g. getSize
     * @param name the name of the property
     * @return getter name based on naming convention
     */
    @Override
    public String toGetter(final String name) {
        return "get" + this.getterAndSetterCapitalize(name);
    }

    /**
     * Output the Getter name, e.g. getSize
     * @param name the name of the property
     * @return setter name based on naming convention
     */
    @Override
    public String toSetter(final String name) {
        return "set" + this.getterAndSetterCapitalize(name);
    }

    /**
     * Output the API (class) name (capitalized) ending with "Api" Return DefaultApi if name is empty
     * @param name the name of the Api
     * @return capitalized Api name ending with "Api"
     */
    @Override
    public String toApiName(final String name) {
        if (name.length() == 0) {
            return "DefaultApi";
        }
        return this.initialCaps(name) + "Api";
    }

    /**
     * Output the proper model name (capitalized). In case the name belongs to the TypeSystem it won't be renamed.
     * @param name the name of the model
     * @return capitalized model name
     */
    @Override
    public String toModelName(final String name) {
        return this.initialCaps(this.modelNamePrefix + name + this.modelNameSuffix);
    }

    /**
     * Convert Swagger Model object to Codegen Model object without providing all model definitions
     * @param name the name of the model
     * @param schema Schema object
     * @return Codegen Model object
     */
    @Override
    public CodegenModel fromModel(final String name, final Schema schema) {
        if (this.openAPI != null && this.openAPI.getComponents() != null
                && this.openAPI.getComponents().getSchemas() != null) {
            return this.fromModel(name, schema, this.openAPI.getComponents().getSchemas());
        }
        return this.fromModel(name, schema, null);
    }

    /**
     * Convert Swagger Model object to Codegen Model object
     * @param name the name of the model
     * @param schema Swagger Model object
     * @param allDefinitions a map of all Swagger models from the spec
     * @return Codegen Model object
     */
    @Override
    public CodegenModel fromModel(final String name, final Schema schema, final Map<String, Schema> allDefinitions) {
        if (this.typeAliases == null) {
            // Only do this once during first call
            this.typeAliases = getAllAliases(allDefinitions);
        }
        final CodegenModel codegenModel = CodegenModelFactory.newInstance(CodegenModelType.MODEL);
        if (this.reservedWords.contains(name)) {
            codegenModel.name = this.escapeReservedWord(name);
        } else {
            codegenModel.name = name;
        }
        codegenModel.title = this.escapeText(schema.getTitle());
        codegenModel.description = this.escapeText(schema.getDescription());
        codegenModel.unescapedDescription = schema.getDescription();
        codegenModel.classname = this.toModelName(name);
        codegenModel.classVarName = this.toVarName(name);
        codegenModel.classFilename = this.toModelFilename(name);
        codegenModel.modelJson = Json.pretty(schema);
        codegenModel.externalDocumentation = schema.getExternalDocs();
        if (schema.getExtensions() != null && !schema.getExtensions().isEmpty()) {
            codegenModel.getVendorExtensions().putAll(schema.getExtensions());
        }
        codegenModel.getVendorExtensions().put(CodegenConstants.IS_ALIAS_EXT_NAME, this.typeAliases.containsKey(name));

        codegenModel.discriminator = schema.getDiscriminator();

        if (schema.getXml() != null) {
            codegenModel.xmlPrefix = schema.getXml().getPrefix();
            codegenModel.xmlNamespace = schema.getXml().getNamespace();
            codegenModel.xmlName = schema.getXml().getName();
        }

        if (schema instanceof ArraySchema) {
            codegenModel.getVendorExtensions().put(IS_ARRAY_MODEL_EXT_NAME, Boolean.TRUE);
            codegenModel.getVendorExtensions().put(IS_CONTAINER_EXT_NAME, Boolean.TRUE);
            codegenModel.arrayModelType = this.fromProperty(name, schema).complexType;
            this.addParentContainer(codegenModel, name, schema);
        } else if (schema instanceof MapSchema) {
            this.processMapSchema(codegenModel, name, schema);

        } else if (schema instanceof ComposedSchema) {
            final ComposedSchema composed = (ComposedSchema) schema;
            final Map<String, Schema> properties = new LinkedHashMap<>();
            final List<String> required = new ArrayList<>();
            Map<String, Schema> allProperties;
            List<String> allRequired;
            if (this.supportsInheritance || this.supportsMixins) {
                allProperties = new LinkedHashMap<>();
                allRequired = new ArrayList<>();
                codegenModel.allVars = new ArrayList<>();
                int modelImplCnt = 0; // only one inline object allowed in a ComposedModel
                if (composed.getAllOf() != null) {
                    for (final Schema innerModel : composed.getAllOf()) {
                        if (codegenModel.discriminator == null) {
                            codegenModel.discriminator = innerModel.getDiscriminator();
                        }
                        if (innerModel.getXml() != null) {
                            codegenModel.xmlPrefix = innerModel.getXml().getPrefix();
                            codegenModel.xmlNamespace = innerModel.getXml().getNamespace();
                            codegenModel.xmlName = innerModel.getXml().getName();
                        }
                        if (modelImplCnt++ > 1) {
                            LOGGER.warn(
                                    "More than one inline schema specified in allOf:. Only the first one is recognized. All others are ignored.");
                            break; // only one ModelImpl with discriminator
                                   // allowed in allOf
                        }
                    }
                }
                if (codegenModel.discriminator != null && codegenModel.discriminator.getPropertyName() != null) {
                    codegenModel.discriminator
                            .setPropertyName(this.toVarName(codegenModel.discriminator.getPropertyName()));
                    final Map<String, String> classnameKeys = new HashMap<>();

                    if (composed.getOneOf() != null) {
                        composed.getOneOf().forEach(s -> {
                            codegenModel.discriminator.getMapping().keySet().stream()
                                    .filter(key -> codegenModel.discriminator.getMapping().get(key).equals(s.get$ref()))
                                    .forEach(key -> {
                                        final String mappingValue = codegenModel.discriminator.getMapping().get(key);
                                        if (classnameKeys.containsKey(codegenModel.classname)) {
                                            throw new IllegalArgumentException(
                                                    "Duplicate shema name in discriminator mapping");
                                        }
                                        classnameKeys.put(
                                                this.toModelName(mappingValue.replace("#/components/schemas/", "")),
                                                key);
                                    });
                        });
                        codegenModel.discriminator.getMapping().putAll(classnameKeys);
                    }
                }

            } else {
                allProperties = null;
                allRequired = null;
            }
            // parent model
            final String parentName = this.getParentName(composed);
            final Schema parent = StringUtils.isBlank(parentName) ? null : allDefinitions.get(parentName);
            final List<Schema> allOf = composed.getAllOf();
            if (allOf != null && !allOf.isEmpty()) {
                final int index = this.copyFirstAllOfProperties(allOf.get(0)) ? 0 : 1;
                for (int i = index; i < allOf.size(); i++) {
                    Schema allOfSchema = allOf.get(i);
                    if (StringUtils.isNotBlank(allOfSchema.get$ref())) {
                        final String ref = OpenAPIUtil.getSimpleRef(allOfSchema.get$ref());
                        if (allDefinitions != null) {
                            allOfSchema = allDefinitions.get(ref);
                        }
                        final String modelName = this.toModelName(ref);
                        this.addImport(codegenModel, modelName);
                    }
                    if (allDefinitions != null && allOfSchema != null) {
                        if (!this.supportsMixins) {
                            this.addProperties(properties, required, allOfSchema, allDefinitions);
                        }
                        if (this.supportsInheritance) {
                            this.addProperties(allProperties, allRequired, allOfSchema, allDefinitions);
                        }
                    }
                }
            }

            if (parent != null) {
                codegenModel.parentSchema = parentName;
                codegenModel.parent = this.typeMapping.containsKey(parentName) ? this.typeMapping.get(parentName)
                        : this.toModelName(parentName);
                this.addImport(codegenModel, codegenModel.parent);
                if (allDefinitions != null) {
                    if (this.supportsInheritance) {
                        this.addProperties(allProperties, allRequired, parent, allDefinitions);
                    } else {
                        this.addProperties(properties, required, parent, allDefinitions);
                    }
                }
            }
            this.addProperties(properties, required, composed, allDefinitions);
            if (this.supportsInheritance) {
                this.addProperties(allProperties, allRequired, composed, allDefinitions);
            }

            this.addVars(codegenModel, properties, required, allProperties, allRequired);
        } else {
            codegenModel.dataType = this.getSchemaType(schema);
            if (schema.getEnum() != null && !schema.getEnum().isEmpty()) {
                codegenModel.getVendorExtensions().put(CodegenConstants.IS_ENUM_EXT_NAME, Boolean.TRUE);
                // comment out below as allowableValues is not set in post processing model enum
                codegenModel.allowableValues = new HashMap<>();
                codegenModel.allowableValues.put("values", schema.getEnum());
                if ("BigDecimal".equals(codegenModel.dataType)) {
                    this.addImport(codegenModel, "BigDecimal");
                }
            }
            codegenModel.getVendorExtensions().put(CodegenConstants.IS_NULLABLE_EXT_NAME,
                    Boolean.TRUE.equals(schema.getNullable()));
            codegenModel.getVendorExtensions().put(IS_NULLABLE_FALSE, Boolean.FALSE.equals(schema.getNullable()));
            codegenModel.getVendorExtensions().put(IS_NULLABLE_TRUE, Boolean.TRUE.equals(schema.getNullable()));

            this.addVars(codegenModel, schema.getProperties(), schema.getRequired());
        }

        if (codegenModel.vars != null) {
            for (final CodegenProperty prop : codegenModel.vars) {
                this.postProcessModelProperty(codegenModel, prop);
            }
        }

        return codegenModel;
    }

    protected boolean copyFirstAllOfProperties(final Schema allOfSchema) {
        return StringUtils.isBlank(allOfSchema.get$ref()) && allOfSchema.getProperties() != null
                && !allOfSchema.getProperties().isEmpty();
    }

    protected void processMapSchema(final CodegenModel codegenModel, final String name, final Schema schema) {
        codegenModel.getVendorExtensions().put(CodegenConstants.IS_MAP_CONTAINER_EXT_NAME, Boolean.TRUE);
        codegenModel.getVendorExtensions().put(IS_CONTAINER_EXT_NAME, Boolean.TRUE);
        this.addParentContainer(codegenModel, name, schema);
        if (hasSchemaProperties(schema) || hasTrueAdditionalProperties(schema)) {
            this.addAdditionPropertiesToCodeGenModel(codegenModel, schema);
        }
    }

    /**
     * Recursively look for a discriminator in the interface tree
     */
    private boolean isDiscriminatorInInterfaceTree(final ComposedSchema composedSchema,
            final Map<String, Schema> allSchema) {
        if (composedSchema == null || allSchema == null || allSchema.isEmpty()) {
            return false;
        }
        if (composedSchema.getDiscriminator() != null) {
            return true;
        }
        final List<Schema> interfaces = this.getInterfaces(composedSchema);
        if (interfaces == null) {
            return false;
        }
        for (final Schema interfaceSchema : interfaces) {
            if (interfaceSchema.getDiscriminator() != null) {
                return true;
            }
        }
        return false;
    }

    protected void addAdditionPropertiesToCodeGenModel(final CodegenModel codegenModel, final Schema schema) {
        this.addParentContainer(codegenModel, codegenModel.name, schema);
    }

    protected void addProperties(final Map<String, Schema> properties, final List<String> required, final Schema schema,
            final Map<String, Schema> allSchemas) {
        if (StringUtils.isNotBlank(schema.get$ref())) {
            final Schema interfaceSchema = allSchemas.get(OpenAPIUtil.getSimpleRef(schema.get$ref()));
            this.addProperties(properties, required, interfaceSchema, allSchemas);
            return;
        }

        if (schema instanceof ComposedSchema) {
            final ComposedSchema composedSchema = (ComposedSchema) schema;
            if (!(composedSchema.getAllOf() == null || composedSchema.getAllOf().isEmpty()
                    || composedSchema.getAllOf().size() == 1)) {
                for (int i = 1; i < composedSchema.getAllOf().size(); i++) {
                    this.addProperties(properties, required, composedSchema.getAllOf().get(i), allSchemas);
                }
            }
        }

        if (schema.getProperties() != null) {
            properties.putAll(schema.getProperties());
        }
        if (schema.getRequired() != null) {
            required.addAll(schema.getRequired());
        }
    }

    /**
     * Camelize the method name of the getter and setter
     * @param name string to be camelized
     * @return Camelized string
     */
    public String getterAndSetterCapitalize(final String name) {
        if (name == null || name.length() == 0) {
            return name;
        }
        return camelize(this.toVarName(name));
    }

    /**
     * Convert Swagger Property object to Codegen Property object
     * @param name name of the property
     * @param propertySchema Schema object
     * @return Codegen Property object TODO : improve repeated code
     */
    public CodegenProperty fromProperty(final String name, final Schema propertySchema) {
        if (propertySchema == null) {
            LOGGER.error("unexpected missing property for name " + name);
            return null;
        }

        final CodegenProperty codegenProperty = CodegenModelFactory.newInstance(CodegenModelType.PROPERTY);
        codegenProperty.name = this.toVarName(name);
        codegenProperty.baseName = name;
        codegenProperty.nameInCamelCase = camelize(codegenProperty.name, false);
        codegenProperty.getter = this.toGetter(name);
        codegenProperty.setter = this.toSetter(name);
        this.setSchemaProperties(name, codegenProperty, propertySchema);

        final String type = this.getSchemaType(propertySchema);

        this.processPropertySchemaTypes(name, codegenProperty, propertySchema);

        codegenProperty.datatype = this.getTypeDeclaration(propertySchema);
        codegenProperty.dataFormat = propertySchema.getFormat();

        // this can cause issues for clients which don't support enums
        final boolean isEnum = getBooleanValue(codegenProperty, IS_ENUM_EXT_NAME);
        if (isEnum) {
            codegenProperty.datatypeWithEnum = this.toEnumName(codegenProperty);
            codegenProperty.enumName = this.toEnumName(codegenProperty);
        } else {
            codegenProperty.datatypeWithEnum = codegenProperty.datatype;
        }

        codegenProperty.baseType = this.getSchemaType(propertySchema);

        this.processPropertySchemaContainerTypes(codegenProperty, propertySchema, type);
        return codegenProperty;
    }

    protected void setSchemaProperties(final String name, final CodegenProperty codegenProperty, final Schema schema) {
        codegenProperty.description = this.escapeText(schema.getDescription());
        codegenProperty.unescapedDescription = schema.getDescription();
        codegenProperty.title = schema.getTitle();
        final String example = this.toExampleValue(schema);
        if (!"null".equals(example)) {
            codegenProperty.example = example;
        }
        codegenProperty.defaultValue = this.toDefaultValue(schema);
        codegenProperty.defaultValueWithParam = this.toDefaultValueWithParam(name, schema);
        codegenProperty.jsonSchema = Json.pretty(schema);
        codegenProperty.nullable = Boolean.TRUE.equals(schema.getNullable());
        codegenProperty.getVendorExtensions().put(CodegenConstants.IS_NULLABLE_EXT_NAME,
                Boolean.TRUE.equals(schema.getNullable()));
        codegenProperty.getVendorExtensions().put(IS_NULLABLE_FALSE, Boolean.FALSE.equals(schema.getNullable()));
        codegenProperty.getVendorExtensions().put(IS_NULLABLE_TRUE, Boolean.TRUE.equals(schema.getNullable()));
        if (schema.getReadOnly() != null) {
            codegenProperty.getVendorExtensions().put(CodegenConstants.IS_READ_ONLY_EXT_NAME, schema.getReadOnly());
        }
        if (schema.getXml() != null) {
            if (schema.getXml().getAttribute() != null) {
                codegenProperty.getVendorExtensions().put(CodegenConstants.IS_XML_ATTRIBUTE_EXT_NAME,
                        schema.getXml().getAttribute());
            }
            codegenProperty.xmlPrefix = schema.getXml().getPrefix();
            codegenProperty.xmlName = schema.getXml().getName();
            codegenProperty.xmlNamespace = schema.getXml().getNamespace();
        }
        if (schema.getExtensions() != null && !schema.getExtensions().isEmpty()) {
            codegenProperty.getVendorExtensions().putAll(schema.getExtensions());
        }
    }

    protected void processPropertySchemaTypes(final String name, final CodegenProperty codegenProperty,
            final Schema propertySchema) {
        if (propertySchema instanceof IntegerSchema) {
            codegenProperty.getVendorExtensions().put(CodegenConstants.IS_NUMERIC_EXT_NAME, Boolean.TRUE);
            if (SchemaTypeUtil.INTEGER64_FORMAT.equals(propertySchema.getFormat())) {
                codegenProperty.getVendorExtensions().put(CodegenConstants.IS_LONG_EXT_NAME, Boolean.TRUE);
            } else {
                codegenProperty.getVendorExtensions().put(CodegenConstants.IS_INTEGER_EXT_NAME, Boolean.TRUE);
            }
            this.handleMinMaxValues(propertySchema, codegenProperty);

            // check if any validation rule defined
            // exclusive* are noop without corresponding min/max
            if (codegenProperty.minimum != null || codegenProperty.maximum != null) {
                codegenProperty.getVendorExtensions().put(CodegenConstants.HAS_VALIDATION_EXT_NAME, Boolean.TRUE);
            }

            // legacy support
            final Map<String, Object> allowableValues = new HashMap<>();
            if (propertySchema.getMinimum() != null) {
                allowableValues.put("min", propertySchema.getMinimum());
            }
            if (propertySchema.getMaximum() != null) {
                allowableValues.put("max", propertySchema.getMaximum());
            }
            if (propertySchema.getEnum() != null) {
                final List<Integer> _enum = propertySchema.getEnum();
                codegenProperty._enum = new ArrayList<>();
                for (final Integer i : _enum) {
                    codegenProperty._enum.add(i.toString());
                }
                codegenProperty.getVendorExtensions().put(IS_ENUM_EXT_NAME, Boolean.TRUE);
                allowableValues.put("values", _enum);
            }
            if (allowableValues.size() > 0) {
                codegenProperty.allowableValues = allowableValues;
            }
        }
        if (propertySchema instanceof StringSchema) {
            codegenProperty.maxLength = propertySchema.getMaxLength();
            codegenProperty.minLength = propertySchema.getMinLength();
            codegenProperty.pattern = this.toRegularExpression(propertySchema.getPattern());

            // check if any validation rule defined
            if (codegenProperty.pattern != null || codegenProperty.minLength != null
                    || codegenProperty.maxLength != null) {
                codegenProperty.getVendorExtensions().put(CodegenConstants.HAS_VALIDATION_EXT_NAME, Boolean.TRUE);
            }

            codegenProperty.getVendorExtensions().put(CodegenConstants.IS_STRING_EXT_NAME, Boolean.TRUE);
            if (propertySchema.getEnum() != null) {
                final List<String> _enum = propertySchema.getEnum();
                codegenProperty._enum = _enum;
                codegenProperty.getVendorExtensions().put(IS_ENUM_EXT_NAME, Boolean.TRUE);

                // legacy support
                final Map<String, Object> allowableValues = new HashMap<>();
                allowableValues.put("values", _enum);
                codegenProperty.allowableValues = allowableValues;
            }
        }
        if (propertySchema instanceof BooleanSchema) {
            codegenProperty.getVendorExtensions().put(CodegenConstants.IS_BOOLEAN_EXT_NAME, Boolean.TRUE);
            codegenProperty.getter = this.toBooleanGetter(name);
        }
        if (propertySchema instanceof FileSchema || propertySchema instanceof BinarySchema) {
            codegenProperty.getVendorExtensions().put(CodegenConstants.IS_FILE_EXT_NAME, Boolean.TRUE);
            codegenProperty.getVendorExtensions().put(CodegenConstants.IS_BINARY_EXT_NAME, Boolean.TRUE);
            codegenProperty.getVendorExtensions().put(CodegenConstants.IS_STRING_EXT_NAME, Boolean.TRUE);
        }
        if (propertySchema instanceof EmailSchema) {
            codegenProperty.getVendorExtensions().put(CodegenConstants.IS_STRING_EXT_NAME, Boolean.TRUE);
        }
        if (propertySchema instanceof UUIDSchema) {
            codegenProperty.getVendorExtensions().put(CodegenConstants.IS_UUID_EXT_NAME, Boolean.TRUE);
            // keep isString to true to make it backward compatible
            codegenProperty.getVendorExtensions().put(CodegenConstants.IS_STRING_EXT_NAME, Boolean.TRUE);
        }
        if (propertySchema instanceof ByteArraySchema) {
            codegenProperty.getVendorExtensions().put(CodegenConstants.IS_BYTE_ARRAY_EXT_NAME, Boolean.TRUE);
        }
        // type is number and without format
        if (propertySchema instanceof NumberSchema) {
            codegenProperty.getVendorExtensions().put(CodegenConstants.IS_NUMERIC_EXT_NAME, Boolean.TRUE);
            if (SchemaTypeUtil.FLOAT_FORMAT.equals(propertySchema.getFormat())) {
                codegenProperty.getVendorExtensions().put(CodegenConstants.IS_FLOAT_EXT_NAME, Boolean.TRUE);
            } else {
                codegenProperty.getVendorExtensions().put(CodegenConstants.IS_DOUBLE_EXT_NAME, Boolean.TRUE);
            }
            this.handleMinMaxValues(propertySchema, codegenProperty);
            if (propertySchema.getEnum() != null && !propertySchema.getEnum().isEmpty()) {
                final List<Number> _enum = propertySchema.getEnum();
                codegenProperty._enum = _enum.stream().map(Number::toString).collect(Collectors.toList());
                codegenProperty.getVendorExtensions().put(IS_ENUM_EXT_NAME, Boolean.TRUE);

                // legacy support
                final Map<String, Object> allowableValues = new HashMap<>();
                allowableValues.put("values", _enum);
                codegenProperty.allowableValues = allowableValues;
            }
        }
        if (propertySchema instanceof DateSchema) {
            codegenProperty.getVendorExtensions().put(CodegenConstants.IS_DATE_EXT_NAME, Boolean.TRUE);
            this.handlePropertySchema(propertySchema, codegenProperty);
        }
        if (propertySchema instanceof DateTimeSchema) {
            codegenProperty.getVendorExtensions().put(CodegenConstants.IS_DATE_TIME_EXT_NAME, Boolean.TRUE);
            this.handlePropertySchema(propertySchema, codegenProperty);
        }
    }

    protected void processPropertySchemaContainerTypes(final CodegenProperty codegenProperty,
            final Schema propertySchema, final String type) {
        if (propertySchema instanceof ArraySchema) {
            codegenProperty.getVendorExtensions().put(CodegenConstants.IS_CONTAINER_EXT_NAME, Boolean.TRUE);
            codegenProperty.getVendorExtensions().put(CodegenConstants.IS_LIST_CONTAINER_EXT_NAME, Boolean.TRUE);
            codegenProperty.containerType = "array";
            codegenProperty.baseType = this.getSchemaType(propertySchema);
            if (propertySchema.getXml() != null) {
                codegenProperty.getVendorExtensions().put(CodegenConstants.IS_XML_WRAPPED_EXT_NAME,
                        propertySchema.getXml().getWrapped() == null ? false : propertySchema.getXml().getWrapped());
                codegenProperty.xmlPrefix = propertySchema.getXml().getPrefix();
                codegenProperty.xmlNamespace = propertySchema.getXml().getNamespace();
                codegenProperty.xmlName = propertySchema.getXml().getName();
            }
            // handle inner property
            codegenProperty.maxItems = propertySchema.getMaxItems();
            codegenProperty.minItems = propertySchema.getMinItems();
            String itemName = null;
            if (propertySchema.getExtensions() != null && propertySchema.getExtensions().get("x-item-name") != null) {
                itemName = propertySchema.getExtensions().get("x-item-name").toString();
            }
            if (itemName == null) {
                itemName = codegenProperty.name;
            }
            final Schema items = ((ArraySchema) propertySchema).getItems();
            final CodegenProperty innerCodegenProperty = this.fromProperty(itemName, items);
            this.updatePropertyForArray(codegenProperty, innerCodegenProperty);
        } else if (propertySchema instanceof MapSchema && hasSchemaProperties(propertySchema)) {

            codegenProperty.getVendorExtensions().put(CodegenConstants.IS_CONTAINER_EXT_NAME, Boolean.TRUE);
            codegenProperty.getVendorExtensions().put(CodegenConstants.IS_MAP_CONTAINER_EXT_NAME, Boolean.TRUE);
            codegenProperty.containerType = "map";
            codegenProperty.baseType = this.getSchemaType(propertySchema);
            codegenProperty.minItems = propertySchema.getMinProperties();
            codegenProperty.maxItems = propertySchema.getMaxProperties();

            // handle inner property
            final CodegenProperty cp = this.fromProperty("inner", (Schema) propertySchema.getAdditionalProperties());
            this.updatePropertyForMap(codegenProperty, cp);
        } else if (propertySchema instanceof MapSchema && hasTrueAdditionalProperties(propertySchema)) {

            codegenProperty.getVendorExtensions().put(CodegenConstants.IS_CONTAINER_EXT_NAME, Boolean.TRUE);
            codegenProperty.getVendorExtensions().put(CodegenConstants.IS_MAP_CONTAINER_EXT_NAME, Boolean.TRUE);
            codegenProperty.containerType = "map";
            codegenProperty.baseType = this.getSchemaType(propertySchema);
            codegenProperty.minItems = propertySchema.getMinProperties();
            codegenProperty.maxItems = propertySchema.getMaxProperties();

            // handle inner property
            final CodegenProperty cp = this.fromProperty("inner", new ObjectSchema());
            this.updatePropertyForMap(codegenProperty, cp);
        } else {
            if (this.isObjectSchema(propertySchema)) {
                codegenProperty.getVendorExtensions().put("x-is-object", Boolean.TRUE);
            }
            this.setNonArrayMapProperty(codegenProperty, type);
        }
    }

    private void handleMinMaxValues(final Schema propertySchema, final CodegenProperty codegenProperty) {
        if (propertySchema.getMinimum() != null) {
            codegenProperty.minimum = String.valueOf(propertySchema.getMinimum().longValue());
        }
        if (propertySchema.getMaximum() != null) {
            codegenProperty.maximum = String.valueOf(propertySchema.getMaximum().longValue());
        }
        if (propertySchema.getExclusiveMinimum() != null) {
            codegenProperty.exclusiveMinimum = propertySchema.getExclusiveMinimum();
        }
        if (propertySchema.getExclusiveMaximum() != null) {
            codegenProperty.exclusiveMaximum = propertySchema.getExclusiveMaximum();
        }
    }

    private void handlePropertySchema(final Schema propertySchema, final CodegenProperty codegenProperty) {
        if (propertySchema.getEnum() != null) {
            final List<String> _enum = propertySchema.getEnum();
            codegenProperty._enum = new ArrayList<>();
            codegenProperty._enum.addAll(_enum);
            codegenProperty.getVendorExtensions().put(IS_ENUM_EXT_NAME, Boolean.TRUE);

            // legacy support
            final Map<String, Object> allowableValues = new HashMap<>();
            allowableValues.put("values", _enum);
            codegenProperty.allowableValues = allowableValues;
        }
    }

    /**
     * Update property for array(list) container
     * @param property Codegen property
     * @param innerProperty Codegen inner property of map or list
     */
    protected void updatePropertyForArray(final CodegenProperty property, final CodegenProperty innerProperty) {
        if (innerProperty == null) {
            LOGGER.warn("skipping invalid array property " + Json.pretty(property));
            return;
        }
        property.dataFormat = innerProperty.dataFormat;
        this.decideIfComplex(property, innerProperty);
        property.items = innerProperty;
        // inner item is Enum
        if (this.isPropertyInnerMostEnum(property)) {
            // isEnum is set to true when the type is an enum
            // or the inner type of an array/map is an enum
            property.getVendorExtensions().put(IS_ENUM_EXT_NAME, Boolean.TRUE);
            // update datatypeWithEnum and default value for array
            // e.g. List<string> => List<StatusEnum>
            this.updateDataTypeWithEnumForArray(property);
            // set allowable values to enum values (including array/map of enum)
            property.allowableValues = this.getInnerEnumAllowableValues(property);
        }

    }

    private void decideIfComplex(final CodegenProperty property, final CodegenProperty innerProperty) {
        if (!this.languageSpecificPrimitives.contains(innerProperty.baseType)) {
            property.complexType = innerProperty.baseType;
        } else {
            property.getVendorExtensions().put(CodegenConstants.IS_PRIMITIVE_TYPE_EXT_NAME, Boolean.TRUE);
        }
    }

    /**
     * Update property for map container
     * @param property Codegen property
     * @param innerProperty Codegen inner property of map or list
     */
    protected void updatePropertyForMap(final CodegenProperty property, final CodegenProperty innerProperty) {
        if (innerProperty == null) {
            LOGGER.warn("skipping invalid map property " + Json.pretty(property));
            return;
        }
        this.decideIfComplex(property, innerProperty);
        property.items = innerProperty;
        property.dataFormat = innerProperty.dataFormat;
        // inner item is Enum
        if (this.isPropertyInnerMostEnum(property)) {
            // isEnum is set to true when the type is an enum
            // or the inner type of an array/map is an enum
            property.getVendorExtensions().put(IS_ENUM_EXT_NAME, Boolean.TRUE);
            // update datatypeWithEnum and default value for map
            // e.g. Dictionary<string, string> => Dictionary<string, StatusEnum>
            this.updateDataTypeWithEnumForMap(property);
            // set allowable values to enum values (including array/map of enum)
            property.allowableValues = this.getInnerEnumAllowableValues(property);
        }

    }

    /**
     * Update property for map container
     * @param property Codegen property
     * @return True if the inner most type is enum
     */
    protected Boolean isPropertyInnerMostEnum(final CodegenProperty property) {
        final CodegenProperty baseItem = BaseItemsHelper.getBaseItemsProperty(property);
        return baseItem == null ? false : getBooleanValue(baseItem, IS_ENUM_EXT_NAME);
    }

    protected Map<String, Object> getInnerEnumAllowableValues(final CodegenProperty property) {
        final CodegenProperty baseItem = BaseItemsHelper.getBaseItemsProperty(property);
        return baseItem == null ? new HashMap<>() : baseItem.allowableValues;
        // return new HashMap<>();
    }

    /**
     * Update datatypeWithEnum for array container
     * @param property Codegen property
     */
    protected void updateDataTypeWithEnumForArray(final CodegenProperty property) {
        final CodegenProperty baseItem = BaseItemsHelper.getBaseItemsProperty(property);
        if (baseItem != null) {
            // set both datatype and datetypeWithEnum as only the inner type is enum
            property.datatypeWithEnum = property.datatypeWithEnum.replace(baseItem.baseType, this.toEnumName(baseItem));

            // naming the enum with respect to the language enum naming convention
            // e.g. remove [], {} from array/map of enum
            property.enumName = this.toEnumName(property);

            // set default value for variable with inner enum
            if (property.defaultValue != null) {
                property.defaultValue = property.defaultValue.replace(baseItem.baseType, this.toEnumName(baseItem));
            }
        }
    }

    /**
     * Update datatypeWithEnum for map container
     * @param property Codegen property
     */
    protected void updateDataTypeWithEnumForMap(final CodegenProperty property) {
        final CodegenProperty baseItem = BaseItemsHelper.getBaseItemsProperty(property);

        if (baseItem != null) {
            // set both datatype and datetypeWithEnum as only the inner type is enum
            property.datatypeWithEnum = property.datatypeWithEnum.replace(", " + baseItem.baseType,
                    ", " + this.toEnumName(baseItem));

            // naming the enum with respect to the language enum naming convention
            // e.g. remove [], {} from array/map of enum
            property.enumName = this.toEnumName(property);

            // set default value for variable with inner enum
            if (property.defaultValue != null) {
                property.defaultValue = property.defaultValue.replace(", " + property.items.baseType,
                        ", " + this.toEnumName(property.items));
            }
        }
    }

    protected void setNonArrayMapProperty(final CodegenProperty property, final String type) {
        property.getVendorExtensions().put(CodegenConstants.IS_NOT_CONTAINER_EXT_NAME, Boolean.TRUE);
        if (this.languageSpecificPrimitives().contains(type)) {
            property.getVendorExtensions().put(CodegenConstants.IS_PRIMITIVE_TYPE_EXT_NAME, Boolean.TRUE);
        } else {
            property.complexType = property.baseType;
        }
    }

    /**
     * Override with any special handling of response codes
     * @param responses Swagger Operation's responses
     * @return default method response or <tt>null</tt> if not found
     */
    protected ApiResponse findMethodResponse(final ApiResponses responses) {

        String code = null;
        for (final String responseCode : responses.keySet()) {
            if ((responseCode.startsWith("2") || "default".equals(responseCode))
                    && (code == null || code.compareTo(responseCode) > 0)) {
                code = responseCode;
            }
        }
        if (code == null) {
            return null;
        }
        return responses.get(code);
    }

    /**
     * Convert Swagger Operation object to Codegen Operation object (without providing a Swagger object)
     * @param path the path of the operation
     * @param httpMethod HTTP method
     * @param operation Swagger operation object
     * @param schemas a map of Swagger models
     * @return Codegen Operation object
     */
    @Override
    public CodegenOperation fromOperation(final String path, final String httpMethod, final Operation operation,
            final Map<String, Schema> schemas) {
        return this.fromOperation(path, httpMethod, operation, schemas, null);
    }

    /**
     * Convert Swagger Operation object to Codegen Operation object
     * @param path the path of the operation
     * @param httpMethod HTTP method
     * @param operation Swagger operation object
     * @param schemas a map of schemas
     * @param openAPI a OpenAPI object representing the spec
     * @return Codegen Operation object
     */
    @Override
    public CodegenOperation fromOperation(final String path, final String httpMethod, final Operation operation,
            final Map<String, Schema> schemas, final OpenAPI openAPI) {
        final CodegenOperation codegenOperation = CodegenModelFactory.newInstance(CodegenModelType.OPERATION);
        final Set<String> imports = new HashSet<>();
        if (operation.getExtensions() != null && !operation.getExtensions().isEmpty()) {
            codegenOperation.vendorExtensions.putAll(operation.getExtensions());
        }

        // LOGGER.info("Original operationId: " + operation.getOperationId());
        String operationId = this.getOrGenerateOperationId(operation, path, httpMethod);
        // LOGGER.info("Generated operationId: " + operationId);
        // remove prefix in operationId
        if (this.removeOperationIdPrefix) {
            final int offset = operationId.indexOf('_');
            if (offset > -1) {
                operationId = operationId.substring(offset + 1);
            }
        }
        // LOGGER.info("Un-prefixed operationId: " + operationId);
        operationId = this.removeNonNameElementToCamelCase(operationId);
        // LOGGER.info("CamelCased operationId: " + operationId);
        operationId = operationId.replaceAll("[0-9]", "");
        // LOGGER.info("Wothout number operationId: " + operationId);
        codegenOperation.path = path;
        codegenOperation.operationId = this.toOperationId(operationId);
        // LOGGER.info("Final operationId: " + codegenOperation.operationId);
        codegenOperation.summary = this.escapeText(operation.getSummary());
        codegenOperation.unescapedNotes = operation.getDescription();
        codegenOperation.notes = this.escapeText(operation.getDescription());
        codegenOperation.getVendorExtensions().put(CodegenConstants.HAS_CONSUMES_EXT_NAME, Boolean.FALSE);
        codegenOperation.getVendorExtensions().put(CodegenConstants.HAS_PRODUCES_EXT_NAME, Boolean.FALSE);
        if (operation.getDeprecated() != null) {
            codegenOperation.getVendorExtensions().put(CodegenConstants.IS_DEPRECATED_EXT_NAME,
                    operation.getDeprecated());
        }

        this.addConsumesInfo(operation, codegenOperation, openAPI);

        if (operation.getResponses() != null && !operation.getResponses().isEmpty()) {
            final ApiResponse methodResponse = this.findMethodResponse(operation.getResponses());

            for (final String key : operation.getResponses().keySet()) {
                final ApiResponse response = operation.getResponses().get(key);

                this.addProducesInfo(response, codegenOperation);

                final CodegenResponse codegenResponse = this.fromResponse(key, response);
                codegenResponse.getVendorExtensions().put(CodegenConstants.HAS_MORE_EXT_NAME, Boolean.TRUE);
                if (codegenResponse.baseType != null && !this.defaultIncludes.contains(codegenResponse.baseType)
                        && !this.languageSpecificPrimitives.contains(codegenResponse.baseType)) {
                    imports.add(codegenResponse.baseType);
                }
                codegenResponse.getVendorExtensions().put(CodegenConstants.IS_DEFAULT_EXT_NAME,
                        response == methodResponse);
                codegenOperation.responses.add(codegenResponse);
                if (getBooleanValue(codegenResponse, CodegenConstants.IS_BINARY_EXT_NAME)
                        && getBooleanValue(codegenResponse, CodegenConstants.IS_DEFAULT_EXT_NAME)) {
                    codegenOperation.getVendorExtensions().put(CodegenConstants.IS_RESPONSE_BINARY_EXT_NAME,
                            Boolean.TRUE);
                }
                if (getBooleanValue(codegenResponse, CodegenConstants.IS_FILE_EXT_NAME)
                        && getBooleanValue(codegenResponse, CodegenConstants.IS_DEFAULT_EXT_NAME)) {
                    codegenOperation.getVendorExtensions().put(CodegenConstants.IS_RESPONSE_FILE_EXT_NAME,
                            Boolean.TRUE);
                }
            }
            if (codegenOperation.produces != null) {
                final Set<String> mediaTypes = new HashSet<>();
                codegenOperation.produces.removeIf(map -> !mediaTypes.add(map.get("mediaType")));
                codegenOperation.produces.get(codegenOperation.produces.size() - 1).remove("hasMore");
            }
            codegenOperation.responses.get(codegenOperation.responses.size() - 1).getVendorExtensions()
                    .put(CodegenConstants.HAS_MORE_EXT_NAME, Boolean.FALSE);

            if (methodResponse != null) {
                final Schema responseSchema = this.getSchemaFromResponse(methodResponse);
                if (responseSchema != null) {
                    final CodegenProperty codegenProperty = this.fromProperty("response", responseSchema);

                    if (responseSchema instanceof ArraySchema) {
                        final ArraySchema arraySchema = (ArraySchema) responseSchema;
                        final CodegenProperty innerProperty = this.fromProperty("response", arraySchema.getItems());
                        codegenOperation.returnBaseType = innerProperty.baseType;
                    } else if (responseSchema instanceof MapSchema && hasSchemaProperties(responseSchema)) {
                        final MapSchema mapSchema = (MapSchema) responseSchema;
                        final CodegenProperty innerProperty = this.fromProperty("response",
                                (Schema) mapSchema.getAdditionalProperties());
                        codegenOperation.returnBaseType = innerProperty.baseType;
                    } else if (responseSchema instanceof MapSchema && hasTrueAdditionalProperties(responseSchema)) {
                        final CodegenProperty innerProperty = this.fromProperty("response", new ObjectSchema());
                        codegenOperation.returnBaseType = innerProperty.baseType;
                    } else {
                        if (codegenProperty.complexType != null) {
                            codegenOperation.returnBaseType = codegenProperty.complexType;
                        } else {
                            codegenOperation.returnBaseType = codegenProperty.baseType;
                        }
                    }
                    if (!this.additionalProperties.containsKey(CodegenConstants.DISABLE_EXAMPLES_OPTION)) {
                        codegenOperation.examples = new ExampleGenerator(openAPI).generate(null, null, responseSchema);
                    }
                    codegenOperation.defaultResponse = this.toDefaultValue(responseSchema);
                    codegenOperation.returnType = codegenProperty.datatype;
                    final boolean hasReference = schemas != null
                            && schemas.containsKey(codegenOperation.returnBaseType);
                    codegenOperation.getVendorExtensions().put(CodegenConstants.HAS_REFERENCE_EXT_NAME, hasReference);

                    // lookup discriminator
                    if (schemas != null) {
                        final Schema schemaDefinition = schemas.get(codegenOperation.returnBaseType);
                        if (schemaDefinition != null) {
                            final CodegenModel cmod = this.fromModel(codegenOperation.returnBaseType, schemaDefinition,
                                    schemas);
                            codegenOperation.discriminator = cmod.discriminator;
                        }
                    }

                    final boolean isContainer = getBooleanValue(codegenProperty,
                            CodegenConstants.IS_CONTAINER_EXT_NAME);
                    if (isContainer) {
                        codegenOperation.returnContainer = codegenProperty.containerType;
                        if ("map".equals(codegenProperty.containerType)) {
                            codegenOperation.getVendorExtensions().put(CodegenConstants.IS_MAP_CONTAINER_EXT_NAME,
                                    Boolean.TRUE);
                        } else if ("list".equalsIgnoreCase(codegenProperty.containerType)) {
                            codegenOperation.getVendorExtensions().put(CodegenConstants.IS_LIST_CONTAINER_EXT_NAME,
                                    Boolean.TRUE);
                        } else if ("array".equalsIgnoreCase(codegenProperty.containerType)) {
                            codegenOperation.getVendorExtensions().put(CodegenConstants.IS_LIST_CONTAINER_EXT_NAME,
                                    Boolean.TRUE);
                        }
                    } else {
                        codegenOperation.returnSimpleType = true;
                    }
                    if (this.languageSpecificPrimitives().contains(codegenOperation.returnBaseType)
                            || codegenOperation.returnBaseType == null) {
                        codegenOperation.returnTypeIsPrimitive = true;
                    }
                }
                Map<String, Header> componentHeaders = null;
                if ((openAPI != null) && (openAPI.getComponents() != null)) {
                    componentHeaders = openAPI.getComponents().getHeaders();
                }
                this.addHeaders(methodResponse, codegenOperation.responseHeaders, componentHeaders);
            }
        }

        final List<Parameter> parameters = operation.getParameters();
        final OperationParameters operationParameters = new OperationParameters();

        RequestBody body = operation.getRequestBody();
        if (body != null) {
            if (StringUtils.isNotBlank(body.get$ref())) {
                final String bodyName = OpenAPIUtil.getSimpleRef(body.get$ref());
                body = openAPI.getComponents().getRequestBodies().get(bodyName);
            }

            final List<Schema> foundSchemas = new ArrayList<>();

            // for (final String contentType : body.getContent().keySet()) {
            final String contentType = body.getContent().keySet().iterator().next();
            final boolean isForm = "application/x-www-form-urlencoded".equalsIgnoreCase(contentType)
                    || "multipart/form-data".equalsIgnoreCase(contentType);

            String schemaName = null;
            Schema schema = body.getContent().get(contentType).getSchema();
            if (schema != null && StringUtils.isNotBlank(schema.get$ref())) {
                schemaName = OpenAPIUtil.getSimpleRef(schema.get$ref());
                try {
                    schemaName = URLDecoder.decode(schemaName, StandardCharsets.UTF_8.name());
                } catch (final UnsupportedEncodingException e) {
                    LOGGER.error("Could not decoded string: " + schemaName, e);
                }
                schema = schemas.get(schemaName);
            }
            final CodegenContent codegenContent = new CodegenContent(contentType);
            codegenContent.getContentExtensions().put(CodegenConstants.IS_FORM_EXT_NAME, isForm);

            if (schema == null) {
                final CodegenParameter codegenParameter = CodegenModelFactory.newInstance(CodegenModelType.PARAMETER);
                codegenParameter.description = body.getDescription();
                codegenParameter.unescapedDescription = body.getDescription();
                String bodyName = REQUEST_BODY_NAME;
                if (body.getExtensions() != null && body.getExtensions().get("x-codegen-request-body-name") != null) {
                    bodyName = body.getExtensions().get("x-codegen-request-body-name").toString();
                }
                codegenParameter.baseName = bodyName;
                codegenParameter.paramName = bodyName;
                codegenParameter.dataType = "Object";
                codegenParameter.baseType = "Object";

                codegenParameter.required = body.getRequired() != null ? body.getRequired() : false;
                if (!isForm) {
                    codegenParameter.getVendorExtensions().put(CodegenConstants.IS_BODY_PARAM_EXT_NAME, Boolean.TRUE);
                }
                // continue;
            } else {
                if (isForm) {
                    final Map<String, Schema> propertyMap = schema.getProperties();
                    final boolean isMultipart = "multipart/form-data".equalsIgnoreCase(contentType);
                    if (propertyMap != null && !propertyMap.isEmpty()) {
                        for (final String propertyName : propertyMap.keySet()) {
                            final CodegenParameter formParameter = this.fromParameter(new Parameter().name(propertyName)
                                    .required(body.getRequired()).schema(propertyMap.get(propertyName)), imports);
                            if (isMultipart) {
                                formParameter.getVendorExtensions().put(CodegenConstants.IS_MULTIPART_EXT_NAME,
                                        Boolean.TRUE);
                            }
                            // todo: this segment is only to support the "older" template design. it should be removed
                            // once all templates are updated with the new {{#contents}} tag.
                            formParameter.getVendorExtensions().put(CodegenConstants.IS_FORM_PARAM_EXT_NAME,
                                    Boolean.TRUE);
                            operationParameters.addFormParam(formParameter.copy());
                            if (body.getRequired() != null && body.getRequired()) {
                                operationParameters.addRequiredParam(formParameter.copy());
                            }
                            operationParameters.addAllParams(formParameter);
                        }
                        operationParameters.addCodegenContents(codegenContent);
                    }
                } else {
                    boolean alreadyAdded = false;
                    final CodegenParameter bodyParam = this.fromRequestBody(body, schemaName, schema, schemas, imports);
                    operationParameters.setBodyParam(bodyParam);
                    if (foundSchemas.isEmpty()) {
                        operationParameters.addBodyParams(bodyParam.copy());
                        operationParameters.addAllParams(bodyParam);
                    } else {
                        for (final Schema usedSchema : foundSchemas) {
                            if (alreadyAdded = usedSchema.equals(schema)) {
                                break;
                            }
                        }
                    }
                    if (!alreadyAdded) {
                        // continue;
                        foundSchemas.add(schema);
                        operationParameters.addCodegenContents(codegenContent);
                    }
                }
            }
            // }
        }

        if (parameters != null) {
            for (Parameter param : parameters) {
                if (StringUtils.isNotBlank(param.get$ref())) {
                    param = this.getParameterFromRef(param.get$ref(), openAPI);
                }
                if ((param instanceof QueryParameter || "query".equalsIgnoreCase(param.getIn()))
                        && param.getStyle() != null && Parameter.StyleEnum.DEEPOBJECT.equals(param.getStyle())) {
                    operationParameters.parseNestedObjects(param.getName(), param.getSchema(), imports, this, openAPI);
                    continue;
                }
                final CodegenParameter codegenParameter = this.fromParameter(param, imports);
                operationParameters.addParameters(param, codegenParameter);
            }
        }

        this.addOperationImports(codegenOperation, imports);

        codegenOperation.bodyParam = operationParameters.getBodyParam();
        codegenOperation.httpMethod = httpMethod.toUpperCase();

        // move "required" parameters in front of "optional" parameters
        // if (this.sortParamsByRequiredFlag) {
        LOGGER.info("-----------------------------------");
        LOGGER.info(httpMethod.toUpperCase() + " " + path);
        StringBuilder allParams = new StringBuilder();
        for (final CodegenParameter p : operationParameters.getAllParams()) {
            allParams.append(p.getParamName());
            allParams.append(",");
        }
        LOGGER.info("Prev params: " + allParams.toString());
        operationParameters.sortRequiredAllParams();
        allParams = new StringBuilder();
        for (final CodegenParameter p : operationParameters.getAllParams()) {
            allParams.append(p.getParamName());
            allParams.append(",");
        }
        LOGGER.info("New params: " + allParams.toString());
        // }

        operationParameters.addHasMore(codegenOperation);
        codegenOperation.externalDocs = operation.getExternalDocs();

        this.configuresParameterForMediaType(codegenOperation, operationParameters.getCodegenContents());
        // legacy support
        codegenOperation.nickname = codegenOperation.operationId;
        // LOGGER.info("Final nickname: " + codegenOperation.nickname);

        if (codegenOperation.allParams.size() > 0) {
            codegenOperation.getVendorExtensions().put(CodegenConstants.HAS_PARAMS_EXT_NAME, Boolean.TRUE);
        }
        final boolean hasRequiredParams = codegenOperation.requiredParams.size() > 0;
        codegenOperation.getVendorExtensions().put(CodegenConstants.HAS_REQUIRED_PARAMS_EXT_NAME, hasRequiredParams);

        final boolean hasOptionalParams = codegenOperation.allParams.stream()
                .anyMatch(codegenParameter -> !codegenParameter.required);
        codegenOperation.getVendorExtensions().put(CodegenConstants.HAS_OPTIONAL_PARAMS_EXT_NAME, hasOptionalParams);

        // set Restful Flag
        codegenOperation.getVendorExtensions().put(CodegenConstants.IS_RESTFUL_SHOW_EXT_NAME,
                codegenOperation.getIsRestfulShow());
        codegenOperation.getVendorExtensions().put(CodegenConstants.IS_RESTFUL_INDEX_EXT_NAME,
                codegenOperation.getIsRestfulIndex());
        codegenOperation.getVendorExtensions().put(CodegenConstants.IS_RESTFUL_CREATE_EXT_NAME,
                codegenOperation.getIsRestfulCreate());
        codegenOperation.getVendorExtensions().put(CodegenConstants.IS_RESTFUL_UPDATE_EXT_NAME,
                codegenOperation.getIsRestfulUpdate());
        codegenOperation.getVendorExtensions().put(CodegenConstants.IS_RESTFUL_DESTROY_EXT_NAME,
                codegenOperation.getIsRestfulDestroy());
        codegenOperation.getVendorExtensions().put(CodegenConstants.IS_RESTFUL_EXT_NAME,
                codegenOperation.getIsRestful());

        this.configureDataForTestTemplate(codegenOperation);

        return codegenOperation;
    }

    protected void addOperationImports(final CodegenOperation codegenOperation, final Set<String> operationImports) {
        for (final String operationImport : operationImports) {
            if (this.needToImport(operationImport)) {
                codegenOperation.imports.add(operationImport);
            }
        }
    }

    /**
     * Convert Swagger Response object to Codegen Response object
     * @param responseCode HTTP response code
     * @param response Swagger Response object
     * @return Codegen Response object
     */
    public CodegenResponse fromResponse(final String responseCode, final ApiResponse response) {
        final CodegenResponse codegenResponse = CodegenModelFactory.newInstance(CodegenModelType.RESPONSE);
        if ("default".equals(responseCode)) {
            codegenResponse.code = "0";
        } else {
            codegenResponse.code = responseCode;
        }
        final Schema responseSchema = this.getSchemaFromResponse(response);
        codegenResponse.schema = responseSchema;
        codegenResponse.message = this.escapeText(response.getDescription());

        if (response.getContent() != null) {
            final Map<String, Object> examples = new HashMap<>();
            for (final String name : response.getContent().keySet()) {
                if (response.getContent().get(name) != null) {

                    if (response.getContent().get(name).getExample() != null) {
                        examples.put(name, response.getContent().get(name).getExample());
                    }
                    if (response.getContent().get(name).getExamples() != null) {

                        for (final String exampleName : response.getContent().get(name).getExamples().keySet()) {
                            examples.put(exampleName,
                                    response.getContent().get(name).getExamples().get(exampleName).getValue());
                        }
                    }
                }
            }
            codegenResponse.examples = this.toExamples(examples);
        }

        codegenResponse.jsonSchema = Json.pretty(response);
        if (response.getExtensions() != null && !response.getExtensions().isEmpty()) {
            codegenResponse.vendorExtensions.putAll(response.getExtensions());
        }
        Map<String, Header> componentHeaders = null;
        if ((this.openAPI != null) && (this.openAPI.getComponents() != null)) {
            componentHeaders = this.openAPI.getComponents().getHeaders();
        }
        this.addHeaders(response, codegenResponse.headers, componentHeaders);
        codegenResponse.getVendorExtensions().put(CodegenConstants.HAS_HEADERS_EXT_NAME,
                !codegenResponse.headers.isEmpty());

        if (responseSchema != null) {
            final CodegenProperty codegenProperty = this.fromProperty("response", responseSchema);

            if (responseSchema instanceof ArraySchema) {
                final ArraySchema arraySchema = (ArraySchema) responseSchema;
                final CodegenProperty innerProperty = this.fromProperty("response", arraySchema.getItems());
                CodegenProperty innerCp = innerProperty;
                while (innerCp != null) {
                    codegenResponse.baseType = innerCp.baseType;
                    innerCp = innerCp.items;
                }
            } else {
                if (codegenProperty.complexType != null) {
                    codegenResponse.baseType = codegenProperty.complexType;
                } else {
                    codegenResponse.baseType = codegenProperty.baseType;
                }
                if (this.isFileTypeSchema(responseSchema)) {
                    codegenResponse.getVendorExtensions().put(CodegenConstants.IS_FILE_EXT_NAME, Boolean.TRUE);
                }
            }
            codegenResponse.dataType = codegenProperty.datatype;

            if (getBooleanValue(codegenProperty, CodegenConstants.IS_STRING_EXT_NAME)) {
                codegenResponse.getVendorExtensions().put(CodegenConstants.IS_STRING_EXT_NAME, Boolean.TRUE);
            }
            if (getBooleanValue(codegenProperty, CodegenConstants.IS_BOOLEAN_EXT_NAME)) {
                codegenResponse.getVendorExtensions().put(CodegenConstants.IS_BOOLEAN_EXT_NAME, Boolean.TRUE);
            }
            if (getBooleanValue(codegenProperty, CodegenConstants.IS_LONG_EXT_NAME)) {
                codegenResponse.getVendorExtensions().put(CodegenConstants.IS_LONG_EXT_NAME, Boolean.TRUE);
                codegenResponse.getVendorExtensions().put(CodegenConstants.IS_NUMERIC_EXT_NAME, Boolean.TRUE);
            }
            if (getBooleanValue(codegenProperty, CodegenConstants.IS_INTEGER_EXT_NAME)) {
                codegenResponse.getVendorExtensions().put(CodegenConstants.IS_INTEGER_EXT_NAME, Boolean.TRUE);
                codegenResponse.getVendorExtensions().put(CodegenConstants.IS_NUMERIC_EXT_NAME, Boolean.TRUE);
            }
            if (getBooleanValue(codegenProperty, CodegenConstants.IS_DOUBLE_EXT_NAME)) {
                codegenResponse.getVendorExtensions().put(CodegenConstants.IS_DOUBLE_EXT_NAME, Boolean.TRUE);
                codegenResponse.getVendorExtensions().put(CodegenConstants.IS_NUMERIC_EXT_NAME, Boolean.TRUE);
            }
            if (getBooleanValue(codegenProperty, CodegenConstants.IS_FLOAT_EXT_NAME)) {
                codegenResponse.getVendorExtensions().put(CodegenConstants.IS_FLOAT_EXT_NAME, Boolean.TRUE);
                codegenResponse.getVendorExtensions().put(CodegenConstants.IS_NUMERIC_EXT_NAME, Boolean.TRUE);
            }
            if (getBooleanValue(codegenProperty, CodegenConstants.IS_BYTE_ARRAY_EXT_NAME)) {
                codegenResponse.getVendorExtensions().put(CodegenConstants.IS_BYTE_ARRAY_EXT_NAME, Boolean.TRUE);
            }
            if (getBooleanValue(codegenProperty, CodegenConstants.IS_BINARY_EXT_NAME)) {
                codegenResponse.getVendorExtensions().put(CodegenConstants.IS_BINARY_EXT_NAME, Boolean.TRUE);
            }
            if (getBooleanValue(codegenProperty, CodegenConstants.IS_FILE_EXT_NAME)) {
                codegenResponse.getVendorExtensions().put(CodegenConstants.IS_FILE_EXT_NAME, Boolean.TRUE);
            }
            if (getBooleanValue(codegenProperty, CodegenConstants.IS_DATE_EXT_NAME)) {
                codegenResponse.getVendorExtensions().put(CodegenConstants.IS_DATE_EXT_NAME, Boolean.TRUE);
            }
            if (getBooleanValue(codegenProperty, CodegenConstants.IS_DATE_TIME_EXT_NAME)) {
                codegenResponse.getVendorExtensions().put(CodegenConstants.IS_DATE_TIME_EXT_NAME, Boolean.TRUE);
            }
            if (getBooleanValue(codegenProperty, CodegenConstants.IS_UUID_EXT_NAME)) {
                codegenResponse.getVendorExtensions().put(CodegenConstants.IS_UUID_EXT_NAME, Boolean.TRUE);
            }

            if (getBooleanValue(codegenProperty, CodegenConstants.IS_CONTAINER_EXT_NAME)) {
                codegenResponse.getVendorExtensions().put(CodegenConstants.IS_SIMPLE_TYPE_EXT_NAME, Boolean.FALSE);
                codegenResponse.containerType = codegenProperty.containerType;
                codegenResponse.getVendorExtensions().put(CodegenConstants.IS_MAP_CONTAINER_EXT_NAME,
                        "map".equals(codegenProperty.containerType));
                codegenResponse.getVendorExtensions().put(CodegenConstants.IS_LIST_CONTAINER_EXT_NAME,
                        "list".equalsIgnoreCase(codegenProperty.containerType)
                                || "array".equalsIgnoreCase(codegenProperty.containerType));
            } else {
                codegenResponse.getVendorExtensions().put(CodegenConstants.IS_SIMPLE_TYPE_EXT_NAME, Boolean.TRUE);
            }
            codegenResponse.getVendorExtensions().put(CodegenConstants.IS_PRIMITIVE_TYPE_EXT_NAME,
                    (codegenResponse.baseType == null
                            || this.languageSpecificPrimitives().contains(codegenResponse.baseType)));
        }
        if (codegenResponse.baseType == null) {
            codegenResponse.getVendorExtensions().put(CodegenConstants.IS_MAP_CONTAINER_EXT_NAME, Boolean.FALSE);
            codegenResponse.getVendorExtensions().put(CodegenConstants.IS_LIST_CONTAINER_EXT_NAME, Boolean.FALSE);
            codegenResponse.getVendorExtensions().put(CodegenConstants.IS_PRIMITIVE_TYPE_EXT_NAME, Boolean.TRUE);
            codegenResponse.getVendorExtensions().put(CodegenConstants.IS_SIMPLE_TYPE_EXT_NAME, Boolean.TRUE);
        }
        return codegenResponse;
    }

    /**
     * Convert Swagger Parameter object to Codegen Parameter object
     * @param parameter Swagger parameter object
     * @param imports set of imports for library/package/module
     * @return Codegen Parameter object
     */
    public CodegenParameter fromParameter(final Parameter parameter, final Set<String> imports) {
        final CodegenParameter codegenParameter = CodegenModelFactory.newInstance(CodegenModelType.PARAMETER);
        codegenParameter.baseName = parameter.getName();
        codegenParameter.description = this.escapeText(parameter.getDescription());
        codegenParameter.unescapedDescription = parameter.getDescription();
        if (parameter.getRequired() != null) {
            codegenParameter.required = parameter.getRequired();
        }
        codegenParameter.jsonSchema = Json.pretty(parameter);

        if (System.getProperty("debugParser") != null) {
            LOGGER.info("working on Parameter " + parameter.getName());
        }

        // move the defaultValue for headers, forms and params
        if (parameter instanceof QueryParameter) {
            final QueryParameter qp = (QueryParameter) parameter;
            if ((qp.getSchema() != null) && (qp.getSchema().getDefault() != null)) {
                codegenParameter.defaultValue = qp.getSchema().getDefault().toString();
            }
        } else if (parameter instanceof HeaderParameter) {
            final HeaderParameter hp = (HeaderParameter) parameter;
            if ((hp.getSchema() != null) && (hp.getSchema().getDefault() != null)) {
                codegenParameter.defaultValue = hp.getSchema().getDefault().toString();
            }
        }

        if (parameter.getExtensions() != null && !parameter.getExtensions().isEmpty()) {
            codegenParameter.vendorExtensions.putAll(parameter.getExtensions());
        }

        Schema parameterSchema = parameter.getSchema();
        if (parameterSchema == null) {
            parameterSchema = this.getSchemaFromParameter(parameter);
        }
        if (parameterSchema != null) {
            String collectionFormat = null;
            if (parameterSchema instanceof ArraySchema) { // for array parameter
                final ArraySchema arraySchema = (ArraySchema) parameterSchema;
                Schema inner = arraySchema.getItems();
                if (inner == null) {
                    LOGGER.warn("warning!  No inner type supplied for array parameter \"" + parameter.getName()
                            + "\", using String");
                    inner = new StringSchema().description("//TODO automatically added by swagger-codegen");
                    arraySchema.setItems(inner);

                } else if (this.isObjectSchema(inner)) {
                    // fixme: codegenParameter.getVendorExtensions().put(CodegenConstants.HAS_INNER_OBJECT_NAME,
                    // Boolean.TRUE);
                    codegenParameter.getVendorExtensions().put("x-has-inner-object", Boolean.TRUE);
                }

                collectionFormat = this.getCollectionFormat(parameter);

                CodegenProperty codegenProperty = this.fromProperty("inner", inner);
                codegenParameter.items = codegenProperty;
                codegenParameter.baseType = codegenProperty.datatype;
                codegenParameter.getVendorExtensions().put(CodegenConstants.IS_CONTAINER_EXT_NAME, Boolean.TRUE);
                codegenParameter.getVendorExtensions().put(CodegenConstants.IS_LIST_CONTAINER_EXT_NAME, Boolean.TRUE);

                // recursively add import
                while (codegenProperty != null) {
                    imports.add(codegenProperty.baseType);
                    codegenProperty = codegenProperty.items;
                }
            } else if (parameterSchema instanceof MapSchema && hasSchemaProperties(parameterSchema)) { // for map
                                                                                                       // parameter
                CodegenProperty codegenProperty = this.fromProperty("inner",
                        (Schema) parameterSchema.getAdditionalProperties());
                codegenParameter.items = codegenProperty;
                codegenParameter.baseType = codegenProperty.datatype;
                codegenParameter.getVendorExtensions().put(CodegenConstants.IS_CONTAINER_EXT_NAME, Boolean.TRUE);
                codegenParameter.getVendorExtensions().put(CodegenConstants.IS_MAP_CONTAINER_EXT_NAME, Boolean.TRUE);
                // recursively add import
                while (codegenProperty != null) {
                    imports.add(codegenProperty.baseType);
                    codegenProperty = codegenProperty.items;
                }
                collectionFormat = this.getCollectionFormat(parameter);
            } else if (parameterSchema instanceof MapSchema && hasTrueAdditionalProperties(parameterSchema)) { // for
                                                                                                               // map
                                                                                                               // parameter
                CodegenProperty codegenProperty = this.fromProperty("inner", new ObjectSchema());
                codegenParameter.items = codegenProperty;
                codegenParameter.baseType = codegenProperty.datatype;
                codegenParameter.getVendorExtensions().put(CodegenConstants.IS_CONTAINER_EXT_NAME, Boolean.TRUE);
                codegenParameter.getVendorExtensions().put(CodegenConstants.IS_MAP_CONTAINER_EXT_NAME, Boolean.TRUE);
                // recursively add import
                while (codegenProperty != null) {
                    imports.add(codegenProperty.baseType);
                    codegenProperty = codegenProperty.items;
                }
                collectionFormat = this.getCollectionFormat(parameter);
            } else if (parameterSchema instanceof FileSchema || parameterSchema instanceof BinarySchema) {
                codegenParameter.getVendorExtensions().put(CodegenConstants.IS_BINARY_EXT_NAME, Boolean.TRUE);
                codegenParameter.getVendorExtensions().put(CodegenConstants.IS_FILE_EXT_NAME, Boolean.TRUE);
            }

            if (parameterSchema == null) {
                LOGGER.warn("warning!  Schema not found for parameter \"" + parameter.getName() + "\", using String");
                parameterSchema = new StringSchema().description("//TODO automatically added by swagger-codegen.");
            }
            final CodegenProperty codegenProperty = this.fromProperty(parameter.getName(), parameterSchema);

            // set boolean flag (e.g. isString)
            this.setParameterBooleanFlagWithCodegenProperty(codegenParameter, codegenProperty);
            this.setParameterNullable(codegenParameter, codegenProperty); // todo: needs to be removed

            codegenParameter.nullable = Boolean.TRUE.equals(parameterSchema.getNullable());
            codegenParameter.getVendorExtensions().put(CodegenConstants.IS_NULLABLE_EXT_NAME,
                    Boolean.TRUE.equals(parameterSchema.getNullable()));

            codegenParameter.dataType = codegenProperty.datatype;
            codegenParameter.dataFormat = codegenProperty.dataFormat;

            this.setParameterJson(codegenParameter, parameterSchema);

            if (getBooleanValue(codegenProperty, IS_ENUM_EXT_NAME)) {
                codegenParameter.datatypeWithEnum = codegenProperty.datatypeWithEnum;
                codegenParameter.enumName = codegenProperty.enumName;

                this.updateCodegenPropertyEnum(codegenProperty);
                codegenParameter.getVendorExtensions().put(CodegenConstants.IS_ENUM_EXT_NAME, Boolean.TRUE);
                codegenParameter._enum = codegenProperty._enum;
            }
            codegenParameter.allowableValues = codegenProperty.allowableValues;

            if (codegenProperty.items != null && getBooleanValue(codegenProperty.items, IS_ENUM_EXT_NAME)) {
                codegenParameter.datatypeWithEnum = codegenProperty.datatypeWithEnum;
                codegenParameter.enumName = codegenProperty.enumName;
                codegenParameter.items = codegenProperty.items;
            }
            codegenParameter.collectionFormat = collectionFormat;
            if ("multi".equals(collectionFormat)) {
                codegenParameter.getVendorExtensions().put(CodegenConstants.IS_COLLECTION_FORMAT_MULTI_EXT_NAME,
                        Boolean.TRUE);
            }
            codegenParameter.paramName = this.toParamName(parameter.getName());

            // import
            if (codegenProperty.complexType != null) {
                imports.add(codegenProperty.complexType);
            }

            // validation
            // handle maximum, minimum properly for int/long by removing the trailing ".0"
            if (parameterSchema instanceof IntegerSchema) {
                codegenParameter.maximum = parameterSchema.getMaximum() == null ? null
                        : String.valueOf(parameterSchema.getMaximum().longValue());
                codegenParameter.minimum = parameterSchema.getMinimum() == null ? null
                        : String.valueOf(parameterSchema.getMinimum().longValue());
            } else {
                codegenParameter.maximum = parameterSchema.getMaximum() == null ? null
                        : String.valueOf(parameterSchema.getMaximum());
                codegenParameter.minimum = parameterSchema.getMinimum() == null ? null
                        : String.valueOf(parameterSchema.getMinimum());
            }

            codegenParameter.exclusiveMaximum = parameterSchema.getExclusiveMaximum() == null ? false
                    : parameterSchema.getExclusiveMaximum();
            codegenParameter.exclusiveMinimum = parameterSchema.getExclusiveMinimum() == null ? false
                    : parameterSchema.getExclusiveMinimum();
            codegenParameter.maxLength = parameterSchema.getMaxLength();
            codegenParameter.minLength = parameterSchema.getMinLength();
            codegenParameter.pattern = this.toRegularExpression(parameterSchema.getPattern());
            codegenParameter.maxItems = parameterSchema.getMaxItems();
            codegenParameter.minItems = parameterSchema.getMinItems();
            codegenParameter.uniqueItems = parameterSchema.getUniqueItems() == null ? false
                    : parameterSchema.getUniqueItems();
            codegenParameter.multipleOf = parameterSchema.getMultipleOf();

            // exclusive* are noop without corresponding min/max
            if (codegenParameter.maximum != null || codegenParameter.minimum != null
                    || codegenParameter.maxLength != null || codegenParameter.minLength != null
                    || codegenParameter.maxItems != null || codegenParameter.minItems != null
                    || codegenParameter.pattern != null) {
                codegenParameter.getVendorExtensions().put(CodegenConstants.HAS_VALIDATION_EXT_NAME, Boolean.TRUE);
            }

        }

        // Issue #2561 (neilotoole) : Set the is<TYPE>Param flags.
        // This code has been moved to here from #fromOperation
        // because these values should be set before calling #postProcessParameter.
        // See: https://github.com/swagger-api/swagger-codegen/issues/2561
        if (parameter instanceof QueryParameter || "query".equalsIgnoreCase(parameter.getIn())) {
            codegenParameter.getVendorExtensions().put(CodegenConstants.IS_QUERY_PARAM_EXT_NAME, Boolean.TRUE);
        } else if (parameter instanceof PathParameter || "path".equalsIgnoreCase(parameter.getIn())) {
            codegenParameter.required = true;
            codegenParameter.getVendorExtensions().put(CodegenConstants.IS_PATH_PARAM_EXT_NAME, Boolean.TRUE);
        } else if (parameter instanceof HeaderParameter || "header".equalsIgnoreCase(parameter.getIn())) {
            codegenParameter.getVendorExtensions().put(CodegenConstants.IS_HEADER_PARAM_EXT_NAME, Boolean.TRUE);
        } else if (parameter instanceof CookieParameter || "cookie".equalsIgnoreCase(parameter.getIn())) {
            codegenParameter.getVendorExtensions().put(CodegenConstants.IS_COOKIE_PARAM_EXT_NAME, Boolean.TRUE);
        }
        /**
         * TODO: else if (parameter instanceof BodyParameter) { codegenParameter.isBodyParam = true;
         * codegenParameter.isBinary = isDataTypeBinary(codegenParameter.dataType); } else if (parameter instanceof
         * FormParameter) { if ("file".equalsIgnoreCase(((FormParameter) parameter).getType()) ||
         * "file".equals(codegenParameter.baseType)) { codegenParameter.isFile = true; } else { codegenParameter.notFile
         * = true; } codegenParameter.isFormParam = true; }
         */
        // set the example value
        // if not specified in x-example, generate a default value
        if (codegenParameter.vendorExtensions != null && codegenParameter.vendorExtensions.containsKey("x-example")) {
            codegenParameter.example = Json.pretty(codegenParameter.vendorExtensions.get("x-example"));
        } else if (getBooleanValue(codegenParameter, CodegenConstants.IS_STRING_EXT_NAME)) {
            codegenParameter.example = codegenParameter.paramName + "_example";
        } else if (getBooleanValue(codegenParameter, CodegenConstants.IS_BOOLEAN_EXT_NAME)) {
            codegenParameter.example = "true";
        } else if (getBooleanValue(codegenParameter, CodegenConstants.IS_LONG_EXT_NAME)) {
            codegenParameter.example = "789";
        } else if (getBooleanValue(codegenParameter, CodegenConstants.IS_INTEGER_EXT_NAME)) {
            codegenParameter.example = "56";
        } else if (getBooleanValue(codegenParameter, CodegenConstants.IS_FLOAT_EXT_NAME)) {
            codegenParameter.example = "3.4";
        } else if (getBooleanValue(codegenParameter, CodegenConstants.IS_DOUBLE_EXT_NAME)) {
            codegenParameter.example = "1.2";
        } else if (getBooleanValue(codegenParameter, CodegenConstants.IS_BINARY_EXT_NAME)) {
            codegenParameter.example = "BINARY_DATA_HERE";
        } else if (getBooleanValue(codegenParameter, CodegenConstants.IS_BYTE_ARRAY_EXT_NAME)) {
            codegenParameter.example = "B";
        } else if (getBooleanValue(codegenParameter, CodegenConstants.IS_FILE_EXT_NAME)) {
            codegenParameter.example = "/path/to/file.txt";
        } else if (getBooleanValue(codegenParameter, CodegenConstants.IS_DATE_EXT_NAME)) {
            codegenParameter.example = "2013-10-20";
        } else if (getBooleanValue(codegenParameter, CodegenConstants.IS_DATE_TIME_EXT_NAME)) {
            codegenParameter.example = "2013-10-20T19:20:30+01:00";
        } else if (getBooleanValue(codegenParameter, CodegenConstants.IS_UUID_EXT_NAME)) {
            codegenParameter.example = "38400000-8cf0-11bd-b23e-10b96e4ef00d";
        }

        // set the parameter excample value
        // should be overridden by lang codegen
        this.setParameterExampleValue(codegenParameter);

        this.postProcessParameter(codegenParameter);
        return codegenParameter;
    }

    public CodegenParameter fromRequestBody(final RequestBody body, String name, Schema schema,
            final Map<String, Schema> schemas, final Set<String> imports) {
        final CodegenParameter codegenParameter = CodegenModelFactory.newInstance(CodegenModelType.PARAMETER);

        String bodyName = REQUEST_BODY_NAME;
        if (body.getExtensions() != null && body.getExtensions().get("x-codegen-request-body-name") != null) {
            bodyName = body.getExtensions().get("x-codegen-request-body-name").toString();
        }
        codegenParameter.baseName = bodyName;
        codegenParameter.paramName = bodyName;
        codegenParameter.description = body.getDescription();
        codegenParameter.unescapedDescription = body.getDescription();
        codegenParameter.required = body.getRequired() != null ? body.getRequired() : false;
        codegenParameter.getVendorExtensions().put(CodegenConstants.IS_BODY_PARAM_EXT_NAME, Boolean.TRUE);

        codegenParameter.jsonSchema = Json.pretty(body);

        if (body.getContent() != null && !body.getContent().isEmpty()) {
            final Object example = new ArrayList<>(body.getContent().values()).get(0).getExample();
            if (example != null) {
                codegenParameter.example = Json.pretty(example);
            } else {
                final Map<String, Example> examples = new ArrayList<>(body.getContent().values()).get(0).getExamples();
                if (examples != null && !examples.isEmpty()) {
                    // get the first.. or concat all as json?
                    codegenParameter.example = Json.pretty(new ArrayList<>(examples.values()).get(0));
                }
            }
        }

        if (schema == null) {
            schema = this.getSchemaFromBody(body);
        }
        if (StringUtils.isNotBlank(schema.get$ref())) {
            name = OpenAPIUtil.getSimpleRef(schema.get$ref());
            schema = schemas.get(name);
        }
        if (this.isObjectSchema(schema)) {
            CodegenModel codegenModel = null;
            if (StringUtils.isNotBlank(name)) {
                schema.setName(name);
                codegenModel = this.fromModel(name, schema, schemas);
            }
            if (codegenModel != null) {
                codegenParameter.baseType = codegenModel.classname;
                codegenParameter.dataType = this.getTypeDeclaration(codegenModel.classname);
                imports.add(codegenParameter.dataType);
            } else {
                final CodegenProperty codegenProperty = this.fromProperty("property", schema);
                if (codegenProperty != null) {
                    codegenParameter.baseType = codegenProperty.baseType;
                    codegenParameter.dataType = codegenProperty.datatype;

                    final boolean isPrimitiveType = getBooleanValue(codegenProperty,
                            CodegenConstants.IS_PRIMITIVE_TYPE_EXT_NAME);
                    final boolean isBinary = getBooleanValue(codegenProperty, CodegenConstants.IS_BINARY_EXT_NAME);
                    final boolean isFile = getBooleanValue(codegenProperty, CodegenConstants.IS_FILE_EXT_NAME);

                    codegenParameter.getVendorExtensions().put(CodegenConstants.IS_PRIMITIVE_TYPE_EXT_NAME,
                            isPrimitiveType);
                    codegenParameter.getVendorExtensions().put(CodegenConstants.IS_BINARY_EXT_NAME, isBinary);
                    codegenParameter.getVendorExtensions().put(CodegenConstants.IS_FILE_EXT_NAME, isFile);

                    if (codegenProperty.complexType != null) {
                        imports.add(codegenProperty.complexType);
                    }
                }
                this.setParameterBooleanFlagWithCodegenProperty(codegenParameter, codegenProperty);
                this.setParameterNullable(codegenParameter, codegenProperty);
            }
        } else if (schema instanceof ArraySchema) {
            final ArraySchema arraySchema = (ArraySchema) schema;
            Schema inner = arraySchema.getItems();
            if (inner == null) {
                inner = new StringSchema().description("//TODO automatically added by swagger-codegen");
                arraySchema.setItems(inner);
            } else if (this.isObjectSchema(inner)) {
                // fixme: codegenParameter.getVendorExtensions().put(CodegenConstants.HAS_INNER_OBJECT_NAME,
                // Boolean.TRUE);
                codegenParameter.getVendorExtensions().put("x-has-inner-object", Boolean.TRUE);
            }

            CodegenProperty codegenProperty = this.fromProperty("property", schema);
            final CodegenProperty innerProperty = this.fromProperty("inner", arraySchema.getItems());
            codegenProperty.baseType = innerProperty.baseType;
            if (codegenProperty.complexType != null) {
                imports.add(codegenProperty.complexType);
            }
            if (codegenParameter.baseType != null) {
                imports.add(codegenProperty.baseType);
            }
            CodegenProperty innerCp = codegenProperty;
            while (innerCp != null) {
                if (innerCp.complexType != null) {
                    imports.add(innerCp.complexType);
                }
                innerCp = innerCp.items;
            }
            codegenParameter.items = codegenProperty;
            codegenParameter.dataType = codegenProperty.datatype;
            codegenParameter.baseType = codegenProperty.baseType;
            final boolean isPrimitiveType = getBooleanValue(codegenProperty,
                    CodegenConstants.IS_PRIMITIVE_TYPE_EXT_NAME);
            codegenParameter.getVendorExtensions().put(CodegenConstants.IS_PRIMITIVE_TYPE_EXT_NAME, isPrimitiveType);
            codegenParameter.getVendorExtensions().put(CodegenConstants.IS_CONTAINER_EXT_NAME, Boolean.TRUE);
            codegenParameter.getVendorExtensions().put(CodegenConstants.IS_LIST_CONTAINER_EXT_NAME, Boolean.TRUE);

            this.setParameterBooleanFlagWithCodegenProperty(codegenParameter, codegenProperty);
            this.setParameterNullable(codegenParameter, codegenProperty);

            while (codegenProperty != null) {
                if (codegenProperty.baseType != null) {
                    imports.add(codegenProperty.baseType);
                }
                codegenProperty = codegenProperty.items;
            }
        } else if (schema instanceof BinarySchema) {
            codegenParameter.dataType = "Object";
            codegenParameter.baseType = "Object";
            codegenParameter.getVendorExtensions().put(CodegenConstants.IS_BINARY_EXT_NAME, Boolean.TRUE);
        } else {
            final CodegenProperty codegenProperty = this.fromProperty(bodyName, schema);
            codegenParameter.dataType = codegenProperty.datatype;
            codegenParameter.baseType = codegenProperty.baseType;
            if (codegenProperty.complexType != null) {
                imports.add(codegenProperty.complexType);
            }
        }
        this.setParameterExampleValue(codegenParameter);
        this.postProcessParameter(codegenParameter);
        return codegenParameter;
    }

    public boolean isDataTypeBinary(final String dataType) {
        if (dataType != null) {
            return dataType.toLowerCase().startsWith("byte");
        }
        return false;
    }

    public boolean isDataTypeFile(final String dataType) {
        if (dataType != null) {
            return "file".equals(dataType.toLowerCase());
        }
        return false;
    }

    /**
     * Convert map of Swagger SecurityScheme objects to a list of Codegen Security objects
     * @param securitySchemeMap a map of Swagger SecuritySchemeDefinition object
     * @return a list of Codegen Security objects
     */
    @Override
    @SuppressWarnings("static-method")
    public List<CodegenSecurity> fromSecurity(final Map<String, SecurityScheme> securitySchemeMap) {
        if (securitySchemeMap == null) {
            return Collections.emptyList();
        }

        final List<CodegenSecurity> securities = new ArrayList<>(securitySchemeMap.size());
        for (final String key : securitySchemeMap.keySet()) {
            final SecurityScheme schemeDefinition = securitySchemeMap.get(key);

            final CodegenSecurity codegenSecurity = CodegenModelFactory.newInstance(CodegenModelType.SECURITY);
            codegenSecurity.name = key;
            codegenSecurity.type = schemeDefinition.getType().toString();

            if (SecurityScheme.Type.APIKEY.equals(schemeDefinition.getType())) {
                codegenSecurity.keyParamName = schemeDefinition.getName();
                codegenSecurity.getVendorExtensions().put(CodegenConstants.IS_API_KEY_EXT_NAME, Boolean.TRUE);

                final boolean isKeyInHeader = schemeDefinition.getIn() == SecurityScheme.In.HEADER;
                codegenSecurity.getVendorExtensions().put(CodegenConstants.IS_KEY_IN_HEADER_EXT_NAME, isKeyInHeader);
                codegenSecurity.getVendorExtensions().put(CodegenConstants.IS_KEY_IN_QUERY_EXT_NAME, !isKeyInHeader);

            } else if (SecurityScheme.Type.HTTP.equals(schemeDefinition.getType())) {
                if ("bearer".equalsIgnoreCase(schemeDefinition.getScheme())) {
                    codegenSecurity.getVendorExtensions().put(CodegenConstants.IS_BEARER_EXT_NAME, Boolean.TRUE);
                    final Map<String, Object> extensions = schemeDefinition.getExtensions();
                    if (extensions != null && extensions.get("x-token-example") != null) {
                        final String tokenExample = extensions.get("x-token-example").toString();
                        if (StringUtils.isNotBlank(tokenExample)) {
                            codegenSecurity.getVendorExtensions().put("x-token-example", tokenExample);
                        }
                    }
                } else {
                    codegenSecurity.getVendorExtensions().put(CodegenConstants.IS_BASIC_EXT_NAME, Boolean.TRUE);
                }
            } else if (SecurityScheme.Type.OAUTH2.equals(schemeDefinition.getType())) {
                codegenSecurity.getVendorExtensions().put(CodegenConstants.IS_OAUTH_EXT_NAME, Boolean.TRUE);
                final OAuthFlows flows = schemeDefinition.getFlows();
                if (schemeDefinition.getFlows() == null) {
                    throw new RuntimeException("missing oauth flow in " + codegenSecurity.name);
                }
                if (flows.getPassword() != null) {
                    this.setOauth2Info(codegenSecurity, flows.getPassword());
                    codegenSecurity.getVendorExtensions().put(CodegenConstants.IS_PASSWORD_EXT_NAME, Boolean.TRUE);
                    codegenSecurity.flow = "password";
                } else if (flows.getImplicit() != null) {
                    this.setOauth2Info(codegenSecurity, flows.getImplicit());
                    codegenSecurity.getVendorExtensions().put(CodegenConstants.IS_IMPLICIT_EXT_NAME, Boolean.TRUE);
                    codegenSecurity.flow = "implicit";
                } else if (flows.getClientCredentials() != null) {
                    this.setOauth2Info(codegenSecurity, flows.getClientCredentials());
                    codegenSecurity.getVendorExtensions().put(CodegenConstants.IS_APPLICATION_EXT_NAME, Boolean.TRUE);
                    codegenSecurity.flow = "application";
                } else if (flows.getAuthorizationCode() != null) {
                    this.setOauth2Info(codegenSecurity, flows.getAuthorizationCode());
                    codegenSecurity.getVendorExtensions().put(CodegenConstants.IS_CODE_EXT_NAME, Boolean.TRUE);
                    codegenSecurity.flow = "accessCode";
                } else {
                    throw new RuntimeException("Could not identify any oauth2 flow in " + codegenSecurity.name);
                }
            }

            securities.add(codegenSecurity);
        }

        // sort auth methods to maintain the same order
        Collections.sort(securities, (one, another) -> ObjectUtils.compare(one.name, another.name));
        // set 'hasMore'
        final Iterator<CodegenSecurity> it = securities.iterator();
        while (it.hasNext()) {
            final CodegenSecurity security = it.next();
            security.getVendorExtensions().put(CodegenConstants.HAS_MORE_EXT_NAME, it.hasNext());
        }

        return securities;
    }

    protected void setReservedWordsLowerCase(final List<String> words) {
        this.reservedWords = new HashSet<>();
        for (final String word : words) {
            this.reservedWords.add(word.toLowerCase());
        }
    }

    protected void setReservedWords(final List<String> words) {
        this.reservedWords = new HashSet<>();
        this.reservedWords.addAll(words);
    }

    protected boolean isReservedWord(final String word) {
        return word != null && this.reservedWords.contains(word.toLowerCase());
    }

    /**
     * Get operationId from the operation object, and if it's blank, generate a new one from the given parameters.
     * @param operation the operation object
     * @param path the path of the operation
     * @param httpMethod the HTTP method of the operation
     * @return the (generated) operationId
     */
    protected String getOrGenerateOperationId(final Operation operation, final String path, final String httpMethod) {
        String operationId = operation.getOperationId();
        if (StringUtils.isBlank(operationId)) {
            String tmpPath = path;
            tmpPath = tmpPath.replace("{", "");
            tmpPath = tmpPath.replace("}", "");
            final String[] parts = (tmpPath + "/" + httpMethod).split("/");
            final StringBuilder builder = new StringBuilder();
            if ("/".equals(tmpPath)) {
                // must be root tmpPath
                builder.append("root");
            }
            for (String part : parts) {
                if (part.length() > 0) {
                    if (builder.toString().length() == 0) {
                        part = Character.toLowerCase(part.charAt(0)) + part.substring(1);
                    } else {
                        part = this.initialCaps(part);
                    }
                    builder.append(part);
                }
            }
            operationId = this.sanitizeName(builder.toString());
            LOGGER.warn("Empty operationId found for path: " + httpMethod + " " + path
                    + ". Renamed to auto-generated operationId: " + operationId);
        }
        return operationId;
    }

    /**
     * Check the type to see if it needs import the library/module/package
     * @param type name of the type
     * @return true if the library/module/package of the corresponding type needs to be imported
     */
    protected boolean needToImport(final String type) {
        return StringUtils.isNotBlank(type) && !this.defaultIncludes.contains(type)
                && !this.languageSpecificPrimitives.contains(type);
    }

    @SuppressWarnings("static-method")
    protected List<Map<String, Object>> toExamples(final Map<String, Object> examples) {
        if (examples == null) {
            return null;
        }

        final List<Map<String, Object>> output = new ArrayList<>(examples.size());
        for (final Map.Entry<String, Object> entry : examples.entrySet()) {
            final Map<String, Object> kv = new HashMap<>();
            kv.put("contentType", entry.getKey());
            kv.put("example", entry.getValue());
            output.add(kv);
        }
        return output;
    }

    private void addHeaders(final ApiResponse response, final List<CodegenProperty> target,
            final Map<String, Header> componentHeaders) {
        if (response.getHeaders() != null) {
            for (final Map.Entry<String, Header> headers : response.getHeaders().entrySet()) {
                final Header header = headers.getValue();
                Schema schema;
                if ((header.get$ref() != null) && (componentHeaders != null)) {
                    final String ref = OpenAPIUtil.getSimpleRef(header.get$ref());
                    schema = componentHeaders.get(ref).getSchema();
                } else {
                    schema = header.getSchema();
                }
                target.add(this.fromProperty(headers.getKey(), schema));
            }
        }
    }

    private static Map<String, Object> addHasMore(final Map<String, Object> objs) {
        if (objs != null) {
            for (int i = 0; i < objs.size() - 1; i++) {
                if (i > 0) {
                    objs.put("secondaryParam", true);
                }
                if (i < objs.size() - 1) {
                    objs.put("hasMore", true);
                }
            }
        }
        return objs;
    }

    /**
     * Add operation to group
     * @param tag name of the tag
     * @param resourcePath path of the resource
     * @param operation Swagger Operation object
     * @param co Codegen Operation object
     * @param operations map of Codegen operations
     */
    @Override
    @SuppressWarnings("static-method")
    public void addOperationToGroup(final String tag, final String resourcePath, final Operation operation,
            final CodegenOperation co, final Map<String, List<CodegenOperation>> operations) {
        List<CodegenOperation> opList = operations.get(tag);
        if (opList == null) {
            opList = new ArrayList<>();
            operations.put(tag, opList);
        }
        // check for operationId uniqueness

        String uniqueName = co.operationId;
        int counter = 0;
        for (final CodegenOperation op : opList) {
            if (uniqueName.equals(op.operationId)) {
                uniqueName = co.operationId + "_" + counter;
                counter++;
            }
        }
        if (!co.operationId.equals(uniqueName)) {
            LOGGER.warn("generated unique operationId `" + uniqueName + "`");
        }
        co.operationId = uniqueName;
        co.operationIdLowerCase = uniqueName.toLowerCase();
        co.operationIdCamelCase = camelize(uniqueName);
        co.operationIdSnakeCase = underscore(uniqueName);
        opList.add(co);
        co.baseName = tag;
    }

    public void addParentContainer(final CodegenModel codegenModel, final String name, final Schema schema) {
        final CodegenProperty codegenProperty = this.fromProperty(name, schema);
        this.addImport(codegenModel, codegenProperty.complexType);
        codegenModel.parent = this.toInstantiationType(schema);
        final String containerType = codegenProperty.containerType;
        final String instantiationType = this.instantiationTypes.get(containerType);
        if (instantiationType != null) {
            this.addImport(codegenModel, instantiationType);
        }
        final String mappedType = this.typeMapping.get(containerType);
        if (mappedType != null) {
            this.addImport(codegenModel, mappedType);
        }
    }

    /**
     * Underscore the given word. Copied from Twitter elephant bird
     * https://github.com/twitter/elephant-bird/blob/master/core/src/main/java/com/twitter/elephantbird/util/Strings.java
     * @param word The word
     * @return The underscored version of the word
     */
    public static String underscore(String word) {
        final String firstPattern = "([A-Z]+)([A-Z][a-z][a-z]+)";
        final String secondPattern = "([a-z\\d])([A-Z])";
        final String replacementPattern = "$1_$2";
        // Replace package separator with slash.
        word = word.replace('.', '/'); // FIXME: a parameter should not be assigned. Also declare the methods parameters
                                       // as 'final'.
        // Replace $ with two underscores for inner classes.
        word = word.replace("$", "__");
        // Replace capital letter with _ plus lowercase letter.
        word = word.replaceAll(firstPattern, replacementPattern);
        word = word.replaceAll(secondPattern, replacementPattern);
        word = word.replace('-', '_');
        // replace space with underscore
        word = word.replace(' ', '_');
        return word.toLowerCase();
    }

    /**
     * Dashize the given word.
     * @param word The word
     * @return The dashized version of the word, e.g. "my-name"
     */
    @SuppressWarnings("static-method")
    protected String dashize(final String word) {
        return underscore(word).replaceAll("[_ ]", "-");
    }

    /**
     * Generate the next name for the given name, i.e. append "2" to the base name if not ending with a number,
     * otherwise increase the number by 1. For example: status => status2 status2 => status3 myName100 => myName101
     * @param name The base name
     * @return The next name for the base name
     */
    private static String generateNextName(final String name) {
        final Pattern pattern = Pattern.compile("\\d+\\z");
        final Matcher matcher = pattern.matcher(name);
        if (matcher.find()) {
            final String numStr = matcher.group();
            final int num = Integer.parseInt(numStr) + 1;
            return name.substring(0, name.length() - numStr.length()) + num;
        }
        return name + "2";
    }

    protected void addImport(final CodegenModel m, final String type) {
        if (type != null && this.needToImport(type)) {
            m.imports.add(type);
        }
    }

    protected void addVars(final CodegenModel codegenModel, final Map<String, Schema> properties,
            final List<String> required) {
        this.addVars(codegenModel, properties, required, null, null);
    }

    private void addVars(final CodegenModel codegenModel, final Map<String, Schema> properties,
            final List<String> required, final Map<String, Schema> allProperties, final List<String> allRequired) {

        codegenModel.getVendorExtensions().put(CodegenConstants.HAS_REQUIRED_EXT_NAME, Boolean.FALSE);
        if (properties != null && !properties.isEmpty()) {
            codegenModel.getVendorExtensions().put(CodegenConstants.HAS_VARS_EXT_NAME, true);
            codegenModel.getVendorExtensions().put(CodegenConstants.HAS_ENUMS_EXT_NAME, false);

            final Set<String> mandatory = required == null ? Collections.<String> emptySet() : new TreeSet<>(required);
            this.addVars(codegenModel, codegenModel.vars, properties, mandatory);
            codegenModel.allMandatory = codegenModel.mandatory = mandatory;
        } else {
            codegenModel.emptyVars = true;
            codegenModel.getVendorExtensions().put(CodegenConstants.HAS_VARS_EXT_NAME, false);
            codegenModel.getVendorExtensions().put(CodegenConstants.HAS_ENUMS_EXT_NAME, false);
        }

        if (allProperties != null) {
            final Set<String> allMandatory = allRequired == null ? Collections.<String> emptySet()
                    : new TreeSet<>(allRequired);
            this.addVars(codegenModel, codegenModel.allVars, allProperties, allMandatory);
            codegenModel.allMandatory = allMandatory;
        }
    }

    private void addVars(final CodegenModel codegenModel, final List<CodegenProperty> vars,
            final Map<String, Schema> properties, final Set<String> mandatory) {
        // convert set to list so that we can access the next entry in the loop
        final List<Map.Entry<String, Schema>> propertyList = new ArrayList<>(properties.entrySet());
        final int totalCount = propertyList.size();
        for (int i = 0; i < totalCount; i++) {
            final Map.Entry<String, Schema> entry = propertyList.get(i);

            final String key = entry.getKey();
            final Schema propertySchema = entry.getValue();

            if (propertySchema == null) {
                LOGGER.warn("null property for " + key);
                continue;
            }
            final CodegenProperty codegenProperty = this.fromProperty(key, propertySchema);
            codegenProperty.required = mandatory.contains(key);

            if (propertySchema.get$ref() != null) {
                if (this.openAPI == null) {
                    LOGGER.warn("open api utility object was not properly set.");
                } else {
                    OpenAPIUtil.addPropertiesFromRef(this.openAPI, propertySchema, codegenProperty);
                }
            }

            final boolean hasRequired = getBooleanValue(codegenModel, HAS_REQUIRED_EXT_NAME)
                    || codegenProperty.required;
            final boolean hasOptional = getBooleanValue(codegenModel, HAS_OPTIONAL_EXT_NAME)
                    || !codegenProperty.required;

            codegenModel.getVendorExtensions().put(HAS_REQUIRED_EXT_NAME, hasRequired);
            codegenModel.getVendorExtensions().put(HAS_OPTIONAL_EXT_NAME, hasOptional);

            final boolean isEnum = getBooleanValue(codegenProperty, IS_ENUM_EXT_NAME);
            if (isEnum) {
                // FIXME: if supporting inheritance, when called a second time for allProperties it is possible for
                // m.hasEnums to be set incorrectly if allProperties has enumerations but properties does not.
                codegenModel.getVendorExtensions().put(CodegenConstants.HAS_ENUMS_EXT_NAME, true);
            }

            // set model's hasOnlyReadOnly to false if the property is read-only
            if (!getBooleanValue(codegenProperty, CodegenConstants.IS_READ_ONLY_EXT_NAME)) {
                codegenModel.getVendorExtensions().put(HAS_ONLY_READ_ONLY_EXT_NAME, Boolean.FALSE);
            }

            if (i + 1 != totalCount) {
                codegenProperty.getVendorExtensions().put(CodegenConstants.HAS_MORE_EXT_NAME, Boolean.TRUE);
                // check the next entry to see if it's read only
                if (!Boolean.TRUE.equals(propertyList.get(i + 1).getValue().getReadOnly())) {
                    codegenProperty.getVendorExtensions().put(CodegenConstants.HAS_MORE_NON_READ_ONLY_EXT_NAME,
                            Boolean.TRUE);
                }
            }

            if (getBooleanValue(codegenProperty, CodegenConstants.IS_CONTAINER_EXT_NAME)) {
                this.addImport(codegenModel, this.typeMapping.get("array"));
            }

            this.addImport(codegenModel, codegenProperty.baseType);
            CodegenProperty innerCp = codegenProperty;
            while (innerCp != null) {
                this.addImport(codegenModel, innerCp.complexType);
                innerCp = innerCp.items;
            }
            vars.add(codegenProperty);

            // if required, add to the list "requiredVars"
            if (Boolean.TRUE.equals(codegenProperty.required)) {
                codegenModel.requiredVars.add(codegenProperty);
            } else { // else add to the list "optionalVars" for optional property
                codegenModel.optionalVars.add(codegenProperty);
            }

            // if readonly, add to readOnlyVars (list of properties)
            if (getBooleanValue(codegenProperty, CodegenConstants.IS_READ_ONLY_EXT_NAME)) {
                codegenModel.readOnlyVars.add(codegenProperty);
            } else { // else add to readWriteVars (list of properties)
                // FIXME: readWriteVars can contain duplicated properties. Debug/breakpoint here while running C#
                // generator (Dog and Cat models)
                codegenModel.readWriteVars.add(codegenProperty);
            }
        }
        // check if one of the property is a object and has import mapping.
        final List<CodegenProperty> modelProperties = vars.stream()
                .filter(codegenProperty -> getBooleanValue(codegenProperty, "x-is-object")
                        && this.importMapping.containsKey(codegenProperty.baseType))
                .collect(Collectors.toList());
        if (modelProperties == null || modelProperties.isEmpty()) {
            return;
        }

        for (final CodegenProperty modelProperty : modelProperties) {
            final List<CodegenProperty> codegenProperties = vars.stream()
                    .filter(codegenProperty -> !getBooleanValue(codegenProperty, "x-is-object")
                            && this.importMapping.containsKey(codegenProperty.baseType)
                            && codegenProperty.baseType.equals(modelProperty.baseType))
                    .collect(Collectors.toList());
            if (codegenProperties == null || codegenProperties.isEmpty()) {
                continue;
            }
            for (final CodegenProperty codegenProperty : codegenProperties) {
                codegenModel.imports.remove(codegenProperty.baseType);
                codegenProperty.datatype = this.importMapping.get(codegenProperty.baseType);
                codegenProperty.datatypeWithEnum = codegenProperty.datatype;
            }
        }
    }

    /**
     * Determine all of the types in the model definitions that are aliases of simple types.
     * @param allSchemas The complete set of model definitions.
     * @return A mapping from model name to type alias
     */
    private static Map<String, String> getAllAliases(final Map<String, Schema> allSchemas) {
        final Map<String, String> aliases = new HashMap<>();
        if (allSchemas == null || allSchemas.isEmpty()) {
            return aliases;
        }
        for (final Map.Entry<String, Schema> entry : allSchemas.entrySet()) {
            final String swaggerName = entry.getKey();
            final Schema schema = entry.getValue();

            if (schema instanceof ArraySchema || schema instanceof MapSchema) {
                continue;
            }

            final String schemaType = getTypeOfSchema(schema);
            if (schemaType != null && !"object".equals(schemaType) && schema.getEnum() == null) {
                aliases.put(swaggerName, schemaType);
            }
        }
        return aliases;
    }

    /**
     * Remove characters not suitable for variable or method name from the input and camelize it
     * @param name string to be camelize
     * @return camelized string
     */
    @SuppressWarnings("static-method")
    public String removeNonNameElementToCamelCase(final String name) {
        return this.removeNonNameElementToCamelCase(name, "[-_:;#]");
    }

    /**
     * Remove characters that is not good to be included in method name from the input and camelize it
     * @param name string to be camelize
     * @param nonNameElementPattern a regex pattern of the characters that is not good to be included in name
     * @return camelized string
     */
    protected String removeNonNameElementToCamelCase(final String name, final String nonNameElementPattern) {
        String result = Arrays.stream(name.split(nonNameElementPattern)).map(StringUtils::capitalize)
                .collect(Collectors.joining(""));
        if (result.length() > 0) {
            result = result.substring(0, 1).toLowerCase() + result.substring(1);
        }
        return result;
    }

    /**
     * Camelize name (parameter, property, method, etc) with upper case for first letter copied from Twitter elephant
     * bird
     * https://github.com/twitter/elephant-bird/blob/master/core/src/main/java/com/twitter/elephantbird/util/Strings.java
     * @param word string to be camelize
     * @return camelized string
     */
    public static String camelize(final String word) {
        return camelize(word, false);
    }

    /**
     * Camelize name (parameter, property, method, etc)
     * @param word string to be camelize
     * @param lowercaseFirstLetter lower case for first letter if set to true
     * @return camelized string
     */
    public static String camelize(String word, final boolean lowercaseFirstLetter) {
        // Replace all slashes with dots (package separator)
        final String originalWord = word;
        LOGGER.trace("camelize start - " + originalWord);
        Pattern p = Pattern.compile("\\/(.?)");
        Matcher m = p.matcher(word);
        int i = 0;
        final int MAX = 100;
        while (m.find()) {
            if (i > MAX) {
                LOGGER.error("camelize reached find limit - {} / {}", originalWord, word);
                break;
            }
            i++;
            word = m.replaceFirst("." + m.group(1)/* .toUpperCase() */); // FIXME: a parameter should not be assigned.
                                                                         // Also declare the methods parameters as
                                                                         // 'final'.
            m = p.matcher(word);
        }
        i = 0;
        // case out dots
        final String[] parts = word.split("\\.");
        final StringBuilder f = new StringBuilder();
        for (final String z : parts) {
            if (z.length() > 0) {
                f.append(Character.toUpperCase(z.charAt(0))).append(z.substring(1));
            }
        }
        word = f.toString();

        m = p.matcher(word);
        while (m.find()) {
            if (i > MAX) {
                LOGGER.error("camelize reached find limit - {} / {}", originalWord, word);
                break;
            }
            i++;
            word = m.replaceFirst(
                    "" + Character.toUpperCase(m.group(1).charAt(0)) + m.group(1).substring(1)/* .toUpperCase() */);
            m = p.matcher(word);
        }
        i = 0;
        // Uppercase the class name.
        p = Pattern.compile("(\\.?)(\\w)([^\\.]*)$");
        m = p.matcher(word);
        if (m.find()) {
            String rep = m.group(1) + m.group(2).toUpperCase() + m.group(3);
            rep = rep.replace("$", "\\$");
            word = m.replaceAll(rep);
        }

        // Remove all underscores (underscore_case to camelCase)
        p = Pattern.compile("(_)(.)");
        m = p.matcher(word);
        while (m.find()) {
            if (i > MAX) {
                LOGGER.error("camelize reached find limit - {} / {}", originalWord, word);
                break;
            }
            i++;
            final String original = m.group(2);
            final String upperCase = original.toUpperCase();
            if (original.equals(upperCase)) {
                word = word.replaceFirst("_", "");
            } else {
                word = m.replaceFirst(upperCase);
            }
            m = p.matcher(word);
        }

        // Remove all hyphens (hyphen-case to camelCase)
        p = Pattern.compile("(-)(.)");
        m = p.matcher(word);
        i = 0;
        while (m.find()) {
            if (i > MAX) {
                LOGGER.error("camelize reached find limit - {} / {}", originalWord, word);
                break;
            }
            i++;
            word = m.replaceFirst(m.group(2).toUpperCase());
            m = p.matcher(word);
        }

        if (lowercaseFirstLetter && word.length() > 0) {
            word = word.substring(0, 1).toLowerCase() + word.substring(1);
        }
        LOGGER.trace("camelize end - {} (new: {})", originalWord, word);
        return word;
    }

    @Override
    public String apiFilename(final String templateName, final String tag) {
        final String suffix = this.apiTemplateFiles().get(templateName);
        return this.apiFileFolder() + File.separator + this.toApiFilename(tag) + suffix;
    }

    /**
     * Return the full path and API documentation file
     * @param templateName template name
     * @param tag tag
     * @return the API documentation file name with full path
     */
    @Override
    public String apiDocFilename(final String templateName, final String tag) {
        final String suffix = this.apiDocTemplateFiles().get(templateName);
        return this.apiDocFileFolder() + '/' + this.toApiDocFilename(tag) + suffix;
    }

    /**
     * Return the full path and API test file
     * @param templateName template name
     * @param tag tag
     * @return the API test file name with full path
     */
    @Override
    public String apiTestFilename(final String templateName, final String tag) {
        final String suffix = this.apiTestTemplateFiles().get(templateName);
        return this.apiTestFileFolder() + '/' + this.toApiTestFilename(tag) + suffix;
    }

    @Override
    public boolean shouldOverwrite(final String filename) {
        return !(this.skipOverwrite && new File(filename).exists());
    }

    @Override
    public boolean isSkipOverwrite() {
        return this.skipOverwrite;
    }

    @Override
    public void setSkipOverwrite(final boolean skipOverwrite) {
        this.skipOverwrite = skipOverwrite;
    }

    @Override
    public boolean isRemoveOperationIdPrefix() {
        return this.removeOperationIdPrefix;
    }

    @Override
    public void setRemoveOperationIdPrefix(final boolean removeOperationIdPrefix) {
        this.removeOperationIdPrefix = removeOperationIdPrefix;
    }

    /**
     * All library languages supported. (key: library name, value: library description)
     * @return the supported libraries
     */
    @Override
    public Map<String, String> supportedLibraries() {
        return this.supportedLibraries;
    }

    /**
     * Set library template (sub-template).
     * @param library Library template
     */
    @Override
    public void setLibrary(final String library) {
        if (library != null && !this.supportedLibraries.containsKey(library)) {
            final StringBuilder sb = new StringBuilder("Unknown library: " + library + "\nAvailable libraries:");
            if (this.supportedLibraries.size() == 0) {
                sb.append("\n  ").append("NONE");
            } else {
                for (final String lib : this.supportedLibraries.keySet()) {
                    sb.append("\n  ").append(lib);
                }
            }
            throw new RuntimeException(sb.toString());
        }
        this.library = library;
    }

    /**
     * Library template (sub-template).
     * @return Library template
     */
    @Override
    public String getLibrary() {
        return this.library;
    }

    /**
     * Set Git user ID.
     * @param gitUserId Git user ID
     */
    @Override
    public void setGitUserId(final String gitUserId) {
        this.gitUserId = gitUserId;
    }

    /**
     * Git user ID
     * @return Git user ID
     */
    @Override
    public String getGitUserId() {
        return this.gitUserId;
    }

    /**
     * Set Git repo ID.
     * @param gitRepoId Git repo ID
     */
    @Override
    public void setGitRepoId(final String gitRepoId) {
        this.gitRepoId = gitRepoId;
    }

    /**
     * Git repo ID
     * @return Git repo ID
     */
    @Override
    public String getGitRepoId() {
        return this.gitRepoId;
    }

    /**
     * Git repo Base URL
     * @return Git repo Base URL
     */
    @Override
    public String getGitRepoBaseURL() {
        return this.gitRepoBaseURL;
    }

    /**
     * Set Git repo Base URL.
     * @param gitRepoBaseURL Git repo Base URL
     */
    @Override
    public void setGitRepoBaseURL(final String gitRepoBaseURL) {
        this.gitRepoBaseURL = gitRepoBaseURL;
    }

    /**
     * Set release note.
     * @param releaseNote Release note
     */
    @Override
    public void setReleaseNote(final String releaseNote) {
        this.releaseNote = releaseNote;
    }

    /**
     * Release note
     * @return Release note
     */
    @Override
    public String getReleaseNote() {
        return this.releaseNote;
    }

    /**
     * Set HTTP user agent.
     * @param httpUserAgent HTTP user agent
     */
    @Override
    public void setHttpUserAgent(final String httpUserAgent) {
        this.httpUserAgent = httpUserAgent;
    }

    /**
     * HTTP user agent
     * @return HTTP user agent
     */
    @Override
    public String getHttpUserAgent() {
        return this.httpUserAgent;
    }

    /**
     * Hide generation timestamp
     * @param hideGenerationTimestamp flag to indicates if the generation timestamp should be hidden or not
     */
    public void setHideGenerationTimestamp(final Boolean hideGenerationTimestamp) {
        this.hideGenerationTimestamp = hideGenerationTimestamp;
    }

    /**
     * Hide generation timestamp
     * @return if the generation timestamp should be hidden or not
     */
    public Boolean getHideGenerationTimestamp() {
        return this.hideGenerationTimestamp;
    }

    @SuppressWarnings("static-method")
    protected CliOption buildLibraryCliOption(final Map<String, String> supportedLibraries) {
        final StringBuilder sb = new StringBuilder("library template (sub-template) to use:");
        for (final String lib : supportedLibraries.keySet()) {
            sb.append("\n").append(lib).append(" - ").append(supportedLibraries.get(lib));
        }
        return new CliOption("library", sb.toString());
    }

    /**
     * Sanitize name (parameter, property, method, etc)
     * @param name string to be sanitize
     * @return sanitized string
     */
    @Override
    @SuppressWarnings("static-method")
    public String sanitizeName(String name) {
        // NOTE: performance wise, we should have written with 2 replaceAll to replace desired
        // character with _ or empty character. Below aims to spell out different cases we've
        // encountered so far and hopefully make it easier for others to add more special
        // cases in the future.

        // better error handling when map/array type is invalid
        if (this.maybeHandleEmptyName(name)) {
            return Object.class.getSimpleName();
        }

        // if the name is just '$', map it to 'value' for the time being.
        if (this.maybeHandleDollarName(name)) {
            return "value";
        }

        // input[] => input
        name = name.replace("[]", ""); // FIXME: a parameter should not be assigned. Also declare the methods parameters
                                       // as 'final'.

        // input[a][b] => input_a_b
        name = name.replace('[', '_');
        name = name.replace("]", "");

        // input(a)(b) => input_a_b
        name = name.replace('(', '_');
        name = name.replace(")", "");

        // input.name => input_name
        name = name.replace('.', '_');

        // input-name => input_name
        name = name.replace('-', '_');

        // input name and age => input_name_and_age
        name = name.replace(' ', '_');

        // remove everything else other than word, number and _
        // $php_variable => php_variable
        if (this.allowUnicodeIdentifiers) { // could be converted to a single line with ?: operator
            name = Pattern.compile("\\W", Pattern.UNICODE_CHARACTER_CLASS).matcher(name).replaceAll("");
        } else {
            name = name.replaceAll("\\W", "");
        }

        return name;
    }

    private boolean maybeHandleDollarName(final String name) {
        if ("$".equals(name)) {
            return true;
        }
        return false;
    }

    private boolean maybeHandleEmptyName(final String name) {
        if (name == null) {
            LOGGER.warn("String to be sanitized is null. Default to " + Object.class.getSimpleName());
            return true;
        }
        return false;
    }

    /**
     * Sanitize tag
     * @param tag Tag
     * @return Sanitized tag
     */
    @Override
    public String sanitizeTag(String tag) {
        tag = camelize(this.sanitizeName(tag));

        // tag starts with numbers
        if (tag.matches("^\\d.*")) {
            tag = "Class" + tag;
        }

        return tag;
    }

    @Override
    public void addHandlebarHelpers(final Handlebars handlebars) {
        handlebars.registerHelper(IsHelper.NAME, new IsHelper());
        handlebars.registerHelper(HasHelper.NAME, new HasHelper());
        handlebars.registerHelper(IsNotHelper.NAME, new IsNotHelper());
        handlebars.registerHelper(HasNotHelper.NAME, new HasNotHelper());
        handlebars.registerHelper(BracesHelper.NAME, new BracesHelper());
        handlebars.registerHelper(BaseItemsHelper.NAME, new BaseItemsHelper());
        handlebars.registerHelper(NotEmptyHelper.NAME, new NotEmptyHelper());
        handlebars.registerHelpers(new StringUtilHelper());
    }

    @Override
    public List<CodegenArgument> readLanguageArguments() {
        final String argumentsLocation = this.getArgumentsLocation();
        if (StringUtils.isBlank(argumentsLocation)) {
            return null;
        }
        final InputStream inputStream = this.getClass().getResourceAsStream(argumentsLocation);
        if (inputStream == null) {
            return null;
        }
        final String content;
        try {
            content = IOUtils.toString(inputStream);
            if (StringUtils.isBlank(content)) {
                return null;
            }
        } catch (final IOException e) {
            LOGGER.error("Could not read arguments for java language.", e);
            return null;
        }
        final JsonNode rootNode;
        try {
            rootNode = Yaml.mapper().readTree(content.getBytes());
            if (rootNode == null) {
                return null;
            }
        } catch (final IOException e) {
            LOGGER.error("Could not parse java arguments content.", e);
            return null;
        }
        final JsonNode arguments = rootNode.findValue("arguments");
        if (arguments == null || !arguments.isArray()) {
            return null;
        }
        final List<CodegenArgument> languageArguments = new ArrayList<>();
        for (final JsonNode argument : arguments) {
            final String option = argument.findValue("option") != null ? argument.findValue("option").textValue()
                    : null;
            final String description = argument.findValue("description") != null
                    ? argument.findValue("description").textValue()
                    : null;
            final String shortOption = argument.findValue("shortOption") != null
                    ? argument.findValue("shortOption").textValue()
                    : null;
            final String type = argument.findValue("type") != null ? argument.findValue("type").textValue() : "string";
            final boolean isArray = argument.findValue("isArray") != null ? argument.findValue("isArray").booleanValue()
                    : false;

            languageArguments.add(new CodegenArgument().option(option).shortOption(shortOption).description(description)
                    .type(type).isArray(isArray));
        }
        return languageArguments;
    }

    @Override
    public void setLanguageArguments(final List<CodegenArgument> languageArguments) {
        this.languageArguments = languageArguments;
    }

    @Override
    public List<CodegenArgument> getLanguageArguments() {
        return this.languageArguments;
    }

    public String getArgumentsLocation() {
        return null;
    }

    protected String getOptionValue(final String optionName) {
        final List<CodegenArgument> codegenArguments = this.getLanguageArguments();
        if (codegenArguments == null || codegenArguments.isEmpty()) {
            return null;
        }
        final Optional<CodegenArgument> codegenArgumentOptional = codegenArguments.stream()
                .filter(argument -> argument.getOption().equalsIgnoreCase(optionName)).findAny();
        if (!codegenArgumentOptional.isPresent()) {
            return null;
        }
        return codegenArgumentOptional.get().getValue();
    }

    /**
     * Only write if the file doesn't exist
     * @param outputFolder Output folder
     * @param supportingFile Supporting file
     */
    public void writeOptional(final String outputFolder, final SupportingFile supportingFile) {
        String folder = "";

        if (outputFolder != null && !"".equals(outputFolder)) {
            folder += outputFolder + File.separator;
        }
        folder += supportingFile.folder;
        if (!"".equals(folder)) {
            folder += File.separator + supportingFile.destinationFilename;
        } else {
            folder = supportingFile.destinationFilename;
        }
        if (!new File(folder).exists()) {
            this.supportingFiles.add(supportingFile);
        } else {
            LOGGER.info("Skipped overwriting " + supportingFile.destinationFilename + " as the file already exists in "
                    + folder);
        }
    }

    /**
     * Set CodegenParameter boolean flag using CodegenProperty.
     * @param parameter Codegen Parameter
     * @param property Codegen property
     */
    public void setParameterBooleanFlagWithCodegenProperty(final CodegenParameter parameter,
            final CodegenProperty property) {
        if (parameter == null) {
            LOGGER.error("Codegen Parameter cannot be null.");
            return;
        }

        if (property == null) {
            LOGGER.error("Codegen Property cannot be null.");
            return;
        }

        parameter.getVendorExtensions().put(CodegenConstants.IS_PRIMITIVE_TYPE_EXT_NAME, Boolean.TRUE);

        if (getBooleanValue(property, CodegenConstants.IS_UUID_EXT_NAME)
                && getBooleanValue(property, CodegenConstants.IS_STRING_EXT_NAME)) {
            parameter.getVendorExtensions().put(CodegenConstants.IS_UUID_EXT_NAME, Boolean.TRUE);
            parameter.getVendorExtensions().put(CodegenConstants.IS_PRIMITIVE_TYPE_EXT_NAME, Boolean.FALSE);
        } else if (getBooleanValue(property, CodegenConstants.IS_BYTE_ARRAY_EXT_NAME)) {
            parameter.getVendorExtensions().put(CodegenConstants.IS_BYTE_ARRAY_EXT_NAME, Boolean.TRUE);
        } else if (getBooleanValue(property, CodegenConstants.IS_STRING_EXT_NAME)) {
            parameter.getVendorExtensions().put(CodegenConstants.IS_STRING_EXT_NAME, Boolean.TRUE);
        } else if (getBooleanValue(property, CodegenConstants.IS_BOOLEAN_EXT_NAME)) {
            parameter.getVendorExtensions().put(CodegenConstants.IS_BOOLEAN_EXT_NAME, Boolean.TRUE);
        } else if (getBooleanValue(property, CodegenConstants.IS_LONG_EXT_NAME)) {
            parameter.getVendorExtensions().put(CodegenConstants.IS_LONG_EXT_NAME, Boolean.TRUE);
        } else if (getBooleanValue(property, CodegenConstants.IS_INTEGER_EXT_NAME)) {
            parameter.getVendorExtensions().put(CodegenConstants.IS_INTEGER_EXT_NAME, Boolean.TRUE);
        } else if (getBooleanValue(property, CodegenConstants.IS_DOUBLE_EXT_NAME)) {
            parameter.getVendorExtensions().put(CodegenConstants.IS_DOUBLE_EXT_NAME, Boolean.TRUE);
        } else if (getBooleanValue(property, CodegenConstants.IS_FLOAT_EXT_NAME)) {
            parameter.getVendorExtensions().put(CodegenConstants.IS_FLOAT_EXT_NAME, Boolean.TRUE);
        } else if (getBooleanValue(property, CodegenConstants.IS_NUMBER_EXT_NAME)) {
            parameter.getVendorExtensions().put(CodegenConstants.IS_NUMBER_EXT_NAME, Boolean.TRUE);
        } else if (getBooleanValue(property, CodegenConstants.IS_BINARY_EXT_NAME)) {
            parameter.getVendorExtensions().put(CodegenConstants.IS_BYTE_ARRAY_EXT_NAME, Boolean.TRUE);
        } else if (getBooleanValue(property, CodegenConstants.IS_FILE_EXT_NAME)) {
            parameter.getVendorExtensions().put(CodegenConstants.IS_FILE_EXT_NAME, Boolean.TRUE);
        } else if (getBooleanValue(property, CodegenConstants.IS_DATE_EXT_NAME)) {
            parameter.getVendorExtensions().put(CodegenConstants.IS_DATE_EXT_NAME, Boolean.TRUE);
        } else if (getBooleanValue(property, CodegenConstants.IS_DATE_TIME_EXT_NAME)) {
            parameter.getVendorExtensions().put(CodegenConstants.IS_DATE_TIME_EXT_NAME, Boolean.TRUE);
        } else {
            LOGGER.debug("Property type is not primitive: " + property.datatype);
            parameter.getVendorExtensions().put(CodegenConstants.IS_PRIMITIVE_TYPE_EXT_NAME, Boolean.FALSE);
        }
    }

    /**
     * Update codegen property's enum by adding "enumVars" (with name and value)
     * @param var list of CodegenProperty
     */
    public void updateCodegenPropertyEnum(final CodegenProperty var) {
        Map<String, Object> allowableValues = var.allowableValues;

        // handle ArrayProperty
        if (var.items != null) {
            allowableValues = var.items.allowableValues;
        }

        if (allowableValues == null) {
            return;
        }

        final List<Object> values = (List<Object>) allowableValues.get("values");
        if (values == null) {
            return;
        }

        // put "enumVars" map into `allowableValues", including `name` and `value`
        final List<Map<String, String>> enumVars = new ArrayList<>();
        final String commonPrefix = this.findCommonPrefixOfVars(values);
        final int truncateIdx = commonPrefix.length();
        for (final Object value : values) {
            final Map<String, String> enumVar = new HashMap<>();
            final String enumName = this.findEnumName(truncateIdx, value);
            enumVar.put("name", this.toEnumVarName(enumName, var.datatype));
            if (value == null) {
                enumVar.put("value", this.toEnumValue(null, var.datatype));
            } else {
                enumVar.put("value", this.toEnumValue(value.toString(), var.datatype));
            }
            enumVars.add(enumVar);
        }
        allowableValues.put("enumVars", enumVars);

        // check repeated enum var names
        if (enumVars != null & !enumVars.isEmpty()) {
            for (int i = 0; i < enumVars.size(); i++) {
                final Map<String, String> enumVarList = enumVars.get(i);
                final String enumVarName = enumVarList.get("name");
                for (int j = 0; j < enumVars.size(); j++) {
                    if (i == j) {
                        continue;
                    }
                    final Map<String, String> enumVarToCheckList = enumVars.get(j);
                    final String enumVarNameToCheck = enumVarToCheckList.get("name");
                    if (enumVarName.equals(enumVarNameToCheck)) {
                        enumVarToCheckList.put("name", enumVarName + "_" + j);
                    }
                }
            }
        }

        // handle default value for enum, e.g. available => StatusEnum.AVAILABLE
        if (var.defaultValue != null) {
            String enumName = null;
            for (final Map<String, String> enumVar : enumVars) {
                if (this.toEnumValue(var.defaultValue, var.datatype).equals(enumVar.get("value"))) {
                    enumName = enumVar.get("name");
                    break;
                }
            }
            if (enumName != null) {
                var.defaultValue = String.format("%s.%s", var.datatypeWithEnum, enumName);
            }
        }
    }

    /**
     * If the pattern misses the delimiter, add "/" to the beginning and end Otherwise, return the original pattern
     * @param pattern the pattern (regular expression)
     * @return the pattern with delimiter
     */
    public String addRegularExpressionDelimiter(final String pattern) {
        if (StringUtils.isEmpty(pattern)) {
            return pattern;
        }

        if (!pattern.matches("^/.*")) {
            return "/" + pattern.replace("/", "\\/") + "/";
        }

        return pattern;
    }

    /**
     * reads propertyKey from additionalProperties, converts it to a boolean and writes it back to additionalProperties
     * to be usable as a boolean in mustache files.
     * @param propertyKey property key
     * @return property value as boolean
     */
    public boolean convertPropertyToBooleanAndWriteBack(final String propertyKey) {
        boolean booleanValue = false;
        if (this.additionalProperties.containsKey(propertyKey)) {
            booleanValue = this.convertPropertyToBoolean(propertyKey);
            // write back as boolean
            this.writePropertyBack(propertyKey, booleanValue);
        }

        return booleanValue;
    }

    /**
     * Provides an override location, if any is specified, for the .swagger-codegen-ignore. This is originally intended
     * for the first generation only.
     * @return a string of the full path to an override ignore file.
     */
    @Override
    public String getIgnoreFilePathOverride() {
        return this.ignoreFilePathOverride;
    }

    /**
     * Sets an override location for the .swagger-codegen.ignore location for the first code generation.
     * @param ignoreFileOverride The full path to an ignore file
     */
    @Override
    public void setIgnoreFilePathOverride(final String ignoreFileOverride) {
        this.ignoreFilePathOverride = ignoreFileOverride;
    }

    public void setUseOas2(final boolean useOas2) {
        this.useOas2 = useOas2;
    }

    public abstract String getDefaultTemplateDir();

    public boolean convertPropertyToBoolean(final String propertyKey) {
        boolean booleanValue = false;
        if (this.additionalProperties.containsKey(propertyKey)) {
            booleanValue = Boolean.parseBoolean(this.additionalProperties.get(propertyKey).toString());
        }

        return booleanValue;
    }

    public void writePropertyBack(final String propertyKey, final boolean value) {
        this.additionalProperties.put(propertyKey, value);
    }

    protected void addOption(final String key, final String description) {
        this.addOption(key, description, null);
    }

    protected void addOption(final String key, final String description, final String defaultValue) {
        final CliOption option = new CliOption(key, description);
        if (defaultValue != null) {
            option.defaultValue(defaultValue);
        }
        this.cliOptions.add(option);
    }

    protected void addSwitch(final String key, final String description, final Boolean defaultValue) {
        final CliOption option = CliOption.newBoolean(key, description);
        if (defaultValue != null) {
            option.defaultValue(defaultValue.toString());
        }
        this.cliOptions.add(option);
    }

    protected String getContentType(final RequestBody requestBody) {
        if (requestBody == null || requestBody.getContent() == null || requestBody.getContent().isEmpty()) {
            return null;
        }
        return new ArrayList<>(requestBody.getContent().keySet()).get(0);
    }

    protected Schema getSchemaFromBody(final RequestBody requestBody) {
        final String contentType = new ArrayList<>(requestBody.getContent().keySet()).get(0);
        final MediaType mediaType = requestBody.getContent().get(contentType);
        return mediaType.getSchema();
    }

    protected Schema getSchemaFromResponse(final ApiResponse response) {
        if (response.getContent() == null || response.getContent().isEmpty()) {
            return null;
        }
        Schema schema = null;
        for (final String contentType : response.getContent().keySet()) {
            schema = response.getContent().get(contentType).getSchema();
            if (schema != null) {
                schema.addExtension("x-content-type", contentType);
            }
            break;
        }
        return schema;
    }

    protected Schema getSchemaFromParameter(final Parameter parameter) {
        if (parameter.getContent() == null || parameter.getContent().isEmpty()) {
            return null;
        }
        Schema schema = null;
        for (final String contentType : parameter.getContent().keySet()) {
            schema = parameter.getContent().get(contentType).getSchema();
            if (schema != null) {
                schema.addExtension("x-content-type", contentType);
            }
            break;
        }
        return schema;
    }

    protected Parameter getParameterFromRef(final String ref, final OpenAPI openAPI) {
        final String parameterName = ref.substring(ref.lastIndexOf('/') + 1);
        final Map<String, Parameter> parameterMap = openAPI.getComponents().getParameters();
        return parameterMap.get(parameterName);
    }

    protected void setTemplateEngine() {
        final String templateEngineKey = this.additionalProperties.get(CodegenConstants.TEMPLATE_ENGINE) != null
                ? this.additionalProperties.get(CodegenConstants.TEMPLATE_ENGINE).toString()
                : null;

        if (templateEngineKey == null) {
            this.templateEngine = new HandlebarTemplateEngine(this);
        } else {
            if (CodegenConstants.HANDLEBARS_TEMPLATE_ENGINE.equalsIgnoreCase(templateEngineKey)) {
                this.templateEngine = new HandlebarTemplateEngine(this);
            } else {
                this.templateEngine = new MustacheTemplateEngine(this);
            }
        }
    }

    protected String getTemplateDir() {
        return new StringBuilder().append(this.templateEngine.getName()).append(File.separatorChar)
                .append(this.getDefaultTemplateDir()).toString();
    }

    private void setOauth2Info(final CodegenSecurity codegenSecurity, final OAuthFlow flow) {
        codegenSecurity.authorizationUrl = flow.getAuthorizationUrl();
        codegenSecurity.tokenUrl = flow.getTokenUrl();
        codegenSecurity.scopes = flow.getScopes();
    }

    private List<Schema> getInterfaces(final ComposedSchema composed) {
        if (composed.getAllOf() != null && composed.getAllOf().size() > 1) {
            return composed.getAllOf().subList(1, composed.getAllOf().size());
        }
        if (composed.getAnyOf() != null && !composed.getAnyOf().isEmpty()) {
            return composed.getAnyOf();
        }
        if (composed.getOneOf() != null && !composed.getOneOf().isEmpty()) {
            return composed.getOneOf();
        }
        return null;
    }

    protected void addConsumesInfo(final Operation operation, final CodegenOperation codegenOperation,
            final OpenAPI openAPI) {
        RequestBody body = operation.getRequestBody();
        if (body == null) {
            return;
        }
        if (StringUtils.isNotBlank(body.get$ref())) {
            final String bodyName = OpenAPIUtil.getSimpleRef(body.get$ref());
            body = openAPI.getComponents().getRequestBodies().get(bodyName);
        }

        if (body.getContent() == null || body.getContent().isEmpty()) {
            return;
        }

        final Set<String> consumes = body.getContent().keySet();
        final List<Map<String, String>> mediaTypeList = new ArrayList<>();
        int count = 0;
        for (final String key : consumes) {
            final Map<String, String> mediaType = new HashMap<>();
            this.decideMediaType(key, mediaType);
            count += 1;
            if (count < consumes.size()) {
                mediaType.put("hasMore", "true");
            } else {
                mediaType.put("hasMore", null);
            }
            mediaTypeList.add(mediaType);
        }
        codegenOperation.consumes = mediaTypeList;
        codegenOperation.getVendorExtensions().put(CodegenConstants.HAS_CONSUMES_EXT_NAME, Boolean.TRUE);
    }

    private void decideMediaType(final String key, final Map<String, String> mediaType) {
        if ("*/*".equals(key)) {
            mediaType.put("mediaType", key);
        } else {
            mediaType.put("mediaType", this.escapeText(this.escapeQuotationMark(key)));
        }
    }

    protected void configureDataForTestTemplate(final CodegenOperation codegenOperation) {
        final String httpMethod = codegenOperation.httpMethod;
        String path = codegenOperation.path;
        if ("GET".equalsIgnoreCase(httpMethod)) {
            codegenOperation.getVendorExtensions().put(CodegenConstants.IS_GET_METHOD_EXT_NAME, Boolean.TRUE);
        }
        if ("POST".equalsIgnoreCase(httpMethod)) {
            codegenOperation.getVendorExtensions().put(CodegenConstants.IS_POST_METHOD_EXT_NAME, Boolean.TRUE);
        }
        if ("PUT".equalsIgnoreCase(httpMethod)) {
            codegenOperation.getVendorExtensions().put(CodegenConstants.IS_PUT_METHOD_EXT_NAME, Boolean.TRUE);
        }
        if ("DELETE".equalsIgnoreCase(httpMethod)) {
            codegenOperation.getVendorExtensions().put(CodegenConstants.IS_DELETE_METHOD_EXT_NAME, Boolean.TRUE);
        }
        if ("HEAD".equalsIgnoreCase(httpMethod)) {
            codegenOperation.getVendorExtensions().put(CodegenConstants.IS_HEAD_METHOD_EXT_NAME, Boolean.TRUE);
        }
        if ("TRACE".equalsIgnoreCase(httpMethod)) {
            codegenOperation.getVendorExtensions().put(CodegenConstants.IS_TRACE_METHOD_EXT_NAME, Boolean.TRUE);
        }
        if ("PATCH".equalsIgnoreCase(httpMethod)) {
            codegenOperation.getVendorExtensions().put(CodegenConstants.IS_PATCH_METHOD_EXT_NAME, Boolean.TRUE);
        }
        if ("OPTIONS".equalsIgnoreCase(httpMethod)) {
            codegenOperation.getVendorExtensions().put(CodegenConstants.IS_OPTIONS_METHOD_EXT_NAME, Boolean.TRUE);
        }

        if (path.contains("{")) {
            while (path.contains("{")) {
                final String pathParam = path.substring(path.indexOf("{"), path.indexOf("}") + 1);
                final String paramName = pathParam.replace("{", StringUtils.EMPTY).replace("}", StringUtils.EMPTY);

                final Optional<CodegenParameter> optionalCodegenParameter = codegenOperation.pathParams.stream()
                        .filter(codegenParam -> codegenParam.baseName.equals(paramName)).findFirst();

                if (!optionalCodegenParameter.isPresent()) {
                    return;
                }

                final CodegenParameter codegenParameter = optionalCodegenParameter.get();

                if (codegenParameter.testExample == null) {
                    return;
                }

                path = path.replace(pathParam, codegenParameter.testExample);
            }
        }
        codegenOperation.testPath = path;
    }

    protected Set<String> getConsumesInfo(final Operation operation) {
        if (operation.getRequestBody() == null || operation.getRequestBody().getContent() == null
                || operation.getRequestBody().getContent().isEmpty()) {
            return null;
        }
        return operation.getRequestBody().getContent().keySet();
    }

    protected void addProducesInfo(final ApiResponse response, final CodegenOperation codegenOperation) {
        if (response == null || response.getContent() == null || response.getContent().isEmpty()) {
            return;
        }
        final Set<String> produces = response.getContent().keySet();
        if (codegenOperation.produces == null) {
            codegenOperation.produces = new ArrayList<>();
        }
        for (final String key : produces) {
            final Map<String, String> mediaType = new HashMap<>();
            // escape quotation to avoid code injection
            this.decideMediaType(key, mediaType);
            mediaType.put("hasMore", "true");
            codegenOperation.produces.add(mediaType);
            codegenOperation.getVendorExtensions().put(CodegenConstants.HAS_PRODUCES_EXT_NAME, Boolean.TRUE);
        }
    }

    protected Set<String> getProducesInfo(final Operation operation) {
        if (operation.getResponses() == null || operation.getResponses().isEmpty()) {
            return null;
        }
        return operation.getResponses().keySet();
    }

    protected Schema detectParent(final ComposedSchema composedSchema, final Map<String, Schema> allSchemas) {
        if (composedSchema.getAllOf() != null && !composedSchema.getAllOf().isEmpty()) {
            final Schema schema = composedSchema.getAllOf().get(0);
            String ref = schema.get$ref();
            if (StringUtils.isBlank(ref)) {
                return null;
            }
            ref = OpenAPIUtil.getSimpleRef(ref);
            return allSchemas.get(ref);
        }
        return null;
    }

    protected String getParentName(final ComposedSchema composedSchema) {
        if (composedSchema.getAllOf() != null && !composedSchema.getAllOf().isEmpty()) {
            final Schema schema = composedSchema.getAllOf().get(0);
            final String ref = schema.get$ref();
            if (StringUtils.isBlank(ref)) {
                return null;
            }
            return OpenAPIUtil.getSimpleRef(ref);
        }
        return null;
    }

    // See: https://swagger.io/docs/specification/serialization/#query
    protected String getCollectionFormat(final Parameter parameter) {
        // "explode: true" is the default and always results in "multi", no matter the style.
        if (parameter.getExplode() == null || parameter.getExplode()) {
            return "multi";
        }

        // Form is the default, if no style is specified.
        if (parameter.getStyle() == null || Parameter.StyleEnum.FORM.equals(parameter.getStyle())) {
            return "csv";
        }
        if (Parameter.StyleEnum.PIPEDELIMITED.equals(parameter.getStyle())) {
            return "pipe";
        }
        if (Parameter.StyleEnum.SPACEDELIMITED.equals(parameter.getStyle())) {
            return "space";
        }
        return null;
    }

    public boolean isObjectSchema(final Schema schema) {
        if (schema == null) {
            return false;
        }
        if (schema instanceof ObjectSchema || schema instanceof ComposedSchema) {
            return true;
        }
        if (SchemaTypeUtil.OBJECT_TYPE.equalsIgnoreCase(schema.getType()) && !(schema instanceof MapSchema)) {
            return true;
        }
        if (schema.getType() == null && schema.getProperties() != null && !schema.getProperties().isEmpty()) {
            return true;
        }
        if (StringUtils.isNotBlank(schema.get$ref())) {
            final Schema refSchema = OpenAPIUtil.getSchemaFromRefSchema(schema, this.openAPI);
            if (refSchema != null) {
                return this.isObjectSchema(refSchema);
            }
        }

        return false;
    }

    private boolean containsFormContentType(final RequestBody body) {
        if (body == null) {
            return false;
        }
        final Content content = body.getContent();
        if (content == null || content.isEmpty()) {
            return false;
        }
        return content.get("application/x-www-form-urlencoded") != null || content.get("multipart/form-data") != null;
    }

    protected static boolean hasSchemaProperties(final Schema schema) {
        final Object additionalProperties = schema.getAdditionalProperties();
        return additionalProperties instanceof Schema;
    }

    protected static boolean hasTrueAdditionalProperties(final Schema schema) {
        final Object additionalProperties = schema.getAdditionalProperties();
        return additionalProperties != null && Boolean.TRUE.equals(additionalProperties);
    }

    protected void configuresParameterForMediaType(final CodegenOperation codegenOperation,
            final List<CodegenContent> codegenContents) {
        if (codegenContents.isEmpty()) {
            final CodegenContent content = new CodegenContent();
            content.getParameters().addAll(codegenOperation.allParams);
            codegenContents.add(content);

            codegenOperation.getContents().add(content);
            return;
        }
        this.addCodegenContentParameters(codegenOperation, codegenContents);
        for (final CodegenContent content : codegenContents) {
            if (this.ensureUniqueParams) {
                this.ensureUniqueParameters(content.getParameters());
            }

            Collections.sort(content.getParameters(), (final CodegenParameter one, final CodegenParameter another) -> {
                // if (one.required == another.required) {
                // return 0;
                // }
                // if (one.required) {
                // return -1;
                // }
                // return 1;
                final int oneIndex = ELEMENTS.indexOf(one.getParamName());
                final int anotherIndex = ELEMENTS.indexOf(another.getParamName());
                if (oneIndex == -1 && anotherIndex == -1) {
                    LOGGER.info("{} {} {}", one.getParamName(), one.getIsPathParam(), one.getIsQueryParam());
                    LOGGER.info("{} {} {}", another.getParamName(), another.getIsPathParam(),
                            another.getIsQueryParam());
                    LOGGER.info(another.toString());
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
            OperationParameters.addHasMore(content.getParameters());
        }
        codegenOperation.getContents().addAll(codegenContents);
    }

    private static final List<String> ELEMENTS = Arrays.asList("pdPTenantKey", "pdPTenantSecret", "tenantId");

    protected void addParameters(final CodegenContent codegenContent, final List<CodegenParameter> codegenParameters) {
        if (codegenParameters == null || codegenParameters.isEmpty()) {
            return;
        }
        for (final CodegenParameter codegenParameter : codegenParameters) {
            codegenContent.getParameters().add(codegenParameter.copy());
        }
    }

    protected void addCodegenContentParameters(final CodegenOperation codegenOperation,
            final List<CodegenContent> codegenContents) {
        for (final CodegenContent content : codegenContents) {
            this.addParameters(content, codegenOperation.headerParams);
            this.addParameters(content, codegenOperation.pathParams);
            this.addParameters(content, codegenOperation.queryParams);
            this.addParameters(content, codegenOperation.cookieParams);
            if (content.getIsForm()) {
                this.addParameters(content, codegenOperation.formParams);
            } else {
                this.addParameters(content, codegenOperation.bodyParams);
            }
        }
    }

    protected void ensureUniqueParameters(final List<CodegenParameter> codegenParameters) {
        if (codegenParameters == null || codegenParameters.isEmpty()) {
            return;
        }
        for (final CodegenParameter codegenParameter : codegenParameters) {
            final long count = codegenParameters.stream()
                    .filter(codegenParam -> codegenParam.paramName.equals(codegenParameter.paramName)).count();
            if (count > 1l) {
                codegenParameter.paramName = generateNextName(codegenParameter.paramName);
            }
        }
    }

    protected void setParameterNullable(final CodegenParameter parameter, final CodegenProperty property) {
        parameter.nullable = property.nullable;
    }

    protected void setParameterJson(final CodegenParameter codegenParameter, final Schema parameterSchema) {
        final String contentType = parameterSchema.getExtensions() == null ? null
                : (String) parameterSchema.getExtensions().get("x-content-type");
        if (contentType != null && contentType.startsWith("application/") && contentType.endsWith("json")) {
            // application/json, application/problem+json, application/ld+json, some more?
            codegenParameter.isJson = true;
        }
    }

    protected boolean isFileTypeSchema(final Schema schema) {
        final Schema fileTypeSchema;
        if (StringUtils.isNotBlank(schema.get$ref())) {
            fileTypeSchema = OpenAPIUtil.getSchemaFromRefSchema(schema, this.openAPI);
        } else {
            fileTypeSchema = schema;
        }
        if (fileTypeSchema.getProperties() != null) {
            final Collection<Schema> propertySchemas = fileTypeSchema.getProperties().values();
            return propertySchemas.stream()
                    .anyMatch(propertySchema -> "string".equalsIgnoreCase(propertySchema.getType())
                            && "binary".equalsIgnoreCase(propertySchema.getFormat()));
        }
        return false;
    }

    @Override
    public boolean needsUnflattenedSpec() {
        return false;
    }

    @Override
    public void setUnflattenedOpenAPI(final OpenAPI unflattenedOpenAPI) {
        this.unflattenedOpenAPI = unflattenedOpenAPI;
    }

    @Override
    public boolean getIgnoreImportMapping() {
        return this.ignoreImportMapping;
    }

    @Override
    public void setIgnoreImportMapping(final boolean ignoreImportMapping) {
        this.ignoreImportMapping = ignoreImportMapping;
    }

    @Override
    public boolean defaultIgnoreImportMappingOption() {
        return false;
    }

    @Override
    public ISchemaHandler getSchemaHandler() {
        return new SchemaHandler(this);
    }

    public OpenAPI getOpenAPI() {
        return this.openAPI;
    }
}
