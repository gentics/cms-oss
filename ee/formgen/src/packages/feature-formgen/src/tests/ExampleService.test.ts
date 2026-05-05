// sum.test.ts
import { testSum } from '../services/ExampleService';

test('addiert 1 + 2 und ergibt 3', () => {
  expect(testSum(1, 2)).toBe(3);
});
