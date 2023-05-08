import { isLiveUrl } from './is-live-url';

describe('isLiveUrl', () => {
    const hosts = [
        'www.example.com',
        'www.gentics.com',
        'domain.tld',
    ];
    const tests = [
        {
            expects: true,
            description:
                'starting with http and host contained in provided hosts array',
            value: 'http://www.example.com/dir/page.html',
        },
        {
            expects: true,
            description:
                'starting with https and host contained in provided hosts array',
            value: 'https://www.example.com/dir/page.html',
        },
        {
            expects: true,
            description:
                'starting with http and host contained in provided hosts array with path without file extension',
            value: 'http://www.example.com/page',
        },
        {
            expects: true,
            description:
                'starting with http and host contained in provided hosts array with path containing directory without file extension',
            value: 'http://www.example.com/dir/page',
        },
        {
            expects: true,
            description:
                'containing host contained in provided hosts array with path containing directory without file extension',
            value: 'http://www.example.com/dir/page',
        },
        {
            expects: true,
            description:
                'without protocol and host contained in provided hosts array',
            value: 'www.example.com/dir/page.html',
        },
        {
            expects: true,
            description:
                'without protocol and host contained in provided hosts array, path with directory',
            value: 'www.example.com/dir/page.html',
        },
        {
            expects: true,
            description:
                'without protocol, host contained in provided hosts array, path with directory, no file extension',
            value: 'www.example.com/dir/page',
        },
        {
            expects: true,
            description:
                'without protocol, host contained in provided hosts array, no file extension',
            value: 'www.example.com/page',
        },
        {
            expects: true,
            description: 'only starting with http',
            value: 'http',
        },
        {
            expects: true,
            description:
                'with protocol, host not contained in provided hosts array, no file extension',
            value: 'http://www.bad.com/page',
        },
        {
            expects: false,
            description:
                'without protocol, host contained in provided hosts array, no path',
            value: 'www.example.com/',
        },
        {
            expects: false,
            description:
                'without protocol, host contained in provided hosts array, no path',
            value: 'www.example.com',
        },
        {
            expects: false,
            description:
                'without protocol, host contained in provided hosts array, only slash',
            value: 'www.example.com/',
        },
        {
            expects: false,
            description:
                'without protocol, host not contained in provided hosts array',
            value: 'www.bad.com/dir/path.html',
        },
        {
            expects: false,
            description:
                'without protocol, host not contained in provided hosts array, no file extension',
            value: 'www.bad.com/dir/path',
        },
        {
            expects: false,
            description:
                'without protocol, host not contained in provided hosts array, no directory',
            value: 'www.bad.com/path',
        },
    ];

    tests.forEach(test => {
        it(`should return ${test.expects} for '${test.value}' - ${test.description}`, () => {
            expect(isLiveUrl(test.value, hosts)).toBe(test.expects);
        })
    })

});
