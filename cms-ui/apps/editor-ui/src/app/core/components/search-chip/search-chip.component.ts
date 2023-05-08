import { ChangeDetectionStrategy, Component, ElementRef, EventEmitter, Input, Output, ViewChild } from '@angular/core';

@Component({
    selector: 'search-chip',
    templateUrl: './search-chip.component.html',
    styleUrls: ['./search-chip.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SearchChipComponent {

    /** Prefix (like category) of the text */
    @Input()
    prefix = '';

    /** Text used in the search bar when editing the search chip */
    @Input()
    editText: string | undefined = undefined;

    /** If bound in the surrounding component, editing the search chip  */
    @Output()
    remove = new EventEmitter<void>();

    @ViewChild('contents', { static: true })
    contents: ElementRef;

    getTextForEditing(): string {
        if (this.editText != null) {
            return this.editText;
        }
        const element = this.contents.nativeElement as HTMLElement;
        return element.innerText;
    }

    showRemoveButton(): boolean {
        return this.remove.observers.length > 0;
    }

}
