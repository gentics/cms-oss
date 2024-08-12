import { ContentPackageOperations, I18nNotificationService, I18nService } from '@admin-ui/core';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectorRef, Pipe, PipeTransform } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { ModalService } from '@gentics/ui-core';
import { BehaviorSubject, of } from 'rxjs';
import { mockPipe } from '@admin-ui/testing';
import { ContentPackageImportErrorTableLoaderService } from '../../providers';
import { ContentPackageImportErrorTableComponent } from './content-package-import-error-table.component';



fdescribe('ContentPackageImportErrorTableComponent', () => {
    let component: ContentPackageImportErrorTableComponent;
    let fixture: ComponentFixture<ContentPackageImportErrorTableComponent>;
    let loaderService: jasmine.SpyObj<ContentPackageImportErrorTableLoaderService>;
    let i18nNotificationService: jasmine.SpyObj<I18nNotificationService>;
    let i18nService: jasmine.SpyObj<I18nService>;
    let operations: jasmine.SpyObj<ContentPackageOperations>;


    beforeEach(async () => {
        const loaderServiceSpy = jasmine.createSpyObj('ContentPackageImportErrorTableLoaderService', [], {
            checkResultAvailable$:  new BehaviorSubject<boolean>(true),
            lastCheckTimestamp$: new BehaviorSubject<string>(''),
        });
        const i18nNotificationServiceSpy = jasmine.createSpyObj('I18nNotificationService', ['show']);
        const operationsSpy = jasmine.createSpyObj('ContentPackageOperations', ['importFromFileSystem']);
        const i18nServiceSpy = jasmine.createSpyObj('I18nService', ['instant']);

        await TestBed.configureTestingModule({
            declarations: [ContentPackageImportErrorTableComponent, mockPipe('i18n')],
            providers: [
                { provide: ChangeDetectorRef, useValue: {} },
                { provide: AppStateService, useValue: {} },
                { provide: I18nService, useValue: i18nServiceSpy },
                { provide: ModalService, useValue: {} },
                { provide: ContentPackageImportErrorTableLoaderService, useValue: loaderServiceSpy },
                { provide: I18nNotificationService, useValue: i18nNotificationServiceSpy },
                { provide: ContentPackageOperations, useValue: operationsSpy },
            ],
        }).compileComponents();

        fixture = TestBed.createComponent(ContentPackageImportErrorTableComponent);
        component = fixture.componentInstance;
        loaderService = TestBed.inject(ContentPackageImportErrorTableLoaderService) as jasmine.SpyObj<ContentPackageImportErrorTableLoaderService>;
        i18nNotificationService = TestBed.inject(I18nNotificationService) as jasmine.SpyObj<I18nNotificationService>;
        i18nService = TestBed.inject(I18nService) as jasmine.SpyObj<I18nService>;
        operations = TestBed.inject(ContentPackageOperations) as jasmine.SpyObj<ContentPackageOperations>;
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should reload data when packageName changes', () => {
        spyOn(component, 'reload');
        component.ngOnChanges({ packageName: { currentValue: 'newPackage', previousValue: 'oldPackage', firstChange: false, isFirstChange: () => false } });

        expect(component.reload).toHaveBeenCalled();
    });

    it('should show message when no result is unavailable', async () => {
        loaderService.checkResultAvailable$.next(false);
        await fixture.whenStable();
        fixture.detectChanges();
        const element = fixture.debugElement.query(By.css('[data-test-id="check-result-unavailable"]'));

        expect(element.nativeElement.textContent).toBeTruthy()
    })

    it('should show appropriate elements when result is available', async () => {
        loaderService.checkResultAvailable$.next(true);
        component.totalCount = 10; // number of import errors
        const lastCheck = 1723453518197;
        loaderService.lastCheckTimestamp$.next(lastCheck.toString())
        await fixture.whenStable();
        fixture.detectChanges();
        const checkResultElement = fixture.debugElement.query(By.css('[data-test-id="check-result-available"]'));
        const errorTable = fixture.debugElement.query(By.css('.entity-table'));
        const okIcon = fixture.debugElement.query(By.css('.check-ok'));

        expect(checkResultElement.nativeElement.textContent).toContain('content_staging.content_package_last_check')
        expect(errorTable).toBeTruthy()
        expect(okIcon).toBeNull()
    })

    it('should show appropriate elements when check is ok', async () => {
        loaderService.checkResultAvailable$.next(true);
        component.totalCount = 0; // no errors
        const lastCheck = 1723453518197;
        loaderService.lastCheckTimestamp$.next(lastCheck.toString())
        await fixture.whenStable();
        fixture.detectChanges();

        const checkResultElement = fixture.debugElement.query(By.css('[data-test-id="check-result-available"]'));
        const errorTable = fixture.debugElement.query(By.css('.entity-table'));
        const checkOkElement = fixture.debugElement.query(By.css('.check-ok > span'));
        const okIcon = fixture.debugElement.query(By.css('.check-ok'));

        expect(checkResultElement.nativeElement.textContent).toContain('content_staging.content_package_last_check')
        expect(checkOkElement.nativeElement.textContent).toContain('content_staging.content_package_check_ok')
        expect(checkResultElement.nativeElement.textContent).toContain(lastCheck)
        expect(errorTable).toBeFalsy()
        expect(okIcon).toBeTruthy()
    })


    it('should show appropriate elements when result is unavailable', async () => {
        loaderService.checkResultAvailable$.next(false);
        await fixture.whenStable();
        fixture.detectChanges();
        const checkResultElement = fixture.debugElement.query(By.css('[data-test-id="check-result-unavailable"]'));
        const errorTable = fixture.debugElement.query(By.css('.entity-table'));
        const okIcon = fixture.debugElement.query(By.css('.check-ok'));

        expect(checkResultElement.nativeElement.textContent).toContain('content_staging.content_package_check_no_result')
        expect(errorTable).toBeNull()
        expect(okIcon).toBeNull()
    })

    it('should handle check button click', () => {
        const packageName = 'testPackage';
        operations.importFromFileSystem.and.returnValue(of(null));
        spyOn(component, 'reload');
        component.handleCheckButtonClick(packageName);

        expect(i18nNotificationService.show).toHaveBeenCalledWith({
            message: 'content_staging.start_import__check_message',
            type: 'success',
            translationParams: { packageName },
        });
        expect(operations.importFromFileSystem).toHaveBeenCalled();
    });
});
