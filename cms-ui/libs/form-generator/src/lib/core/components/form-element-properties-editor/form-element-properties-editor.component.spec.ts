import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { UntypedFormBuilder } from '@angular/forms';
import { FormEditorService } from '../../providers';

import { FormElementPropertiesEditorComponent } from './form-element-properties-editor.component';

describe('FormElementPropertiesEditorComponent', () => {
  let component: FormElementPropertiesEditorComponent;
  let fixture: ComponentFixture<FormElementPropertiesEditorComponent>;

  beforeEach(waitForAsync(() => {

    // create mock, no function spies
    /**
     * createSpyObj not usable without function spies and does not support property spies declarations in our version
     * replace with https://jasmine.github.io/api/3.6/jasmine.html#.createSpyObj after updating
     */
    const formEditorServiceMock = {} as any; // SpyObj<T> seems not to be exported

    TestBed.configureTestingModule({
      declarations: [ FormElementPropertiesEditorComponent ],
      providers: [
          { provide: FormEditorService, useValue: formEditorServiceMock },
          UntypedFormBuilder,
      ],
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(FormElementPropertiesEditorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
