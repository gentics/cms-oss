import TestService from '../services/TestService';

test('vergleicht laenge vom String, von TestService', () => {
  expect(TestService.getExampleData().length).toBe(12);
});
