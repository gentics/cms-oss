import { AccessibilityState, ActivityIndicator, Platform, TouchableOpacity, TouchableOpacityProps, View } from "react-native"
import { useState } from "react";
import AndpText from "./AndpText";
import { buttonsStyle } from "../styles/common/common-style";

interface IAndpButton  extends TouchableOpacityProps {
  index?: any;
  title?: any;
  onPress?: () => void | Promise<void>;
  onFocus?: () => void | Promise<void>;
  onBlur?: () => void | Promise<void>;
  type?: 'primary' | 'secondary';
  loading?: boolean;
  wrapperStyle?: any;
  buttonStyle?: any;
  textStyle?: any;
  disabled?: boolean;
  accessibilityHint?: string;
  accessibilityState?: AccessibilityState;
  accessibilityLiveRegion?: "none" | "polite" | "assertive";
}

const AndpButton: React.FC<IAndpButton> = ({
  title,
  index,
  type = 'primary',
  loading = false,
  wrapperStyle,
  buttonStyle,
  textStyle,
  ...touchableProps
}) => {
  const [hover, setHover] = useState<boolean>(false);

  const mergedButtonStyle = type === 'primary' 
    ? buttonsStyle.btnPrimary 
    : buttonsStyle.btnSecondary;

  const buttonHoverStyle = type === 'primary' 
    ? buttonsStyle.btnPrimaryHover 
    : buttonsStyle.btnSecondaryHover;

  const mergedTextStyle = type === 'primary' 
    ? buttonsStyle.btnPrimaryText 
    : buttonsStyle.btnSecondaryText;

  const textHoverStyle = type === 'primary' 
    ? buttonsStyle.btnPrimaryTextHover 
    : buttonsStyle.btnSecondaryTextHover;

  

  return (
    <View key={index} style={[buttonsStyle.btnWrapper, wrapperStyle]}>   
      <TouchableOpacity
        activeOpacity={0.7}
        style={[
          buttonsStyle.btn,
          mergedButtonStyle,
          buttonStyle,
          hover && buttonHoverStyle,
        ]}
        {...(Platform.OS === "web" && {
          onMouseEnter: () => setHover(true),
          onMouseLeave: () => setHover(false),
        })}
        accessible
        accessibilityRole="button"
        accessibilityLabel={title}
        importantForAccessibility="yes"
        {...touchableProps}
      >
        <AndpText
          selectable={false}
          style={[
            buttonsStyle.btnText,
            mergedTextStyle,
            textStyle,
            hover && textHoverStyle,
          ]}
        >
          {loading ? 
            <ActivityIndicator color={mergedTextStyle.color}></ActivityIndicator>: 
            title
          }
        </AndpText>
      </TouchableOpacity>
    </View>
  );
}

export default AndpButton;