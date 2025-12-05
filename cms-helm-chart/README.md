# Gentics CMS Helm

## Introduction

This chart bootstraps a Gentics CMS with a persistent volume & ingress.

## Links

* [Documentation](https://www.gentics.com/Content.Node/guides/index.html)
* [Changelog](https://www.gentics.com/Content.Node/changelog/)
* [Environment variables](https://www.gentics.com/Content.Node/guides/admin_docker.html#environment-variables)


## Changelog

### 1.0.12
* Allow to set LicenseKey in new structure (CMP 8.2) and optionally via an existing secret

### 1.0.11
* Allow to perist only data (and not implementation) to data volume (`Values.cms.persistence.onlyData`)
* Allow to configure parts of the keycloak config in the helm chart (`keycloak.json`)

### 1.0.10
* Allow to override the default backup.sh script with `Values.cms.backup.backupScript`

### 1.0.9
* Change default persisted data. Now all CMS relevant files are automatically mounted into the -data volume. You can remove previously used `cmsExtraVolumeMounts`, but maybe need to migrate the persisted data to a different volume subPath.
* /backup is now (readonly) available inside the cms container to access backup data
### 1.0.8
* Mount defined `cmsExtraVolumes` and `cmsExtraVolumeMounts` also in the backup pod to allow this data to be accessible during the backup process.
### 1.0.7
* Change URLs for Nexus migration
### 1.0.6
* Backup image options are now optional, if not set the image configuration of the deployment is used
### 1.0.5
* Fix backup job if a custom securityContext is used
### 1.0.4
* Default `backup.sh` script is now written to the config-map and can be overridden by custom `cmsConfigMapFiles`
### 1.0.3
* Fix CMS Backup Cronjob
### 1.0.2
* A new boolean config item `cms.service.useHttp2` has been added, to enable connections over h2c protocol.
### 1.0.1
* Persist `publish` directory also allow hardlinks within the same volume
### 1.0.0

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
