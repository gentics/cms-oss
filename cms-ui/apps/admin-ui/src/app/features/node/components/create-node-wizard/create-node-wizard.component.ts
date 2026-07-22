import { createFormValidityTracker, WizardStepNextClickFn } from '@admin-ui/common';
import { FeatureOperations, NodeHandlerService, NodeOperations } from '@admin-ui/core';
import { LanguageTableComponent, Wizard, WizardComponent } from '@admin-ui/shared';
import { AppStateService } from '../../../../state/providers/app-state/app-state.service';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit, ViewChild } from '@angular/core';
import { FormControl, Validators } from '@angular/forms';
import { createNestedControlValidator, I18nService } from '@gentics/cms-components';
import { FormTypeConfiguration, Language, Node, NodeCreateRequest, NodeFeature, NodeFeatureModel, NodeUrlMode, Raw } from '@gentics/cms-models';
import { Observable, of as observableOf, of } from 'rxjs';
import { map, startWith, switchMap, tap } from 'rxjs/operators';
import { NodeFeaturesFormData } from '../node-features/node-features.component';
import { NodePropertiesComponent, NodePropertiesFormData, NodePropertiesMode } from '../node-properties/node-properties.component';
import { NodePublishingPropertiesFormData } from '../node-publishing-properties/node-publishing-properties.component';
import { PickListItem } from '@gentics/ui-core';

const FG_PUBLISHING_DEFAULT: Partial<NodePublishingPropertiesFormData> = {
    urlRenderWayFiles: NodeUrlMode.AUTOMATIC,
    urlRenderWayPages: NodeUrlMode.AUTOMATIC,
};

@Component({
    selector: 'gtx-create-node-wizard',
    templateUrl: './create-node-wizard.component.html',
    styleUrls: ['./create-node-wizard.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class CreateNodeWizardComponent implements OnInit, Wizard<Node<Raw>> {

    public readonly NodePropertiesMode = NodePropertiesMode;

    @ViewChild(WizardComponent, { static: true })
    wizard: WizardComponent;

    @ViewChild(NodePropertiesComponent)
    nodeProperties: NodePropertiesComponent;

    @ViewChild(LanguageTableComponent)
    public langTable: LanguageTableComponent;

    /** Form data of tab 'Properties' */
    fgProperties: FormControl<NodePropertiesFormData>;

    isChildNode: boolean;

    /** form of tab 'Publishing' */
    fgPublishing: FormControl<NodePublishingPropertiesFormData>;

    /** form of tab 'Node Features' */
    fgNodeFeatures: FormControl<NodeFeaturesFormData>;

    fgFormTypes = new FormControl<PickListItem[]>([]);

    isFormsEnabled = false;

    public allFormTypes: FormTypeConfiguration[] = [];

    public allFormTypesAsPickListItems: PickListItem[] = [];

    nodeFeatures$: Observable<NodeFeatureModel[]>;

    selectedLanguages: string[] = [];

    fgPropertiesValid$: Observable<boolean>;
    fgPublishingValid$: Observable<boolean>;
    fgNodeFeaturesValid$: Observable<boolean>;

    finishClickAction: WizardStepNextClickFn<Node<Raw>> = () => {
        return this.onFinishClick();
    };

    setChildNodeAction: WizardStepNextClickFn<void> = () => {
        this.setChildNode();
        return Promise.resolve();
    };

    constructor(
        private changeDetector: ChangeDetectorRef,
        private appState: AppStateService,
        private nodeOps: NodeOperations,
        private featureOps: FeatureOperations,
        private nodeHandler: NodeHandlerService,
        private i18n: I18nService,
    ) { }

    ngOnInit(): void {
        this.initForms();

        this.nodeFeatures$ = this.appState.select((state) => state.ui.language).pipe(
            startWith(observableOf(true)),
            switchMap(() => this.nodeOps.getAvailableFeatures({ sort: [{ attribute: 'id' }] })),
        );

        this.nodeHandler.listAllFormConfigurations().subscribe((items) => {
            this.allFormTypes = items;
            this.allFormTypesAsPickListItems = items.map((item) => this.mapFormTypeToPickListItem(item));
            this.changeDetector.markForCheck();
        });

        this.fgNodeFeatures.valueChanges.subscribe((value) => {
            this.isFormsEnabled = value?.[NodeFeature.FORMS] ?? false;
            this.changeDetector.markForCheck();
        });
    }

    mapFormTypeToPickListItem(form: FormTypeConfiguration): PickListItem {
        return {
            id: form.type,
            label: form.nameI18n[this.i18n.getCurrentLanguage()],
        };
    }

    private setChildNode(): void {
        this.isChildNode = typeof this.fgProperties.value.inheritedFromId === 'number';
    }

    private initForms(): void {
        this.fgProperties = new FormControl<NodePropertiesFormData>(null, [
            Validators.required,
            createNestedControlValidator(),
        ]);
        this.fgPropertiesValid$ = createFormValidityTracker(this.fgProperties);

        this.fgPublishing = new FormControl<NodePublishingPropertiesFormData>(FG_PUBLISHING_DEFAULT as any, [
            Validators.required,
            createNestedControlValidator(),
        ]);
        this.fgPublishingValid$ = createFormValidityTracker(this.fgPublishing);

        this.fgNodeFeatures = new FormControl<NodeFeaturesFormData>({});
        this.fgNodeFeaturesValid$ = createFormValidityTracker(this.fgNodeFeatures);
    }

    private async onFinishClick(): Promise<Node<Raw>> {
        const created = await this.createNode().toPromise();

        try {
            await this.setNodeFeatures(created).toPromise();
        } catch (error) {
            // Ignored, worst case the user has to update the node again later
        }
        try {
            await this.setNodeLanguages(created).toPromise();
        } catch (error) {
            // Same here
        }
        if (this.isFormsEnabled && this.fgFormTypes.value?.length > 0) {
            try {
                const formTypes = this.fgFormTypes.value
                    .map((item) => this.allFormTypes.find((f) => f.type === String(item.id)))
                    .filter(Boolean);

                await this.nodeHandler
                    .updateFormConfigurations(created.id, formTypes, [])
                    .toPromise();
            } catch (error) {
                // Same here
            }
        }

        return created;
    }

    private createNode(): Observable<Node<Raw>> {
        const { description, ...nodeProperties } = this.fgProperties.value;
        const publishingData: NodePublishingPropertiesFormData = this.fgPublishing.value;

        const nodeCreateReq: NodeCreateRequest = {
            node: {
                ...nodeProperties,
                ...publishingData,
            },
            description,
        };
        if (typeof nodeProperties.inheritedFromId === 'number') {
            // Strange, but we need the ID of the parent's root folder.
            nodeCreateReq.node.masterId = this.appState.now.entity.node[nodeProperties.inheritedFromId].folderId;
        }

        return this.nodeOps.addNode(nodeCreateReq);
    }

    private setNodeFeatures(node: Node<Raw>): Observable<Node<Raw>> {
        const featureData: NodeFeaturesFormData = this.fgNodeFeatures.value;
        return this.nodeOps.updateNodeFeatures(node.id, featureData || {}, true).pipe(
            tap(() => this.featureOps.getNodeFeatures(node.id)),
            map(() => node),
        );
    }

    private setNodeLanguages(node: Node<Raw>): Observable<Node<Raw>> {
        if (this.selectedLanguages.length === 0) {
            return of(node);
        }

        const nodeLanguages: Language[] = this.langTable.getSelectedEntities();

        return this.nodeOps.updateNodeLanguages(node.id, nodeLanguages, true).pipe(
            map(() => node),
        );
    }
}
