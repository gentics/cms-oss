function gtx_alohalink(tag, renderMode, options) {
    let ctx = {};
    let parts = tag.parts;

    if (parts.target.text) {
        ctx['target'] = ' target="' + parts.target.text + '"';

        if (parts.target.text.trim() === '_blank') {
            ctx['target'] += ' rel="noopener noreferrer"';
        }
    } else {
        if (parts.url.isinternal) {
            ctx['target'] = ' target="_self"';
        } else {
            ctx['target'] = ' target="_blank" rel="noopener noreferrer"'
        }
    }
    if (parts.language.text) {
        ctx['language'] = ' hreflang="' + parts.language.text + '"';
    }
    if (parts.title.text) {
        ctx['title'] = ' title="' 
            + parts.title.text
                .replaceAll(/([\s]+)/, ' ')
                .replaceAll('&', '&amp;')
                .replaceAll('"', '&quot;')
                .replaceAll('\'', '&apos;')
                .replaceAll('<', '&lt;')
                .replaceAll('>', '&gt;')
                .replaceAll('\\\\', '&bsol;')
                .replaceAll(':', '&col;')
                .replaceAll('\\(', '&lpar;')
                .replaceAll('\\)', '&rpar;')
            + '"';
    }

    if (parts.class.text) {
        ctx['class'] = ' class="' + parts.class.text + '"';
    }

    let url = parts.url;
    let href = '';

    if (url.target && url.target.id) {
        // Set the language only then, if no language has been saved yet.
        if (!parts.language.text && url.target.language && url.target.language.code) {
            ctx['language'] = ' hreflang="' + url.target.language.code + '"';
        }
        ctx['data'] = ' data-gentics-aloha-repository="com.gentics.aloha.GCN.Page" data-gentics-aloha-object-id="10007.' + url.target.id + '" data-gentics-aloha-object-online="' + url.target.online + '"';
        href = url.target.url;
    } else {
        if (/^(https|http):\/\/(.*)$/.test(url.externalurl)) {
            ctx['data'] = ' data-gentics-gcn-url="' + encodeURIComponent(url.externalurl) + '"';
            href = url.externalurl;
        }
    }

    // If href is not set here, there is no page URL set, so check for a file URL.
    if (!href && parts.fileurl.target && parts.fileurl.target.id) {
        ctx['data'] = ' data-gentics-aloha-repository="com.gentics.aloha.GCN.File" data-gentics-aloha-object-id="' + parts.fileurl.target.ttype + '.' + parts.fileurl.target.id + '"';
        href = parts.fileurl.target.url;
        ctx['target']  = ' target="_blank"';
    }

    if (!href) {
        ctx['data'] = ' data-gentics-gcn-url="#"';
        href = '#';
    }

    if (href.startsWith('<plink') && renderMode.edit) {
        href = '#';
    }

    if (parts.anchor.text) {
        if (href.endsWith('#')) {
            href += parts.anchor.text;
        } else {
            href += '#' + parts.anchor.text;
        }

        if (!renderMode.publish) {
            ctx['data'] += ' data-gentics-gcn-anchor="' + parts.anchor.text + '"';
        }
    }

    if (href.startsWith('#')) {
        ctx['target'] = ' ';
    }

    ctx['href'] = href.trim();

    return options.fn(ctx);
}
