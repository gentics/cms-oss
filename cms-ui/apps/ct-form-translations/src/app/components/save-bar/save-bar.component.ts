import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Input,
    Output,
} from '@angular/core';

@Component({
    selector: 'gtx-save-bar',
    standalone: false,
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './save-bar.component.html',
    styleUrls: ['./save-bar.component.scss'],
})
export class SaveBarComponent {
    @Input() dirtyCount = 0;
    @Input() saving = false;

    @Output() readonly save = new EventEmitter<void>();
    @Output() readonly discard = new EventEmitter<void>();

    /** Backwards-compatible alias for the template (`isSaving`). */
    get isSaving(): boolean { return this.saving; }
}
