type UILanguage = 'en' | 'de';

export interface AppSettings {
    sid: string;
    language: UILanguage;
    displayFields: string[];
}
