import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
    name: 'gtxI18n',
    standalone: false,
})
export class MockI18nPipe implements PipeTransform {
    transform(query: string, ...args: any[]): string {
        return query;
    }
}
