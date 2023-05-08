import { ObjectTag, StringTagPartProperty, TagPartType, TagPropertyType } from '@gentics/cms-models';
import { mockEditableObjectTag } from '../../../../testing/test-tag-editor-data.mock';
import { ObjectTagNamePipe } from './object-tag-name.pipe';

describe('ObjectTagNamePipe', () => {

    let objTag: ObjectTag;
    let pipe: ObjectTagNamePipe;

    beforeEach(() => {
        objTag = mockEditableObjectTag<StringTagPartProperty>([
            {
                type: TagPropertyType.STRING,
                typeId: TagPartType.Text,
                stringValue: 'test'
            }
        ], {
            displayName: 'Test Object Tag'
        });

        pipe = new ObjectTagNamePipe();
    });

    it('works for not required ObjectTags', () => {
        objTag.required = false;
        const expectedLabel = objTag.displayName;
        expect(pipe.transform(objTag)).toEqual(expectedLabel);
    });

    it('adds an asterisk for required ObjectTags', () => {
        objTag.required  = true;
        const expectedLabel = objTag.displayName + ' *';
        expect(pipe.transform(objTag)).toEqual(expectedLabel);
    });

    it('works with null and undefined', () => {
        expect(pipe.transform(null)).toEqual('');
        expect(pipe.transform(undefined)).toEqual('');
    });

});
