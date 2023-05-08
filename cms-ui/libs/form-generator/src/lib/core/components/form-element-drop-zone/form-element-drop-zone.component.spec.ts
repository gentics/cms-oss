import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { FormElementDropZoneComponent } from '..';

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
    })
    .compileComponents();
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
