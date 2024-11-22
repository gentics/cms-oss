/* eslint-disable no-underscore-dangle */

import { arrayEqual, extractFormatting } from './utils';

/**
 * Registers common chai assertions
 *
 * * `formatting`
 * * `included`
 */
export function registerCommonAssertions(): void {
    function validateElementObject(ref: Chai.AssertionStatic): HTMLElement[] | undefined {
        let elements: HTMLElement[] | null = null;

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
            }

            elements = Array.from(ref._obj);
        } else {
            elements = [ref._obj];
        }

        // Safety check
        if (elements == null) {
            ref.assert(
                false,
                'expected #{this} to be a HTMLElement or jQuery element',
                'expected #{this} to be a HTMLElement or jQuery element',
                ref._obj,
            );
            return;
        }

        for (const el of elements) {
            ref.assert(
                el.nodeType === Node.ELEMENT_NODE,
                'expected #{this} to be an HTMLElement',
                'expected #{this} to be an HTMLElement',
                el,
            );
        }

        return elements;
    }

    chai.use((ref, _utils) => {
        ref.Assertion.addMethod('formatting', function assertHaveFormatting(text: string, formats: string[]) {
            const elements = validateElementObject(this);
            if (elements == null) {
                return;
            }

            for (const el of elements) {
                const availableFormats = extractFormatting(el);
                const hasFormat = availableFormats.some(entry => {
                    return (entry.text === text || entry.text.trim() === text)
                        && arrayEqual(entry.formats, formats);
                });

                this.assert(
                    hasFormat,
                    `expected #{act} to have a formatted text "${text}" with [${formats.join(', ')}]`,
                    `expected #{act} to not have a formatted text "${text}" with [${formats.join(', ')}]`,
                    { text, formats },
                    el.innerHTML,
                );
            }
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
            const elements = validateElementObject(this);
            if (elements == null) {
                return;
            }

            const observer = new IntersectionObserver(snapshots => {
                // Only validate once all snapshots are available
                if (snapshots.length !== elements.length) {
                    return;
                }

                for (const snap of snapshots) {
                    this.assert(
                        snap.isIntersecting,
                        'expected #{act} to be displayed/visible to the user',
                        'expected ${act} to not be displayed/visible to the user',
                        snap.target,
                    );
                }

                observer.disconnect();
            }, options);

            for (const el of elements) {
                observer.observe(el);
            }
        });
    });
}
