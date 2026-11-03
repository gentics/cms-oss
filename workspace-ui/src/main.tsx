import { QueryClientProvider } from '@tanstack/react-query';
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';

import { UiProvider } from '@/components/ui/provider';
import { createQueryClient } from '@/helper/queryClient/queryClient';

import App from './App.tsx';

import './index.css';

const queryClient = createQueryClient();

createRoot(document.getElementById('root')!).render(
    <StrictMode>
        <QueryClientProvider client={queryClient}>
            <UiProvider>
                <App />
            </UiProvider>
        </QueryClientProvider>
    </StrictMode>,
);
