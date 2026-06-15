import { HttpEvent, HttpHandler, HttpHeaders, HttpInterceptor, HttpRequest, HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, delay, of } from 'rxjs';

import { environment } from '../../../environments/environment';

/**
 * Dev-only HTTP interceptor that stubs the five form-translation endpoints
 * (`/rest/form/translations(/languages)?`, `/rest/form/types(/{type}/translations)?`)
 * so the UI can be exercised before the backend lands them.
 *
 * Registered conditionally in `app.module.ts` — only when
 * `environment.production === false`. Production builds skip it entirely
 * and the real `GCMSRestClientService` calls go through.
 *
 * Bypasses every other URL untouched — auth, other endpoints, assets.
 *
 * To toggle off without uninstalling: set `environment.useDevMock = false`
 * (see `environment.ts`).
 */
@Injectable()
export class DevMockInterceptor implements HttpInterceptor {

    /** In-memory state — survives multiple POSTs within one page load. */
    private readonly globalTranslations: Record<string, Record<string, string>> = { ...DEMO_GLOBAL };
    private readonly typeTranslations: Record<string, Record<string, Record<string, string>>> = {
        andp: { ...DEMO_ANDP },
        contact: { ...DEMO_CONTACT },
        survey: {},
    };

    intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
        const url = req.url;
        const method = req.method.toUpperCase();

        /* The GCMSRestClient adds ?sid=… to every request, we ignore that. */

        if (method === 'GET' && /\/rest\/form\/translations\/languages\b/.test(url)) {
            return this.respond({
                responseInfo: { responseCode: 'OK' },
                languages: DEMO_LANGUAGES,
            });
        }

        const typeGet = method === 'GET' && url.match(/\/rest\/form\/types\/([^/?]+)\/translations\b/);
        if (typeGet) {
            return this.respond({
                responseInfo: { responseCode: 'OK' },
                translations: this.typeTranslations[typeGet[1]] ?? {},
            });
        }

        const typePost = method === 'POST' && url.match(/\/rest\/form\/types\/([^/?]+)\/translations\b/);
        if (typePost) {
            const key = typePost[1];
            this.typeTranslations[key] = mergeTranslations(
                this.typeTranslations[key] ?? {},
                (req.body ?? {}) as Record<string, Record<string, string>>,
            );
            return this.respond({
                responseInfo: { responseCode: 'OK' },
                translations: this.typeTranslations[key],
            });
        }

        if (method === 'GET' && /\/rest\/form\/types(\?|$)/.test(url)) {
            return this.respond({
                responseInfo: { responseCode: 'OK' },
                items: DEMO_FORM_TYPES,
                numItems: DEMO_FORM_TYPES.length,
                hasMoreItems: false,
            });
        }

        if (method === 'GET' && /\/rest\/form\/translations(\?|$)/.test(url)) {
            return this.respond({
                responseInfo: { responseCode: 'OK' },
                translations: this.globalTranslations,
            });
        }

        if (method === 'POST' && /\/rest\/form\/translations(\?|$)/.test(url)) {
            const merged = mergeTranslations(
                this.globalTranslations,
                (req.body ?? {}) as Record<string, Record<string, string>>,
            );
            Object.keys(this.globalTranslations).forEach(k => delete this.globalTranslations[k]);
            Object.assign(this.globalTranslations, merged);
            return this.respond({
                responseInfo: { responseCode: 'OK' },
                translations: this.globalTranslations,
            });
        }

        return next.handle(req);
    }

    /**
     * `GCMSRestClientService` configures HttpClient with `responseType: 'text'`
     * and calls `JSON.parse` on the body itself, so the body has to be a JSON
     * STRING, not the parsed object.
     */
    private respond<T>(body: T): Observable<HttpEvent<string>> {
        return of(new HttpResponse<string>({
            status: 200,
            body: JSON.stringify(body),
            url: '',
            headers: new HttpHeaders({ 'Content-Type': 'application/json' }),
        })).pipe(
            delay(environment.devMockLatencyMs ?? 200),
        );
    }
}

function mergeTranslations(
    current: Record<string, Record<string, string>>,
    changes: Record<string, Record<string, string>>,
): Record<string, Record<string, string>> {
    const result: Record<string, Record<string, string>> = { ...current };
    for (const [key, langs] of Object.entries(changes)) {
        result[key] = { ...(result[key] ?? {}), ...langs };
    }
    return result;
}

/* =====================================================================
 *  Demo data — replaced once the real backend endpoints are available.
 * ===================================================================== */

const DEMO_LANGUAGES = [
    { code: 'de', name: 'Deutsch' },
    { code: 'en', name: 'English' },
    { code: 'fr', name: 'Français' },
    { code: 'it', name: 'Italiano' },
    { code: 'es', name: 'Español' },
];

const DEMO_FORM_TYPES = [
    { type: 'andp',    nameI18n: { de: 'Acta Nova Dialog Portal', en: 'Acta Nova Dialog Portal' } },
    { type: 'contact', nameI18n: { de: 'wkBlue',                  en: 'wkBlue' } },
    { type: 'survey',  nameI18n: { de: 'Marktplatz',              en: 'Marketplace' } },
];

const DEMO_GLOBAL: Record<string, Record<string, string>> = {
    form_start_button:        { de: 'Service starten',     en: 'Start service',         fr: 'Démarrer le service', it: 'Avvia il servizio',  es: 'Iniciar el servicio' },
    form_submit_button:       { de: 'Absenden',            en: 'Submit',                fr: 'Envoyer',             it: 'Invia',              es: 'Enviar' },
    form_cancel_button:       { de: 'Abbrechen',           en: 'Cancel',                fr: 'Annuler',             it: 'Annulla',            es: 'Cancelar' },
    form_reset_button:        { de: 'Zurücksetzen',        en: 'Reset',                 fr: 'Réinitialiser',       it: 'Reimposta',          es: 'Restablecer' },
    form_next_button:         { de: 'Weiter',              en: 'Next',                  fr: 'Suivant',             it: 'Avanti',             es: 'Siguiente' },
    form_back_button:         { de: 'Zurück',              en: 'Back',                  fr: 'Retour',              it: 'Indietro',           es: 'Atrás' },
    form_loading:             { de: 'Laden …',             en: 'Loading…',              fr: 'Chargement…',         it: 'Caricamento…',       es: 'Cargando…' },
    form_saving:              { de: 'Speichern …',         en: 'Saving…',               fr: 'Enregistrement…',     it: 'Salvataggio…',       es: 'Guardando…' },
    form_saved:               { de: 'Gespeichert',         en: 'Saved',                 fr: 'Enregistré',          it: 'Salvato',            es: 'Guardado' },
    form_error_required:      { de: 'Pflichtfeld',         en: 'Required field',        fr: 'Champ requis',        it: 'Campo obbligatorio', es: 'Campo obligatorio' },
    form_error_email:         { de: 'Ungültige E-Mail',    en: 'Invalid email',         fr: 'E-mail non valide',   it: 'Email non valida',   es: 'Correo no válido' },
    form_error_phone:         { de: 'Ungültige Nummer',    en: 'Invalid number',        fr: 'Numéro non valide',   it: 'Numero non valido',  es: 'Número no válido' },
    form_error_minlength:     { de: 'Zu kurz',             en: 'Too short',             fr: 'Trop court',          it: 'Troppo corto',       es: 'Demasiado corto' },
    form_error_maxlength:     { de: 'Zu lang',             en: 'Too long',              fr: 'Trop long',           it: 'Troppo lungo',       es: 'Demasiado largo' },
    form_error_generic:       { de: 'Ein Fehler ist aufgetreten', en: 'An error occurred', fr: '',               it: '',                   es: '' },
    form_field_label_name:    { de: 'Name',                en: 'Name',                  fr: 'Nom',                 it: 'Nome',               es: 'Nombre' },
    form_field_label_email:   { de: 'E-Mail',              en: 'Email',                 fr: 'E-mail',              it: 'Email',              es: 'Correo' },
    form_field_label_phone:   { de: 'Telefon',             en: 'Phone',                 fr: 'Téléphone',           it: 'Telefono',           es: 'Teléfono' },
    form_field_label_company: { de: 'Firma',               en: 'Company',               fr: 'Société',             it: '',                   es: '' },
    form_field_label_message: { de: 'Nachricht',           en: 'Message',               fr: 'Message',             it: 'Messaggio',          es: 'Mensaje' },
    form_success_title:       { de: 'Vielen Dank!',        en: 'Thank you!',            fr: 'Merci !',             it: 'Grazie!',            es: '¡Gracias!' },
    form_success_message:     { de: 'Ihre Eingabe wurde übermittelt.', en: 'Your submission has been received.', fr: 'Votre envoi a bien été reçu.', it: '', es: '' },
    form_terms_accept:        { de: 'Ich akzeptiere die AGB',    en: 'I accept the terms', fr: 'J’accepte les CGV', it: '',                   es: '' },
    form_privacy_accept:      { de: 'Ich akzeptiere die Datenschutzerklärung', en: 'I accept the privacy policy', fr: '', it: '', es: '' },
    form_newsletter_optin:    { de: 'Newsletter abonnieren',     en: 'Subscribe to newsletter', fr: '',              it: '',                   es: '' },
    form_upload_label:        { de: 'Datei hochladen',           en: 'Upload file',       fr: 'Téléverser un fichier', it: 'Carica file',     es: 'Subir archivo' },
    form_upload_drop:         { de: 'Datei hier ablegen',        en: 'Drop file here',    fr: 'Déposez le fichier ici', it: '',                es: '' },
    form_upload_browse:       { de: 'Durchsuchen',               en: 'Browse',            fr: 'Parcourir',           it: 'Sfoglia',            es: 'Examinar' },
    form_search_placeholder:  { de: 'Suchen …',                  en: 'Search…',           fr: 'Rechercher…',         it: 'Cerca…',             es: 'Buscar…' },
    form_no_results:          { de: 'Keine Ergebnisse',          en: 'No results',        fr: 'Aucun résultat',      it: 'Nessun risultato',   es: 'Sin resultados' },
};

const DEMO_ANDP: Record<string, Record<string, string>> = {
    form_start_button:    { de: 'Dialog starten',     en: 'Start dialog',         fr: 'Démarrer le dialogue', it: 'Avvia il dialogo',    es: 'Iniciar el diálogo' },
    form_submit_button:   { de: 'Antrag senden',      en: 'Submit application',   fr: 'Envoyer la demande',   it: 'Invia richiesta',     es: 'Enviar solicitud' },
    form_success_title:   { de: 'Antrag erhalten',    en: 'Application received', fr: 'Demande reçue',        it: '',                    es: '' },
    form_success_message: { de: 'Ihr Antrag wurde im Acta Nova Dialog Portal registriert.', en: 'Your application has been registered in the Acta Nova Dialog Portal.', fr: '', it: '', es: '' },
};

const DEMO_CONTACT: Record<string, Record<string, string>> = {
    form_submit_button: { de: 'Anfrage senden',   en: 'Send request',     fr: 'Envoyer la demande', it: 'Invia richiesta', es: 'Enviar consulta' },
    form_success_title: { de: 'Anfrage erhalten', en: 'Request received', fr: '',                   it: '',                es: '' },
};
