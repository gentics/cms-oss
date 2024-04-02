import {
    createFormSaveDisabledTracker,
    detailLoading,
    FormTabHandle,
    hasInstancePermission,
} from '@admin-ui/common';
import {
    BREADCRUMB_RESOLVER,
    ConstructOperations,
    ConstructTableLoaderService,
    EditorTabTrackerService,
    I18nService,
    PackageOperations,
    ResolveBreadcrumbFn,
} from '@admin-ui/core';
import { BaseDetailComponent, ConstructDataService, LanguageDataService } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    OnDestroy,
    OnInit,
    Type,
} from '@angular/core';
import { UntypedFormControl } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { CONTROL_INVALID_VALUE, createNestedControlValidator } from '@gentics/cms-components';
import {
    ConstructUpdateRequest,
    Index,
    Language,
    NormalizableEntityType,
    Raw,
    SingleInstancePermissionType,
    TagPart,
    TagPartType,
    TagTypeBO,
} from '@gentics/cms-models';
import { ModalService } from '@gentics/ui-core';
import { isEqual } from 'lodash-es';
import { NGXLogger } from 'ngx-logger';
import { combineLatest, Observable, of, Subscription } from 'rxjs';
import { delay, map, publishReplay, refCount, repeat, takeUntil, tap } from 'rxjs/operators';
import { ConstructPropertiesMode } from '../construct-properties/construct-properties.component';

export enum ConstructDetailTabs {
    PROPERTIES = 'properties',
    PARTS = 'parts',
}

// List of TagPart entity keys to validate changes on filesystem
const TAG_PART_VALIDATION_PROPERTIES: (keyof TagPart)[] = [
    'globalId',
    'nameI18n',
    'keyword',
    'markupLanguageId',
    'partOrder',
    'hidden',
    'editable',
    'liveEditable',
    'mandatory',
    'type',
    'typeId',
    'id',
    'defaultProperty',
    'hideInEditor',
    'externalEditorUrl',
    'options',
    'overviewSettings',
    'selectSettings',
    'regex',
];

// *************************************************************************************************
/**
 * # ConstructDetailComponent
 * __Note:__ *Construct* is the new term for *TagType*.
 * Display and edit entity construct detail information
 */
// *************************************************************************************************
@Component({
    selector: 'gtx-construct-detail',
    templateUrl: './construct-detail.component.html',
    styleUrls: ['./construct-detail.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConstructDetailComponent
    extends BaseDetailComponent<'construct', ConstructOperations>
    implements OnInit, OnDestroy {

    // tslint:disable-next-line: variable-name
    public readonly ConstructDetailTabs = ConstructDetailTabs;
    // tslint:disable-next-line: variable-name
    public readonly ConstructPropertiesMode = ConstructPropertiesMode;

    public readonly entityIdentifier: NormalizableEntityType = 'construct';

    public activeTabId$: Observable<ConstructDetailTabs>;

    /** current entity value */
    public currentEntity: TagTypeBO<Raw>;
    public currentActiveTabId: ConstructDetailTabs;
    public entityIsClean = true;

    public fgProperties: UntypedFormControl;
    public fgParts: UntypedFormControl;

    public fgPropertiesSaveDisabled$: Observable<boolean>;
    public fgPartsSaveDisabled$: Observable<boolean>
    public supportedLanguages$: Observable<Language[]>;

    get isLoading(): boolean {
        return this.currentEntity == null || this.currentEntity.keyword == null;
    }

    get isLoading$(): Observable<boolean> {
        return this.currentEntity$.pipe(
            map((entity: TagTypeBO<Raw>) => entity == null || !this.currentEntity.keyword),
            publishReplay(1),
            refCount(),
        );
    }

    get activeFormTab(): FormTabHandle {
        return this.tabHandles[this.appState.now.ui.editorTab];
    }

    private tabHandles: Index<ConstructDetailTabs, FormTabHandle> = {} as any;
    private subscriptions: Subscription[] = [];

    constructor(
        logger: NGXLogger,
        route: ActivatedRoute,
        router: Router,
        appState: AppStateService,
        changeDetectorRef: ChangeDetectorRef,
        entityData: ConstructDataService,
        private operations: ConstructOperations,
        private languageData: LanguageDataService,
        private editorTabTracker: EditorTabTrackerService,
        private tableLoader: ConstructTableLoaderService,
        private devtools: PackageOperations,
        private modals: ModalService,
        private i18n: I18nService,
    ) {
        super(
            logger,
            route,
            router,
            appState,
            entityData,
            changeDetectorRef,
        );
    }

    static [BREADCRUMB_RESOLVER]: ResolveBreadcrumbFn = (route, injector) => {
        const appState = injector.get<AppStateService>(AppStateService as Type<AppStateService>);
        const gtxTagType = appState.now.entity.construct[route.params.id];
        return of(gtxTagType ? { title: gtxTagType.name, doNotTranslate: true } : null);
    }

    ngOnInit(): void {
        super.ngOnInit();

        this.initForms();

        // assign values and validation of current entity
        this.currentEntity$.pipe(
            takeUntil(this.stopper.stopper$),
        ).subscribe((currentEntity: TagTypeBO<Raw>) => {
            this.currentEntity = currentEntity;
            this.fgProperties = null;
            this.fgParts = null;
            this.entityIsClean = true;
            this.changeDetectorRef.markForCheck();

            // fill form with entity property values
            this.initForms();
            this.changeDetectorRef.markForCheck();
        });

        // Hacky way to make tabs properly select, as they get stuck on the first value and don't
        // render anything.
        this.activeTabId$ = this.editorTabTracker.trackEditorTab(this.route).pipe(
            map((tabId: ConstructDetailTabs) => !Object.values(ConstructDetailTabs).includes(tabId) ? ConstructDetailTabs.PROPERTIES : tabId),
            tap(id => this.currentActiveTabId = id),
            repeat(1),
            delay(10),
        );

        // get available system languages for i18n-properties
        this.supportedLanguages$ = this.languageData.watchSupportedLanguages();
    }

    ngOnDestroy(): void {
        super.ngOnDestroy();
        this.subscriptions.forEach(s => s.unsubscribe());
    }

    btnSavePropertiesOnClick(): void {
        this.updateEntity();
    }

    async btnSavePartsOnClick(): Promise<void> {
        const syncRes = await this.devtools.getSyncState().toPromise();

        if (!syncRes.enabled) {
            this.updateParts();
            return;
        }

        const currentState = await this.operations.get(this.currentEntity.id).toPromise();
        const hasFSChanges = !isEqual(this.currentEntity.parts, currentState.parts);
        const hasManualChanges = this.hasTagPartChanges(currentState.parts);

        if (!hasFSChanges || !hasManualChanges) {
            this.updateParts();
            return;
        }

        const dialog = await this.modals.dialog({
            title: this.i18n.instant('construct.tagpart_conflict_title', { entityName: this.currentEntity.keyword }),
            body: this.i18n.instant('construct.tagpart_conflict_body'),
            buttons: [
                {
                    label: this.i18n.instant('modal.discard_changes_button'),
                    type: 'warning',
                    returnValue: 'discard',
                },
                {
                    label: this.i18n.instant('construct.overwrite_button'),
                    type: 'alert',
                    returnValue: 'override',
                },
                {
                    label: this.i18n.instant('common.cancel_button'),
                    type: 'secondary',
                    returnValue: false,
                },
            ],
        });
        const res = await dialog.open();

        switch (res) {
            case 'override':
                this.updateParts();
                return;

            case 'discard':
                this.currentEntity = currentState;
                this.initForms();
                this.changeDetectorRef.markForCheck();
                return;

            default:
                return;
        }
    }

    /**
     * Requests changes of user by id to CMS
     */
    async updateProperties(): Promise<void> {
        const payload: ConstructUpdateRequest = {
            nameI18n: this.fgProperties.value.nameI18n,
            descriptionI18n: this.fgProperties.value.descriptionI18n,
            keyword: this.fgProperties.value.keyword,
            icon: this.fgProperties.value.icon,
            externalEditorUrl: this.fgProperties.value.externalEditorUrl,
            mayBeSubtag: this.fgProperties.value.mayBeSubtag,
            mayContainSubtags: this.fgProperties.value.mayContainSubtags,
            categoryId: this.fgProperties.value.categoryId,
            categorySortorder: this.fgProperties.value.categorySortorder,
            autoEnable: this.fgProperties.value.autoEnable,
        };

        if (payload.categoryId == null) {
            payload.categoryId = -1;
        }

        return this.operations.update(this.currentEntity.id, payload).pipe(
            detailLoading(this.appState),
            tap((updatedEntity: TagTypeBO<Raw>) => {
                this.currentEntity = updatedEntity;
                this.entityIsClean = true;
                this.entityData.reloadEntities();
                this.tableLoader.reload();
                this.initForms();
            }),
            map(() => undefined),
        ).toPromise();
    }

    async updateEntity(): Promise<void> {
        return this.activeFormTab.save();
    }

    /**
     * Requests changes of user by id to CMS
     */
    async updateParts(): Promise<void> {
        const normalizedParts = (this.fgParts.value as TagPart[]).map(part => {
            // Regexes are saved as `int` in the DB, because they reference entries in the regex table.
            // The rest model includes this info inline for easier usage.
            // However, setting it to `null` will not do in this case, because the backend thinks this is
            // a partial update and therefore ignores the value.
            // When the value is `null`, we instead post it with a regex of ID 0 to clear it in the backend.
            if ('regex' in part) {
                part.regex = part.regex || {
                    id: 0,
                } as any;
            }
            return part;
        });
        const payload: ConstructUpdateRequest = {
            parts: normalizedParts,
        };

        return this.operations.update(this.currentEntity.id, payload).pipe(
            detailLoading(this.appState),
            tap((updatedEntity: TagTypeBO<Raw>) => {
                this.currentEntity = updatedEntity;
                this.entityIsClean = true;
                this.entityData.reloadEntities();
                this.initForms();
            }),
            map(() => undefined),
        ).toPromise();
    }

    private initForms(): void {
        if (!this.currentEntity) {
            return;
        }
        this.initPropertiesForm();
        this.initPartsForm();

        this.tabHandles = {
            [ConstructDetailTabs.PROPERTIES]: {
                isDirty: () => this.fgProperties.dirty,
                isValid: () => this.fgProperties.valid,
                save: (): Promise<void> => this.updateProperties(),
                reset: (): Promise<void> => Promise.resolve(this.initPropertiesForm()),
            },
            [ConstructDetailTabs.PARTS]: {
                isDirty: () => this.fgParts.dirty,
                isValid: () => this.fgParts.valid,
                save: () => this.updateParts(),
                reset: () => Promise.resolve(this.initPartsForm()),
            },
        };
    }

    private initPropertiesForm(): void {
        if (this.fgProperties) {
            this.fgProperties.setValue({ ...this.currentEntity });
            this.fgProperties.markAsPristine();
            return;
        }

        this.fgProperties = new UntypedFormControl({ ...this.currentEntity }, createNestedControlValidator());
        this.fgPropertiesSaveDisabled$ = combineLatest([
            this.currentEntity$.pipe(
                map(item => hasInstancePermission(item, SingleInstancePermissionType.EDIT)),
            ),
            createFormSaveDisabledTracker(this.fgProperties),
        ]).pipe(
            map(([hasPermission, formInvalid]) => !hasPermission || formInvalid),
        );

        this.fgProperties.markAsPristine();
    }

    private initPartsForm(): void {
        /*
         * We need to normalize the parts from the API before we set the into the properties.
         * Some properties are only available with a certain type.
         * These might be omited from the backend response (instead of sending `null`).
         * The properties will emit a change with this property set to `null` however,
         * resulting in the form to be marked as changed/dirty which isn't what we want.
         * Therefore, add the property initially with a `null` value and it's fine.
         */
        const normalizedParts: TagPart[] = (this.currentEntity.parts || []).map(rawPart => {
            const { markupLanguageId, regex, selectSettings, overviewSettings, ...part } = rawPart;

            if (part.typeId === TagPartType.Text) {
                return {
                    ...part,
                    defaultProperty: part.defaultProperty || null,
                    regex: regex || null,
                };
            }

            if (part.typeId === TagPartType.HtmlLong) {
                return {
                    ...part,
                    defaultProperty: part.defaultProperty || null,
                    regex: regex || null,
                    markupLanguageId: markupLanguageId || null,
                };
            }

            if (part.typeId === TagPartType.SelectMultiple || part.typeId === TagPartType.SelectSingle) {
                return {
                    ...part,
                    defaultProperty: part.defaultProperty || null,
                    selectSettings: selectSettings || null,
                };
            }

            if (part.typeId === TagPartType.Overview) {
                return {
                    ...part,
                    defaultProperty: part.defaultProperty || null,
                    overviewSettings: overviewSettings || null,
                };
            }

            return {
                ...part,
                defaultProperty: part.defaultProperty || null,
            };
        });

        if (this.fgParts) {
            this.fgParts.setValue(normalizedParts);
            this.fgParts.markAsPristine();
            return;
        }

        this.fgParts = new UntypedFormControl(normalizedParts, (control) => {
            if (control == null || control.value == null) {
                return { null: true };
            }
            if (control.value === CONTROL_INVALID_VALUE) {
                return { nestedError: true };
            }
            if (!Array.isArray(control.value)) {
                return { notArray: true };
            }
            const missingArray: number[] = [];
            const invalidArray: number[] = [];

            control.value.forEach((partValue, index) => {
                if (partValue == null) {
                    missingArray.push(index);
                } else if (partValue === CONTROL_INVALID_VALUE) {
                    invalidArray.push(index);
                }
            });

            if (missingArray.length > 0) {
                return { missingParts: missingArray };
            }
            if (invalidArray.length > 0) {
                return { invalidParts: invalidArray };
            }

            return null;
        });

        this.fgPartsSaveDisabled$ = combineLatest([
            this.currentEntity$.pipe(
                map(item => hasInstancePermission(item, SingleInstancePermissionType.EDIT)),
            ),
            createFormSaveDisabledTracker(this.fgParts),
        ]).pipe(
            map(([hasPermission, formInvalid]) => !hasPermission || formInvalid),
        );

        this.fgParts.markAsPristine();
    }

    private hasTagPartChanges(parts: TagPart[]): boolean {
        if (this.fgParts.value.length !== parts.length) {
            return true;
        }
        for (let i = 0; i < this.fgParts.value.length; i++) {
            for (const key of TAG_PART_VALIDATION_PROPERTIES) {
                const savedTagPart = parts[i][key];
                const formValue = this.fgParts.value[i][key];
                if ((formValue && !savedTagPart) || (!formValue && savedTagPart)) {
                    return true;
                } else if (formValue && savedTagPart && !isEqual(formValue, savedTagPart)) {
                    return true;
                }
            }
        }
        return false;
    }

}
