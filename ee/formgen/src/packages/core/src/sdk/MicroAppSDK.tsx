import React, { createContext, useContext, useMemo, PropsWithChildren } from 'react';
import { TFunction } from 'i18next';
import { Platform } from 'react-native';

export type SharedFn = (...args: any[]) => any;

export interface AndpNavigationApi { change(screen: string, params?: object): void; previous(): void; }
export interface IUserData { firstName: string; lastName: string; email: string; username: string; dateOfBirth?: string | Date; }
export interface AndpAuthApi { userData: IUserData | undefined; logout(setBiometricAuthToInactive?: boolean): void; }

export interface AndpTranslationApi {
  cultures: string[];
  selectedCulture: string;
  setCulture: (culture: string) => void;
  t: TFunction;
}

export interface AndpSharedFunctionsApi {
  register(key: string, fn: SharedFn): void;
  unregister(key: string): void;
  get(key: string): SharedFn | undefined;
  list(): string[];
}
export interface AndpSharedPropsApi {
  get(key: string): any;
  set(key: string, value: any): void;
  removeItem?: (key: string) => void;
  state?: Record<string, any>; // optional -> Web kann das weglassen
}

export interface AlertButton { text: string; onPress?: () => void; style?: 'default' | 'cancel' | 'destructive'; }
export interface AlertOptions { cancelable?: boolean; onDismiss?: () => void; }
export type AlertType = 'native' | 'custom';
export type AlertRenderFn = (p: AlertPayload & { close: (result?: any) => void }) => React.ReactNode;
export interface AlertPayload {
  title: string;
  message?: string;
  buttons?: AlertButton[];
  options?: AlertOptions;
  type?: AlertType;
  render?: AlertRenderFn;
}
export interface ConfirmOpts {
  title?: string;
  okText?: string;
  cancelText?: string;
  destructive?: boolean;
  options?: AlertOptions;
  type?: AlertType;
  render?: AlertRenderFn;
}
export interface AndpAlertApi {
  show(payload: AlertPayload): void;
  confirm(message: string, opts?: ConfirmOpts): Promise<boolean>;
}

// ---- Toast API ----
export type ToastPosition = 'top' | 'bottom';
export interface ToastShowPayload {
  type?: string;            // maps to config key (e.g., 'success' | 'error' | custom)
  text1?: string;
  text2?: string;
  position?: ToastPosition; // 'top' | 'bottom'
  autoHide?: boolean;
  visibilityTime?: number;  // ms
  onShow?: () => void;
  onHide?: () => void;
  onPress?: () => void;
  props?: any;              // extra props passed to custom renderer
  topOffset?: number;     // default 120 when position === 'top' (overridable)
  bottomOffset?: number;  // default 100 when position === 'bottom' (overridable)
}
export interface AndpToastApi {
  show(payload: ToastShowPayload): void;
  hide(): void;
  register(type: string, render: (props: any) => React.ReactNode): void;    // register custom toast type
  unregister(type: string): void;                                           // remove custom toast type
}

// ---- Error API ----
export type ErrorType = string | number;
export type ErrorRenderFn = (p: ErrorPayload & { dismiss: () => void }) => React.ReactNode;
export interface ErrorPayload {
  type: ErrorType;
  title?: string;
  message?: string;
  data?: any;
  render?: ErrorRenderFn;
  blocking?: boolean;
}
export interface AndpErrorApi {
  show(payload: ErrorPayload): void;
  dismiss(): void;
  register(type: ErrorType, render: ErrorRenderFn): void;
}

export interface MicroAppSDKValue {
  andpNavigation: AndpNavigationApi;
  andpAuth: AndpAuthApi;
  andpTranslation: AndpTranslationApi;
  andpSharedFunctions: AndpSharedFunctionsApi;
  andpSharedProps: AndpSharedPropsApi;
  andpAlert: AndpAlertApi;
  andpToast: AndpToastApi;
  andpError: AndpErrorApi;
  config: any;
  devMode: boolean;
  refreshNotifications?: () => Promise<void> | void;
}

const Ctx = createContext<MicroAppSDKValue | null>(null);

export interface MicroAppSDKProviderProps extends PropsWithChildren {
  andpNavigation: AndpNavigationApi;
  andpAuth: AndpAuthApi;
  andpTranslation: AndpTranslationApi;
  andpSharedFunctions: AndpSharedFunctionsApi;
  andpSharedProps: AndpSharedPropsApi;
  andpAlert: AndpAlertApi;
  andpToast: AndpToastApi;
  andpError: AndpErrorApi;
  config: any;
  devMode: boolean;
  refreshNotifications?: () => Promise<void> | void;
}

export const MicroAppSDKProvider: React.FC<MicroAppSDKProviderProps> = ({
  children, andpNavigation, andpAuth, andpTranslation, andpSharedFunctions, andpSharedProps, andpAlert, andpToast, andpError,
  config, devMode, refreshNotifications,
}) => {
  const value = useMemo(() => ({
    andpNavigation,
    andpAuth,
    andpTranslation,
    andpSharedFunctions,
    andpSharedProps,
    andpAlert,
    andpToast,
    andpError,
    config,
    devMode,
    refreshNotifications,
  }), [andpNavigation, andpAuth, andpTranslation, andpSharedFunctions, andpSharedProps, andpAlert, andpToast, andpError, config, devMode, refreshNotifications]);

  return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
};

export const useMicroAppSDK = () => {
  const v = useContext(Ctx);
  if (!v) {
    if (Platform.OS === 'web') return {} as MicroAppSDKValue;
    throw new Error('useMicroAppSDK must be used within a MicroAppSDKProvider');
  }
  return v;
};

export const useTranslationApi = () => useMicroAppSDK().andpTranslation;
export const useAlertApi = () => useMicroAppSDK().andpAlert;
export const useErrorApi = () => useMicroAppSDK().andpError;
export const useToastApi = () => useMicroAppSDK().andpToast;