#!/bin/bash

set -o nounset
set -o errexit

DEST=/backup

# Sources
CMS_DATA=/cms

# Name of the final backup folder
NAME=current

# Name of the backup staging folder
STAGE_NAME=current_tmp

function runBackup() {
  echo "Invoking CMS database dump"
  mysqldump -h {{ .Values.database.host }} \
    -u {{ .Values.database.username }} \
    -P {{ .Values.database.port }} \
    --max_allowed_packet=512M \
    --skip-triggers \
    --skip-routines \
    --result-file $DEST/$STAGE_NAME/gentics_cms_dump.sql \
    {{ .Values.database.name }}

  echo "Syncing CMS files"
  mkdir -p $DEST/$STAGE_NAME/files
    rsync -rav --exclude "*/log" $CMS_DATA $DEST/$STAGE_NAME/files
}

function prepare() {
  echo "Running backup with user $(id)"
  if [ -e $DEST/${NAME}_old ] ; then
    echo "Removing old backup folder"
    rm -rf $DEST/${NAME}_old
  fi
  if [ -e $DEST/$STAGE_NAME ] ; then
    echo "Removing old staging folder"
    rm -rf $DEST/$STAGE_NAME
  fi
  mkdir -p $DEST/$STAGE_NAME
  mkdir -p $DEST/$NAME

}

function finalize() {
  echo "New Backup size: $(du -sh $DEST/$STAGE_NAME)"
  echo "Old Backup size: $(du -sh $DEST/$NAME)"

  echo "Moving current backup away"
  mv $DEST/$NAME $DEST/${NAME}_old

  echo "Moving new backup in-place"
  mv $DEST/$STAGE_NAME $DEST/$NAME

  echo "Removing old backup"
  rm -rf $DEST/${NAME}_old

  echo "Backup process completed."
}

prepare
runBackup
finalize
