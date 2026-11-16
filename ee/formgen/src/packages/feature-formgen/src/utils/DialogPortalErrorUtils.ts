import { OverlayTypeEnum } from '../custom/OverlayTypes';
import { StatesToUpdateEnum } from '../constants/enums/StateToUpdateEnum';
import { ApiResponse, FormState, UIState } from '../types/interfaces';
import * as overlayUtils from './OverlayUtils';

/**
 * Error handling for the problem types of the ActaNova DialogPortal (RFC 9457
 * problem details, forwarded by the acta-nova plugin as
 * `{ "type": "urn:dialog-portal:error:...", "title": ..., "status": ... }`; in debug
 * mode the complete body from ActaNova, e.g. with `detail`).
 */

const PROBLEM_TYPE_PREFIX = 'urn:dialog-portal:error:';

/** Problem type the plugin uses when ActaNova sent no `type` (RFC 9457 default). */
const PROBLEM_TYPE_ABOUT_BLANK = 'about:blank';

/**
 * Problem types where retrying cannot help: the form host object cannot be
 * used anymore, so the only way out is to restart the form.
 */
const RESTART_ONLY_PROBLEM_TYPES = [
  'urn:dialog-portal:error:badrequest',
  'urn:dialog-portal:error:forbidden',
  'urn:dialog-portal:error:dialog-portal-form-host-object:not-found',
  'urn:dialog-portal:error:dialog-portal-form-host-object:in-error-state',
  'urn:dialog-portal:error:dialog-portal-form-host-object:entering-error-state',
];

/**
 * @returns the DialogPortal problem type of a failed request, or `undefined`
 *   if the response is not a DialogPortal problem
 */
export const getDialogPortalProblemType = (
  response?: ApiResponse | Error,
): string | undefined => {
  const apiResponse = response instanceof Error ? undefined : response;
  const type =
    apiResponse && !apiResponse.success ? apiResponse.responseData?.type : undefined;

  if (
    typeof type === 'string' &&
    (type.startsWith(PROBLEM_TYPE_PREFIX) || type === PROBLEM_TYPE_ABOUT_BLANK)
  ) {
    return type;
  }
  return undefined;
};

/**
 * @returns `true` if the form can only be restarted, `false` if retrying may
 *   help (internal-error and every unknown problem type)
 */
export const isRestartOnlyProblem = (problemType?: string): boolean =>
  !!problemType && RESTART_ONLY_PROBLEM_TYPES.includes(problemType);

/**
 * Show the DialogPortal error overlay if the failed request returned a
 * DialogPortal problem type.
 *
 * @returns `true` if the overlay was shown and the caller must not handle the
 *   error any further, `false` otherwise
 */
export const handleDialogPortalProblem = (
  response: ApiResponse | Error | undefined,
  uiState: UIState,
  formState: FormState,
  updateState?: (state: StatesToUpdateEnum) => void,
): boolean => {
  if (!getDialogPortalProblemType(response)) {
    return false;
  }

  overlayUtils.setOverlay(
    uiState,
    formState,
    OverlayTypeEnum.DIALOG_PORTAL_ERROR,
    updateState,
    { response },
  );
  return true;
};
