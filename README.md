# PDC SDK Generators

This repository houses the infrastructure for generating PDC SDKs.

We're using a project called [swagger-codegen](https://github.com/swagger-api/swagger-codegen) to power our SDK, and this repository provides tooling and templates to make SDK generation easier in specifically supported languages.

## Prequisites

- JRE (18+)

## Generating SDKs

To generate the SDKs supported by this project, you will need a [Swagger](https://swagger.io/) specification located at the project root and named `openapi.json`. The current Swagger specification for the PDC can be found in the [service repository](https://github.com/PhilanthropyDataCommons/service/blob/main/src/openapi.json).

### Generating official SDKs:

1. Clone the repository:

```shell
git clone git@github.com:PhilanthropyDataCommons/sdk.git
```

2. Change directory:

```shell
cd sdk
```

3. Copy your Swagger specification:

```shell
cp YOUR_SWAGGER_SPEC_LOCATION ./openapi.json
```

4. Generate the code:

```shell
> ./gradlew generateSwaggerCode
```

The generated code will now exist in the `build` directory.

Each language target will have its own directory inside of `build/`, and you can follow the README instructions in that directory with additional steps related to that language's SDK.

- Typescript: `build/typescript/README.md`

### Generating other SDKs:

If you are interested in another language please review the [many languages](https://github.com/swagger-api/swagger-codegen/tree/master/modules/swagger-codegen/src/main/resources) that the Swagger Codegen project supports directly.  Please see the [Swagger Codegen documentation](https://github.com/swagger-api/swagger-codegen?tab=readme-ov-file#generators) for information on how to generate libraries for other languages.
