/* eslint-disable no-underscore-dangle */

import { arrayEqual, extractFormatting } from './utils';

/**
 * Registers common chai assertions
 *
 * * `formatting`
 * * `included`
 */
export function registerCommonAssertions(): void {
    function validateElementObject(ref: Chai.AssertionStatic): HTMLElement | undefined {
        let elem: HTMLElement;

        if (ref._obj == null || typeof ref._obj !== 'object') {
            ref.assert(
                false,
                'expected #{this} to be a HTMLElement or jQuery element',
                'expected #{this} to be a HTMLElement or jQuery element',
                ref._obj,
            );
            return;
        }

        // Treat it as jQuery element
        if (typeof ref._obj.length === 'number') {
            if (ref._obj.lenght === 0) {
                ref.assert(
                    false,
                    'expected #{this} jQuery element to reference a HTMLElement',
                    'expected #{this} jQuery element to reference a HTMLElement',
                    1,
                    ref._obj.length,
                );
                return;
            } else if (ref._obj.length > 1) {
                ref.assert(
                    false,
                    'expected #{this} jQuery element to reference a single HTMLElement',
                    'expected #{this} jQuery element to reference a single HTMLElement',
                    1,
                    ref._obj.length,
                );
                return;
            } else {
                elem = ref._obj[0];
            }
        } else {
            elem = ref._obj;
        }

        // Safety check
        if (elem == null || typeof elem !== 'object') {
            ref.assert(
                false,
                'expected #{this} to be a HTMLElement or jQuery element',
                'expected #{this} to be a HTMLElement or jQuery element',
                ref._obj,
            );
            return;
        // Safety check
        } else if (elem.nodeType !== Node.ELEMENT_NODE) {
            ref.assert(
                false,
                'expected #{this} to be an HTMLElement',
                'expected #{this} to be an HTMLElement',
                ref._obj,
            );
            return;
        }

        return elem;
    }

    chai.use((ref, _utils) => {
        ref.Assertion.addMethod('formatting', function assertHaveFormatting(text: string, formats: string[]) {
            const elem = validateElementObject(this);
            if (elem == null) {
                return;
            }

            const availableFormats = extractFormatting(elem);
            const hasFormat = availableFormats.some(entry => {
                return (entry.text === text || entry.text.trim() === text)
                    && arrayEqual(entry.formats, formats);
            });

            this.assert(
                hasFormat,
                `expected #{act} to have a formatted text "${text}" with [${formats.join(', ')}]`,
                `expected #{act} to not have a formatted text "${text}" with [${formats.join(', ')}]`,
                { text, formats },
                JSON.stringify(availableFormats),
            );
        });

        ref.Assertion.addMethod('included', function assertToBeIncluded(array: any[]) {
            if (!Array.isArray(array)) {
                this.assert(
                    false,
                    'expected paramter #{act} to be an Array',
                    'expected paramter #{act} to be an Array',
                    array,
                );
                return;
            }

            const objIsStr = typeof this._obj === 'string'
            this.assert(
                array.some(arrVal => {
                    return objIsStr && typeof arrVal === 'string' ? arrVal.includes(this._obj) : arrVal === this._obj;
                }),
                'expected #{this} to be included in #{act}',
                'expected #{this} not to included in #{act}',
                JSON.stringify([this._obj]),
                JSON.stringify(array),
            );
        });

        ref.Assertion.addMethod('displayed', function assertElementIsDislayed(options?: IntersectionObserverInit) {
            const elem = validateElementObject(this);
            if (elem == null) {
                return;
            }

            const observer = new IntersectionObserver(snapshot => {
                if (snapshot.length !== 1) {
                    return;
                }

                this.assert(
                    snapshot[0].isIntersecting,
                    'expected #{this} to be displayed/visible to the user',
                    'expected #{this} to not be displayed/visible to the user',
                    true,
                );

                observer.disconnect();
            }, options);

            observer.observe(elem);
        });
    });
}
