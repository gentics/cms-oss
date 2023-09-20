import { ChangeDetectorRef, Component, Input } from '@angular/core';
import { AlohaSettings } from '../../../common/models/aloha';

@Component({ template: '' })
export abstract class BaseControlsComponent {

    @Input()
    public selectedElement: HTMLElement;

    @Input()
    public settings: AlohaSettings;

    constructor(
        protected changeDetector: ChangeDetectorRef,
    ) {}
}
