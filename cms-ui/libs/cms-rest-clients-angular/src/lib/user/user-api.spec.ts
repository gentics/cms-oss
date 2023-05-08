import { UserGroupsRequestOptions, UserListOptions, UserRequestOptions, UserUpdateRequest } from '@gentics/cms-models';
import { MockApiBase } from '../util/api-base.mock';
import { UserApi } from './user-api';

const USER_ID = 4711;
const GROUP_ID = 815;

describe('UserApi', () => {

    let apiBase: MockApiBase;
    let userApi: UserApi;

    beforeEach(() => {
        apiBase = new MockApiBase();
        userApi = new UserApi(apiBase as any);
    });

    it('getUsers() sends the correct GET request', () => {
        userApi.getUsers();
        expect(apiBase.get).toHaveBeenCalledWith(`user/list`, undefined);
    });

    it('getUsers() sends the correct GET request with query parameters', () => {
        const options: UserListOptions = {
            firstname: 'Test',
        };
        userApi.getUsers(options);
        expect(apiBase.get).toHaveBeenCalledWith(`user/list`, options);
    });

    it('getUser() sends the correct GET request', () => {
        userApi.getUser(USER_ID);
        expect(apiBase.get).toHaveBeenCalledWith(`user/${USER_ID}`, undefined);
    });

    it('getUser() sends the correct GET request with query parameters', () => {
        const options: UserRequestOptions = {
            groups: true,
        };
        userApi.getUser(USER_ID, options);
        expect(apiBase.get).toHaveBeenCalledWith(`user/${USER_ID}`, options);
    });

    it('updateUser() sends the correct PUT request', () => {
        const update: Partial<UserUpdateRequest> = {
            email: 'test@gentics.com',
        };
        userApi.updateUser(USER_ID, update as any);
        expect(apiBase.put).toHaveBeenCalledWith(`user/${USER_ID}`, update);
    });

    it('deleteUser() sends the correct DELETE request', () => {
        userApi.deleteUser(USER_ID);
        expect(apiBase.delete).toHaveBeenCalledWith(`user/${USER_ID}`);
    });

    it('getUserGroups() sends the correct GET request', () => {
        userApi.getUserGroups(USER_ID);
        expect(apiBase.get).toHaveBeenCalledWith(`user/${USER_ID}/groups`, undefined);
    });

    it('getUserGroups() sends the correct GET request', () => {
        userApi.getUserGroups(USER_ID);
        expect(apiBase.get).toHaveBeenCalledWith(`user/${USER_ID}/groups`, undefined);
    });

    it('getUserGroups() sends the correct GET request with query parameters', () => {
        const options: UserGroupsRequestOptions = {
            page: 2,
            pageSize: 20,
        };
        userApi.getUserGroups(USER_ID, options);
        expect(apiBase.get).toHaveBeenCalledWith(`user/${USER_ID}/groups`, options);
    });

    it('getUserNodeRestrictions() sends the correct GET request with query parameters', () => {
        userApi.getUserNodeRestrictions(USER_ID, GROUP_ID);
        expect(apiBase.get).toHaveBeenCalledWith(`user/${USER_ID}/groups/${GROUP_ID}/nodes`);
    });

    it('addUserNodeRestrictions() sends the correct PUT request', () => {
        userApi.addUserNodeRestrictions(USER_ID, GROUP_ID);
        expect(apiBase.put).toHaveBeenCalledWith(`user/${USER_ID}/groups/${GROUP_ID}/nodes`, {});
    });

    it('removeUserNodeRestrictions() sends the correct DELETE request', () => {
        userApi.removeUserNodeRestrictions(USER_ID, GROUP_ID);
        expect(apiBase.delete).toHaveBeenCalledWith(`user/${USER_ID}/groups/${GROUP_ID}/nodes`);
    });

});
