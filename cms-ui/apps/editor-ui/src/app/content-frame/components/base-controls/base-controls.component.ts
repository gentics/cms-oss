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
            if (this.aloha) {
                try {
                    this.contentRules = this.aloha.require('aloha/content-rules');
                } catch (err) {
                    this.contentRules = null;
                    console.error('Error while loading aloha content-rules!', err);
                }
                try {
                    this.pubSub = this.aloha.require('PubSub');
                    this.pubSub.sub('aloha.editable.activated', () => {
                        this.editableChanged();
                    });
                    this.pubSub.sub('aloha.editable.deactivated', () => {
                        this.editableChanged();
                    });
                } catch (err) {
                    console.warn('Error while loading pub-sub!', err);
                    this.pubSub = null;
                }
            } else {
                this.contentRules = null;
                this.pubSub = null;
            }
        }

        if (changes.range || changes.settings) {
            this.rangeOrSettingsChanged();
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
