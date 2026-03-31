/* eslint-disable @typescript-eslint/naming-convention */
import {
    EditableFileProps,
    EditableFolderProps,
    EditableFormProperties,
    EditablePageProps,
    FileOrImage,
    Folder,
    Form,
    InheritableItem,
    Node,
    Page,
} from '@gentics/cms-models';
import { EditableProperties } from '../../common/models';

export function getItemProperties(item: InheritableItem | Node): EditableProperties {
    if (item == null) {
        return null;
    }

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
            const f = item as Form;
            const props: Partial<EditableFormProperties> = {
                formType: f.formType,
                name: f.name,
                fileName: f.fileName,
                description: f.description,
                languages: f.languages,
                templateContext: f.templateContext,
                flow: f.flow,
                successPageId: f.successPageId,
                successNodeId: f.successNodeId,
                successUrlI18n: f.successUrlI18n,
                adminEmailAddress: f.adminEmailAddress,
                adminEmailSubject: f.adminEmailSubject,
                adminEmailPageId: f.adminEmailPageId,
                adminEmailNodeId: f.adminEmailNodeId,
                adminEmailTemplate: f.adminEmailTemplate,
            };
            return props;
        }

        case 'page':{
            const p = item as Page;
            const props: EditablePageProps = {
                name: p.name,
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
                customCdate: f.customCdate,
                customEdate: f.customEdate,
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
