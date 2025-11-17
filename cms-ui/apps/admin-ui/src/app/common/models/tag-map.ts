import { MeshTagmapEntryAttributeTypes, SQLTagmapEntryAttributeTypes, TagmapEntryAttributeTypes } from '@gentics/cms-models';

export const TAGMAP_ENTRY_ATTRIBUTES: { id: TagmapEntryAttributeTypes; label: string; }[] = [
    {
        id: TagmapEntryAttributeTypes.TEXT,
        label: 'tagmap_entry.attributetype_text',
    },
    {
        id: TagmapEntryAttributeTypes.REFERENCE,
        label: 'tagmap_entry.attributetype_reference',
    },
    {
        id: TagmapEntryAttributeTypes.INTEGER,
        label: 'tagmap_entry.attributetype_integer',
    },
    {
        id: TagmapEntryAttributeTypes.TEXT_LONG,
        label: 'tagmap_entry.attributetype_text_long',
    },
    {
        id: TagmapEntryAttributeTypes.BINARY,
        label: 'tagmap_entry.attributetype_binary',
    },
    {
        id: TagmapEntryAttributeTypes.FOREIGN_LINK,
        label: 'tagmap_entry.attributetype_foreign_link',
    },
    {
        id: TagmapEntryAttributeTypes.DATE,
        label: 'tagmap_entry.attributetype_date',
    },
    {
        id: TagmapEntryAttributeTypes.BOOLEAN,
        label: 'tagmap_entry.attributetype_boolean',
    },
    {
        id: TagmapEntryAttributeTypes.MICRONODE,
        label: 'tagmap_entry.attributetype_micronode',
    },
];

export const TAGMAP_ENTRY_ATTRIBUTES_MAP = TAGMAP_ENTRY_ATTRIBUTES.reduce((acc, entry) => {
    acc[entry.id] = entry.label;
    return acc;
}, {});
export const MESH_TAGMAP_ENTRY_ATTRIBUTES = TAGMAP_ENTRY_ATTRIBUTES.filter(attr => MeshTagmapEntryAttributeTypes[attr.id]);
export const SQL_TAGMAP_ENTRY_ATTRIBUTES = TAGMAP_ENTRY_ATTRIBUTES.filter(attr => SQLTagmapEntryAttributeTypes[attr.id]);
