import { I18nService, TemplateOperations } from '@admin-ui/core';
import { Injectable } from '@angular/core';
import { Node, Raw, Tag, Template } from '@gentics/cms-models';
import { cloneDeep } from 'lodash-es';


function cleanTags<T extends Tag>(tags: Record<string, T>): Record<string, T> {
    const out: Record<string, T> = {};

    Object.entries(tags).forEach(([key, tag]) => {
        const tagCopy = cloneDeep(tag);
        delete tagCopy.id;

        out[key] = tagCopy;
    });

    return out;
}

@Injectable()
export class CopyTemplateService {

    constructor(
        protected operations: TemplateOperations,
        protected i18n: I18nService,
    ) {
    }

    async createCopy(node: Node, templateToCopy: Template<Raw>): Promise<any> {
        const fullCopy: Template<Raw> = {
            ...templateToCopy,
            name: this.generateCopyName(templateToCopy),
            objectTags: cleanTags(templateToCopy.objectTags),
            templateTags: cleanTags(templateToCopy.templateTags),
        }

        const created = await this.operations.create({
            folderIds: [
                node.folderId,
            ],
            nodeId: node.id,
            template: fullCopy,
        }).toPromise();

        return created;
    }

    private generateCopyName(template: Template<Raw>): string {
        const suffix = this.i18n.instant('common.copy_suffix');

        return `${template.name} ${suffix}`;
    }
}
