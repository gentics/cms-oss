import {
    AccessControlledType,
    FolderItemType,
    GCMS_ROLE_PRIVILEGES_TO_GCMS_PERMISSIONS_MAP,
    GcmsPermission,
    GcmsRolePrivilege,
    PermissionsMapCollection,
    RecursivePartial,
} from '@gentics/cms-models';
import { cloneDeep } from 'lodash-es';
import {
    DefaultPermissionsFactory,
    FolderInstancePermissions,
    FolderInstancePermissionsImpl,
    InstancePermissionsImpl,
    PermissionsFactory,
    TypePermissions,
    TypePermissionsImpl,
} from './permissions';

const MOCK_PERMISSIONS_MAP_COLLECTION: PermissionsMapCollection = {
    permissions: {
        read: true,
        setperm: false,
        update: true,
    },
} as const;

const MOCK_FOLDER_INSTANCE_PERMISSIONS_MAP_COLLECTION: RecursivePartial<PermissionsMapCollection> = {
    permissions: {
        readitems: true,
        createitems: false,
        updateitems: true,
        deleteitems: false,
        publishpages: false,
    },
    rolePermissions: {
        file: {
            createitems: true,
            deleteitems: false,
        },
        page: {
            createitems: true,
            publishpages: false,
            translatepages: true,
            deleteitems: false,
        },
        pageLanguages: {
            en: {
                createitems: true,
                publishpages: true,
                deleteitems: true,
                translatepages: true,
            },
            de: {
                createitems: true,
                publishpages: false,
                deleteitems: true,
                translatepages: true,
            },
        },
    },
} as const;

describe('permissions', () => {

    describe('TypePermissionsImpl', () => {

        let permissions: TypePermissions;

        beforeEach(() => {
            permissions = new TypePermissionsImpl(AccessControlledType.USER_ADMIN, MOCK_PERMISSIONS_MAP_COLLECTION);
        });

        it('type is set correctly', () => {
            expect(permissions.type).toEqual(AccessControlledType.USER_ADMIN);
        });

        it('hasPermission() works for permissions, which are granted', () => {
            expect(permissions.hasPermission(GcmsPermission.READ)).toBe(true);
            expect(permissions.hasPermission(GcmsPermission.UPDATE)).toBe(true);
        });

        it('hasPermission() works for permissions, which are not granted', () => {
            expect(permissions.hasPermission(GcmsPermission.SET_PERMISSION)).toBe(false);
        });

        it('hasPermission() works for permissions, which are not applicable', () => {
            expect(() => permissions.hasPermission(GcmsPermission.PUBLISH_PAGES)).toThrow();
        });

    });

    describe('InstancePermissionsImpl', () => {

        it('instanceId and nodeId are set correctly', () => {
            const permissions = new InstancePermissionsImpl(AccessControlledType.USER_ADMIN, MOCK_PERMISSIONS_MAP_COLLECTION, 4711, 1);
            expect(permissions.instanceId).toBe(4711);
            expect(permissions.nodeId).toBe(1);
        });

    });

    describe('FolderInstancePermissionsImpl', () => {

        let folderPermissions: FolderInstancePermissions;

        beforeEach(() => {
            folderPermissions = new FolderInstancePermissionsImpl(MOCK_FOLDER_INSTANCE_PERMISSIONS_MAP_COLLECTION as any, 4711, 1);
            spyOn(folderPermissions, 'hasPermission').and.callThrough();
        });

        describe('hasPermissionOrRolePrivilege()', () => {

            function assertHasPermOrPrivWorks(itemType: FolderItemType, privilege: GcmsRolePrivilege, expectedResult: boolean): void {
                const result = folderPermissions.hasPermissionOrRolePrivilege(itemType, privilege);
                expect(folderPermissions.hasPermission).toHaveBeenCalledTimes(1);
                expect(folderPermissions.hasPermission).toHaveBeenCalledWith(GCMS_ROLE_PRIVILEGES_TO_GCMS_PERMISSIONS_MAP[privilege]);
                expect(result).toBe(expectedResult);
            }

            function assertHasPermOrPrivWorksWithLanguage(privilege: GcmsRolePrivilege, lang: string, expectedResult: boolean): void {
                const result = folderPermissions.hasPermissionOrRolePrivilege('page', privilege, lang);
                expect(folderPermissions.hasPermission).toHaveBeenCalledTimes(1);
                expect(folderPermissions.hasPermission).toHaveBeenCalledWith(GCMS_ROLE_PRIVILEGES_TO_GCMS_PERMISSIONS_MAP[privilege]);
                expect(result).toBe(expectedResult);
            }

            function assertHasPermOrPrivWorksWithoutRoles(itemType: FolderItemType): void {
                const mapsClone = cloneDeep(MOCK_FOLDER_INSTANCE_PERMISSIONS_MAP_COLLECTION);
                delete mapsClone.rolePermissions;
                folderPermissions = new FolderInstancePermissionsImpl(mapsClone as any, 4711, 1);

                expect(MOCK_FOLDER_INSTANCE_PERMISSIONS_MAP_COLLECTION.permissions.createitems).toBe(false);
                expect(folderPermissions.hasPermissionOrRolePrivilege('page', GcmsRolePrivilege.createitems)).toBe(false);
            }

            it('checks the permissions first and maps the privilege correctly to a permission', () => {
                expect(MOCK_FOLDER_INSTANCE_PERMISSIONS_MAP_COLLECTION.rolePermissions.page.readitems).toBeUndefined();

                // Since readitems is not set in the rolePermissions, the result would not be true if the check had continued past hasPermission().
                assertHasPermOrPrivWorks('page', GcmsRolePrivilege.readitems, true);
            });

            it('does not check permissions for translatepages', () => {
                const result = folderPermissions.hasPermissionOrRolePrivilege('page', GcmsRolePrivilege.translatepages);
                expect(folderPermissions.hasPermission).not.toHaveBeenCalled();
                expect(result).toBe(true);
            });

            it('throws an error when checking a non applicable privilege', () => {
                expect(() => folderPermissions.hasPermissionOrRolePrivilege('file', GcmsRolePrivilege.publishpages)).toThrow();
                expect(() => folderPermissions.hasPermissionOrRolePrivilege('file', GcmsRolePrivilege.translatepages)).toThrow();
                expect(() => folderPermissions.hasPermissionOrRolePrivilege('image', GcmsRolePrivilege.publishpages)).toThrow();
                expect(() => folderPermissions.hasPermissionOrRolePrivilege('image', GcmsRolePrivilege.translatepages)).toThrow();
                expect(() => folderPermissions.hasPermissionOrRolePrivilege('folder', GcmsRolePrivilege.publishpages)).toThrow();
                expect(() => folderPermissions.hasPermissionOrRolePrivilege('folder', GcmsRolePrivilege.translatepages)).toThrow();
            });

            describe('file', () => {

                it('checks privileges if the permission is set to false and returns true for a granted privilege', () => {
                    expect(MOCK_FOLDER_INSTANCE_PERMISSIONS_MAP_COLLECTION.permissions.createitems).toBe(false);
                    assertHasPermOrPrivWorks('file', GcmsRolePrivilege.createitems, true);
                });

                it('checks privileges if the permission is set to false and returns false for a denied privilege', () => {
                    expect(MOCK_FOLDER_INSTANCE_PERMISSIONS_MAP_COLLECTION.permissions.deleteitems).toBe(false);
                    assertHasPermOrPrivWorks('file', GcmsRolePrivilege.deleteitems, false);
                });

                it('works if no rolePrivileges are set', () => {
                    assertHasPermOrPrivWorksWithoutRoles('file');
                });

            });

            describe('image', () => {

                it('checks privileges if the permission is set to false and returns true for a granted privilege', () => {
                    expect(MOCK_FOLDER_INSTANCE_PERMISSIONS_MAP_COLLECTION.permissions.createitems).toBe(false);
                    assertHasPermOrPrivWorks('image', GcmsRolePrivilege.createitems, true);
                });

                it('checks privileges if the permission is set to false and returns false for a denied privilege', () => {
                    expect(MOCK_FOLDER_INSTANCE_PERMISSIONS_MAP_COLLECTION.permissions.deleteitems).toBe(false);
                    assertHasPermOrPrivWorks('image', GcmsRolePrivilege.deleteitems, false);
                });

                it('works if no rolePrivileges are set', () => {
                    assertHasPermOrPrivWorksWithoutRoles('image');
                });

            });

            describe('folder', () => {

                it('checks permissions only', () => {
                    expect(MOCK_FOLDER_INSTANCE_PERMISSIONS_MAP_COLLECTION.permissions.createitems).toBe(false);
                    assertHasPermOrPrivWorks('folder', GcmsRolePrivilege.createitems, false);
                });

            });

            describe('page', () => {

                it('without language: checks privileges if the permission is set to false and returns true for a granted privilege', () => {
                    expect(MOCK_FOLDER_INSTANCE_PERMISSIONS_MAP_COLLECTION.permissions.createitems).toBe(false);
                    assertHasPermOrPrivWorks('page', GcmsRolePrivilege.createitems, true);
                });

                it('without language: checks privileges if the permission is set to false and returns false for a denied privilege', () => {
                    expect(MOCK_FOLDER_INSTANCE_PERMISSIONS_MAP_COLLECTION.permissions.deleteitems).toBe(false);
                    assertHasPermOrPrivWorks('page', GcmsRolePrivilege.deleteitems, false);
                });

                it('with language: checks language privileges if permissions and type privileges are false and returns true for a granted privilege', () => {
                    expect(MOCK_FOLDER_INSTANCE_PERMISSIONS_MAP_COLLECTION.permissions.publishpages).toBe(false);
                    expect(MOCK_FOLDER_INSTANCE_PERMISSIONS_MAP_COLLECTION.rolePermissions.page.publishpages).toBe(false);
                    assertHasPermOrPrivWorksWithLanguage(GcmsRolePrivilege.publishpages, 'en', true);
                });

                it('with language: checks language privileges if permissions and type privileges are false and returns false for a denied privilege', () => {
                    expect(MOCK_FOLDER_INSTANCE_PERMISSIONS_MAP_COLLECTION.permissions.publishpages).toBe(false);
                    expect(MOCK_FOLDER_INSTANCE_PERMISSIONS_MAP_COLLECTION.rolePermissions.page.publishpages).toBe(false);
                    assertHasPermOrPrivWorksWithLanguage(GcmsRolePrivilege.publishpages, 'de', false);
                });

                it('with language: returns false if the language does not exist', () => {
                    expect(MOCK_FOLDER_INSTANCE_PERMISSIONS_MAP_COLLECTION.permissions.publishpages).toBe(false);
                    expect(MOCK_FOLDER_INSTANCE_PERMISSIONS_MAP_COLLECTION.rolePermissions.page.publishpages).toBe(false);
                    assertHasPermOrPrivWorksWithLanguage(GcmsRolePrivilege.publishpages, 'it', false);
                });

                it('with language: works if no language specific privileges are set', () => {
                    const mapsClone = cloneDeep(MOCK_FOLDER_INSTANCE_PERMISSIONS_MAP_COLLECTION);
                    delete mapsClone.rolePermissions.pageLanguages;
                    folderPermissions = new FolderInstancePermissionsImpl(mapsClone as any, 4711, 1);

                    expect(mapsClone.permissions.publishpages).toBe(false);
                    expect(mapsClone.rolePermissions.page.publishpages).toBe(false);
                    expect(folderPermissions.hasPermissionOrRolePrivilege('page', GcmsRolePrivilege.publishpages)).toBe(false);
                });

                it('works if no rolePrivileges are set', () => {
                    assertHasPermOrPrivWorksWithoutRoles('page');
                });

            });

        });

    });

    describe('DefaultTypePermissionsFactory', () => {

        let factory: PermissionsFactory;

        beforeEach(() => {
            factory = new DefaultPermissionsFactory();
        });

        describe('createPermissionsFromMaps()', () => {

            it('works for TypePermissions', () => {
                const permissions = factory.createPermissionsFromMaps(AccessControlledType.INBOX, MOCK_PERMISSIONS_MAP_COLLECTION);
                expect(permissions).toBeTruthy();
                expect(permissions instanceof TypePermissionsImpl).toBe(true);
                expect(permissions.type).toEqual(AccessControlledType.INBOX);

                expect(permissions.hasPermission(GcmsPermission.READ)).toBe(true);
                expect(permissions.hasPermission(GcmsPermission.SET_PERMISSION)).toBe(false);
            });

            it('works for InstancePermissions with a nodeId', () => {
                const permissions = factory.createPermissionsFromMaps(AccessControlledType.NODE, MOCK_PERMISSIONS_MAP_COLLECTION, 4711, 1);
                expect(permissions).toBeTruthy();
                expect(permissions instanceof InstancePermissionsImpl).toBe(true);
                expect(permissions instanceof FolderInstancePermissionsImpl).toBe(false);
                expect(permissions.type).toEqual(AccessControlledType.NODE);
                expect(permissions.instanceId).toBe(4711);
                expect(permissions.nodeId).toBe(1);

                expect(permissions.hasPermission(GcmsPermission.READ)).toBe(true);
                expect(permissions.hasPermission(GcmsPermission.SET_PERMISSION)).toBe(false);
            });

            it('works for InstancePermissions without a nodeId', () => {
                const permissions = factory.createPermissionsFromMaps(AccessControlledType.NODE, MOCK_PERMISSIONS_MAP_COLLECTION, 4711);
                expect(permissions).toBeTruthy();
                expect(permissions instanceof InstancePermissionsImpl).toBe(true);
                expect(permissions instanceof FolderInstancePermissionsImpl).toBe(false);
                expect(permissions.type).toEqual(AccessControlledType.NODE);
                expect(permissions.instanceId).toBe(4711);
                expect(permissions.nodeId).toBeUndefined();

                expect(permissions.hasPermission(GcmsPermission.READ)).toBe(true);
                expect(permissions.hasPermission(GcmsPermission.SET_PERMISSION)).toBe(false);
            });

            it('works for FolderInstancePermissions', () => {
                const permissions =
                    factory.createPermissionsFromMaps(AccessControlledType.FOLDER, MOCK_FOLDER_INSTANCE_PERMISSIONS_MAP_COLLECTION as any, 4711, 1);
                expect(permissions).toBeTruthy();
                expect(permissions instanceof FolderInstancePermissionsImpl).toBe(true);
                expect(permissions.type).toEqual(AccessControlledType.FOLDER);
                expect(permissions.instanceId).toBe(4711);
                expect(permissions.nodeId).toBe(1);

                expect(permissions.hasPermission(GcmsPermission.READ_ITEMS)).toBe(true);
                expect(permissions.hasPermission(GcmsPermission.DELETE_ITEMS)).toBe(false);
            });

        });

    });

});
