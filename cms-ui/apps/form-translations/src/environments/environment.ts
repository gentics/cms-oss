/**
 * Dev-Konfiguration. Mock-Modus standardmäßig aktiv, damit `npm start`
 * ohne CMS-Verbindung funktioniert. Für Tests gegen das echte CMS
 * `useMockData: false` setzen und `npm run start:proxy` verwenden.
 */
export const environment = {
  production: false,
  apiBaseUrl: '/rest',
  toolKey: 'form-translations',
  useMockData: true,
  mockSid: 'mock-session-137',
  logLevel: 'debug' as 'debug' | 'info' | 'warn' | 'error',
  toolApiHandshakeTimeoutMs: 10000,
  toolApiHandshakeRetries: 3,
  /** Künstliche Verzögerung für Mock-Antworten in ms, damit Loading-States sichtbar werden. */
  mockLatencyMs: 250
};
