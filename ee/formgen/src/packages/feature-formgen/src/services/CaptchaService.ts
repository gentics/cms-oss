export type CaptchaProvider = 'friendly' | 'recaptcha';

type ExecuteFn = () => Promise<string | null>;
type Listener = () => void;

let _provider: CaptchaProvider | null = null;
let _token: string | null = null;
let _hasError = false;
let _widgetRef: { current: any } | null = null;
let _execute: ExecuteFn | null = null;
const _listeners: Set<Listener> = new Set();

function notify(): void {
  _listeners.forEach((fn) => fn());
}

/**
 * Provider-agnostic captcha service. Bridges the captcha widget component
 * with RestService, submit hooks, and form-level validation.
 *
 * Activation is driven by the form-level configuration (`CaptchaConfig`):
 * the active widget calls `activate(provider)` on mount, `deactivate()` on
 * unmount. The token is verified by the backend on the first save/submit;
 * subsequent requests in the same form session are authorised via a session
 * cookie, so the same token can be sent repeatedly without reset.
 */
export const CaptchaService = {
  // ---------- Lifecycle ----------

  activate(provider: CaptchaProvider): void {
    _provider = provider;
  },

  deactivate(): void {
    _provider = null;
  },

  isActive(): boolean {
    return _provider !== null;
  },

  getProvider(): CaptchaProvider | null {
    return _provider;
  },

  // ---------- Token ----------

  /**
   * Stores the latest token, or null when the widget expires / errors.
   * Setting a non-null token auto-clears any pending error state so the UI
   * recovers as soon as the user solves the captcha.
   */
  setToken(token: string | null): void {
    _token = token;
    if (token != null && _hasError) {
      _hasError = false;
      notify();
    }
  },

  getToken(): string | null {
    return _token;
  },

  // ---------- Error / UI ----------

  hasError(): boolean {
    return _hasError;
  },

  setError(value: boolean): void {
    if (_hasError === value) return;
    _hasError = value;
    notify();
  },

  subscribe(fn: Listener): () => void {
    _listeners.add(fn);
    return () => {
      _listeners.delete(fn);
    };
  },

  // ---------- Scroll target ----------

  registerRef(ref: { current: any } | null): void {
    _widgetRef = ref;
  },

  getRef(): { current: any } | null {
    return _widgetRef;
  },

  unregisterRef(): void {
    _widgetRef = null;
  },

  // ---------- Invisible-mode execute bridge (reCAPTCHA-invisible only) ----------

  /**
   * Registers the function that programmatically triggers an invisible
   * challenge. The function resolves with the token (or null on failure).
   * Only used by reCAPTCHA-invisible; Friendly never registers an execute.
   */
  registerExecute(fn: ExecuteFn): void {
    _execute = fn;
  },

  unregisterExecute(): void {
    _execute = null;
  },

  hasExecute(): boolean {
    return _execute !== null;
  },

  /**
   * Runs the registered execute function. No-op if none registered. Caller
   * must afterwards check `getToken()` to verify success.
   */
  async execute(): Promise<void> {
    if (_execute) {
      const token = await _execute();
      if (token != null) {
        _token = token;
        if (_hasError) {
          _hasError = false;
          notify();
        }
      }
    }
  },

  // ---------- HTTP integration ----------

  /**
   * Returns the request headers needed by the backend to verify the token.
   * Empty object if no token is currently stored.
   */
  getRequestHeaders(): Record<string, string> {
    if (_token == null || _provider == null) return {};
    if (_provider === 'friendly') return { 'frc-captcha-response': _token };
    if (_provider === 'recaptcha') return { 'x-recaptcha-token': _token };
    return {};
  },

  // ---------- Form reset ----------

  /**
   * Fully resets the service. Call when the form is closed / reinitialised.
   */
  reset(): void {
    _provider = null;
    _token = null;
    _execute = null;
    _widgetRef = null;
    if (_hasError) {
      _hasError = false;
      notify();
    }
  },
};
