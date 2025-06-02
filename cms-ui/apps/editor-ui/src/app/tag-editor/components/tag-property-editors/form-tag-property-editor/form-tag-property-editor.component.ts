import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { I18nService } from '@editor-ui/app/core/providers/i18n/i18n.service';
import { RepositoryBrowserClient } from '@editor-ui/app/shared/providers';
import { SelectedItemHelper } from '@editor-ui/app/shared/util/selected-item-helper/selected-item-helper';
import { TagEditorContext, TagEditorError, TagPropertiesChangedFn, TagPropertyEditor } from '@gentics/cms-integration-api-models';
import {
    CmsFormTagPartProperty,
    EditableTag,
    Form,
    ItemInNode,
    Raw,
    TagPart,
    TagPartProperty,
    TagPropertyMap,
    TagPropertyType,
} from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { Observable, merge } from 'rxjs';
import { map, tap } from 'rxjs/operators';

/**
 * Used to insert forms created with Editor UI Form Generator.
 */
@Component({
    selector: 'form-tag-property-editor',
    templateUrl: './form-tag-property-editor.component.html',
    styleUrls: ['./form-tag-property-editor.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class FormTagPropertyEditorComponent implements TagPropertyEditor {

    /** Form this edited tag belongs to */
    private form?: Form<Raw>;

    /** The helper for managing and loading the selected internal form. */
    private selectedInternalForm: SelectedItemHelper<ItemInNode<Form<Raw>>>;

    /** The string that should be displayed in the input field for an internal form. */
    internalFormDisplayValue$: Observable<string>;

    /** The TagPart that the hosted TagPropertyEditor is responsible for. */
    tagPart: TagPart;

    /** The TagProperty that we are editing. */
    tagProperty: CmsFormTagPartProperty;

    /** Whether the tag is opened in read-only mode. */
    readOnly: boolean;

    /** The onChange function registered by the TagEditor. */
    private onChangeFn: TagPropertiesChangedFn;

    constructor(
        private client: GCMSRestClientService,
        private changeDetector: ChangeDetectorRef,
        private repositoryBrowserClient: RepositoryBrowserClient,
        private i18n: I18nService,
    ) { }

    initTagPropertyEditor(tagPart: TagPart, tag: EditableTag, tagProperty: TagPartProperty, context: TagEditorContext): void {
        this.selectedInternalForm = new SelectedItemHelper('form', context.node.id, this.client);

        this.internalFormDisplayValue$ = merge(
            this.selectedInternalForm.selectedItem$.pipe(
                map((selectedItem: Form<Raw>) => {
                    if (selectedItem) {
                        return selectedItem.name;
                    } else {
                        /**
                         * null is emitted, when nothing is selected.
                         * Also, null is emitted in case a referenced form got deleted and the tag property data was refetched.
                         * (Since the formId in tagProperty gets removed).
                         */
                        return this.i18n.translate('editor.form_no_selection');
                    }
                }),
            ),
            this.selectedInternalForm.loadingError$.pipe(
                map((error: { error: any, item: { itemId: number, nodeId?: number } }) => {
                    /**
                     * When a form that is referenced gets deleted, the formId is kept in tagProperty.
                     * When we try to fetch the form information we get an error message.
                     * In that case we want to inform the user that the form got deleted
                     * (and thus avoid suggesting that a valid form is still selected).
                     */
                    if (this.tagProperty && this.tagProperty.formId) {
                        /** additional check, in case the loadingError$ Subject is changed to a BehaviorSubject in the future.
                         * This could trigger an emission before this.tagProperty is set in updateTagProperty
                         */
                        return this.i18n.translate('editor.form_not_found', { id: this.tagProperty.formId });
                    } else {
                        return '';
                    }
                }),
            ),
        ).pipe(
            tap(() => this.changeDetector.markForCheck()),
        );

        this.tagPart = tagPart;
        this.readOnly = context.readOnly;
        this.form = context.form;
        this.updateTagProperty(tagProperty);
    }

    registerOnChange(fn: TagPropertiesChangedFn): void {
        this.onChangeFn = fn;
    }

    writeChangedValues(values: Partial<TagPropertyMap>): void {
        // We only care about changes to the TagProperty that this control is responsible for.
        const tagProp = values[this.tagPart.keyword];
        if (tagProp) {
            this.updateTagProperty(tagProp);
        }
    }

    /**
     * Changes the values of this.tagProperty and this.selectedInternalForm$ according
     * to newSelectedForm. This method must only be called in response to
     * user input.
     */
    changeSelectedForm(newSelectedForm: ItemInNode<Form<Raw>>): void {
        let selectedInternalForm: ItemInNode<Form<Raw>>;

        if (typeof newSelectedForm === 'string') {
            // Invalid, shouldn't happen
            this.tagProperty.formId = 0;
            selectedInternalForm = null;
        } else {
            if (newSelectedForm) {
                this.tagProperty.formId = newSelectedForm.id;
            } else {
                this.tagProperty.formId = 0;
            }

            selectedInternalForm = newSelectedForm;
        }

        if (this.onChangeFn) {
            const changes: Partial<TagPropertyMap> = {};
            changes[this.tagPart.keyword] = this.tagProperty;
            this.onChangeFn(changes);
        }
        this.selectedInternalForm.setSelectedItem(selectedInternalForm);
    }

    /**
     * Opens the repository browser to allow the user to select an internal form.
     */
    browseForForm(): void {
        let contentLanguage: string;
        if (this.form) {
            contentLanguage = this.form.languages[0];
        }
        this.repositoryBrowserClient.openRepositoryBrowser({ allowedSelection: 'form', selectMultiple: false, contentLanguage })
            .then((selectedForm) => this.changeSelectedForm(selectedForm));
    }

    /**
     * Used to update the currently edited TagProperty with external changes.
     */
    private updateTagProperty(newValue: TagPartProperty): void {
        if (newValue.type !== TagPropertyType.CMSFORM) {
            throw new TagEditorError(`TagPropertyType ${newValue.type} not supported by FormUrlTagPropertyEditor.`);
        }
        this.tagProperty = newValue ;

        this.selectedInternalForm.setSelectedItem(this.tagProperty.formId || null);

        // This seems to be necessary to make the radio buttons react correctly.
        this.changeDetector.detectChanges();
        setTimeout(() => this.changeDetector.detectChanges());
    }

}
