import { Component } from '@angular/core';
import { TestBed, tick } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { TagPart } from '@gentics/cms-models';
import { GenticsUICoreModule } from '@gentics/ui-core';
import { componentTest, configureComponentTest } from '../../../../testing';
import { getExampleEditableTag } from '../../../../testing/test-tag-editor-data.mock';
import { ApplicationStateService } from '../../../state';
import { TestApplicationState } from '../../../state/test-application-state.mock';
import { TagPropertyLabelPipe } from '../../pipes/tag-property-label/tag-property-label.pipe';
import { TagPropertyEditorResolverService } from '../../providers/tag-property-editor-resolver/tag-property-editor-resolver.service';
import { ValidationErrorInfoComponent } from '../shared/validation-error-info/validation-error-info.component';
import { TextTagPropertyEditor } from '../tag-property-editors/text-tag-property-editor/text-tag-property-editor.component';
import { TagPropertyEditorHostComponent } from './tag-property-editor-host.component';

describe('TagPropertyEditorHostComponent', () => {

    beforeEach(() => {
        configureComponentTest({
            imports: [
                GenticsUICoreModule.forRoot(),
                FormsModule,
            ],
            providers: [
                { provide: ApplicationStateService, useClass: TestApplicationState },
                TagPropertyEditorResolverService,
            ],
            declarations: [
                TagPropertyEditorHostComponent,
                TagPropertyLabelPipe,
                TestComponent,
                TextTagPropertyEditor,
                ValidationErrorInfoComponent,
            ],
        });
    });

    it('queries the TagPropertyEditorResolverService for the TagPropertyEditor component\'s factory and instantiates the component',
        componentTest(() => TestComponent, (fixture, instance) => {
            const tagPart = getExampleEditableTag().tagType.parts[0];
            const resolverService: TagPropertyEditorResolverService = TestBed.inject(TagPropertyEditorResolverService);
            spyOn(resolverService, 'resolveTagPropertyEditorFactory').and.callThrough();
            expect(tagPart).toBeTruthy();
            expect(instance.tagPart).toBeFalsy();

            fixture.detectChanges();
            tick();
            const tagPropertyEditorHost = fixture.debugElement.query(By.directive(TagPropertyEditorHostComponent));
            expect((<HTMLElement> tagPropertyEditorHost.nativeElement).children.length).toBe(0);

            instance.tagPart = tagPart;
            fixture.detectChanges();
            tick();

            expect(resolverService.resolveTagPropertyEditorFactory).toHaveBeenCalledWith(tagPart);
            expect(fixture.debugElement.query(By.directive(TextTagPropertyEditor))).toBeTruthy();
        }),
    );

    it('properly distroys the TagPropertyEditor component',
        componentTest(() => TestComponent, (fixture, instance) => {
            const tagPart = getExampleEditableTag().tagType.parts[0];
            const resolverService: TagPropertyEditorResolverService = TestBed.inject(TagPropertyEditorResolverService);
            expect(tagPart).toBeTruthy();
            expect(instance.tagPart).toBeFalsy();

            fixture.detectChanges();
            tick();
            const tagPropertyEditorHost = fixture.debugElement.query(By.directive(TagPropertyEditorHostComponent));
            expect((<HTMLElement> tagPropertyEditorHost.nativeElement).children.length).toBe(0);

            instance.tagPart = tagPart;
            fixture.detectChanges();
            tick();
            expect(fixture.debugElement.query(By.directive(TextTagPropertyEditor))).toBeTruthy();

            fixture.destroy();
            tick();
            expect((<HTMLElement> tagPropertyEditorHost.nativeElement).children.length).toBe(0);
        }),
    );

});

@Component({
    template: `
        <tag-property-editor-host [tagPart]="tagPart"></tag-property-editor-host>
    `,
    standalone: false,
})
class TestComponent {
    tagPart: TagPart;
}
