import { TagPart } from '@gentics/cms-models';
import { getExampleEditableTag } from '../../../../testing/test-tag-editor-data.mock';
import { TagPropertyLabelPipe } from './tag-property-label.pipe';

describe('TagPropertyLabelPipe', () => {

    let tagPart: TagPart;
    let pipe: TagPropertyLabelPipe;

    beforeEach(() => {
        const tag = getExampleEditableTag();
        tagPart = tag.tagType.parts[0];
        pipe = new TagPropertyLabelPipe();
    });

    it('works for non-mandatory TagParts', () => {
        tagPart.mandatory = false;
        const expectedLabel = tagPart.name;
        expect(pipe.transform(tagPart)).toEqual(expectedLabel);
    });

    it('adds an asterisk for mandatory TagParts', () => {
        tagPart.mandatory = true;
        const expectedLabel = tagPart.name + ' *';
        expect(pipe.transform(tagPart)).toEqual(expectedLabel);
    });

    it('works with null and undefined', () => {
        expect(pipe.transform(null)).toEqual('');
        expect(pipe.transform(undefined)).toEqual('');
    });

});
