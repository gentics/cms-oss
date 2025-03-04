import { Pipe, PipeTransform } from '@angular/core';
import { DocumentationType } from '../../common/docs';

@Pipe({
    name: 'gtxSelectorDisplay',
})
export class SelectorDisplayPipe implements PipeTransform {

    transform(selector: string, type: DocumentationType): string {
        if (!selector || !type) {
            return '';
        }

        switch (type) {
            case DocumentationType.COMPONENT:
                return `<${selector}>...</${selector}>`;
            case DocumentationType.DIRECTIVE:
                return selector.startsWith('[') ? `<div ${selector}>...</div>` : `<${selector}>...</${selector}>`;
            case DocumentationType.PIPE:
                return `{{ exampleData | ${selector} }}`;
            case DocumentationType.SERVICE:
            default:
                return '';
        }
    }

}
