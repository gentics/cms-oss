import { ViewStyle } from 'react-native';
import React, { ComponentType } from 'react';
import { RestService } from '../services/RestService';
import { TFunction } from 'i18next';
import { StatesToUpdateEnum } from '../constants/enums/StateToUpdateEnum';
import { OverlayTypeEnum } from '../custom/OverlayTypes';
import { IDebugSectionProps } from '../components/overlays/DebugSection';
import { IOverlayFullScreenTextEnum } from '../constants/enums/IOverlayFullScreenTextEnum';
import { PluginNameEnum } from '../constants/enums/PluginNameEnum';

//SummaryDetails
export interface ISummaryDetailsUser {
  email: string;
  name: string;
  username: string;
}

export type IOverlayType = ComponentType<ICommonOverlayProps>;

export type IOverlayTypes = {
  [key in Exclude<OverlayTypeEnum, OverlayTypeEnum.NONE>]: IOverlayType;
};

// ExpertSearch
export interface IExpertSearch {
  formState: FormState;
  uiState: UIState;
  restService: RestService;
  handleFormCancel?: (callback: any) => void;
  handleExpertSearch?: (callback?: any) => void;
  handleExpertExcelDownload?: () => void;
  updateState: (state: StatesToUpdateEnum) => void;
  setExpertMultipleHandler?: (fn: (buttonID: string) => void) => void;
  hasSearched: boolean;
  t: TFunction;
  reportResult?: IReportResult;
  // setCallbackExpertSearch: (callbackFunction: any) => void;
}

export interface ITriggerFormEndpointResponse {
  formID?: string;
  version?: string;
  action?: string;
  targetFormID?: string;
  initializeData?: any;
  targetUrl?: string;
}

// For SummaryElement
export interface ISummaryAttributes {
  formState: FormState;
  uiState: UIState;
  summaryOption: SummaryOptions;
  label?: ITranslationSet;
  value: string | React.ReactNode[] | string[] | null;
  aggregat?: boolean;
  formGridOptions?: IFormGridOptions;
  description?: ITranslationSet;
  formGridElement?: boolean;
  numberOfColumns?: number;
  dangerouslySetInnerHTML?: boolean;
  t: any;
  initUrl: string;
}

export interface IOverlayConfirmationState {
  title: string;
  content: string;
  handleOverlayConfirmation: () => void;
  handleOverlayClose: () => void;
}

export interface IOverlayConfirmationProps {
  overlayState?: IOverlayConfirmationState;
}


export interface IErrorMessage {
  validationMessage: string;
  pageNumber: number;
  elementInfo: IFirstErrorElementInfo;
}

export interface IFirstErrorElementInfo {
  elementID: string;
  aggregateID?: string;
  subAggregateID?: string;
  subAggregateSortIndex?: number;
  internalParentID?: string;
  jsonPath?: string;
}

export interface IElementBeingFocusedInfo {
  elementID: string;
  parentAggregateID?: string;
  isSubAggregateElement?: boolean;
  jsonPath?: string;
  insideSubaggregatIndex?: number;
}

export interface IErrorBlockProps {
  errorMessages: IErrorMessage[];
  currentPage: number;
}

export interface Theme {
  container: ViewStyle;
  screenTitle: any;
}

export interface IFeatures {
  [key: string]: IFeaturesConfig & IFeatureUploadConstraintOptions;
}

export interface IFeaturesConfig {
  [key: string]: string;
}

interface IFeatureUploadConstraintOptions {
  maxSizePerFile: number;
  maxSizeOverall: number;
  fileExtensionsAllowed: string[];
}

export enum PaymentStateTypes {
  PaymentInProgress = 1,
  PaymentFailed = 2,
  PaymentAuthorized = 3,
}

export enum SummaryOptions {
  No = 0,
  SummaryText = 1,
  ReadOnly = 3,
}

export interface IElementTypes {
  [name: string]: IElementType;
}

export interface IPluginElements {
  [name: string]: IElementTypes;
}

// TODO! SOME VARIABLES MIGHT NOT BE NEEDED
export type IElementType = (
  | ComponentType<FormGridTextElementProps>
  | ComponentType<IFormComponentAttributes>
) &
  IValidateable &
  ISummarizeable;

export interface FormGridTextElementProps {
  formState: FormState;
  uiState: UIState;
  formGridOptions?: IFormGridOptions;
  summary: SummaryOptions;
}

// TODO! SOME VARIABLES MIGHT NOT BE NEEDED
interface IValidateable {
  validate(
    state: FormState,
    jsonPath: string,
    id: string,
    updateErrorMessages: boolean,
    t: TFunction,
    formGridOptions?: IFormGridOptions,
    localValue?: any,
  ): CommonValidation;
}

interface ISummarizeable {
  getSummaryValue(
    state: FormState,
    jsonPath: string,
    id: string,
    t: TFunction,
    formGridOptions?: IFormGridOptions,
    restService?: RestService,
  ): string | any;
}

export interface CommonValidation {
  isValid: boolean;
  errorMessage?: string;
}

export interface IFormComponentBaseAttributes {
  elements: IFormElement[] | undefined;
  handleExpertSearch?: (callback?: any) => void;
  handleChange?: (
    jsonpath: string,
    event: any,
    id: string,
    value?: any,
    instant?: boolean,
    formGridOptions?: IFormGridOptions,
  ) => void;
  submitCSVUpload?: (jsonpath: string, id: string) => void;
  handleSearch?: (jsonPath: string, searchTerm: string, callback: any) => void;
  handleLazyCatalog?: (
    jsonPath: string,
    searchTerm: string,
    callback: any,
  ) => void;
  handleAutocomplete?: (
    jsonPath: string,
    searchTerm: string,
    callback: any,
  ) => void;
  listAddElement?: (parentId: string, jsonpath: string) => void;
  listDeleteElement?: (parentJsonPath: string, index: number) => void;
  uiState: UIState;
  formState: FormState;
  parentId: string;
  jsonpath: string;
  restService: RestService;
  jsonpathParent?: string;
  summary: SummaryOptions;
  aggregatShowOnlyFirstLine?: boolean;
  afterChangeMultiple?: (elements: IElementIdentifier[]) => void;
  handleDialogFormEdit?: TDialogFormEdit;
  updateState: (stateOption: StatesToUpdateEnum) => void;
  setFirstErrorElementRef?: (ref: any) => void;
  updateFirstErrorElementRef?: (
    elementProps: IFormComponentAttributes,
    elementRef: React.RefObject<any>,
  ) => void;
  elementBeingFocused?: IElementBeingFocusedInfo;
  setElementBeingFocused?: (info: IElementBeingFocusedInfo) => void;
  t: TFunction;
  key?: string;
  isAggregate?: boolean;
  isSearch?: boolean;
  isLoggedIn?: boolean;
  rawErrorMsgs?: IErrorMessage[];
  insideSubaggregatIndex?: number;
  selectedElementId?: string;

}

export interface IDialogFormInitBody {
  FormID: string;
  ParentDialogPortalFormHostObjectID: string;
  // only present when editing existing entry
  RowID?: string;
  // only present when adding entry
  PropertyReferenceID?: string;
  // only present when new or copied entry should be inserted somewhere other than the end of the list
  CreateAfterIndex?: number;
  // only present if existing entry should be copied
  DuplicateRowID?: string;
}

export interface IFormComponentAttributes extends IFormComponentBaseAttributes {
  key: string;
  label: ITranslationSet;
  description: ITranslationSet;
  id: string;
  value: any;
  required: boolean;
  editable: boolean;
  createable?: boolean;
  error: string;
  type: string;
  parentId: string;
  isList: boolean;
  possibleValues: IPossibleValue[];
  loading: boolean;
  ANFieldLoadError: boolean;
  formGridOptions?: IFormGridOptions;
  children?: React.ReactNode[] | React.ReactNode;
  autoCompleteResults?: IAutocompleteValue[];
  csvUploadEnabled?: boolean;
  editMode?: formEditModeValues;
  editModeDialogFormID?: string;
  t: TFunction;
  numberOfColumns?: number;
  ElemTypeNode?: any;
  elemTypeString?: string;
}

export interface IElementIdentifier {
  jsonpath: string;
  schemaID: string;
}

type TDialogFormEdit = (
  scrollReference: string,
  editModeDialogFormID: string,
  refreshPropertyID: string,
  createAfterIndex?: number,
  businessObjectId?: string,
  isCopy?: boolean,
) => Promise<void>;

export interface UIState {
  flowPageIndex: number;
  flowList: IFormFlowClass[];
  currentPage: number; // only for the real form page
  lastPage: number;
  initDone: boolean;
  loading: boolean;
  isExpertSearch: boolean;
  isReadOnlyForm: boolean;
  showErrorBlockOnInvalid?: boolean; // Error block will only be shown after one of the following actions: 1. changeToNextPage, 2. updateonchange
  dialogForm: {
    refreshPropertyID?: string;
    mainFormState?: {
      uiState: UIState;
      formState: FormState;
    } | null;
    isDialogFormOpen?: boolean;
    scrollReference?: string;
  };
  overlayState: OverlayState;
  loadingText?: string;
  isMockForm?: boolean;
  isDevMode?: boolean;
  andpNavigation?: any;
  linkCallback?: any;
}
export interface IOverlayState {
  overlayType: OverlayTypeEnum;
}

export interface FormState {
  //TODO v8.2 the following logics must be implemented
  // initDone: boolean;
  formid: string;
  businessObjectID: string;
  culture: string;
  features: IFeatures;
  form: IForm; //uiSchema
  data: IData;
  schema: ISchema;
  normalizedSchema: NormalizedSchemas;
  paymentActive?: boolean;
  paymentState?: PaymentStateTypes;
  transactionId?: string;
  anRoundTrip?: boolean; //TODO v8.2
  t: TFunction;
  fileUploads?: any;
  submitted: boolean;
  handleFocusError: () => void;
  expertMultipleHandler?: (buttonID: string) => void;
  expertSearchCheckMarkCount?: number;
  deleted?: boolean;
  onBoardingFeatureActivated?: boolean;
  errorOpenSubAggNonce: number;
  isLoggedIn: boolean;
  detailUrl?: string;
  hasDetail?: boolean;
  isFormgridPreview?: boolean;
  pluginName: PluginNameEnum;
  captchaConfig?: CaptchaConfig;
}

export type FriendlyCaptchaMode = 'auto' | 'button' | 'background'
export type reCaptchaMode = 'checkbox' | 'invisible'

/**
 * This 
 */
export interface CaptchaConfig {
  provider: 'friendly' | 'recaptcha';
  siteKey: string;
  mode?: FriendlyCaptchaMode | reCaptchaMode;
  language?: string;
  baseUrl?: string;
};
//TODO Method uncomment "?""
export interface IFormPageComponent {
  formState: FormState;
  uiState: UIState;
  flowObj?: IFormFlowClass;
  t: TFunction;
  updateState: (stateOption: StatesToUpdateEnum) => void;
  setFirstErrorElementRef?: (ref: React.RefObject<any>) => void;
  restService: RestService;
  devMode: boolean;
  mockForm: boolean;
  isLoggedIn?: boolean;
  screenType?: string;
  translateKey?: string;
  handleSubmitDialogForm: () => void;
  handleFormCancel?: (
    callback: any,
    isOnlyCloseDialog?: boolean,
  ) => Promise<void>;
  scrollToElement: (elementRef: any) => void;
  updateFirstErrorElementRef: (
    elementProps: IFormComponentAttributes,
    elementRef: React.RefObject<any>,
  ) => void;
  //TODO v8.2 state-management: all the methods should be moved to the utils
  handlePageChange?: (
    pageIndex: number,
    formRef?: React.RefObject<HTMLElement | undefined>, // TODO: Behalten oder entfernen?
  ) => void;
  handleExpertSearch?: (callback?: any) => void;
  setExpertMultipleHandler?: (fn: (buttonID: string) => void) => void;
  // Ergänzungen aus dem alten Interface
  handleFormSubmit?: () => void;
  pageName?: ITranslationSet;
  handleFormSave?: () => void;
  handleTransactionCreate?: () => void;
  handleBackgroundFormSave?: any;
  schema?: ISchema;
  handleChange?: (
    jsonpath: string,
    event: any,
    id: string,
    value?: any,
    instant?: boolean,
    formGridOptions?: IFormGridOptions,
  ) => void;
  submitCSVUpload?: (jsonpath: string, id: string) => void;
  handleSearch?: (jsonPath: string, searchTerm: string, callback: any) => void;
  handleLazyCatalog?: (
    jsonPath: string,
    searchTerm: string,
    callback: any,
  ) => void;
  handleAutocomplete?: (
    jsonPath: string,
    searchTerm: string,
    callback: any,
  ) => void;
  listAddElement?: (parentId: string, jsonpath: string) => void;
  listDeleteElement?: (parentJsonPath: string, index: number) => void;
  //formRef?: React.RefObject<HTMLElement | undefined>;
  afterChangeMultiple?: (elements: IElementIdentifier[]) => void;
  hasSearched?: boolean;
  handleDialogFormEdit?: TDialogFormEdit;
  elementBeingFocused?: IElementBeingFocusedInfo;
  setElementBeingFocused: (info: IElementBeingFocusedInfo) => void;
  rawErrorMsgs?: IErrorMessage[];
  reportResult?: IReportResult;
}

export interface NormalizedSchemas {
  [key: string]: ISchemaFieldProperties;
}

export interface IFormFlowClass {
  reactClass: string;
  buttons: IFormFlowButtonDefinition[];
  nameTranslationKey: string;
  descriptionTranslationKey: string;
  variables?: IFormFlowClassVariables[];
  buttonLabelTranslationKey: string;
}

export interface IFormFlowClassVariables {
  name: string;
  value: any;
}

export interface IFormFlowButtonDefinition {
  reactClass: string;
  labelTranslationKey: string;
  mobileDisabled?: boolean;
}

export interface IFormFlowData {
  meshEndpoint: string;
}

export interface IFormFlowSteps {
  id: string;
  nameTranslationKey: string;
  steps: IFormFlowClass[];
}
export interface IFormFlowStep {
  buttonLabelTranslationKey: string;
  reactClass: string;
}
/**
 * interface for deserializing uiSchema
 */
export interface IForm {
  key?: string;
  version?: string;
  formname?: ITranslationSet;
  formdescription?: ITranslationSet;
  pages?: IFormPage[];
  formwidth?: number;
  formwidthOptimized?: boolean;
}

/**
 * interface for a single deserialized uischema.page element
 */
export interface IFormPage {
  pagename: ITranslationSet;
  elements: IFormElement[];
}

export interface IFormElement {
  id: string;
  type: string;
  label: ITranslationSet;
  description: ITranslationSet;
  elements?: IFormElement[];
  formGridOptions?: IFormGridOptions;
  csvUploadEnabled?: boolean;
  editMode?: formEditModeValues;
  editModeDialogFormID?: string;
  editModeDialogUsage: number;
}

// Bild-Optionen (neu)
export type ImageObjectFit = 'contain' | 'cover' | 'fill' | 'none' | 'scale-down';
export type ImageAlign = 'start' | 'center' | 'end' | 'stretch';

export interface IFormGridImageOptions {
  url: string;
  width?: number | 'auto';
  height?: number | 'auto';
  objectFit?: ImageObjectFit;
  align?: ImageAlign;
  alt: ITranslationSet;
  caption?: ITranslationSet;
}

// TODO!ITranslationSet
export interface IFormGridOptions {
  numberOfColumns: number;
  numberOfLines?: number;
  showFilter?: boolean;
  //deprecated
  hideListicons?: boolean;
  showOnlyFirstLine?: boolean;
  type?: string;
  value?: ITranslationSet;
  valueSummary?: ITranslationSet;
  dependsOn?: string;
  replacementText?: boolean;
  ocr?: boolean;
  button?: boolean;
  buttonIsPrimary?: boolean;
  radio?: boolean;
  radioColumns?: number;
  dropdown?: boolean;
  numberPlaceholder?: string;
  numberStep?: number;
  fileExtensionsNotAllowed?: string[];
  tableColumnWidthOptimized?: boolean;
  tableRowMaxLineHeight?: number;
  alignment?: 'left' | 'center' | 'right';
  alignmentUnit?: 'left' | 'center' | 'right';
  buttonDisplayStyle?: 'icon' | 'text' | 'icontext';
  overlayOptions?: IOverlayInfoOptions;
  // Sichtbarkeitssteuerung für Grid-Elemente
  // Wenn inForm === false, wird das Element im normalen Formular nicht angezeigt
  inForm?: boolean;
  // Wenn inSummary === false, wird das Element in der Summary nicht angezeigt
  inSummary?: boolean;

  image?: IFormGridImageOptions;
  siteKey?: string;
  baseUrl?: string;
  mode?: 'checkbox' | 'invisible';
  condition?: ICmsCondition;
}
export interface IOverlayInfoOptions {
  isShowOverlay: boolean;
  texts?: {
    title?: ITranslationSet;
    textPre?: ITranslationSet;
    textPost?: ITranslationSet;
  };
  value?: string;
}

/**
 * the deserialized schema interface
 */
export interface ISchema {
  key: string;
  version: string;
  properties: ISchemaFields;
  formType?: string;
  excelResultSupported?: boolean;
}
export interface ISchemaFields {
  [key: string]: ISchemaFieldProperties;
}
// TODO!
export interface ISchemaFieldProperties {
  unit?: ITranslationSet;
  value?: string;
  dateOnly?: boolean;
  type: string;
  searchReferenceValueMinLength?: number;
  updateOnChange?: boolean;
  precision?: number;
  name?: string;
  validation?: IValidation;
  mandatory?: boolean;
  isList?: boolean;
  properties?: ISchemaFields;
  formPage?: number;
  autoCompleteMinLength?: number;
  isRaw?: boolean;
  condition?: ICmsCondition;
}

export interface IValidation {
  formID?: string;
  hasValidationErrors?: boolean;
  [key: string]: any;
}

export interface ICmsCondition {
  source: { control: string };       // ID of the referenced field
  equals: string | boolean | number | null; // Expected value (Phase 1: equals only)
}
// TODO!
/**
 * the deserialized data object
 */
export interface IData {
  formID: string;
  DialogPortalFormHostObjectID?: string;
  businessObjectID?: string;
  version?: string;
  culture?: string;
  properties?: IDataFields;
  savedPageIndex?: number;
  isUpdateOnChange?: boolean;
  // only use in expertsearch
  hasErrors?: boolean;
  hasValidationErrors?: boolean;
  preconditions?: IDataPrecondition[];
  reportResultType?: string;
  reportResult?: IReportResult;
  antiDtoManipulationHash?: string;
  initializeData?: any;
  formControls?: any;
  transactionId?: string;
  paymentStatus?: string;
  possibleValueSets?: PossibleValueSet[];
}

export interface IReportResult {
  header?: String[];
  headerColumns: IReportResultHeaderColumn[];
  content: [][];
  searchQueryUUID?: string | undefined;
  resultsExceededLimit?: boolean;
}

export interface IReportResultHeaderColumn {
  name: string;
  type: string;
  id?: string;
  isMultiSelect?: boolean;
}

export interface PossibleValueSet {
  possibleValueSetID: string;
  possibleValues: IPossibleValue[];
}

export interface IDataPrecondition {
  message: string;
  formID?: string;
}

export interface IDataFields {
  [key: string]: IDataFieldProperties | null;
}
// TODO!
export interface IDataFieldProperties {
  id?: string;
  value?: string | any;
  values?: any;
  oldValue?: any;
  visible?: boolean;
  validationMessage?: string;
  mandatory?: boolean;
  editable?: boolean;
  properties?: IDataFields;
  possibleValueSetID?: string;
  possibleValues?: IPossibleValue[];
  possibleAggregateValues?: IDataPossibleAggregationValueFields;
  searchValue?: string;
  loading?: boolean;
  ANFieldLoadError?: boolean;
  renderingType?: string;
  autoCompleteSearchValue?: string;
  autoCompleteResults?: IAutocompleteValue[];
  csvUpload?: boolean;
  isUpdateOnChange?: boolean;
  createable?: boolean;
}

export interface IAutocompleteValue {
  displayName: string;
  values?: any;
}

export interface IDataPossibleAggregationValueFields {
  [key: string]: IDataPossibleAggregationValue | null;
}

export interface IDataPossibleAggregationValue {
  defaultValue?: string;
  possibleValues?: IPossibleValue[];
  possibleValueSetID: string;
  renderingType?: string;
  visible?: boolean;
  editalbe?: boolean;
}

export interface IPossibleValue {
  identifier: string;
  displayName: string;
  shortDisplayName?: string;
}

export interface IBinary {
  fileName: string;
  value: string; // z.B. "mesh(not uploaded yet):Dateiname"
  uri?: string | null;
  type?: string;
  size?: number;
}

/**
 * Source classification for a file shown by BinaryElement V2.
 * - 'local'    : selected by the user, not yet uploaded (value === "")
 * - 'mesh'     : uploaded to Mesh (value starts with "mesh:")
 * - 'actanova' : stored in ActaNova (any other non-empty value)
 */
export type BinarySource = 'local' | 'mesh' | 'actanova';

/**
 * IBinaryFile is the single source of truth for the list UI.
 * - `binary`: the canonical IBinary object used by backend and Summary rendering
 * - `file`  : the original File object (only for local selections); null once
 *             the backend returns a server-side IBinary for the same file
 * - `source`: local | mesh | actanova
 * - `fileType`: lowercased extension derived from binary.fileName (no dot)
 */
export interface IBinaryFile {
  binary: IBinary;
  file: File | null;
  source: BinarySource;
  fileType: string;
}

export interface ITranslationSet {
  [key: string]: string;
}

export const editModeMap = {
  1: 'inline',
  2: 'dialog',
  4: 'reference',
  8: 'reference - dialog',
  16: 'inline - dialog',
};

export interface IDataAggregationListEntry {
  id: string;
  deletable: boolean;
  visible: boolean;
  editable: boolean;
  creatable: boolean;
  sortIndex: number;
  name: string;
  value: IDataAggregationValue;
}

export interface IDataAggregationListInitialEntry {
  value: {
    [key: string]: IDataAggregationListInitialEntryValue;
  };
  deletable?: boolean;
  editable?: boolean;
  id?: string;
  name?: string;
  sortIndex?: number;
  visible?: boolean;
}

export interface IDataAggregationListInitialEntryValue {
  visible: boolean;
  mandatory?: boolean;
  value?: any;
  possibleValues?: IPossibleValue[];
  possibleValueSetID?: string;
  renderingType?: string;
  name?: string;
  type?: string;
  updateOnChange?: boolean;
  validation?: IValidation;
}

export interface IDataAggregationValue {
  [key: string]: {
    id: string;
    name: string;
    visible: boolean;
    editable: boolean;
    renderingType?: string;
    hostObjectId?: string;
    hostObjectIndex?: number;
    value: any;
  };
}

/**
 * This interface is stored in Uistate. There is chance, that the individual overlay has specific logic and additional properties.
 */
export type OverlayAdditionalProps =
  | IOverlayExtraCommonProps
  | IOverlayInfoExtraProps
  | IOverlayLeaveExtraProps
  | IOverlayFullScreenExtraProps
  | IOverlayAggregateDeleteConfirmationExtraProps
  | IOverlaySubmitExtraProps;

export interface OverlayState {
  overlayType: OverlayTypeEnum;
  additionalProps?: OverlayAdditionalProps;
}

export interface ICommonOverlayProps {
  uiState: UIState;
  formState: FormState;
  updateState: (stateOption: StatesToUpdateEnum) => void;
  initializeForm: (uiState: UIState, formState: FormState, data?: any, newFormId?: string) => void;
  restService: RestService;
  handleFormCancel?: (
    callback: any,
    isOnlyCloseDialog?: boolean,
  ) => Promise<void>;
  handleExpertExcelDownload?: () => void;
}

export interface IDefaultOverlayProps extends ICommonOverlayProps {
  title?: string;
  buttonList?: OverlayButtonDefinition[];
  contentMessages?: string[];
  contentElementNodes?: React.ReactNode;
  footerMessages?: string[];
  closeAble?: boolean;
  buttonObjects?: any[];
  onClose?: () => void;
  debugInfo?: ApiResponse | Error;
}

export interface IOverlayExtraCommonProps {
  response?: ApiResponse | Error; //for debug
}
export interface IDebugOverlayExtraProps extends IOverlayExtraCommonProps {
  headerTitle?: string;
  debugSections?: IDebugSectionProps[];
}

export interface IOverlayFullScreenExtraProps extends IOverlayExtraCommonProps {
  loadingText?: IOverlayFullScreenTextEnum;
}

export interface IOverlayInfoExtraProps extends IOverlayExtraCommonProps {
  overlayInfoOptions: IOverlayInfoOptions;
  handleCancel: () => Promise<void>;
}

export interface IOverlayLeaveExtraProps extends IOverlayExtraCommonProps {
  link: string;
}

export interface IOverlayAggregateDeleteConfirmationExtraProps
  extends IOverlayExtraCommonProps {
  parentJsonPath: string;
  index: number;
}

export interface IOverlaySubmitExtraProps extends IOverlayExtraCommonProps {
  errorMsgs?: IErrorMessage[];
}

export interface OverlayButtonDefinition {
  key: string;
  title: string;
  onPress: () => void;
  loading?: boolean;
  buttonStyle?: any;
  textStyle?: any;
  wrapperStyle?: any;
  disabled?: boolean;
}

export interface OverlayButtonRowProps {
  buttons?: OverlayButtonDefinition[];
  buttonObjects?: any[];
}

export interface IOverlayHeaderProps {
  uiState: UIState;
  updateState: (state: StatesToUpdateEnum) => void;
  title?: string;
  closeAble?: boolean;
  onClose?: () => void;
}

export interface LoadingOverlayScreenProps {
  visible: boolean;
  text?: string;
}

export interface IOverlayFooterProps {
  footerMessages?: (string | undefined)[];
}

export interface IAggregateAddButtonProps {
  id?: string;
  className?: string;
  onPress: () => any;
  showButtonText?: boolean;
}

type formEditModeValues = keyof typeof editModeMap;

export interface ApiResponse {
  success: boolean;
  responseCode?: number;
  responseData?: any;
  exception?: Error;
}
