import { Injectable } from '@angular/core';
import { CmsFormElementBO, CmsFormType, Form, FormElementLabelPropertyI18nValues, Normalized, Raw } from '@gentics/cms-models';
import { Observable } from 'rxjs';
import { first, map } from 'rxjs/operators';
import { ElementPropertyPipe } from '../../pipes/element-property/element-property.pipe';
import { FormEditorConfigurationService } from '../form-editor-configuration/form-editor-configuration.service';
import { FormEditorMappingService } from '../form-editor-mapping/form-editor-mapping.service';

@Injectable()
export class FormReportService {

    private transformer: ElementPropertyPipe;

    constructor(
        private formEditorConfigurationService: FormEditorConfigurationService,
        private formEditorMappingService: FormEditorMappingService,
    ) {
        this.transformer = new ElementPropertyPipe();
    }

    getFormElementLabelPropertyValues = (form: Form<Raw | Normalized>): Observable<FormElementLabelPropertyI18nValues> => {
        return this.formEditorConfigurationService.getConfiguration$(!!form.data.type ? form.data.type : CmsFormType.GENERIC).pipe(
            first(),
            map(formConfiguration => {
                return this.formEditorMappingService.mapFormToFormBO(form, formConfiguration);
            }),
            map(mappedForm => {
                if (!mappedForm || !mappedForm.data || !Array.isArray(mappedForm.data.elements)) {
                    return {};
                }

                const map: FormElementLabelPropertyI18nValues = {};
                this.addElementsToMap(map, mappedForm.data.elements);

                return map;
            }),
        );
    }

    private addElementsToMap(map: FormElementLabelPropertyI18nValues, elements: CmsFormElementBO[]): void {
        elements.forEach(elem => {
            map[elem.name] = this.transformer.transform(elem);

            if (Array.isArray(elem.elements)) {
                this.addElementsToMap(map, elem.elements);
            }
        });
    }

}
