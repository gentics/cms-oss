import axios from 'axios';

const createAxiosClient = (url: string, useInterceptors?: boolean) => {
  const instance = axios.create({
    baseURL: url,
    withCredentials: true,
  });

  if (useInterceptors) {
    instance.interceptors.request.use(req => {
      console.log('[HTTP →]', req.method?.toUpperCase(), req.url);
      console.log('  → headers:', JSON.stringify(req.headers, null, 2));
      console.log('  → body:', req.data);
      return req;
    });
  
    instance.interceptors.response.use(
      res => {
        console.log('[HTTP ←]', res.status, res.config.url);
        console.log('  ← headers:', JSON.stringify(res.headers, null, 2));
        console.log('  ← body:', res.data);
        return res;
      },
      err => {
        console.log('[HTTP !]', err.response?.status, err.config.url);
        console.log('  ! response headers:', JSON.stringify(err.response?.headers, null, 2));
        console.log('  ! response body:', err.response?.data);
        console.log('  ! error message:', err.message);
        return Promise.reject(err);
      }
    );
  }

  return instance;
};

//Später autorefresh token
export default createAxiosClient;