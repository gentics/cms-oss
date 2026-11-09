// ButtonProvider.tsx
import React, { createContext, useContext, useMemo } from "react";
import { IFormFlowButtonDefinition } from "../../types/interfaces";
import { Platform } from "react-native";
import i18n from "../../../../core/src/services/i18n";

interface ButtonContextValue {
  buttons: IFormFlowButtonDefinition[];
  getButtonTranslationKey: (key: string) => string;
}

interface ButtonProviderProps {
  definitions?: IFormFlowButtonDefinition[];
  children: React.ReactNode;
}

const ButtonContext = createContext<ButtonContextValue | undefined>(undefined);

export const ButtonProvider: React.FC<ButtonProviderProps> = ({
  definitions = [],
  children,
}) => {
  const getButtonTranslationKey = (key: string): string => {
    if (Platform.OS !== "web") {
      const mobileKey = `app_${key}`;
      if (i18n.exists(mobileKey)) {
        return mobileKey;
      }
    }
    return key;
  };

  const value = useMemo(
    () => ({
      buttons: definitions,
      getButtonTranslationKey,
    }),
    [definitions]
  );

  return <ButtonContext.Provider value={value}>{children}</ButtonContext.Provider>;
};

export const useButtons = () => {
  const ctx = useContext(ButtonContext);
  if (!ctx) throw new Error("useButtons must be used within a ButtonProvider");
  return ctx;
};