import { NO_ERRORS_SCHEMA, Pipe, PipeTransform } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { FormEditorService } from '../../providers';
import { FormEditorElementComponent, FormEditorElementListComponent } from '../form-editor-element/form-editor-element-and-list.component';
import { FormEditorMenuComponent } from './form-editor-menu.component';

describe('FormEditorMenuComponent', () => {
    let component: FormEditorMenuComponent;
    let fixture: ComponentFixture<FormEditorMenuComponent>;

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
                FormEditorMenuComponent,
                MockI18nPipe,
            ],
            imports: [
                FormsModule,
            ],
            providers: [
                { provide: FormEditorService, useValue: formEditorServiceMock },
            ],
            schemas: [NO_ERRORS_SCHEMA],
        }).compileComponents();
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(FormEditorMenuComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    xit('should create', () => {
        expect(component).toBeTruthy();
    });
});

@Pipe({
    name: 'i18n',
    standalone: false,
})
class MockI18nPipe implements PipeTransform {
    transform(value: string): string {
        return value;
    }
}
