import {AccessControlledType} from '@gentics/cms-models';
import {Observable, of as observableOf} from 'rxjs';

import {MockApiBase} from '../base/api-base.mock';
import {PermissionApi} from './permission-api';

describe('PermissionApi', () => {

    const FOLDERID = 1234;
    const NODEID = 5678;

    let permissionApi: PermissionApi;
    let apiBase: MockApiBase;
    beforeEach(() => {
        apiBase = new MockApiBase();
        permissionApi = new PermissionApi(apiBase as any);
    });

    it('getPermissionsForType sends the correct GET request', () => {
        const type = AccessControlledType.GROUP_ADMIN;
        permissionApi.getPermissionsForType(type);
        expect(apiBase.get).toHaveBeenCalledWith(`perm/${type}?map=true`);
    });

    it('getPermissionsForInstance sends the correct GET request without a nodeId', () => {
        const type = AccessControlledType.FOLDER;
        permissionApi.getPermissionsForInstance(type, FOLDERID);
        expect(apiBase.get).toHaveBeenCalledWith(`perm/${type}/${FOLDERID}?map=true`, undefined);
    });

    it('getPermissionsForInstance sends the correct GET request with a nodeId', () => {
        const type = AccessControlledType.FOLDER;
        permissionApi.getPermissionsForInstance(type, FOLDERID, NODEID);
        expect(apiBase.get).toHaveBeenCalledWith(`perm/${type}/${FOLDERID}?map=true`, { nodeId: NODEID });
    });

    it('getFolderPermission sends the correct GET request', () => {
        permissionApi.getFolderPermissions(FOLDERID, NODEID);
        expect(apiBase.get).toHaveBeenCalledWith(`perm/10002/${FOLDERID}?map=true`, { nodeId: NODEID });
    });

    describe('getInboxPermissions', () => {

        it('sends the correct GET request', () => {
            permissionApi.getInboxPermissions();
            expect(apiBase.get).toHaveBeenCalledWith(`perm/17/0`);
        });

        it('maps the permission bits of the response to the correct flags', () => {
            apiBase.get = () => observableOf({ perm: '10000000000000000000000000000000' });
            expect(firstValue(permissionApi.getInboxPermissions())).toEqual({
                view: true,
                assignPermissions: false,
                instantMessages: false
            });

            apiBase.get = () => observableOf({ perm: '01000000000000000000000000000000' });
            expect(firstValue(permissionApi.getInboxPermissions())).toEqual({
                view: false,
                assignPermissions: true,
                instantMessages: false
            });

            apiBase.get = () => observableOf({ perm: '00100000000000000000000000000000' });
            expect(firstValue(permissionApi.getInboxPermissions())).toEqual({
                view: false,
                assignPermissions: false,
                instantMessages: true
            });
        });

    });

    it('getNodePermissions sends the correct GET request', () => {
        permissionApi.getNodePermissions(NODEID);
        expect(apiBase.get).toHaveBeenCalledWith(`perm/10001/${NODEID}?map=true`);
    });

    describe('getPublishQueuePermissions', () => {

        it('sends the correct GET request', () => {
            permissionApi.getPublishQueuePermissions();
            expect(apiBase.get).toHaveBeenCalledWith(`perm/10013/0`);
        });

        it('maps the permission bits of the response to the correct flags', () => {
            apiBase.get = () => observableOf({ perm: '10000000000000000000000000000000' });
            expect(firstValue(permissionApi.getPublishQueuePermissions())).toEqual({
                view: true,
                assignPermissions: false
            });

            apiBase.get = () => observableOf({ perm: '01000000000000000000000000000000' });
            expect(firstValue(permissionApi.getPublishQueuePermissions())).toEqual({
                view: false,
                assignPermissions: true
            });
        });

    });

});

function firstValue<T>(o: Observable<T>): T {
    let value: T;
    let sub = o.subscribe(v => value = v);
    sub.unsubscribe();
    return value;
}
