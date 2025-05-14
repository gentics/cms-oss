import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { WindowRef } from '@gentics/cms-components';
import { GenticsUICoreModule, SplitViewContainerComponent } from '@gentics/ui-core';
import { Subject } from 'rxjs';
import { componentTest } from '../../../../testing/component-test';
import { configureComponentTest } from '../../../../testing/configure-component-test';
import { SpyEventTarget } from '../../../../testing/spy-event-target';
import { EntityResolver } from '../../../core/providers/entity-resolver/entity-resolver';
import { I18nService } from '../../../core/providers/i18n/i18n.service';
import { ApplicationStateService, FolderActionsService } from '../../../state';
import { TestApplicationState } from '../../../state/test-application-state.mock';
import { FileSizePipe } from '../../pipes/file-size/file-size.pipe';
import { GetInheritancePipe } from '../../pipes/get-inheritance/get-inheritance.pipe';
import { I18nDatePipe } from '../../pipes/i18n-date/i18n-date.pipe';
import { UserFullNamePipe } from '../../pipes/user-full-name/user-full-name.pipe';
import { DetailChip } from '../detail-chip/detail-chip.component';
import { ListItemDetails } from './list-item-details.component';

@Component({
    template: `
        <list-item-details
            [fields]="displayFields"
            [item]="item"
            (usageClick)="showUsage($event)"
        ></list-item-details>`,
    standalone: false,
})
class TestComponent {
    displayFields: string[] = [];
    item: any = {
        creator: 1,
        editor: 1,
    };

    showUsage(): void { }
}

class MockSplitViewContainer {
    rightPanelOpened = new Subject<void>();
    rightPanelClosed = new Subject<void>();
    splitDragEnd = new Subject<number>();
    rightPanelVisible = true;
    split = 50;
}

class MockEntityResolver {
    getUser(): any {
        return {
            firstName: 'Test',
            lastName: 'User',
        };
    }
    getTemplate(): any {
        return { name: 'Test Template' };
    }
}

class MockWindow extends SpyEventTarget {
    innerWidth = 1800;

    triggerResizeEvent(widthInPx: number): void {
        this.innerWidth = widthInPx;
        this.triggerListeners('resize');
    }
}

class MockI18nService { }


class MockFolderActions {

}


describe('ListItemDetails', () => {

    beforeEach(() => {
        configureComponentTest({
            imports: [GenticsUICoreModule],
            declarations: [
                TestComponent,
                ListItemDetails,
                DetailChip,
                GetInheritancePipe,
                I18nDatePipe,
                FileSizePipe,
                UserFullNamePipe,
            ],
            providers: [
                { provide: SplitViewContainerComponent, useClass: MockSplitViewContainer },
                { provide: EntityResolver, useClass: MockEntityResolver },
                { provide: WindowRef, useValue: { nativeWindow: new MockWindow() } },
                { provide: I18nService, useClass: MockI18nService },

                { provide: FolderActionsService, useClass: MockFolderActions },
                { provide: ApplicationStateService, useClass: TestApplicationState },
            ],
        });
    });

    it('is empty when displayFields is an empty array',
        componentTest(() => TestComponent, (fixture, instance) => {
            instance.displayFields = [];
            fixture.detectChanges();

            const chips = fixture.debugElement.queryAll(By.directive(DetailChip));
            expect(chips.length).toBe(0);
        }),
    );

    it('is empty when displayFields is undefined',
        componentTest(() => TestComponent, (fixture, instance) => {
            instance.displayFields = undefined;
            fixture.detectChanges();

            const chips = fixture.debugElement.queryAll(By.directive(DetailChip));
            expect(chips.length).toBe(0);
        }),
    );

    it('displays the displayFields',
        componentTest(() => TestComponent, (fixture, instance) => {
            instance.displayFields = [
                'creator',
                'editor',
                'cdate',
                'edate',
            ];
            fixture.detectChanges();

            const chips = fixture.debugElement.queryAll(By.directive(DetailChip));
            expect(chips.length).toBe(4);
        }),
    );

    describe('compact mode', () => {

        let splitViewContainer: MockSplitViewContainer;
        const displayFields = [
            'creator',
            'editor',
            'cdate',
            'edate',
            'fileSize',
            'pageStatus',
            'priority',
            'template',
            'fileType',
        ];

        beforeEach(() => {
            splitViewContainer = TestBed.inject(SplitViewContainerComponent);
        });

        it('is not in compact mode for large window width with right panel closed',
            componentTest(() => TestComponent, (fixture, instance) => {
                instance.displayFields = displayFields;
                splitViewContainer.rightPanelVisible = false;
                fixture.detectChanges();

                const listItemDetails = fixture.debugElement.query(By.directive(ListItemDetails));
                expect(listItemDetails.classes.compact).toBeFalsy();
            }),
        );

        it('is in compact mode for window width>1600 when right panel open with low split value',
            componentTest(() => TestComponent, (fixture, instance) => {
                instance.displayFields = displayFields;
                splitViewContainer.split = 10;
                splitViewContainer.rightPanelVisible = true;
                fixture.detectChanges();

                const listItemDetails = fixture.debugElement.query(By.directive(ListItemDetails));
                expect(listItemDetails.classes.compact).toBe(true);
            }),
        );

        it('is not in compact mode for window width>1600 when right panel open with high split value',
            componentTest(() => TestComponent, (fixture, instance) => {
                instance.displayFields = displayFields;
                splitViewContainer.split = 60;
                splitViewContainer.rightPanelVisible = true;
                fixture.detectChanges();

                const listItemDetails = fixture.debugElement.query(By.directive(ListItemDetails));
                expect(listItemDetails.classes.compact).toBeFalsy();
            }),
        );

        it('switches into compact mode when split is changed to lower value',
            componentTest(() => TestComponent, (fixture, instance) => {
                instance.displayFields = displayFields;
                splitViewContainer.split = 60;
                splitViewContainer.rightPanelVisible = true;
                fixture.detectChanges();

                const listItemDetails = fixture.debugElement.query(By.directive(ListItemDetails));
                expect(listItemDetails.classes.compact).toBeFalsy();

                splitViewContainer.splitDragEnd.next(20);
                fixture.detectChanges();
                expect(listItemDetails.classes.compact).toBe(true);
            }),
        );

        it('switches out from compact mode when split is changed to higher value',
            componentTest(() => TestComponent, (fixture, instance) => {
                instance.displayFields = displayFields;
                splitViewContainer.split = 20;
                splitViewContainer.rightPanelVisible = true;
                fixture.detectChanges();

                const listItemDetails = fixture.debugElement.query(By.directive(ListItemDetails));
                expect(listItemDetails.classes.compact).toBe(true);

                splitViewContainer.splitDragEnd.next(60);
                fixture.detectChanges();
                expect(listItemDetails.classes.compact).toBeFalsy();
            }),
        );

    });
});
