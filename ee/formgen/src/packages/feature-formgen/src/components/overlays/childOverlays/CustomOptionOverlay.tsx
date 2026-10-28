import React, { useEffect, useMemo, useRef, useState } from 'react';
import { View, TextInput, TextInputInstance } from 'react-native';
import {
  ICommonOverlayProps,
  IOverlayCustomOptionExtraProps,
  OverlayButtonDefinition,
} from '../../../types/interfaces';
import DefaultOverlay from '../DefaultOverlay';
import SharedInputComponent from '../../elements/shared/components/InputElementComponent';
import { inputElementStyles } from '../../../styles/common/common-style';
import { inputElementStyles as customInputElementStyles } from '../../../styles/custom/custom-style';
import { mergeStyles } from '@andp/core/src/AndpUtils';
import * as overlayUtils from '../../../utils/OverlayUtils';

/**
 * Overlay used by the catalog dropdown element when `customOption` is enabled.
 *
 * It shows a single text input. On confirm the entered value is handed back to
 * the caller through `additionalProps.onConfirm(...)`, which adds and selects the
 * value in the dropdown. Cancelling invokes `additionalProps.onCancel(...)` so the
 * special option gets deselected again. The overlay keeps only the local input
 * text; all persistence happens in the calling element.
 */
const CustomOptionOverlay: React.FC<ICommonOverlayProps> = (
  props: ICommonOverlayProps,
) => {
  const t = props.formState.t;
  const additionalProps = props.uiState.overlayState
    .additionalProps as IOverlayCustomOptionExtraProps;

  const [value, setValue] = useState('');
  const [isFocused, setIsFocused] = useState(false);
  const inputRef = useRef<TextInputInstance>(null);
  const inputStyles = useMemo(
    () => mergeStyles(inputElementStyles, customInputElementStyles),
    [],
  );

  // Preserve the previous autoFocus behavior (SharedInputComponent has no
  // autoFocus prop): focus the field once when the overlay mounts.
  useEffect(() => {
    inputRef.current?.focus();
  }, []);

  const labelText = t('form_catalog_customOption_placeholder');

  const close = () => overlayUtils.hideOverlay(props.uiState, props.updateState);

  const buttonList: OverlayButtonDefinition[] = [
    {
      key: 'custom-option-overlay-cancel-btn',
      title: t('form_cancel'),
      onPress: () => {
        additionalProps?.onCancel && additionalProps.onCancel();
        close();
      },
    },
    {
      key: 'custom-option-overlay-confirm-btn',
      title: t('form_catalog_customOption_confirm'),
      disabled: value.trim().length === 0,
      onPress: () => {
        const trimmed = value.trim();
        if (trimmed.length === 0) {
          return;
        }
        additionalProps?.onConfirm && additionalProps.onConfirm(trimmed);
        close();
      },
    },
  ];

  return (
    <DefaultOverlay
      {...props}
      closeAble={true}
      onClose={() => {
        additionalProps?.onCancel && additionalProps.onCancel();
        close();
      }}
      title={
        additionalProps?.title || t('form_catalog_customOption_overlay_title')
      }
      buttonList={buttonList}
      contentElementNodes={
        <View style={{ marginVertical: 12 }}>
          <SharedInputComponent
            formState={props.formState}
            uiState={props.uiState}
            // The overlay is not a real form element; synthesize the minimal
            // attributes the shared component / FormElementWrapper reads.
            formComponentAttributes={{
              jsonpath: 'form_customOption_overlay',
              id: 'form_customOption_overlay',
              editable: true,
              error: undefined,
              required: false,
            } as any}
            localErrorMsg=""
            showFilledIcon={false}
            isInputAllowed={true}
            value={value}
            editable={true}
            isNumber={false}
            isTextarea={false}
            numberOfLines={1}
            isFocused={isFocused}
            labeltext={labelText}
            onChangeText={setValue}
            onNumberChangeText={() => {}}
            onFocus={() => setIsFocused(true)}
            onBlur={() => setIsFocused(false)}
            elementRef={inputRef}
            styles={inputStyles}
            t={t}
          />
        </View>
      }
    />
  );
};

export default CustomOptionOverlay;
