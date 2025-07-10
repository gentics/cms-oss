import { ChangeDetectionStrategy, Component } from '@angular/core';
import { ISortableEvent } from '@gentics/ui-core';
import { IDocumentation } from '../../common/docs';
import { InjectDocumentation } from '../../common/docs-loader';

@Component({
    templateUrl: './sortable-list-demo.component.html',
    styleUrls: ['./sortable-list-demo.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class SortableListDemoPage {

    @InjectDocumentation('sortable-list.component')
    documentation: IDocumentation;

    items: any[] = [
        {
            name: 'John',
            age: 21,
        },
        {
            name: 'Mary',
            age: 26,
        },
        {
            name: 'Barry',
            age: 43,
        },
        {
            name: 'Susan',
            age: 32,
        },
    ];
    longList: string[] = [];
    disabled = false;

    constructor() {
        for (let i = 1; i < 30; i++) {
            this.longList.push(`Item ${i}`);
        }
    }

    sortList(e: ISortableEvent): void {
        this.items = e.sort(this.items);
    }

    sortLongList(e: ISortableEvent): void {
        this.longList = e.sort(this.longList) as string[];
    }
}
