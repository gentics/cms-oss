import { ContentItem, PickableEntity } from '@admin-ui/common';
import { I18nService, JoinOptions } from '@admin-ui/core';
import { Pipe, PipeTransform } from '@angular/core';

type DisplayableEntity = PickableEntity | ContentItem;

interface NamingOptions {
    filter?: boolean;
    join?: JoinOptions;
}

function isPickableEntity(value: any): value is PickableEntity {
    return value != null && typeof value === 'object' && value.hasOwnProperty('entity') && value.hasOwnProperty('nodeId');
}

@Pipe({
    name: 'entityName',
    standalone: false
})
export class EntityNamePipe implements PipeTransform {

    constructor(private i18n: I18nService) { }

    transform(value: DisplayableEntity | DisplayableEntity[], options?: NamingOptions): any {
        if (value == null || typeof value !== 'object') {
            return options?.filter ? '' : value;
        }

        if (Array.isArray(value)) {
            return this.i18n.join(value.map(entity => this.getEntityName(entity)), options?.join);
        }

        return this.getEntityName(value);
    }

    private getEntityName(entity: DisplayableEntity, options?: NamingOptions): string {
        if (isPickableEntity(entity)) {
            entity = entity.entity;
        }

        switch (entity.type) {
            case 'channel':
            case 'file':
            case 'folder':
            case 'form':
            case 'image':
            case 'node':
            case 'page':
            case 'template':
                return entity.name;
            default:
                return options?.filter ? '' : String(entity);
        }
    }
}
