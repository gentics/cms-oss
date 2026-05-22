export const environment = {
  production: true,
  apiBaseUrl: '/rest',
  toolKey: 'form-translations',
  useMockData: false,
  mockSid: '',
  logLevel: 'warn' as 'debug' | 'info' | 'warn' | 'error',
  toolApiHandshakeTimeoutMs: 10000,
  toolApiHandshakeRetries: 3,
  mockLatencyMs: 0
};
