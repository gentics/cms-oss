import { ContentRepositoryFragmentTagmapEntryOperations, ContentRepositoryTagmapEntryOperations } from '@admin-ui/core';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import {
    Normalized,
    TagmapEntry,
    TagmapEntryBO,
    TagmapEntryCreateRequest,
    TagmapEntryParentType,
    TagmapEntryUpdateRequest,
} from '@gentics/cms-models';
import { BaseModal } from '@gentics/ui-core';

export enum CreateTagmapEntryModalComponentMode {
    CREATE = 'create',
    UPDATE = 'update',
}

export enum TagmapEntryDisplayFields {
    ALL = 'all',
    SQL = 'sql',
    MESH = 'mesh',
}

@Component({
    selector: 'gtx-create-update-tagmap-entry-modal',
    templateUrl: './create-update-tagmapentry-modal.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class CreateUpdateTagmapEntryModalComponent extends BaseModal<TagmapEntryBO<Normalized>> implements OnInit {

    public readonly CreateTagmapEntryModalComponentMode = CreateTagmapEntryModalComponentMode;

    @Input()
    public value: TagmapEntryBO<Normalized>;

    @Input({ required: true })
    public mode: CreateTagmapEntryModalComponentMode;

    @Input({ required: true })
    public displayFields: TagmapEntryDisplayFields;

    @Input({ required: true })
    public parentType: TagmapEntryParentType;

    @Input({ required: true })
    public parentId: string;

    @Input()
    public tagmapId?: string | number;

    @Input()
    public reserved = false;

    /** form instance */
    public form: FormControl<TagmapEntryCreateRequest | TagmapEntryUpdateRequest>;
    public loading = false;

    constructor(
        private changeDetector: ChangeDetectorRef,
        private crOperations: ContentRepositoryTagmapEntryOperations,
        private fragmentOperations: ContentRepositoryFragmentTagmapEntryOperations,
    ) {
        super();
    }

    ngOnInit(): void {
        const payload: TagmapEntryCreateRequest | TagmapEntryUpdateRequest = {
            tagname: this.value?.tagname ?? '',
            mapname: this.value?.mapname ?? '',
            object: this.value?.objType ?? this.value?.object ?? null,
            objType: this.value?.objType ?? this.value?.object ?? null,
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
            noIndex: this.value?.noIndex ?? null,
            elasticsearch: this.value?.elasticsearch ?? null,
            micronodeFilter: this.value?.micronodeFilter ?? '',
            fragmentName: this.value?.fragmentName ?? '',
        };
        // instantiate form
        this.form = new FormControl<TagmapEntryCreateRequest | TagmapEntryUpdateRequest>(payload);
    }

    /**
     * If user clicks to create/update a tagmapEntry
     */
    async confirmButtonClicked(): Promise<void> {
        if (this.mode === CreateTagmapEntryModalComponentMode.CREATE) {
            const created = await this.createEntity();
            this.closeFn(created);
        } else {
            const updated = await this.updateEntity();
            this.closeFn(updated);
        }
    }

    /**
     * If user clicks to update an existing tagmapEntry
     */
    buttonUpdateEntityClicked(): void {
        this.updateEntity()
            .then(tagmapEntryUpdated => this.closeFn(tagmapEntryUpdated));
    }

    private getFormValue(): TagmapEntry {
        const formData: TagmapEntryCreateRequest | TagmapEntryUpdateRequest = this.form.value;

        if (formData?.elasticsearch) {
            if (typeof formData.elasticsearch === 'string') {
                try {
                    formData.elasticsearch = JSON.parse(formData.elasticsearch);
                } catch (err) {
                    formData.elasticsearch = this.value?.elasticsearch;
                }
            }
        }

        return formData as any;
    }

    private async createEntity(): Promise<TagmapEntryBO<Normalized>> {
        if (!this.parentId) {
            throw new Error('Missing input: parentId');
        }

        if (this.parentType === 'contentRepositoryFragment') {
            return this.fragmentOperations.create(this.parentId, this.getFormValue() as any).toPromise();
        }

        // adapt for REST API specs
        const payload = this.getFormValue();
        payload.object = payload.objType;
        delete payload.objType;

        this.form.disable();
        this.loading = true;

        try {
            return await this.crOperations.create(this.parentId, payload as any).toPromise();
        } catch (err) {
            this.loading = false;
            this.form.enable();
            this.changeDetector.markForCheck();
        }
    }

    private async updateEntity(): Promise<TagmapEntryBO<Normalized>> {
        if (!this.parentId) {
            throw new Error('Missing input: parentId');
        }
        if (!this.tagmapId) {
            throw new Error('Missing input: tagmapId');
        }

        if (this.parentType === 'contentRepositoryFragment') {
            return this.fragmentOperations.update(this.parentId, this.tagmapId, this.getFormValue()).toPromise();
        }

        // adapt for REST API specs
        const payload = this.getFormValue();
        payload.object = payload.objType;
        delete payload.objType;

        this.form.disable();
        this.loading = true;

        try {
            return await this.crOperations.update(this.parentId, this.tagmapId, payload).toPromise();
        } catch (err) {
            this.loading = false;
            this.form.enable();
            this.changeDetector.markForCheck();
        }
    }

}
