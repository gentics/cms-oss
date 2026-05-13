import {
  ICommonOverlayProps,
  IOverlayExtraCommonProps,
} from '../../../types/interfaces';
import DefaultOverlay from '../DefaultOverlay';


const LoadOverlayWithoutOnClose: React.FC<ICommonOverlayProps> = (
  props: ICommonOverlayProps,
) => {
  const t = props.formState.t;
  const extraProps: IOverlayExtraCommonProps = props.uiState.overlayState
    .additionalProps as IOverlayExtraCommonProps;
  return (
    <DefaultOverlay
      {...props}
      title={t('form_OverlayErrorTitle')}
      contentMessages={[t('form_OverlayErrorContent')]}
      footerMessages={[
        t('form_error_overlay__first_line'),
        t('form_error_overlay__second_line'),
      ]}
      closeAble={true}
      debugInfo={extraProps ? extraProps.response : undefined}
    />
  );
};

export default LoadOverlayWithoutOnClose;
