import React, { useState, forwardRef, useMemo } from 'react';
import {
  View,
  Pressable,
  ViewStyle,
  TextStyle,
  AccessibilityRole,
} from 'react-native';
import AndpText from './AndpText';
import theme from '../styles/theme';
import AndpIcon from './AndpIcon';
import { booleanElementStyles } from '../../../feature-formgen/src/styles/common/common-style';
import { booleanElementStyles as customBooleanElementStyles } from '../../../feature-formgen/src/styles/custom/custom-style';
import { mergeStyles } from '../AndpUtils';

type CustomStyles = {
  container?: ViewStyle; // Wrapper Pressable
  iconContainer?: ViewStyle; // Circle / checkbox wrapper
  iconFocused?: ViewStyle; // Focused state on icon
  iconError?: ViewStyle; // Error state on icon
  label?: TextStyle; // Label text
  labelFocused?: TextStyle; // Focused state on label
  labelError?: TextStyle; // Error state on label
};

export type AndpRadioButtonProps = {
  id?: string;
  selected: boolean;
  label: string;
  onPress: () => void;
  disabled?: boolean;
  error?: boolean;
  type?: 'radio' | 'checkbox';
  style?: CustomStyles;
  accessibilityLabel?: string;
  accessibilityHint?: string;
};

const AndpRadioButton = forwardRef<View, AndpRadioButtonProps>(
  (
    {
      id,
      selected,
      label,
      onPress,
      disabled = false,
      error = false,
      type = 'radio',
      style = {},
      accessibilityLabel,
      accessibilityHint,
    },
    ref,
  ) => {
    const [focused, setFocused] = useState(false);
    const role: AccessibilityRole = type === 'radio' ? 'radio' : 'checkbox';
    const styles = useMemo(() => mergeStyles(booleanElementStyles, customBooleanElementStyles), []);

    // icon container styles (circle or checkbox)
    let iconStyles = [] as ViewStyle[];
    let labelStyles = [] as TextStyle[];

    if (type === 'radio') {
      iconStyles = [
        styles.radioButtonElement,
        selected && styles.selectedRadioButton,
        error && [styles.errorRadioButton, style.iconError],
        focused && [styles.focusedRadioButton, style.iconFocused],
        style.iconContainer,
      ] as ViewStyle[];
      labelStyles = [
        styles.radioButtonLabel,
        selected && styles.selectedRadioButtonLabel,
        focused && [styles.focusedRadioButtonLabel, style.labelFocused],
        error && [styles.errorRadioButtonLabel, style.labelError],
        style.label,
      ] as TextStyle[];
    } else {
      iconStyles = [
        styles.checkboxElement,
        selected && styles.selectedCheckbox,
        error && [styles.errorCheckbox, style.iconError],
        error && selected && styles.selectedErrorCheckbox,
        disabled && styles.disabledCheckbox,
        disabled && selected && styles.selectedDisabledCheckbox,
        focused && [styles.focusedCheckbox, style.iconFocused],
        style.iconContainer,
      ] as ViewStyle[];
      labelStyles = [
        styles.checkboxLabel,
        selected && styles.selectedCheckboxLabel,
        error && [styles.errorCheckboxLabel, style.labelError],
        focused && [styles.focusedCheckboxLabel, style.labelFocused],
        disabled && styles.disabledCheckboxLabel,
        disabled && selected && styles.selectedDisabledCheckboxLabel,
        style.label,
      ] as TextStyle[];
    }
    return (
      <Pressable
        id={id}
        ref={ref}
        onPress={onPress}
        disabled={disabled}
        accessibilityRole={role}
        accessibilityLabel={accessibilityLabel ?? label}
        accessibilityHint={accessibilityHint}
        accessibilityState={{
          disabled,
          selected: role === 'radio' ? selected : undefined,
          checked: role === 'checkbox' ? selected : undefined,
        }}
        onFocus={() => setFocused(true)} // funktioniert nicht auf native -> onPressIn={() => setFocused(true)}
        onBlur={() => setFocused(false)} // funktioniert nicht auf native -> onPressOut={() => setFocused(false)}
        style={[
          styles.booleanElementContainer,
          disabled && styles.disabledBooleanElementContainer,
          style.container,
        ]}
      >
        <View style={iconStyles}>
          {type === 'radio'
            ? selected && <View style={[styles.radioButtonDot,
              error && styles.errorRadioButtonDot]}/>
            : selected && 
                <AndpIcon
                  name="checkMark"
                  style={[styles.checkboxIcon, 
                    disabled && styles.disabledCheckboxIcon,
                    error && styles.errorCheckboxIcon
                  ]}
                />
              }
        </View>

        <AndpText style={labelStyles}>{label}</AndpText>
      </Pressable>
    );
  },
);

export default AndpRadioButton;
