import { createFormValidityTracker, WizardStepNextClickFn } from '@admin-ui/common';
import { FeatureOperations, LanguageTableLoaderService, NodeOperations } from '@admin-ui/core';
import { WizardComponent } from '@admin-ui/shared';
import { Wizard } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import { AfterViewInit, Component, OnInit, ViewChild } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { Language, Node, NodeCreateRequest, NodeFeatureModel, Raw } from '@gentics/cms-models';
import { Observable, of as observableOf, of } from 'rxjs';
import { catchError, map, startWith, switchMap, tap } from 'rxjs/operators';
import { NodePropertiesFormData } from '..';
import { NodeFeaturesFormData } from '../node-features/node-features.component';
import { NodePropertiesComponent } from '../node-properties/node-properties.component';
import { NodePublishingPropertiesFormData } from '../node-publishing-properties/node-publishing-properties.component';

const FG_PROPERTIES_DEFAULT: Partial<NodePropertiesFormData> = {
};

const FG_PUBLISHING_DEFAULT: Partial<NodePublishingPropertiesFormData> = {
    urlRenderWayFiles: 0,
    urlRenderWayPages: 0,
};

@Component({
    selector: 'gtx-create-node-wizard',
    templateUrl: './create-node-wizard.component.html',
    styleUrls: ['./create-node-wizard.component.scss'],
})
export class CreateNodeWizardComponent implements OnInit, AfterViewInit, Wizard<Node<Raw>> {

    @ViewChild(WizardComponent, { static: true })
    wizard: WizardComponent;

    @ViewChild(NodePropertiesComponent)
    nodeProperties: NodePropertiesComponent;

    /** Form data of tab 'Properties' */
    fgProperties: UntypedFormGroup;

    isChildNode: boolean;

    /** form of tab 'Publishing' */
    fgPublishing: UntypedFormGroup;

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

    ngAfterViewInit(): void {
        // We must set the default values here, because if we do it in initForms() the form
        // will not emit an invalid value, because of the missing properties.
        this.fgProperties.patchValue({ data: FG_PROPERTIES_DEFAULT });
        this.fgPublishing.patchValue({ data: FG_PUBLISHING_DEFAULT });

        this.fgProperties.get('data').valueChanges
            .subscribe(() => this.isChildNode = this.nodeProperties.isChildNode);
    }

    onPublishingStepActivate(): void {
        const publishingProps: NodePublishingPropertiesFormData = this.fgPublishing.value.data;
        if (!publishingProps.fileSystemBinaryDir && !publishingProps.fileSystemPagesDir) {
            publishingProps.fileSystemBinaryDir = '';
            publishingProps.fileSystemPagesDir = '';
            this.fgPublishing.patchValue({ data: publishingProps });
        }
    }

    private initForms(): void {
        this.fgProperties = new UntypedFormGroup({
            data: new UntypedFormControl(null, Validators.required),
        });
        this.fgPropertiesValid$ = createFormValidityTracker(this.fgProperties);

        this.fgPublishing = new UntypedFormGroup({
            data: new UntypedFormControl({}, Validators.required),
        });
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
        const nodeProperties: NodePropertiesFormData = this.getFormData(this.fgProperties);
        const publishingData: NodePublishingPropertiesFormData = this.getFormData(this.fgPublishing);

        const nodeCreateReq: NodeCreateRequest = {
            node: {
                name: nodeProperties.name,
                https: !!nodeProperties.https,
                host: nodeProperties.hostname,
                meshPreviewUrl: nodeProperties.meshPreviewUrl,
                insecurePreviewUrl: nodeProperties.insecurePreviewUrl,
                disablePublish: !!publishingData.disableUpdates,
                publishFs: !!publishingData.fileSystem,
                publishFsPages: !!publishingData.fileSystemPages,
                publishDir: publishingData.fileSystemPagesDir || undefined,
                publishFsFiles: !!publishingData.fileSystemFiles,
                binaryPublishDir: publishingData.fileSystemBinaryDir || undefined,
                publishContentMap: !!publishingData.contentRepository,
                publishContentMapPages: !!publishingData.contentRepositoryPages,
                publishContentMapFiles: !!publishingData.contentRepositoryFiles,
                publishContentMapFolders: !!publishingData.contentRepositoryFolders,
                pubDirSegment: !!nodeProperties.pubDirSegment,
                omitPageExtension: !!publishingData.omitPageExtension,
                pageLanguageCode: publishingData.pageLanguageCode || null,
            },
        };
        if (typeof nodeProperties.inheritedFromId === 'number') {
            // Strange, but we need the ID of the parent's root folder.
            nodeCreateReq.node.masterId = this.appState.now.entity.node[nodeProperties.inheritedFromId].folderId;
        }
        if (nodeProperties.description) {
            nodeCreateReq.description = nodeProperties.description;
        }
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
