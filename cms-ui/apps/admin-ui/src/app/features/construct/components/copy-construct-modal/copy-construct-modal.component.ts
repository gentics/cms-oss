import { ConstructBO } from '@admin-ui/common';
import { ALL_TRANSLATIONS, ConstructHandlerService, I18nNotificationService, LanguageHandlerService } from '@admin-ui/core';
import { ConstructPropertiesMode } from '@admin-ui/features/construct/components';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnDestroy, OnInit } from '@angular/core';
import { UntypedFormControl } from '@angular/forms';
import { CmsI18nValue, Language, TagTypeBO } from '@gentics/cms-models';
import { BaseModal } from '@gentics/ui-core';
import { Subscription } from 'rxjs';
import { switchMap } from 'rxjs/operators';

@Component({
    selector: 'gtx-copy-construct-modal',
    templateUrl: './copy-construct-modal.component.html',
    styleUrls: ['./copy-construct-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CopyConstructModalComponent extends BaseModal<boolean> implements OnInit, OnDestroy {

    // tslint:disable-next-line: variable-name
    readonly ConstructPropertiesMode = ConstructPropertiesMode;

    @Input()
    public construct: TagTypeBO | ConstructBO;

    public form: UntypedFormControl;
    public supportedLanguages: Language[] = [];
    public loading = false;

    private subscriptions: Subscription[] = [];

    constructor(
        private changeDetector: ChangeDetectorRef,
        private languageHandler: LanguageHandlerService,
        private handler: ConstructHandlerService,
        private notifications: I18nNotificationService,
    ) {
        super();
    }

    ngOnInit(): void {
        this.loading = true;

        this.subscriptions.push(this.languageHandler.getSupportedLanguages().subscribe(langs => {
            langs = langs || [];
            this.supportedLanguages = langs;
            const fallbackLanguage = langs?.[0];
            const fallbackSuffix = ALL_TRANSLATIONS.common?.copy_suffix?.[fallbackLanguage.code];
            const newName: CmsI18nValue = {};

            this.supportedLanguages.forEach(lang => {
                const suffix: string = ALL_TRANSLATIONS.common?.copy_suffix?.[lang.code] ?? fallbackSuffix;

                if ((this.construct.nameI18n || {})[lang.code]) {
                    newName[lang.code] = `${this.construct.nameI18n[lang.code]} ${suffix}`;
                } else {
                    newName[lang.code] = `${this.construct.nameI18n[fallbackLanguage.code]} ${suffix}`;
                }
            });

            this.construct.nameI18n = newName;
            this.construct.keyword += '_copy';

            this.initForm();
            this.loading = false;

            this.changeDetector.markForCheck();
        }));
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
    }

    initForm(): void {
        this.form = new UntypedFormControl(this.construct);
    }

    buttonCopyEntityClicked(): void {
        const { nodeIds, ...body } = this.form.value;
        const cleanParts = (this.construct.parts || []).map(part => {
            // Delete all IDs and GlobalIDs, as they will be populated by creating them
            delete part.globalId;
            delete part.id;

            if (part.defaultProperty) {
                delete part.defaultProperty.globalId;
                delete part.defaultProperty.id;
                delete part.defaultProperty.partId;
            }

            return part;
        });

        this.loading = true;
        this.form.disable();

        this.subscriptions.push(this.handler.createMapped(body, nodeIds).pipe(
            switchMap(created => this.handler.updateMapped(created.id, {
                parts: cleanParts,
            })),
        ).subscribe(() => {
            this.closeFn(true);
        }, err => {
            this.loading = false;
            this.form.enable();
            this.changeDetector.markForCheck();

            console.error(err);
            this.notifications.show({
                type: 'alert',
                delay: 10_000,
                message: 'construct.copy_error',
                translationParams: {
                    errorMessage: err.message,
                },
            });
        }));
    }
}
