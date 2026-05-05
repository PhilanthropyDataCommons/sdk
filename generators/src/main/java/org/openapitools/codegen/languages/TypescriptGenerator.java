
package org.openapitools.codegen.languages;

import org.openapitools.codegen.*;
import org.openapitools.codegen.model.ModelMap;
import org.openapitools.codegen.model.ModelsMap;
import com.pdc.SemVerUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TypescriptGenerator extends AbstractTypeScriptClientCodegen {
	protected String sourceFolder = "src";
	protected String templateVersion = "0.1.0";
	protected String packageVersion = "";

	// Schema-name → inline-oneOf metadata (branches + discriminator). Populated during
	// preprocessOpenAPI; consumed in postProcessAllModels to stamp vendor extensions onto the
	// CodegenModel for the base AND any model that allOf-extends it. Schema-level vendor
	// extensions don't reliably flow through to CodegenModel.vendorExtensions in 7.8, so we
	// route via this field instead.
	private final Map<String, Map<String, Object>> inlineOneOfBySchema = new HashMap<>();

	public CodegenType getTag() {
			return CodegenType.CLIENT;
	}

	public String getName() {
		return "typescript";
	}

	public String getHelp() {
		return "Generates a typescript client library.";
	}

	public TypescriptGenerator() {
		super();
		outputFolder = "build" + File.separator + "typescript";
		embeddedTemplateDir = templateDir = "typescript";
		modelTemplateFiles.put(
			"types/type.mustache",
			".ts"
		);

		supportingFiles.add(new SupportingFile("index.ts", "", "src/index.ts"));
		supportingFiles.add(new SupportingFile("README.md", "", "README.md"));
		supportingFiles.add(new SupportingFile("tsconfig.json", "", "tsconfig.json"));
		supportingFiles.add(new SupportingFile("types/Writable.ts", "", "src/types/Writable.ts"));
		supportingFiles.add(new SupportingFile("types/index.mustache", "", "src/types/index.ts"));

		// We want the package / lock files to be detectable and maintained by dependabot.
		// We also want the files to be interpreted as templates.
		// Both of these requirements are driven by file name, which is why we are using symlinks.
		supportingFiles.add(new SupportingFile("package.mustache", "", "package.json"));
		supportingFiles.add(new SupportingFile("package-lock.mustache", "", "package-lock.json"));

	}

	@Override
	public void preprocessOpenAPI(OpenAPI openAPI) {
		// io.swagger.parser silently drops inline `oneOf` from hybrid object-with-properties
		// schemas (the PermissionGrantBase pattern) when parsing OAS 3.1 — by the time
		// preprocessOpenAPI runs, openAPI.getComponents() doesn't carry it. We therefore
		// re-parse the raw spec JSON ourselves to capture the inline-oneOf info, then drive
		// the const→enum rewrite via the (possibly lossy) parsed Schema tree for everything
		// else.
		captureInlineOneOfFromRawSpec();
		if (openAPI.getComponents() != null && openAPI.getComponents().getSchemas() != null) {
			for (Schema<?> schema : openAPI.getComponents().getSchemas().values()) {
				normalizeSchema(schema);
			}
		}
		super.preprocessOpenAPI(openAPI);
		this.packageVersion = SemVerUtils.combineSemVer(
			this.templateVersion,
			openAPI.getInfo().getVersion()
		).toString();
		additionalProperties.put("packageVersion", this.packageVersion);
	}

	// Parse the raw spec JSON via Jackson, walk components.schemas, and record any hybrid
	// object+inline-oneOf schemas in inlineOneOfBySchema. Each entry captures the discriminator
	// property (a const/single-value-enum field appearing on every branch) and the branch-
	// specific id fields (with TypeScript types) needed to render the discriminated union.
	private void captureInlineOneOfFromRawSpec() {
		String specPath = inputSpec != null ? inputSpec : "openapi.json";
		File specFile = new File(specPath);
		if (!specFile.exists()) {
			throw new RuntimeException("Inline-oneOf capture: spec file not found at " + specPath);
		}
		try {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(specFile);
			JsonNode schemas = root.path("components").path("schemas");
			if (!schemas.isObject()) return;
			schemas.fields().forEachRemaining(entry -> {
				String name = entry.getKey();
				JsonNode schema = entry.getValue();
				if (!schema.has("properties") || !schema.has("oneOf")) return;
				if (!schema.path("properties").isObject() || !schema.path("oneOf").isArray()) return;

				List<Map<String, Object>> branches = new ArrayList<>();
				String discriminator = null;

				for (JsonNode branch : schema.path("oneOf")) {
					JsonNode branchProps = branch.path("properties");
					if (!branchProps.isObject()) continue;
					Map<String, Object> branchInfo = new LinkedHashMap<>();
					List<Map<String, String>> ownFields = new ArrayList<>();
					String branchDiscriminator = null;
					String branchLiteral = null;

					java.util.Iterator<Map.Entry<String, JsonNode>> propIt = branchProps.fields();
					while (propIt.hasNext()) {
						Map.Entry<String, JsonNode> p = propIt.next();
						String propName = p.getKey();
						JsonNode propSchema = p.getValue();

						String constValue = constOrSingleEnumValue(propSchema);
						if (constValue != null && branchDiscriminator == null) {
							branchDiscriminator = propName;
							branchLiteral = constValue;
						} else {
							Map<String, String> field = new LinkedHashMap<>();
							field.put("name", propName);
							field.put("tsType", jsonNodeToTsType(propSchema));
							ownFields.add(field);
						}
					}

					if (branchDiscriminator == null) {
						throw new RuntimeException("Schema " + name
							+ " has an inline oneOf branch without a const/single-value-enum discriminator property");
					}
					if (discriminator == null) {
						discriminator = branchDiscriminator;
					} else if (!discriminator.equals(branchDiscriminator)) {
						throw new RuntimeException("Schema " + name
							+ " has inline oneOf branches with mismatched discriminator properties: "
							+ discriminator + " vs " + branchDiscriminator);
					}

					branchInfo.put("discriminator", branchDiscriminator);
					branchInfo.put("literalValue", branchLiteral);
					branchInfo.put("ownFields", ownFields);
					branches.add(branchInfo);
				}

				if (discriminator != null) {
					Map<String, Object> meta = new LinkedHashMap<>();
					meta.put("branches", branches);
					inlineOneOfBySchema.put(name, meta);
				}
			});
		} catch (Exception e) {
			throw new RuntimeException("Failed to capture inline-oneOf from raw spec at " + specPath, e);
		}
	}

	private String constOrSingleEnumValue(JsonNode schema) {
		if (schema.has("const") && schema.get("const").isTextual()) {
			return schema.get("const").asText();
		}
		JsonNode enumNode = schema.path("enum");
		if (enumNode.isArray() && enumNode.size() == 1 && enumNode.get(0).isTextual()) {
			return enumNode.get(0).asText();
		}
		return null;
	}

	private String jsonNodeToTsType(JsonNode schema) {
		if (schema.has("$ref")) {
			String ref = schema.get("$ref").asText();
			return ref.substring(ref.lastIndexOf('/') + 1);
		}
		String type = schema.path("type").asText(null);
		if (type == null) return "unknown";
		switch (type) {
			case "string": return "string";
			case "integer":
			case "number": return "number";
			case "boolean": return "boolean";
			case "array": return "Array<" + jsonNodeToTsType(schema.path("items")) + ">";
			default: return "unknown";
		}
	}

	// Walk the parsed Schema tree and rewrite `const: "X"` as `enum: ["X"]`. OAS 3.1 `const`
	// is not honored by AbstractTypeScriptClientCodegen 7.8 (long-standing upstream bug); a
	// single-value enum surfaces as CodegenProperty.isEnum=true with _enum=["X"], which we
	// then render as a TypeScript literal in the template via x-is-literal/x-const-value.
	@SuppressWarnings({"rawtypes", "unchecked"})
	private void normalizeSchema(Schema schema) {
		if (schema == null) return;

		Object constValue = schema.getConst();
		if (constValue != null && (schema.getEnum() == null || schema.getEnum().isEmpty())) {
			List<Object> singleValueEnum = new ArrayList<>();
			singleValueEnum.add(constValue);
			schema.setEnum(singleValueEnum);
		}

		if (schema.getProperties() != null) {
			for (Object prop : schema.getProperties().values()) {
				if (prop instanceof Schema) normalizeSchema((Schema) prop);
			}
		}
		if (schema.getItems() != null) normalizeSchema(schema.getItems());
		if (schema.getAllOf() != null) {
			for (Object s : schema.getAllOf()) {
				if (s instanceof Schema) normalizeSchema((Schema) s);
			}
		}
		if (schema.getOneOf() != null) {
			for (Object s : schema.getOneOf()) {
				if (s instanceof Schema) normalizeSchema((Schema) s);
			}
		}
		if (schema.getAnyOf() != null) {
			for (Object s : schema.getAnyOf()) {
				if (s instanceof Schema) normalizeSchema((Schema) s);
			}
		}
	}

	@Override
	public Map<String, ModelsMap> postProcessAllModels(Map<String, ModelsMap> objs) {
		objs = super.postProcessAllModels(objs);
		for (ModelsMap modelsMap : objs.values()) {
			for (ModelMap modelMap : modelsMap.getModels()) {
				CodegenModel cm = modelMap.getModel();
				markDiscriminatedUnion(cm);
				markLiteralAndEnumArrayVars(cm);
				markInlineOneOf(cm);
			}
		}
		return objs;
	}

	// Top-level oneOf models (PermissionGrant): cm.oneOf is a Set<String> of branch names.
	// AbstractTypeScriptClientCodegen flattens vars across all branches, which is wrong for
	// a union — we set a flag so the template emits `export type X = B1 | B2 | ...` and
	// ignores the bogus flattened vars.
	private void markDiscriminatedUnion(CodegenModel cm) {
		if (cm.oneOf != null && !cm.oneOf.isEmpty()) {
			cm.vendorExtensions.put("x-is-discriminated-union", true);
			List<String> oneOfTypes = new ArrayList<>(cm.oneOf);
			java.util.Collections.sort(oneOfTypes);
			cm.vendorExtensions.put("x-oneof-types", oneOfTypes);
		}
	}

	// For each property: detect single-value enums (rendered as `'literal'`) and arrays whose
	// items are string enums (rendered as `Array<'a' | 'b'>`). The template uses these vendor
	// extensions to bypass the per-class `ScopeEnum`/`PropertyEnum` namespace pattern, which
	// would otherwise collide across allOf-merged variants.
	private void markLiteralAndEnumArrayVars(CodegenModel cm) {
		if (cm.vars == null) return;
		for (CodegenProperty p : cm.vars) {
			if (p.isEnum && p._enum != null && p._enum.size() == 1) {
				p.vendorExtensions.put("x-is-literal", true);
				p.vendorExtensions.put("x-const-value", p._enum.get(0));
			}
			if (p.isArray && p.items != null && p.items.isEnum
					&& p.items._enum != null && !p.items._enum.isEmpty()) {
				p.vendorExtensions.put("x-is-string-enum-array", true);
				p.vendorExtensions.put("x-enum-values", new ArrayList<>(p.items._enum));
			}
		}
	}

	// Stamp `x-has-inline-oneof` and `x-inline-oneof-branches` onto every model that either IS
	// the hybrid base or allOf-extends it. Each branch carries its own discriminator + literal,
	// so the template can render its own type intersection without referencing the base.
	private void markInlineOneOf(CodegenModel cm) {
		if (inlineOneOfBySchema.isEmpty()) return;
		String matchedSchema = null;
		if (inlineOneOfBySchema.containsKey(cm.classname)) {
			matchedSchema = cm.classname;
		} else if (cm.allOf != null) {
			for (String parent : cm.allOf) {
				if (inlineOneOfBySchema.containsKey(parent)) {
					matchedSchema = parent;
					break;
				}
			}
		}
		if (matchedSchema == null) return;
		Map<String, Object> meta = inlineOneOfBySchema.get(matchedSchema);
		cm.vendorExtensions.put("x-has-inline-oneof", true);
		cm.vendorExtensions.put("x-inline-oneof-branches", meta.get("branches"));
	}

	@Override
	public String modelFileFolder() {
		return outputFolder + File.separator + sourceFolder + File.separator + "types"  + File.separator + modelPackage().replace('.', File.separatorChar);
	}

	@Override
	public String apiFileFolder() {
		return outputFolder + File.separator + sourceFolder + File.separator + apiPackage().replace('.', File.separatorChar);
	}
}
