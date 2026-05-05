import {
  ApiResponse,
  FormState,
  IOverlayInfoExtraProps,
  IOverlayInfoOptions,
  OverlayAdditionalProps,
  UIState,
} from '../types/interfaces';
import { StatesToUpdateEnum } from '../constants/enums/StateToUpdateEnum';
import * as utils from './Utils';
import { OverlayTypeEnum } from '../custom/OverlayTypes';
import { IDebugSectionProps } from '../components/overlays/DebugSection';
/**
 * Method for set a specific overlay
 * @param uiState
 * @param formState
 * @param updateState
 * @param overlayType
 * @param additionalProps extra variables that are used in specific overlays.
 * @param response is used for loading action (for example load form or validation), we need that because we can trigger the debugOverlay using this information.
 * @returns
 */
export const setOverlay = (
  uiState: UIState,
  formState: FormState,
  overlayType: OverlayTypeEnum,
  updateState?: (state: StatesToUpdateEnum) => void,
  additionalProps?: OverlayAdditionalProps,
) => {
  if (overlayType == OverlayTypeEnum.OVERLAY_INFO) {
    setOverlayInfo(
      uiState,
      additionalProps as IOverlayInfoExtraProps,
      updateState,
    );
  } else {    
    uiState.overlayState = {
      overlayType: overlayType,
      additionalProps: additionalProps,
    };
    updateState && updateState(StatesToUpdateEnum.ALL);
  }
};

/**
 * shows the configurable modal overlay with info texts from formgrid
 * not closeable - action buttons will be provided
 * @param uiState
 * @param updateState
 * @param overlayInfoOptions
 * @param handleCancel method which will be called before calling the default handleOverlayClose
 */
export const setOverlayInfo = (
  uiState: UIState,
  extraProps: IOverlayInfoExtraProps,
  updateState?: (state: StatesToUpdateEnum) => void,
) => {
  const overlayInfoOptions: IOverlayInfoOptions = extraProps.overlayInfoOptions;
  uiState.overlayState = {
    overlayType: overlayInfoOptions.isShowOverlay
      ? OverlayTypeEnum.OVERLAY_INFO
      : OverlayTypeEnum.NONE,
    additionalProps: {
      overlayInfoOptions: overlayInfoOptions,
      handleCancel: extraProps.handleCancel,
    },
  };
  updateState && updateState(StatesToUpdateEnum.UIState);
};

export const showDebugSections = (
  uiState: UIState,
  formState: FormState,
  debugInfo?: ApiResponse | Error,
): IDebugSectionProps[] | undefined => {
  const debugFeatureActive: boolean = utils.isFeatureActive(
    'feature_debug',
    formState.features,
  );
  let debugTitle = '';
  if (debugInfo && debugFeatureActive) {
    if (debugInfo instanceof Error) {
      //any errors occurs, but not because of network request
      debugTitle = debugInfo.message;
    } else if (debugInfo.success == false) {
      if (debugInfo.exception && debugInfo.exception.message) {
        //Exceptions occurs during the network request (client side)
        debugTitle = debugInfo.exception.message;
      } else {
        //response received from acta nova
        const responseData = debugInfo.responseData || {};
        if (responseData.message) {
          debugTitle = responseData.message;
        }
        let requestUserContext = decodeURIComponent(
          responseData.requestUserContext,
        );
        if (requestUserContext == 'undefined') {
          requestUserContext = '';
        }
        const responseContent = responseData.response || '';
        const requestBodyContent = responseData.requestBody || '';
        const stackTraceContent = responseData.stackTrace
          ? JSON.stringify(responseData.stackTrace, null, 2)
          : '';
        // Es wird gar kein content angezeigt
        if (
          debugTitle == '' &&
          responseContent == '' &&
          requestBodyContent == '' &&
          stackTraceContent == ''
        ) {          
          return;
        }
        return [
          {
            label: 'Message:',
            content: debugTitle,
            isMultiline: false,
          },
          {
            label: 'User-Context:',
            content: requestUserContext,
            isMultiline: false,
          },
          {
            label: 'Response:',
            content: responseContent,
            isMultiline: true,
          },
          {
            label: 'Request Body:',
            content: requestBodyContent,
            isMultiline: true,
          },
          {
            label: 'Stack-Trace:',
            content: stackTraceContent,
            isMultiline: true,
          },
        ];
      }
    } else {
      console.warn(
        'Unusual case, debugInfo is not an instance of Error or ApiResponse',
      );
    }
  }
};

export const hideOverlay = (
  uiState: UIState,
  updateState?: (state: StatesToUpdateEnum) => void,
) => {
  uiState.overlayState = {
    overlayType: OverlayTypeEnum.NONE,
  };
  updateState && updateState(StatesToUpdateEnum.UIState);
};

export function getOverlayInfoOptions(
  overlayOptions: IOverlayInfoOptions | undefined,
  value: string,
): IOverlayInfoOptions {
  if (!overlayOptions?.isShowOverlay) {
    return {
      isShowOverlay: false,
    };
  }
  let printAbleValue = '';
  if (typeof value === 'string') {
    printAbleValue = value;
  }
  return {
    isShowOverlay: overlayOptions.isShowOverlay || false,
    texts: {
      title: overlayOptions.texts?.title,
      textPre: overlayOptions.texts?.textPre,
      textPost: overlayOptions.texts?.textPost,
    },
    value: printAbleValue,
  };
}
