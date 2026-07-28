import { Pipe, PipeTransform } from '@angular/core';
import { I18nService } from '@gentics/cms-components';

@Pipe({
    name: 'dependenciesCount',
    standalone: false,
})
export class DependenciesCountPipe implements PipeTransform {

    constructor(private i18n: I18nService) { }

    transform(items: any): string {
        if (!items) {
            return '';
        }
        const allTypes = ['page', 'file', 'image'];

        const dependencyTextParts = allTypes
            .filter((type) => items[type])
            .map((type) => {
                const itemsCount = Object.keys(items[type]).length;
                const typeText = this.i18n.instant('common.type_' + type + (itemsCount > 1 ? 's' : ''));
                return `${itemsCount} ${typeText}`;
            });
        return dependencyTextParts.join(', ');
    }
}
