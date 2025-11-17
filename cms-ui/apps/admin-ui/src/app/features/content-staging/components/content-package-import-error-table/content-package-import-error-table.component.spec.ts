import { ContentPackageOperations } from '@admin-ui/core';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectorRef } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { I18nNotificationService } from '@gentics/cms-components';
import { ModalService } from '@gentics/ui-core';
import { mockPipe } from '@gentics/ui-core/testing';
import { I18nService } from '@gentics/cms-components';
import { BehaviorSubject, of } from 'rxjs';
import { ContentPackageImportErrorTableLoaderService, ContentPackageTableLoaderService } from '../../providers';
import { ContentPackageImportErrorTableComponent } from './content-package-import-error-table.component';

describe('ContentPackageImportErrorTableComponent', () => {

    let component: ContentPackageImportErrorTableComponent;
    let fixture: ComponentFixture<ContentPackageImportErrorTableComponent>;
    let loaderService: jasmine.SpyObj<ContentPackageImportErrorTableLoaderService>;
    let i18nNotificationService: jasmine.SpyObj<I18nNotificationService>;
    let operations: jasmine.SpyObj<ContentPackageOperations>;

    beforeEach(async () => {
        const loaderServiceSpy = jasmine.createSpyObj('ContentPackageImportErrorTableLoaderService', ['loadTablePage'], {
            checkResultAvailable$: new BehaviorSubject<boolean>(true),
            lastCheckTimestamp$: new BehaviorSubject<string>(''),
            reload$: of(null),
        });
        const i18nNotificationServiceSpy = jasmine.createSpyObj('I18nNotificationService', ['show']);
        const operationsSpy = jasmine.createSpyObj('ContentPackageOperations', ['importFromFileSystem']);
        const i18nServiceSpy = jasmine.createSpyObj('I18nService', ['instant']);
        const cptls = jasmine.createSpyObj('ContentPackageTableLoaderService', ['reload']);

        await TestBed.configureTestingModule({
            declarations: [
                ContentPackageImportErrorTableComponent,
                mockPipe('i18n'),
            ],
            providers: [
                { provide: ChangeDetectorRef, useValue: {} },
                { provide: AppStateService, useValue: {} },
                { provide: I18nService, useValue: i18nServiceSpy },
                { provide: ModalService, useValue: {} },
                { provide: ContentPackageImportErrorTableLoaderService, useValue: loaderServiceSpy },
                { provide: I18nNotificationService, useValue: i18nNotificationServiceSpy },
                { provide: ContentPackageOperations, useValue: operationsSpy },
                { provide: ContentPackageTableLoaderService, useValue: cptls },
            ],
        }).compileComponents();

        fixture = TestBed.createComponent(ContentPackageImportErrorTableComponent);
        component = fixture.componentInstance;
        loaderService = TestBed.inject(ContentPackageImportErrorTableLoaderService) as jasmine.SpyObj<ContentPackageImportErrorTableLoaderService>;
        i18nNotificationService = TestBed.inject(I18nNotificationService) as jasmine.SpyObj<I18nNotificationService>;
        operations = TestBed.inject(ContentPackageOperations) as jasmine.SpyObj<ContentPackageOperations>;
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should reload data when contentPackage changes', () => {
        spyOn(component, 'reload');
        component.ngOnChanges({
            contentPackage: {
                currentValue: { name: 'newPackage' },
                previousValue: { name: 'oldPackage' },
                firstChange: false,
                isFirstChange: () => false,
            },
        });

        expect(component.reload).toHaveBeenCalled();
    });

    it('should show message when no result is unavailable', async () => {
        loaderService.checkResultAvailable$.next(false);

        fixture.detectChanges();
        await fixture.whenStable();

        const element = fixture.debugElement.query(By.css('[data-test-id="check-result-unavailable"]'));

        expect(element.nativeElement.textContent).toBeTruthy();
    });

    it('should show appropriate elements when result is available', async () => {
        loaderService.checkResultAvailable$.next(true);
        (loaderService.loadTablePage as jasmine.Spy).and.callFake(() => {
            return of({
                totalCount: 10,
                rows: [],
            });
        });
        const lastCheck = 1723453518197;
        loaderService.lastCheckTimestamp$.next(lastCheck.toString());

        fixture.detectChanges();
        await fixture.whenStable();

        fixture.detectChanges();
        await fixture.whenStable();

        const checkResultElement = fixture.debugElement.query(By.css('[data-test-id="check-result-available"]'));
        const errorTable = fixture.debugElement.query(By.css('.entity-table'));
        const okIcon = fixture.debugElement.query(By.css('.check-ok'));

        expect(checkResultElement.nativeElement.textContent).toContain('content_staging.content_package_last_check');
        expect(errorTable).toBeTruthy();
        expect(okIcon).toBeNull();
    });

    it('should show appropriate elements when check is ok', async () => {
        loaderService.checkResultAvailable$.next(true);
        component.totalCount = 0; // no errors
        const lastCheck = 1723453518197;
        loaderService.lastCheckTimestamp$.next(lastCheck.toString());

        fixture.detectChanges();
        await fixture.whenStable();

        const checkResultElement = fixture.debugElement.query(By.css('[data-test-id="check-result-available"]'));
        const errorTable = fixture.debugElement.query(By.css('.entity-table'));
        const checkOkElement = fixture.debugElement.query(By.css('.check-ok > span'));
        const okIcon = fixture.debugElement.query(By.css('.check-ok'));

        expect(checkResultElement.nativeElement.textContent).toContain('content_staging.content_package_last_check');
        expect(checkOkElement.nativeElement.textContent).toContain('content_staging.content_package_check_ok');
        expect(checkResultElement.nativeElement.textContent).toContain(lastCheck);
        expect(errorTable).toBeFalsy();
        expect(okIcon).toBeTruthy();
    });

    it('should show appropriate elements when result is unavailable', async () => {
        loaderService.checkResultAvailable$.next(false);

        fixture.detectChanges();
        await fixture.whenStable();

        const checkResultElement = fixture.debugElement.query(By.css('[data-test-id="check-result-unavailable"]'));
        const errorTable = fixture.debugElement.query(By.css('.entity-table'));
        const okIcon = fixture.debugElement.query(By.css('.check-ok'));

        expect(checkResultElement.nativeElement.textContent).toContain('content_staging.content_package_check_no_result');
        expect(errorTable).toBeNull();
        expect(okIcon).toBeNull();
    });

    it('should handle check button click', () => {
        const packageName = 'testPackage';
        operations.importFromFileSystem.and.returnValue(of(null));
        component.contentPackage = {
            id: packageName,
            name: packageName,
        };
        spyOn(component, 'reload');
        component.handleCheckButtonClick();

        expect(i18nNotificationService.show).toHaveBeenCalledWith({
            message: 'content_staging.start_import__check_message',
            type: 'success',
            translationParams: { packageName },
        });
        expect(operations.importFromFileSystem).toHaveBeenCalled();
    });
});
