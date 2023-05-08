import { ContentRepositoryFragmentTagmapEntryOperations, ContentRepositoryTagmapEntryOperations } from '@admin-ui/core';
import { ChangeDetectionStrategy, Component, Input, OnInit } from '@angular/core';
import { UntypedFormControl } from '@angular/forms';
import { Normalized, TagmapEntryBO, TagmapEntryCreateRequest, TagmapEntryParentType, TagmapEntryUpdateRequest } from '@gentics/cms-models';
import { BaseModal, IModalDialog } from '@gentics/ui-core';

export enum CreateTagmapEntryModalComponentMode {
    CREATE = 'create',
    UPDATE = 'update',
};

export enum TagmapEntryDisplayFields {
    ALL = 'all',
    SQL = 'sql',
    MESH = 'mesh',
};

@Component({
    selector: 'gtx-create-update-tagmap-entry-modal',
    templateUrl: './create-update-tagmapentry-modal.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CreateUpdateTagmapEntryModalComponent extends BaseModal<TagmapEntryBO<Normalized>> implements OnInit {

    @Input()
    value: TagmapEntryBO<Normalized>;

    @Input()
    mode: CreateTagmapEntryModalComponentMode;

    @Input()
    displayFields: TagmapEntryDisplayFields;

    @Input()
    parentType: TagmapEntryParentType;

    @Input()
    parentId: string;

    @Input()
    tagmapId?: string;

    /** form instance */
    form: UntypedFormControl;

    isValid: boolean;

    constructor(
        private crOperations: ContentRepositoryTagmapEntryOperations,
        private fragmentOperations: ContentRepositoryFragmentTagmapEntryOperations,
    ) {
        super();
    }

    ngOnInit(): void {
        const payload: TagmapEntryCreateRequest | TagmapEntryUpdateRequest = {
            tagname: this.value?.tagname ?? '',
            mapname: this.value?.mapname ?? '',
            object: this.value?.objType ?? null,
            objType: this.value?.objType ?? null,
            attributeType: this.value?.attributeType ?? null,
            targetType: this.value?.targetType ?? null,
            multivalue: this.value?.multivalue ?? null,
            optimized: this.value?.optimized ?? null,
            filesystem: this.value?.filesystem ?? null,
            foreignlinkAttribute: this.value?.foreignlinkAttribute ?? '',
            foreignlinkAttributeRule: this.value?.foreignlinkAttributeRule ?? '',
            category: this.value?.category ?? '',
            segmentfield: this.value?.segmentfield ?? null,
            displayfield: this.value?.displayfield ?? null,
            urlfield: this.value?.urlfield ?? null,
            elasticsearch: this.value?.elasticsearch ?? null,
            micronodeFilter: this.value?.micronodeFilter ?? '',
            fragmentName: this.value?.fragmentName ?? '',
        };
        // instantiate form
        this.form = new UntypedFormControl(payload);
    }

    /**
     * If user clicks to create a new tagmapEntry
     */
    buttonCreateEntityClicked(): void {
        this.createEntity()
            .then(tagmapEntryCreated => this.closeFn(tagmapEntryCreated));
    }

    /**
     * If user clicks to update an existing tagmapEntry
     */
     buttonUpdateEntityClicked(): void {
        this.updateEntity()
            .then(tagmapEntryUpdated => this.closeFn(tagmapEntryUpdated));
    }

    private createEntity(): Promise<TagmapEntryBO<Normalized>> {
        if (!this.parentId) {
            throw new Error('Missing input: contentRepositoryId');
        }

        if (this.parentType === 'contentRepositoryFragment') {
            return this.fragmentOperations.create(this.parentId, this.form.value).toPromise();
        }

        // adapt for REST API specs
        const payload = {...this.form.value};
        payload.object = payload.objType;
        delete payload.objType;

        return this.crOperations.create(this.parentId, payload).toPromise();
    }

    private updateEntity(): Promise<TagmapEntryBO<Normalized>> {
        if (!this.parentId) {
            throw new Error('Missing input: contentRepositoryId');
        }
        if (!this.tagmapId) {
            throw new Error('Missing input: tagmapId');
        }

        if (this.parentType === 'contentRepositoryFragment') {
            return this.fragmentOperations.update(this.parentId, this.tagmapId, this.form.value).toPromise();
        }

        // adapt for REST API specs
        const payload = {...this.form.value};
        payload.object = payload.objType;
        delete payload.objType;

        return this.crOperations.update(this.parentId, this.tagmapId, payload).toPromise();
    }

}
