type ExecuteRecaptchaFn = () => Promise<string>;

// Does Form contain a reCAPTCHA Element?
let _required = false;
let _execute: ExecuteRecaptchaFn | null = null;
let _token: string | null = null;

/**
 * Singleton service that bridges the CmsRecaptchaElement (component layer)
 * with RestService and hook utilities (service layer) — without polluting the
 * global object.
 */
export const RecaptchaService = {
  /**
   * Marks the current form session as requiring reCAPTCHA.
   */
  markRequired(): void {
    _required = true;
  },

  /**
   * Registers the function that programmatically triggers the invisible challenge.
   * Only needed for web + invisible mode.
   */
  registerExecute(fn: ExecuteRecaptchaFn): void {
    _execute = fn;
  },

  /**
   * Removes the execute bridge when the component unmounts (e.g. page navigation).
   * Does NOT clear the token — the verified token must survive page changes.
   */
  unregisterExecute(): void {
    _execute = null;
  },

  /** Stores the verified token, or null when the widget expires. */
  setToken(token: string | null): void {
    _token = token;
  },

  /** Returns the current verified token, or null if not yet verified / expired. */
  getToken(): string | null {
    return _token;
  },

  /**
   * True if this form session contains a reCAPTCHA field.
   * Used by RestService to decide whether to enforce the token on requests.
   */
  isRequired(): boolean {
    return _required;
  },

  /**
   * True when an invisible-mode execute function is currently registered.
   */
  hasExecute(): boolean {
    return _execute !== null;
  },

  /**
   * Triggers the invisible reCAPTCHA challenge and waits for verification.
   * No-op if no execute function is registered (checkbox / native modes).
   */
  async execute(): Promise<void> {
    if (_execute) {
      await _execute();
    }
  },

  /**
   * Fully resets the service. Call after a successful submit or when the form
   * is closed / reinitialised — not on page navigation within the same form.
   */
  reset(): void {
    _required = false;
    _execute = null;
    _token = null;
  },
};
