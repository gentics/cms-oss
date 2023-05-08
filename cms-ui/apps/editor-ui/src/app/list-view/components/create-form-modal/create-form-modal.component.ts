import { Component, OnInit, ViewChild } from '@angular/core';
import { I18nNotification } from '@editor-ui/app/core/providers/i18n-notification/i18n-notification.service';
import { CmsFormData, CmsFormElement, CmsFormElementI18nValue, CmsFormType, EditableFormProps, Form, IndexById, Language } from '@gentics/cms-models';
import { IModalDialog } from '@gentics/ui-core';
import { Observable, combineLatest, forkJoin } from 'rxjs';
import { first, map } from 'rxjs/operators';
import { FormPropertiesFormComponent } from '../../../shared/components/form-properties-form/form-properties-form.component';
import { ApplicationStateService, FolderActionsService } from '../../../state';

@Component({
    selector: 'create-form-modal',
    templateUrl: './create-form-modal.tpl.html',
    styleUrls: ['./create-form-modal.scss'],
    })
export class CreateFormModalComponent implements IModalDialog, OnInit {

    defaultProps: EditableFormProps = {};

    @ViewChild('formPropertiesForm', { static: true }) formPropertiesForm: FormPropertiesFormComponent;
    creating$: Observable<boolean>;
    /** Code strings of active Node languages to be provided for orm-properties-form language drop-down menu */
    activeNodeLanguages$: Observable<Language[]>;
    folderId: number;
    nodeId: number;

    activeContentLanguage$: Observable<Language>;
    isMultiLang$: Observable<boolean>;

    constructor(
        private folderActions: FolderActionsService,
        private appState: ApplicationStateService,
        private notification: I18nNotification,
    ) {

        this.creating$ = appState.select(state => state.folder.forms.creating);
    }

    ngOnInit(): void {
        const folderState = this.appState.now.folder;

        this.folderId = folderState.activeFolder;
        this.nodeId = folderState.activeNode;

        this.activeNodeLanguages$ = combineLatest([
            this.appState.select(state => state.entities.language),
            this.appState.select(state => state.folder.activeNodeLanguages.list),
        ]).pipe(
            map(([indexedLanguages, activeNodeLanguagesIds]: [IndexById<Language>, number[]]) => {
                return activeNodeLanguagesIds.map(id => indexedLanguages[id]);
            }),
        );

        this.activeContentLanguage$ = combineLatest([
            this.appState.select(state => state.entities.language),
            this.appState.select(state => state.folder.activeLanguage),
        ]).pipe(
            map(([indexedLanguages, activeLanguage]: [IndexById<Language>, number]) => {
                return indexedLanguages[activeLanguage];
            }),
        );

        this.isMultiLang$ = this.activeNodeLanguages$.pipe(
            map(activeNodeLanguages => activeNodeLanguages && activeNodeLanguages.length > 0),
        );
    }

    closeFn(form: Form): void { }
    cancelFn(): void { }

    registerCloseFn(close: (form: Form) => void): void {
        this.closeFn = close;
    }

    registerCancelFn(cancel: (val?: any) => void): void {
        this.cancelFn = cancel;
    }

    saveChanges(): void {
        forkJoin([
            this.isMultiLang$.pipe(first()),
            this.activeContentLanguage$.pipe(first()),
        ]).toPromise()
            .then(([isMultiLang, activeContentLanguage]: [boolean, Language]) => {
                if (!isMultiLang && !activeContentLanguage) {
                    this.notification.show({
                        type: 'alert',
                        message: 'message.form_missing_language',
                    });
                    return;
                }
                if (!this.formPropertiesForm) {
                    return;
                }

                const formValue = this.formPropertiesForm.formValue as {
                    name: string,
                    description: string | null,
                    email?: string,
                    mailsubject_i18n?: CmsFormElementI18nValue<string>,
                    successPageId?: number,
                    successNodeId?: number,
                    successurl_i18n?: CmsFormElementI18nValue<string>,
                    mailsource_pageid?: number,
                    mailsource_nodeid?: number,
                    mailtemp_i18n?: CmsFormElementI18nValue<string>,
                    languages: string[],
                    templateContext: string,
                    type: CmsFormType,
                    elements: CmsFormElement[],
                };

                const form: Partial<Form> & {
                    nodeId: number,
                    folderId: number,
                    name: string,
                    languages: string[],
                    data: CmsFormData & { type: CmsFormType, templateContext: string },
                } = {
                    name: formValue.name,
                    folderId: this.folderId,
                    description: formValue.description,
                    languages: isMultiLang ? formValue.languages : [activeContentLanguage.code],
                    nodeId: this.nodeId,
                    successPageId: formValue.successPageId,
                    successNodeId: formValue.successNodeId,
                    mailsource_pageid: formValue.mailsource_pageid,
                    mailsource_nodeid: formValue.mailsource_nodeid,
                    data: {
                        email: formValue.email,
                        successurl_i18n: formValue.successurl_i18n,
                        mailsubject_i18n: formValue.mailsubject_i18n,
                        mailtemp_i18n: formValue.mailtemp_i18n,
                        templateContext: formValue.templateContext,
                        type: formValue.type,
                        elements: formValue.elements ? formValue.elements : [],
                    },
                };

                this.folderActions.createNewForm(form).then(form => {
                    if (form) {
                        this.closeFn(form);
                    }
                });
            });

    }
}
