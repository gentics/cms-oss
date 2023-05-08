import {itemIsLocalized} from './item-is-localized';
import {createLocalItem, createInheritedItem, createLocalizedItem, itemWithoutInheritanceIds} from '../../../testing/item-fixtures';
import {itemIsLocal} from './item-is-local';


describe('itemIsLocalized', () => {

    describe('with Content.Node >= 5.27.0', () => {

        it('returns false for a local, non-inherited item', () => {
            const localItem = createLocalItem();
            const actual = itemIsLocalized(localItem);
            expect(actual).toBe(false);
        });

        it('returns false for an inherited item', () => {
            const inheritedItem = createInheritedItem();
            const actual = itemIsLocalized(inheritedItem);
            expect(actual).toBe(false);
        });

        it('returns true for a localized item', () => {
            const localizedItem = createLocalizedItem();
            const actual = itemIsLocalized(localizedItem);
            expect(actual).toBe(true);
        });

    });

    describe('with ContentNode 5.26.x', () => {

        it('returns false for a local, non-inherited item', () => {
            const localItem = itemWithoutInheritanceIds(createLocalItem());
            const actual = itemIsLocalized(localItem);
            expect(actual).toBe(false);
        });

        it('returns false for an inherited item', () => {
            const inheritedItem = itemWithoutInheritanceIds(createInheritedItem());
            const actual = itemIsLocalized(inheritedItem);
            expect(actual).toBe(false);
        });

        it('returns true for a localized item', () => {
            const localizedItem = itemWithoutInheritanceIds(createLocalizedItem());
            const actual = itemIsLocalized(localizedItem);
            expect(actual).toBe(true);
        });

    });

    describe('unexpected input', () => {

        it('does not throw for null or undefined input', () => {
            expect(itemIsLocal(undefined as any)).toBe(false, 'undefined');
            expect(itemIsLocal(null as any)).toBe(false, 'null');
        });
    });

});
