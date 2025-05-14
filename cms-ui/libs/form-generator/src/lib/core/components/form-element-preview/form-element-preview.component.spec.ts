import { NO_ERRORS_SCHEMA, Pipe, PipeTransform } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { FormElementPreviewComponent } from '..';

describe('FormElementPreviewComponent', () => {
    let component: FormElementPreviewComponent;
    let fixture: ComponentFixture<FormElementPreviewComponent>;

    beforeEach(waitForAsync(() => {

        TestBed.configureTestingModule({
            declarations: [
                FormElementPreviewComponent,
                MockI18nFgPipe,
            ],
            schemas: [NO_ERRORS_SCHEMA],
        }).compileComponents();
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(FormElementPreviewComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    xit('should create', () => {
        expect(component).toBeTruthy();
    });
});

@Pipe({
    name: 'i18nfg$',
    standalone: false,
})
class MockI18nFgPipe implements PipeTransform {
    transform(value: string): string {
        return value;
    }
}
