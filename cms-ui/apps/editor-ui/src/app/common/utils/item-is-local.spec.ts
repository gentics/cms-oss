import {itemIsLocal} from './item-is-local';
import {createInheritedItem, createLocalItem, createLocalizedItem, itemWithoutInheritanceIds} from '../../../testing/item-fixtures';


describe('itemIsLocal', () => {

    describe('with Content.Node >= 5.27.0', () => {

        it('returns true for a local, non-inherited item', () => {
            const localItem = createLocalItem();
            const actual = itemIsLocal(localItem);
            expect(actual).toBe(true);
        });

        it('returns false for an inherited item', () => {
            const inheritedItem = createInheritedItem();
            const actual = itemIsLocal(inheritedItem);
            expect(actual).toBe(false);
        });

        it('returns false for a localized item', () => {
            const localizedItem = createLocalizedItem();
            const actual = itemIsLocal(localizedItem);
            expect(actual).toBe(false);
        });

    });

    describe('with ContentNode 5.26.x', () => {

        it('returns true for a local, non-inherited item', () => {
            const localItem = itemWithoutInheritanceIds(createLocalItem());
            const actual = itemIsLocal(localItem);
            expect(actual).toBe(true);
        });

        it('returns false for an inherited item', () => {
            const inheritedItem = itemWithoutInheritanceIds(createInheritedItem());
            const actual = itemIsLocal(inheritedItem);
            expect(actual).toBe(false);
        });

        it('returns false for a localized item', () => {
            const localizedItem = itemWithoutInheritanceIds(createLocalizedItem());
            const actual = itemIsLocal(localizedItem);
            expect(actual).toBe(false);
        });

    });

    describe('unexpected input', () => {

        it('does not throw for null or undefined input', () => {
            expect(itemIsLocal(undefined as any)).toBe(false, 'undefined');
            expect(itemIsLocal(null as any)).toBe(false, 'null');
        });

    });

});
