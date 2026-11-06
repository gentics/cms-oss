import * as ButtonComponentsDef from './ButtonComponentDefinitions';

export const ButtonComponents: { [key: string]: React.ComponentType<any> } = {
  NextPageButton: ButtonComponentsDef.NextPageButton,
  PreviousPageButton: ButtonComponentsDef.PreviousPageButton,
  CancelButton: ButtonComponentsDef.CancelButton,
  PrintPdfButton: ButtonComponentsDef.PrintPdfButton,
  DetailInfoButton: ButtonComponentsDef.DetailInfoButton,
  SubmitButton: ButtonComponentsDef.SubmitButton,
};
