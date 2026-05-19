import { Platform } from "react-native";

const FORMGRID_ELEMENT_SELECTED_EVENT = 'andp:formgen/elementSelected' as const;

export type FormgridElementSelectedDetail = {
  elementId: string;
  jsonPath: string;
  source: 'pointer';
};

export default function emitSelected(elementId: string, jsonPath: string) {
  if (Platform.OS !== 'web') return;
  if (typeof window === 'undefined') return;

  const detail: FormgridElementSelectedDetail = {
    elementId,
    jsonPath,
    source: 'pointer',
  };

  window.dispatchEvent(
    new CustomEvent<FormgridElementSelectedDetail>(
      FORMGRID_ELEMENT_SELECTED_EVENT,
      { detail },
    ),
  );
}