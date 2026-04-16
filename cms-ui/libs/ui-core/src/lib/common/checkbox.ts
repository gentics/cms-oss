/** The special state of the checkbox where it is neither checked, nor unchecked.  */
export const CHECKBOX_STATE_INDETERMINATE = 'indeterminate';

/** @deprecated Use `CheckboxState` instead. */
export type CheckState = boolean | typeof CHECKBOX_STATE_INDETERMINATE;
export type CheckboxState = CheckState;
