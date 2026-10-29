import React, { useState } from 'react';
import { View, TouchableOpacity, Text, StyleSheet, ViewStyle, TextStyle, Platform } from 'react-native';
import theme from '../styles/theme';
import AndpText from './AndpText';

export interface AndpRadioButtonOption {
  value: string;
  label: string;
}

export interface AndpRadioButtonGroupProps {
  options: AndpRadioButtonOption[];
  selectedValue: string;
  onValueChange: (value: string) => void;
  containerStyle?: ViewStyle;
  optionContainerStyle?: ViewStyle;
  radioButtonStyle?: ViewStyle;
  selectedRadioButtonStyle?: ViewStyle;
  selectedIndicatorStyle?: ViewStyle;
  labelStyle?: TextStyle;
}

const AndpRadioButtonGroup: React.FC<AndpRadioButtonGroupProps> = ({
  options,
  selectedValue,
  onValueChange,
  containerStyle,
  optionContainerStyle,
  radioButtonStyle,
  selectedRadioButtonStyle,
  labelStyle,
}) => {
  const [selected, setSelected] = useState<string>(selectedValue);
  const [hover, setHover]  = useState<string | null>(null);

  return (
    <View style={[styles.container, containerStyle]}>
      {options.map((option) => {
        const isSelected = selected === option.value;
        const isHover = hover === option.value;
        return (
          <TouchableOpacity
            key={option.value}
            style={[styles.radioButton, optionContainerStyle]}
            onPress={() => {
              setSelected(option.value)
              onValueChange(option.value)
            }}
            {...Platform.OS === 'web' &&
              {
                onMouseEnter: () => setHover(option.value),
                onMouseLeave: () => setHover(null),
              }
            }
          >
            <View
              style={[
                styles.radioButtonIndicator,
                radioButtonStyle,
                isHover && styles.onHoverRadioButton,
                isSelected && styles.selectedRadioButton,
                isSelected && selectedRadioButtonStyle
              ]}
            >
            </View>
            <AndpText style={labelStyle}>{option.label}</AndpText>
          </TouchableOpacity>
        );
      })}
    </View>
  );
};

const styles = StyleSheet.create({
   container: {
    width: '100%',
    flexDirection: Platform.OS === 'web' ? 'row' : 'column',
    flexWrap: 'wrap',
    gap: theme.space.regular,
    alignItems:  Platform.OS === 'web' ? 'center' : 'flex-start',
  },
  radioButton: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: theme.space.extraSmall,
    marginLeft: theme.space.small,
  },
  radioButtonIndicator: {
    width: 20,
    height: 20,
    borderRadius: 99,
    borderColor: theme.border.color.darkGray,
    borderWidth: theme.border.width.regular,
    marginTop: 1,
    ...Platform.select({
      web: {
        transition: 'border-color 0.1s ease, border-width 0.1s ease-in-out',
      },
    }),
  },
  selectedRadioButton: {
    borderWidth: 5,
    borderColor: theme.colors.colorAccent,
    opacity: 1,
  },
  onHoverRadioButton: {
    borderWidth: 5,
    borderColor: theme.colors.colorAccent,
    opacity: 0.3,
    ...Platform.select({
      web: {
        transition: 'border-color 0.1s ease, border-width 0.1s ease',
      },
    }),
  }
});

export default AndpRadioButtonGroup;