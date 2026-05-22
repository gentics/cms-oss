import { Injectable } from '@angular/core';
import {
  HttpEvent,
  HttpHandler,
  HttpInterceptor,
  HttpRequest,
  HttpResponse
} from '@angular/common/http';
import { Observable, delay, of } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  FormTypesResponseDto,
  LanguagesResponseDto,
  TranslationsPayloadDto,
  TranslationsResponseDto
} from '../../models/dto.model';

/**
 * Fängt alle `/rest/...`-Requests im Mock-Modus ab und gibt realistische
 * Testdaten zurück. Wird nur in `core.module.ts` registriert, wenn
 * `environment.useMockData === true`.
 */
@Injectable()
export class MockApiInterceptor implements HttpInterceptor {
  private state = {
    global: { ...DEMO_TRANSLATIONS_GLOBAL } as TranslationsPayloadDto,
    types: {
      andp:    { ...DEMO_TRANSLATIONS_ANDP }    as TranslationsPayloadDto,
      contact: { ...DEMO_TRANSLATIONS_CONTACT } as TranslationsPayloadDto,
      survey:  {}                               as TranslationsPayloadDto
    } as Record<string, TranslationsPayloadDto>
  };

  intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    const url = req.url;
    const method = req.method.toUpperCase();

    /* GET /rest/form/translations/languages */
    if (method === 'GET' && url.includes('/rest/form/translations/languages')) {
      const body: LanguagesResponseDto = {
        responseInfo: { responseCode: 'OK' },
        languages: DEMO_LANGUAGES
      };
      return this.respond(body);
    }

    /* GET /rest/form/types  — Endpoint im echten CMS noch zu verifizieren */
    if (method === 'GET' && /\/rest\/form\/types\/?(\?|$)/.test(url)) {
      const body: FormTypesResponseDto = {
        responseInfo: { responseCode: 'OK' },
        types: DEMO_FORM_TYPES
      };
      return this.respond(body);
    }

    /* GET /rest/form/types/{type}/translations */
    const typeGet = method === 'GET' && url.match(/\/rest\/form\/types\/([^/?]+)\/translations/);
    if (typeGet) {
      const typeKey = typeGet[1];
      const body: TranslationsResponseDto = {
        responseInfo: { responseCode: 'OK' },
        translations: this.state.types[typeKey] ?? {}
      };
      return this.respond(body);
    }

    /* POST /rest/form/types/{type}/translations */
    const typePost = method === 'POST' && url.match(/\/rest\/form\/types\/([^/?]+)\/translations/);
    if (typePost) {
      const typeKey = typePost[1];
      const changes = (req.body ?? {}) as TranslationsPayloadDto;
      this.state.types[typeKey] = mergeTranslations(this.state.types[typeKey] ?? {}, changes);
      const body: TranslationsResponseDto = {
        responseInfo: { responseCode: 'OK' },
        translations: this.state.types[typeKey]
      };
      return this.respond(body);
    }

    /* GET /rest/form/translations */
    if (method === 'GET' && /\/rest\/form\/translations\/?(\?|$)/.test(url)) {
      const body: TranslationsResponseDto = {
        responseInfo: { responseCode: 'OK' },
        translations: this.state.global
      };
      return this.respond(body);
    }

    /* POST /rest/form/translations */
    if (method === 'POST' && /\/rest\/form\/translations\/?(\?|$)/.test(url)) {
      const changes = (req.body ?? {}) as TranslationsPayloadDto;
      this.state.global = mergeTranslations(this.state.global, changes);
      const body: TranslationsResponseDto = {
        responseInfo: { responseCode: 'OK' },
        translations: this.state.global
      };
      return this.respond(body);
    }

    /* Unbekannter Endpoint → durchreichen (für UI-i18n-Assets etc.) */
    return next.handle(req);
  }

  private respond<T>(body: T): Observable<HttpEvent<T>> {
    return of(new HttpResponse<T>({ status: 200, body })).pipe(
      delay(environment.mockLatencyMs)
    );
  }
}

/**
 * Merged Änderungen in den bestehenden Translation-Bestand:
 *   - vorhandene Keys werden flach gemerged (Sprache für Sprache)
 *   - leere Strings werden mit gespeichert (User hat die Übersetzung gelöscht)
 */
function mergeTranslations(
  current: TranslationsPayloadDto,
  changes: TranslationsPayloadDto
): TranslationsPayloadDto {
  const result: TranslationsPayloadDto = { ...current };
  for (const [key, langs] of Object.entries(changes)) {
    result[key] = { ...(result[key] ?? {}), ...langs };
  }
  return result;
}

/* =====================================================================
   Demo-Daten — werden im echten Tool durch CMS-Antworten ersetzt.
   ===================================================================== */

const DEMO_LANGUAGES = [
  { code: 'de', name: 'Deutsch' },
  { code: 'en', name: 'English' },
  { code: 'fr', name: 'Français' },
  { code: 'it', name: 'Italiano' },
  { code: 'es', name: 'Español' }
];

const DEMO_FORM_TYPES = [
  { key: 'andp',    name: 'Acta Nova Dialog Portal', description: 'Acta Nova Dialog Portal' },
  { key: 'contact', name: 'wkBlue',                  description: 'wkBlue Kontaktformular' },
  { key: 'survey',  name: 'Marktplatz',              description: 'Marktplatz' }
];

const DEMO_TRANSLATIONS_GLOBAL: TranslationsPayloadDto = {
  form_start_button:        { de: 'Service starten',           en: 'Start service',     fr: 'Démarrer le service', it: 'Avvia il servizio',  es: 'Iniciar el servicio' },
  form_submit_button:       { de: 'Absenden',                  en: 'Submit',            fr: 'Envoyer',             it: 'Invia',              es: 'Enviar' },
  form_cancel_button:       { de: 'Abbrechen',                 en: 'Cancel',            fr: 'Annuler',             it: 'Annulla',            es: 'Cancelar' },
  form_reset_button:        { de: 'Zurücksetzen',              en: 'Reset',             fr: 'Réinitialiser',       it: 'Reimposta',          es: 'Restablecer' },
  form_next_button:         { de: 'Weiter',                    en: 'Next',              fr: 'Suivant',             it: 'Avanti',             es: 'Siguiente' },
  form_back_button:         { de: 'Zurück',                    en: 'Back',              fr: 'Retour',              it: 'Indietro',           es: 'Atrás' },
  form_loading:             { de: 'Laden …',                   en: 'Loading…',          fr: 'Chargement…',         it: 'Caricamento…',       es: 'Cargando…' },
  form_saving:              { de: 'Speichern …',               en: 'Saving…',           fr: 'Enregistrement…',     it: 'Salvataggio…',       es: 'Guardando…' },
  form_saved:               { de: 'Gespeichert',               en: 'Saved',             fr: 'Enregistré',          it: 'Salvato',            es: 'Guardado' },
  form_error_required:      { de: 'Pflichtfeld',               en: 'Required field',    fr: 'Champ requis',        it: 'Campo obbligatorio', es: 'Campo obligatorio' },
  form_error_email:         { de: 'Ungültige E-Mail',          en: 'Invalid email',     fr: 'E-mail non valide',   it: 'Email non valida',   es: 'Correo no válido' },
  form_error_phone:         { de: 'Ungültige Nummer',          en: 'Invalid number',    fr: 'Numéro non valide',   it: 'Numero non valido',  es: 'Número no válido' },
  form_error_minlength:     { de: 'Zu kurz',                   en: 'Too short',         fr: 'Trop court',          it: 'Troppo corto',       es: 'Demasiado corto' },
  form_error_maxlength:     { de: 'Zu lang',                   en: 'Too long',          fr: 'Trop long',           it: 'Troppo lungo',       es: 'Demasiado largo' },
  form_error_generic:       { de: 'Ein Fehler ist aufgetreten', en: 'An error occurred', fr: '',                   it: '',                   es: '' },
  form_field_label_name:    { de: 'Name',                      en: 'Name',              fr: 'Nom',                 it: 'Nome',               es: 'Nombre' },
  form_field_label_email:   { de: 'E-Mail',                    en: 'Email',             fr: 'E-mail',              it: 'Email',              es: 'Correo' },
  form_field_label_phone:   { de: 'Telefon',                   en: 'Phone',             fr: 'Téléphone',           it: 'Telefono',           es: 'Teléfono' },
  form_field_label_company: { de: 'Firma',                     en: 'Company',           fr: 'Société',             it: '',                   es: '' },
  form_field_label_message: { de: 'Nachricht',                 en: 'Message',           fr: 'Message',             it: 'Messaggio',          es: 'Mensaje' },
  form_success_title:       { de: 'Vielen Dank!',              en: 'Thank you!',        fr: 'Merci !',             it: 'Grazie!',            es: '¡Gracias!' },
  form_success_message:     { de: 'Ihre Eingabe wurde übermittelt.', en: 'Your submission has been received.', fr: 'Votre envoi a bien été reçu.', it: '', es: '' },
  form_terms_accept:        { de: 'Ich akzeptiere die AGB',    en: 'I accept the terms', fr: 'J’accepte les CGV',  it: '',                   es: '' },
  form_privacy_accept:      { de: 'Ich akzeptiere die Datenschutzerklärung', en: 'I accept the privacy policy', fr: '', it: '', es: '' },
  form_newsletter_optin:    { de: 'Newsletter abonnieren',     en: 'Subscribe to newsletter', fr: '',              it: '',                   es: '' },
  form_upload_label:        { de: 'Datei hochladen',           en: 'Upload file',       fr: 'Téléverser un fichier', it: 'Carica file',     es: 'Subir archivo' },
  form_upload_drop:         { de: 'Datei hier ablegen',        en: 'Drop file here',    fr: 'Déposez le fichier ici', it: '',                es: '' },
  form_upload_browse:       { de: 'Durchsuchen',               en: 'Browse',            fr: 'Parcourir',           it: 'Sfoglia',            es: 'Examinar' },
  form_search_placeholder:  { de: 'Suchen …',                  en: 'Search…',           fr: 'Rechercher…',         it: 'Cerca…',             es: 'Buscar…' },
  form_no_results:          { de: 'Keine Ergebnisse',          en: 'No results',        fr: 'Aucun résultat',      it: 'Nessun risultato',   es: 'Sin resultados' }
};

const DEMO_TRANSLATIONS_ANDP: TranslationsPayloadDto = {
  form_start_button:    { de: 'Dialog starten',     en: 'Start dialog',         fr: 'Démarrer le dialogue', it: 'Avvia il dialogo',    es: 'Iniciar el diálogo' },
  form_submit_button:   { de: 'Antrag senden',      en: 'Submit application',   fr: 'Envoyer la demande',   it: 'Invia richiesta',     es: 'Enviar solicitud' },
  form_success_title:   { de: 'Antrag erhalten',    en: 'Application received', fr: 'Demande reçue',        it: '',                    es: '' },
  form_success_message: { de: 'Ihr Antrag wurde im Acta Nova Dialog Portal registriert.', en: 'Your application has been registered in the Acta Nova Dialog Portal.', fr: '', it: '', es: '' }
};

const DEMO_TRANSLATIONS_CONTACT: TranslationsPayloadDto = {
  form_submit_button: { de: 'Anfrage senden',   en: 'Send request',     fr: 'Envoyer la demande', it: 'Invia richiesta', es: 'Enviar consulta' },
  form_success_title: { de: 'Anfrage erhalten', en: 'Request received', fr: '',                   it: '',                es: '' }
};
