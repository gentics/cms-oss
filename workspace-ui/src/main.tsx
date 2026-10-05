import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';

import { UiProvider } from '@/components/ui/provider';

import App from './App.tsx';

import './index.css';

const queryClient = new QueryClient();

createRoot(document.getElementById('root')!).render(
    <StrictMode>
        <QueryClientProvider client={queryClient}>
            <UiProvider>
                <App />
            </UiProvider>
        </QueryClientProvider>
    </StrictMode>,
);
