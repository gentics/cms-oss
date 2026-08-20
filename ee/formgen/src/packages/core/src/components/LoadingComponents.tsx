import React, { useMemo } from 'react';
import {
  View,
  StyleSheet,
  ActivityIndicator,
  Platform,
  BackHandler,
} from 'react-native';
import theme from '../styles/theme';
import AndpText from './AndpText';
import { loadingComponentsStyles } from '../styles/common/common-style';
import { loadingComponentsStyles as customLoadingComponentsStyles } from '../styles/custom/custom-style';
import { mergeStyles } from '../AndpUtils';
import { loaderTheme } from '../styles/theme';

interface SpinnerProps {
  size?: number;
  color?: string;
}

const Spinner: React.FC<SpinnerProps> = ({
  size = 60,
  color = theme.colors.colorAccent,
}) => {
  return <ActivityIndicator size={size} color={color}></ActivityIndicator>;
};

/* -------------------------------------------------------------------------------------- */
/*                        Komponenten zur allgemeinen Verwendung                         */
/* -------------------------------------------------------------------------------------- */
interface LoadingProps {
  text?: string;
  color?: string;
  size?: 'small' | 'large' | number;
  containerStyle?: object;
  textStyle?: object;
}

export const Loading: React.FC<LoadingProps> = ({
  text,
  color = loaderTheme.spinnerColor,
  size = loaderTheme.spinnerSize,
  containerStyle,
  textStyle,
}) => {
  const styles = useMemo(
    () => mergeStyles(loadingComponentsStyles, customLoadingComponentsStyles),
    [],
  );
  return (
    <View style={[styles.loaderContainer, containerStyle]}>
      <ActivityIndicator size={size} color={color} />
      {text && (
        <AndpText style={[styles.loaderText, textStyle]}>{text}</AndpText>
      )}
    </View>
  );
};

interface LoadingScreenProps {
  text?: string;
  color?: string;
  backgroundColor?: string;
}

export const LoadingScreen: React.FC<LoadingScreenProps> = ({
  text,
  color = loaderTheme.spinnerColor,
  backgroundColor = theme.colors.backgroundPrimary,
}) => {
  const styles = useMemo(
    () => mergeStyles(loadingComponentsStyles, customLoadingComponentsStyles),
    [],
  );
  return (
    <View
      style={[styles.fullScreenContainer, { backgroundColor: backgroundColor }]}
    >
      <ActivityIndicator size={loaderTheme.spinnerSize} color={color} />
      {text && <AndpText style={styles.loaderText}>{text}</AndpText>}
    </View>
  );
};

export default Spinner;
