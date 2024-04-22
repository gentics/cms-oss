import { Pipe, PipeTransform } from '@angular/core';
import { iconForItemType } from '@editor-ui/app/common/utils/icon-for-item-type';

@Pipe({
    name: 'gtxTypeIcon',
})
export class TypeIconPipe implements PipeTransform {
    transform(value: string, fallback?: string): string {
        return iconForItemType(value, fallback);
    }

}
