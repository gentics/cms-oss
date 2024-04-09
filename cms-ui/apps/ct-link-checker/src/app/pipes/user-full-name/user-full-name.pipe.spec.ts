import { getExampleUserData } from '@gentics/cms-models/testing';
import { UserFullNamePipe } from './user-full-name.pipe';

const MOCK_USER = getExampleUserData();
const MOCK_USER_NAME = MOCK_USER.firstName + ' ' + MOCK_USER.lastName;

class MockEntityResolver {
    getUser = jasmine.createSpy('getUser').and.returnValue(MOCK_USER);
}

describe('UserFullNamePipe', () => {
    let pipe: UserFullNamePipe;
    let entityResolver: MockEntityResolver;

    beforeEach(() => {
        entityResolver = new MockEntityResolver();
        pipe = new UserFullNamePipe();
    });

    it('can be created', () => {
        expect(pipe).toBeDefined();
    });

    it('works with a user object', () => {
        const result = pipe.transform(MOCK_USER);
        expect(entityResolver.getUser).not.toHaveBeenCalled();
        expect(result).toEqual(MOCK_USER_NAME);
    });

    it('works with null', () => {
        const result = pipe.transform(null);
        expect(entityResolver.getUser).not.toHaveBeenCalled();
        expect(result).toEqual('');
    });

});
