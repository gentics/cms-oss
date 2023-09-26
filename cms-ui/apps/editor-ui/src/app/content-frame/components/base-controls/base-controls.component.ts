import { ChangeDetectorRef, Component, Input } from '@angular/core';
import { AlohaRangeObject, AlohaSettings } from '@gentics/aloha-models';
import { AlohaGlobal } from '../content-frame/common';

@Component({ template: '' })
export abstract class BaseControlsComponent {

    @Input()
    public aloha: AlohaGlobal;

    @Input()
    public range: AlohaRangeObject;

    @Input()
    public settings: AlohaSettings;

    constructor(
        protected changeDetector: ChangeDetectorRef,
    ) {}
}
