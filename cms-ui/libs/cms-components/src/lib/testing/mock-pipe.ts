import { Pipe } from '@angular/core';

/**
 * Generates an identity pipe with the passed name
 * for unit tests which returns its input as output.
 */
export function mockPipe(name: string): any {
    return Pipe({ name })(
        class MockPipe {
            transform(input: any): any {
                return input;
            }
        },
    );
}

/**
 * Generates multiple identity pipes for unit tests.
 */
export function mockPipes(...names: string[]): any[] {
    return names.map(mockPipe);
}
