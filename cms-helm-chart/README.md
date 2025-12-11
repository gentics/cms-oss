# Gentics CMS Helm

## Introduction

This chart bootstraps a Gentics CMS with a persistent volume & ingress.

## Links

* [Documentation](https://www.gentics.com/Content.Node/cmp8/guides/)
* [Changelog](https://www.gentics.com/cmp/changelog/)
* [Environment variables](https://www.gentics.com/Content.Node/cmp8/guides/admin_configuration.html#environment-variables-system-properties)

## Building / Release

Increase `version` in `Chart.yaml` and create git Tag. Then this tag can be pushed to Artifactory

### package current version 
```bash
helm package ./
```

### Upload the generated/packaged file (check filename)

```bash
curl -u USERNAME:PASSWORD -s -w "%{http_code}\n" https://repo.gentics.com/repository/helm/ --upload-file ./cms-xxx.tgz
```
