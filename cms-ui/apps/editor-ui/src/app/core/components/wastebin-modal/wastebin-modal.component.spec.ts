import { TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { FALLBACK_LANGUAGE, I18nDatePipe, I18nService } from '@gentics/cms-components';
import { Folder, Normalized } from '@gentics/cms-models';
import { GenticsUICoreModule, ModalService } from '@gentics/ui-core';
import { NgxPaginationModule } from 'ngx-pagination';
import { NEVER, Observable } from 'rxjs';
import { configureComponentTest } from '../../../../testing';
import { DetailChip } from '../../../shared/components/detail-chip/detail-chip.component';
import { IconCheckbox } from '../../../shared/components/icon-checkbox/icon-checkbox.component';
import { ImageThumbnailComponent } from '../../../shared/components/image-thumbnail/image-thumbnail.component';
import { PagingControls } from '../../../shared/components/paging-controls/paging-controls.component';
import { TypeIconPipe } from '../../../shared/pipes';
import { ApplicationStateService, FolderActionsService, WastebinActionsService } from '../../../state';
import { TestApplicationState } from '../../../state/test-application-state.mock';
import { EntityResolver } from '../../providers/entity-resolver/entity-resolver';
import { ErrorHandler } from '../../providers/error-handler/error-handler.service';
import { LocalizationsService } from '../../providers/localizations/localizations.service';
import { WastebinList } from '../wastebin-list/wastebin-list.component';
import { SortingModal } from './../../../shared/components/sorting-modal/sorting-modal.component';
import { WastebinModal } from './wastebin-modal.component';

describe('WastebinModal', () => {
    let state: TestApplicationState;

    beforeEach(() => {
        configureComponentTest({
            imports: [GenticsUICoreModule, FormsModule, NgxPaginationModule],
            providers: [
                { provide: ApplicationStateService, useClass: TestApplicationState },
                EntityResolver,
                { provide: LocalizationsService, useClass: MockLocalizationsService },
                { provide: ErrorHandler, useClass: MockErrorHandler },
                { provide: ModalService, useClass: MockModalService },
                { provide: WastebinActionsService, useClass: MockWastebinActions },
                { provide: FolderActionsService, useClass: MockFolderActions },
                { provide: I18nService, useClass: MockI18nService },
            ],
            declarations: [
                WastebinModal,
                WastebinList,
                IconCheckbox,
                ImageThumbnailComponent,
                DetailChip,
                I18nDatePipe,
                PagingControls,
                TypeIconPipe,
            ],
        });

        state = TestBed.inject(ApplicationStateService) as any;
        state.mockState({
            entities: {
                folder: {
                    1234: {
                        id: 1234,
                        type: 'folder',
                        deleted: {
                            by: {
                                firstName: 'some',
                                lastName: 'user',
                            },
                        },
                    } as Folder<Normalized>,
                },
            },
            folder: {
                activeNode: 1,
            },
        });
    });

    describe('empty placeholder', () => {

        it('is not displayed if any requests still open', () => {

            state.mockState({
                wastebin: {
                    folder: { list: [], requesting: true },
                    form: { list: [], requesting: false },
                    page: { list: [], requesting: false },
                    file: { list: [], requesting: false },
                    image: { list: [], requesting: false },
                    lastError: '',
                },
            });

            const fixture = TestBed.createComponent(WastebinModal);
            fixture.detectChanges();

            expect(fixture.nativeElement.querySelector('.empty-message')).toBeNull();
        });

        it('is displayed when all requests are done and all lists empty', () => {
            state.mockState({
                wastebin: {
                    folder: { list: [], requesting: false },
                    form: { list: [], requesting: false },
                    page: { list: [], requesting: false },
                    file: { list: [], requesting: false },
                    image: { list: [], requesting: false },
                    lastError: '',
                },
            });

            const fixture = TestBed.createComponent(WastebinModal);
            fixture.detectChanges();

            expect(fixture.nativeElement.querySelector('.empty-message')).not.toBeNull();
        });

        it('is not displayed when all requests are done and any list has items', () => {
            state.mockState({
                wastebin: {
                    folder: { list: [1234], requesting: false },
                    form: { list: [], requesting: false },
                    page: { list: [], requesting: false },
                    file: { list: [], requesting: false },
                    image: { list: [], requesting: false },
                    lastError: '',
                },
            });

            const fixture = TestBed.createComponent(WastebinModal);
            fixture.detectChanges();

            expect(fixture.nativeElement.querySelector('.empty-message')).toBeNull();
        });
    });

    describe('sort controls', () => {

        beforeEach(() => {
            state.mockState({
                wastebin: {
                    sortBy: 'name',
                    sortOrder: 'desc',
                    folder: { list: [], requesting: false },
                    form: { list: [], requesting: false },
                    page: { list: [], requesting: false },
                    file: { list: [], requesting: false },
                    image: { list: [], requesting: false },
                },
            });
        });

        it('changes the icon if sort order changed', () => {
            const fixture = TestBed.createComponent(WastebinModal);
            const component = fixture.componentInstance;
            fixture.detectChanges();

            let arrowIcon = fixture.debugElement.query(By.css('.filter-wrapper gtx-button icon'));
            expect(arrowIcon.nativeElement.innerHTML).toEqual('arrow_downward');

            state.mockState({
                wastebin: {
                    ...state.now.wastebin,
                    sortOrder: 'asc',
                },
            });

            fixture.detectChanges();
            arrowIcon = fixture.debugElement.query(By.css('.filter-wrapper gtx-button icon'));
            expect(arrowIcon.nativeElement.innerHTML).toEqual('arrow_upward');
        });

        it('opens modal to select the sorting if button clicked', () => {
            const fixture = TestBed.createComponent(WastebinModal);
            fixture.detectChanges();
            const component = fixture.componentInstance;

            spyOn(component, 'selectSorting').and.callThrough();
            const button = fixture.debugElement.query(By.css('.filter-wrapper gtx-button'));
            button.triggerEventHandler('click', {});
            expect(component.selectSorting).toHaveBeenCalled();

            const modalService: MockModalService = TestBed.inject(ModalService) as any;
            expect(modalService.fromComponent).toHaveBeenCalledWith(SortingModal, {}, {
                itemType: 'wastebin',
                sortBy: 'name',
                sortOrder: 'desc',
            });
        });
    });

});

class MockWastebinActions {
    getWastebinContents(): void {}
}

class MockFolderActions {}

class MockErrorHandler {
    catch(): void {}
}

class MockModalService {
    fromComponent = jasmine.createSpy('ModalService.fromComponent')
        .and.returnValue(new Promise((neverResolve) => {}));
}
class MockLocalizationsService {
}

class MockI18nService implements Partial<I18nService> {
    public onLanguageChange(): Observable<string> {
        return NEVER;
    }

    public getCurrentLanguage(): string {
        return FALLBACK_LANGUAGE;
    }

    translate(key: string, params?: any): string {
        return key;
    }
}
