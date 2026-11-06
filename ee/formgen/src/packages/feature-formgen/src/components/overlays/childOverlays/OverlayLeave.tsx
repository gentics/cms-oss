import {
  IDefaultOverlayProps,
  FormState,
  IOverlayLeaveExtraProps,
  UIState,
  OverlayButtonDefinition,
} from '../../../types/interfaces';
import DefaultOverlay from '../DefaultOverlay';
import * as overlayUtils from '../../../utils/OverlayUtils';
import * as formEventUtils from '../../../utils/FormEventsUtils';
import { Platform } from 'react-native';

const OverlayLeave: React.FC<IDefaultOverlayProps> = (
  props: IDefaultOverlayProps,
) => {
  const t = props.formState.t;
  const uiState: UIState = props.uiState;
  const formState: FormState = props.formState;
  const updateState = props.updateState;
  const additionalProps: IOverlayLeaveExtraProps = uiState.overlayState
    .additionalProps as IOverlayLeaveExtraProps;

  const content = props.uiState.dialogForm.isDialogFormOpen
    ? t('form_dialog_exit_without_save_overlay_content')
    : t('form_save_overlay_content');

  const callBackLeave = () => {
    console.log('done cancel');
    if (Platform.OS != "web") {
      if (additionalProps != null && additionalProps.link != null && additionalProps.link.length > 0) {
        uiState.linkCallback(additionalProps.link);
      } else {
        uiState.andpNavigation.previous();
      }
    }
    else if (additionalProps != null && additionalProps.link != null) {
      window.location.href = additionalProps.link;
    }
  };

  const getButtonList = (): OverlayButtonDefinition[] => {
    let buttonList: OverlayButtonDefinition[] = [{
      key: 'leave-overlay-leave-btn',
      title: t('form_save_overlay_button_leave'),
      onPress: async () => {
        console.log('leave');
        await props.handleFormCancel?.(callBackLeave, false);
      },
    }];

    // Add Save button if it is not dialogform
    if (!props.uiState.dialogForm.isDialogFormOpen && !props.uiState.isReadOnlyForm) {
      buttonList.push({
        key: 'leave-overlay-save-btn',
        title: t('form_save_overlay_button_save'),
        onPress: async () => {
          if (Platform.OS != "web") {
            overlayUtils.hideOverlay(uiState, updateState);
            await new Promise((resolve) => setTimeout(resolve, 500));
            await formEventUtils.saveForm(
              uiState,
              formState,
              updateState,
              props.restService,
              true,
              true,
              async () => {
                callBackLeave();
              },
              () => { },
              true,
            );
          }
          else if (additionalProps.link) {
            await formEventUtils.saveForm(
              uiState,
              formState,
              updateState,
              props.restService,
              true,
              true,
              async () => {
                console.log('done saving');
                window.location.href = additionalProps.link;
              },
              () => { },
              true,
            );
          }
        },
      });
    }

    buttonList.push({
      key: 'leave-overlay-cancel-btn',
      title: t('form_save_overlay_button_cancel'),
      onPress: async () => { overlayUtils.hideOverlay(uiState, updateState); },
    });

    return buttonList;
  }

  return (
    <DefaultOverlay
      {...props}
      title={t('form_save_overlay_title')}
      contentMessages={[content]}
      buttonList={getButtonList()}
      footerMessages={[
        t('form_save_overlay__first_line'),
        t('form_save_overlay__second_line'),
      ]}
      debugInfo={additionalProps ? additionalProps.response ?? undefined : undefined}
    />
  );
};

export default OverlayLeave;
