
package org.openapitools.codegen.languages;

import org.openapitools.codegen.*;
import com.pdc.SemVerUtils;
import io.swagger.v3.oas.models.OpenAPI;
import java.io.File;

public class TypescriptGenerator extends AbstractTypeScriptClientCodegen {
	protected String sourceFolder = "src";
	protected String templateVersion = "0.0.1";
	protected String packageVersion = "";

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
		super.preprocessOpenAPI(openAPI);
		this.packageVersion = SemVerUtils.combineSemVer(
			this.templateVersion,
			openAPI.getInfo().getVersion()
		).toString();
		additionalProperties.put("packageVersion", this.packageVersion);
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
