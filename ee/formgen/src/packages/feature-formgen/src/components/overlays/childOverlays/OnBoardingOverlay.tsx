import {
  ICommonOverlayProps,
  IOverlayExtraCommonProps,
} from '../../../types/interfaces';
import DefaultOverlay from '../DefaultOverlay';

const OnBoardingOverlay: React.FC<ICommonOverlayProps> = (
  props: ICommonOverlayProps,
) => {
  const t = props.formState.t;
    const additionalProps: IOverlayExtraCommonProps = props.uiState.overlayState
          .additionalProps as IOverlayExtraCommonProps;
  return (
    <DefaultOverlay
      {...props}
      title={t('form_overlay_onboardingForm_title')}
      contentMessages={[t('form_overlay_onboardingForm_content')]}
      footerMessages={[
        t('form_save_overlay__first_line'),
        t('form_save_overlay__second_line'),
      ]}
      closeAble={true}
      debugInfo={additionalProps ? additionalProps.response : undefined}
    />
  );
};

export default OnBoardingOverlay;
