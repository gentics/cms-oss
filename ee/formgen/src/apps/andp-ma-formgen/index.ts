import { registerRootComponent } from 'expo';
import { createRoot } from 'react-dom/client';
import { createElement } from 'react';
import { Platform } from 'react-native';
import App from './App';
import { fontFamilies as commonFontFamilies } from '@andp/core/src/styles/common/font-config';
import { fontFamilies as customFontFamilies } from '@andp/core/src/styles/custom/font-config';
import Constants from 'expo-constants';
// registerRootComponent calls AppRegistry.registerComponent('main', () => App);
// It also ensures that whether you load the app in Expo Go or in a native build,
// the environment is set up appropriately
// to fix the warning => react 18

const isDevMode = Constants.manifest.extra.devMode;
const isMockForm = Constants.manifest.extra.mockForm;

if (Platform.OS === 'web') {
  const ensureFonts = () => {
    if (isDevMode || isMockForm) return;
    if (document.getElementById('formgen-fonts')) return;

    const style = document.createElement('style');
    style.id = 'formgen-fonts';
    const fontFamilies =
      customFontFamilies && Object.keys(customFontFamilies).length > 0
        ? customFontFamilies
        : commonFontFamilies;

    Object.entries(fontFamilies).forEach(([fontName, fontPath]) => {
      const fullName = (fontPath as string).split('/').pop() || '';
      const ext = fullName.split('.').pop();
      const filename = fullName.substring(0, fullName.lastIndexOf('.'));
      const fontFace = `@font-face {
        font-family: '${fontName}';
        src: url('/static/cpa/fonts/${filename}.${ext}') format('truetype');
        font-display: auto;
      }`;
      style.appendChild(document.createTextNode(fontFace));
    });
    document.head.appendChild(style);
  };

  const rootTag =
    document.getElementById('root') ?? document.getElementById('main');

  if (rootTag) {
    ensureFonts();
    createRoot(rootTag).render(createElement(App));
  }
} else {
  registerRootComponent(App);
}
