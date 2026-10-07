import { RouterProvider } from '@tanstack/react-router';

import { ErrorNotifications } from '@/components/ErrorNotifications/ErrorNotifications';
import { router } from '@/router';

import './i18n';

function App() {
    return (
        <>
            <RouterProvider router={router} />

            <ErrorNotifications />
        </>
    );
}

export default App;
