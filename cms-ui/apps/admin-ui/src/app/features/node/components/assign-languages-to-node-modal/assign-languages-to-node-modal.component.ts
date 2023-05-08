import { LanguageTableLoaderService, NodeOperations } from '@admin-ui/core';
import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
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

    constructor(
        protected loader: LanguageTableLoaderService,
        protected operations: NodeOperations,
    ) {
        super();
    }

    async updateLanguages(): Promise<void> {
        const nodeLanguages: Language[] = this.loader.getEntitiesByIds(this.selectedLanguages);
        const languages = await this.operations.updateNodeLanguages(this.nodeId, nodeLanguages).toPromise();

        this.closeFn(languages);
    }
}
