import { NodeOperations } from '@admin-ui/core';
import { LanguageTableComponent } from '@admin-ui/shared';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, forwardRef, Input, ViewChild } from '@angular/core';
import { Language } from '@gentics/cms-models';
import { BaseModal } from '@gentics/ui-core';

@Component({
    selector: 'gtx-assign-languages-to-node-modal',
    templateUrl: './assign-languages-to-node-modal.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AssignLanguagesToNodeModal extends BaseModal<Language[]> {

    @Input()
    public nodeId: number;

    @Input()
    public nodeName: string;

    @Input()
    public selectedLanguages: string[];

    @ViewChild(forwardRef(() => LanguageTableComponent))
    public table: LanguageTableComponent;

    public loading = false;

    constructor(
        protected changeDetector: ChangeDetectorRef,
        protected operations: NodeOperations,
    ) {
        super();
    }

    async updateLanguages(): Promise<void> {
        try {
            this.loading = true;
            this.changeDetector.markForCheck();

            const nodeLanguages: Language[] = this.table.getSelectedEntities();
            const languages = await this.operations.updateNodeLanguages(this.nodeId, nodeLanguages).toPromise();

            this.loading = false;
            this.changeDetector.markForCheck();

            this.closeFn(languages);
        } catch (err) {
            // Ignored
            this.loading = false;
            this.changeDetector.markForCheck();
        }
    }
}
