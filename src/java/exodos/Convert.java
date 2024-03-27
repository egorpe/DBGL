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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.dbgl.gui.interfaces.PreProgressNotifyable;
import org.dbgl.gui.interfaces.ProgressNotifyable;
import org.dbgl.model.GamePack;
import org.dbgl.model.aggregate.DosboxVersion;
import org.dbgl.model.aggregate.Profile;
import org.dbgl.model.entity.GamePackEntry;
import org.dbgl.service.DatabaseService;
import org.dbgl.service.FileLocationService;
import org.dbgl.service.ImportExportProfilesService;
import org.dbgl.util.FilesUtils;
import org.dbgl.util.XmlUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


public class Convert {

	private static final String CONVERTER_TITLE = "eXoDOS converter";
	private static final String CONVERTER_VERSION = "0.98";
	private static final String GPA_TITLE = "eXoDOS conversion";
	private static final String GPA_NOTES = StringUtils.EMPTY;
	private static final String GPA_AUTHOR = StringUtils.EMPTY;
	private static final String GPA_BASE_FILENAME_V5 = "eXoV5";
	private static final String GPA_BASE_FILENAME_V6 = "eXoV6";
	private static final long BYTES_IN_MB = 1024L * 1024L;
	private static final long MAX_PART_SIZE_DEFAULT_IN_MB = 1024L * 16L;

	private static boolean analyzeOnly_ = false;
	private static boolean skipZips_ = false;
	private static boolean defaultDosboxOnly_ = false;
	private static boolean verboseOutput_ = false;
	private static long maxPartSizeInMB_ = MAX_PART_SIZE_DEFAULT_IN_MB;
	private static int nrOfThreads_ = Math.min(Runtime.getRuntime().availableProcessors(), 6);
	private static ExoDosVersion eXoDOSVersion = ExoDosVersion.UNKNOWN;

	private static void displaySyntax() {
		System.out.println("Use: Convert <inputexodosdir> <dstdir> [-a] [-d] [-v] [-s:size] [game-1] [game-2] [game-N]");
		System.out.println("-a\t\tAnalyze only, don't generate GamePackArchives");
		System.out.println("-d\t\tUse only the default DOSBox version, do not reference the ones used in eXoDOS");
		System.out.println("-v\t\tVerbose output");
		System.out.println("-s:size\t\tTarget size of the GamePackArchives in MB, " + MAX_PART_SIZE_DEFAULT_IN_MB + " is the default (= " + MAX_PART_SIZE_DEFAULT_IN_MB / 1024L + " GB packages)");
		System.out.println("-t:nrOfThreads\tAmount of CPU cores to use when generating GamePackArchives (instead of the default " + nrOfThreads_ + " threads)");
		System.out.println("-z\t\tDon't convert zips (debugging)");
		System.out.println("Optional: game(s) to export based on title or abbreviation");
		System.exit(1);
	}

	public static void main(String[] args) {
		System.out.println("Converts eXoDOS games into DBGL GamePackArchives (v" + CONVERTER_VERSION + ")");
		System.out.println();

		if (args.length < 2)
			displaySyntax();

		File srcDir = new File(args[0]);
		File dstDir = new File(args[1]);
		
		if (!dstDir.exists()) {
			System.out.println("The directory [" + dstDir + "] does not exist.");
			System.exit(1);
		}
		
		List<String> impTitles = new ArrayList<>();

		if (args.length > 2) {
			for (int i = 2; i < args.length; i++) {
				if (args[i].equalsIgnoreCase("-a"))
					analyzeOnly_ = true;
				else if (args[i].equalsIgnoreCase("-d"))
					defaultDosboxOnly_ = true;
				else if (args[i].equalsIgnoreCase("-v"))
					verboseOutput_ = true;
				else if (args[i].toLowerCase().startsWith("-s:")) {
					try {
						maxPartSizeInMB_ = Long.parseLong(args[i].substring(3));
					} catch (NumberFormatException e) {
						// ignore, use the default value
					}
				} else if (args[i].toLowerCase().startsWith("-t:")) {
					try {
						nrOfThreads_ = Integer.parseInt(args[i].substring(3));
					} catch (NumberFormatException e) {
						// ignore, use the default value
					}
				} else if (args[i].equalsIgnoreCase("-z")) {
					skipZips_ = true;
				} else
					impTitles.add(args[i].toLowerCase());
			}
		}

		if (analyzeOnly_)
			System.out.println("* Analyze only");
		if (defaultDosboxOnly_)
			System.out.println("* Default DOSBox version only");
		if (verboseOutput_)
			System.out.println("* Verbose output");
		if (maxPartSizeInMB_ != MAX_PART_SIZE_DEFAULT_IN_MB)
			System.out.println("* Target size of the GamePackArchives: " + maxPartSizeInMB_ + "MB");
		if (skipZips_)
			System.out.println("* Don't convert zips");
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
		
		impTitles = ExoUtils.determineTitles(impTitles, srcDir, eXoDOSVersion);
		if (!impTitles.isEmpty())
			System.out.println("* Games: " + StringUtils.join(impTitles, ", "));
		
		List<Integer> gameIndices = ExoUtils.determineGameIndices(srcDir, eXoDOSVersion);
		
		new Convert().convertData(impTitles, gameIndices, srcDir, dstDir);

		try {
			DatabaseService.getInstance().shutdown();
		} catch (SQLException e) {
			// nothing we can do
		}
	}

	private void convertData(final List<String> impTitles, final List<Integer> gameIndices, File srcDir, File dstDir) {

		System.out.println();
		System.out.println("==========================================");
		System.out.println(" Phase 1 of 2: Analyzing meta-data");
		System.out.println("==========================================");

		final File contentDir = new File(srcDir, ExoUtils.EXODOS_CONTENT_DIR);
		final File gameZipsDir = new File(srcDir, ExoUtils.EXODOS_GAMEZIPS_DIR);
		final File exoDir = new File(srcDir, ExoUtils.EXODOS_EXO_DIR);
		final File utilDir = new File(exoDir, ExoUtils.UTIL_DIR);
		
		GamePack gamePack = new GamePack();
		gamePack.setCreationApp(CONVERTER_TITLE);
		gamePack.setCreationAppVersion(CONVERTER_VERSION);
		gamePack.setCreationDate(new Date());
		gamePack.setTitle(GPA_TITLE);
		gamePack.setAuthor(GPA_AUTHOR);
		gamePack.setNotes(GPA_NOTES);
		gamePack.setCapturesAvailable(true);
		gamePack.setGamedataAvailable(true);
		gamePack.setMapperfilesAvailable(false);
		gamePack.setNativecommandsAvailable(false);
		gamePack.setVersion(ImportExportProfilesService.PROFILES_XML_FORMAT_VERSION);
		
		try (ZipFile xodosZipfile = new ZipFile(new File(contentDir, ExoUtils.XODOS_METADATA_ARC), ExoUtils.CP437);
				ZipFile dosZipfile = new ZipFile(new File(contentDir, ExoUtils.DOS_METADATA_ARC), ExoUtils.CP437)) {

			final Map<String, DosboxVersion> gameDosboxversionMap = (eXoDOSVersion == ExoDosVersion.V5) || analyzeOnly_ || defaultDosboxOnly_ 
					? null : ExoUtils.ensureGameDosboxversions(utilDir, dstDir, impTitles);
			final DosboxVersion defaultDosboxVersion = gameDosboxversionMap == null 
					? ExoUtils.findDefaultDosboxVersion(verboseOutput_): null;
			
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
			
			ProgressNotifyable prog = new AsciiProgressBar("Analysis", gameNodes.getLength());

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
					
					long size = Stream.of(gameImageEntries, gameZipEntries).flatMap(Collection::stream).mapToLong(ZipEntry::getCompressedSize).sum()
							+ gameCombinedExtraEntries.stream().mapToLong(x -> x.zipEntry_.getCompressedSize()).sum();

					GamePackEntry newGamePackEntry = new GamePackEntry(i, profile, gameDirName, gameImageEntries, gameCombinedExtraEntries, gameSrcZipfile, size);
					gamePack.getEntries().add(newGamePackEntry);
				}

				prog.incrProgress(1);
			}

			Collections.sort(gamePack.getEntries());

			System.out.println();
			System.out.println("Meta-data analysis done");

			if (!analyzeOnly_)
				generateGamePackArchives(gamePack, xodosZipfile, dstDir);

		} catch (IOException | SAXException | ParserConfigurationException | XPathExpressionException e) {
			e.printStackTrace();
		}
	}

	private static void generateGamePackArchives(GamePack gamePack, ZipFile xodosZipfile, File tmpDir) {
		System.out.println();
		System.out.println("===========================================");
		System.out.println(" Phase 2 of 2: Generating GamePackArchives");
		System.out.println("===========================================");

		List<List<GamePackEntry>> allGamePacksToCreate = new ArrayList<>();
		List<GamePackEntry> remainingGamePackEntries = new ArrayList<>(gamePack.getEntries());

		while (!remainingGamePackEntries.isEmpty()) {

			long totalSize = 0L;
			List<GamePackEntry> currentGamePackEntries = new ArrayList<>();
			boolean reachedMaxSize = false;

			while (!remainingGamePackEntries.isEmpty() && !reachedMaxSize) {
				GamePackEntry gamePackEntry = remainingGamePackEntries.get(0);
				long gameSize = gamePackEntry.getSize() + 1024 * 4; // reserved 4 KB for profiles.xml data
				if (currentGamePackEntries.isEmpty() || ((totalSize + gameSize) < (maxPartSizeInMB_ * BYTES_IN_MB))) {
					currentGamePackEntries.add(gamePackEntry);
					remainingGamePackEntries.remove(0);
					totalSize += gameSize;
				} else {
					reachedMaxSize = true;
				}
			}

			allGamePacksToCreate.add(currentGamePackEntries);
		}

		ThreadPoolExecutor executor = (ThreadPoolExecutor)Executors.newFixedThreadPool(nrOfThreads_);
		CountDownLatch latch = new CountDownLatch(allGamePacksToCreate.size());

		for (List<GamePackEntry> gamePackEntries: allGamePacksToCreate) {
			executor.submit(() -> {
				File currentOutputGpa = new File(tmpDir,
						(eXoDOSVersion == ExoDosVersion.V5 ? GPA_BASE_FILENAME_V5: GPA_BASE_FILENAME_V6) + "__" + FilesUtils.toSafeFilename(gamePackEntries.get(0).getProfile().getTitle())
								+ (gamePackEntries.size() > 1 ? " - " + FilesUtils.toSafeFilename(gamePackEntries.get(gamePackEntries.size() - 1).getProfile().getTitle()): StringUtils.EMPTY)
								+ FilesUtils.GAMEPACKARCHIVE_EXT);

				try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(currentOutputGpa))) {
					for (GamePackEntry game: gamePackEntries) {
						try {

							Profile prof = game.getProfile();
							File relativeGameDirInZip = game.getArchiveGameDir();
							File relativeExtrasGameDirInZip = new File(relativeGameDirInZip, ExoUtils.EXTRAS_DIR);

							PreProgressNotifyable prog = new DotProgress(prof.getTitle());

							if (!skipZips_)
								ExoUtils.copyZipData(xodosZipfile, game.getCaptures(), game.getArchiveCapturesDir(), zipOutputStream, prog);
							
							if (!game.getExtras().isEmpty() && !skipZips_) {
								if (eXoDOSVersion == ExoDosVersion.V5) {
									ExoUtils.copyZipData(game.getExtras(), relativeExtrasGameDirInZip, zipOutputStream, prog);
								} else {
									try (ZipFile extraGameDataZipfile = new ZipFile(game.getExtras().get(0).zipFile_.getName(), ExoUtils.CP437)) {
										List<ZipReference> map = game.getExtras().stream().map(x -> new ZipReference(extraGameDataZipfile, x.zipEntry_, x.name_)).toList();
										ExoUtils.copyZipData(map, relativeExtrasGameDirInZip, zipOutputStream, prog);
									}
								}
							}
							
							if (!skipZips_)
								ExoUtils.copyZipData(game.getGameZipFile(), relativeGameDirInZip, zipOutputStream, prog);

						} catch (IOException e2) {
							System.out.println(
								"\nWARNING: The file [" + game.getGameZipFile() + "] could not be copied (completely) properly into the [" + currentOutputGpa + "], this game may be corrupt");
							e2.printStackTrace();
						}
					}
					ImportExportProfilesService.export(gamePack, gamePackEntries, zipOutputStream);
				} catch (ParserConfigurationException | TransformerException | IOException e) {
					e.printStackTrace();
				}

				latch.countDown();
				double p = (double)(allGamePacksToCreate.size() - latch.getCount()) / (double)allGamePacksToCreate.size() * 100d;
				System.out.println(String.format(StringUtils.LF + "DBGL GamePackArchive %s successfully generated. Overall progress: %3.1f%%", currentOutputGpa.getPath(), p));
				return null;
			});
		}

		executor.shutdown();

		try {
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
			Thread.currentThread().interrupt();
		}

		System.out.println(StringUtils.LF + StringUtils.LF + "Finished.");
	}
}
