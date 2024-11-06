import { TagChangedFn, TagEditorContext } from '@gentics/cms-integration-api-models';
import { StringTagPartProperty, TagPropertyMap } from '@gentics/cms-models';
import { cloneDeep } from 'lodash-es';
import { getExampleValidationFailed, getExampleValidationSuccess } from '../../../../testing/test-tag-editor-data.mock';

/**
 * TODO: Implement tests after feature release in Dec 2018.
 */
describe('CustomTagEditorHostComponent', () => {

    it('loads an external editor in an IFrame and initializes it properly (including calling registerOnSizeChange())', () => {});

    it('forwards editTag() Promise.resolve() correctly from the IFrame to the TagEditorService', () => {});

    it('forwards editTag() Promise.reject() correctly from the IFrame to the TagEditorService', () => {});

    it('displays an error message if there is an error loading the IFrame and displays a "Close" button, which rejects the Promise', () => {});

    it('forwards editTagLive() onChangeFn calls if all properties are valid', () => {});

    it('does not forward editTagLive() onChangeFn calls if not all properties are valid', () => {});

    it('simluates editTag() if the CustomTagEditor does not support it natively', () => {});

    it('onSizeChangeFn updates the size of the IFrame', () => {});

});

/**
 * Tests if onChangeFn calls are validated and passed on correctly.
 */
function testOnChangeFnCalls(context: TagEditorContext, onChangeFn: TagChangedFn, reportedChangedStates: TagPropertyMap[]): void {
    const tagValidatorSpy = spyOn(context.validator, 'validateAllTagProperties').and.returnValue(getExampleValidationSuccess() as any);
    const checkValidationSpy = (expectedState) => {
        expect(tagValidatorSpy).toHaveBeenCalledTimes(1);
        expect(tagValidatorSpy).toHaveBeenCalledWith(expectedState);
        tagValidatorSpy.calls.reset();
    };

    // Set up the changes.
    const changedProperties = cloneDeep(context.editedTag.properties);
    (changedProperties['property0'] as StringTagPartProperty).stringValue = 'modified value0';
    const changedState0 = cloneDeep(changedProperties);
    const changedState1 = null;
    (changedProperties['property1'] as StringTagPartProperty).stringValue = 'modified value1';
    const changedState2 = cloneDeep(changedProperties);

    // Make sure that the onChangeFn calls are passed on correctly.
    onChangeFn(changedState0);
    expect(reportedChangedStates.length).toBe(1);
    expect(reportedChangedStates[0]).toEqual(changedState0);
    expect(reportedChangedStates[0]).not.toBe(changedState0);
    checkValidationSpy(changedState0);

    onChangeFn(changedState1);
    expect(reportedChangedStates.length).toBe(2);
    expect(reportedChangedStates[1]).toBe(changedState1);
    expect(tagValidatorSpy).not.toHaveBeenCalled(); // changedState1 is null

    onChangeFn(changedState2);
    expect(reportedChangedStates.length).toBe(3);
    expect(reportedChangedStates[2]).toEqual(changedState2);
    expect(reportedChangedStates[2]).not.toBe(changedState2);
    checkValidationSpy(changedState2);

    // An invalid change should not be passed on
    tagValidatorSpy.and.returnValue(getExampleValidationFailed() as any);
    (changedProperties['property1'] as StringTagPartProperty).stringValue = '';
    const invalidChange = cloneDeep(changedProperties);
    onChangeFn(invalidChange);
    expect(reportedChangedStates.length).toBe(4);
    expect(reportedChangedStates[3]).toBe(null);
    checkValidationSpy(invalidChange);
}
