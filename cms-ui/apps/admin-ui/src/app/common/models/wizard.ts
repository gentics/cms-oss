/**
 * Callback function that is called when the "Next" or "Finish" button is clicked.
 *
 * It must return a promise that resolves if the wizard may advance to the next
 * step or close if it is the last step.
 * The value that the promise resolves to is used as argument to the next
 * step's `activate` event or to the modal's promise resolution, if this
 * was the last step.
 */
export type WizardStepNextClickFn<T> = () => Promise<T>;
