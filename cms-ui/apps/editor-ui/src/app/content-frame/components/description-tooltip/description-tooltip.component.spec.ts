import { Component, NO_ERRORS_SCHEMA } from '@angular/core';
import { TestBed, waitForAsync } from '@angular/core/testing';
import { componentTest } from '../../../../testing';
import { mockPipes } from '../../../../testing/mock-pipe';
import { I18nNotification } from '../../../core/providers/i18n-notification/i18n-notification.service';
import { DescriptionTooltipComponent } from './description-tooltip.component';

const target = 'testTarget';
const objectProperty = {
    displayName: 'test',
    name: 'testName',
    id: 123,
    tagType: {
        name: 'tagTypeName',
        visibleInMenu: false,
    },
    description: '',
    readonly: true,
    required: false,
};

describe('DescriptionTooltipComponent', () => {
    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            declarations: [
                DescriptionTooltipComponent,
                TestComponent,
                mockPipes('i18n'),
            ],
            providers: [
                { provide: I18nNotification, useClass: MockI18Notification },
            ],
            schemas: [NO_ERRORS_SCHEMA],
        });
    }));

    it('should create',
        componentTest(() => TestComponent, (fixture, component) => {
            expect(component).toBeTruthy();
        }),
    );

    it('should have the right target',
        componentTest(() => TestComponent, (fixture, component) => {
            component.target = target;

            fixture.detectChanges();

            expect(component.target).toEqual('testTarget');
        }),
    );

    it('should have the right objectProperty',
        componentTest(() => TestComponent, (fixture, component) => {
            component.objectProperty = objectProperty;

            fixture.detectChanges();

            expect(component.objectProperty.name).toEqual('testName');
        }),
    );
});

@Component({
    template: '<description-tooltip [target]="target" [(visible)]="visible"></description-tooltip>',
})
class TestComponent {
    objectProperty: any;
    target: string;
    visible: boolean;
}

class MockI18Notification {}
