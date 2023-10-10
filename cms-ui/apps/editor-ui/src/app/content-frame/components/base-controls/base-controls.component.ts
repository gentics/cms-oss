import { ChangeDetectorRef, Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { AlohaContentRules, AlohaPubSub, AlohaRangeObject, AlohaSettings } from '@gentics/aloha-models';
import { BaseComponent } from '@gentics/ui-core';
import { AlohaGlobal } from '../../models/content-frame';

@Component({ template: '' })
export abstract class BaseControlsComponent extends BaseComponent implements OnChanges {

    @Input()
    public aloha: AlohaGlobal;

    @Input()
    public range: AlohaRangeObject;

    @Input()
    public settings: AlohaSettings;

    protected pubSub: AlohaPubSub;
    protected contentRules: AlohaContentRules;

    constructor(
        changeDetector: ChangeDetectorRef,
    ) {
        super(changeDetector);
    }

    public override ngOnChanges(changes: SimpleChanges): void {
        super.ngOnChanges(changes);

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
