import { StatesToUpdateEnum } from '../constants/enums/StateToUpdateEnum';
import { RestService } from '../services/RestService';
import { RecaptchaService } from '../services/RecaptchaService';
import { ApiResponse, FormState, IData, UIState } from '../types/interfaces';

// Submit
export async function preSubmitForm(
  formid: any,
  data: FormData,
  formState: FormState,
  uiState: UIState,
  updateState: (state: StatesToUpdateEnum) => void,
  restService: RestService,
): Promise<void> {
  if (RecaptchaService.hasExecute()) {
    await RecaptchaService.execute();
  }
}

export async function postSubmitForm(
  formid: any,
  data: FormData,
  formState: FormState,
  uiState: UIState,
  updateState: (state: StatesToUpdateEnum) => void,
  restService: RestService,
): Promise<void> {
  // Füge hier asynchrone Logik ein, die nach dem eigentlichen Submit ausgeführt werden soll
}


// HandlePageHooks - Form
export async function preHandleNextFormPageChange(
  formState: FormState,
  uiState: UIState,
  restService: RestService,
  updateState: (state: StatesToUpdateEnum) => void,
  mockForm?: boolean,
): Promise<void> {
  console.log("Pre-Hook: handlePageChange to pageIndex " + (uiState.currentPage + 1));
  // Füge hier asynchrone Logik ein, die vor dem eigentlichen Funktionsaufruf ausgeführt werden soll
}

export async function postHandleNextFormPageChange(
  formState: FormState,
  uiState: UIState,
  restService: RestService,
  updateState: (state: StatesToUpdateEnum) => void,
  mockForm?: boolean,
): Promise<void> {
  console.log("Post-Hook: handlePageChange to pageIndex " + (uiState.currentPage + 1));
  // Füge hier asynchrone Logik ein, die nach dem eigentlichen Funktionsaufruf ausgeführt werden soll
}

export async function preHandlePreviousFormPageChange(
  formState: FormState,
  uiState: UIState,
  restService: RestService,
  updateState: (state: StatesToUpdateEnum) => void,
  mockForm?: boolean,
): Promise<void> {
  // Füge hier asynchrone Logik ein, die vor dem eigentlichen Funktionsaufruf ausgeführt werden soll
}

export async function postHandlePreviousFormPageChange(
  formState: FormState,
  uiState: UIState,
  restService: RestService,
  updateState: (state: StatesToUpdateEnum) => void,
  mockForm?: boolean,
): Promise<void> {
  // Füge hier asynchrone Logik ein, die vor dem eigentlichen Funktionsaufruf ausgeführt werden soll
}

// HandlePageHooks - Formular Flow
export async function preHandleNextFormFlowPageChange(
  uiState: UIState,
  updateState: (state: StatesToUpdateEnum) => void,
): Promise<void> {
  console.log("Pre-Hook: handlePageChange to pageIndex " + (uiState.flowPageIndex + 1));
  // Füge hier asynchrone Logik ein, die vor dem eigentlichen Funktionsaufruf ausgeführt werden soll
}

export async function postHandleFormFlowPageChange(
  uiState: UIState,
  updateState: (state: StatesToUpdateEnum) => void,
): Promise<void> {
  console.log("Post-Hook: handlePageChange to pageIndex " + (uiState.flowPageIndex + 1));
  // Füge hier asynchrone Logik ein, die nach dem eigentlichen Funktionsaufruf ausgeführt werden soll
}

export async function preHandlePreviousFormFlowPageChange(
  uiState: UIState,
  updateState: (state: StatesToUpdateEnum) => void,
): Promise<void> {
  // Füge hier asynchrone Logik ein, die vor dem eigentlichen Funktionsaufruf ausgeführt werden soll
}

export async function postHandlePreviousFormFlowPageChange(
  uiState: UIState,
  updateState: (state: StatesToUpdateEnum) => void,
): Promise<void> {
  // Füge hier asynchrone Logik ein, die vor dem eigentlichen Funktionsaufruf ausgeführt werden soll
}


// Validation Hooks
export async function preLoadValidation(
  restService: RestService,
  formState: FormState,
  uiState: UIState,
  updateState: (stateToUpdate: StatesToUpdateEnum) => void,
  afterValidation: (valid?: boolean) => void,
  jsonPath?: string,
  mockForm?: boolean,
  updateOnChange?: boolean
): Promise<void> {
  console.log("Pre-Hook: loadValidation");
  // Füge hier asynchrone Logik ein, die vor dem eigentlichen Funktionsaufruf ausgeführt werden soll
}

export async function postLoadValidation(
  restService: RestService,
  formState: FormState,
  uiState: UIState,
  updateState: (stateToUpdate: StatesToUpdateEnum) => void,
  afterValidation: (valid?: boolean) => void,
  jsonPath?: string,
  mockForm?: boolean,
  updateOnChange?: boolean
): Promise<void> {
  console.log("Post-Hook: loadValidation");
  // Füge hier asynchrone Logik ein, die nach dem eigentlichen Funktionsaufruf ausgeführt werden soll
}

// InitForm Hooks:
// Init Formular Hooks

/**
 * 
 * @param uiState 
 * @param formState
 * @param newFormId - ID from the sub form
 */
export async function preInitForm(
  uiState: UIState, formState: FormState, newFormId?: string
): Promise<void> {
  console.log("Pre-Hook: initForm");
  // Füge hier asynchrone Logik ein, die vor dem eigentlichen Funktionsaufruf ausgeführt werden soll
}

/**
 * 
 * @param uiState 
 * @param formState 
 * @param responseFromInitForm - ApiResponse from initForm-Request / initSubForm
 * @param newFormId - ID from the sub form
 */
export async function postInitForm(
  uiState: UIState, formState: FormState, responseFromInitForm: ApiResponse, newFormId?: string
): Promise<void> {
  console.log("Post-Hook: initForm");
  // Füge hier asynchrone Logik ein, die nach dem eigentlichen Funktionsaufruf ausgeführt werden soll
}


