import React, { forwardRef, ReactNode, useEffect, useRef, useState } from 'react';
import {
  TextInput,
  View,
  Text,
  TextInputProps,
  StyleProp,
  ViewStyle,
  TextStyle,
  Platform,
} from 'react-native';
import theme, { customComponentsTheme } from '../styles/theme';
import AndpText from './AndpText';

interface AndpInputProps extends TextInputProps {
  icon?: ReactNode;
  rightElement?: ReactNode[];
  style?: StyleProp<ViewStyle>;
  inputStyle?: StyleProp<TextStyle>;
  flex?: boolean;
  leftElement?: ReactNode[];
  useCustomPlaceholderWrapper?: boolean;
  customPlaceholderWrapperStyle?: StyleProp<TextStyle>;
  disableHeightForDesktop?: boolean;
}

const AndpInputElement = forwardRef<TextInput, AndpInputProps>(({
  flex = false,
  style,
  inputStyle,
  icon,
  rightElement = [],
  leftElement = [],
  useCustomPlaceholderWrapper = false,
  customPlaceholderWrapperStyle,
  onFocus,
  onBlur,
  onChangeText,
  value,
  placeholder,
  placeholderTextColor,
  ...props
}, ref) => {
  const [isFocused, setIsFocused] = useState(false);
  const [internalValue, setInternalValue] = useState(
    typeof value === 'string' ? value : ''
  );

  useEffect(() => {
    if (typeof value === 'string') setInternalValue(value);
  }, [value]);

  const handleFocus = (e: any) => {
    setIsFocused(true);
    if (onFocus) onFocus(e);
  };

  const handleBlur = (e: any) => {
    setIsFocused(false);
    if (onBlur) onBlur(e);
  };

  const handleChangeText = (text: string) => {
    setInternalValue(text);
    if (onChangeText) onChangeText(text);
  };

  const currentValue = typeof value === 'string' ? value : internalValue;

  const defaultPlaceholderStyle: TextStyle = {
    position: 'absolute',
    left: 0,
    right: 0,
    top: 0,
    bottom: 0,
    color:
      placeholderTextColor ||
      customComponentsTheme.input.placeholderTextColor ||
      "black",
    fontSize: customComponentsTheme.input.fontSize,
    textAlignVertical: 'center',
    paddingVertical: 0,
    paddingHorizontal: 0,
    pointerEvents: 'none',
  };

  return (
    <View
      style={[
        {
          flexDirection: 'row',
          justifyContent: 'flex-start',
          padding: customComponentsTheme.input.padding,
          backgroundColor: customComponentsTheme.input.backgroundColor,
          borderRadius: customComponentsTheme.input.borderRadius,
          width: '100%',
          alignItems: 'center',
        },
        flex && { flex: 1, width: undefined },
        Platform.OS === 'web' && {
          paddingHorizontal: customComponentsTheme.input.horizontalPaddingForDesktop,
        },
        style
      ]}
    >
      {icon ?? icon}
      {leftElement ?? leftElement}
      <View style={[{ flex: 1, position: 'relative' },
      {
        ...(Platform.OS === 'web' && !props.disableHeightForDesktop && {
          height: '100%'
        })
      }
      ]}>
        <TextInput
          ref={ref}
          style={[
            {
              flex: 1,
              fontSize: customComponentsTheme.input.fontSize,
              color: customComponentsTheme.input.fontColor,
              paddingVertical: 0,
            },
            icon && { marginLeft:  customComponentsTheme.input.iconTextInputGap },
            {
              ...(Platform.OS === 'web' && !props.disableHeightForDesktop && {
                height: customComponentsTheme.input.heightForDesktop,
              })
            },
            {...(Platform.OS === 'web' && { outlineStyle: 'none' } as any)},
            inputStyle,
          ]}
          onFocus={handleFocus}
          onBlur={handleBlur}
          onChangeText={handleChangeText}
          value={currentValue}
          placeholder={useCustomPlaceholderWrapper ? '' : placeholder}
          placeholderTextColor={
            useCustomPlaceholderWrapper ? undefined : placeholderTextColor
          }
          {...props}
        />
        {useCustomPlaceholderWrapper &&
          // !currentValue &&
          placeholder ? (
          <AndpText
            numberOfLines={1}
            ellipsizeMode="tail"
            style={[defaultPlaceholderStyle, customPlaceholderWrapperStyle]}
          >
            {placeholder}
          </AndpText>
        ) : null}
      </View>
      {rightElement && 
        <View style={{gap: theme.space.extraSmall, flexDirection: 'row', height: '100%', alignItems: 'center'}}>
          {rightElement}
        </View>
      }
    </View>
  );
});

export default AndpInputElement;