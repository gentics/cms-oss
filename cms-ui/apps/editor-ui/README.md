# Gentics CMS Editor UI

This is the editor interface for the Gentics CMS.

## Prerequisites

To test the Gentics CMS Editor UI (GCMS UI) a running instance of Gentics CMS **v6.0.0+** is required.
The default setup expects the CMS to be available under Port `8080`.

## Running Locally

1. Copy `proxy.conf.json.example` to `proxy.conf.json` and make changes if necessary.
2. `cd ../..`
3. `npm install`
4. `npm start editor-ui`

This will run a live development server on http://localhost:4200 and proxy requests to the GCMS REST API to http://localhost:8080.
This will watch the source files for changes and rebuild on every change. Since these Applications are big applications, auto-reload of the browser window has been disabled.
The proxy configuration can be changed in [proxy.conf.json](./proxy.conf.json).

## Analyzing Prod Bundle Size

To analyze the size and composition of the production build `webpack-bundle-analyzer` is used.
The [package.json](./package.json) file contains ready to use scripts for this purpose.
Run the following commands to analyze the bundle size:

1. `npm run build-with-stats editor-ui`
2. `npm run bundle-report editor-ui`

## Automated Tests

To run the unit tests in watch mode (rebuild and rerun on every change):

`npm run test:watch editor-ui`

To run the unit tests and then terminate the test process:

`npm test editor-ui`

## Release Build

1. `npm install`
2. `npm run build editor-ui`

## Serving Release Build Locally

1. Make a release build.
2. Serve the `/dist/apps/editor-ui` directory from a web server, and proxy to the Gentics CMS url. See examples below:

#### Example using [light-server](https://www.npmjs.com/package/light-server)

1. `npm install -g light-server`
2. `light-server -s ./dist/apps/editor-ui -x http://<url of contentnode> --no-reload`

#### Alternative example: nginx proxy setup for development

```
http {
    include       mime.types;
    default_type  application/octet-stream;

    server {
        listen   8080;
        index index.html index.htm;

        gzip on;
        gzip_types text/plain text/xml text/css application/javascript;

        # Maximum upload filesize, increase as needed
        client_max_body_size 20M;

        proxy_set_header  Host $host;
        proxy_set_header  X-Real-IP  $remote_addr;
        proxy_set_header  X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_redirect    off;

        location /.Node/ui {
            alias <path-to-editor-ui-dist>/;
            expires -1;
            add_header Pragma "no-cache";
            add_header Cache-Control "no-store, no-cache, must-revalidate, post-check=0, pre-check=0";
        }

        location /.Node {
            proxy_pass    http://<url of contentnode>/.Node;
        }

        location /CNPortletapp {
            proxy_pass    http://<url of contentnode>/CNPortletapp;
        }

        location /GenticsImageStore {
            proxy_pass    http://<url of contentnode>/CNPortletapp/GenticsImageStore;
        }
    }
}
```

## Developer Guide

Information regarding setting up custom configuration and scripts can be found in the [Developer Guide](./DEVELOPER_GUIDE.md).
