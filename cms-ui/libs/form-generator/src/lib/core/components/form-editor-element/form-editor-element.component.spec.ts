import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { FormEditorService } from '../../providers';
import { FormEditorElementComponent, FormEditorElementListComponent } from './form-editor-element-and-list.component';

describe('FormEditorElementComponent', () => {
    let component: FormEditorElementComponent;
    let fixture: ComponentFixture<FormEditorElementComponent>;

    beforeEach(waitForAsync(() => {

        // create mock, no function spies
        /**
         * createSpyObj not usable without function spies and does not support property spies declarations in our version
         * replace with https://jasmine.github.io/api/3.6/jasmine.html#.createSpyObj after updating
         */
        const formEditorServiceMock = {} as any; // SpyObj<T> seems not to be exported

        TestBed.configureTestingModule({
            declarations: [
                FormEditorElementComponent,
                FormEditorElementListComponent,
            ],
            providers: [
                { provide: FormEditorService, useValue: formEditorServiceMock },
            ],
            schemas: [NO_ERRORS_SCHEMA],
        }).compileComponents();
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(FormEditorElementComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    xit('should create', () => {
        expect(component).toBeTruthy();
    });
});
