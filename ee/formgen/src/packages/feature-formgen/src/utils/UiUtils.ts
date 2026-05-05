import { FormState, UIState } from '../types/interfaces';
import { StatesToUpdateEnum } from '../constants/enums/StateToUpdateEnum';
import * as validationUtils from '../utils/ValidationUtils';
import { RestService } from '../services/RestService';
import { 
  preHandleNextFormFlowPageChange, 
  postHandleFormFlowPageChange, 
  preHandleNextFormPageChange, 
  postHandleNextFormPageChange, 
  preHandlePreviousFormPageChange, 
  postHandlePreviousFormPageChange,
  preHandlePreviousFormFlowPageChange,
  postHandlePreviousFormFlowPageChange 
} from '../custom/HooksUtils';

export const changeToPage = async(
  uiState: UIState,
  pageChangeTo: number,
  updateState: (state: StatesToUpdateEnum) => void,
  isFormPage: boolean = true,
) => {
  if (isFormPage) {
    console.log('Formpage current page:', uiState.currentPage);
    if (pageChangeTo >= 0 && pageChangeTo < uiState.lastPage) {
      uiState.currentPage = pageChangeTo;
      console.log('Formpage change to page:', pageChangeTo);
    } else {
      console.error('Formpage cannot change to page:', pageChangeTo);
    }
  } else {
    // Flowpage
    console.log('Flowpage current page:', uiState.flowPageIndex);
    if (pageChangeTo >= 0 && pageChangeTo < uiState.flowList.length) {
      uiState.flowPageIndex = pageChangeTo;
      console.log('Flowpage change to page:', pageChangeTo);
    } else {
      console.error('Flowpage cannot change to page:', pageChangeTo);
    }
  }
  uiState.loading = false;
  updateState(StatesToUpdateEnum.UIState);
};

/**
 * only for navigation inside the real form
 * only one exception when changing form the last form page to the flow page
 * @param uiState
 * @param updateState
 */
export const changeNextPage = async (
  formState: FormState,
  uiState: UIState,
  restService: RestService,
  updateState: (state: StatesToUpdateEnum) => void,
  mockForm?: boolean,
) => {
  const currentPage = uiState.currentPage;
  const nextPage = currentPage + 1;

  // We are at the last page of the formScreen and want to switch to the next flow page
  const switchFromFormToFlow = currentPage == uiState.lastPage - 1;

  if (nextPage < uiState.lastPage || switchFromFormToFlow) {
    if (!switchFromFormToFlow) {
      console.log('Formpage current page:', currentPage);
      console.log('Formpage next page:', nextPage);
    }

    let clientSideValid = validationUtils.isPageValid(
      formState,
      true,
      currentPage,
      mockForm,
    );
    if (!uiState.showErrorBlockOnInvalid) {
      uiState.showErrorBlockOnInvalid = true;
    }
    if (!clientSideValid) {
      formState.errorOpenSubAggNonce++;
      formState.data.hasValidationErrors = true;
      uiState.loading = false;
      updateState(StatesToUpdateEnum.ALL);
      setTimeout(function () {
        formState.handleFocusError();
      }, 650);
      return;
    }

    formState.data.hasValidationErrors = false;

    uiState.loading = true;
    updateState(StatesToUpdateEnum.ALL);

    const afterValidation = (valid?: boolean) => {
      if (!valid) {
        uiState.loading = false;
        updateState(StatesToUpdateEnum.ALL);
        setTimeout(function () {
          formState.handleFocusError();
        }, 500);
        return;
      }
      if (currentPage == uiState.lastPage - 1) {
        // Last page of formgen
        changeNextFlowPage(uiState, updateState);
      } else {
        changeToPage(uiState, nextPage, updateState);
      }
    };
    await validationUtils.loadValidationWithHooks(
      restService,
      formState,
      uiState,
      updateState,
      afterValidation,
      undefined,
      mockForm,
    );
  } else {
    console.log('Current Page is already at the lastpage.');
  }
};

/**
 * only for navigation inside the real form
 * @param uiState
 * @param updateState
 */
const changeNextFlowPage = async(
  uiState: UIState,
  updateState: (state: StatesToUpdateEnum) => void,
) => {
  let currentPage = uiState.flowPageIndex;

  const nextPage = currentPage + 1;
  if (nextPage < uiState.flowList.length) {
    console.log('Current Page:', currentPage);
    console.log('Next Page:', nextPage);
    uiState.flowPageIndex++;
    updateState(StatesToUpdateEnum.UIState);
  } else {
    console.log('Current Page is already at the lastpage.');
  }
};

/**
 * handleHooksChangeNextFlowPage
 * 
 * Ruft zunächst den Pre-Hook für den Flow-Seitenwechsel auf, 
 * führt anschließend die normale Logik (changeNextFlowPage) aus und 
 * ruft abschließend den Post-Hook auf.
 */
export const handleHooksChangeNextFlowPage = async (
  uiState: UIState,
  updateState: (state: StatesToUpdateEnum) => void,
) => {
  await preHandleNextFormFlowPageChange(uiState, updateState);
  await changeNextFlowPage(uiState, updateState);
  await postHandleFormFlowPageChange(uiState, updateState);
};

/**
 * handleHooksChangeNextForm
 * 
 * Ruft zunächst den Pre-Hook für den Formular-Seitenwechsel (nach vorne) auf, 
 * führt anschließend die normale Logik (changeNextPage) aus und 
 * ruft abschließend den Post-Hook auf.
 */
export const handleHooksChangeNextForm = async (
  formState: FormState,
  uiState: UIState,
  restService: RestService,
  updateState: (state: StatesToUpdateEnum) => void,
  mockForm?: boolean,
) => {
  await preHandleNextFormPageChange(formState, uiState, restService, updateState, mockForm);
  await changeNextPage(formState, uiState, restService, updateState, mockForm);
  await postHandleNextFormPageChange(formState, uiState, restService, updateState, mockForm);
};

/**
 * changePreviousForm
 * 
 * Navigiert zur vorherigen Formularseite, wenn vorhanden.
 * Diese Funktion wird ausschließlich für die Navigation innerhalb des realen Formulars verwendet.
 * @param formState
 * @param uiState
 * @param restService
 * @param updateState
 * @param mockForm
 */
export const changePreviousForm = async (
  formState: FormState,
  uiState: UIState,
  restService: RestService,
  updateState: (state: StatesToUpdateEnum) => void,
  mockForm?: boolean,
) => {
  const currentPage = uiState.currentPage;
  const previousPage = currentPage - 1;
  if (previousPage >= 0) {
    console.log('Current Page:', currentPage);
    console.log('Previous Page:', previousPage);
    uiState.loading = true;
    updateState(StatesToUpdateEnum.ALL);
    // Hier können asynchrone Validierungen o. Ä. eingebaut werden, falls erforderlich
    changeToPage(uiState, previousPage, updateState);
  } else {
    console.log('Current Page is already the first page.');
  }
};

/**
 * handleHooksChangePreviousForm
 * 
 * Ruft zunächst den Pre-Hook für den Rückwärts-Seitenwechsel im Formular auf, 
 * führt anschließend die normale Logik (changePreviousForm) aus und 
 * ruft abschließend den Post-Hook auf.
 */
export const handleHooksChangePreviousForm = async (
  formState: FormState,
  uiState: UIState,
  restService: RestService,
  updateState: (state: StatesToUpdateEnum) => void,
  mockForm?: boolean,
) => {
  await preHandlePreviousFormPageChange(formState, uiState, restService, updateState, mockForm);
  await changePreviousForm(formState, uiState, restService, updateState, mockForm);
  await postHandlePreviousFormPageChange(formState, uiState, restService, updateState, mockForm);
};

/**
 * changePreviousFormFlow
 * 
 * Navigiert zur vorherigen Flow-Seite, wenn vorhanden.
 * Diese Funktion wird ausschließlich für die Navigation zwischen Flow-Seiten verwendet.
 * @param uiState
 * @param updateState
 */
export const changePreviousFormFlow = async(
  uiState: UIState,
  updateState: (state: StatesToUpdateEnum) => void,
) => {
  const currentPage = uiState.flowPageIndex;
  const previousPage = currentPage - 1;
  if (previousPage >= 0) {
    console.log('Current Flow Page:', currentPage);
    console.log('Previous Flow Page:', previousPage);
    uiState.flowPageIndex--;
    updateState(StatesToUpdateEnum.UIState);
  } else {
    console.log('Current Flow Page is already the first page.');
  }
};

/**
 * handleHooksChangePreviousFormFlow
 * 
 * Ruft zunächst den Pre-Hook für den Rückwärts-Seitenwechsel im Formular-Flow auf, 
 * führt anschließend die normale Logik (changePreviousFormFlow) aus und 
 * ruft abschließend den Post-Hook auf.
 */
export const handleHooksChangePreviousFormFlow = async (
  uiState: UIState,
  updateState: (state: StatesToUpdateEnum) => void,
) => {
  await preHandlePreviousFormFlowPageChange(uiState, updateState);
  await changePreviousFormFlow(uiState, updateState);
  await postHandlePreviousFormFlowPageChange(uiState, updateState);
};