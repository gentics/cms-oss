import { ChangeDetectionStrategy, Component } from '@angular/core';
import { DropdownListComponent } from '@gentics/ui-core';
import { IDocumentation } from '../../common/docs';
import { InjectDocumentation } from '../../common/docs-loader';

@Component({
    templateUrl: './dropdown-list-demo.component.html',
    styles: [`
        .sticky-demo gtx-dropdown-item {
            display: flex;
        }
        .resize-buttons {
            display: flex;
            flex-direction: column;
            align-items: center;
        }
        .resize-buttons gtx-button {
            flex: 1 1 auto;
        }
    `],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DropdownListDemoPage {

    @InjectDocumentation('dropdown-list.component')
    documentation: IDocumentation;

    dropdownIsDisabled = false;
    dropdownItemIsDisabled = true;
    variableItems: string[] = ['Item 1', 'Item 2', 'Item 3'];

    add(dropdown: DropdownListComponent): void {
        const count = this.variableItems.length;
        this.variableItems.push(`Item ${count + 1}`);
        dropdown.resize();
    }

    remove(dropdown: DropdownListComponent): void {
        if (0 < this.variableItems.length) {
            this.variableItems.pop();
            dropdown.resize();
        }
    }
}
