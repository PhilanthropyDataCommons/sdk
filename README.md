# PDC SDK Generators

This repository houses the infrastructure for generating PDC SDKs.

We're using a project called [swagger-codegen](https://github.com/swagger-api/swagger-codegen) to power our SDK, and this repository provides tooling and templates
to make sdk generation easier in particular target languages.  The codegen project supports a [many languages](https://github.com/swagger-api/swagger-codegen/tree/master/modules/swagger-codegen/src/main/resources)
beyond the ones explicitly defined here.

## Prequisites
- JRE (18+)
- Maven (3.9+)

## Scripts
### Generating sdks
To generate the SDKs supported by this project, you will need a locally defined [swagger](https://swagger.io/) specification.  The current swagger specification for the PDC can be found in the [service repository](https://github.com/PhilanthropyDataCommons/service/blob/main/src/openapi.json).

#### Steps:
1. `./gradlew generateSdks -PswaggerSpecificationPath=example/path.json`.
2. Inspect the `build` directory to see the resulting sdk files.

Each language target will have its own directory inside of `build` (e.g. `build/typescript`).
Some languages might have additional build steps defined in their build's README.md file.

### Adding a new language

The `swagger-codegen` project itself provides two ways to customize the code generation process:

- Modifying existing language templates (this is for cases where the fundamental structure of a given generator is in line with what you want,
but maybe there's a bug or something subtle that needs updating).
- Creating new language templates / generators (this gives complete control over things like configurable parameters and the shape of the output).

This repository is designed to support the second approach, and there are a few gradle tasks to bootstrap the creation of those templates:

#### Steps:
1. Add a new language generator module: `./gradlew createLanguageModule -Planguage=foo`
2. Develop the language templates.
3. Compile the language generator module: `./gradlew packageLanguageModule -Planguage=foo`

If you want to package all of the language generator modules at once you can run `./gradlew packageLanguageModules`
