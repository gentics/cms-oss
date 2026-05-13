import React from 'react';
import { View, Image, ImageStyle, StyleProp, ViewStyle } from 'react-native';

export enum LogoVariant {
  Primary = 'primary',
  Secondary = 'secondary',
  Black = 'black',
  White = 'white',
  MarkPrimary = 'mark-primary',
  MarkSecondary = 'mark-secondary',
  MarkBlack = 'mark-black',
  MarkWhite = 'mark-white',
}

const logoFiles: Record<LogoVariant, any> = {
  [LogoVariant.Primary]: require('../assets/gentics-logo-primaryblue.png'),
  [LogoVariant.Secondary]: require('../assets/gentics-logo-secondaryblue.png'),
  [LogoVariant.Black]: require('../assets/gentics-logo-black.png'),
  [LogoVariant.White]: require('../assets/gentics-logo-white.png'),
  [LogoVariant.MarkPrimary]: require('../assets/gentics-logomark-primaryblue.png'),
  [LogoVariant.MarkSecondary]: require('../assets/gentics-logomark-secondaryblue.png'),
  [LogoVariant.MarkBlack]: require('../assets/gentics-logomark-black.png'),
  [LogoVariant.MarkWhite]: require('../assets/gentics-logomark-white.png'),
};

interface LogoProps {
  type: LogoVariant;
  width?: number | string; // Erlaubt flexible Breite
  height?: number | string; // Erlaubt flexible Höhe
  style?: StyleProp<ViewStyle>; // Für Container-Anpassungen (Margin etc.)
  imageStyle?: StyleProp<ImageStyle>; // Falls man das Image direkt stylen will
  resizeMode?: 'cover' | 'contain' | 'stretch' | 'repeat' | 'center' | 'none';
}

const AndpLogo: React.FC<LogoProps> = ({
  type,
  width = 150, // Standardbreite
  height = 50, // Standardhöhe
  style,
  imageStyle,
  resizeMode = 'contain',
}) => {
  return (
    <View
      style={[{
        width: width,
        height: height,
        justifyContent: 'center',
        alignItems: 'center',
      }, style]}
    >
      <Image
        source={logoFiles[type]}
        style={[{ width: '100%', height: '100%' }, imageStyle]}
        resizeMode={resizeMode}
      />
    </View>
  );
};

export default AndpLogo;
