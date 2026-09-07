#!/bin/sh

set -o nounset
set -o errexit

echo "Running pre-upgrade hook"
echo "base: http://{{ include "cms.fullname" . }}.{{ .Release.Namespace }}.svc.cluster.local:{{ .Values.cms.service.port }}"

java -cp cms-server.jar com.gentics.contentnode.tools.UpdateImplementation \
  -u {{ .Values.cms.nodeUser | default "node" }} \
  -p "${NODE_USER_PASSWORD}" \
  --base http://{{ include "cms.fullname" . }}.{{ .Release.Namespace }}.svc.cluster.local:{{ .Values.cms.service.port }}


