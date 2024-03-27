/*
 *  Copyright (C) 2006-2022  Ronald Blankendaal
 *
 *  Many thanks to Manuel J. Gallego for his work on MobyGames querying
 *  for TincoreADB. This file is based on his code.
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.dbgl.model.SearchEngineImageInformation;
import org.dbgl.model.SearchEngineImageInformation.SearchEngineImageType;
import org.dbgl.model.WebProfile;
import org.dbgl.service.ImageService;
import org.dbgl.service.SettingsService;
import org.dbgl.service.TextService;
import org.json.JSONArray;
import org.json.JSONObject;


public class MobyGamesSearchEngine extends WebSearchEngine {

	static final String MOBY_GAMES_HOST_NAME = "www.mobygames.com";
	static final String MOBY_GAMES_CDN_URL = "cdn.mobygames.com";
	static final String MOBY_GAMES_API_URL = "api.mobygames.com/v1";

    private static final int RESULTS_PER_PAGE = 100;
    
	private static Long lastApiRequest_; 

	private MobyGamesSearchEngine() {
	}

	private static class SearchEngineHolder {
		private static WebSearchEngine instance_ = new MobyGamesSearchEngine();
	}

	public static WebSearchEngine getInstance() {
		return SearchEngineHolder.instance_;
	}

	@Override
	public String getIcon() {
		return ImageService.IMG_MOBYGAMES;
	}

	@Override
	public String getName() {
		return "MobyGames";
	}

	@Override
	public String getSimpleName() {
		return "mobygames";
	}

	@Override
	public boolean available() {
		return true;
	}
	
	@Override
	public void updatePlatforms(String[] platforms) throws IOException {
		List<Integer> platformIds = new ArrayList<>();
		
		if (platforms.length > 0) {
			String url = HTTPS_PROTOCOL + MOBY_GAMES_API_URL + "/platforms";
			JSONObject gameObj = new JSONObject(getApiResponse(url));
			
			Iterator<Object> platformIt = gameObj.getJSONArray("platforms").iterator();
			while (platformIt.hasNext()) {
				JSONObject platformObj = (JSONObject) platformIt.next();
				String name = platformObj.getString("platform_name");
				if (Stream.of(platforms).anyMatch(x -> x.equalsIgnoreCase(name)))
					platformIds.add(platformObj.getInt("platform_id"));
			}	
		}
		
		SettingsService.getInstance().setValue(getSimpleName(), "platform_filter_ids", StringUtils.join(platformIds, ' '));
	}
	
	protected static String getApiResponse(String url) throws IOException {
		if (lastApiRequest_ != null) {
			long waiting = 1000 - (new Date().getTime() - lastApiRequest_);
			if (waiting > 0) {
				try {
					Thread.sleep(waiting);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		}
		
		String key = SettingsService.getInstance().getValue("mobygames", "api_key");
		url += (url.contains("?") ? '&': '?') + "api_key=" + URLEncoder.encode(key, StandardCharsets.UTF_8);
		
		StringBuilder response = new StringBuilder(8192);
		
		HttpURLConnection conn = getHttpURLConnection(url);
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setRequestProperty("Accept", "application/json");

		int responseCode = conn.getResponseCode();
		if (responseCode == 429) {
			throw new IOException(TextService.getInstance().get("dialog.profile.mobygames.error.toomanyrequests"));
		}
		
		try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
			String str;
			while ((str = in.readLine()) != null)
				response.append(str);
		}
		
		lastApiRequest_ = new Date().getTime();
		return response.toString();
	}
	
	@Override
	public List<WebProfile> getEntries(String title, String[] platforms) throws IOException {
		Set<WebProfile> allEntries = new TreeSet<>();
		String baseUrl = HTTPS_PROTOCOL + MOBY_GAMES_API_URL + "/games"
				+ "?title=" + URLEncoder.encode(prepareSearchTerm(title), StandardCharsets.UTF_8);
		
		int[] platformIds = SettingsService.getInstance().getIntValues(getSimpleName(), "platform_filter_ids");
		if (platformIds.length == 0)
			allEntries.addAll(getAllPagedEntries(baseUrl, -1));
		else
			for (int platformId: platformIds)
				allEntries.addAll(getAllPagedEntries(baseUrl + "&platform=" + platformId, platformId));
		
		return new ArrayList<>(allEntries);
	}
	
	private Set<WebProfile> getAllPagedEntries(String baseUrl, int platformId) throws IOException {
		Set<WebProfile> allEntries = new TreeSet<>();
		
		do {
			String url = baseUrl + "&limit=" + RESULTS_PER_PAGE +"&offset=" + allEntries.size();
		
			String json = getApiResponse(url);
			
			Iterator<Object> gameIt = new JSONObject(json).getJSONArray("games").iterator();
			while (gameIt.hasNext()) {
				JSONObject gameObj = (JSONObject) gameIt.next();
				
				WebProfile gameEntry = new WebProfile();
				gameEntry.setGameId(gameObj.getInt("game_id"));
				gameEntry.setTitle(gameObj.getString("title"));
				gameEntry.setUrl(gameObj.getString("moby_url"));
				gameEntry.setGenre(getGenre(gameObj));

				Iterator<Object> platformIt = gameObj.getJSONArray("platforms").iterator();
				
				if (platformId == -1) {
					JSONObject platformObj = (JSONObject) platformIt.next();
					gameEntry.setPlatformId(platformObj.getInt("platform_id"));
					gameEntry.setPlatform(platformObj.getString("platform_name"));
					gameEntry.setYear(platformObj.getString("first_release_date"));
				} else {
					gameEntry.setPlatformId(platformId);
				
					while (platformIt.hasNext()) {
						JSONObject platformObj = (JSONObject) platformIt.next();
						if (platformObj.getInt("platform_id") == platformId) {
							gameEntry.setPlatform(platformObj.getString("platform_name"));
							gameEntry.setYear(platformObj.getString("first_release_date"));
						}
					}
				}
				
				allEntries.add(gameEntry);
			}
		} while (!allEntries.isEmpty() && (allEntries.size() % RESULTS_PER_PAGE == 0));
		
		return allEntries;
	}
	
	@Override
	public WebProfile getEntryDetailedInformation(WebProfile entry) throws IOException {
		String url = HTTPS_PROTOCOL + MOBY_GAMES_API_URL + "/games/" + entry.getGameId();
		JSONObject gameObj = new JSONObject(getApiResponse(url));
		
		entry.setGenre(getGenre(gameObj));
		entry.setRank((int)(gameObj.optDouble("moby_score") * 10));
		entry.setNotes(unescapeHtml(removeAllTags(gameObj.getString("description"))));
		
		String coreCoverUrl = gameObj.getJSONObject("sample_cover").getString("image");
		if (!coreCoverUrl.startsWith(HTTPS_PROTOCOL + MOBY_GAMES_CDN_URL + "/screenshots/"))
			entry.setCoreGameCoverUrl(coreCoverUrl);
		
		url += "/platforms/" + entry.getPlatformId();
		JSONObject gameOnPlatformObj = new JSONObject(getApiResponse(url));
		
		Set<String> publishers = new LinkedHashSet<>();
		Set<String> developers = new LinkedHashSet<>();
		
		JSONArray releasesArr = gameOnPlatformObj.getJSONArray("releases");
		Iterator<Object> releasesIt = releasesArr.iterator();
		while (releasesIt.hasNext()) {
			JSONObject releaseObj = (JSONObject) releasesIt.next();
			
			JSONArray companiesArr = releaseObj.getJSONArray("companies");
			Iterator<Object> companiesIt = companiesArr.iterator();
			
			while (companiesIt.hasNext()) {
				JSONObject companyObj = (JSONObject) companiesIt.next();
				
				if (companyObj.getString("role").equals("Published by"))
					publishers.add(companyObj.getString("company_name"));
				else if (companyObj.getString("role").equals("Developed by"))
					developers.add(companyObj.getString("company_name"));
			}
		}
		
		entry.setPublisherName(StringUtils.join(publishers, ", "));
		entry.setDeveloperName(StringUtils.join(developers, ", "));
		
		return entry;
	}
	
	private String getGenre(JSONObject gameObj) {
		List<String> genres = new ArrayList<>();
		Iterator<Object> genresIt = gameObj.getJSONArray("genres").iterator();
		while (genresIt.hasNext()) {
			JSONObject genresObj = (JSONObject) genresIt.next();
			
			if (genresObj.getString("genre_category").equals("Basic Genres"))
				genres.add(genresObj.getString("genre_name"));
		}
		return StringUtils.join(genres, ", ");
	}

	@Override
	public SearchEngineImageInformation[] getEntryImages(WebProfile entry, int coverArtMax, int screenshotsMax, boolean forceAllRegionsCoverArt) throws IOException {
		List<SearchEngineImageInformation> result = new ArrayList<>();
		if (coverArtMax > 0) {
			result.addAll(getEntryCoverArtInformation(entry, coverArtMax, forceAllRegionsCoverArt));
		}
		if (screenshotsMax > 0) {
			result.addAll(getEntryScreenshotInformation(entry, screenshotsMax));
		}
		entry.setWebImages(result.toArray(new SearchEngineImageInformation[0]));
		return entry.getWebImages();
	}

	private static List<SearchEngineImageInformation> getEntryScreenshotInformation(WebProfile entry, int max) throws IOException {
		List<SearchEngineImageInformation> result = new ArrayList<>();
		
		String url = HTTPS_PROTOCOL + MOBY_GAMES_API_URL + "/games/" + entry.getGameId() + "/platforms/" + entry.getPlatformId() + "/screenshots";
		JSONObject obj = new JSONObject(getApiResponse(url));
		
		Iterator<Object> ssIt = obj.getJSONArray("screenshots").iterator();
		while (ssIt.hasNext() && (result.size() < max)) {
			JSONObject ssObj = (JSONObject) ssIt.next();
			
			result.add(new SearchEngineImageInformation(SearchEngineImageType.SCREENSHOT, ssObj.getString("image"), ssObj.getString("caption")));
		}
		
		return result;
	}

	private static List<SearchEngineImageInformation> getEntryCoverArtInformation(WebProfile entry, int max, boolean forceAllRegionsCoverArt) throws IOException {
		List<SearchEngineImageInformation> result = new ArrayList<>();
		
		String url = HTTPS_PROTOCOL + MOBY_GAMES_API_URL + "/games/" + entry.getGameId() + "/platforms/" + entry.getPlatformId() + "/covers";
		JSONObject obj = new JSONObject(getApiResponse(url));
		
		Iterator<Object> cgIt = obj.getJSONArray("cover_groups").iterator();
		while (cgIt.hasNext()) {
			JSONObject cgObj = (JSONObject) cgIt.next();
			
			JSONArray coversArr = cgObj.getJSONArray("covers");
			Iterator<Object> coversIt = coversArr.iterator();
			while (coversIt.hasNext() && (result.size() < max)) {
				JSONObject coverObj = (JSONObject) coversIt.next();
				
				String descr = StringUtils.join(new String[] {coverObj.getString("scan_of"), coverObj.optString("comments")}, ' ').trim();

				result.add(new SearchEngineImageInformation(SearchEngineImageType.COVER_ART, coverObj.getString("image"), descr));
			}
				
			if (!forceAllRegionsCoverArt)
				break;
		}
		
		return result;
	}
}
