import logomark from '@/assets/gentics-logomark.png';
import type {
    ApiCallLogPart,
    CitationPart,
    ConstructDraftPart,
    FileRefPart,
    ImageGridPart,
    MessagePart,
    PageStructurePart,
    PartType,
    PropertiesListPart,
    SelectableListPart,
    StatusNotePart,
    TablePart,
    TextPart,
    TreeViewPart,
    UnknownPart,
} from '@/services/apiService/genaix/types';

/** One variant of a part, shown in the catalogue; `id` names its label (`catalogue.parts.variants.<id>`). */
export interface PartSample {
    id: string;
    part: MessagePart | UnknownPart;
}

/** The variants of one part type; `type` is the contract's part type, or `fallback`. */
export interface PartSampleGroup {
    type: string;
    samples: PartSample[];
}

const textMarkdown: TextPart = {
    type: 'text',
    format: 'markdown',
    text: [
        '### Neue Nutzungsbedingungen ab Q3',
        '',
        'Ich habe die Aktualisierung gelesen. Sie bringt **drei inhaltliche Änderungen**:',
        '',
        '- Die Versandkostenfrei-Grenze steigt von 35 Euro auf *50 Euro*.',
        '- Das Widerrufsrecht läuft 14 Tage ab Erhalt der Ware.',
        '- Die Garantie wird auf 24 Monate verlängert.',
        '',
        '> Der Widerruf kann innerhalb von 14 Tagen ab Erhalt der Ware erfolgen.',
        '',
        'Die Seite liegt unter `/richtlinien/neue-nutzungsbedingungen-ab-q3.html`, mehr dazu in der [Dokumentation](https://www.gentics.com).',
    ].join('\n'),
};

const textPlain: TextPart = {
    type: 'text',
    format: 'plain',
    text: 'Der Entwurf steht unveröffentlicht in Richtlinien.\nMarkdown wie *dieses* bleibt hier als Text stehen.',
};

const noteInfo: StatusNotePart = { type: 'status_note', kind: 'info', text: 'Die Seite wurde als unveröffentlichter Entwurf angelegt.' };
const noteWarning: StatusNotePart = { type: 'status_note', kind: 'warning', text: 'Das Hero-Bild hat noch keinen Alternativtext.' };
const noteSkipped: StatusNotePart = { type: 'status_note', kind: 'skipped', text: 'Der Querverweis von „Garantiebedingungen“ wurde übersprungen.' };

const treeNested: TreeViewPart = {
    type: 'tree_view',
    label: 'Ordnerstruktur von Acme GmbH',
    nodes: [
        {
            id: 'n-3',
            label: 'Acme GmbH',
            type: 'node',
            children: [
                {
                    id: 'f-42',
                    label: 'Richtlinien',
                    type: 'folder',
                    selected: true,
                    children: [
                        { id: 'p-8871', label: 'Garantiebedingungen', type: 'page', children: [] },
                        { id: 'p-9142', label: 'Neue Nutzungsbedingungen ab Q3', type: 'page', children: [], selected: true },
                    ],
                },
                {
                    id: 'f-51',
                    label: 'Bilder',
                    type: 'folder',
                    children: [{ id: 'i-311', label: 'hero-nutzungsbedingungen.jpg', type: 'image', children: [] }],
                },
                { id: 'f-60', label: 'Downloads', type: 'folder', children: [] },
            ],
        },
    ],
};

const treeFlat: TreeViewPart = {
    type: 'tree_view',
    nodes: [
        { id: 'p-1', label: 'Startseite', type: 'page', children: [] },
        { id: 'p-2', label: 'Kontakt', type: 'page', children: [] },
        { id: 'x-3', label: 'Element ohne bekannten Typ', children: [] },
    ],
};

const listSingle: SelectableListPart = {
    type: 'selectable_list',
    label: 'Template wählen',
    multi: false,
    items: [
        {
            id: '17',
            label: 'Kampagnen-Landingpage',
            description: 'Erlaubt hero_teaser, text_columns, quote_block und form_embed.',
            ref: { type: 'template', id: '17', node_id: 3, label: 'Kampagnen-Landingpage' },
        },
        {
            id: '21',
            label: 'Themenseite',
            description: 'Ohne form_embed: die Anmeldung würde nur ein Link.',
            ref: { type: 'template', id: '21', node_id: 3, label: 'Themenseite' },
        },
    ],
};

const listMulti: SelectableListPart = {
    type: 'selectable_list',
    label: 'Seiten zum Aktualisieren',
    multi: true,
    selected: ['8871', '9020'],
    items: [
        { id: '8871', label: 'Garantiebedingungen', ref: { type: 'page', id: '8871', node_id: 3, label: 'Garantiebedingungen' } },
        { id: '9020', label: 'Versand und Lieferung' },
        { id: '9031', label: 'Häufige Fragen' },
    ],
};

const properties: PropertiesListPart = {
    type: 'properties_list',
    label: 'Seiteneigenschaften',
    items: [
        { key: 'name', label: 'Name', value: 'Neue Nutzungsbedingungen ab Q3', editable: false },
        { key: 'folder', label: 'Ordner', value: 'Richtlinien', editable: false, ref: { type: 'folder', id: '42', node_id: 3, label: 'Richtlinien' } },
        { key: 'template', label: 'Template', value: null, editable: false, ref: { type: 'template', id: '17', node_id: 3 } },
        { key: 'language', label: 'Sprache', value: 'Deutsch', editable: true },
        { key: 'publish_at', label: 'Veröffentlichen am', value: null, editable: true },
    ],
};

const imagesSingle: ImageGridPart = {
    type: 'image_grid',
    label: 'Hero-Bild wählen',
    multi: false,
    items: [
        { id: 'i-311', url: logomark, alt: 'Logo auf Weiß', ref: { type: 'image', id: '311', node_id: 3 } },
        { id: 'i-312', url: logomark, alt: 'Logo, Variante 2' },
        { id: 'i-313', url: logomark, alt: 'Logo, Variante 3' },
    ],
};

const imagesMulti: ImageGridPart = {
    type: 'image_grid',
    label: 'Bilder für die Galerie',
    multi: true,
    items: [
        { id: 'i-401', url: logomark, alt: 'Galerie 1', selected: true },
        { id: 'i-402', url: logomark, alt: 'Galerie 2' },
        { id: 'i-403', url: logomark, alt: 'Galerie 3', selected: true },
        { id: 'i-404', url: logomark, ref: { type: 'image', id: '404', node_id: 3, label: 'galerie-4.png' } },
        { id: 'i-405', url: logomark },
    ],
};

const tableFull: TablePart = {
    type: 'table',
    label: 'Seiten mit dem alten Versandschwellenwert',
    columns: [
        { key: 'page', label: 'Seite', type: 'ref' },
        { key: 'views', label: 'Aufrufe', type: 'number' },
        { key: 'edited', label: 'Zuletzt geändert', type: 'date' },
        { key: 'online', label: 'Online', type: 'boolean' },
        { key: 'owner', label: 'Verantwortlich', type: 'string' },
    ],
    rows: [
        { page: { type: 'page', id: '8871', node_id: 3, label: 'Garantiebedingungen' }, views: 12840, edited: '2026-06-14', online: true, owner: 'Marketing' },
        { page: { type: 'page', id: '9020', node_id: 3, label: 'Versand und Lieferung' }, views: 5321, edited: '2026-09-30', online: true, owner: null },
        { page: { type: 'page', id: '9031', node_id: 3, label: 'Häufige Fragen' }, views: 0, edited: null, online: false, owner: '' },
    ],
    total: 7,
    source: { summary: 'search_content, Knoten Acme GmbH, nur Seiten', tool: 'search_content' },
};

const tableSmall: TablePart = {
    type: 'table',
    columns: [
        { key: 'key', label: 'Einstellung', type: 'string' },
        { key: 'value', label: 'Wert', type: 'string' },
    ],
    rows: [
        { key: 'Sprache', value: 'Deutsch' },
        { key: 'Template', value: 'Kampagnen-Landingpage' },
    ],
};

const structureDone: PageStructurePart = {
    type: 'page_structure',
    label: 'Seitenstruktur',
    page_ref: { type: 'page', id: 9142, node_id: 3, name: 'Neue Nutzungsbedingungen ab Q3' },
    blocks: [
        { id: 'blk-hero', construct_keyword: 'hero_teaser', label: 'Hero', status: 'created', verbatim: false, tag_keyword: 'content_1' },
        { id: 'blk-changes', construct_keyword: 'text_columns', label: 'Die drei wichtigsten Änderungen', status: 'updated', verbatim: false, tag_keyword: 'content_2' },
        { id: 'blk-legal', construct_keyword: 'quote_block', label: 'Widerrufsbelehrung Absatz 1', status: 'locked', verbatim: true, tag_keyword: 'content_3', note: 'Wortgleich aus dem PDF' },
        { id: 'blk-form', construct_keyword: 'form_embed', label: 'Anmeldung Info-Webinar', status: 'failed', verbatim: false, note: 'Formular nicht gefunden' },
        { id: 'blk-link', construct_keyword: 'link_list', label: 'Weiterführende Seiten', status: 'planned', verbatim: false },
    ],
};

const structurePlanned: PageStructurePart = {
    type: 'page_structure',
    label: 'Seitenstruktur',
    blocks: [
        { id: 'blk-hero', construct_keyword: 'hero_teaser', label: 'Hero', status: 'planned', verbatim: false },
        { id: 'blk-legal', construct_keyword: 'quote_block', label: 'Widerrufsbelehrung Absatz 1', status: 'planned', verbatim: true },
    ],
};

const draftValid: ConstructDraftPart = {
    type: 'construct_draft',
    keyword: 'terms_change_table',
    name: { de: 'Änderungstabelle', en: 'Change table' },
    description: { de: 'Stellt alte und neue Regelung gegenüber.', en: 'Compares the old and the new rule.' },
    parts: [
        { keyword: 'title', name: { de: 'Titel', en: 'Title' }, type_id: 1, editable: true, mandatory: true, hidden: false },
        { keyword: 'rows', name: { de: 'Zeilen', en: 'Rows' }, type_id: 21, editable: true, mandatory: false, hidden: false },
        { keyword: 'css_class', type_id: 1, editable: false, mandatory: false, hidden: true, default: 'changes' },
    ],
    template: '<table class="{{cms.tag.parts.css_class}}">\n  <caption>{{cms.tag.parts.title}}</caption>\n  {{cms.tag.parts.rows}}\n</table>',
    validation: { ok: true },
};

const draftIssues: ConstructDraftPart = {
    type: 'construct_draft',
    keyword: 'webinar_teaser',
    name: { de: 'Webinar-Teaser', en: 'Webinar teaser' },
    parts: [{ keyword: 'headline', type_id: 1, editable: true, mandatory: true, hidden: false }],
    template: '<div class="teaser">\n  <h3>{{cms.tag.parts.headline}</h3>\n  {{#if cms.tag.parts.date}}<p>{{cms.tag.parts.date}}</p>\n</div>',
    validation: {
        ok: false,
        issues: [
            { severity: 'error', message: 'Nicht geschlossener Ausdruck: {{cms.tag.parts.headline}', line: 2, column: 7 },
            { severity: 'warning', message: 'Block {{#if}} wird nicht geschlossen.', line: 3 },
            { severity: 'info', message: 'Der Teil „date“ ist im Template, aber nicht als Teil angelegt.' },
        ],
    },
};

const draftMinimal: ConstructDraftPart = {
    type: 'construct_draft',
    keyword: 'spacer',
    name: {},
    parts: [],
    template: '',
};

const apiCalls: ApiCallLogPart = {
    type: 'api_call_log',
    label: 'CMS-Aufrufe',
    calls: [
        { method: 'GET', path: '/rest/construct/list', summary: 'Bausteine des Knotens gelesen', status: 200, duration_ms: 84 },
        { method: 'POST', path: '/rest/construct', summary: 'Baustein angelegt', status: 201, tool: 'create_construct' },
        { method: 'PUT', path: '/rest/construct/88', summary: 'Template aktualisiert', status: 409 },
        { method: 'DELETE', path: '/rest/construct/89', summary: 'Entwurf gelöscht', status: 500 },
    ],
};

const citations: CitationPart = {
    type: 'citation',
    refs: [
        {
            marker: '[1]',
            ref: { type: 'page', id: '8871', node_id: 3, label: 'Garantiebedingungen' },
            snippet: '… Versand ist ab einem Warenwert von <em>35 Euro</em> kostenfrei …',
            score: 8.41,
        },
        {
            marker: '[2]',
            ref: { type: 'session_file', id: '6b0d9e31-44a8-4c57-9f2e-8d1a3c7b5e02', label: 'neue-nutzungsbedingungen-ab-q3.pdf' },
            snippet: 'Widerrufsbelehrung Absatz 1: Der Widerruf kann innerhalb von <em>14 Tagen</em> ab Erhalt der Ware erfolgen.',
        },
    ],
};

const citationsBare: CitationPart = {
    type: 'citation',
    refs: [
        { ref: { type: 'url', id: 'https://www.gentics.com' } },
        { ref: { type: 'guideline', id: 'tone-of-voice-de', label: 'Tone of Voice (DE)' } },
    ],
};

const fileNamed: FileRefPart = {
    type: 'file_ref',
    file_id: 'a70c2f18-9b5d-4e63-8c01-7d4f2a9e6b53',
    name: 'page-draft-9142.json',
    media_type: 'application/json',
    size: 14702,
    kind: 'artifact',
};

const fileUnnamed: FileRefPart = { type: 'file_ref', file_id: '0f3e9a21-5c84-4b17-a6d2-8e1b7c4f9a30' };

const unknownWithText: UnknownPart = { type: 'chart_v2', text: 'Aufrufe der letzten 30 Tage: 12 840 (+14 %).', series: [1, 2, 3] };
const unknownWithoutText: UnknownPart = { type: 'chart_v2', series: [1, 2, 3] };
// A known type whose renderer fails on it: the error boundary shows the `UnknownPart` fallback. Not a
// valid part on purpose (`UnknownPart.type` is meant for types outside `PartType`).
const brokenTable: UnknownPart = { type: 'table', columns: 'kaputt', text: 'Diese Tabelle konnte nicht dargestellt werden.' };

// One entry per member of the contract's `PartType`: a part type without samples does not compile.
const samplesByType: { [Type in PartType]: PartSample[] } & { fallback: PartSample[] } = {
    text: [{ id: 'textMarkdown', part: textMarkdown }, { id: 'textPlain', part: textPlain }],
    status_note: [{ id: 'noteInfo', part: noteInfo }, { id: 'noteWarning', part: noteWarning }, { id: 'noteSkipped', part: noteSkipped }],
    tree_view: [{ id: 'treeNested', part: treeNested }, { id: 'treeFlat', part: treeFlat }],
    selectable_list: [{ id: 'listSingle', part: listSingle }, { id: 'listMulti', part: listMulti }],
    properties_list: [{ id: 'properties', part: properties }],
    image_grid: [{ id: 'imagesSingle', part: imagesSingle }, { id: 'imagesMulti', part: imagesMulti }],
    table: [{ id: 'tableFull', part: tableFull }, { id: 'tableSmall', part: tableSmall }],
    page_structure: [{ id: 'structureDone', part: structureDone }, { id: 'structurePlanned', part: structurePlanned }],
    construct_draft: [{ id: 'draftValid', part: draftValid }, { id: 'draftIssues', part: draftIssues }, { id: 'draftMinimal', part: draftMinimal }],
    api_call_log: [{ id: 'apiCalls', part: apiCalls }],
    citation: [{ id: 'citations', part: citations }, { id: 'citationsBare', part: citationsBare }],
    file_ref: [{ id: 'fileNamed', part: fileNamed }, { id: 'fileUnnamed', part: fileUnnamed }],
    fallback: [
        { id: 'unknownWithText', part: unknownWithText },
        { id: 'unknownWithoutText', part: unknownWithoutText },
        { id: 'brokenTable', part: brokenTable },
    ],
};

/** Every part type of the registry, each in its variants, plus the fallbacks. */
export const partSampleGroups: PartSampleGroup[] = Object.entries(samplesByType).map(([type, samples]) => ({ type, samples }));
