import { ChangeDetectorRef } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ApplicationStateService, STATE_MODULES } from '@editor-ui/app/state';
import { Page } from '@gentics/cms-models';
import { NgxsModule } from '@ngxs/store';
import { Observable, Subject } from 'rxjs';
import { PermissionService } from '../../../core/providers/permissions/permission.service';
import { TestApplicationState } from '../../../state/test-application-state.mock';
import { PermissionsPipe } from './permission.pipe';

const ACTIVE_NODE = 9999;

describe('PermissionPipe:', () => {

    let appState: TestApplicationState;
    let changeDetector: MockChangeDetector;
    let permissionService: MockPermissionService;
    let permissionPipe: PermissionsPipe;
    let inputPage: Page;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [NgxsModule.forRoot(STATE_MODULES)],
            providers: [
                { provide: ApplicationStateService, useClass: TestApplicationState },
            ],
        });
        appState = TestBed.get(ApplicationStateService);
        changeDetector = new MockChangeDetector();
        permissionService = new MockPermissionService();

        appState.mockState({
            folder: {
                activeNode: ACTIVE_NODE,
                activeNodeLanguages:  {
                    list: [1, 2],
                    total: 2,
                },
            },
            entities: {
                language: {
                    1: {
                        code: 'de',
                        id: 1,
                        name: 'Deutsch',
                    },
                    2: {
                        code: 'en',
                        id: 2,
                        name: 'English',
                    },
                },
            },
        });

        permissionPipe = new PermissionsPipe(
            appState,
            changeDetector as any as ChangeDetectorRef,
            permissionService as any as PermissionService,
        );

        inputPage = { id: 1234, folderId: 9876, type: 'page' } as Partial<Page> as Page;
    });

    afterEach(() => {
        if (permissionPipe) {
            permissionPipe.ngOnDestroy();
        }
    });

    it('returns undefined with undefined input', () => {
        expect(permissionPipe.transform(undefined)).toBe(undefined);
    });

    it('returns undefined with null input', () => {
        expect(permissionPipe.transform(null)).toBe(undefined);
    });

    describe('with language', () => {

        it('calls PermissionService.forFolderInLanguage', () => {
            const languageId = 1111;
            permissionPipe.transform(inputPage, languageId);
            expect(permissionService.forFolder).not.toHaveBeenCalled();
            expect(permissionService.forFolderInLanguage).toHaveBeenCalledWith(9876, ACTIVE_NODE, 1111);
        });

        it('works when passed a language id', () => {
            const languageId = 1234;
            permissionPipe.transform(inputPage, languageId);
            expect(permissionService.forFolderInLanguage).toHaveBeenCalledWith(9876, ACTIVE_NODE, 1234);
        });

        it('resolves a passed language code to the language id', () => {
            permissionPipe.transform(inputPage, 'en');
            expect(permissionService.forFolderInLanguage).toHaveBeenCalledWith(9876, ACTIVE_NODE, 2);
        });

        it('returns the permissions if they are in the app state', () => {
            const existingPermissions = { already: 'fetched permissions' };
            permissionService.forFolderInLanguage = () => Observable.of(existingPermissions);

            const output = permissionPipe.transform(inputPage, 'en');
            expect(output).toBe(existingPermissions);
        });

        it('does not mark for change detection if the permissions are in the app state', () => {
            const existingPermissions = { already: 'fetched permissions' };
            permissionService.forFolderInLanguage = () => Observable.of(existingPermissions);

            permissionPipe.transform(inputPage, 'en');
            expect(changeDetector.markForCheck).not.toHaveBeenCalled();
        });

        it('returns a "no permissions" object if they need to be fetched', () => {
            const result = permissionPipe.transform(inputPage, 'en');
            expect(everyValueIsFalse(result)).toBe(true);
        });

        it('marks for change detection after fetching permissions', () => {
            const permissionsSubject = new Subject<any>();
            permissionService.forFolderInLanguage = () => permissionsSubject;
            const result = permissionPipe.transform(inputPage, 'en');

            expect(changeDetector.markForCheck).not.toHaveBeenCalled();
            permissionsSubject.next({ permissions: 'permissions from the server' });
            expect(changeDetector.markForCheck).toHaveBeenCalled();
        });

        it('returns the permissions after fetching them', () => {
            const permissionsSubject = new Subject<any>();
            permissionService.forFolderInLanguage = () => permissionsSubject;

            let result = permissionPipe.transform(inputPage, 'en');
            expect(result).not.toEqual({ permissions: 'permissions from the server' });

            permissionsSubject.next({ permissions: 'permissions from the server' });
            result = permissionPipe.transform(inputPage, 'en');
            expect(result).toEqual({ permissions: 'permissions from the server' });
        });

    });

    describe('without language', () => {

        it('calls PermissionService.forFolder', () => {
            permissionPipe.transform(inputPage);
            expect(permissionService.forFolder).toHaveBeenCalledWith(9876, ACTIVE_NODE);
            expect(permissionService.forFolderInLanguage).not.toHaveBeenCalled();
        });

        it('returns the permissions if they are in the app state', () => {
            const existingPermissions = { already: 'fetched permissions' };
            permissionService.forFolder = () => Observable.of(existingPermissions);

            const output = permissionPipe.transform(inputPage);
            expect(output).toBe(existingPermissions);
        });

        it('does not mark for change detection if the permissions are in the app state', () => {
            const existingPermissions = { already: 'fetched permissions' };
            permissionService.forFolder = () => Observable.of(existingPermissions);

            permissionPipe.transform(inputPage);
            expect(changeDetector.markForCheck).not.toHaveBeenCalled();
        });

        it('returns a "no permissions" object if they need to be fetched', () => {
            const result = permissionPipe.transform(inputPage);
            expect(everyValueIsFalse(result)).toBe(true);
        });

        it('marks for change detection after fetching permissions', () => {
            const permissionsSubject = new Subject<any>();
            permissionService.forFolder = () => permissionsSubject;
            const result = permissionPipe.transform(inputPage);

            expect(changeDetector.markForCheck).not.toHaveBeenCalled();
            permissionsSubject.next({ permissions: 'permissions from the server' });
            expect(changeDetector.markForCheck).toHaveBeenCalled();
        });

        it('returns the permissions after fetching them', () => {
            const permissionsSubject = new Subject<any>();
            permissionService.forFolder = () => permissionsSubject;

            let result = permissionPipe.transform(inputPage);
            expect(result).not.toEqual({ permissions: 'permissions from the server' });

            permissionsSubject.next({ permissions: 'permissions from the server' });
            result = permissionPipe.transform(inputPage);
            expect(result).toEqual({ permissions: 'permissions from the server' });
        });

    });

});

class MockChangeDetector {
    markForCheck = jasmine.createSpy('markForCheck');
}

class MockPermissionService {
    constructor() {
        spyOn(this as any, 'forFolder').and.callThrough();
        spyOn(this as any, 'forFolderInLanguage').and.callThrough();
    }

    forFolder(): Observable<any> {
        return Observable.never();
    }

    forFolderInLanguage(): Observable<any> {
        return Observable.never();
    }
}

const everyValueIsFalse = (obj: { [key: string]: any }): boolean => Object.keys(obj).every(key => obj[key] === false || everyValueIsFalse(obj[key]));
