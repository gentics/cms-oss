import { OverlayTypeEnum } from '../custom/OverlayTypes';
import { StatesToUpdateEnum } from '../constants/enums/StateToUpdateEnum';
import { ApiResponse, FormState, UIState } from '../types/interfaces';
import * as overlayUtils from './OverlayUtils';

/**
 * Error handling for the problem responses of ActaNova (RFC 9457
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
 * @returns the ActaNova problem type of a failed request, or `undefined`
 *   if the response is not an ActaNova problem
 */
export const getActaNovaProblemType = (
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
 * Show the ActaNova problem overlay if the failed request returned an
 * ActaNova problem type.
 *
 * @returns `true` if the overlay was shown and the caller must not handle the
 *   error any further, `false` otherwise
 */
export const handleActaNovaProblem = (
  response: ApiResponse | Error | undefined,
  uiState: UIState,
  formState: FormState,
  updateState?: (state: StatesToUpdateEnum) => void,
): boolean => {
  if (!getActaNovaProblemType(response)) {
    return false;
  }

  overlayUtils.setOverlay(
    uiState,
    formState,
    OverlayTypeEnum.ACTA_NOVA_PROBLEM,
    updateState,
    { response },
  );
  return true;
};
