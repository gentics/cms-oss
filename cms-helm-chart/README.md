# Gentics CMS Helm Chart

## Introduction

This chart bootstraps a Gentics CMS with a persistent volume & ingress.

## Links

* [Installation Guide](https://www.gentics.com/cmp8/operations/kubernetes/installation)
* [Documentation](https://www.gentics.com/Content.Node/cmp8/guides/)
* [Changelog](https://www.gentics.com/cmp/changelog/)

## Update dependency charts

Set gentics-common `version` in `Chart.yml`
```bash
helm dependency update
```

## Building / Release

Helm chart is automatically built with a mesh-enterprise release