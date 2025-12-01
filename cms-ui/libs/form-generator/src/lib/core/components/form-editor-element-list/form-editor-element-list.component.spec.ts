import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MockI18nPipe } from '@gentics/cms-components/testing';
import { FormEditorService } from '../../providers';
import { FormEditorElementListComponent } from '../form-editor-element/form-editor-element-and-list.component';
import { FormElementDropZoneComponent } from '../form-element-drop-zone/form-element-drop-zone.component';

describe('FormEditorElementListComponent', () => {
    let component: FormEditorElementListComponent;
    let fixture: ComponentFixture<FormEditorElementListComponent>;

    beforeEach(waitForAsync(() => {

        // create mock, no function spies
        /**
         * createSpyObj not usable without function spies and does not support property spies declarations in our version
         * replace with https://jasmine.github.io/api/3.6/jasmine.html#.createSpyObj after updating
         */
        const formEditorServiceMock = {} as any; // SpyObj<T> seems not to be exported

        TestBed.configureTestingModule({
            declarations: [
                FormEditorElementListComponent,
                FormElementDropZoneComponent,
                MockI18nPipe,
            ],
            imports: [
                NoopAnimationsModule,
            ],
            providers: [
                { provide: FormEditorService, useValue: formEditorServiceMock },
            ],
            schemas: [NO_ERRORS_SCHEMA],
        })
            .compileComponents();
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(FormEditorElementListComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
