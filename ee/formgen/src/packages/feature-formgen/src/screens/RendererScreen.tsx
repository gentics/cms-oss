import React, { useEffect, useMemo, useRef, useState } from 'react';
import {
  View,
  ScrollView,
  Platform,
  StyleSheet,
  useWindowDimensions,
  Modal,
  TouchableOpacity,
  BackHandler,
  UIManager,
  findNodeHandle,
  InteractionManager,
} from 'react-native';
import FormProgressScreenPart from './screen-parts/FormProgress';
import StickyLayerScreenPart from './screen-parts/StickyLayer';
import ErrorBlockScreenPart from './screen-parts/ErrorBlockScreenPart';
import {
  ApiResponse,
  FormState,
  IData,
  IElementBeingFocusedInfo,
  IElementIdentifier,
  IErrorMessage,
  IFeatures,
  IForm,
  IFormComponentAttributes,
  IFormFlowClass,
  IFormFlowData,
  IFormFlowSteps,
  IFormPage,
  IOverlayLeaveExtraProps,
  IPossibleValue,
  IReportResult,
  ISchemaFieldProperties,
  ISummaryDetailsUser,
  SummaryOptions,
  UIState,
} from '../types/interfaces';
import { RestService } from '../services/RestService';
import { ScreenComponents } from '../custom/ScreenComponents';
import ReadOnlyFormScreen from './ReadOnlyFormScreen';
import {
  endANRequest,
  getFeatureActive,
  setNormalizedSchema,
  startANRequest,
} from '../utils/Utils';
import * as validationUtils from '../utils/ValidationUtils';
import i18n from '@andp/core/src/services/i18n';
import * as overlayUtils from '../utils/OverlayUtils';
import * as formEventsUtils from '../utils/FormEventsUtils';
import * as utils from '../utils/Utils';
import { StatesToUpdateEnum } from '../constants/enums/StateToUpdateEnum';
import { isPageValid } from '../utils/ValidationUtils';
import { CaptchaService } from '../services/CaptchaService';
import { TFunction } from 'i18next';
import * as mockFormularUtils from '../utils/MockFormularUtils';
import { OverlayTypeEnum, overlayTypes } from '../custom/OverlayTypes';
import { IOverlayFullScreenTextEnum } from '../constants/enums/IOverlayFullScreenTextEnum';
import { postInitForm, preInitForm } from '../custom/HooksUtils';
import { IMockFormTypeEnum } from '../constants/enums/IMockFormTypeEnum';
import AndpText from '@andp/core/src/components/AndpText';
import AndpIcon from '@andp/core/src/components/AndpIcon';
import theme, { expertSearchTableTheme } from '@andp/core/src/styles/theme';
import { Host } from 'react-native-portalize';
import { useMicroAppSDK } from '@andp/core/src/sdk/MicroAppSDK';
import { ButtonProvider } from './essentials/ButtonProvider';
import { useFormMirrorStore } from '../state/useFormMirrorState';
import { ElementRenderer } from './essentials/ElementRenderer';
import { PluginNameEnum } from '../constants/enums/PluginNameEnum';
import { changeToPage } from '../utils/UiUtils';
import CMSFormCaptcha from '../components/elements/cms/CmsCaptcha/CMSFormCaptcha';

const RendererScreen: React.FC<{
  formId: string;
  businessObjectId: string;
  config: IFormFlowData;
  features: IFeatures;
  language: string;
  isLoggedIn: boolean;
  apiToken: string;
  devMode: boolean;
  mockForm: boolean;
  t: TFunction;
  endpoint: string;
  translationUrl?: string;
  successContent?: string;
  prefillContent?: string;
  userData?: ISummaryDetailsUser;
  detailUrl?: string;
  hasDetail?: boolean;
  pluginName: PluginNameEnum;
  isFormgridPreview?: boolean;
  formJson?: string;
  currentPage?: number;
  selectedElementId?: string;
}> = (props) => {
  // Nutze useWindowDimensions, um die Bildschirmbreite zu ermitteln
  const { width, height } = useWindowDimensions();
  const isWeb = Platform.OS === 'web';
  const containerWidth = width < 770 ? '95%' : '80%';
  const { andpNavigation, andpSharedFunctions, andpSharedProps } =
    Platform.OS != 'web' ? useMicroAppSDK() : {};
  const isMobile = Platform.OS !== 'web';
  const scrollableExpersearchTable = expertSearchTableTheme.table.scrollable;

  function getCorrectANCulture(lng: string, features: IFeatures): string {
    // If full culture is already provided (e.g. "de-AT"), just return it
    if (lng && lng.length > 2) return lng;

    // Otherwise, resolve the best matching locale from feature_languages
    let locales: string[] = [];
    const lngFeature = getFeatureActive('feature_languages', features);

    if (lngFeature?.languages && Array.isArray(lngFeature.languages)) {
      locales = lngFeature.languages as string[];
    } else {
      // sensible defaults
      locales = ['en-AT', 'de-AT'];
    }

    let localeLng: string | null = null;
    for (const locale of locales) {
      if (locale.startsWith(lng)) {
        localeLng = locale;
      }
    }

    if (localeLng == null) {
      console.warn(
        'There is no locale in the list of available languages (' +
        locales.toString() +
        ') for the current language (' +
        lng +
        ')',
      );
      localeLng = lng; // fallback to short language
    }

    return localeLng;
  }

  // Initialer Zustand
  const initialFormState: FormState = {
    form: {},
    formid: '',
    schema: {
      key: '',
      version: '',
      properties: {},
    },
    normalizedSchema: {},
    data: {
      formID: '',
    },
    businessObjectID: '',
    culture: 'de-AT',
    features: {},
    t: ((key: string) => key) as TFunction<'translation', undefined>,
    submitted: false,
    handleFocusError: handleFocusError,
    expertMultipleHandler: () => {},
    expertSearchCheckMarkCount: 0,
    errorOpenSubAggNonce: 0,
    isLoggedIn: props.isLoggedIn,
    detailUrl: props.detailUrl,
    hasDetail: props.hasDetail,
    pluginName: props.pluginName as PluginNameEnum,
    captchaConfig: undefined,
  };

  // Initialer Zustand
  const initialUIState: UIState = {
    flowPageIndex: 0,
    flowList: [],
    currentPage: 0,
    lastPage: 0,
    initDone: false,
    loading: false,
    isExpertSearch: false,
    isReadOnlyForm: false,
    showErrorBlockOnInvalid: false,
    dialogForm: {},
    overlayState: {
      overlayType: OverlayTypeEnum.NONE,
    },
    isMockForm: props.mockForm,
    isDevMode: props.devMode,
    andpNavigation: andpNavigation,
    linkCallback:
      Platform.OS != 'web' && andpSharedFunctions != undefined
        ? (link: string, isForm?: boolean) =>
            andpSharedFunctions.get('services.linkCallback')?.(link, isForm)
        : () => {},
  };

  //i18n.changeLanguage(props.culture.split('-')[0]);
  const t = props.t;

  //Don't manipulate state from redux store
  // -> due to "A state mutation was detected between dispatches, in the path '...'. This may cause incorrect behavior."
  // since we don't want to do deep copy every time when modifying the state => we use the useState
  const [formState, setFormState] = useState<FormState>(initialFormState);
  const [uiState, setUiState] = useState<UIState>(initialUIState);

  // Global mirror state (Zustand) – keeps a read-only copy of formState/uiState
  const setMirrorFormState = useFormMirrorStore((state) => state.setFormState);
  const setMirrorUIState = useFormMirrorStore((state) => state.setUIState);
  const resetMirrorState = useFormMirrorStore((state) => state.reset);

  const [clickedLinkOutside, setClickedLinkOutside] = useState<{
    overlayLeaveExtraProps: IOverlayLeaveExtraProps;
    clicked: boolean;
  }>({
    overlayLeaveExtraProps: { link: '' },
    clicked: false,
  });

  const currentPage = uiState.currentPage;
  const uiSchema: IForm = formState.form;
  const lastPage = uiState.lastPage;
  let initialFullCulture = getCorrectANCulture(props.language, props.features);
  if(props.pluginName == PluginNameEnum.ACTA_NOVA){
    initialFullCulture = utils.getCorrectANCulture(props.language, props.features!);
  }

  let restService = new RestService(props.endpoint, props.pluginName, props.apiToken, initialFullCulture, props.language);

  // Verwende den ComponentType-Typ für FlowPage, um sicherzustellen, dass TypeScript den Typ korrekt erkennt
  const [flowPageReactClass, setFlowPageReactClass] = useState<string>('');

  const [flowPageObj, setFlowPageObj] = useState<IFormFlowClass>();

  // Mockform selection -- Set Mockformtype static HERE
  const [mockFormType, setMockFormType] = useState<IMockFormTypeEnum>(
    IMockFormTypeEnum.DEFAULT_AN_FORM,
  );

  const [showMockModal, setShowMockModal] = useState(false);
  const mockOptions: { label: string; value: IMockFormTypeEnum }[] = [
    { label: 'Acta-Nova Formular', value: IMockFormTypeEnum.DEFAULT_AN_FORM },
    { label: 'CMS Formular', value: IMockFormTypeEnum.DEFAULT_CMS_FORM },
    { label: 'Extended Formular', value: IMockFormTypeEnum.EXTENDED_FORM },
    { label: 'Suchformular', value: IMockFormTypeEnum.EXPERTSEARCH_FORM },
  ];

  // // ExpertSearch
  const [hasSearched, setHasSearched] = useState<boolean>(false);

  /**
   * Holds the heavy expert search result payload separately from the
   * global FormState. Keeping this data out of FormState prevents large
   * result sets from triggering unnecessary re-renders and shallow copies
   * of the entire form state tree.
   */
  const [reportResult, setReportResult] = useState<IReportResult | null>(null);

  // Neuer Ref für den ScrollView
  const scrollViewRef = useRef<ScrollView>(null);
  // Neuer Ref für den ErrorBlock
  const errorBlockRef = useRef<View>(null);
  // ref of the first error element
  const firstErrorElementRef = useRef<any>(null);
  const firstErrorElementInfo = useRef<any>(null);
  const elementBeingFocused = useRef<any>(null);
  // Ref for Summary Print Area:
  const printRef = useRef(null);
  const currentScrollYRef = useRef(0);

  const scrollToElement = (elementRef?: any) => {
    if (!scrollViewRef.current) return;

    // Bestimme das zu scrollende Element: entweder das übergebene elementRef oder, falls nicht vorhanden, den errorBlockRef
    const refToScrollTo =
      elementRef && elementRef.current
        ? elementRef
        : errorBlockRef && errorBlockRef.current
          ? errorBlockRef
          : null;

    // If a valid reference is found, measure its position within the ScrollView
    if (refToScrollTo) {
      // Check if measureLayout is a function before calling it.
      // Check if measureLayout is a function before calling it.
      if (Platform.OS === 'web') {
        if (typeof refToScrollTo.current.measureLayout === 'function') {
          // Web specific fallback or standard behavior
          // On web, getInnerViewNode might be required or just the ref
          const relativeNode = scrollViewRef.current.getInnerViewNode
            ? scrollViewRef.current.getInnerViewNode()
            : scrollViewRef.current;
          refToScrollTo.current.measureLayout(
            relativeNode,
            (x: number, y: number) => {
              const offset = height * 0.1;
              const scrollToY = y - offset < 0 ? 0 : y - offset;
              scrollViewRef.current?.scrollTo({
                x: 0,
                y: scrollToY,
                animated: true,
              });
            },
            (error: any) => {
              console.error('Error measuring layout on web:', error);
            },
          );
        }
      } else {
        try {
          const targetHandle = findNodeHandle(refToScrollTo.current);
          const scrollViewHandle = findNodeHandle(scrollViewRef.current);
          if (targetHandle && scrollViewHandle) {
            UIManager.measureLayout(
              targetHandle,
              scrollViewHandle,
              (error: any) => {
                console.error('Error measuring layout:', error);
              },
              (x: number, y: number, width: number, height: number) => {
                const offset = height * 0.1; // 10% of the screen height
                const scrollToY = y - offset < 0 ? 0 : y - offset;
                scrollViewRef.current?.scrollTo({
                  x: 0,
                  y: scrollToY,
                  animated: true,
                });
              },
            );
          } else {
            // Fallback
            scrollViewRef.current.scrollTo({ x: 0, y: 0, animated: true });
          }
        } catch (e) {
          console.error('Error in native measureLayout', e);
          scrollViewRef.current.scrollTo({ x: 0, y: 0, animated: true });
        }
      }
    } else {
      // Fallback: if measureLayout is not available, scroll to top
      scrollViewRef.current.scrollTo({ x: 0, y: 0, animated: true });
    }
  };

  let FlowPage = ScreenComponents[flowPageReactClass];
  let OverlayComponent;
  //if mock => mockType
  useEffect(() => {
    if (props.mockForm) {
      const selectedPluginName = formState.pluginName;

      Object.assign(formState, initialFormState);
      Object.assign(uiState, initialUIState);
      // Preserve the plugin selection made in the mock selector.
      formState.pluginName = selectedPluginName;
    }

    handleInitFormHooks(uiState, formState);

    if (props.mockForm) {
      updateState(StatesToUpdateEnum.ALL);
    }
  }, [mockFormType]);

  useEffect(() => {
    if (
      uiState.flowList.length > 0 &&
      !uiState.isExpertSearch &&
      uiState.initDone
    ) {
      const currentFlowObj = uiState.flowList[uiState.flowPageIndex];
      if (currentFlowObj) {
        const newComp = currentFlowObj.reactClass;
        // Sicherstellen, dass newComp eine gültige Komponente ist
        if (newComp && newComp !== flowPageReactClass) {
          setFlowPageReactClass(newComp);
          setFlowPageObj(currentFlowObj);
        }
      }
    } else if (uiState.initDone) {
      const newComp = 'FormScreen';
      if (newComp && newComp !== flowPageReactClass) {
        setFlowPageReactClass(newComp);
        setFlowPageObj(uiState.flowList.find((x) => x.reactClass === newComp));
      }
    }
  }, [uiState.initDone, uiState.flowPageIndex, currentPage]);

  // Android back-button (digital or hardware) -> triggers leaveOverlay
  useEffect(() => {
    if (isWeb) return;
    const onBackPress = () => {
      triggerLeaveOverlay({  link: ''  });
      return true;
    };;

    const subscription = BackHandler.addEventListener(
      'hardwareBackPress',
      onBackPress,
    );

    return () => subscription.remove();
  }, []);

  useEffect(() => {
    if (!isWeb) return;
    if (clickedLinkOutside.clicked == true) {
      triggerLeaveOverlay(clickedLinkOutside.overlayLeaveExtraProps);
    }
  }, [clickedLinkOutside]);

  /**
   * Scrolls the form back to the top whenever the user navigates to a new page.
   *
   * Why the platform split?
   * - On **web**, using ScrollView's `scrollTo()` with `animated: false` can still
   *   trigger a visible scroll animation because the browser respects the CSS
   *   `scroll-behavior: smooth` property. To bypass this, we directly set
   *   `scrollTop = 0` on the underlying DOM node — which is always instant.
   * - On **native** (iOS/Android), `scrollTo()` with `animated: false` works as
   *   expected and jumps to the top immediately without any transition.
   */
  useEffect(() => {
    if (Platform.OS === 'web') {
      try {
        // getInnerViewNode() returns the inner content wrapper of the ScrollView.
        // Its parentNode is the actual scrollable DOM element.
        const scrollNode = (scrollViewRef.current as any)?.getInnerViewNode?.()?.parentNode;
        if (scrollNode && scrollNode.scrollTop > 0) scrollNode.scrollTop = 0;
        else window.scrollTo({top: 0, left: 0, behavior: 'instant'});
      } catch (e) {
        // Fallback: if DOM access fails for any reason, use the standard RN method.
        // This may animate on web depending on CSS scroll-behavior, but is better than nothing.
        console.warn('RendererScreen: Could not access ScrollView DOM node for instant scroll reset. Falling back to animated scroll.', e);
        scrollViewRef.current?.scrollTo({ x: 0, y: 0, animated: false });
      }
    } else {
      scrollViewRef.current?.scrollTo({ x: 0, y: 0, animated: false });
    }
  }, [uiState.flowPageIndex, currentPage]);

  useEffect(() => {
    if (props.isFormgridPreview) return;
    if (
      (!uiState.overlayState ||
        uiState.overlayState.overlayType === OverlayTypeEnum.NONE) &&
      formState.data.preconditions &&
      formState.data.preconditions.length > 0
    ) {
      overlayUtils.setOverlay(
        uiState,
        formState,
        OverlayTypeEnum.PRECONDITION,
        updateState,
      );
    }
  }, [uiState.overlayState, formState.data.preconditions]);

  /**
   * Ziel: Full-Screen-Loading-Overlay nur anzeigen, wenn die normalen Inhalte (FlowPage) noch nicht geladen bzw. angezeigt werden können.
   * Wir können diese Logik nicht außerhalb auslagern, da es zu endless rerender loop führt
   *
   * Wenn also keine dieser Bedingungen erfüllt ist, dann bedeutet dies, dass die
   * Hauptinhalte noch nicht bereit oder nicht sichtbar sind – und daher muss der
   * Full-Screen-Loading-Overlay angezeigt werden.
   *
   * Ist hingegen mindestens eine der Bedingungen erfüllt, wird angenommen, dass der
   * Ladeprozess abgeschlossen ist und das Overlay ausgeblendet werden soll.
   */
  useEffect(() => {
    // const shouldShowLoading = !(
    //   (FlowPage && uiState.isReadOnlyForm) ||
    //   (FlowPage &&
    //     uiState.isExpertSearch &&
    //     uiSchema &&
    //     uiSchema !== undefined &&
    //     uiSchema !== null) ||
    //   (FlowPage &&
    //     uiSchema !== undefined &&
    //     uiSchema !== null &&
    //     uiSchema.pages?.length! > 0)
    // );

    if (!uiState.initDone && !props.isFormgridPreview) {
      overlayUtils.setOverlay(
        uiState,
        formState,
        OverlayTypeEnum.FULL_SCREEN_LOADING,
        updateState,
        { loadingText: IOverlayFullScreenTextEnum.LoadPage },
      );
    }
  }, []);

  useEffect(() => {
    // Seed the global mirror store once the component is mounted.
    // It will be kept in sync by _updateFormState / _updateUIState.
    setMirrorFormState(formState);
    setMirrorUIState(uiState);

    // Optional: reset the mirror store when this renderer unmounts
    // so that no stale state is left over if another form mounts later.
    return () => {
      resetMirrorState();
    };
    // We intentionally only run this on mount/unmount; subsequent
    // updates are propagated via the updateState helper.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const _updateFormState = () => {
    // Always clone the current local formState so React can detect the change
    // and then push the same snapshot into the global mirror store.
    const nextFormState = { ...formState };
    setFormState(nextFormState);
    setMirrorFormState(nextFormState);
  };

  const _updateUIState = () => {
    // Same pattern for uiState: keep the local state as the single mutable source
    // and mirror a cloned snapshot into the global store.
    const nextUiState = { ...uiState };
    setUiState(nextUiState);
    setMirrorUIState(nextUiState);
  };

  const updateState = (stateOption: StatesToUpdateEnum) => {
    switch (stateOption) {
      case StatesToUpdateEnum.FormState:
        _updateFormState();
        break;
      case StatesToUpdateEnum.UIState:
        _updateUIState();
        break;
      case StatesToUpdateEnum.ALL:
        _updateFormState();
        _updateUIState();
        break;
    }
  };

  const handleInitFormHooks = async (
    uiState: UIState,
    formState: FormState,
    data?: any,
    newFormId?: string,
  ) => {
    await preInitForm(uiState, formState, newFormId);
    const response: ApiResponse = await initForm(data, newFormId);
    await postInitForm(uiState, formState, response, newFormId);
    updateState(StatesToUpdateEnum.ALL);
  };

  /**
   * method for setting the first error Eelement which should be focused
   * @param refContentCurrent should be the content of ref
   */
  const setFirstErrorElementRef = (refContentCurrent: any) => {
    firstErrorElementRef.current = refContentCurrent;
  };

  const setElementBeingFocused = (
    refContentCurrent: IElementBeingFocusedInfo | undefined | null,
  ) => {
    elementBeingFocused.current = refContentCurrent;
  };

  /**
   * Loads prefilldata from dom #egov-template--prefill element
   * checks if feature "feature_prefill" is active
   * check if prefilldata is valid
   * will skip invalid / outdate fields and values
   *
   * expected content is json object with keys matching formData property keys and string values
   * for catalog fields the value is expected to be one of the possibleValues identifiers
   *
   * {
   *  "form-field-id0": "value",
   *  "form-field-id-with-string-value": "this is the value to be prefilled",
   *  "form-field-id-with-catalog-value": "RefID_SpecialdataCatalogValue|..."
   * }
   */
  function getPrefillData(data: IData) {
    if (Platform.OS !== 'web') {
      return data;
    }
    if (
      (Platform.OS === 'web' &&
        utils.isFeatureActive('feature_prefill', formState.features)) ||
      new URLSearchParams(window.location.search).get('feature_prefill') !==
        null
    ) {
      const prefillText = props.prefillContent;
      if (prefillText) {
        try {
          const preFill = JSON.parse(prefillText.trim());
          //only prefill if form was not saved before (no savedPageIndex available)
          if (typeof data.savedPageIndex === 'undefined') {
            for (const [key, value] of Object.entries(preFill)) {
              const fieldToPrefill = data?.properties?.[key];
              const isMultiSelect = Array.isArray(value);
              // check if the field to be prefilled exists
              // check if the value to be prefilled is a string
              // on catalog fields, arrays are also allowed
              if (
                fieldToPrefill &&
                (typeof value === 'string' ||
                  (fieldToPrefill.possibleValues && isMultiSelect))
              ) {
                //only prefill if field is empty / not available
                if (
                  !(fieldToPrefill as Object).hasOwnProperty('value') ||
                  fieldToPrefill?.value === ''
                ) {
                  // special handling for catalog fields:
                  if (
                    fieldToPrefill.possibleValues ||
                    fieldToPrefill.possibleValueSetID
                  ) {
                    //get possible values of field
                    let possibleValues: IPossibleValue[] = [];
                    //backwards compatibility, old possibleValues, remove in next version
                    if (
                      fieldToPrefill.possibleValues &&
                      fieldToPrefill.possibleValues.length > 0
                    ) {
                      possibleValues = fieldToPrefill.possibleValues;
                    } else if (
                      data.possibleValueSets &&
                      fieldToPrefill.possibleValueSetID
                    ) {
                      const possibleValueSet = data.possibleValueSets.find(
                        (i) =>
                          i.possibleValueSetID ===
                          fieldToPrefill.possibleValueSetID,
                      );
                      if (possibleValueSet?.possibleValues) {
                        possibleValues = possibleValueSet.possibleValues;
                      }
                    }

                    const catalogValueToPrefill =
                      // multi select?
                      isMultiSelect
                        ? // get all possible values with matching identifiers
                          possibleValues.filter((possibleValue) =>
                            (value as IPossibleValue[]).some(
                              (v) => v.identifier === possibleValue.identifier,
                            ),
                          )
                        : // get first matching value by identifier
                          possibleValues.find(
                            (possibleValue) =>
                              possibleValue.identifier === value,
                          );
                    if (catalogValueToPrefill) {
                      // multi select fields expect the property `values`, while single select fields expect the property `value`
                      if (isMultiSelect) {
                        fieldToPrefill.values = catalogValueToPrefill;
                        delete fieldToPrefill.oldValue;
                      } else {
                        fieldToPrefill.value = catalogValueToPrefill;
                      }
                    }
                  } else {
                    // usage of not-null assertion operator "!" because we just checked for its existence
                    fieldToPrefill!.value = value;
                  }
                }
              }
            }
          }
        } catch (error) {
          console.error('Parsing prefill json failed');
        }
      }
    }

    return data;
  }

  const handleMissingFormFlow = (): ApiResponse => {
    const missingFormFlowResponse = {
      success: false,
      responseData: undefined,
    } as ApiResponse;

    overlayUtils.setOverlay(
      uiState,
      formState,
      OverlayTypeEnum.LOAD_FORM,
      updateState,
      { response: missingFormFlowResponse },
    );

    console.warn("FormFlow-Config missing.");
    

    return missingFormFlowResponse;
  };

  const applyFormFlow = (responseBody: any): ApiResponse | null => {
    const flowStepsList: IFormFlowSteps[] | undefined = responseBody?.flows;
    if (!flowStepsList || flowStepsList.length === 0) {
      return handleMissingFormFlow();
    }
    const flowId = responseBody.flowId ?? "standard";
    const matchingFlow = flowStepsList.find((x: IFormFlowSteps) => x.id == flowId);
    uiState.flowList = matchingFlow ? matchingFlow.steps : [];
    return null;
  };

  // TODO v8.2 ist setNormalizedSchema notwendig ?????
  const initForm = async (
    data?: any,
    newFormId?: string,
  ): Promise<ApiResponse> => {
    if (newFormId) {
      //redirect to new form
      //revert all state to initial state
      Object.assign(formState, initialFormState);
      Object.assign(uiState, initialUIState);
      // Reset any existing expert search result so the new form starts clean.
      setReportResult(null);
      setHasSearched(false);
    }
    
    const formId = newFormId
      ? newFormId
      : data && data.data && data.data.formID
        ? data.data.formID
        : props.formId;
    const features = props.features;
    const culture = props.language;
    const isLoggedIn = props.isLoggedIn;
    const businessObjectId = props.businessObjectId;
    let requestInitializeData = undefined;

    if (Platform.OS == 'web') {
      const localStorageFormId = localStorage.getItem('formID');
      const localStorageInitializedData =
        localStorage.getItem('initializeData');
      if (localStorageInitializedData && formId === localStorageFormId) {
        requestInitializeData = localStorageInitializedData;
        localStorage.removeItem('initializeData');
        localStorage.removeItem('formID');
      }
    } else {
      const localStorageFormId = andpSharedProps?.get('formgen.formID');
      const localStorageInitializedData = andpSharedProps?.get(
        'formgen.initializeData',
      );
      if (localStorageInitializedData && formId === localStorageFormId) {
        requestInitializeData = localStorageInitializedData;
        andpSharedProps?.removeItem?.('formgen.initializeData');
        andpSharedProps?.removeItem?.('formgen.formID');
      }
    }
    uiState.isDevMode = props.devMode;
    uiState.isMockForm = props.mockForm;

    // Set formId
    if (formId) {
      formState.formid = formId;
    }

    if (features) {
      formState.features = features;
    }

    // fallback to german
    if (culture === '') {
      formState.culture = 'de';
    } else {
      formState.culture = culture;
    }

    formState.t = t;
    formState.isFormgridPreview = props.isFormgridPreview;

    !props.isFormgridPreview && handleChangeLanguage(formState.culture);

    formState.businessObjectID = businessObjectId;
    updateState(StatesToUpdateEnum.ALL);

    

    if (isLoggedIn) {
      // intercept clicks on links away from the form and present the overlayLeave
      // sometimes not everything is loaded yet, so it's a good idea to wait a few ms here before catching click events
      if (Platform.OS == 'web') {
        document.body.addEventListener('click', (e) => {
          const clickedElement = e.target as HTMLElement;
          if (
            clickedElement.tagName.toLowerCase() === 'a' &&
            !clickedElement.closest('#root')
          ) {
            if (
              !uiState.isReadOnlyForm &&
              formState.schema.formType !== 'redirectForm' &&
              !(global as any).submitted // TODO v8.2 ?
            ) {
              e.preventDefault();
              const overlayLeaveExtraProps: IOverlayLeaveExtraProps = {
                link: clickedElement.getAttribute('href') || '',
              };

              setClickedLinkOutside({
                overlayLeaveExtraProps: overlayLeaveExtraProps,
                clicked: true,
              });
              return;
            }
          }
        });
      }
    }

    // 1) Data provided directly
    if (data) {
      formState.form = data.uiSchema;
      const missingFlowResponse1 = applyFormFlow(data);
      if (missingFlowResponse1) return missingFlowResponse1;
      formState.data = getPrefillData(data.data);
      formState.schema = data.schema;
      formState.captchaConfig = data.captchaConfig;
      uiState.lastPage = data.uiSchema.pages.length;

      if (data.schema.formType) {
        uiState.isReadOnlyForm = data.schema.formType === 'readonlyform';
        uiState.isExpertSearch =
          data.schema.formType === 'searchform' ||
          data.schema.formType === 'report';
      }

      setNormalizedSchema(formState, formState.schema, updateState);
      uiState.initDone = true;
      updateState(StatesToUpdateEnum.ALL);

      return data as ApiResponse;
    }

    // 2) Real API load
    if (!props.mockForm) {
      const apiResponse = await restService.loadFormAsync(
        formId,
        businessObjectId,
        requestInitializeData,
      );
      if (!apiResponse.success) {
        overlayUtils.setOverlay(
          uiState,
          formState,
          OverlayTypeEnum.LOAD_FORM,
          updateState,
          { response: apiResponse },
        );
        return apiResponse;
      }

      const responseBody = apiResponse.responseData;
      formState.form = responseBody.uiSchema;
      formState.data = getPrefillData(responseBody.data);
      formState.schema = responseBody.schema;
      formState.captchaConfig = responseBody.captchaConfig;
      uiState.lastPage = responseBody.uiSchema.pages.length;
      const missingFlowResponse2 = applyFormFlow(responseBody);
      if (missingFlowResponse2) return missingFlowResponse2;
      if (
        formState.data.savedPageIndex != null &&
        uiState.lastPage >= formState.data.savedPageIndex
      ) {
        uiState.currentPage = formState.data.savedPageIndex;

        const firstFormScreenIndex = uiState.flowList.findIndex(
          (step) => step.reactClass === 'FormScreen',
        );
        uiState.flowPageIndex = firstFormScreenIndex + currentPage;
        delete formState.data.savedPageIndex;
      } else {
        //no savedPageIndex => form inital loaded ==> we set oldValue attribute
        //on all fields - it`s only used by AN
        // TODO v8.2 This has to be done!!!!!!!!!
      }

      // delete initial ErrorMessages
      formState.data.hasValidationErrors = false;
      Object.entries(formState.data.properties || {}).map(function ([k, v]) {
        if (v?.validationMessage && v.validationMessage !== '') {
          v.validationMessage = '';
        }
        if (v?.ANFieldLoadError && v.ANFieldLoadError === true)
          v.ANFieldLoadError = false;
        if (v?.properties) {
          Object.entries(v.properties || {}).map(function ([k, v]) {
            if (v?.validationMessage && v.validationMessage !== '') {
              v.validationMessage = '';
            }
            if (v?.ANFieldLoadError && v.ANFieldLoadError === true)
              v.ANFieldLoadError = false;
            return;
          });
        }
        return;
      });

      if (responseBody.schema.formType) {
        uiState.isReadOnlyForm =
          responseBody.schema.formType === 'readonlyform';
        uiState.isExpertSearch =
          responseBody.schema.formType === 'searchform' ||
          responseBody.schema.formType === 'report'; // Not sure yet TODO
      }

      setNormalizedSchema(formState, formState.schema, updateState);
      uiState.initDone = true;
      if (apiResponse.responseData.deleted) {
        formState.deleted = true;
      }
      if (Platform.OS == 'web') {
        const search = window.location.search;
        const params = new URLSearchParams(search);
        if (
          params.get('onboarding') &&
          utils.isFeatureActive('feature_onboarding', formState.features)
        ) {
          // TODO v8.2 Lösung für Mobile nötig!!!
          formState.onBoardingFeatureActivated = true;
        }
      }
      overlayUtils.hideOverlay(uiState, updateState);
      redirectFormCheck(formState);
      updateState(StatesToUpdateEnum.ALL);
      return apiResponse;
    }

    // 3) Mock load
    let responseBody: any = "";
    if (props.isFormgridPreview) {
      const emptyForm = { uiSchema: { pages: [] }, schema: {} };
      if (!props.formJson) {
        responseBody = emptyForm;
      } else {
        try {
          responseBody = JSON.parse(props.formJson);
        } catch (e) {
          console.warn('Invalid formJson from Formgrid preview', e);
          responseBody = emptyForm;
        }
      }
      formState.form = responseBody.uiSchema;
      formState.data = { formID: props.formId, properties: {} };
      formState.schema = responseBody.schema;
      formState.captchaConfig = responseBody.captchaConfig;
    } else {
      const mockJSON = await mockFormularUtils.fetchMockDefaultFormular(mockFormType);
      responseBody = mockJSON;
      formState.form = responseBody.uiSchema;
      formState.data = responseBody.data;
      formState.schema = responseBody.schema;
      formState.captchaConfig = responseBody.captchaConfig;
    }
    uiState.lastPage = responseBody.uiSchema.pages.length;
    const missingFlowResponse3 = applyFormFlow(responseBody);
    if (missingFlowResponse3) return missingFlowResponse3;
    if (responseBody.schema.formType) {
      uiState.isReadOnlyForm = responseBody.schema.formType === 'readonlyform';
      uiState.isExpertSearch =
        responseBody.schema.formType === 'searchform' ||
        responseBody.schema.formType === 'report'; // Not sure yet TODO
    }
    setNormalizedSchema(formState, formState.schema, updateState);
    uiState.initDone = true;
    overlayUtils.hideOverlay(uiState, updateState);
    redirectFormCheck(formState);
    updateState(StatesToUpdateEnum.ALL);

    return {
      success: true,
      responseData: responseBody,
    } as ApiResponse;
  };

  const redirectFormCheck = (formState: FormState) => {
    //check if it's a redirectForm and a redirect should be done immediately
    if (
      formState.schema.formType === 'redirectForm' &&
      formState.data &&
      formState.data.formControls &&
      formState.data.formControls.button &&
      formState.data.formControls?.button?.targetUrl !== ''
    ) {
      overlayUtils.setOverlay(
        uiState,
        formState,
        OverlayTypeEnum.FULL_SCREEN_LOADING,
        updateState,
        {
          loadingText: IOverlayFullScreenTextEnum.Redirect,
        },
      );

      utils.redirectFormRedirect(formState.data);
    }
  };

  const triggerLeaveOverlay = (
    overlayLeaveExtraProps: IOverlayLeaveExtraProps,
  ) => {
    overlayUtils.setOverlay(
      uiState,
      formState,
      OverlayTypeEnum.ON_LEAVE,
      updateState,
      overlayLeaveExtraProps,
    );
  };

  useEffect(() => {
    if (!props.isFormgridPreview) return;
    if (props.currentPage == null || props.currentPage < 0) return;
    if (!uiState.initDone) return;
    if (uiState.currentPage === props.currentPage) return;
    changeToPage(uiState, props.currentPage, updateState, true);
  }, [props.isFormgridPreview, props.currentPage, uiState.initDone, uiState.currentPage]);


  useEffect(() => {
    if (props.isFormgridPreview) return;
    if (
      formState.onBoardingFeatureActivated &&
      uiState.overlayState.overlayType == OverlayTypeEnum.NONE
    ) {
      formState.onBoardingFeatureActivated = false;
      overlayUtils.setOverlay(
        uiState,
        formState,
        OverlayTypeEnum.ON_BOARDING,
        updateState,
      );
    }
  }, [
    props.isFormgridPreview,
    formState.onBoardingFeatureActivated,
    updateState,
  ]);

  useEffect(() => {
    if (props.isFormgridPreview) return;
    if (
      formState.deleted &&
      uiState.overlayState.overlayType == OverlayTypeEnum.NONE
    ) {
      formState.deleted = false;
      overlayUtils.setOverlay(
        uiState,
        formState,
        OverlayTypeEnum.UPDATE_OCCURRED,
        updateState,
      );
    }
  }, [props.isFormgridPreview, formState.deleted, updateState]);

  function handleChangeLanguage(lng: any) {
    i18n.changeLanguage(lng);
    if (lng.length > 2) {
      formState.culture = lng;
      restService.setFullCulture(lng);
    } else {
      if (props.pluginName == PluginNameEnum.ACTA_NOVA) {
        const resolved = utils.getCorrectANCulture(lng, formState.features);
        formState.culture = resolved;
        restService.setFullCulture(resolved);
      }
    }
    updateState(StatesToUpdateEnum.ALL);
  }

  const handleDialogFormEdit = async (
    scrollReference: string,
    editModeDialogFormID: string,
    refreshPropertyID: string,
    createAfterIndex?: number,
    rowID?: string,
    isCopy?: boolean,
  ) => {
    if (!formState.data.DialogPortalFormHostObjectID) {
      console.warn('No DialogPortalFormHostObjectID found in formdata');
      return;
    }
    if (!editModeDialogFormID) {
      console.warn('No dialog form id provided for this element');
      return;
    }
    const dialogPortalFormHostObjectID =
      formState.data.DialogPortalFormHostObjectID;

    const rowIdForRequest = isCopy ? undefined : rowID;
    const duplicateRowID = isCopy ? rowID : undefined;

    const showError = true;
    uiState.loading = true;
    updateState(StatesToUpdateEnum.UIState);
    // TODO v8.2 Considering whether to only one fullscreen overlay for the whole aciton

    if (props.mockForm) {
      await formEventsUtils.loadDialogForm(
        uiState,
        formState,
        restService,
        updateState,
        handleInitFormHooks,
        refreshPropertyID,
        scrollReference,
        editModeDialogFormID,
        dialogPortalFormHostObjectID,
        refreshPropertyID,
        rowIdForRequest,
        duplicateRowID,
        createAfterIndex,
        props.mockForm,
      );
      return;
    }
    await formEventsUtils.saveForm(
      uiState,
      formState,
      updateState,
      restService,
      false,
      true,
      async () =>
        await formEventsUtils.loadDialogForm(
          uiState,
          formState,
          restService,
          updateState,
          handleInitFormHooks,
          refreshPropertyID,
          scrollReference,
          editModeDialogFormID,
          dialogPortalFormHostObjectID,
          refreshPropertyID,
          rowIdForRequest,
          duplicateRowID,
          createAfterIndex,
          props.mockForm,
        ),
      () => {},
      showError,
    );
  };

  const handleSubmitDialogForm = async () => {
    const isMock: boolean = props.mockForm;
    if (
      !validationUtils.isPageValid(formState, true, uiState.currentPage, isMock)
    ) {
      formState.data.hasValidationErrors = true;
      uiState.loading = false;
      updateState(StatesToUpdateEnum.ALL);
      setTimeout(function () {
        handleFocusError();
      }, 300);
      return;
    }
    uiState.loading = true;
    updateState(StatesToUpdateEnum.UIState);

    const afterValidation = async (valid?: boolean) => {
      if (!valid) {
        uiState.loading = false;
        updateState(StatesToUpdateEnum.UIState);
        setTimeout(function () {
          handleFocusError();
        }, 300);
        return;
      }
      await formEventsUtils.handleSubmitForm(
        formState,
        uiState,
        updateState,
        restService,
        isMock,
        () =>
          formEventsUtils.revertToMainFormFromDialogForm(
            formState,
            uiState,
            updateState,
            restService,
            handleInitFormHooks,
          ),
      );
    };

    await validationUtils.loadValidationWithHooks(
      restService,
      formState,
      uiState,
      updateState,
      afterValidation,
      undefined,
      isMock,
    );

    return;
  };

  const handleFormCancel = async (
    callback?: any,
    isOnlyCloseDialog = false,
    isRedirect = false,
  ) => {
    // UVEK-856 close dialog form if it is currently open
    if (isOnlyCloseDialog) {
      await formEventsUtils.revertToMainFormFromDialogForm(
        formState,
        uiState,
        updateState,
        restService,
        handleInitFormHooks,
      ); //TODO v8.2 Overlay
      return;
    }

    const errorFunc = (errorResponse: any) => {
      console.error(errorResponse);
      overlayUtils.setOverlay(
        uiState,
        formState,
        OverlayTypeEnum.DEFAULT_ERRORS,
        updateState,
        { response: errorResponse },
      );
    };
    if (isRedirect) {
      overlayUtils.setOverlay(
        uiState,
        formState,
        OverlayTypeEnum.FULL_SCREEN_LOADING,
        updateState,
        {
          loadingText: IOverlayFullScreenTextEnum.Redirect,
        },
      );
    } else {
      startANRequest(() => {
        overlayUtils.setOverlay(
          uiState,
          formState,
          OverlayTypeEnum.FULL_SCREEN_LOADING,
          updateState,
        );
      });
    }

    if (!uiState.isMockForm) {
      let apiResponse = await restService.ANCancelFormANRequest(formState);

      if (!apiResponse.success) {
        endANRequest(uiState, updateState);
        errorFunc(apiResponse);
      } else {
        callback();
      }
    } else {
      setTimeout(() => {
        if (!isRedirect) endANRequest(uiState, updateState);
        callback();
      }, 1000); // MockTimeout
    }
  };

  // Dropdowns - ANDP-420
  function handleSearch(
    jsonPath: string,
    searchTerm: string,
    callback: (possibleValues: any[] | null) => void,
  ): void {
    if (formState.data != null) {
      const localData = JSON.parse(JSON.stringify(formState.data));
      const element = utils.getElementByJsonPath(localData, jsonPath);
      if (!element) return;
      element.searchValue = searchTerm;
      restService.ANLoadSearch(
        searchTerm,
        localData,
        (json: any) => {
          if (typeof json.properties !== 'undefined') {
            const elementToChange = utils.getElementByJsonPath(
              formState.data,
              jsonPath,
            );
            const returnedElement = utils.getElementByJsonPath(json, jsonPath);
            //const possibleValues = utils.getPossibleValues(json, jsonPath);
            let possibleValues: IPossibleValue[] = returnedElement
              ? returnedElement.possibleValues!
              : [];

            if (returnedElement?.possibleValueSetID) {
              json.possibleValueSets.forEach((x: any) => {
                if (
                  returnedElement.possibleValueSetID === x.possibleValueSetID
                ) {
                  possibleValues = x.possibleValues;
                }
              });
            }
            if (
              elementToChange &&
              returnedElement &&
              (returnedElement.possibleValues ||
                returnedElement.possibleValueSetID !== '')
            ) {
              utils.setANFieldLoadError(formState, jsonPath, false);
              callback(possibleValues);
              if (returnedElement.validationMessage) {
                utils.setStateElementByJsonpath(
                  formState.data,
                  jsonPath,
                  returnedElement,
                );
                updateState(StatesToUpdateEnum.ALL);
              } else {
                const elem = utils.getElementByJsonPath(
                  formState.data,
                  jsonPath,
                );
                if (elem?.validationMessage) {
                  elem.validationMessage = undefined;
                  updateState(StatesToUpdateEnum.ALL);
                }
              }
            } else {
              utils.setANFieldLoadError(formState, jsonPath, true);
              updateState(StatesToUpdateEnum.ALL);
              callback(null);
            }
          } else {
            utils.setANFieldLoadError(formState, jsonPath, true);
            updateState(StatesToUpdateEnum.ALL);
            callback(null);
          }
        },
        (exception: any) => {
          utils.setANFieldLoadError(formState, jsonPath, true);
          updateState(StatesToUpdateEnum.ALL);
          callback(null);
        },
        formState,
        uiState,
        jsonPath,
      );
    }
  }

  function handleLazyCatalog(
    jsonPath: string,
    searchTerm: string,
    callback: (possibleValues: any[] | null) => void,
  ): void {
    if (formState.data != null) {
      const localData: IData = JSON.parse(JSON.stringify(formState.data));
      const element = utils.getElementByJsonPath(localData, jsonPath);
      if (!element) return;
      element.searchValue = '*';
      restService.ANLoadLazyCatalog(
        searchTerm,
        localData,
        (json: any) => {
          if (typeof json.properties !== 'undefined') {
            const elementToChange = utils.getElementByJsonPath(
              formState.data,
              jsonPath,
            );
            const returnedElement = utils.getElementByJsonPath(json, jsonPath);
            const possibleValues = utils.getPossibleValues(json, jsonPath, props.pluginName);
            if (
              elementToChange &&
              returnedElement &&
              (returnedElement.possibleValues ||
                returnedElement.possibleValueSetID !== '')
            ) {
              utils.setANFieldLoadError(formState, jsonPath, false);
              callback(possibleValues);
              utils.setStateElementByJsonpath(
                formState.data,
                jsonPath,
                returnedElement,
              );
              if (returnedElement.possibleValueSetID !== '') {
                if ((json as IData).possibleValueSets) {
                  const newPossibleValueSets = (json as IData)
                    .possibleValueSets;
                  const newPossibleValueSet =
                    newPossibleValueSets &&
                    newPossibleValueSets.find(
                      (i: any) =>
                        i.possibleValueSetID ===
                        returnedElement.possibleValueSetID,
                    );
                  if (newPossibleValueSet) {
                    const oldIndex =
                      formState.data.possibleValueSets?.findIndex(
                        (i: any) =>
                          i.possibleValueSetID ===
                          returnedElement.possibleValueSetID,
                      );
                    if (!formState.data.possibleValueSets)
                      formState.data.possibleValueSets = [];
                    if (oldIndex !== undefined && oldIndex >= 0) {
                      formState.data.possibleValueSets[oldIndex] =
                        newPossibleValueSet;
                    } else {
                      formState.data.possibleValueSets.push(
                        newPossibleValueSet,
                      );
                    }
                  }
                }
              }
              updateState(StatesToUpdateEnum.ALL);
            } else {
              utils.setANFieldLoadError(formState, jsonPath, true);
              updateState(StatesToUpdateEnum.ALL);
              callback(null);
            }
          } else {
            utils.setANFieldLoadError(formState, jsonPath, true);
            updateState(StatesToUpdateEnum.ALL);
            callback(null);
          }
        },
        (exception: any) => {
          utils.setANFieldLoadError(formState, jsonPath, true);
          updateState(StatesToUpdateEnum.ALL);
          callback(null);
        },
        formState,
        uiState,
        jsonPath,
      );
    }
  }

  /*
    Autocomplete Methods 
  */
  function handleAutocomplete(
    jsonPath: string,
    autocompleteTerm: string,
    callback: (result: any) => void,
  ): void {
    if (formState.data != null) {
      const localData: IData = JSON.parse(JSON.stringify(formState.data));
      const element = utils.getElementByJsonPath(localData, jsonPath);
      if (!element) return;
      // we set both `value` and `autoCompleteSearchValue` because otherwise AN does not return the correct results
      element.autoCompleteSearchValue = autocompleteTerm;
      element.value = autocompleteTerm;
      restService.ANLoadAutocomplete(
        localData,
        (json: any) => {
          if (typeof json.properties !== 'undefined') {
            const elementToChange = utils.getElementByJsonPath(
              formState.data,
              jsonPath,
            );
            const returnedElement = utils.getElementByJsonPath(json, jsonPath);
            if (elementToChange && returnedElement?.autoCompleteResults) {
              utils.setANFieldLoadError(formState, jsonPath, false);
              callback(returnedElement);
              return;
            }
          }
          utils.setANFieldLoadError(formState, jsonPath, true);
          updateState(StatesToUpdateEnum.ALL);
          callback(null);
        },
        (exception: any) => {
          utils.setANFieldLoadError(formState, jsonPath, true);
          updateState(StatesToUpdateEnum.ALL);
          callback(null);
        },
        formState,
        jsonPath,
        uiState.isMockForm,
      );
    }
  }

  function afterChangeMultiple(elements: IElementIdentifier[]) {
    let validateAN = false;
    let jsonpathAN: string = '';
    let schemaAN: ISchemaFieldProperties | null = null;
    //cancel pending updateOnChange Requests
    formEventsUtils.cancelUpdateOnChange();

    for (let element of elements) {
      if (
        validationUtils.validateField(
          formState,
          element.jsonpath,
          element.schemaID,
          true,
        ).isValid
      ) {
        if (
          validationUtils.needsANValidation(
            formState,
            element.schemaID,
            element.jsonpath,
          )
        ) {
          validateAN = true;
          jsonpathAN = element.jsonpath;
          schemaAN = utils.getSchemaForElement(
            formState.schema,
            element.schemaID,
          );
        }
      }
    }
    if (validateAN) {
      formEventsUtils.updateOnChange(
        formState,
        uiState,
        restService,
        jsonpathAN,
        true,
        schemaAN,
        updateState,
      );
    }
    updateState(StatesToUpdateEnum.FormState);
  }

  function handleExpertSearch(callback?: any) {
    const callbackOverlay = (reportResult?: IReportResult) => {
      if (reportResult && typeof reportResult === 'object') {
        if (reportResult.resultsExceededLimit) {
          overlayUtils.setOverlay(
            uiState,
            formState,
            OverlayTypeEnum.EXPERT_SEARCH_RESULT_LIMIT,
            updateState,
          );
        }
      } else {
        if (
          formState.data.hasValidationErrors &&
          formState.data.hasValidationErrors == true
        ) {
          overlayUtils.setOverlay(
            uiState,
            formState,
            OverlayTypeEnum.EXPERT_SEARCH_TRIGGER_ENDPOINT,
            updateState,
          );
        }
      }
      //updateState(StatesToUpdateEnum.ALL);
    };
    let errorFunc = function (exception: any) {
      if (!hasSearched) setHasSearched(true);
      utils.endANRequest(uiState, updateState);
      //disableLoadingState();
      //state.fullScreenOverlay = false; TODO!
      formState.data.hasValidationErrors = true;

      // Clear any stale expert search result on error to avoid showing
      // outdated data in the UI.
      setReportResult(null);

      updateState(StatesToUpdateEnum.ALL);
      callbackOverlay();
    };
    //enableLoadingState();
    utils.startANRequest(
      () =>
        overlayUtils.setOverlay(
          uiState,
          formState,
          OverlayTypeEnum.FULL_SCREEN_LOADING,
          updateState,
          { loadingText: IOverlayFullScreenTextEnum.ExpertSearch },
        ),
      true,
    ); // disableThreshold for ExpertSearch
    updateState(StatesToUpdateEnum.UIState);

    if (!uiSchema.pages?.length || isPageValid(formState, true, uiState.currentPage, props.mockForm)) {
      if (!props.mockForm) {
        restService.ANcallExpertSearch(
          formState,
          function (jsonData: IData) {
            if (!hasSearched) setHasSearched(true);
            if (jsonData.formID) {
              const data: IData = jsonData;

              // Detach the potentially large expert search result from the
              // form data and store it in a dedicated state slice. This keeps
              // the global FormState lightweight and avoids unnecessary
              // re-renders when the result changes.
              const nextReportResult = data.reportResult;
              data.reportResult = undefined;
              setReportResult(nextReportResult ?? null);

              formState.data = data;
              //disableLoadingState();
              utils.endANRequest(uiState, updateState);
              formState.data.hasValidationErrors = false;
              //formState.customElementErrorMessages = []; TODO
              //formState.fullScreenOverlay = false; TODO
              // updateState(StatesToUpdateEnum.FormState);
              callbackOverlay(nextReportResult);
            } else {
              errorFunc(null);
            }
          },
          errorFunc,
        );
      } else {
        mockFormularUtils.fetchFakeExpertSearchData(function (jsonData: IData) {
          setTimeout(() => {
            if (!hasSearched) setHasSearched(true);
            if (jsonData.formID) {
              const data: IData = jsonData;

              // Detach the potentially large expert search result from the
              // form data and store it in a dedicated state slice. This keeps
              // the global FormState lightweight and avoids unnecessary
              // re-renders when the result changes.
              const nextReportResult = data.reportResult;
              data.reportResult = undefined;
              setReportResult(nextReportResult ?? null);

              formState.data = data;
              //disableLoadingState();
              utils.endANRequest(uiState, updateState);
              formState.data.hasValidationErrors = false;
              //formState.customElementErrorMessages = []; TODO
              //formState.fullScreenOverlay = false; TODO
              // updateState(StatesToUpdateEnum.FormState);
              callbackOverlay(nextReportResult);
            } else {
              errorFunc(null);
            }
          }, 2000);
        });
      }
    } else {
      formState.data.hasValidationErrors = true;
      uiState.showErrorBlockOnInvalid = true;
      //disableLoadingState();
      // state.fullScreenOverlay = false; TODO
      utils.endANRequest(uiState, updateState);
      updateState(StatesToUpdateEnum.ALL);
      //errorFunc(null);
      //callbackOverlay();
    }
  }

  function handleExpertExcelDownload() {
    //enableLoadingState(t('form_excel_download_try'));
    // Local Validation first
    if (isPageValid(formState, true, uiState.currentPage, props.mockForm)) {
      utils.startANRequest(
        () =>
          overlayUtils.setOverlay(
            uiState,
            formState,
            OverlayTypeEnum.FULL_SCREEN_LOADING,
            updateState,
            {
              loadingText:
                IOverlayFullScreenTextEnum.ExpertSearchExcelDownloadTry,
            },
          ),
        true,
      ); // disableThreshold for ExpertSearch
      const ok = function () {
        //disableLoadingState();
        utils.endANRequest(uiState, updateState);
      };
      restService.ANCallExpertExcelDownload(
        formState,
        ok,
        (data: IData) => {
          // if validationError true
          utils.endANRequest(uiState, updateState);
          formState.data = data;
          uiState.showErrorBlockOnInvalid = true;
          updateState(StatesToUpdateEnum.ALL);
        },
        (request: ApiResponse) => {
          // on exception
          overlayUtils.setOverlay(
            uiState,
            formState,
            OverlayTypeEnum.EXPERT_SEARCH_TRIGGER_ENDPOINT,
            updateState,
            { response: request },
          );
        },
      );
    } else {
      formState.data.hasValidationErrors = true;
      uiState.showErrorBlockOnInvalid = true;
      utils.endANRequest(uiState, updateState);
      updateState(StatesToUpdateEnum.ALL);
    }
  }

  const getRawErrorMessages = (): IErrorMessage[] => {
    let errorMessages: IErrorMessage[] = [];
    if (props.isFormgridPreview) return [];

    if (formState.data.hasValidationErrors && formState.data.properties) {
      errorMessages = utils.getErrorMessages(
        formState.data.properties,
        formState.form.pages,
      );
    }
    return errorMessages;
  };

  const getErrorMessages = () => {
    let errorMessages: IErrorMessage[] = getRawErrorMessages();
    // Erstelle ein Set, um doppelte Nachrichtentexte zu filtern.
    const seenMessages = new Set<string>();
    // Filtere die Fehlermeldungen, sodass nur die eindeutigen (basierend auf error.message) übrig bleiben.
    const uniqueErrorMessages = errorMessages.filter((error) => {
      if (seenMessages.has(error.validationMessage)) {
        return false;
      }
      seenMessages.add(error.validationMessage);
      return true;
    });
    return uniqueErrorMessages;
  };

  const updateFirstErrorElementRef = (
    elementProps: IFormComponentAttributes,
    elementRef: React.RefObject<any>,
  ): void => {
    // 0) Fokus aufheben wenn dieses Element kein  fFehler mehr hat
    if (
      elementBeingFocused.current &&
      elementBeingFocused.current.elementID === elementProps.id &&
      elementBeingFocused.current.jsonPath === elementProps.jsonpath &&
      (!elementProps.error || elementProps.error.length === 0)
    ) {
      setElementBeingFocused(null);
      setFirstErrorElementRef(null);
    }

    // Kein global bestimmtes erstes Fehlerziel -> raus
    if (!firstErrorElementInfo.current) return;

    const info = firstErrorElementInfo.current;
    const hasError =
      elementProps.error &&
      elementProps.error !== '' &&
      elementProps.error.length > 0;

    // 1) Fehler NICHT in Aggregat
    if (!info.aggregateID) {
      if (
        hasError &&
        info.elementID === elementProps.id &&
        elementRef?.current
      ) {
        setFirstErrorElementRef(elementRef.current);
        setElementBeingFocused({
          elementID: elementProps.id,
          isSubAggregateElement: false,
          jsonPath: elementProps.jsonpath,
        });
      }
      return;
    }

    // 2) Fehler IN Aggregat
    // Ziel-Unterfeld ist das global erste Fehlerelement
    const isTargetSubField =
      info.aggregateID === elementProps.parentId &&
      info.elementID === elementProps.id;

    const currentFocus = elementBeingFocused.current;

    // b) Sub-Feld darf Aggregat-Container-Fokus ersetzen oder ersten Fokus setzen
    if (isTargetSubField && hasError && elementRef?.current) {
      const extractSubAggSortIndex = (p?: string | null): number => {
        if (!p) return -1;
        const m = p.match(/_(\d+)$/);
        return m ? parseInt(m[1], 10) : -1;
      };

      // a) Wenn schon ein Sub-Element in diesem Aggregat fokussiert wurde -> nicht überschreiben
      if (
        currentFocus &&
        currentFocus.isSubAggregateElement === true &&
        currentFocus.parentAggregateID === info.aggregateID &&
        currentFocus.insideSubaggregatIndex <
          elementProps.insideSubaggregatIndex!
      ) {
        return;
      }
      setFirstErrorElementRef(elementRef.current);
      setElementBeingFocused({
        elementID: elementProps.id,
        isSubAggregateElement: true,
        parentAggregateID: info.aggregateID,
        jsonPath: elementProps.jsonpath,
        insideSubaggregatIndex: elementProps.insideSubaggregatIndex,
      });
      // }
      return;
    }

    // c) Aggregat-Container nur setzen, wenn noch nichts gesetzt (und kein Sub-Feld vorher)
    if (
      info.aggregateID === elementProps.id &&
      elementRef?.current &&
      !currentFocus
    ) {
      setFirstErrorElementRef(elementRef.current);
      setElementBeingFocused({
        elementID: info.aggregateID,
        isSubAggregateElement: false,
        jsonPath: elementProps.jsonpath,
      });
    }
  };

  useEffect(() => {
    if (props.isFormgridPreview) return;
    if (
      props.isLoggedIn &&
      !uiState.isReadOnlyForm &&
      formState.schema.formType !== 'redirectForm' &&
      !formState.paymentState
    ) {
      //add background save / autosave
      const autoSave = () => {
        const nowRounded = Math.floor(Date.now() / 1000);
        if (nowRounded % 6 === 0) {
          if (
            formState.submitted || //TODO v8.2 (global as any).submitted better option?
            uiState.overlayState.overlayType != OverlayTypeEnum.NONE ||
            (global as any).anRequestRunning === true
          ) {
            return;
          }
          console.log('saveFormBackground - check');
          //there need to be a change before a new autosave is triggered
          //no autosave if a save is currently running
          if (
            typeof (global as any).valueIsChanged !== 'undefined' &&
            (global as any).saveRunning !== true
          ) {
            console.log('save');
            formEventsUtils.handleBackgroundFormSave(
              formState,
              uiState,
              updateState,
              restService,
              undefined,
            );
          }
        }
      };
      const intervalId = setInterval(autoSave, 1000);
      return () => clearInterval(intervalId);
    }
  }, [formState.data, currentPage, uiState.overlayState]);

  /**
   * Attempts to scroll to the first error element and set focus on it.
   *
   * This function checks if the reference to the first error element (firstErrorElementRef)
   * is defined. If it exists, it calls the scrollToElement() helper function to smoothly scroll
   * the view so that the error element is visible. After scrolling, if the element supports
   * the focus() method, it sets the focus to that element.
   *
   * Finally, the reference is reset (set to null) to ensure that subsequent validations
   * can set a new first error element reference.
   */
  function handleFocusError() {
    formState.errorOpenSubAggNonce++;
    if (firstErrorElementRef.current) {
      elementBeingFocused.current = null;
      // Scroll to the first error element with a predefined offset.
      scrollToElement(firstErrorElementRef);
      // If the error element is focusable, set focus on it.
      if (firstErrorElementRef.current.focus) {
        setTimeout(() => {
          // added setTime to ensure the scroll is completed before focusing. Otherwise the animation is not smooth
          firstErrorElementRef.current.focus();
          firstErrorElementRef.current = null;
        }, 500);
      }
    } else if (CaptchaService.hasError()) {
      // Fallback: no field-level error, but captcha is unsolved → scroll to widget.
      const captchaRef = CaptchaService.getRef();
      if (captchaRef?.current) {
        scrollToElement(captchaRef);
      }
    } else {
      // Reset the first error element reference so it can be set anew on the next validation.
      firstErrorElementRef.current = null;
      // for the case error happens inside aggregat but the element inside it are not opened ("detailsinformation ausklappen")
    }
  }

  const handlePageChange = (pageNum: number) => {
    if (pageNum >= 0 && pageNum <= lastPage) {
      console.log('Current Page:', currentPage);
      console.log('Changing to Page:', pageNum);
      uiState.currentPage = pageNum;
    } else if (pageNum > lastPage) {
      console.log('Cannot change to page beyond lastPage:', lastPage);
    } else if (pageNum < 0) {
      console.log('Cannot change to a negative page index.');
    }
    updateState(StatesToUpdateEnum.UIState);
  };

  const uniqueErrorMsgs = useMemo(() => getErrorMessages(), [formState]);

  const rawErrorMsgs = useMemo(() => getRawErrorMessages(), [formState]);

  if (rawErrorMsgs && rawErrorMsgs.length > 0) {
    firstErrorElementInfo.current = utils.findVisuallyFirstErrorElement(
      rawErrorMsgs,
      formState.form,
    );
  } else {
    firstErrorElementInfo.current = null;
  }

  if (props.isFormgridPreview && uiSchema !== undefined && uiSchema !== null && uiSchema.pages?.length! > 0) {
    const currentFormPageUiSchema: IFormPage | undefined =
      formState.form?.pages![uiState.currentPage];
    const elements = currentFormPageUiSchema.elements;
    return (
      <><Host>
        <ScrollView
          style={[
            localStyles.scrollView,
            // @ts-ignore
            Platform.OS == "web" && !scrollableExpersearchTable ? { overflow: 'auto' } : null,
            Platform.OS === 'web' &&
                          formState.isFormgridPreview &&
                          ({ cursor: 'not-allowed' } as any),
          ]}
          scrollEnabled={true}

          ref={scrollViewRef}
          scrollEventThrottle={16}
          contentContainerStyle={{ flexGrow: 1 }}
        >
          <FormProgressScreenPart
            updateState={updateState}
            restService={restService}
            handlePageChange={handlePageChange}
            currentPage={currentPage}
            formState={formState}
            uiState={uiState}
            handleFormCancel={handleFormCancel}
            errorMsgs={uniqueErrorMsgs}
          />
          <ElementRenderer
            {...props}
            summary={SummaryOptions.No}
            elements={elements}
            parentId=""
            jsonpath=""
            t={props.t}
            uiState={uiState}
            formState={formState}
            restService={restService}
            updateState={updateState}
            isLoggedIn={props.isLoggedIn}
            handleSearch={handleSearch}
            handleExpertSearch={handleExpertSearch}
            selectedElementId={props.selectedElementId}
          />
        </ScrollView></Host>
      </>)
  }

  let renderScreen = <></>;
  if (FlowPage && uiState.isReadOnlyForm) {
    renderScreen = (
      <View style={{ flex: 1 }}>
        {Platform.OS != 'web' ? (
          <FormProgressScreenPart
            updateState={updateState}
            restService={restService}
            handlePageChange={handlePageChange}
            currentPage={currentPage}
            formState={formState}
            uiState={uiState}
            handleFormCancel={handleFormCancel}
            errorMsgs={uniqueErrorMsgs}
          />
        ) : null}
        <ScrollView contentContainerStyle={{ flexGrow: 1 }}>
          <View
            style={{
              width: containerWidth,
              marginLeft: 'auto',
              marginRight: 'auto',
              paddingBottom: 50,
            }}
          >
            <ReadOnlyFormScreen
              uiState={uiState}
              formState={formState}
              updateState={updateState}
              restService={restService}
              t={t}
            />
          </View>
        </ScrollView>
      </View>
    );
  } else if (
    FlowPage &&
    uiSchema !== undefined &&
    uiSchema !== null &&
    !uiState.isReadOnlyForm
  ) {
    const hasPages = !!uiSchema.pages?.length;
    renderScreen = (
      <>
        {Platform.OS != 'web' ? (
          <FormProgressScreenPart
            updateState={updateState}
            restService={restService}
            handlePageChange={handlePageChange}
            currentPage={currentPage}
            formState={formState}
            uiState={uiState}
            handleFormCancel={handleFormCancel}
            errorMsgs={uniqueErrorMsgs}
          />
        ) : null}
        <ScrollView
          style={[
            localStyles.scrollView,
            // @ts-ignore
            Platform.OS == 'web' && !scrollableExpersearchTable
              ? { overflow: 'auto' }
              : null,
          ]}
          scrollEnabled={true}
          ref={scrollViewRef}
          scrollEventThrottle={16}
          contentContainerStyle={{ flexGrow: 1 }}
        >
          {Platform.OS == 'web' &&
          flowPageReactClass !== 'SuccessScreen' &&
          uiSchema.pages?.length! > 0 &&
          hasPages ? (
            <FormProgressScreenPart
              updateState={updateState}
              restService={restService}
              handlePageChange={handlePageChange}
              currentPage={currentPage}
              formState={formState}
              uiState={uiState}
              handleFormCancel={handleFormCancel}
              errorMsgs={uniqueErrorMsgs}
            />
          ) : null}
          {Platform.OS === 'web' &&
            formState.data.hasValidationErrors &&
            uiState.showErrorBlockOnInvalid && (
              <View ref={errorBlockRef}>
                <ErrorBlockScreenPart
                  formState={formState}
                  uiState={uiState}
                  errorMsgs={uniqueErrorMsgs}
                />
              </View>
            )}
          <FlowPage
            updateState={updateState}
            setFirstErrorElementRef={setFirstErrorElementRef}
            elementBeingFocused={elementBeingFocused}
            setElementBeingFocused={setElementBeingFocused}
            formState={formState}
            uiState={uiState}
            t={t} //TODO v8.2 wirklich notwendig ? kann man nicht via useTranslation lösen?
            restService={restService}
            devMode={props.devMode}
            mockForm={props.mockForm}
            isLoggedIn={props.isLoggedIn}
            handleDialogFormEdit={handleDialogFormEdit}
            variables={flowPageObj?.variables}
            handleSearch={handleSearch}
            handleLazyCatalog={handleLazyCatalog}
            handleAutocomplete={handleAutocomplete}
            scrollToElement={scrollToElement}
            successContent={props.successContent}
            structure={formState.normalizedSchema}
            handleExpertSearch={handleExpertSearch}
            hasSearched={hasSearched}
            isSearch={uiState.isExpertSearch}
            updateFirstErrorElementRef={updateFirstErrorElementRef}
            // setCallbackExpertSearch={setCallbackExpertSearch}
            userData={props.userData}
            printRef={printRef}
            afterChangeMultiple={afterChangeMultiple}
            rawErrorMsgs={rawErrorMsgs}
            reportResult={reportResult}
          ></FlowPage>
          {flowPageReactClass !== 'SuccessScreen' && (
            <CMSFormCaptcha
              formState={formState}
              config={{
                provider: 'friendly',
                siteKey: 'FCMMSGLUK81DK3C6',
                mode: 'button',
              }}
            />
          )}
        </ScrollView>
        <StickyLayerScreenPart
          updateState={updateState}
          flowObj={flowPageObj}
          formState={formState}
          uiState={uiState}
          t={t}
          screenType={flowPageReactClass}
          restService={restService}
          devMode={props.devMode}
          mockForm={props.mockForm}
          handleSubmitDialogForm={handleSubmitDialogForm}
          handleFormCancel={handleFormCancel}
          scrollToElement={scrollToElement}
          handleExpertExcelDownload={handleExpertExcelDownload}
          handleExpertSearch={handleExpertSearch}
          setElementBeingFocused={setElementBeingFocused}
          updateFirstErrorElementRef={updateFirstErrorElementRef}
          printRef={printRef}
          isLoggedIn={props.isLoggedIn}
        />
      </>
    );
  } else {
    // This case is handled in useeffect, because otherwise showing fullscreenoverlay leads to endless rerendering process.
  }

  if (
    uiState.overlayState &&
    uiState.overlayState.overlayType != undefined &&
    uiState.overlayState.overlayType !== OverlayTypeEnum.NONE
  ) {
    OverlayComponent = overlayTypes[uiState.overlayState.overlayType];
  }
  return !props.isFormgridPreview ? (
    <>
      {OverlayComponent && (
        <OverlayComponent
          uiState={uiState}
          formState={formState}
          updateState={updateState}
          initializeForm={handleInitFormHooks}
          restService={restService}
          handleFormCancel={handleFormCancel}
          handleExpertExcelDownload={handleExpertExcelDownload}
        />
      )}
      {props.mockForm && (
        <>
          <View
            style={[
              !isMobile && localStyles.overlayButtonContainer,
              isMobile && localStyles.mobileOverlayButtonContainer,
            ]}
          >
            <TouchableOpacity
              onPress={() => {
                overlayUtils.setOverlay(
                  uiState,
                  formState,
                  OverlayTypeEnum.ON_LEAVE,
                  updateState,
                );
              }}
              style={[
                !isMobile && localStyles.overlayButton,
                isMobile && localStyles.mobileOverlayButton,
              ]}
            >
              <AndpText
                style={{ color: theme.colors.textOnColor, fontWeight: 'bold' }}
              >
                Show Overlay
              </AndpText>
            </TouchableOpacity>
            <TouchableOpacity
              onPress={() => setShowMockModal(true)}
              style={[
                !isMobile && localStyles.overlayButton,
                isMobile && localStyles.mobileOverlayButton,
              ]}
            >
              <AndpText
                style={{ color: theme.colors.textOnColor, fontWeight: 'bold' }}
              >
                Mock auswählen
              </AndpText>
            </TouchableOpacity>
          </View>

          <Modal
            visible={showMockModal}
            transparent
            animationType="fade"
            onRequestClose={() => setShowMockModal(false)}
          >
            <TouchableOpacity
              activeOpacity={1}
              onPressOut={() => setShowMockModal(false)}
            >
              <View
                style={[
                  !isMobile && localStyles.modalOverlay,
                  isMobile && localStyles.mobileModalOverlay,
                ]}
              >
                {mockOptions.map((opt) => (
                  <TouchableOpacity
                    key={opt.value}
                    onPress={() => {
                      switch (opt.value) {
                        case IMockFormTypeEnum.DEFAULT_AN_FORM:
                          formState.pluginName = PluginNameEnum.ACTA_NOVA;
                          break;
                        case IMockFormTypeEnum.DEFAULT_CMS_FORM:
                          formState.pluginName = PluginNameEnum.CMS_FORMS;
                          break;
                      }

                      setMockFormType(opt.value);
                      setShowMockModal(false);
                    }}
                    style={{
                      flexDirection: 'row',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                      paddingVertical: 10,
                      paddingHorizontal: 12,
                      gap: 10,
                    }}
                  >
                    <AndpText>{opt.label}</AndpText>
                    {mockFormType === opt.value && (
                      <AndpIcon
                        style={{ justifyContent: 'flex-end' }}
                        name="checkMark"
                        color={theme.colors.colorAccent}
                      ></AndpIcon>
                    )}
                  </TouchableOpacity>
                ))}
              </View>
            </TouchableOpacity>
          </Modal>
        </>
      )}
      <ButtonProvider definitions={flowPageObj?.buttons}>
        {Platform.OS == 'web' ? <Host>{renderScreen}</Host> : renderScreen}
      </ButtonProvider>
    </>
  ) : <></>;
};
const localStyles = StyleSheet.create({
  modalOverlay: {
    position: 'absolute',
    top: 90,
    left: 10,
    width: 160,
    backgroundColor: theme.colors.backgroundPrimary,
    borderRadius: 8,
    paddingVertical: 8,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.3,
    shadowRadius: 4,
    elevation: 5,
  },
  mobileModalOverlay: {
    position: 'absolute',
    top: 195,
    right: 15,
    width: 200,
    backgroundColor: theme.colors.backgroundPrimary,
    borderRadius: 8,
    paddingVertical: 8,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.3,
    shadowRadius: 4,
    elevation: 5,
  },

  overlayButtonContainer: {
    position: 'absolute',
    top: 10,
    left: 10,
    zIndex: 1000,
  },

  mobileOverlayButtonContainer: {
    position: 'absolute',
    top: 90,
    right: 5,
    zIndex: 1000,
  },
  mobileOverlayButton: {
    paddingVertical: 6,
    paddingHorizontal: 10,
    backgroundColor: theme.colors.colorAccent,
    borderRadius: 4,
    marginBottom: 6,
  },
  mobileOverlayButtonText: {
    color: theme.colors.textOnColor,
    fontWeight: 'bold',
    fontSize: 12,
  },

  overlayButton: {
    padding: 8,
    marginTop: 4,
    backgroundColor: theme.colors.colorAccent,
    borderRadius: 4,
    zIndex: 1000,
  },

  fullScreenContainer: {
    flex: 1,
    height: '100%',
  },
  scrollView: {
    padding: theme.space.large,
  },
  scrollContent: {},
  contentWrapper: {
    height: '100%',
    marginLeft: 'auto',
    marginRight: 'auto',
    marginTop: '5%',
  },
  stickyFooter: {
    bottom: 0,
    left: 0,
    right: 0,
    backgroundColor: 'white',
    borderTopWidth: 1,
    borderTopColor: '#CCCCCC',
    paddingVertical: 10,
    paddingHorizontal: 10,
    zIndex: 10,
  },
});

export default RendererScreen;
