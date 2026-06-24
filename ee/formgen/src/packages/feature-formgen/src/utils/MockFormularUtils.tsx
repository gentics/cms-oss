import { Platform } from 'react-native'
import * as interfaces from '../types/interfaces'
import * as validationUtils from '../utils/ValidationUtils'
import { StatesToUpdateEnum } from '../constants/enums/StateToUpdateEnum'
import { IMockFormTypeEnum } from '../constants/enums/IMockFormTypeEnum'
import { IFormFlowSteps } from '../types/interfaces'
export function getDefaultFormFlow(): IFormFlowSteps[] {
  const tempSolution = `[
  {
    "key": "default",
    "name": "Standard",
    "formFlowSteps": [
      {
        "buttonText": "form_flow_btn_form",
        "reactClass": "FormScreen",
        "name": "form_flow_name_form",
        "description": "form_flow_desc_form",
        "buttonDefinition": [
          { "btnComponent": "DetailInfoButton",  "translateKey": "form_detailinfo",  "mobileDisabled": true },
          { "btnComponent": "CancelButton",      "translateKey": "form_cancel" },
          { "btnComponent": "PreviousPageButton", "translateKey": "form_previousPage" },
          { "btnComponent": "NextPageButton",    "translateKey": "form_nextPage" }
        ],
        "variables": []
      },
      {
        "buttonText": "form_flow_btn_summary",
        "reactClass": "SummaryScreen",
        "name": "form_flow_name_summary",
        "description": "form_flow_desc_summary",
        "buttonDefinition": [
          { "btnComponent": "CancelButton",      "translateKey": "form_cancel" },
          { "btnComponent": "PrintPdfButton",    "translateKey": "form_print",       "mobileDisabled": true },
          { "btnComponent": "PreviousPageButton", "translateKey": "form_previousPage" },
          { "btnComponent": "NextPageButton",    "translateKey": "form_nextPage" },
          { "btnComponent": "SubmitButton",      "translateKey": "form_submit" }
        ],
        "variables": [
          { "name": "showDetails", "value": true }
        ]
      },
      {
        "buttonText": "form_flow_btn_success",
        "reactClass": "SuccessScreen",
        "name": "form_flow_name_success",
        "description": "form_flow_desc_success",
        "buttonDefinition": [],
        "variables": []
      }
    ]
  },
  {
    "key": "no_summary",
    "name": "Ohne Zusammenfassung",
    "formFlowSteps": [
      {
        "buttonText": "form_flow_btn_form",
        "reactClass": "FormScreen",
        "name": "form_flow_name_form",
        "description": "form_flow_desc_form",
        "buttonDefinition": [
          { "btnComponent": "DetailInfoButton",  "translateKey": "form_detailinfo",  "mobileDisabled": true },
          { "btnComponent": "CancelButton",      "translateKey": "form_cancel" },
          { "btnComponent": "PreviousPageButton", "translateKey": "form_previousPage" },
          { "btnComponent": "NextPageButton",    "translateKey": "form_nextPage" },
          { "btnComponent": "SubmitButton",    "translateKey": "form_submit" }
        ],
        "variables": []
      },
      {
        "buttonText": "form_flow_btn_success",
        "reactClass": "SuccessScreen",
        "name": "form_flow_name_success",
        "description": "form_flow_desc_success",
        "buttonDefinition": [],
        "variables": []
      }
    ]
  }
]`;

  return JSON.parse(tempSolution) as IFormFlowSteps[];
}

export function fetchMockDefaultFormular(
  mockFormType: IMockFormTypeEnum
): Promise<any> {
  if (!__DEV__) return Promise.resolve(null)
  if (Platform.OS === 'web') {
    let url = ''
    switch (mockFormType) {
      case IMockFormTypeEnum.DEFAULT_AN_FORM:
        url = '/mock-files/defaultANFormular.json'
        break
      case IMockFormTypeEnum.DEFAULT_CMS_FORM:
        url = '/mock-files/defaultCMSFormular.json'
        break
      case IMockFormTypeEnum.EXTENDED_FORM:
        url = '/mock-files/extendedFormular.json'
        break
      case IMockFormTypeEnum.EXPERTSEARCH_FORM:
        url = '/mock-files/mockExpertSearchForm.json'
        break
      default:
        return Promise.resolve(null)
    }
    return fetch(url).then(res => {
      if (!res.ok) throw new Error(`HTTP ${res.status}`)
      return res.json()
    })
  } else {
    let data: any = null
    switch (mockFormType) {
      case IMockFormTypeEnum.DEFAULT_AN_FORM:
        data = require('../mock-files/defaultANFormular.json')
        break
      case IMockFormTypeEnum.DEFAULT_CMS_FORM:
        data = require('../mock-files/defaultCMSFormular.json')
        break
      case IMockFormTypeEnum.EXTENDED_FORM:
        data = require('../mock-files/extendedFormular.json')
        break
      case IMockFormTypeEnum.EXPERTSEARCH_FORM:
        data = require('../mock-files/mockExpertSearchForm.json')
        break
      default:
        return Promise.resolve(null)
    }
    return Promise.resolve(data)
  }
}

export function fetchMockDefaultFeatures(): Promise<any> {
  if (!__DEV__) return Promise.resolve({})
  if (Platform.OS === 'web') {
    return fetch('/mock-files/defaultFeatures.json').then(res => {
      if (!res.ok) throw new Error(`HTTP ${res.status}`)
      return res.json()
    })
  } else {
    const data = require('../mock-files/defaultFeatures.json')
    return Promise.resolve(data)
  }
}

export function fetchMockSearch(): Promise<any> {
  if (!__DEV__) return Promise.resolve(null)
  if (Platform.OS === 'web') {
    return fetch('/mock-files/searchResult.json').then(res => {
      if (!res.ok) throw new Error(`HTTP ${res.status}`)
      return res.json()
    })
  } else {
    const data = require('../mock-files/searchResult.json')
    return Promise.resolve(data)
  }
}

export function fetchMockLazyCatalog(): Promise<any> {
  if (!__DEV__) return Promise.resolve(null)
  if (Platform.OS === 'web') {
    return fetch('/mock-files/lazyCatalog.json').then(res => {
      if (!res.ok) throw new Error(`HTTP ${res.status}`)
      return res.json()
    })
  } else {
    const data = require('../mock-files/lazyCatalog.json')
    return Promise.resolve(data)
  }
}

export function fetchMockAutoComplete(): Promise<any> {
  if (!__DEV__) return Promise.resolve(null)
  if (Platform.OS === 'web') {
    return fetch('/mock-files/autocompleteResult.json').then(res => {
      if (!res.ok) throw new Error(`HTTP ${res.status}`)
      return res.json()
    })
  } else {
    const data = require('../mock-files/autocompleteResult.json')
    return Promise.resolve(data)
  }
}

export function fetchDefaultFormularData(path: string, callback: any): void {
  if (!path) return
  if (Platform.OS === 'web') {
    fetch(path)
      .then(res => {
        if (!res.ok) throw new Error(`HTTP ${res.status}`)
        return res.json()
      })
      .then(content => callback(content))
      .catch(() => { })
  } else {
    // Map web paths to static requires for React‑Native bundler
    let data: any = null
    switch (path) {
      case '/mock-files/defaultFeatures.json':
        data = require('../mock-files/defaultFeatures.json')
        break
      case '/mock-files/defaultANFormular.json':
        data = require('../mock-files/defaultANFormular.json')
        break
      case '/mock-files/defaultCMSFormular.json':
        data = require('../mock-files/defaultCMSFormular.json')
        break
      case '../mock-files/subformular.json':
        data = require('../mock-files/subformular.json')
        break
      case '/mock-files/searchResult.json':
        data = require('../mock-files/searchResult.json')
        break
      case '/mock-files/lazyCatalog.json':
        data = require('../mock-files/lazyCatalog.json')
        break
      case '/mock-files/mockExpertSearchReportResult.json':
        data = require('../mock-files/mockExpertSearchReportResult.json')
        break
      case '/mock-files/updateOnChangeTriggerPreviousPageError.json':
        data = require('../mock-files/updateOnChangeTriggerPreviousPageError.json')
        break
      default:
        data = null
    }
    if (data) callback(data)
  }
}

export function fetchDefaultFeatures(callback: any): void {
  fetchDefaultFormularData('/mock-files/defaultFeatures.json', callback)
}

export function fetchDefaultFormular(callback: any): void {
  fetchDefaultFormularData('/mock-files/defaultANFormular.json', callback)
}

export function fetchSubFormular(callback: any): void {
  fetchDefaultFormularData('../mock-files/subformular.json', callback)
}

export function fetchDefaultSearchData(callback: any): void {
  fetchDefaultFormularData('/mock-files/searchResult.json', callback)
}

export function fetchDefaultLazyCatalogData(callback: any): void {
  fetchDefaultFormularData('/mock-files/lazyCatalog.json', callback)
}

export function fetchFakeExpertSearchData(callback: any): void {
  fetchDefaultFormularData('/mock-files/mockExpertSearchReportResult.json', callback)
}

export function fetchUpdateOnChangeTriggerPreviousPageError(callback: any): void {
  fetchDefaultFormularData('/mock-files/updateOnChangeTriggerPreviousPageError.json', callback)
}

export function fetchValidationRedirectFormData(callback: any): void {
  fetchDefaultFormularData('/mock-files/validationResponseRedirectForm.json', callback)
}

export function fakeValidation(
  formState: interfaces.FormState,
  uiState: interfaces.UIState,
  updateState: (state: StatesToUpdateEnum) => void,
  afterValidation: (valid?: boolean) => void,
  jsonpath?: string,
  pageIndex?: number
): void {
  validationUtils.handleValidation(
    formState.data,
    formState,
    uiState,
    updateState,
    jsonpath,
    undefined,
    true
  )
  afterValidation(true)
}

const loaderIds = [
  '40679F86-C11F-4D1E-8BC9-077A06327B02',
  'F35C9670-8B2A-4731-8A01-9592C0DB6A31',
  'A8BF4722-ED09-4F43-AB58-2BC39CA65678'
]

export function fakeLoader(loading: boolean, id?: string): boolean {
  return id != null && loaderIds.includes(id) ? true : loading
}

const anFieldErrorIds = [
  '40679F86-C11F-4D1E-8BC9-077A06327B02',
  'F35C9670-8B2A-4731-8A01-9592C0DB6A31',
  'A8BF4722-ED09-4F43-AB58-2BC39CA65678'
]

export function fakeANfieldLoadError(id?: string): boolean {
  return id != null && anFieldErrorIds.includes(id)
}