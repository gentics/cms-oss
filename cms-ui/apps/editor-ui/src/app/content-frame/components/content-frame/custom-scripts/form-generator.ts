import { CNIFrameDocument } from '../../../models/content-frame';

export function handleFormGeneratorSaveButton(customOkayButton: HTMLButtonElement, document: CNIFrameDocument): void {
    customOkayButton.classList.add('disabled');
    customOkayButton.disabled = true;

    document.addEventListener('click', event => {
        const target = event.target as Element;
        if (isSaveButton(target)) {
            customOkayButton.classList.remove('disabled');
            customOkayButton.removeAttribute('disabled');
        }
    });

}

export function isFormGenerator(document: CNIFrameDocument): boolean {
    return !!document.querySelector('#fg-form');
}

function isSaveButton(element: Element): boolean {
    const selector = '#toolbar-button-save, #toolbar-button-save *';
    if (element && element.matches) {
        return element.matches(selector);
    } else if (element && (<any> element).msMatchesSelector) {
        return (<any> element).msMatchesSelector(selector);
    } else {
        return false;
    }
}
