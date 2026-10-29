# Licensing

This repository contains code under two different licenses.

| Path | License | License text |
|------|---------|--------------|
| `ee/` | Business Source License 1.1 | [`ee/LICENSE`](ee/LICENSE), [`LICENSES/BUSL-1.1.txt`](LICENSES/BUSL-1.1.txt) |
| everything else | Apache License 2.0 | [`LICENSE`](LICENSE), [`LICENSES/Apache-2.0.txt`](LICENSES/Apache-2.0.txt) |

The `LICENSE` file in `ee/` overrides the `LICENSE` file in the repository root for everything below `ee/`.

The modules below `ee/` are:

* `ee/cms-contentstaging`
* `ee/cms-forms`
* `ee/cms-keycloak`
* `ee/cms-multichannelling`
* `ee/formgen`

The Maven artifacts of these modules declare the license `BUSL-1.1`, all other artifacts declare `Apache-2.0`.

## Business Source License parameters

| Parameter | Value |
|-----------|-------|
| Licensor | `<<LICENSOR>>` |
| Licensed Work | `<<LICENSED_WORK>>` |
| Additional Use Grant | `<<ADDITIONAL_USE_GRANT>>` |
| Change Date | `<<CHANGE_DATE>>` |
| Change License | `<<CHANGE_LICENSE>>` |

## Third-party software

See [`NOTICE`](NOTICE).
