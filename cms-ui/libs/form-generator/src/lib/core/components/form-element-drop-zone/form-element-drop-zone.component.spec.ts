import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { FormElementDropZoneComponent } from '../form-element-drop-zone/form-element-drop-zone.component';

describe('FormElementDropZoneComponent', () => {
    let component: FormElementDropZoneComponent;
    let fixture: ComponentFixture<FormElementDropZoneComponent>;

    beforeEach(waitForAsync(() => {

        TestBed.configureTestingModule({
            imports: [
                BrowserAnimationsModule,
            ],
            declarations: [
                FormElementDropZoneComponent,
            ],
            schemas: [NO_ERRORS_SCHEMA],
        }).compileComponents();
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(FormElementDropZoneComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    xit('should create', () => {
        expect(component).toBeTruthy();
    });
});
