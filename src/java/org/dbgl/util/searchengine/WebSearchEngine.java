/*
 *  Copyright (C) 2006-2022  Ronald Blankendaal
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.dbgl.util.searchengine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.dbgl.constants.Constants;
import org.dbgl.model.SearchEngineImageInformation;
import org.dbgl.model.WebProfile;
import org.dbgl.service.SettingsService;
import org.dbgl.util.StringRelatedUtils;


public abstract class WebSearchEngine {

	protected static final String HTTP_PROTOCOL = "http://";
	protected static final String HTTPS_PROTOCOL = "https://";

	protected static final String HTML_HREF_OPEN = " href=\"";
	protected static final String HTML_HREF_OPEN_SQ = " href='";
	protected static final String HTML_SRC_OPEN = " src=\"";
	protected static final String HTML_SRC_OPEN_SQ = " src='";
	protected static final String HTML_QUOTE = "\"";
	protected static final String HTML_QUOTE_SQ = "'";

	protected static final String HTML_MOBY_OPEN = "</moby ";
	protected static final String HTML_MOBY_CLOSE = "</moby>";
	protected static final String HTML_SPAN_OPEN = "<span ";
	protected static final String HTML_SPAN_CLOSE = "</span>";
	protected static final String HTML_ANCHOR_OPEN = "<a ";
	protected static final String HTML_ANCHOR_CLOSE = "</a>";
	protected static final String HTML_DIV_OPEN = "<div";
	protected static final String HTML_DIV_CLOSE = "</div>";
	protected static final String HTML_BLOCKQUOTE_OPEN = "<blockquote>";
	protected static final String HTML_BLOCKQUOTE_CLOSE = "</blockquote>";
	protected static final String HTML_I_OPEN = "<i>";
	protected static final String HTML_I_CLOSE = "</i>";
	protected static final String HTML_UL_OPEN = "<ul>";
	protected static final String HTML_UL_CLOSE = "</ul>";
	protected static final String HTML_OL_OPEN = "<ol>";
	protected static final String HTML_OL_CLOSE = "</ol>";
	protected static final String HTML_LI_OPEN = "<li>";
	protected static final String HTML_LI_CLOSE = "</li>";
	protected static final String HTML_B_OPEN = "<b>";
	protected static final String HTML_B_CLOSE = "</b>";
	protected static final String HTML_STRONG_OPEN = "<strong>";
	protected static final String HTML_STRONG_CLOSE = "</strong>";
	protected static final String HTML_P_OPEN = "<p>";
	protected static final String HTML_PU_OPEN = "<p ";
	protected static final String HTML_P_CLOSE = "</p>";
	protected static final String HTML_EM_OPEN = "<em>";
	protected static final String HTML_EM_CLOSE = "</em>";
	protected static final String HTML_BR_UNCLOSED = "<br>";
	protected static final String HTML_BR_CLOSED = "<br/>";
	protected static final String HTML_BR_CLOSED_ALT = "<br />";
	protected static final String HTML_TD_OPEN = "<td>";
	protected static final String HTML_TD_CLOSE = "</td>";
	protected static final String HTML_TH_OPEN = "<th";
	protected static final String HTML_TH_CLOSE = "</th>";
	protected static final String HTML_TITLE_OPEN = "<title>";
	protected static final String HTML_TITLE_CLOSE = "</title>";
	protected static final String HTML_SMALL_OPEN = "<small>";
	protected static final String HTML_SMALL_CLOSE = "</small>";

	public abstract String getIcon();

	public abstract String getName();

	public abstract String getSimpleName();

	public abstract boolean available();

	public abstract WebProfile getEntryDetailedInformation(WebProfile entry) throws IOException;

	public abstract SearchEngineImageInformation[] getEntryImages(WebProfile entry, int coverArtMax, int screenshotsMax, boolean forceAllRegionsCoverArt) throws IOException;

	public abstract List<WebProfile> getEntries(String title, String[] platforms) throws IOException;

	public void updatePlatforms(String[] platforms) throws IOException {}

	public static int getEntryFirstExactMatchIndex(String title, List<WebProfile> profs) {
		return IntStream.range(0, profs.size()).filter(i -> title.equalsIgnoreCase(profs.get(i).getTitle())).findFirst().orElse(-1);
	}

	public static int getEntryBestMatchIndex(String search, List<WebProfile> profs) {
		return StringRelatedUtils.findBestMatchIndex(search, profs.stream().map(WebProfile::getTitle).toArray(String[]::new));
	}

	public static HttpURLConnection getHttpURLConnection(String url) throws IOException {
		try {
			URL urlConnection = new URI(url).toURL();
			HttpURLConnection conn = (HttpURLConnection)urlConnection.openConnection();
			conn.setConnectTimeout(10000); // 10 seconds
			conn.setReadTimeout(20000); // 20 seconds
			conn.setRequestProperty("User-Agent", Constants.PROGRAM_NAME_FULL);
			return conn;
		} catch (MalformedURLException | URISyntaxException e) {
			throw new IOException(e);
		}
	}
	
	public static WebSearchEngine getBySimpleName(String simpleName) {
		return SettingsService.ALL_WEBSEARCH_ENGINES.stream().filter(x -> x.getSimpleName().equalsIgnoreCase(simpleName)).findAny().orElse(null);
	}

	public static WebSearchEngine getByName(String name) {
		return SettingsService.ALL_WEBSEARCH_ENGINES.stream().filter(x -> x.getName().equals(name)).findAny().orElse(null);
	}

	protected static String getResponseContent(String url, Charset cs) throws IOException {
		HttpURLConnection conn = getHttpURLConnection(url);

		try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), cs))) {
			StringBuilder result = new StringBuilder(8192);
			String str;
			while ((str = in.readLine()) != null)
				result.append(str);
			return result.toString();
		} catch (MalformedURLException e) {
			return null;
		}
	}

	protected static String absoluteUrl(String hostName, String url) {
		if (url.startsWith(HTTP_PROTOCOL) || url.startsWith(HTTPS_PROTOCOL))
			return url;
		return HTTPS_PROTOCOL + hostName + (url.charAt(0) == '/' ? "": "/") + url;
	}

	protected static String extractNextContent(String htmlChunk, int startIndex, String openTag, String closeTag) {
		int divStartIndex = htmlChunk.indexOf(openTag, startIndex);
		divStartIndex = htmlChunk.indexOf(">", divStartIndex) + 1;
		int divEndIndex = htmlChunk.indexOf(closeTag, divStartIndex);
		return htmlChunk.substring(divStartIndex, divEndIndex);
	}

	protected static String extractNextHrefContent(String htmlChunk, int startIndex) {
		int aStartIndex = htmlChunk.indexOf(HTML_HREF_OPEN, startIndex) + HTML_HREF_OPEN.length();
		int aEndIndex = htmlChunk.indexOf(HTML_QUOTE, aStartIndex);
		return htmlChunk.substring(aStartIndex, aEndIndex);
	}

	protected static String extractNextHrefContentSingleQuotes(String htmlChunk, int startIndex) {
		int aStartIndex = htmlChunk.indexOf(HTML_HREF_OPEN_SQ, startIndex) + HTML_HREF_OPEN_SQ.length();
		int aEndIndex = htmlChunk.indexOf(HTML_QUOTE_SQ, aStartIndex);
		return htmlChunk.substring(aStartIndex, aEndIndex);
	}

	protected static String extractNextSrcContentSingleQuotes(String htmlChunk, int startIndex) {
		int aStartIndex = htmlChunk.indexOf(HTML_SRC_OPEN_SQ, startIndex) + HTML_SRC_OPEN_SQ.length();
		int aEndIndex = htmlChunk.indexOf(HTML_QUOTE_SQ, aStartIndex);
		return htmlChunk.substring(aStartIndex, aEndIndex);
	}

	protected static String removeAllTags(String htmlChunk) {
		String result = removeTag(HTML_DIV_OPEN, HTML_DIV_CLOSE, htmlChunk);
		result = removeTag(HTML_ANCHOR_OPEN, HTML_ANCHOR_CLOSE, result);
		result = removeTag(HTML_MOBY_OPEN, HTML_MOBY_CLOSE, result);
		result = replaceTag(HTML_I_OPEN, HTML_I_CLOSE, "", "", result);
		result = replaceTag(HTML_B_OPEN, HTML_B_CLOSE, "", "", result);
		result = replaceTag(HTML_STRONG_OPEN, HTML_STRONG_CLOSE, "", "", result);
		result = replaceTag(HTML_LI_OPEN, HTML_LI_CLOSE, "", StringUtils.LF, result);
		result = replaceTag(HTML_EM_OPEN, HTML_EM_CLOSE, "", "", result);
		result = replaceTag(HTML_UL_OPEN, HTML_UL_CLOSE, StringUtils.LF + StringUtils.LF, StringUtils.LF, result);
		result = replaceTag(HTML_OL_OPEN, HTML_OL_CLOSE, StringUtils.LF + StringUtils.LF, StringUtils.LF, result);
		result = replaceTag(HTML_BLOCKQUOTE_OPEN, HTML_BLOCKQUOTE_CLOSE, StringUtils.LF + StringUtils.LF, StringUtils.LF, result);
		result = result.replaceAll(HTML_P_CLOSE + "\\s*" + HTML_P_OPEN, StringUtils.LF + StringUtils.LF);
		result = replaceTag(HTML_P_OPEN, HTML_P_CLOSE, StringUtils.LF, "", result);
		result = result.replaceAll("\n\n+","\n\n");
		return result;
	}

	protected static String replaceTag(String openTag, String closeTag, String r1, String r2, String htmlChunk) {
		return replaceTag(closeTag, r2, replaceTag(openTag, r1, htmlChunk));
	}

	protected static String replaceTag(String openTag, String r1, String htmlChunk) {
		return htmlChunk.replace(openTag, r1).replace(openTag.toUpperCase(), r1);
	}

	protected static String removeTag(String openTag, String closeTag, String htmlChunk) {
		StringBuilder result = new StringBuilder(htmlChunk);
		int openingIndex = StringUtils.indexOfIgnoreCase(result, openTag);
		while (openingIndex != -1) {
			result.delete(openingIndex, result.indexOf(">", openingIndex + openTag.length()) + 1);
			int closingIndex = StringUtils.indexOfIgnoreCase(result, closeTag);
			result.delete(closingIndex, closingIndex + closeTag.length());
			openingIndex = StringUtils.indexOfIgnoreCase(result, openTag);
		}
		return result.toString();
	}

	protected static String unescapeHtml(String htmlChunk) {
		String result = replaceTag(HTML_BR_UNCLOSED, StringUtils.LF, htmlChunk);
		result = replaceTag(HTML_BR_CLOSED, StringUtils.LF, result);
		result = replaceTag(HTML_BR_CLOSED_ALT, StringUtils.LF, result);
		result = replaceTag("&nbsp;", " ", result);
		result = replaceTag("&apos;", "'", result);
		return StringEscapeUtils.unescapeHtml4(StringUtils.strip(result));
	}

	protected static List<WebProfile> filterEntries(String[] platforms, Collection<WebProfile> allEntries) {
		List<WebProfile> entries = allEntries.stream().filter(x -> Stream.of(platforms).anyMatch(p -> p.equalsIgnoreCase(x.getPlatform()))).sorted().toList();
		if (entries.isEmpty())
			entries = allEntries.stream().sorted().toList();
		return entries;
	}
	
	protected static String prepareSearchTerm(String term) {
		String[] f = SettingsService.getInstance().getValues("websearchengine", "search_term_filter");
		term = term.replace("/", " ");
		for(String s : f)
			term = StringUtils.replaceIgnoreCase(term, s, StringUtils.EMPTY);
		return StringUtils.trim(term);
	}
}