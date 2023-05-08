import { TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { GenticsUICoreModule } from '@gentics/ui-core';
import { configureComponentTest } from '../../../../testing';
import { EntityResolver } from '../../../core/providers/entity-resolver/entity-resolver';
import { NavigationService } from '../../../core/providers/navigation/navigation.service';
import { DetailChip } from '../../../shared/components/detail-chip/detail-chip.component';
import { ItemStateContextMenuComponent } from '../../../shared/components/item-state-contextmenu/item-state-contextmenu.component';
import { ItemStatusLabelComponent } from '../../../shared/components/item-status-label/item-status-label.component';
import { I18nDatePipe } from '../../../shared/pipes/i18n-date/i18n-date.pipe';
import { ApplicationStateService } from '../../../state';
import { TestApplicationState } from '../../../state/test-application-state.mock';
import { UsageActionsService } from '../../../state';
import { UsageList } from '../usage-list/usage-list.component';
import { UsageModalComponent } from './usage-modal.component';

class MockUsageActions {
    getUsage(): void { }
}

describe('UsageModal', () => {

    let state: TestApplicationState;
    let usageModal: UsageModalComponent;

    beforeEach(() => {
        configureComponentTest({
            imports: [GenticsUICoreModule, RouterTestingModule.withRoutes([])],
            providers: [
                EntityResolver,
                NavigationService,
                { provide: ApplicationStateService, useClass: TestApplicationState },
                { provide: UsageActionsService, useClass: MockUsageActions },
            ],
            declarations: [
                UsageModalComponent,
                UsageList,
                ItemStatusLabelComponent,
                DetailChip,
                I18nDatePipe,
                ItemStateContextMenuComponent,
            ],
        });

        state = TestBed.get(ApplicationStateService);
        state.mockState({
            folder: {
                activeNodeLanguages: {
                    list: [1, 2, 3],
                },
            },
        });
    });

    beforeEach(() => {
        let fixture = TestBed.createComponent(UsageModalComponent);
        usageModal = fixture.componentInstance;
    });

    describe('groupPagesByLanguage', () => {

        let pages: any[];

        beforeEach(() => {
            pages = [
                {
                    id: 1,
                    contentGroupId: 1,
                    contentSetId: 111,
                    name: 'Hallo',
                },
                {
                    id: 2,
                    contentGroupId: 2,
                    contentSetId: 111,
                    name: 'Hello',
                },
                {
                    id: 3,
                    contentGroupId: 1,
                    contentSetId: 333,
                    name: 'Foo',
                },
                {
                    id: 4,
                    contentGroupId: 2,
                    contentSetId: 444,
                    name: 'Bar',
                },
            ];
        });

        it('should group pages which share a contentSetId', () => {
            let grouped = usageModal.groupPagesByLanguage(pages);

            expect(grouped.length).toBe(3);
            expect(grouped.map(p => p.id)).toEqual([1, 3, 4]);
        });

        it('should add variants to the languageVariants object with a single language', () => {
            let grouped = usageModal.groupPagesByLanguage(pages);

            expect(grouped[1].languageVariants).toBeDefined();
            expect(grouped[1].languageVariants).toEqual({
                1: 3,
            });
        });

        it('should add variants to the languageVariants object with multiple languages', () => {
            let grouped = usageModal.groupPagesByLanguage(pages);

            expect(grouped[0].languageVariants).toBeDefined();
            expect(grouped[0].languageVariants).toEqual({
                1: 1,
                2: 2,
            });
        });

        it('should not fold up the currentLanguageId', () => {
            let grouped1 = usageModal.groupPagesByLanguage(pages, 1);
            expect(grouped1[0].contentGroupId).toBe(1);

            let grouped2 = usageModal.groupPagesByLanguage(pages, 2);
            expect(grouped2[0].contentGroupId).toBe(2);
        });

        it('should use the default order if the currentLanguageId is invalid', () => {
            let grouped1 = usageModal.groupPagesByLanguage(pages, 99);
            expect(grouped1[0].contentGroupId).toBe(1);
        });

    });
});
