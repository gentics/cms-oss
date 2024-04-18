import { createFormValidityTracker, WizardStepNextClickFn } from '@admin-ui/common';
import { FeatureOperations, LanguageTableLoaderService, NodeOperations } from '@admin-ui/core';
import { Wizard, WizardComponent } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import { Component, OnInit, ViewChild } from '@angular/core';
import { FormControl, UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { createNestedControlValidator } from '@gentics/cms-components';
import { Language, Node, NodeCreateRequest, NodeFeatureModel, Raw } from '@gentics/cms-models';
import { Observable, of as observableOf, of } from 'rxjs';
import { map, startWith, switchMap, tap } from 'rxjs/operators';
import { NodePropertiesFormData } from '..';
import { NodeFeaturesFormData } from '../node-features/node-features.component';
import { NodePropertiesComponent, NodePropertiesMode } from '../node-properties/node-properties.component';
import { NodePublishingPropertiesFormData } from '../node-publishing-properties/node-publishing-properties.component';

const FG_PUBLISHING_DEFAULT: Partial<NodePublishingPropertiesFormData> = {
    urlRenderWayFiles: 0,
    urlRenderWayPages: 0,
};

@Component({
    selector: 'gtx-create-node-wizard',
    templateUrl: './create-node-wizard.component.html',
    styleUrls: ['./create-node-wizard.component.scss'],
})
export class CreateNodeWizardComponent implements OnInit, Wizard<Node<Raw>> {

    public readonly NodePropertiesMode = NodePropertiesMode;

    @ViewChild(WizardComponent, { static: true })
    wizard: WizardComponent;

    @ViewChild(NodePropertiesComponent)
    nodeProperties: NodePropertiesComponent;

    /** Form data of tab 'Properties' */
    fgProperties: FormControl<NodePropertiesFormData>;

    isChildNode: boolean;

    /** form of tab 'Publishing' */
    fgPublishing: FormControl<NodePublishingPropertiesFormData>;

    /** form of tab 'Node Features' */
    fgNodeFeatures: UntypedFormGroup;

    nodeFeatures$: Observable<NodeFeatureModel[]>;

    selectedLanguages: string[] = [];

    fgPropertiesValid$: Observable<boolean>;
    fgPublishingValid$: Observable<boolean>;
    fgNodeFeaturesValid$: Observable<boolean>;

    finishClickAction: WizardStepNextClickFn<Node<Raw>> = () => {
        return this.onFinishClick();
    }

    constructor(
        private appState: AppStateService,
        private nodeOps: NodeOperations,
        private featureOps: FeatureOperations,
        private languageLoader: LanguageTableLoaderService,
    ) { }

    ngOnInit(): void {
        this.initForms();

        this.nodeFeatures$ = this.appState.select(state => state.ui.language).pipe(
            startWith(observableOf(true)),
            switchMap(() => this.nodeOps.getAvailableFeatures({ sort: [ { attribute: 'id' } ] })),
        );
    }

    private initForms(): void {
        this.fgProperties = new FormControl<NodePropertiesFormData>(null, [
            Validators.required,
            createNestedControlValidator(),
        ]);
        this.fgPropertiesValid$ = createFormValidityTracker(this.fgProperties);

        this.fgPublishing = new FormControl(FG_PUBLISHING_DEFAULT as any, [
            Validators.required,
            createNestedControlValidator(),
        ]);
        this.fgPublishingValid$ = createFormValidityTracker(this.fgPublishing);

        this.fgNodeFeatures = new UntypedFormGroup({
            data: new UntypedFormControl(null),
        });
        this.fgNodeFeaturesValid$ = createFormValidityTracker(this.fgNodeFeatures);
    }

    private getFormData<T>(formGroup: UntypedFormGroup): T {
        return formGroup.value.data || {};
    }

    private async onFinishClick(): Promise<Node<Raw>> {
        try {
            const created = await this.createNode().toPromise();
            await this.setNodeFeatures(created).toPromise();
            await this.setNodeLanguages(created).toPromise();
            return created;
        } catch (error) {
            // We need to catch any errors and emit something to make sure that the wizard closes.
            return null;
        }
    }

    private createNode(): Observable<Node<Raw>> {
        const { description, ...nodeProperties } = this.fgProperties.value;
        const publishingData: NodePublishingPropertiesFormData = this.fgPublishing.value;

        const nodeCreateReq: NodeCreateRequest = {
            node: nodeProperties,
            description,
        };
        if (typeof nodeProperties.inheritedFromId === 'number') {
            // Strange, but we need the ID of the parent's root folder.
            nodeCreateReq.node.masterId = this.appState.now.entity.node[nodeProperties.inheritedFromId].folderId;
        }
        // if (nodeProperties.description) {
        //     nodeCreateReq.description = nodeProperties.description;
        // }
        if (typeof publishingData.urlRenderWayPages === 'number') {
            nodeCreateReq.node.urlRenderWayPages = publishingData.urlRenderWayPages;
        }
        if (typeof publishingData.urlRenderWayFiles === 'number') {
            nodeCreateReq.node.urlRenderWayFiles = publishingData.urlRenderWayFiles;
        }

        return this.nodeOps.addNode(nodeCreateReq);
    }

    private setNodeFeatures(node: Node<Raw>): Observable<Node<Raw>> {
        const nodeFeatures: NodeFeaturesFormData = this.getFormData(this.fgNodeFeatures);
        return this.nodeOps.updateNodeFeatures(node.id, nodeFeatures).pipe(
            tap(() => this.featureOps.getNodeFeatures(node.id)),
            map(() => node),
        );
    }

    private setNodeLanguages(node: Node<Raw>): Observable<Node<Raw>> {
        if (this.selectedLanguages.length === 0) {
            return of(node);
        }

        const nodeLanguages: Language[] = this.languageLoader.getEntitiesByIds(this.selectedLanguages);

        return this.nodeOps.updateNodeLanguages(node.id, nodeLanguages).pipe(
            map(() => node),
        );
    }
}
