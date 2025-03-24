import { EditableEntity } from '@admin-ui/common';
import { BaseEntityEditorComponent } from '@admin-ui/core/components';
import { ConstructCategoryHandlerService, LanguageHandlerService } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ConstructCategory, Language } from '@gentics/cms-models';
import { Observable } from 'rxjs';
import { ConstructCategoryTableLoaderService } from '../../providers';
import { ConstructCategoryPropertiesMode } from '../construct-category-properties/construct-category-properties.component';

@Component({
    selector: 'gtx-construct-category-editor',
    templateUrl: './construct-category-editor.component.html',
    styleUrls: ['./construct-category-editor.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConstructCategoryEditorComponent extends BaseEntityEditorComponent<EditableEntity.CONSTRUCT_CATEGORY> implements OnInit {

    public readonly ConstructCategoryPropertiesMode = ConstructCategoryPropertiesMode;

    public fgProperties: FormControl<ConstructCategory>;

    public supportedLanguages$: Observable<Language[]>;

    constructor(
        changeDetector: ChangeDetectorRef,
        route: ActivatedRoute,
        router: Router,
        appState: AppStateService,
        handler: ConstructCategoryHandlerService,
        protected languageHandler: LanguageHandlerService,
        protected tableLoader: ConstructCategoryTableLoaderService,
    ) {
        super(
            EditableEntity.CONSTRUCT_CATEGORY,
            changeDetector,
            route,
            router,
            appState,
            handler,
        );
    }

    override ngOnInit(): void {
        super.ngOnInit();

        this.supportedLanguages$ = this.languageHandler.getSupportedLanguages();
    }

    override onEntityUpdate(): void {
        this.tableLoader.reload();
    }

    protected initializeTabHandles(): void {
        this.fgProperties = new FormControl(this.entity);
        this.tabHandles[this.Tabs.PROPERTIES] = this.createTabHandle(this.fgProperties);
    }

    protected onEntityChange(): void {
        if (this.fgProperties) {
            this.fgProperties.reset(this.entity);
        }
    }
}
