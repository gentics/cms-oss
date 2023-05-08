
/**
 * Mocks the most commonly used part of the ErrorHandler service.
 */
export class MockErrorHandler {
    catch = jasmine.createSpy('ErrorHandler.catch');
}
