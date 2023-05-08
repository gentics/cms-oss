import { DEFAULT_COMPARE_FN, IncludesPipe } from './includes.pipe';

function printable(value: any): string {
    if (value != null && value.__proto__ === Set.prototype) {
        return JSON.stringify(Array.from(value));
    } else {
        return JSON.stringify(value);
    }
}

describe('IncludesPipe', () => {
    const instance = new IncludesPipe();

    it('should detect the options and apply them', () => {

        const testSet = [
            {
                strict: true,
                source: ['hello', 'world'],
                value: ['world'],
                result: true,
                callTimes: 2,
            },
        ];

        for (const options of testSet) {
            let wasStrictInFn: boolean | null = null;
            let callCount = 0;

            const compareFn = (a, b, strict) => {
                callCount++;
                wasStrictInFn = strict;
                return DEFAULT_COMPARE_FN(a, b, strict);
            };

            const result = instance.transform(options.source, {
                values: options.value,
                strict: options.strict,
                fn: compareFn,
            });

            expect(wasStrictInFn).toEqual(options.strict);
            (expect(result) as any).withContext(`Source: "${printable(options.source)}", Value: "${printable(options.value)}"`).toEqual(options.result);
            expect(callCount).toEqual(options.callTimes);
        }
    });

    it('should detect regular values', () => {
        const testSet = [
            {
                source: new Set(['hello', 'world']),
                value: 'world',
                result: true,
            },
            {
                source: new Set(['hello', 'world']),
                value: new Set(['world']),
                result: true,
            },
            {
                source: new Set(['hello', 'world']),
                value: ['world'],
                result: true,
            },
            {
                source: new Set(['hello', 'world']),
                value: 123,
                result: false,
            },
            {
                source: ['hello', 'world'],
                value: 'hello',
                result: true,
            },
            {
                source: ['hello', 'world'],
                value: new Set(['hello']),
                result: true,
            },
            {
                source: ['hello', 'world'],
                value: ['hello'],
                result: true,
            },
            {
                source: ['hello', 'world'],
                value: 'foobar',
                result: false,
            },
            {
                source: {
                    foobar: 'test',
                    123: 'zigzag',
                },
                value: 'foobar',
                result: true,
            },
            {
                source: {
                    foobar: 'test',
                    123: 'zigzag',
                },
                value: new Set(['foobar']),
                result: true,
            },
            {
                source: {
                    foobar: 'test',
                    123: 'zigzag',
                },
                value: ['foobar'],
                result: true,
            },
            {
                source: {
                    foobar: 'test',
                    123: 'zigzag',
                },
                value: 123,
                result: false,
            },
            {
                source: {
                    foobar: 'test',
                    123: 'zigzag',
                },
                value: '123',
                result: true,
            },
            {
                source: {
                    foobar: 'test',
                    123: 'zigzag',
                },
                value: 'something else',
                result: false,
            },
        ];

        for (const options of testSet) {
            const result = instance.transform(options.source, options.value as any);
            (expect(result) as any).withContext(`Source: "${printable(options.source)}", Value: "${printable(options.value)}"`).toEqual(options.result);
        }
    })
});
