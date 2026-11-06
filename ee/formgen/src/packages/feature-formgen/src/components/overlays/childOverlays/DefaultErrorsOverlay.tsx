import { Platform } from 'react-native';
import {
  ICommonOverlayProps,
  IOverlayExtraCommonProps,
} from '../../../types/interfaces';
import DefaultOverlay from '../DefaultOverlay';

const DefaultErrorsOverlay: React.FC<ICommonOverlayProps> = (
  props: ICommonOverlayProps,
) => {
  const isMobile = Platform.OS != "web";
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
      debugInfo={extraProps ? extraProps.response : undefined}
      closeAble={isMobile}
      onClose={isMobile ? () => props.uiState.andpNavigation.previous() : undefined}
    />
  );
};

export default DefaultErrorsOverlay;
