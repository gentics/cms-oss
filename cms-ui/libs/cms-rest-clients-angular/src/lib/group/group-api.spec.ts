import {
    AccessControlledType,
    GcmsPermission,
    Group,
    GroupCreateRequest,
    GroupListOptions,
    GroupPermissionsListOptions,
    GroupSetPermissionsRequest,
    GroupUserCreateRequest,
    Omit,
} from '@gentics/cms-models';
import { MockApiBase } from '../util/api-base.mock';
import { GroupApi } from './group-api';

const GROUP_ID = 2;
const PARENT_GROUP_ID = 3;
const SUBGROUP_ID = 4;
const USER_ID = 4711;
const TYPE = AccessControlledType.ADMIN;
const INSTANCE_ID = 1234;

const MOCK_SET_PERM_REQ: GroupSetPermissionsRequest = {
    perms: [ { type: GcmsPermission.CREATE, value: true } ],
    roles: [ { id: 2, value: true } ],
    subGroups: false,
    subObjects: true,
};

describe('GroupApi', () => {

    let apiBase: MockApiBase;
    let groupApi: GroupApi;

    beforeEach(() => {
        apiBase = new MockApiBase();
        groupApi = new GroupApi(apiBase as any);
    });

    it('getGroups() sends the correct GET request', () => {
        groupApi.getGroupsTree();
        expect(apiBase.get).toHaveBeenCalledWith(`group/load`);
    });

    it('getGroupsFlattned() sends the correct GET request', () => {
        groupApi.listGroups();
        expect(apiBase.get).toHaveBeenCalledWith(`group/list`, undefined);
    });

    it('getGroupsFlattned() sends the correct GET request with query parameters', () => {
        const options: GroupListOptions = {
            sortby: 'name',
            sortorder: 'asc',
        };
        groupApi.listGroups(options);
        expect(apiBase.get).toHaveBeenCalledWith(`group/list`, options);
    });

    it('getGroup() sends the correct GET request', () => {
        groupApi.getGroup(GROUP_ID);
        expect(apiBase.get).toHaveBeenCalledWith(`group/${GROUP_ID}`);
    });

    it('getSubgroups() sends the correct GET request', () => {
        groupApi.getSubgroups(GROUP_ID);
        expect(apiBase.get).toHaveBeenCalledWith(`group/${GROUP_ID}/groups`);
    });

    it('createSubgroup() sends the correct PUT request', () => {
        const subgroup: GroupCreateRequest = {
            name: 'Test',
            description: 'Test Group',
        };
        groupApi.createSubgroup(PARENT_GROUP_ID, subgroup);
        expect(apiBase.put).toHaveBeenCalledWith(`group/${PARENT_GROUP_ID}/groups`, subgroup);
    });

    it('moveSubgroup() sends the correct PUT request', () => {
        groupApi.moveSubgroup(SUBGROUP_ID, PARENT_GROUP_ID);
        expect(apiBase.put).toHaveBeenCalledWith(`group/${PARENT_GROUP_ID}/groups/${SUBGROUP_ID}`, {});
    });

    it('deleteGroup() sends the correct DELETE request', () => {
        groupApi.deleteGroup(GROUP_ID);
        expect(apiBase.delete).toHaveBeenCalledWith(`group/${GROUP_ID}`);
    });

    it('updateGroup() sends the correct POST request', () => {
        const update: Omit<Group, 'id' | 'children'> = {
            name: 'New Name',
            description: 'New Description',
        };
        groupApi.updateGroup(GROUP_ID, update);
        expect(apiBase.post).toHaveBeenCalledWith(`group/${GROUP_ID}`, update);
    });

    it('createUser() sends the correct PUT request', () => {
        const user: Partial<GroupUserCreateRequest> = {
            login: 'testUser',
            firstName: 'Test',
            lastName: 'Tester',
        };
        groupApi.createUser(GROUP_ID, user as any);
        expect(apiBase.put).toHaveBeenCalledWith(`group/${GROUP_ID}/users`, user);
    });

    it('addUserToGroup() sends the correct PUT request', () => {
        groupApi.addUserToGroup(GROUP_ID, USER_ID);
        expect(apiBase.put).toHaveBeenCalledWith(`group/${GROUP_ID}/users/${USER_ID}`, {});
    });

    it('removeUserFromGroup() sends the correct DELETE request', () => {
        groupApi.removeUserFromGroup(GROUP_ID, USER_ID);
        expect(apiBase.delete).toHaveBeenCalledWith(`group/${GROUP_ID}/users/${USER_ID}`);
    });

    it('getGroupPermissions() sends the correct GET request', () => {
        groupApi.getGroupPermissions(GROUP_ID);
        expect(apiBase.get).toHaveBeenCalledWith(`group/${GROUP_ID}/perms`, undefined);
    });

    it('getGroupPermissions() sends the correct GET request', () => {
        const options: GroupPermissionsListOptions = {
            parentId: 10,
        };
        groupApi.getGroupPermissions(GROUP_ID, options);
        expect(apiBase.get).toHaveBeenCalledWith(`group/${GROUP_ID}/perms`, options);
    });

    it('getGroupTypePermissions() sends the correct GET request', () => {
        groupApi.getGroupTypePermissions(GROUP_ID, TYPE);
        expect(apiBase.get).toHaveBeenCalledWith(`group/${GROUP_ID}/perms/${TYPE}`);
    });

    it('getGroupInstancePermissions() sends the correct GET request', () => {
        groupApi.getGroupInstancePermissions(GROUP_ID, TYPE, INSTANCE_ID);
        expect(apiBase.get).toHaveBeenCalledWith(`group/${GROUP_ID}/perms/${TYPE}/${INSTANCE_ID}`);
    });

    it('setGroupTypePermissions() sends the correct POST request', () => {
        groupApi.setGroupTypePermissions(GROUP_ID, TYPE, MOCK_SET_PERM_REQ);
        expect(apiBase.post).toHaveBeenCalledWith(`group/${GROUP_ID}/perms/${TYPE}`, MOCK_SET_PERM_REQ);
    });

    it('setGroupInstancePermissions() sends the correct POST request', () => {
        groupApi.setGroupInstancePermissions(GROUP_ID, TYPE, INSTANCE_ID, MOCK_SET_PERM_REQ);
        expect(apiBase.post).toHaveBeenCalledWith(`group/${GROUP_ID}/perms/${TYPE}/${INSTANCE_ID}`, MOCK_SET_PERM_REQ);
    });

});
