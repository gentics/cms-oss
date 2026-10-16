import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';

import { UiProvider } from '@/components/ui/provider';

import { Catalogue } from './Catalogue';

import '@/index.css';

import '@/i18n';

// Development page for the component layer, served by `npm run catalogue` (vite.catalogue.config.ts).
createRoot(document.getElementById('catalogue')!).render(
    <StrictMode>
        <UiProvider>
            <Catalogue />
        </UiProvider>
    </StrictMode>,
);
