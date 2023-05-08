import { Injectable } from '@angular/core';
import { IBreadcrumbRouterLink, IBreadcrumbLink } from '@gentics/ui-core';

@Injectable()
export class BreadcrumbsService {
    addTooltip <T extends IBreadcrumbLink | IBreadcrumbRouterLink>(breadcrumbs: T[]): T[] {
        const fullBreadcrumbsArray: string[] = [];

        return breadcrumbs.map(breadcrumb => {
            fullBreadcrumbsArray.push(breadcrumb.text);
            return {
                ...breadcrumb,
                tooltip: fullBreadcrumbsArray.join(' › '),
            };
        });
    }
}
