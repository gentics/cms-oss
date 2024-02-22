import { EditableEntity, FormGroupTabHandle, discard } from '@admin-ui/common';
import { ConstructHandlerService, ConstructTableLoaderService, LanguageHandlerService } from '@admin-ui/core';
import { BaseEntityEditorComponent } from '@admin-ui/core/components';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { CONTROL_INVALID_VALUE } from '@gentics/cms-components';
import { ConstructUpdateRequest, Language, Raw, TagPart, TagPartType, TagType } from '@gentics/cms-models';
import { Observable } from 'rxjs';
import { ConstructPropertiesMode } from '../construct-properties/construct-properties.component';

function normalizeTagPart(rawPart: TagPart<Raw>): TagPart<Raw> {
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
}

function tagPartValidator(parts: typeof CONTROL_INVALID_VALUE | ((TagPart | typeof CONTROL_INVALID_VALUE)[])): any {
    if (parts == null) {
        return { null: true };
    }
    if (parts === CONTROL_INVALID_VALUE) {
        return { nestedError: true };
    }
    if (!Array.isArray(parts)) {
        return { notArray: true };
    }
    const missingArray: number[] = [];
    const invalidArray: number[] = [];

    parts.forEach((partValue, index) => {
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
}

@Component({
    selector: 'gtx-construct-editor',
    templateUrl: './construct-editor.component.html',
    styleUrls: ['./construct-editor.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConstructEditorComponent extends BaseEntityEditorComponent<EditableEntity.CONSTRUCT> implements OnInit {

    public readonly ConstructPropertiesMode = ConstructPropertiesMode;

    public fgProperties: FormControl<TagType>;
    public fgParts: FormControl<TagPart[]>;

    public supportedLanguages$: Observable<Language[]>;

    constructor(
        changeDetector: ChangeDetectorRef,
        route: ActivatedRoute,
        router: Router,
        appState: AppStateService,
        handler: ConstructHandlerService,
        protected languageHandler: LanguageHandlerService,
        protected tableLoader: ConstructTableLoaderService,
    ) {
        super(
            EditableEntity.CONSTRUCT,
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

        this.fgParts = new FormControl((this.entity.parts || []).map(normalizeTagPart), (control) => tagPartValidator(control.value));
        this.tabHandles[this.Tabs.PARTS] = new FormGroupTabHandle(this.fgParts, {
            save: () => this.updateParts(),
            reset: () => {
                this.fgParts.reset((this.entity.parts || []).map(normalizeTagPart));
                return Promise.resolve();
            },
        });
    }

    protected onEntityChange(): void {
        if (this.fgProperties) {
            this.fgProperties.reset(this.entity);
        }
        if (this.fgParts) {
            this.fgParts.reset((this.entity.parts || []).map(normalizeTagPart));
        }
    }

    protected override finalizeEntityToUpdate(construct: TagType): TagType {
        if (construct.categoryId == null) {
            construct.categoryId = -1;
        }

        return construct;
    }

    async updateParts(): Promise<void> {
        const normalizedParts = this.fgParts.value.map(part => {
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

        return this.handler.updateMapped(this.entity.id, payload).pipe(
            discard(updatedEntity => {
                this.handleEntityLoad(updatedEntity);
                this.onEntityUpdate();
            }),
        ).toPromise();
    }
}
