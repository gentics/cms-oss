import { IOverlayTypes } from '../types/interfaces';
import * as Overlays from './OverlayDefinitions';

export enum OverlayTypeEnum {
  PRECONDITION,
  ON_BOARDING,
  UPDATE_OCCURRED, // * show error/info when formdata was deleted
  OVERLAY_INFO,
  LOAD_FORM,
  LOAD_VALIDATION,
  LOAD_SAVE,
  LOAD_SUBMIT,
  EXPERT_SEARCH_RESULT_LIMIT,
  EXPERT_SEARCH_TRIGGER_ENDPOINT,
  DEFAULT_ERRORS,
  ON_LEAVE,
  FULL_SCREEN_LOADING,
  AGGREGATE_DELETE_CONFIRMATION,
  SUBMIT_USER_INPUT_FAILURE,
  NONE,
}

export const overlayTypes: IOverlayTypes = {
  [OverlayTypeEnum.LOAD_FORM]: Overlays.LoadOverlayWithCancelRedirectOnClose,
  [OverlayTypeEnum.LOAD_VALIDATION]: Overlays.LoadOverlayWithCancelRedirectOnClose,
  [OverlayTypeEnum.LOAD_SAVE]: Overlays.LoadOverlayWithoutOnClose,
  [OverlayTypeEnum.LOAD_SUBMIT]: Overlays.LoadOverlayWithoutOnClose,
  [OverlayTypeEnum.UPDATE_OCCURRED]: Overlays.UpdateFormOverlay,
  [OverlayTypeEnum.ON_BOARDING]: Overlays.OnBoardingOverlay,
  [OverlayTypeEnum.OVERLAY_INFO]: Overlays.OverlayInfo,
  [OverlayTypeEnum.PRECONDITION]: Overlays.PreconditionOverlay,
  [OverlayTypeEnum.DEFAULT_ERRORS]: Overlays.DefaultErrorsOverlay,
  [OverlayTypeEnum.EXPERT_SEARCH_RESULT_LIMIT]:
    Overlays.ExpertSearchTableOverlayResultLimit,
  [OverlayTypeEnum.EXPERT_SEARCH_TRIGGER_ENDPOINT]:
    Overlays.DefaultErrorsOverlay,
  [OverlayTypeEnum.ON_LEAVE]: Overlays.OverlayLeave,
  [OverlayTypeEnum.FULL_SCREEN_LOADING]: Overlays.FullScreenOverlay,
  [OverlayTypeEnum.AGGREGATE_DELETE_CONFIRMATION]:
    Overlays.AggregateDeleteConfirmationOverlay,
  [OverlayTypeEnum.SUBMIT_USER_INPUT_FAILURE]: Overlays.SubmitOverlay,
};
