#!/bin/bash
# Dieses Skript baut den Expo-Web-Export, findet die generierte index-Datei,
# benennt sie in index-formgen.js um und verschiebt sie in das Zielverzeichnis.
#
# Voraussetzung: npx (Node.js) und expo-cli sind installiert
 
set -e  # Beendet das Skript beim ersten Fehler
 
# 1. Expo-App builden
echo "Baue die Expo-App für Web mit: npx expo export --platform web"
npx expo export --platform web || { echo "Build fehlgeschlagen"; exit 1; }
echo "Build abgeschlossen."
 
# 2. Suchen der generierten index-Datei
# Wir nehmen an, dass die Datei im Verzeichnis dist/_expo/static/js/web/ liegt und den Namen index-*.js hat.
echo "Suche nach der generierten index-Datei in dist/_expo/static/js/web/ ..."
INDEX_SRC=$(find dist/_expo/static/js/web/ -maxdepth 1 -type f -name 'index-*.js' | head -n 1)
 
if [[ -z "$INDEX_SRC" ]]; then
  echo "Fehler: Keine Datei gefunden, die dem Muster 'index-*.js' entspricht."
  exit 1
fi
echo "Gefundene Datei: $INDEX_SRC"
 
# 3. Datei umbenennen in index-formgen.js im gleichen Verzeichnis
TARGET_FILE="dist/_expo/static/js/web/index-formgen.js"
echo "Benenne Datei um in: $TARGET_FILE"
mv -f "$INDEX_SRC" "$TARGET_FILE" || { echo "Fehler beim Umbennenen der Datei."; exit 1; }
 
# 4. Verschieben der Datei ins Zielverzeichnis
DEST_DIR=~/Documents/Gentics/andp-cpa-app/portal/data-common/static/js/
if [[ ! -d "$DEST_DIR" ]]; then
  echo "Zielverzeichnis $DEST_DIR existiert nicht. Erstelle Verzeichnis ..."
  mkdir -p "$DEST_DIR" || { echo "Fehler beim Erstellen des Zielverzeichnisses."; exit 1; }
fi
 
echo "Verschiebe $TARGET_FILE nach $DEST_DIR (vorhandene Datei wird ersetzt)."
mv -f "$TARGET_FILE" "$DEST_DIR" || { echo "Fehler beim Verschieben der Datei."; exit 1; }
 
echo "Fertig: Die Datei wurde erfolgreich verschoben und ersetzt falls vorhanden."