# Integration Tests

This directory contains all the required files and configurations to start the required services to execute the UI integration tests against.

## Managing Services

To start the services for the OSS Tests, simply run:

```sh
docker compose up -d --wait
```

For Tests which require the Enterprise-Edition, you first need to aquire a valid Licensekey and add it either as ENV Variable (`CI_LICENSEKEY`),
or create a copy of `compose.ee.local.example.yml` -> `compose.ee.local.yml`, and add the Licensekey there.

With the ENV Variable set, you can start it like this:

```sh
docker compose -f compose.ee.yml up -d --wait
```

Otherwise, you may start it with the local compose file:

```sh
docker compose -f compose.ee.local.yml up -d --wait
```

----

To stop the services again, simply run the appropiate down command:

```sh
# OpenSource/Default version
docker compose down
# Enterprise Edition, with ENV Variables
docker compose -f compose.ee.yml down
# Enterprise Edition, with local compose file
docker compose -f compose.ee.local.yml down
```

*Note*: These services do not persist their data on purpose.
If they are shut down (`down`, not `stop`/`restart`), then their content is gone as well,
as only test data should be used in the first place.

## Running Tests

Please see the [Documentation in the UI Module](../cms-ui/README.md#e2eintegration-tests) for how to run/execute the tests.
Otherwise you can also check the [Jenkinsfile](./Jenkinsfile), how they are executed in the CI.

If you want to execute the Tests for EE, you also need to set the ENV Variable `CMS_VARIANT` to `EE` in either your shell before executing the tests,
set it temporarily in the appropiate `cypress.env.json` file, or pass them as argument: `--env.CMS_VARIANT="EE"`.
