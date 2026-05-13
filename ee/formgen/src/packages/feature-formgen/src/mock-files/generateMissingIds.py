import json
import uuid

# Datei einlesen
default_file = 'defaultFormular.json'
with open(default_file, 'r', encoding='utf-8') as file:
    data = json.load(file)

# Existierende IDs sammeln
existing_ids = {
    element['id']
    for page in data.get('uiSchema', {}).get('pages', [])
    for element in page.get('elements', [])
    if 'id' in element
}

# IDs für fehlende Elemente hinzufügen
for page in data.get('uiSchema', {}).get('pages', []):
    for element in page.get('elements', []):
        form_grid_type = element.get('formGridOptions', {}).get('type')
        if not element.get('id') and (
            element.get('type') in ['formgridText', 'formgridSpacer'] or form_grid_type in ['formgridText', 'formgridSpacer']
        ):
            new_id = str(uuid.uuid4()).upper()
            while new_id in existing_ids:
                new_id = str(uuid.uuid4()).upper()
            element['id'] = new_id
            existing_ids.add(new_id)

# Datei speichern
with open(default_file, 'w', encoding='utf-8') as file:
    json.dump(data, file, indent=2)
