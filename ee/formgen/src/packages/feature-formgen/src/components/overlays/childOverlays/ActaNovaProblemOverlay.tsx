import {
  ApiResponse,
  ICommonOverlayProps,
  IOverlayExtraCommonProps,
  OverlayButtonDefinition,
} from '../../../types/interfaces';
import DefaultOverlay from '../DefaultOverlay';
import * as overlayUtils from '../../../utils/OverlayUtils';
import * as formEventsUtils from '../../../utils/FormEventsUtils';
import { OverlayTypeEnum } from '../../../custom/OverlayTypes';
import { IOverlayFullScreenTextEnum } from '../../../constants/enums/IOverlayFullScreenTextEnum';
import {
  getActaNovaProblemType,
  isRestartOnlyProblem,
} from '../../../utils/ActaNovaProblemUtils';

/**
 * Shown when ActaNova answered with a problem type (`urn:dialog-portal:error:...`).
 *
 * - "Neu starten" deletes the saved form data and initializes the form again.
 * - "Wiederholen" loads the form again without deleting the saved data. It is
 *   only offered when retrying may help (internal-error and unknown types).
 * - Closing leaves the form (as the load error overlay does), the saved data is kept.
 *   So the user is not trapped when the same problem occurs again after a restart.
 */
const ActaNovaProblemOverlay: React.FC<ICommonOverlayProps> = (
  props: ICommonOverlayProps,
) => {
  const t = props.formState.t;
  const additionalProps = props.uiState.overlayState
    .additionalProps as IOverlayExtraCommonProps;
  const response = additionalProps?.response as ApiResponse | undefined;
  // If a dialog form is open, the main form is reloaded and its data deleted.
  const mainFormState =
    props.uiState.dialogForm?.mainFormState?.formState ?? props.formState;
  const formId = mainFormState.formid;

  // Passing the form ID resets the whole form state before loading (initForm).
  const reloadForm = () =>
    props.initializeForm(props.uiState, props.formState, undefined, formId);

  const restartForm = async () => {
    // Loading overlay while the saved data is deleted, initForm keeps it until the form is loaded.
    overlayUtils.setOverlay(
      props.uiState,
      props.formState,
      OverlayTypeEnum.FULL_SCREEN_LOADING,
      props.updateState,
      { loadingText: IOverlayFullScreenTextEnum.LoadPage },
    );
    if (!props.uiState.isMockForm) {
      const cancelResponse = await props.restService.cancelFormRequest(mainFormState);
      if (!cancelResponse.success) {
        // Reload anyway: loading the form validates the saved data again.
        console.error(cancelResponse);
      }
    }
    reloadForm();
  };

  const buttonList: OverlayButtonDefinition[] = [
    {
      key: 'acta-nova-problem-overlay-restart-btn',
      title: t('form_dialog_portal_error_restart_button'),
      onPress: restartForm,
    },
  ];

  if (!isRestartOnlyProblem(getActaNovaProblemType(response))) {
    buttonList.push({
      key: 'acta-nova-problem-overlay-retry-btn',
      title: t('form_dialog_portal_error_retry_button'),
      onPress: reloadForm,
    });
  }

  return (
    <DefaultOverlay
      {...props}
      title={t('form_dialog_portal_error_title')}
      contentMessages={[t('form_dialog_portal_error_content')]}
      buttonList={buttonList}
      closeAble={true}
      onClose={() => formEventsUtils.cancelRedirect(props.uiState, mainFormState.returnUrl)}
      debugInfo={response}
    />
  );
};

export default ActaNovaProblemOverlay;
