import { ChangeDetectionStrategy, Component } from '@angular/core';
import { IDocumentation } from '../../common/docs';
import { InjectDocumentation } from '../../common/docs-loader';

@Component({
    templateUrl: './select-demo.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SelectDemoPage {

    @InjectDocumentation('select.component')
    documentation: IDocumentation;

    options: string[] = ['foo', 'bar', 'baz', 'quux', 'qwerty', 'dump', 'lorem',
        'ipsum', 'dolor', 'sit', 'amet', 'consectetur', 'adipiscing', 'elit'];
    selectMultiVal: string[] = ['bar', 'baz'];
    smallOptions: string[] = ['foo', 'bar', 'baz'];
    people: any[] = [
        { name: 'John', age: 22, disabled: false },
        { name: 'Susan', age: 34, disabled: true },
        { name: 'Paul', age: 30, disabled: false },
    ];
    selectVal = 'bar';
    selectNewVal = 'baz';
    clearableSelectVal: string = null;
    placeholderSelectVal: string = null;
    selectGroup: any;
    selectedPerson: any;
    disableEntireControl = false;
    disableSingleOption = false;
    disableOptionGroup = false;
}
