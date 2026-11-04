import { getExampleUserData } from '@gentics/cms-models/testing/test-data.mock';
import { UserFullNamePipe } from './user-full-name.pipe';
import { I18nService } from '@gentics/cms-components';

const MOCK_USER = getExampleUserData();
const MOCK_USER_NAME = MOCK_USER.firstName + ' ' + MOCK_USER.lastName;

describe('UserFullNamePipe', () => {

    let pipe: UserFullNamePipe;
    let entityResolver: MockEntityResolver;

    beforeEach(() => {
        entityResolver = new MockEntityResolver();
        pipe = new UserFullNamePipe(entityResolver as any, new MockI18nService() as any);
    });

    it('can be created', () => {
        expect(pipe).toBeDefined();
    });

    it('works with a user ID', () => {
        const result = pipe.transform(MOCK_USER.id);
        expect(entityResolver.getUser).toHaveBeenCalledTimes(1);
        expect(entityResolver.getUser).toHaveBeenCalledWith(MOCK_USER.id);
        expect(result).toEqual(MOCK_USER_NAME);
    });

    it('works with a user object', () => {
        const result = pipe.transform(MOCK_USER);
        expect(entityResolver.getUser).not.toHaveBeenCalled();
        expect(result).toEqual(MOCK_USER_NAME);
    });

    it('works with null', () => {
        const result = pipe.transform(null);
        expect(entityResolver.getUser).not.toHaveBeenCalled();
        expect(result).toEqual('editor.user_unknown');
    });

    it('works with a user ID that is not in the entity state', () => {
        entityResolver.getUser.and.returnValue(undefined);
        const result = pipe.transform(MOCK_USER.id);
        expect(entityResolver.getUser).toHaveBeenCalledTimes(1);
        expect(entityResolver.getUser).toHaveBeenCalledWith(MOCK_USER.id);
        expect(result).toEqual('editor.user_unknown');
    });

    it('works with null (skip unknown)', () => {
        const result = pipe.transform(null, true);
        expect(entityResolver.getUser).not.toHaveBeenCalled();
        expect(result).toEqual(null);
    });

    it('works with a user ID that is not in the entity state (skip unknown)', () => {
        entityResolver.getUser.and.returnValue(undefined);
        const result = pipe.transform(MOCK_USER.id, true);
        expect(entityResolver.getUser).toHaveBeenCalledTimes(1);
        expect(entityResolver.getUser).toHaveBeenCalledWith(MOCK_USER.id);
        expect(result).toEqual(null);
    });

});

class MockEntityResolver {
    getUser = jasmine.createSpy('getUser').and.returnValue(MOCK_USER);
}

class MockI18nService implements Partial<I18nService> {
    instant = jasmine.createSpy('instant').and.callFake((key) => key);
}
