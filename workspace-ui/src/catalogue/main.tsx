import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';

import { UiProvider } from '@/components/ui/provider';
import i18n from '@/i18n';

import { Catalogue } from './Catalogue';

import '@/index.css';

// Development page for the component layer, served by `npm run catalogue` (vite.catalogue.config.ts).
// It starts in English (the app starts in German); its language select switches.
void i18n.changeLanguage('en');

createRoot(document.getElementById('catalogue')!).render(
    <StrictMode>
        <UiProvider>
            <Catalogue />
        </UiProvider>
    </StrictMode>,
);
