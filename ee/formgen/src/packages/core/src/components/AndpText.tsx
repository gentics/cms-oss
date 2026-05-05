import { ReactNode, forwardRef } from 'react';
import { Platform, Text, TextProps } from 'react-native';
import theme from '../styles/theme';
import Reanimated from 'react-native-reanimated';

type AndpTextProps = TextProps & {
  size?: number;
  weight?:
    | 'normal'
    | 'bold'
    | '100'
    | '200'
    | '300'
    | '400'
    | '500'
    | '600'
    | '700'
    | '800'
    | '900';
  color?: string;
  children?: ReactNode;
};

const AndpText = forwardRef<Text, AndpTextProps>(({
  children,
  style,
  size = theme.fontSize.regular,
  weight = theme.fontWeight.regular,
  color = theme.colors.textPrimary,
  ...props
}, ref) => {
  return (
    <Reanimated.Text
      ref={ref}
      style={[
        {
          fontFamily: theme.fontFamily.regular,
          userSelect: Platform.OS === 'web' ? 'text' : 'none',
          fontSize: size,
          fontWeight: weight,
          color: color,
        },
        style,
      ]}
      {...props}
    >
      {children}
    </Reanimated.Text>
  );
});

export default AndpText;
