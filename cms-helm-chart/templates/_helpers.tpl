{{/* vim: set filetype=mustache: */}}
{{/*
Expand the name of the chart.
*/}}
{{- define "cms.name" -}}
{{- $global := .Values.global | default dict -}}
{{- default .Chart.Name $global.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "cms.fullname" -}}
{{- $global := .Values.global | default dict -}}
{{- if $global.fullnameOverride -}}
{{- $global.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := default .Chart.Name $global.nameOverride -}}
{{- if contains $name .Release.Name -}}
{{- .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}
{{- end -}}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "cms.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Common labels
*/}}
{{- define "cms.labels" -}}
helm.sh/chart: {{ include "cms.chart" . }}
{{ include "cms.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end -}}

{{/*
Selector labels
*/}}
{{- define "cms.selectorLabels" -}}
app.kubernetes.io/name: {{ include "cms.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{/*
Create the name of the service account to use
*/}}
{{- define "cms.serviceAccountName" -}}
{{- if .Values.serviceAccount.create -}}
    {{ default (include "cms.fullname" .) .Values.serviceAccount.name }}
{{- else -}}
    {{ default "default" .Values.serviceAccount.name }}
{{- end -}}
{{- end -}}

{{/*
Database host for CMS.
Uses explicit database.host if set; falls back to the bundled MariaDB service name
when global.mariadb.enabled is true; otherwise raises an error.
*/}}
{{- define "cms.databaseHost" -}}
{{- include "gentics-common.databaseHost" (dict "root" . "db" .Values.database) -}}
{{- end -}}

{{/*
Secret name and key for the CMS database password.
Priority: explicit database.passwordSecret → umbrella MariaDB secret → sub-chart local secret.
Returns a full secretKeyRef block.
*/}}
{{- define "cms.databasePasswordRef" -}}
{{- include "gentics-common.databasePasswordRef" (dict "root" . "db" .Values.database "component" "cms" "localSecretName" (include "cms.fullname" .) "localSecretKey" "database-password") -}}
{{- end -}}

{{/*
Image pull secrets for the main CMS image (secret NAME, string format).
Three-state: set -> local wins (global ignored); set to "" -> no pull secret;
absent -> global.imagePullSecrets, else gentics-common.defaultImagePullSecret.
*/}}
{{- define "cms.imagePullSecrets" -}}
{{- include "gentics-common.imagePullSecrets" (dict "root" . "localName" .Values.image.pullSecret) -}}
{{- end }}

{{/*
Image pull secrets for the spellchecker pod, which pulls its own image
(gentics/languagetool) into a separate pod spec — imagePullSecrets is pod-scoped,
so it cannot inherit the CMS pod's block.
Three-state: set -> local wins (global ignored); set to "" -> no pull secret;
absent -> the same chain as the main CMS image (cms.imagePullSecrets).
*/}}
{{- define "cms.spellchecker.imagePullSecrets" -}}
{{- if not (kindIs "invalid" .Values.spellchecker.pullSecret) }}
{{- include "gentics-common.imagePullSecrets" (dict "root" . "localName" .Values.spellchecker.pullSecret) -}}
{{- else }}
{{- include "cms.imagePullSecrets" . -}}
{{- end }}
{{- end }}

{{/*
Fully qualified CMS image (registry/repository:tag, global.imageRegistry aware).
*/}}
{{- define "cms.image" -}}
{{- include "gentics-common.image" (dict "root" . "image" .Values.image "tag" (default .Chart.AppVersion .Values.image.tag)) -}}
{{- end -}}
