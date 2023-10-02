import { ChangeDetectorRef, Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { AlohaContentRules, AlohaPubSub, AlohaRangeObject, AlohaSettings } from '@gentics/aloha-models';
import { AlohaGlobal } from '../content-frame/common';

@Component({ template: '' })
export abstract class BaseControlsComponent implements OnChanges {

    @Input()
    public aloha: AlohaGlobal;

    @Input()
    public range: AlohaRangeObject;

    @Input()
    public settings: AlohaSettings;

    protected pubSub: AlohaPubSub;
    protected contentRules: AlohaContentRules;

    constructor(
        protected changeDetector: ChangeDetectorRef,
    ) {}

    public ngOnChanges(changes: SimpleChanges): void {
        if (changes.aloha) {
            this.contentRules = this.safeRequire('aloha/content-rules');
            this.pubSub = this.safeRequire('PubSub');
        }

        if (changes.range || changes.settings) {
            this.rangeOrSettingsChanged();
        }
    }

    protected safeRequire(dependency: string): any {
        if (!this.aloha) {
            return null;
        }
        try {
            return this.aloha.require(dependency);
        } catch (err) {
            console.warn(`Could not require aloha element "${dependency}"!`, err);
            return null;
        }
    }

    protected editableChanged(): void {
        this.selectionOrEditableChanged();
    }

    protected rangeOrSettingsChanged(): void {
        this.selectionOrEditableChanged();
    }

    protected selectionOrEditableChanged(): void {
        // No-op
    }
}
