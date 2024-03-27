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
package exodos;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.dbgl.gui.interfaces.PreProgressNotifyable;
import org.dbgl.model.aggregate.DosboxVersion;
import org.dbgl.model.aggregate.Profile;
import org.dbgl.model.entity.Filter;
import org.dbgl.model.repository.FilterRepository;
import org.dbgl.model.repository.ProfileRepository;
import org.dbgl.service.DatabaseService;
import org.dbgl.service.FileLocationService;
import org.dbgl.util.FilesUtils;
import org.dbgl.util.XmlUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


public class Import {

	private static final String IMPORTER_VERSION = "0.98";

	static final String MEDIAPACK_BOOKS = "Books";
	static final String MEDIAPACK_CATALOGS = "Catalogs";
	static final String MEDIAPACK_MAGAZINES = "Magazines";
	static final String MEDIAPACK_SOUNDTRACKS = "Soundtracks";
	static final String MEDIAPACK_BOOKSDIR = ExoUtils.EXODOS_EXO_DIR + "/" + MEDIAPACK_BOOKS;
	static final String MEDIAPACK_CATALOGSDIR = ExoUtils.EXODOS_EXO_DIR + "/" + MEDIAPACK_CATALOGS;
	static final String MEDIAPACK_MAGAZINESDIR = ExoUtils.EXODOS_EXO_DIR + "/" + MEDIAPACK_MAGAZINES;
	static final String MEDIAPACK_SOUNDTRACKSDIR = ExoUtils.EXODOS_EXO_DIR + "/" + MEDIAPACK_SOUNDTRACKS;
	static final String DOSBOX_IMPORT_BOOKS_DIR = ExoUtils.BASE_IMPORT_DIR + "/" + MEDIAPACK_BOOKS;
	static final String DOSBOX_IMPORT_CATALOGS_DIR = ExoUtils.BASE_IMPORT_DIR + "/" + MEDIAPACK_CATALOGS;
	static final String DOSBOX_IMPORT_MAGAZINES_DIR = ExoUtils.BASE_IMPORT_DIR + "/" + MEDIAPACK_MAGAZINES;
	static final String DOSBOX_IMPORT_SOUNDTRACKS_DIR = ExoUtils.BASE_IMPORT_DIR + "/" + MEDIAPACK_SOUNDTRACKS;

	private static boolean listOnly_ = false;
	private static boolean analyzeOnly_ = false;
	private static boolean skipZips_ = false;
	private static boolean defaultDosboxOnly_ = false;
	private static boolean verboseOutput_ = false;
	private static boolean mediapack_ = false;
	private static ExoDosVersion eXoDOSVersion = ExoDosVersion.UNKNOWN;

	private static void displaySyntax() {
		System.out.println("Use: Import <inputexodosdir> [-l] [-a] [-d] [-v] [game-1] [game-2] [game-N]");
		System.out.println("-l\t\tList game titles and abbreviations, don't import");
		System.out.println("-a\t\tAnalyze only, don't import");
		System.out.println("-d\t\tUse only the default DOSBox version, do not import the ones used in eXoDOS");
		System.out.println("-v\t\tVerbose output");
		System.out.println("-z\t\tDon't import zips (debugging)");
		System.out.println("Optional: game(s) to import based on title or abbreviation");
		System.exit(1);
	}

	public static void main(String[] args) {
		System.out.println("Imports eXoDOS games into DBGL (v" + IMPORTER_VERSION + ")");
		System.out.println();

		if (args.length < 1)
			displaySyntax();

		File srcDir = new File(args[0]);
		List<String> impTitles = new ArrayList<>();

		if (args.length > 1) {
			for (int i = 1; i < args.length; i++) {
				if (args[i].equalsIgnoreCase("-l"))
					listOnly_ = true;
				else if (args[i].equalsIgnoreCase("-a"))
					analyzeOnly_ = true;
				else if (args[i].equalsIgnoreCase("-d"))
					defaultDosboxOnly_ = true;
				else if (args[i].equalsIgnoreCase("-v"))
					verboseOutput_ = true;
				else if (args[i].equalsIgnoreCase("-z"))
					skipZips_ = true;
				else
					impTitles.add(args[i].toLowerCase());
			}
		}

		if (listOnly_)
			System.out.println("* List only");
		if (analyzeOnly_)
			System.out.println("* Analyze only");
		if (defaultDosboxOnly_)
			System.out.println("* Default DOSBox version only");
		if (verboseOutput_)
			System.out.println("* Verbose output");
		if (skipZips_)
			System.out.println("* Don't import zips");
		if (!impTitles.isEmpty())
			System.out.println("* Processing: " + StringUtils.join(impTitles, ", "));
		else
			System.out.println("* Processing all games");

		eXoDOSVersion = ExoUtils.determineVersion(srcDir);
		if (eXoDOSVersion == ExoDosVersion.UNKNOWN) {
			System.out.println("The eXoDOS version could not be determined.");
			System.exit(1);
		}
		
		System.out.println("* eXoDOS " + eXoDOSVersion + " found");
			
		if ((eXoDOSVersion == ExoDosVersion.V6) || (eXoDOSVersion == ExoDosVersion.V6_R2)) {
			mediapack_ = ExoUtils.isMediapackAvailable(srcDir);
			if (mediapack_) {
				System.out.println("* eXoDOS Media Add On Pack found");
			}
		}

		impTitles = ExoUtils.determineTitles(impTitles, srcDir, eXoDOSVersion);
		if (!impTitles.isEmpty())
			System.out.println("* Games: " + StringUtils.join(impTitles, ", "));

		List<Integer> gameIndices = ExoUtils.determineGameIndices(srcDir, eXoDOSVersion);
		
		new Import().importData(impTitles, gameIndices, srcDir);

		try {
			DatabaseService.getInstance().shutdown();
		} catch (SQLException e) {
			// nothing we can do
		}
	}

	private void importData(final List<String> impTitles, final List<Integer> gameIndices, final File srcDir) {
		final File contentDir = new File(srcDir, ExoUtils.EXODOS_CONTENT_DIR);
		final File gameZipsDir = new File(srcDir, ExoUtils.EXODOS_GAMEZIPS_DIR);
		final File exoDir = new File(srcDir, ExoUtils.EXODOS_EXO_DIR);
		final File utilDir = new File(exoDir, ExoUtils.UTIL_DIR);

		try (ZipFile xodosZipfile = new ZipFile(new File(contentDir, ExoUtils.XODOS_METADATA_ARC), ExoUtils.CP437);
				ZipFile dosZipfile = new ZipFile(new File(contentDir, ExoUtils.DOS_METADATA_ARC), ExoUtils.CP437)) {

			final Map<String, DosboxVersion> gameDosboxversionMap = (eXoDOSVersion == ExoDosVersion.V5) || listOnly_ || analyzeOnly_ || defaultDosboxOnly_ 
					? null 
					: ExoUtils.ensureGameDosboxversions(utilDir, null, impTitles);
			final DosboxVersion defaultDosboxVersion = gameDosboxversionMap == null 
					? ExoUtils.findDefaultDosboxVersion(verboseOutput_): null;

			if (((eXoDOSVersion == ExoDosVersion.V6) || (eXoDOSVersion == ExoDosVersion.V6_R2)) && !listOnly_ && !analyzeOnly_ && !skipZips_)
				ExoUtils.ensureRoms(utilDir);
			
			if (mediapack_ && !listOnly_&& !analyzeOnly_ && impTitles.isEmpty() && !skipZips_) {
				System.out.println("Importing Media Add On Pack ...");

				try (ZipFile soundtracksZipfile = new ZipFile(new File(contentDir, ExoUtils.MEDIAPACK_DOS_SOUNDTRACKS_ARC), ExoUtils.CP437)) {
					final List<ZipEntry> soundtracksZipEntries = ExoUtils.listEntries(soundtracksZipfile, true);
					final List<ZipEntry> soundtrackZipfiles = soundtracksZipEntries.parallelStream().filter(
							x -> x.getName().startsWith(MEDIAPACK_SOUNDTRACKSDIR)).toList();
					long size = soundtrackZipfiles.stream().mapToLong(x -> x.getSize()).sum();
					
					PreProgressNotifyable prog = new AsciiProgressBar(MEDIAPACK_SOUNDTRACKS, size);
					
					for (ZipEntry entry: soundtrackZipfiles) {
						prog.setPreProgress(entry.getSize());
						try (ZipInputStream zipInputStream = new ZipInputStream(soundtracksZipfile.getInputStream(entry), ExoUtils.CP437)) {
							ZipEntry ze = null;
							while ((ze = zipInputStream.getNextEntry()) != null) {
								if (!ze.isDirectory()) {
									File dir = new File(DOSBOX_IMPORT_SOUNDTRACKS_DIR, FilenameUtils.getBaseName(entry.getName()));
									File dstFile = new File(dir, ze.getName());
									dstFile.getParentFile().mkdirs();
									try (FileOutputStream out = new FileOutputStream(dstFile)) {
										zipInputStream.transferTo(out);
									}
								}
							}
						}
						prog.incrProgress(entry.getSize());
					}
				} catch (IOException e) {
					e.printStackTrace();
					System.err.println("There was a problem extracting " + ExoUtils.MEDIAPACK_DOS_SOUNDTRACKS_ARC);
				}

				System.out.println(String.format("\r%s Imported.", StringUtils.rightPad(MEDIAPACK_SOUNDTRACKS, 70, '.')));

				extractMedia(new File(contentDir, ExoUtils.MEDIAPACK_DOS_BOOKS_ARC),
						MEDIAPACK_BOOKSDIR, MEDIAPACK_BOOKS, DOSBOX_IMPORT_BOOKS_DIR);
				extractMedia(new File(contentDir, ExoUtils.MEDIAPACK_DOS_CATALOGS_ARC),
						MEDIAPACK_CATALOGSDIR, MEDIAPACK_CATALOGS, DOSBOX_IMPORT_CATALOGS_DIR);
				extractMedia(new File(contentDir, ExoUtils.MEDIAPACK_DOS_MAGAZINES_ARC),
						MEDIAPACK_MAGAZINESDIR, MEDIAPACK_MAGAZINES, DOSBOX_IMPORT_MAGAZINES_DIR);
			}
			
			final List<ZipEntry> xodosZipEntries = ExoUtils.listEntries(xodosZipfile, true);
			final List<ZipEntry> dosZipEntries = ExoUtils.listEntries(dosZipfile, true);

			final List<ZipEntry> dosboxConfEntries = dosZipEntries.parallelStream().filter(x -> x.getName().toLowerCase().endsWith(FileLocationService.DOSBOX_CONF_STRING)).toList();
			final List<ZipEntry> imageEntries = xodosZipEntries.parallelStream().filter(x -> x.getName().startsWith(ExoUtils.METADATA_IMAGESDIR)).toList();
			final List<ZipEntry> manualEntriesV5 = xodosZipEntries.parallelStream().filter(
				x -> x.getName().startsWith(ExoUtils.METADATA_MANUALSDIR) && ExoUtils.EXTRAFILES.contains(FilenameUtils.getExtension(x.getName()).toLowerCase())).toList();
			final List<ZipEntry> musicEntriesV5 = xodosZipEntries.parallelStream().filter(
				x -> x.getName().startsWith(ExoUtils.METADATA_MUSICDIR) && ExoUtils.EXTRAFILES.contains(FilenameUtils.getExtension(x.getName()).toLowerCase())).toList();
			final List<ZipEntry> extrasEntriesV5 = dosZipEntries.parallelStream().filter(x -> ExoUtils.EXTRAFILES.contains(FilenameUtils.getExtension(x.getName()).toLowerCase())).toList();
			final NodeList gameNodes = ExoUtils.getGameNodes(xodosZipfile, xodosZipEntries, eXoDOSVersion);

			Map<String, Set<Integer>> filters = new TreeMap<>();

			int processed = 0;

			for (int i = 0; i < gameNodes.getLength(); i++) {
				Element gameNode = (Element)gameNodes.item(gameIndices.get(i));

				String gameApplicationPath = XmlUtils.getTextValue(gameNode, "ApplicationPath");
				String gameTitle = StringUtils.defaultString(XmlUtils.getTextValue(gameNode, "Title"));
				String fullGameTitle = FilenameUtils.getBaseName(gameApplicationPath);
				String gameZipFilename = fullGameTitle + ".zip";
				File gamePath = new File(FilenameUtils.separatorsToSystem(gameApplicationPath)).getParentFile();
				String gameDirName = gamePath != null ? gamePath.getName(): StringUtils.EMPTY;
				if (StringUtils.isBlank(fullGameTitle) || (!impTitles.isEmpty() && !impTitles.contains(fullGameTitle)))
					continue;

				if (listOnly_) {
					System.out.println(String.format("%4d %-100s %-10s", i + 1, fullGameTitle, gameDirName));
					continue;
				}

				File gameSrcZipfile = new File(gameZipsDir, gameZipFilename);
				if (!FilesUtils.isExistingFile(gameSrcZipfile)) {
					System.err.println(fullGameTitle + ": Zip file " + gameSrcZipfile + " is missing, skipping");
					continue;
				}

				String confPathAndFile = FilenameUtils.separatorsToUnix(new File(gamePath, FileLocationService.DOSBOX_CONF_STRING).getPath());
				ZipEntry confEntry = dosboxConfEntries.parallelStream().filter(x -> x.getName().equalsIgnoreCase(confPathAndFile)).findAny().orElse(null);
				if (confEntry == null) {
					System.err.println(fullGameTitle + ": Zip file " + dosZipfile.getName() + " does not contain " + confPathAndFile + ", skipping");
					continue;
				}
				
				Set<String> playlists = new HashSet<>();

				if ((eXoDOSVersion == ExoDosVersion.V6) || (eXoDOSVersion == ExoDosVersion.V6_R2)) {
					String series = XmlUtils.getTextValue(gameNode, "Series");
					String[] seriesValues = StringUtils.split(series, ';');
					for (String serieValue: seriesValues) {
						String[] kv = StringUtils.split(serieValue, ':');
						if ("Playlist".equals(StringUtils.trim(kv[0]))) {
							playlists.add(StringUtils.trim(kv[1]));
						}
					}
				}

				final Collection<ZipEntry> gameImageEntries = ExoUtils.getImages(imageEntries, gameTitle);
				if (gameImageEntries.isEmpty() && verboseOutput_)
					System.out.println(fullGameTitle + ": No images found");

				final List<ZipReference> gameCombinedExtraEntries = ExoUtils.getGameCombinedExtras(contentDir, xodosZipfile,
						dosZipfile, manualEntriesV5, musicEntriesV5, extrasEntriesV5, gameTitle, fullGameTitle,
						gameZipFilename, gameDirName, eXoDOSVersion);
				
				try (ZipFile gameZipfile = new ZipFile(gameSrcZipfile, ExoUtils.CP437)) {
					final List<ZipEntry> gameZipEntries = ExoUtils.listEntries(gameZipfile, false);
					final DosboxVersion db = gameDosboxversionMap == null 
							? defaultDosboxVersion
							: (gameDosboxversionMap.containsKey(fullGameTitle) 
								? gameDosboxversionMap.get(fullGameTitle)
								: gameDosboxversionMap.entrySet().stream().filter(x -> x.getKey().equalsIgnoreCase(fullGameTitle)).map(x -> x.getValue()).findAny().orElse(null));

					Profile profile = ExoUtils.createProfile(gameNode, fullGameTitle, gameTitle, gamePath, gameDirName, db,
							confEntry, dosZipfile, gameSrcZipfile.getPath(), gameZipEntries, gameCombinedExtraEntries, verboseOutput_);
				
					if (profile.getConfiguration().getAutoexec().isExit() && profile.isIncomplete()) {
						System.out.println(fullGameTitle + ": WARNING - autoexec is incomplete");
					}
					
					if (!analyzeOnly_) {
						profile = new ProfileRepository().add(profile);

						for (String playlist: playlists) {
							Set<Integer> profileIds = filters.getOrDefault(playlist, new HashSet<>());
							profileIds.add(profile.getId());
							filters.put(playlist, profileIds);
						}
				
						PreProgressNotifyable prog = new AsciiProgressBar(fullGameTitle, Stream.of(gameImageEntries, gameZipEntries).flatMap(Collection::stream).mapToLong(ZipEntry::getSize).sum()
								+ gameCombinedExtraEntries.stream().mapToLong(x1 -> x1.zipEntry_.getSize()).sum());
				
						File canonicalGamePath = new File(FileLocationService.getInstance().getDosroot(), gameDirName);

						if (!gameImageEntries.isEmpty() && !skipZips_) {
							ExoUtils.unzip(xodosZipfile, gameImageEntries, profile.getCanonicalCaptures(), false, true, prog);
						}
						
						if (!gameCombinedExtraEntries.isEmpty() && !skipZips_) {
							if (eXoDOSVersion == ExoDosVersion.V5) {
								ExoUtils.unzip(gameCombinedExtraEntries, new File(canonicalGamePath, ExoUtils.EXTRAS_DIR), prog);
							} else {
								try (ZipFile extraGameDataZipfile = new ZipFile(gameCombinedExtraEntries.get(0).zipFile_.getName(), ExoUtils.CP437)) {
									List<ZipReference> map = gameCombinedExtraEntries.stream().map(x -> new ZipReference(extraGameDataZipfile, x.zipEntry_, x.name_)).toList();
									ExoUtils.unzip(map, new File(canonicalGamePath, ExoUtils.EXTRAS_DIR), prog);
								}
							}
						}
						
						if (!skipZips_)
							ExoUtils.unzip(gameZipfile, gameZipEntries, canonicalGamePath, true, false, prog);
				
						double p = (impTitles != null) && !impTitles.isEmpty() ? (double)++processed / (double)impTitles.size() * 100d: (double)(i + 1) / (double)gameNodes.getLength() * 100d;
						System.out.println(String.format("\r%s Imported. Overall progress: %5.1f%%", StringUtils.rightPad(StringUtils.abbreviate(fullGameTitle, 68), 70, '.'), p));
					}
				}
			}

			if (impTitles.isEmpty()) {
				// Create playlists / filters
				FilterRepository repo = new FilterRepository();
				for (Entry<String, Set<Integer>> entry: filters.entrySet()) {
					System.out.print("Creating filter " + entry.getKey() + " ... ");
					repo.add(new Filter(entry.getKey(), "GAM.ID IN (" + StringUtils.join(entry.getValue(), ',') + ")"));
					System.out.println("done");
				}
			}
			
			System.out.println(StringUtils.LF + StringUtils.LF + "Finished.");

		} catch (SQLException | IOException | SAXException | ParserConfigurationException | XPathExpressionException e) {
			e.printStackTrace();
		}
	}

	private void extractMedia(File zipfile, String zipBaseDir, String title, String dstDir) {
		try (ZipFile magazinesZipfile = new ZipFile(zipfile, ExoUtils.CP437)) {
			final List<ZipEntry> allZipEntries = ExoUtils.listEntries(magazinesZipfile, true);
			final List<ZipEntry> zipEntries = allZipEntries.parallelStream().filter(
					x -> x.getName().startsWith(zipBaseDir)).toList();
			long size = zipEntries.stream().mapToLong(x -> x.getSize()).sum();
			
			PreProgressNotifyable prog = new AsciiProgressBar(title, size);
			
			for (ZipEntry entry: zipEntries) {
				File dstFile = new File(dstDir, FilesUtils.relativize(new File(zipBaseDir), new File(entry.getName())));
				dstFile.getParentFile().mkdirs();
				try (InputStream zis = magazinesZipfile.getInputStream(entry);
						FileOutputStream out = new FileOutputStream(dstFile)) {
					prog.setPreProgress(entry.getSize());
					prog.incrProgress(zis.transferTo(out));
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("There was a problem extracting " + zipfile);
		}
		
		System.out.println(String.format("\r%s Imported.", StringUtils.rightPad(title, 70, '.')));
	}
}