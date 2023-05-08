import { MeshTagmapEntryAttributeTypes, SQLTagmapEntryAttributeTypes, TagmapEntryAttributeTypes } from '@gentics/cms-models';

export const TAGMAP_ENTRY_ATTRIBUTES: { id: TagmapEntryAttributeTypes; label: string; }[] = [
    {
        id: TagmapEntryAttributeTypes.TEXT,
        label: 'tagmapEntry.attributetype_text',
    },
    {
        id: TagmapEntryAttributeTypes.REFERENCE,
        label: 'tagmapEntry.attributetype_reference',
    },
    {
        id: TagmapEntryAttributeTypes.INTEGER,
        label: 'tagmapEntry.attributetype_integer',
    },
    {
        id: TagmapEntryAttributeTypes.TEXT_LONG,
        label: 'tagmapEntry.attributetype_text_long',
    },
    {
        id: TagmapEntryAttributeTypes.BINARY,
        label: 'tagmapEntry.attributetype_binary',
    },
    {
        id: TagmapEntryAttributeTypes.FOREIGN_LINK,
        label: 'tagmapEntry.attributetype_foreign_link',
    },
    {
        id: TagmapEntryAttributeTypes.DATE,
        label: 'tagmapEntry.attributetype_date',
    },
    {
        id: TagmapEntryAttributeTypes.BOOLEAN,
        label: 'tagmapEntry.attributetype_boolean',
    },
    {
        id: TagmapEntryAttributeTypes.MICRONODE,
        label: 'tagmapEntry.attributetype_micronode',
    },
];

export const TAGMAP_ENTRY_ATTRIBUTES_MAP = TAGMAP_ENTRY_ATTRIBUTES.reduce((acc, entry) => {
    acc[entry.id] = entry.label;
    return acc;
}, {});
export const MESH_TAGMAP_ENTRY_ATTRIBUTES = TAGMAP_ENTRY_ATTRIBUTES.filter(attr => MeshTagmapEntryAttributeTypes[attr.id]);
export const SQL_TAGMAP_ENTRY_ATTRIBUTES = TAGMAP_ENTRY_ATTRIBUTES.filter(attr => SQLTagmapEntryAttributeTypes[attr.id]);
