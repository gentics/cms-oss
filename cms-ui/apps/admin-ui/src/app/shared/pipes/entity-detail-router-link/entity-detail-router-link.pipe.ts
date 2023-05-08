import { buildEntityDetailPath } from '@admin-ui/common';
import { Pipe, PipeTransform } from '@angular/core';
import { NormalizableEntityType } from '@gentics/cms-models';

@Pipe({ name: 'gtxEntityDetailRouterLink' })
export class EntityDetailRouterLinkPipe implements PipeTransform {

    transform(typeOrItem: NormalizableEntityType | any, id?: string | number, nodeId?: number): any[] {
        return buildEntityDetailPath(typeOrItem, id, nodeId);
    }
}
