package io.swagger.codegen.v3.generators.typescript;

import com.pdc.SemVerUtils;
import io.swagger.codegen.v3.SupportingFile;
import io.swagger.v3.oas.models.OpenAPI;

import java.io.File;

public class TypescriptGenerator extends AbstractTypeScriptClientCodegen {
	protected String sourceFolder = "src";
	protected String templateVersion = "0.0.1";
	protected String packageVersion = "";

	/**
	 * Configures a friendly name for the generator.  This will be used by the generator
	 * to select the library with the -l flag.
	 *
	 * @return the friendly name for the generator
	 */
	public String getName() {
		return "typescript";
	}

	/**
	 * Returns human-friendly help for the generator.  Provide the consumer with help
	 * tips, parameters here
	 *
	 * @return A string value for the help message
	 */
	public String getHelp() {
		return "Generates a typescript client library.";
	}

	public TypescriptGenerator() {
		super();
		outputFolder = "build/typescript";
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
		super.preprocessOpenAPI(openAPI);
		this.packageVersion = SemVerUtils.combineSemVer(
			this.templateVersion,
			openAPI.getInfo().getVersion()
		).toString();
		additionalProperties.put("packageVersion", this.packageVersion);
	}

	@Override
	public String modelFileFolder() {
		return outputFolder + "/" + sourceFolder + "/types/" + modelPackage().replace('.', File.separatorChar);
	}

	@Override
	public String apiFileFolder() {
		return outputFolder + "/" + sourceFolder + "/" + apiPackage().replace('.', File.separatorChar);
	}

	@Override
	public String getArgumentsLocation() {
		return null;
	}

	@Override
	public String getDefaultTemplateDir() {
		return "typescript";
	}

	@Override
	public String getTemplateDir() {
		return "typescript";
	}
}
