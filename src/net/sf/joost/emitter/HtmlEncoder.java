package net.sf.joost.emitter;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * Encodes text strings and URLs to be HTML-safe.
 */
public final class HtmlEncoder {

	private static float newSizeFactor = 1.3f;


	/**
	 * Lookup table for use in encode() method.
	 *
	 * @see #encode
	 */
	public static final String[] TABLE_HTML = new String[256];

	/**
	 * Lookup table for use in encodeTextXxx() methods.
	 *
	 * @see #encodeText
	 * @see #encodeTextSmart
	 * @see #encodeTextStrict
	 */
	public static final String[] TABLE_HTML_STRICT = new String[256];

	static {
		for (int i = 0; i < 10; i++) {
			TABLE_HTML[i] = "&#00" + i + ";";
		}
		for (int i = 10; i < 32; i++) {
			TABLE_HTML[i] = "&#0" + i + ";";
		}
		for (int i = 32; i < 128; i++) {
			TABLE_HTML[i] = String.valueOf((char)i);
		}
		for (int i = 128; i < 256; i++) {
			TABLE_HTML[i] = "&#" + i + ";";
		}

		// special characters
		TABLE_HTML['\''] = "&#039;";	// apostrophe ('&apos;' doesn't work - it is not by the w3 specs)
		TABLE_HTML['\"'] = "&quot;";	// double quote
		TABLE_HTML['&'] = "&amp;";		// ampersand
		TABLE_HTML['<'] = "&lt;";		// lower than
		TABLE_HTML['>'] = "&gt;";		// greater than

		// strict table
		System.arraycopy(TABLE_HTML, 0, TABLE_HTML_STRICT, 0, 256);
		TABLE_HTML_STRICT[' '] = "&nbsp;";
		TABLE_HTML_STRICT['\n'] = "<br>";		// ascii 10
		TABLE_HTML_STRICT['\r'] = "<br>";		// ascii 13
	}

	// ---------------------------------------------------------------- encoding

	/**
	 * Encode string to HTML-safe text. Extra characters are encoded as decimals,
	 * and five special characters are replaced with their HTML values:
	 * <li>' with &amp;#039;</li>
	 * <li>" with &amp;quot;</li>
	 * <li>&amp; with &amp;amp;</li>
	 * <li>&lt; with &amp;lt;</li>
	 * <li>&gt; with &amp;gt;</li>
	 *
	 * @param string input string
	 *
	 * @return HTML-safe string
	 * @see #encodeText
	 */
	public static String encode(String string) {
		if ((string == null) || (string.length() == 0)) {
			return "";
		}
		int n = string.length();
		StringBuffer buffer = new StringBuffer((int) (n * newSizeFactor));
		int tableLen = TABLE_HTML.length;
		char c;
		for (int i = 0; i < n; i++) {
			c = string.charAt(i);
			if (c < tableLen) {
				buffer.append(TABLE_HTML[c]);
			} else {
				buffer.append("&#").append((int)c).append(';');
			}
		}
		return buffer.toString();
	}

	/**
	 * Encodes text int HTML-safe text and preserves format. Additionaly, the following
	 * characters are replaced:
	 * <li>' ' with &amp;nbsp;</li>
	 * <li>\n with &lt;br&gt;</li>
	 * <li>\r with &lt;br&gt;</li>
	 * <br><br>
	 * Additionaly, this method takes care about CRLF and LF texts and handles
	 * both.
	 * 
	 * Common problem with this method is that spaces are not breakable, so they
	 * may break the outline of the page.
	 *
	 * @param string input string
	 *
	 * @return HTML-safe format
	 */
	public static String encodeTextStrict(String string) {
		if ((string == null) || (string.length() == 0)) {
			return "";
		}
		int n = string.length();
		StringBuffer buffer = new StringBuffer((int) (n * newSizeFactor));
		int tableLen = TABLE_HTML_STRICT.length;
		char c = 0, prev = 0;
		for (int i = 0; i < n; i++, prev = c) {
			c = string.charAt(i);

			if ((c == '\n') && (prev == '\r')) {
				continue;		// previously '\r' (CR) was encoded, so skip '\n' (LF)
			}
			if (c < tableLen) {
				buffer.append(TABLE_HTML_STRICT[c]);
			} else {
				buffer.append("&#").append((int)c).append(';');
			}
		}
		return buffer.toString();
	}

	/**
	 * Encodes text int HTML-safe text and preserves format except spaces.
	 * Additionaly, the following characters are replaced:
	 *
	 * <li>\n with &lt;br&gt;</li>
	 * <li>\r with &lt;br&gt;</li>
	 * <br><br>
	 * Additionaly, this method takes care about CRLF and LF texts and handles
	 * both.
	 *
	 * @param string input string
	 *
	 * @return HTML-safe format
	 */
	public static String encodeText(String string) {
		if ((string == null) || (string.length() == 0)) {
			return "";
		}
		int n = string.length();
		StringBuffer buffer = new StringBuffer((int) (n * newSizeFactor));
		int tableLen = TABLE_HTML_STRICT.length;
		char c = 0, prev = 0;
		for (int i = 0; i < n; i++, prev = c) {
			c = string.charAt(i);;

			if (c == ' ') {
				buffer.append(' ');
				continue;
			}
			if ((c == '\n') && (prev == '\r')) {
				continue;		// previously '\r' (CR) was encoded, so skip '\n' (LF)
			}
			if (c < tableLen) {
				buffer.append(TABLE_HTML_STRICT[c]);
			} else {
				buffer.append("&#").append((int)c).append(';');
			}
		}
		return buffer.toString();
	}

	/**
	 * Encodes text int HTML-safe text and preserves format using smart spaces.
	 * Additionaly, the following characters are replaced:
	 *
	 * <li>\n with &lt;br&gt;</li>
	 * <li>\r with &lt;br&gt;</li>
	 * <br><br>
	 * Additionaly, this method takes care about CRLF and LF texts and handles
	 * both.<br>
	 *
	 * This method is special since it preserves format, but with combination of
	 * not-breakable spaces and common spaces, so breaks are availiable.
	 *
	 * @param string input string
	 *
	 * @return HTML-safe format
	 */
	public static String encodeTextSmart(String string) {
		if ((string == null) || (string.length() == 0)) {
			return "";
		}
		int n = string.length();
		StringBuffer buffer = new StringBuffer((int) (n * newSizeFactor));
		int tableLen = TABLE_HTML_STRICT.length;
		char c = 0, prev = 0;
		boolean prevSpace = false;
		for (int i = 0; i < n; i++, prev = c) {
			c = string.charAt(i);;

			if (c == ' ') {
				if (prev != ' ') {
					prevSpace = false;
				}
				if (prevSpace == false) {
					buffer.append(' ');
				} else {
					buffer.append("&nbsp;");
				}
				prevSpace = !prevSpace;
				continue;
			}
			if ((c == '\n') && (prev == '\r')) {
				continue;		// previously '\r' (CR) was encoded, so skip '\n' (LF)
			}
			if (c < tableLen) {
				buffer.append(TABLE_HTML_STRICT[c]);
			} else {
				buffer.append("&#").append((int)c).append(';');
			}
		}
		return buffer.toString();
	}


	// ---------------------------------------------------------------- URL encode/decode

	/**
	 * Encodes HTML JavaScript for page output using ISO-88591-1 encoding. Null
	 * strings are converted to empty ones. Unfortunatelly, this encoding is not
	 * comatible with the javascripts functions escape/unescape.
	 *
	 * @param string input string
	 *
	 * @return HTML ready string.
	 */
	public static String encodeUrl(String string) {
		return encodeUrl(string, "ISO-8859-1");
	}
	/**
	 * Encodes HTML JavaScript for page output. Null strings are converted to
	 * empty ones.
	 *
	 * @param string   input string
	 * @param encoding
	 *
	 * @return HTML ready string.
	 */
	public static String encodeUrl(String string, String encoding) {
		if (string == null) {
			return "";
		}
		try {
			return URLEncoder.encode(string, encoding);
		} catch (UnsupportedEncodingException e) {
			return null;
		}
	}

	/**
	 * Encodes HTML JavaScript for page output using ISO-88591-1 encoding. Null
	 * strings are converted to empty ones.
	 *
	 * @param string input
	 *
	 * @return HTML ready string.
	 */
	public static String decodeUrl(String string) {
		return decodeUrl(string, "ISO-8859-1");
	}

	/**
	 * Encodes HTML JavaScript for page output. Null strings are converted to empty ones.
	 *
	 * @param string   input
	 * @param encoding encoding
	 *
	 * @return HTML ready string.
	 */
	public static String decodeUrl(String string, String encoding) {
		if (string == null) {
			return "";
		}
		try {
			return URLDecoder.decode(string, encoding);
		} catch (UnsupportedEncodingException e) {
			return null;
		}
	}

}
