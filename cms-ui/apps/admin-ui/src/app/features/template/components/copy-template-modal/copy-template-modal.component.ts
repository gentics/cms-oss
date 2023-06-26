import { I18nService, TemplateOperations } from '@admin-ui/core';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit } from '@angular/core';
import { FormControl, UntypedFormControl } from '@angular/forms';
import { createNestedControlValidator } from '@gentics/cms-components';
import { Node, Raw, Tag, Template, TemplateBO } from '@gentics/cms-models';
import { BaseModal } from '@gentics/ui-core';
import { cloneDeep, pick } from 'lodash-es';
import { TemplatePropertiesMode } from '../template-properties/template-properties.component';

function cleanTags<T extends Tag>(tags: Record<string, T>): Record<string, T> {
    const out: Record<string, T> = {};

    Object.entries(tags).forEach(([key, tag]) => {
        const tagCopy = cloneDeep(tag);
        delete tagCopy.id;

        out[key] = tagCopy;
    });

    return out;
}

@Component({
    selector: 'gtx-copy-template-modal',
    templateUrl: './copy-template-modal.component.html',
    styleUrls: ['./copy-template-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CopyTemplateModal extends BaseModal<TemplateBO<Raw>> implements OnInit {

    public readonly TemplatePropertiesMode = TemplatePropertiesMode;

    @Input()
    public template: Template;

    @Input()
    public node: Node;

    public form: FormControl;
    public loading = false;

    constructor(
        protected changeDetector: ChangeDetectorRef,
        protected operations: TemplateOperations,
        protected i18n: I18nService,
    ) {
        super();
    }

    ngOnInit(): void {
        const initialValue: Partial<Template> = pick(this.template, ['name', 'description', 'markupLanguage', 'source']);
        const suffix = this.i18n.instant('common.copy_suffix');
        initialValue.name += ` ${suffix}`;

        this.form = new UntypedFormControl(initialValue, createNestedControlValidator());
    }

    async createCopy(): Promise<void> {
        if (this.form.invalid || this.loading) {
            return;
        }

        const fullCopy: Template<Raw> = {
            ...this.form.value,
            objectTags: cleanTags(this.template.objectTags),
            templateTags: cleanTags(this.template.templateTags),
        };

        this.loading = true;
        this.form.disable({ emitEvent: false });
        this.changeDetector.markForCheck();

        try {
            const created = await this.operations.create({
                folderIds: [
                    this.node.folderId,
                ],
                nodeId: this.node.id,
                template: fullCopy,
            }).toPromise();
            this.closeFn(created);
        } catch (err) {
            // Nothing to do?
        }

        this.loading = false;
        this.form.enable({ emitEvent: false });
        this.changeDetector.markForCheck();
    }
}
