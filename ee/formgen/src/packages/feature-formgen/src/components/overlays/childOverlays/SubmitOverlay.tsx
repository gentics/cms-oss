import { Platform } from 'react-native';
import {
  ICommonOverlayProps,
  IErrorMessage,
  IOverlaySubmitExtraProps,
  UIState,
} from '../../../types/interfaces';
import DefaultOverlay from '../DefaultOverlay';

const SubmitOverlay: React.FC<ICommonOverlayProps> = (
  props: ICommonOverlayProps,
) => {
  const t = props.formState.t;
  const uiState: UIState = props.uiState;
  const additionalProps: IOverlaySubmitExtraProps = uiState.overlayState
    .additionalProps as IOverlaySubmitExtraProps;
  const errorMsgs: IErrorMessage[] | undefined = additionalProps?.errorMsgs;
  errorMsgs;

  return (
    <DefaultOverlay
      {...props}
      closeAble={true}
      title={t('form_OverlayErrorOnSubmitTitle')}
      contentMessages={[
        t('form_OverlayErrorOnSubmitContent'),
        ...(errorMsgs || []).map((x) => x.validationMessage),
      ]}
      footerMessages={[
        t('form_error_overlay__first_line'),
        t('form_error_overlay__second_line'),
      ]}
      debugInfo={additionalProps ? additionalProps.response : undefined}
    />
  );
};

export default SubmitOverlay;
