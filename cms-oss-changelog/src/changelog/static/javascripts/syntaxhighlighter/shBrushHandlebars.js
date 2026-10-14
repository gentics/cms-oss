/**
 * Custom Handlebars highlighter syntax impl
 *
 * Highlights mustache expressions ({{ ... }}, {{{ ... }}}, {{{{ ... }}}}) and comments.
 * Everything outside of mustaches (e.g. HTML) is rendered as plain text.
 */
;(function()
{
	// CommonJS
	typeof(require) != 'undefined' ? SyntaxHighlighter = require('shCore').SyntaxHighlighter : null;

	function Brush()
	{
		var builtins = ' if unless each with lookup log else as ';

		function process(match, regexInfo)
		{
			var constructor = SyntaxHighlighter.Match,
				code = match[0],
				offset = match.index,
				result = [],
				// opening delimiter, optional whitespace control and optional sigil (block, inverse, partial, unescaped, ...)
				open = /^(\{{2,4}~?)(\s*)(#&gt;|#>|#\*|#|\/|\^|&gt;|>|&amp;|&)?/.exec(code),
				close = /~?\}{2,4}$/.exec(code),
				start = open[0].length,
				end = close ? close.index : code.length,
				inner = code.substring(start, end),
				sigil = open[3],
				expectHead = true,
				isHelperContext = sigil != null,
				token = /("(?:[^"\\]|\\.)*"|'(?:[^'\\]|\\.)*')|(-?\b\d+(?:\.\d+)?\b)|\b(true|false|null|undefined)\b|([\w$-]+)(?=\s*=)|(\|[^|]*\|)|(\()|(\))|((?:\.\.\/)*(?:@?[\w$-]+|\[[^\]]*\])(?:[.\/](?:[\w$-]+|\[[^\]]*\]))*|\.\.|\.)/g,
				t
				;

			result.push(new constructor(open[1], offset, 'preprocessor'));

			if (sigil != null)
				result.push(new constructor(sigil, offset + open[1].length + open[2].length, 'preprocessor'));

			if (close != null)
				result.push(new constructor(close[0], offset + close.index, 'preprocessor'));

			while ((t = token.exec(inner)) != null)
			{
				var index = offset + start + t.index,
					css = null,
					nextIsHead = false;

				if (t[1] != null)
					css = 'string';
				else if (t[2] != null || t[3] != null)
					css = 'value';
				else if (t[4] != null)
					css = 'color1';			// hash argument key
				else if (t[5] != null)
					css = 'variable';		// block params: as |item index|
				else if (t[6] != null)
					nextIsHead = true;		// subexpression, its first path is always a helper
				else if (t[8] != null)
				{
					var name = t[8];

					if (builtins.indexOf(' ' + name + ' ') != -1)
					{
						css = 'keyword';
						nextIsHead = name == 'else';	// "else if ..." chains another helper
					}
					else if (expectHead && (isHelperContext || /\S/.test(inner.substring(t.index + name.length))))
						css = 'functions';	// helper, block or partial name
					else if (name.charAt(0) == '@' || name == 'this' || name.indexOf('this.') == 0 || name.indexOf('this/') == 0 || name.indexOf('../') == 0)
						css = 'constants';	// data variables and context references
					else
						css = 'variable';
				}

				if (css != null)
					result.push(new constructor(t[0], index, css));

				expectHead = nextIsHead;
				isHelperContext = nextIsHead;
			}

			return result;
		}

		this.regexList = [
			{ regex: /\{\{~?!--[\s\S]*?--~?\}\}/g,				css: 'comments' },	// {{!-- ... --}}
			{ regex: /\{\{~?![\s\S]*?\}\}/g,					css: 'comments' },	// {{! ... }}
			{ regex: /\{\{\{?\{?[\s\S]*?\}?\}?\}\}/g,			func: process }	// {{ ... }}, {{{ ... }}}, {{{{ ... }}}}
		];
	};

	Brush.prototype	= new SyntaxHighlighter.Highlighter();
	Brush.aliases	= ['hbs', 'handlebars'];

	SyntaxHighlighter.brushes.Handlebars = Brush;

	// CommonJS
	typeof(exports) != 'undefined' ? exports.Brush = Brush : null;
})();
