/* eslint-disable @typescript-eslint/naming-convention */
import { EditableProperties } from '@editor-ui/app/common/models';
import {
    CmsFormData,
    EditableFileProps,
    EditableFolderProps,
    EditableFormProps,
    EditablePageProps,
    FileOrImage,
    Folder,
    Form,
    InheritableItem,
    Node,
    Page,
} from '@gentics/cms-models';

export function getItemProperties(item: InheritableItem | Node): EditableProperties {
    // an item with type "node" or "channel" may be the base folder of a node. If it has
    // a folder-only property, then we can assume it is the base folder.
    if ((item.type === 'node' || item.type === 'channel') && item.hasOwnProperty('hasSubfolders')) {
        (item as any).type = 'folder';
    }

    switch (item.type) {
        case 'folder': {
            const f = item as Folder;
            const props: EditableFolderProps = {
                name: f.name,
                description: f.description,
                publishDir: f.publishDir,
                descriptionI18n: f.descriptionI18n,
                nameI18n: f.nameI18n,
                publishDirI18n: f.publishDirI18n,
            };
            return props;
        }

        case 'form':{
            let dataProperties: Partial<CmsFormData> = {};
            const f = item as Form;
            if (f.data) {
                dataProperties = {
                    email: f.data.email,
                    successurl_i18n: f.data.successurl_i18n,
                    successurl: f.data.successurl,
                    mailsubject_i18n: f.data.mailsubject_i18n,
                    mailtemp_i18n: f.data.mailtemp_i18n,
                    mailsource_pageid: f.data.mailsource_pageid,
                    mailsource_nodeid: f.data.mailsource_nodeid,
                    templateContext: f.data.templateContext,
                    type: f.data.type,
                    elements: f.data.elements,
                }
            }

            const props: EditableFormProps = {
                name: f.name,
                description: f.description,
                languages: f.languages,
                successPageId: f.successPageId,
                successNodeId: f.successNodeId,
                data: dataProperties,
            };
            return props;
        }

        case 'page':{
            const p = item as Page;
            const props: EditablePageProps = {
                pageName: (item as any).pageName ?? p.name,
                fileName: p.fileName,
                description: p.description,
                niceUrl: p.niceUrl,
                alternateUrls: p.alternateUrls,
                templateId: p.templateId,
                language: p.language,
                customCdate: p.customCdate,
                customEdate: p.customEdate,
                priority: p.priority,
            };
            return props;
        }

        case 'file':
        case 'image': {
            const f = item as FileOrImage;
            const props: EditableFileProps = {
                name: f.name,
                description: f.description,
                forceOnline: f.forceOnline,
                niceUrl: f.niceUrl,
                alternateUrls: f.alternateUrls,
            };
            return props;
        }

        case 'node':
        case 'channel':
            return item;

        default:
            return null;
    }
}
