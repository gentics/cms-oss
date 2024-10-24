# Integration Tests

This directory contains all the required files and configurations to start the required services to execute the UI integration tests against.

## Managing Services

To start the services for the OSS Tests, simply run:

```sh
docker compose up -d --wait
```

For the EE Tests, you first need to aquire a valid Licensekey and add it either as ENV Variable (`CI_LICENSEKEY`),
or create a copy of `compose.ee.override.example.yml` -> `compose.ee.override.yml`, and add the Licensekey there.

With the ENV Variable, you can start it like this:

```sh
docker compose -f compose.ee.yml up -d --wait
```

Without, you also have to specify the override file:

```sh
docker compose -f compose.ee.yml -f compose.ee.override.yml up -d --wait
```

----

To stop the services again, simply run the appropiate down command:

```sh
docker compose down
docker compose -f compose.ee.yml down
docker compose -f compose.ee.yml -f compose.ee.override.yml down
```

## Running Tests

Please see the [Documentation in the UI Module](../cms-ui/README.md#e2eintegration-tests) for how to run/execute the tests.
Otherwise you can also check the [Jenkinsfile](./Jenkinsfile), how they are executed in the CI.

If you want to execute the Tests for EE, you also need to set the ENV Variable `CMS_VARIANT` to `EE` in either your shell before executing the tests,
or pass them as argument: `--env.CMS_VARIANT="EE"`.
