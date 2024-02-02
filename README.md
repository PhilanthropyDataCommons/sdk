# PDC SDK Generators

This repository houses the infrastructure for generating PDC SDKs.

We're using a project called [swagger-codegen](https://github.com/swagger-api/swagger-codegen) to power our SDK, and this repository provides tooling and templates
to make sdk generation easier in particular target languages. The codegen project supports a [many languages](https://github.com/swagger-api/swagger-codegen/tree/master/modules/swagger-codegen/src/main/resources)
beyond the ones explicitly defined here.

## Prequisites

- JRE (18+)

## Generating sdks
To generate the SDKs supported by this project, you will need:

1. A [swagger](https://swagger.io/) specification located at the project root and named `openapi.json`. The current swagger specification for the PDC can be found in the [service repository](https://github.com/PhilanthropyDataCommons/service/blob/main/src/openapi.json).

2. Populated configuration files, for each language, located in the `configuration` directory.

#### Steps:

From the project root you can run the following commands:

```shell
> cp YOUR_SWAGGER_SPEC_LOCATION ./openapi.json
> ./gradlew generateSwaggerCode
```

The generated code will now exist in the `build` directory.
Each language target will have its own directory inside of `build` (e.g. `build/typescript`).
Some languages might have additional build steps defined in their build's README.md file.
