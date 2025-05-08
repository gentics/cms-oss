import { EditableEntity } from '@admin-ui/common';
import { LanguageHandlerService, LanguageTableLoaderService } from '@admin-ui/core';
import { BaseEntityEditorComponent } from '@admin-ui/core/components';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { FormControl } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Language } from '@gentics/cms-models';

@Component({
    selector: 'gtx-language-editor',
    templateUrl: './language-editor.component.html',
    styleUrls: ['./language-editor.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class LanguageEditorComponent extends BaseEntityEditorComponent<EditableEntity.LANGUAGE> {

    public fgProperties: FormControl<Language>;

    constructor(
        changeDetector: ChangeDetectorRef,
        route: ActivatedRoute,
        router: Router,
        appState: AppStateService,
        handler: LanguageHandlerService,
        protected tableLoader: LanguageTableLoaderService,
    ) {
        super(
            EditableEntity.LANGUAGE,
            changeDetector,
            route,
            router,
            appState,
            handler,
        );
    }

    protected initializeTabHandles(): void {
        this.fgProperties = new FormControl(this.entity);
        this.tabHandles[this.Tabs.PROPERTIES] = this.createTabHandle(this.fgProperties);
    }

    protected onEntityChange(): void {
        if (this.fgProperties) {
            this.fgProperties.setValue(this.entity);
            this.fgProperties.markAsPristine();
        }
    }

    override onEntityUpdate(): void {
        this.tableLoader.reload();
    }
}
