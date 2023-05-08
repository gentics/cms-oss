import { IconComponent } from '@admin-ui/shared';
import { configureComponentTest } from '@admin-ui/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActionAllowedDirective } from '../../../shared/directives/action-allowed/action-allowed.directive';
import { DashboardItemComponent } from './dashboard-item.component';

/**
 * @todo Fix this test ActionAllowedDirective problem (fix `EntityDetailHeaderComponent` test first)
 */
xdescribe('DashboardItemComponent', () => {
    let component: DashboardItemComponent;
    let fixture: ComponentFixture<DashboardItemComponent>;

    beforeEach(() => {
        configureComponentTest({
            declarations: [
                ActionAllowedDirective,
                IconComponent,
                DashboardItemComponent,
            ],
        });
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(DashboardItemComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
