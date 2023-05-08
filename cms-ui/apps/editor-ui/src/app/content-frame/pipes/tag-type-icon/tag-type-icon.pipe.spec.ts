import { StringTagPartProperty, TagPartType, TagPropertyType, TagType } from '@gentics/cms-models';
import { mockEditableObjectTag } from '../../../../testing/test-tag-editor-data.mock';
import { TagTypeIconPipe } from './tag-type-icon.pipe';

describe('TagTypeIconPipe', () => {

    let tagType: TagType;
    let pipe: TagTypeIconPipe;

    beforeEach(() => {
        const objTag = mockEditableObjectTag<StringTagPartProperty>([
            {
                type: TagPropertyType.STRING,
                typeId: TagPartType.Text,
                stringValue: 'test'
            }
        ], {
            tagType: {
                icon: 'tag.gif'
            }
        });
        tagType = objTag.tagType;

        pipe = new TagTypeIconPipe();
    });

    it('works for TagTypes with an icon', () => {
        tagType.icon = 'tag.gif';
        expect(pipe.transform(tagType)).toEqual('code');

        tagType.icon = 'url.gif';
        expect(pipe.transform(tagType)).toEqual('link');
    });

    it('works for TagTypes without an icon', () => {
        tagType.icon = '';
        expect(pipe.transform(tagType)).toEqual('');

        delete tagType.icon;
        expect(pipe.transform(tagType)).toEqual('');
    });

    it('works with null and undefined', () => {
        expect(pipe.transform(null)).toEqual('');
        expect(pipe.transform(undefined)).toEqual('');
    });

});
