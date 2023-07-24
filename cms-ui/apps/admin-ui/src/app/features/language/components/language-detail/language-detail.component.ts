import { FormGroupTabHandle, FormTabHandle, LanguageDetailTabs } from '@admin-ui/common';
import { detailLoading } from '@admin-ui/common/utils/rxjs-loading-operators/detail-loading.operator';
import { EditorTabTrackerService, LanguageOperations, LanguageTableLoaderService, PermissionsService } from '@admin-ui/core/providers';
import { BaseDetailComponent, LanguageDataService } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state/providers/app-state/app-state.service';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AccessControlledType, GcmsPermission, Index, Language, NormalizableEntityType, TypePermissions } from '@gentics/cms-models';
import { NGXLogger } from 'ngx-logger';
import { Observable } from 'rxjs';
import { map, takeUntil, tap } from 'rxjs/operators';

// *************************************************************************************************
/**
 * # LanguageDetailComponent
 * Display and edit entity language detail information
 */
// *************************************************************************************************
@Component({
    selector: 'gtx-language-detail',
    templateUrl: './language-detail.component.html',
    styleUrls: [ './language-detail.component.scss' ],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LanguageDetailComponent extends BaseDetailComponent<'language', LanguageOperations> implements OnInit {

    public readonly LanguageDetailTabs = LanguageDetailTabs;

    entityIdentifier: NormalizableEntityType = 'language';

    /** current entity value */
    currentEntity: Language;

    /** form of tab 'Properties' */
    fgProperties: UntypedFormGroup;

    get activeFormTab(): FormTabHandle {
        return this.tabHandles[this.appState.now.ui.editorTab];
    }

    get isLoading(): boolean {
        return this.currentEntity == null || !this.currentEntity.name || this.currentEntity.name === '';
    }

    /** TRUE if logged-in user is allowed to read entity `content` */
    permissionContentRead$: Observable<boolean>;

    activeTabId$: Observable<string>;

    private tabHandles: Index<LanguageDetailTabs, FormTabHandle>;

    constructor(
        logger: NGXLogger,
        route: ActivatedRoute,
        router: Router,
        appState: AppStateService,
        languageData: LanguageDataService,
        changeDetectorRef: ChangeDetectorRef,
        private languageOperations: LanguageOperations,
        private permissionsService: PermissionsService,
        private editorTabTracker: EditorTabTrackerService,
        private tableLoader: LanguageTableLoaderService,
    ) {
        super(
            logger,
            route,
            router,
            appState,
            languageData,
            changeDetectorRef,
        );
    }

    ngOnInit(): void {
        super.ngOnInit();

        // init form
        this.fgPropertiesInit();

        // assign values and validation of current entity
        this.currentEntity$.pipe(
            takeUntil(this.stopper.stopper$),
        ).subscribe((currentEntity: Language) => {
            this.currentEntity = currentEntity;
            // fill form with entity property values
            this.fgPropertiesUpdate(currentEntity);
            this.changeDetectorRef.markForCheck();
        });

        this.permissionContentRead$ = this.permissionsService.getPermissions(AccessControlledType.LANGUAGE_ADMIN).pipe(
            map((typePermissions: TypePermissions) => typePermissions.hasPermission(GcmsPermission.READ)),
        );

        this.activeTabId$ = this.editorTabTracker.trackEditorTab(this.route);
    }

    /**
     * Requests changes of language by id to CMS
     */
    private updateLanguage(): Promise<Language> {
        // assemble payload with conditional properties
        const language: Language = {
            id: this.currentEntity.id,
            ...(this.fgProperties.value.name && { name: this.fgProperties.value.name }),
            ...(this.fgProperties.value.code && { code: this.fgProperties.value.code }),
        };

        this.currentEntity = language;

        return this.languageOperations.updateLanguage(language.id, language).pipe(
            detailLoading(this.appState),
            tap(() => {
                this.fgProperties.markAsPristine();
                this.tableLoader.reload();
            }),
        ).toPromise();
    }

    btnSavePropertiesOnClick(): void {
        this.updateLanguage();
    }

    /**
     * Initialize form 'Properties'
     */
    protected fgPropertiesInit(): void {
        this.fgProperties = new UntypedFormGroup({
            name: new UntypedFormControl(''),
            code: new UntypedFormControl(''),
        });

        this.tabHandles = {
            [LanguageDetailTabs.PROPERTIES]: new FormGroupTabHandle(this.fgProperties, {
                save: () => this.updateLanguage().then(() => {}),
            }),
        };
    }

    /**
     * Set new value of form 'Properties'
     */
    protected fgPropertiesUpdate(language: Language): void {
        this.fgProperties.setValue({
            name: language.name,
            code: language.code,
        });
        this.fgProperties.markAsPristine();
    }
}
