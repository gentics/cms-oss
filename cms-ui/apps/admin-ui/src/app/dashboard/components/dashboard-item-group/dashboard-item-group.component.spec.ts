import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { DashboardItemGroupComponent } from './dashboard-item-group.component';

describe('DashboardItemGroupComponent', () => {
  let component: DashboardItemGroupComponent;
  let fixture: ComponentFixture<DashboardItemGroupComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ DashboardItemGroupComponent ],
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(DashboardItemGroupComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
