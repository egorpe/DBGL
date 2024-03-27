package exodos;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.SequenceInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.dbgl.gui.interfaces.PreProgressNotifyable;
import org.dbgl.model.FileLocation;
import org.dbgl.model.Link;
import org.dbgl.model.SearchResult;
import org.dbgl.model.SearchResult.ResultType;
import org.dbgl.model.aggregate.DosboxVersion;
import org.dbgl.model.aggregate.Profile;
import org.dbgl.model.conf.Autoexec;
import org.dbgl.model.conf.mount.DirMount;
import org.dbgl.model.conf.mount.ImageMount;
import org.dbgl.model.conf.mount.Mount;
import org.dbgl.model.factory.DosboxVersionFactory;
import org.dbgl.model.factory.ProfileFactory;
import org.dbgl.model.repository.BaseRepository;
import org.dbgl.model.repository.DosboxVersionRepository;
import org.dbgl.service.FileLocationService;
import org.dbgl.service.TextService;
import org.dbgl.util.ExecuteUtils;
import org.dbgl.util.FilesUtils;
import org.dbgl.util.ShortFilenameUtils;
import org.dbgl.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


class ExoUtils {

	static final String DOS_METADATA_ARC = "!DOSmetadata.zip";
	static final String XODOS_METADATA_ARC = "XODOSMetadata.zip";
	static final String MEDIAPACK_DOS_BOOKS_ARC = "DOSBooks.zip";
	static final String MEDIAPACK_DOS_CATALOGS_ARC = "DOSCatalogs.zip";
	static final String MEDIAPACK_DOS_MAGAZINES_ARC = "DOSMagazines.zip";
	static final String MEDIAPACK_DOS_SOUNDTRACKS_ARC = "DOSSoundtracks.zip";
	static final String EXODOS_DIR = "eXoDOS";
	static final String UTIL_DIR = "util";
	static final String EXODOS_CONTENT_DIR = "Content";
	static final String EXODOS_EXO_DIR = "eXo";
	static final String EXODOS_GAMEZIPS_DIR = EXODOS_EXO_DIR + File.separator + EXODOS_DIR;
	static final String EXODOS_V6_EXTRA_GAMEDATA_DIR = "GameData" + File.separator + EXODOS_DIR;
	static final String XODOS_METADATA_XML_V5 = "xml/MS-DOS.xml";
	static final String XODOS_METADATA_XML_V6 = "xml/all/MS-DOS.msdos";
	static final String XODOS_METADATA_XML_V6_R2 = "xml/all/MS-DOS.xml";
	static final String UTIL_ZIP = "util.zip";
	static final String UTIL_DOSBOX_TXT = "dosbox.txt";
	static final String UTIL_EXTDOS_ZIP = "EXTDOS.zip";
	static final String UTIL_EXTDOS_EMU_DOSBOX = "emulators/dosbox";
	static final String UTIL_EXTDOS_GLIDE = "glide";
	static final String UTIL_EXTDOS_ROMS = "mt32";
	static final String METADATA_IMAGESDIR = "Images/MS-DOS";
	static final String METADATA_MANUALSDIR = "Manuals/MS-DOS";
	static final String METADATA_MUSICDIR = "Music/MS-DOS";
	static final String METADATA_VIDEODIR = "Videos/MS-DOS";
	static final String DOS_GAMEDIR = "eXo/eXoDOS/!dos/";	
	static final String EXTRAS_DIR = "Extras";
	static final String BASE_IMPORT_DIR = "exo";
	static final String DOSBOX_IMPORT_DIR = BASE_IMPORT_DIR + "/DOSBox";

	static final Charset CP437 = Charset.forName("CP437");
	
	static final List<String> EXTRAFILES = Arrays.asList("rtf", "pdf", "doc", "xls", "htm", "html", "txt", "jpg", "png", "bmp", "gif", "flac", "wav", "ogg", "mp2", "mp3", "mp4", "mod", "s3m", "amf",
		"dsm", "mo3", "psm", "sfx", "voc", "xm");

	private static final Pattern IMG_FILENAME_PTRN = Pattern.compile("^(.*?)(\\.........\\-....\\-....\\-....\\-............)?\\-[0-9][0-9]( \\(.\\))?\\.(.*)$");
	private static final String[] DOS_EXECUTABLES = {".BAT", ".COM", ".EXE"};
	private static final List<String> IMG_EXTENSIONS = Arrays.asList("ISO", "CUE", "BIN", "IMG", "GOG", "IMA", "INST", "DSK", "VFD");
	private static final List<String> DOS_EXTENSIONS = Arrays.asList("BAT", "COM", "EXE");
	private static final List<String> BOOTER_EXTENSIONS = Arrays.asList("IMG", "JRC", "TC", "IMA");

	private static final Map<String, DosboxVersionExo> DOSBOX_EXE_TO_EXO = Map.ofEntries(
			Map.entry("dosbox0.63\\dosbox.exe", new DosboxVersionExo("DOSBox 0.63", "0.63", null, null, false)),
			Map.entry("dosbox0.73\\dosbox.exe", new DosboxVersionExo("DOSBox 0.74-3", "0.74-3", null, null, true)),
			Map.entry("dosbox.exe", new DosboxVersionExo("DOSBox 0.74-3", "0.74-3", "dosbox0.73\\dosbox.exe", null, true)),
			Map.entry("GunStick_dosbox\\dosbox.exe", new DosboxVersionExo("DOSBox Gunstick", "0.74", null, "dosbox.conf", true)),
			Map.entry("DWDdosbox\\dosbox.exe", new DosboxVersionExo("DOSBox R17 by David Walters", "0.74", null, "dosbox.conf", true)),
			Map.entry("daum\\dosbox.exe", new DosboxVersionExo("DOSBox SVN Daum Jan 15, 2015", "0.74", null, null, true)),
			Map.entry("ece4230\\dosbox.exe", new DosboxVersionExo("DOSBox ECE r4230", "0.74", null, null, true)),
			Map.entry("ece4460\\dosbox.exe", new DosboxVersionExo("DOSBox ECE r4460", "0.74", null, null, true)),
			Map.entry("ece4481\\dosbox.exe", new DosboxVersionExo("DOSBox ECE r4481", "0.74", null, null, true)),
			Map.entry("staging0.80.1\\dosbox.exe", new DosboxVersionExo("DOSBox Staging 0.80.1", "0.74", null, "dosbox-staging.conf", true)),
			Map.entry("staging0.81.0a\\dosbox.exe", new DosboxVersionExo("DOSBox Staging 0.81.0a", "0.74", null, "dosbox-staging.conf", true)),
			Map.entry("x\\dosbox.exe", new DosboxVersionExo("DOSBox-X 0.82.18", "0.74", null, "dosbox.reference.conf", true)),
			Map.entry("x2\\dosbox.exe", new DosboxVersionExo("DOSBox-X 0.83.25", "0.74", null, "dosbox-x.reference.conf", true)),
			Map.entry("tc_dosbox\\dosbox.exe", new DosboxVersionExo("DOSBox TC", "0.74", null, null, true)),
			Map.entry("mpubuild_dosbox\\dosbox.exe", new DosboxVersionExo("DOSBox MPU", "0.74", null, null, true))
	);

	private ExoUtils() {
	}

	static ExoDosVersion determineVersion(File inputDir) {
		if (!inputDir.exists()) {
			System.out.println("The directory [" + inputDir + "] does not exist.");
			return ExoDosVersion.UNKNOWN;
		}
		File contentDir = new File(inputDir, EXODOS_CONTENT_DIR);
		if (!contentDir.exists()) {
			System.out.println("The directory [" + inputDir + "] does not contain the [" + EXODOS_CONTENT_DIR + "] directory.");
			return ExoDosVersion.UNKNOWN;
		}
		File exoDir = new File(inputDir, EXODOS_EXO_DIR);
		if (!exoDir.exists()) {
			System.out.println("The directory [" + inputDir + "] does not contain the [" + EXODOS_EXO_DIR + "] directory.");
			return ExoDosVersion.UNKNOWN;
		}
		File gameZipsDir = new File(exoDir, EXODOS_DIR);
		if (!gameZipsDir.exists()) {
			System.out.println("The directory [" + exoDir + "] does not contain the [" + EXODOS_DIR + "] directory.");
			return ExoDosVersion.UNKNOWN;
		}
		File utilDir = new File(exoDir, UTIL_DIR);
		if (!utilDir.exists()) {
			System.out.println("The directory [" + exoDir + "] does not contain the [" + UTIL_DIR + "] directory.");
			return ExoDosVersion.UNKNOWN;
		}
		File dosMetadataFile = new File(contentDir, DOS_METADATA_ARC);
		if (!FilesUtils.isExistingFile(dosMetadataFile)) {
			System.out.println("The file [" + dosMetadataFile + "] does not exist.");
			return ExoDosVersion.UNKNOWN;
		}
		File xodosMetadataFile = new File(contentDir, XODOS_METADATA_ARC);
		if (!FilesUtils.isExistingFile(xodosMetadataFile)) {
			System.out.println("The file [" + xodosMetadataFile + "] does not exist.");
			return ExoDosVersion.UNKNOWN;
		}
		File utilFile = new File(utilDir, UTIL_ZIP);
		if (!FilesUtils.isExistingFile(utilFile)) {
			System.out.println("The file [" + utilFile + "] does not exist.");
			return ExoDosVersion.UNKNOWN;
		}

		try (ZipFile xodosZipfile = new ZipFile(new File(contentDir, XODOS_METADATA_ARC), CP437)) {
			var entries = listEntries(xodosZipfile, true);
			if (entries.parallelStream().anyMatch(x -> x.getName().equals(XODOS_METADATA_XML_V5)))
				return ExoDosVersion.V5;
			else if (listEntries(xodosZipfile, true).parallelStream().anyMatch(x -> x.getName().equals(XODOS_METADATA_XML_V6)))
				return ExoDosVersion.V6;
			else if (listEntries(xodosZipfile, true).parallelStream().anyMatch(x -> x.getName().equals(XODOS_METADATA_XML_V6_R2)))
				return ExoDosVersion.V6_R2;
		} catch (IOException e) {
			e.printStackTrace();
		}

		return ExoDosVersion.UNKNOWN;
	}
	
	public static boolean isMediapackAvailable(File inputDir) {
		File contentDir = new File(inputDir, EXODOS_CONTENT_DIR);
		File file1 = new File(contentDir, MEDIAPACK_DOS_BOOKS_ARC);
		File file2 = new File(contentDir, MEDIAPACK_DOS_CATALOGS_ARC);
		File file3 = new File(contentDir, MEDIAPACK_DOS_MAGAZINES_ARC);
		File file4 = new File(contentDir, MEDIAPACK_DOS_SOUNDTRACKS_ARC);
		return (FilesUtils.isExistingFile(file1) && FilesUtils.isExistingFile(file2) 
				&& FilesUtils.isExistingFile(file3) && FilesUtils.isExistingFile(file4));
	}

	static List<String> determineTitles(List<String> impTitles, final File srcDir, ExoDosVersion version) {
		if (impTitles.isEmpty())
			return impTitles;
		
		final File contentDir = new File(srcDir, EXODOS_CONTENT_DIR);
		
		List<String> result = new ArrayList<>();
		try (ZipFile xodosZipfile = new ZipFile(new File(contentDir, XODOS_METADATA_ARC), CP437)) {
			final List<ZipEntry> xodosZipEntries = listEntries(xodosZipfile, true);
			final NodeList gameNodes = getGameNodes(xodosZipfile, xodosZipEntries, version);

			for (int i = 0; i < gameNodes.getLength(); i++) {
				Element gameNode = (Element)gameNodes.item(i);

				String gameApplicationPath = XmlUtils.getTextValue(gameNode, "ApplicationPath");
				String fullGameTitle = FilenameUtils.getBaseName(gameApplicationPath);
				File gamePath = new File(FilenameUtils.separatorsToSystem(gameApplicationPath)).getParentFile();
				String gameDirName = gamePath != null ? gamePath.getName(): StringUtils.EMPTY;
				if (StringUtils.isBlank(fullGameTitle) || (!impTitles.contains(fullGameTitle.toLowerCase()) && !impTitles.contains(gameDirName.toLowerCase())))
					continue;
				
				result.add(fullGameTitle);
			}
		} catch (IOException | SAXException | ParserConfigurationException | XPathExpressionException e) {
			e.printStackTrace();
		}
		Collections.sort(result);
		return result;
	}
	
	static List<Integer> determineGameIndices(final File srcDir, ExoDosVersion version) {
		final File contentDir = new File(srcDir, EXODOS_CONTENT_DIR);
		
		Map<String, Integer> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		try (ZipFile xodosZipfile = new ZipFile(new File(contentDir, XODOS_METADATA_ARC), CP437)) {
			final List<ZipEntry> xodosZipEntries = listEntries(xodosZipfile, true);
			final NodeList gameNodes = getGameNodes(xodosZipfile, xodosZipEntries, version);

			for (int i = 0; i < gameNodes.getLength(); i++) {
				Element gameNode = (Element)gameNodes.item(i);

				String gameApplicationPath = XmlUtils.getTextValue(gameNode, "ApplicationPath");
				String fullGameTitle = FilenameUtils.getBaseName(gameApplicationPath);
				
				result.put(fullGameTitle, i);
			}
		} catch (IOException | SAXException | ParserConfigurationException | XPathExpressionException e) {
			e.printStackTrace();
		}
		
		return new ArrayList<>(result.values());
	}
	
	static void ensureRoms(File utilDir) throws IOException, ZipException {
		System.out.print("Importing Roland MT-32 roms and SC-55 soundfont ...");
		
		try (ZipFile utilZipFile = new ZipFile(new File(utilDir, UTIL_ZIP));
			 ZipInputStream extdosZipInputStream = new ZipInputStream(utilZipFile.getInputStream(utilZipFile.getEntry(UTIL_EXTDOS_ZIP)))) {
			
			try {
				ZipEntry ze = null;
				while ((ze = extdosZipInputStream.getNextEntry()) != null) {
					if (!ze.isDirectory()) {
						if (ze.getName().startsWith(UTIL_EXTDOS_ROMS)) {
					 		File dstFile = FileLocationService.getInstance().dosrootRelative().canonicalize(new File(ze.getName()));
					 		FilesUtils.createDir(dstFile.getParentFile());
							try (FileOutputStream out = new FileOutputStream(dstFile)) {
								extdosZipInputStream.transferTo(out);
								System.out.print(".");
							}
					 	}
					}
				}
			} catch (IOException e) {
				System.err.println("There was a problem extracting " + UTIL_EXTDOS_ZIP + " from " + UTIL_ZIP);
			}
		}
		
		System.out.println(" done");
	}
	
	static DosboxVersion findDefaultDosboxVersion(boolean verboseOutput) {
		List<DosboxVersion> dbversionsList = null;
		try {

			DosboxVersionRepository dosboxRepo = new DosboxVersionRepository();
			dbversionsList = dosboxRepo.listAll();

			if (BaseRepository.findDefault(dbversionsList) == null) {
				SearchResult result = FileLocationService.getInstance().findDosbox();
				if (result.result_ == ResultType.COMPLETE) {
					new DosboxVersionRepository().add(result.dosbox_);
					dbversionsList = dosboxRepo.listAll();
				}
				if (BaseRepository.findDefault(dbversionsList) == null) {
					System.out.println("DOSBox installation could not be located. Please add a DOSBox Version in DBGL.");
					System.exit(1);
				}
			}

			if (verboseOutput)
				System.out.println("* Using DOSBox installation located in: [" + BaseRepository.findDefault(dbversionsList).getConfigurationCanonicalFile().getPath() + "]");
		} catch (SQLException e) {
			e.printStackTrace();
			System.exit(1);
		}

		return BaseRepository.findDefault(dbversionsList);
	}
	
	public static NodeList getGameNodes(ZipFile xodosZipfile, final List<ZipEntry> xodosZipEntries, ExoDosVersion version)
			throws SAXException, IOException, ParserConfigurationException, XPathExpressionException {
		if (version == ExoDosVersion.V5) {
			final ZipEntry msdosXmlEntry = xodosZipEntries.parallelStream().filter(x -> x.getName().equals(
					XODOS_METADATA_XML_V5)).findAny().orElse(null);

			final Document doc = XmlUtils.getDocumentBuilder().parse(xodosZipfile.getInputStream(msdosXmlEntry));
			return (NodeList)XPathFactory.newInstance().newXPath().evaluate("/LaunchBox/Game", doc, XPathConstants.NODESET);
		} else if (version == ExoDosVersion.V6) {
			final ZipEntry msdosXmlEntry = xodosZipEntries.parallelStream().filter(x -> x.getName().equals(
					XODOS_METADATA_XML_V6)).findAny().orElse(null);
			final Document doc = XmlUtils.getDocumentBuilder().parse(
				new SequenceInputStream( Collections.enumeration(Arrays.asList(
					new ByteArrayInputStream("<?xml version=\"1.0\" standalone=\"yes\"?>".getBytes(StandardCharsets.UTF_8)),
					new ByteArrayInputStream("<LaunchBox>".getBytes(StandardCharsets.UTF_8)),
					xodosZipfile.getInputStream(msdosXmlEntry),
					new ByteArrayInputStream("</LaunchBox>".getBytes(StandardCharsets.UTF_8))
				))));
			return (NodeList)XPathFactory.newInstance().newXPath().evaluate("/LaunchBox/Game[ApplicationPath!='Setup eXoDOS.bat']", doc, XPathConstants.NODESET);
		} else if (version == ExoDosVersion.V6_R2) {
			final ZipEntry msdosXmlEntry = xodosZipEntries.parallelStream().filter(x -> x.getName().equals(
					XODOS_METADATA_XML_V6_R2)).findAny().orElse(null);
			final Document doc = XmlUtils.getDocumentBuilder().parse(xodosZipfile.getInputStream(msdosXmlEntry));
			return (NodeList)XPathFactory.newInstance().newXPath().evaluate("/LaunchBox/Game[ApplicationPath!='Setup eXoDOS.bat']", doc, XPathConstants.NODESET);
		}
		return null;
	}

	static Collection<ZipEntry> getImages(List<ZipEntry> imageEntries, String gameTitle) {
		String gameTitleClean = cleanupGameTitle(gameTitle);
		return imageEntries.parallelStream().filter(x -> {
			Matcher matcher = IMG_FILENAME_PTRN.matcher(FilenameUtils.getName(x.getName()));
			return matcher.matches() && matcher.groupCount() == 4 && matcher.group(1).equalsIgnoreCase(gameTitleClean);
		}).collect(Collectors.toMap(ZipEntry::getCrc, Function.identity(), (entry1, entry2) -> entry1)).values();
	}
	
	static List<ZipReference> getGameCombinedExtras(final File contentDir, ZipFile xodosZipfile, ZipFile dosZipfile,
			final List<ZipEntry> manualEntriesV5, final List<ZipEntry> musicEntriesV5,
			final List<ZipEntry> extrasEntriesV5, String gameTitle, String fullGameTitle, String gameZipFilename,
			String gameDirName, ExoDosVersion version) throws IOException {
		if (version == ExoDosVersion.V5) {
			return getCombinedExtras(xodosZipfile, dosZipfile, extrasEntriesV5, manualEntriesV5, musicEntriesV5, fullGameTitle, gameTitle, gameDirName);
		} else if ((version == ExoDosVersion.V6) || (version == ExoDosVersion.V6_R2)) {
			final File extraGameDataDir = new File(contentDir, EXODOS_V6_EXTRA_GAMEDATA_DIR);
			final File extraZipFile = new File(extraGameDataDir, gameZipFilename);
			if (FilesUtils.isExistingFile(extraZipFile)) {
				try (ZipFile extraGameDataZipfile = new ZipFile(extraZipFile, CP437)) {
					final List<ZipEntry> extraZipEntries = listEntries(extraGameDataZipfile, true);
					final List<ZipEntry> manualEntriesv6 = extraZipEntries.parallelStream().filter(
							x -> x.getName().startsWith(METADATA_MANUALSDIR) && EXTRAFILES.contains(FilenameUtils.getExtension(x.getName()).toLowerCase())).toList();
					final List<ZipEntry> musicEntriesv6 = extraZipEntries.parallelStream().filter(
							x -> x.getName().startsWith(METADATA_MUSICDIR) && EXTRAFILES.contains(FilenameUtils.getExtension(x.getName()).toLowerCase())).toList();
					final List<ZipEntry> videoEntriesv6 = extraZipEntries.parallelStream().filter(
							x -> x.getName().startsWith(METADATA_VIDEODIR) && EXTRAFILES.contains(FilenameUtils.getExtension(x.getName()).toLowerCase())).toList();
					final List<ZipEntry> extrasEntriesv6 = extraZipEntries.parallelStream().filter(
							x -> x.getName().startsWith(DOS_GAMEDIR + gameDirName + "/" + EXTRAS_DIR + "/") && EXTRAFILES.contains(FilenameUtils.getExtension(x.getName()).toLowerCase())).toList();
					return getCombinedExtras(extraGameDataZipfile, manualEntriesv6, musicEntriesv6, videoEntriesv6, extrasEntriesv6);
				}
			}
		}
		return List.of();
	}

	private static List<ZipReference> getCombinedExtras(ZipFile xodosZipfile, ZipFile dosZipfile, List<ZipEntry> extrasEntries, List<ZipEntry> manualEntries, List<ZipEntry> musicEntries, String fullGameTitle,
			String gameTitle, String gameDirName) {
		String gameTitleClean = cleanupGameTitle(gameTitle);
		List<ZipReference> gameCombinedExtraEntries = new ArrayList<>();
		addToCollection(dosZipfile, extrasEntries.parallelStream().filter(x -> x.getName().startsWith(DOS_GAMEDIR + gameDirName + '/' + EXTRAS_DIR)).collect(
			Collectors.toMap(ZipEntry::getCrc, Function.identity(), (entry1, entry2) -> entry1)).values(), gameCombinedExtraEntries);
		addToCollection(xodosZipfile,
			manualEntries.parallelStream().filter(x -> FilenameUtils.getBaseName(x.getName()).equals(fullGameTitle) || FilenameUtils.getBaseName(x.getName()).equals(gameTitleClean)).collect(
				Collectors.toMap(ZipEntry::getCrc, Function.identity(), (entry1, entry2) -> entry1)).values(),
			gameCombinedExtraEntries);
		addToCollection(xodosZipfile,
			musicEntries.parallelStream().filter(x -> FilenameUtils.getBaseName(x.getName()).equals(fullGameTitle) || FilenameUtils.getBaseName(x.getName()).equals(gameTitleClean)).collect(
				Collectors.toMap(ZipEntry::getCrc, Function.identity(), (entry1, entry2) -> entry1)).values(),
			gameCombinedExtraEntries);
		return gameCombinedExtraEntries;
	}

	private static List<ZipReference> getCombinedExtras(ZipFile extraGameDataZipfile, List<ZipEntry> manualEntriesv6,
			List<ZipEntry> musicEntriesv6, List<ZipEntry> videoEntriesv6, List<ZipEntry> extrasEntriesv6) {
		List<ZipReference> gameCombinedExtraEntries = new ArrayList<>();
		addToCollection(extraGameDataZipfile, manualEntriesv6, gameCombinedExtraEntries);
		addToCollection(extraGameDataZipfile, musicEntriesv6, gameCombinedExtraEntries);
		addToCollection(extraGameDataZipfile, videoEntriesv6, gameCombinedExtraEntries);
		addToCollection(extraGameDataZipfile, extrasEntriesv6, gameCombinedExtraEntries);
		return gameCombinedExtraEntries;
	}

	static Profile createProfile(Element gameNode, String fullGameTitle, String titleString, File gamePath, String gameDirName, DosboxVersion dosboxVersion, ZipEntry confEntry, ZipFile dosZipfile,
			String gameSrcZipfile, List<ZipEntry> gameZipEntries, List<ZipReference> gameExtraEntries, boolean verboseOutput) throws IOException {
		HashSet<String> filesInZip = gameZipEntries.stream().filter(x -> !x.isDirectory()).map(x -> FilenameUtils.separatorsToSystem(x.getName())).collect(
			Collectors.toCollection(HashSet::new));

		String developerString = StringUtils.defaultString(XmlUtils.getTextValue(gameNode, "Developer"));
		String publisherString = StringUtils.defaultString(XmlUtils.getTextValue(gameNode, "Publisher"));
		String genreString = StringUtils.defaultString(XmlUtils.getTextValue(gameNode, "Genre"));
		String yearString = StringUtils.left(StringUtils.defaultString(XmlUtils.getTextValue(gameNode, "ReleaseDate")), 4);
		String statusString = StringUtils.EMPTY;
		String notesString = StringUtils.defaultString(XmlUtils.getTextValue(gameNode, "Notes"));
		String ratingString = XmlUtils.getTextValue(gameNode, "CommunityStarRating");
		boolean favorite = false;
		String[] links = new String[Profile.NR_OF_LINK_DESTINATIONS];
		Arrays.fill(links, StringUtils.EMPTY);
		String[] linkTitles = new String[Profile.NR_OF_LINK_TITLES];
		Arrays.fill(linkTitles, StringUtils.EMPTY);
		String confPathAndFile = new File(gameDirName, FileLocationService.DOSBOX_CONF_STRING).getPath();

		List<Link> extraLinks = gameExtraEntries.parallelStream().map(x -> new Link(x.name_, FileLocationService.DOSROOT_DIR_STRING + gameDirName + '/' + EXTRAS_DIR + '/' + x.name_)).limit(
			Profile.NR_OF_LINK_DESTINATIONS).toList();

		IntStream.range(0, extraLinks.size()).forEach(l -> {
			links[l] = extraLinks.get(l).getDestination();
			linkTitles[l] = extraLinks.get(l).getTitle();
		});

		Profile profile = ProfileFactory.create(titleString, developerString, publisherString, genreString, yearString, statusString, notesString, favorite, links, linkTitles, ratingString,
			dosboxVersion, confPathAndFile);

		String warnings = profile.loadConfigurationData(TextService.getInstance(), StringUtils.join(IOUtils.readLines(dosZipfile.getInputStream(confEntry), StandardCharsets.UTF_8), StringUtils.LF),
			new File(gameSrcZipfile));
		if (StringUtils.isNotBlank(warnings)) {
			System.out.println(fullGameTitle + ": " + warnings);
		}

		// minor bit of clean up of the actual dosbox configuration
		if ("64".equals(profile.getConfiguration().getValue("dosbox", "memsize")))
			profile.getConfiguration().setValue("dosbox", "memsize", "63");
		if ("overlay".equals(profile.getConfiguration().getValue("sdl", "output")))
			profile.getConfiguration().removeValue("sdl", "output");

		Autoexec autoexec = profile.getConfiguration().getAutoexec();
		autoexec.migrate(new FileLocation(EXODOS_DIR, FileLocationService.getInstance().dosrootRelative()), FileLocationService.getInstance().getDosrootLocation());

		if (!fixupMainFileLocation(fullGameTitle, profile, gameSrcZipfile, filesInZip, verboseOutput)) {
			String main = autoexec.getGameMain();
			if (StringUtils.isNotEmpty(main))
				System.out.println(fullGameTitle + ": WARNING - Main file [" + main + "] not found inside [" + gameSrcZipfile + "]");
		}
		
		for (Mount m: profile.getNettoMountingPoints()) {
			if (m instanceof ImageMount) {
				fixupImageMount(fullGameTitle, gameSrcZipfile, filesInZip, autoexec, (ImageMount)m, verboseOutput);
			}
		}

		// Check for multiple root entries
		if (filesInZip.parallelStream().filter(x1 -> x1.indexOf(File.separatorChar) == -1).limit(2).count() == 2) {
			autoexec.setBaseDir(gamePath);
			if (verboseOutput)
				System.out.println(fullGameTitle + ": " + gameDirName + " is moved one directory level deeper");
		}

		autoexec.migrate(FileLocationService.getInstance().getDosrootLocation(), new FileLocation(gameDirName, FileLocationService.getInstance().dosrootRelative()));
		
		return profile;
	}

	private static boolean fixupMainFileLocation(String fullGameTitle, Profile profile, String zipFile, Set<String> list, boolean verboseOutput) {
		Autoexec autoexec = profile.getConfiguration().getAutoexec();

		String gameMain = FilenameUtils.separatorsToSystem(autoexec.getGameMain());
		if (StringUtils.isBlank(gameMain))
			return false;

		Mount mnt = null;
		String dosDir = null;
		int minLengthMount = Integer.MAX_VALUE;
		
		for (Mount mount: profile.getNettoMountingPoints()) {
			FileLocation f = new FileLocation(gameMain, FileLocationService.getInstance().dosrootRelative());
			File dosboxDir = mount.canBeUsedFor(f);
			if (dosboxDir != null && (dosboxDir.getPath().length() < minLengthMount)) {
				mnt = mount;
				dosDir = dosboxDir.getParent() == null ? StringUtils.EMPTY: dosboxDir.getParent();
				minLengthMount = dosboxDir.getPath().length();
			}
		}

		if (mnt instanceof DirMount) {
			Set<String> fileSet = FilenameUtils.getName(gameMain).contains("~") ? ShortFilenameUtils.convertToShortFileSet(list): list;
			String mntPath = FilenameUtils.normalize(((DirMount)mnt).getPath().getPath());
			dosDir = StringUtils.isBlank(mntPath) ? StringUtils.removeStart(dosDir, File.separator): StringUtils.prependIfMissing(dosDir, File.separator);
			File dosPath = new File(dosDir, FilenameUtils.getName(gameMain));
			File fullPath = new File(mntPath, dosPath.getPath());
			return fixupDirMount(fullGameTitle, fileSet, profile, autoexec, mntPath, dosDir, dosPath.getPath(), fullPath.getPath(), verboseOutput);
		} else if (mnt instanceof ImageMount) {
			return fixupImageMount(fullGameTitle, zipFile, list, autoexec, (ImageMount)mnt, verboseOutput);
		} else if (autoexec.isBooter()) {
			return fixupBooter(fullGameTitle, zipFile, list, autoexec, verboseOutput);
		}
		
		return false;
	}

	private static boolean fixupBooter(String title, String zipFile, Set<String> fileSet, Autoexec autoexec, boolean verbose) {
		fileSet = fileSet.stream().filter(x -> BOOTER_EXTENSIONS.contains(FilenameUtils.getExtension(x).toUpperCase())).sorted(new FilesUtils.FilenameComparator()).collect(
			Collectors.toCollection(LinkedHashSet::new));

		boolean result = true;
		if (!fixupBooterPart(title, fileSet, autoexec, FilenameUtils.normalize(autoexec.getImg1()), autoexec::setImg1, verbose))
			result = false;
		if (StringUtils.isNotBlank(autoexec.getImg2())) {
			if (!fixupBooterPart(title, fileSet, autoexec, FilenameUtils.normalize(autoexec.getImg2()), autoexec::setImg2, verbose))
				result = false;
		}
		if (StringUtils.isNotBlank(autoexec.getImg3())) {
			if (!fixupBooterPart(title, fileSet, autoexec, FilenameUtils.normalize(autoexec.getImg3()), autoexec::setImg3, verbose))
				result = false;
		}
		
		if (!result && verbose)
			System.out.println(title + ": WARNING - At least one file in [" + StringUtils.join(autoexec.getImg1(), autoexec.getImg2(), autoexec.getImg3()) + "] not found inside [" + zipFile + "]");
		
		return result;
	}
	
	private static boolean fixupBooterPart(String title, Set<String> fileSet, Autoexec autoexec, String path, Consumer<String> updateAutoexec, boolean verbose) {
		if (fileSet.contains(path))
			return true;
		
		String otherBooterPath = fileSet.stream().filter(x -> StringUtils.endsWithIgnoreCase(x, FilenameUtils.getName(path))).findFirst().orElse(null);
		if (otherBooterPath != null) {
			updateAutoexec.accept(otherBooterPath);
			if (verbose)
				System.out.println(title + ": Booter [" + path + "] has wrong path, set to [" + otherBooterPath + "]");
			return true;
		}
		
		return false;
	}
	
	private static boolean fixupImageMount(String title, String zipFile, Set<String> fileSet, Autoexec autoexec, ImageMount mnt, boolean verbose) {
		fileSet = fileSet.stream().filter(x -> IMG_EXTENSIONS.contains(FilenameUtils.getExtension(x).toUpperCase())).sorted(new FilesUtils.FilenameComparator()).collect(
			Collectors.toCollection(LinkedHashSet::new));
		
		boolean result = true;
		for (File mntPath: mnt.getImgPaths()) {
			String normalizedMntPath = FilenameUtils.normalize(mntPath.getPath());
			
			if (!fixupImagemountPart(title, zipFile, fileSet, autoexec, mnt, normalizedMntPath, verbose))
				result = false;
		}
		
		if (!result && verbose)
			System.out.println(title + ": WARNING - At least one file in [" + mnt + "] not found inside [" + zipFile + "]");
		
		return result;
	}
	
	private static boolean fixupImagemountPart(String title, String zipFile, Set<String> fileSet, Autoexec autoexec, ImageMount mnt, String path, boolean verbose) {
		if (fileSet.contains(path))
			return true;

		String mntDifferentCasing = fileSet.stream().filter(x -> x.equalsIgnoreCase(path)).findFirst().orElse(null);
		if (mntDifferentCasing != null) {
			FileLocation f = new FileLocation(path, FileLocationService.getInstance().dosrootRelative());
			FileLocation t = new FileLocation(mntDifferentCasing, FileLocationService.getInstance().dosrootRelative());
			autoexec.migrate(f, t);
			if (verbose)
				System.out.println(title + ": Mount [" + mnt + "] has wrong casing, set to [" + mntDifferentCasing + "]");
			return true;
		}

		String otherMntPath = fileSet.stream().filter(x -> StringUtils.endsWithIgnoreCase(x, FilenameUtils.getName(path))).findFirst().orElse(null);
		if (otherMntPath != null) {
			FileLocation f = new FileLocation(path, FileLocationService.getInstance().dosrootRelative());
			FileLocation t = new FileLocation(otherMntPath, FileLocationService.getInstance().dosrootRelative());
			autoexec.migrate(f, t);
			if (verbose)
				System.out.println(title + ": Mount [" + mnt + "] has wrong path, set to [" + otherMntPath + "]");
			return true;
		}
		
		return false;
	}
	
	private static boolean fixupDirMount(String title, Set<String> fileSet, Profile profile, Autoexec autoexec, String mntPath, String dosDir, String dosPath, String path, boolean verbose) {
		List<String> exts = autoexec.isDos() ? DOS_EXTENSIONS: BOOTER_EXTENSIONS;
		fileSet = fileSet.stream().filter(x -> exts.contains(FilenameUtils.getExtension(x).toUpperCase())).sorted(new FilesUtils.FilenameComparator()).collect(
			Collectors.toCollection(LinkedHashSet::new));
		
		if (fileSet.contains(path))
			return true;
		
		String differentDosPathCasing = fileSet.stream().filter(x -> x.startsWith(mntPath)
				&& x.substring(mntPath.length()).equalsIgnoreCase(dosPath)).findFirst().orElse(null);
		if (differentDosPathCasing != null) {
			autoexec.setGameMain(differentDosPathCasing);
			if (verbose)
				System.out.println(title + ": Main file [" + path + "] has wrong casing for DOS path, set to [" + differentDosPathCasing + "]");
			return true;
		}
		
		String differentExtension = fileSet.stream().filter(x -> x.startsWith(mntPath)
				&& exts.stream().anyMatch(y -> x.substring(mntPath.length()).equalsIgnoreCase(
					FilenameUtils.concat(dosDir, FilenameUtils.getBaseName(dosPath) + FilenameUtils.EXTENSION_SEPARATOR + y)))).findFirst().orElse(null);
		if (differentExtension != null) {
			autoexec.setGameMain(differentExtension);
			if (verbose)
				System.out.println(title + ": Main file [" + path + "] has wrong extension, set to [" + differentExtension + "]");
			return true;
		}
		
		String differentDosPath = fileSet.stream().filter(x -> x.startsWith(mntPath)
				&& exts.stream().anyMatch(y -> StringUtils.endsWithIgnoreCase(x.substring(mntPath.length()), 
					FilenameUtils.getBaseName(dosPath) + FilenameUtils.EXTENSION_SEPARATOR + y))).findFirst().orElse(null);
		if (differentDosPath != null) {
			autoexec.setGameMain(differentDosPath);
			if (verbose)
				System.out.println(title + ": Main file [" + path + "] has wrong DOS path, set to [" + differentDosPath + "]");
			return true;
		}

		String differentCasing = fileSet.stream().filter(x -> StringUtils.startsWithIgnoreCase(x, mntPath)
				&& x.substring(mntPath.length()).equalsIgnoreCase(dosPath)).findFirst().orElse(null);
		if (differentCasing != null) {
			FileLocation f = new FileLocation(mntPath, FileLocationService.getInstance().dosrootRelative());
			FileLocation t = new FileLocation(differentCasing.substring(0, mntPath.length()), FileLocationService.getInstance().dosrootRelative());
			autoexec.migrate(f, t);
			autoexec.setGameMain(differentCasing);
			if (verbose)
				System.out.println(title + ": Main file [" + path + "] has wrong casing, set to [" + differentCasing + "]");
			return true;
		}
		
		String differentPath = fileSet.stream().filter(x -> exts.stream().anyMatch(y -> StringUtils.endsWithIgnoreCase(
				x, FilenameUtils.concat(dosDir, FilenameUtils.getBaseName(dosPath) + FilenameUtils.EXTENSION_SEPARATOR + y)))).findFirst().orElse(null);
		if (differentPath != null) {
			String toDir = new File(differentPath).getParent();
			FileLocation f = new FileLocation(mntPath, FileLocationService.getInstance().dosrootRelative());
			FileLocation t = new FileLocation(toDir != null ? toDir: ".", FileLocationService.getInstance().dosrootRelative());
			autoexec.migrate(f, t);
			autoexec.setGameMain(differentPath);
			if (verbose)
				System.out.println(title + ": Main file [" + path + "] has wrong mount path and/or extension, set to [" + differentPath + "]");
			return true;
		}
		
		
		String[] setPaths = autoexec.getSetPathsFromCustomSections();
		if (FilenameUtils.getName(path).toUpperCase().startsWith("WIN")) {
			String mainBaseFolder = FilenameUtils.getPath(path);
			for (String setPath: setPaths) {
				char pd = setPath.toUpperCase().charAt(0);

				for (Mount m: profile.getNettoMountingPoints()) {
					if (m instanceof DirMount && m.getDrive() == pd) {
						File cp = new File(((DirMount)m).getPathString(), setPath.substring(3));
						File f1 = new File(cp, dosPath);
						String newMain = findSuitableExtension(FilenameUtils.separatorsToWindows(f1.getPath().toUpperCase()), fileSet);
						if (newMain != null) {
							autoexec.setGameMain(newMain);
							if (verbose)
								System.out.println(title + ": Main file [" + path + "] located using set path, changed to [" + newMain + "]");

							// Check and fix path to Windows parameter executable(s)
							String params = autoexec.getParameters();
							if (StringUtils.isNotEmpty(params)) {
								String[] paramArray = StringUtils.split(params);
								String[] fixedParamArray = StringUtils.split(params);
								for (int i = 0; i < paramArray.length; i++) {
									if (paramArray[i].startsWith("/") || paramArray[i].startsWith("-"))
										continue; // unlikely to be file parameter, accept in any case

									String p = fixParameterPath(fileSet, mainBaseFolder, ((DirMount)m).getPathString(), paramArray[i]);
									if (p == null) {
										if (verbose)
											System.out.println(title + ": Parameter [" + paramArray[i] + "] not found, might not be a file or folder");
									} else {
										fixedParamArray[i] = p;
									}
								}
								autoexec.setParameters(StringUtils.join(fixedParamArray, ' '));
								if (verbose)
									System.out.println(title + ": Main file parameter(s) [" + params + "] changed to [" + autoexec.getParameters() + "]");
							}
							return true;
						}
					}
				}
			}
		}
		
		return false;
	}

	private static String fixParameterPath(Set<String> fileSet, String mainBaseFolder, String mountPath, String param) {
		String newParamFile = findSuitableExtension(FilenameUtils.normalize(new File(mainBaseFolder, param).getPath()), fileSet);
		if (newParamFile != null)
			return newParamFile.substring(mountPath.length());
		newParamFile = findSuitableExtension(FilenameUtils.normalize(new File(FilenameUtils.getPath(mainBaseFolder), param).getPath()), fileSet);
		if (newParamFile != null)
			return newParamFile.substring(mountPath.length());
		newParamFile = findSuitableExtension(FilenameUtils.normalize(new File(mountPath, param).getPath()), fileSet);
		if (newParamFile != null)
			return newParamFile.substring(mountPath.length());
		return null;
	}

	private static String findSuitableExtension(String main, Set<String> fileSet) {
		return Stream.of(DOS_EXECUTABLES).filter(x -> fileSet.contains(FilenameUtils.removeExtension(main) + x)).map(x -> FilenameUtils.removeExtension(main) + x).findAny().orElse(null);
	}

	static void addToCollection(ZipFile zipFile, Collection<ZipEntry> entries, List<ZipReference> combinedEntries) {
		for (ZipEntry entry: entries) {
			StringBuilder suggestion = new StringBuilder(FilenameUtils.getName(cleanup(entry)));
			int c = 1;
			do {
				if (c > 1) {
					suggestion.setLength(0);
					suggestion.append(FilenameUtils.getBaseName(entry.getName()) + "(" + c + ")" + FilenameUtils.EXTENSION_SEPARATOR + FilenameUtils.getExtension(entry.getName()));
				}
				c++;
			} while (combinedEntries.parallelStream().anyMatch(x -> x.name_.equalsIgnoreCase(suggestion.toString())));

			combinedEntries.add(new ZipReference(zipFile, entry, suggestion.toString()));
		}
	}

	static List<ZipEntry> listEntries(ZipFile zipFile, boolean ignoreDirectories) {
		List<ZipEntry> entries = new ArrayList<>();
		for (Enumeration<? extends ZipEntry> e = zipFile.entries(); e.hasMoreElements();) {
			try {
				ZipEntry entry = e.nextElement();
				if (!ignoreDirectories || !entry.isDirectory())
					entries.add(entry);
			} catch (IllegalArgumentException iae) {
				System.out.println("WARNING - Zip file [" + zipFile + "] contains an entry with problematic characters in its filename");
			}
		}
		return entries;
	}

	static void unzip(ZipFile zipFile, Collection<ZipEntry> entries, File dstDir, boolean keepDirStructure, boolean prependCounter, PreProgressNotifyable prog) throws IOException {
		if (keepDirStructure) {
			Set<String> dirs = new HashSet<>();
			for (ZipEntry entry: entries) {
				String fullPath = FilenameUtils.getFullPath(entry.getName());
				if (dirs.stream().noneMatch(x -> x.startsWith(fullPath))) {
					dirs.removeIf(fullPath::startsWith);
					dirs.add(fullPath);
				}
			}
			dirs.forEach(x -> new File(dstDir, x).mkdirs());
		} else {
			dstDir.mkdirs();
		}

		int counter = 1;
		for (ZipEntry entry: entries) {
			if (!entry.isDirectory())
				try {
					File dstFile = new File(dstDir, keepDirStructure ? entry.getName(): FilenameUtils.getName(entry.getName()));
					if (prependCounter)
						dstFile = new File(dstFile.getParentFile(), String.format("%02d", counter++) + "_" + dstFile.getName());
					try (InputStream zis = zipFile.getInputStream(entry)) {
						prog.setPreProgress(entry.getSize());
						prog.incrProgress(Files.copy(zis, dstFile.toPath()));
					}
				} catch (IllegalArgumentException e) {
					System.out.println("WARNING - Zip file [" + zipFile + "] contains an entry with problematic characters in its filename");
				}
		}
	}

	static void unzip(List<ZipReference> gameCombinedExtraEntries, File dstDir, PreProgressNotifyable prog) throws IOException {
		dstDir.mkdirs();

		for (ZipReference dst: gameCombinedExtraEntries) {
			try {
				File dstFile = new File(dstDir, dst.name_);
				try (InputStream zis = dst.zipFile_.getInputStream(dst.zipEntry_)) {
					prog.setPreProgress(dst.zipEntry_.getSize());
					prog.incrProgress(Files.copy(zis, dstFile.toPath()));
				}
			} catch (IllegalArgumentException e) {
				System.out.println("WARNING - Zip file [" + dst.zipFile_ + "] contains an entry with problematic characters in its filename");
			}
		}
	}

	static void copyZipData(File srcFile, File baseDirectory, ZipOutputStream zos, PreProgressNotifyable prog) throws IOException {
		try (ZipFile zipFile = new ZipFile(srcFile, CP437)) {
			for (Enumeration<? extends ZipEntry> entries = zipFile.entries(); entries.hasMoreElements();) {
				try {
					ZipEntry srcEntry = entries.nextElement();
					zipEntry(zipFile, srcEntry, FilesUtils.toArchivePath(new File(baseDirectory, cleanup(srcEntry)), srcEntry.isDirectory()), zos, prog);
				} catch (IllegalArgumentException e) {
					System.out.println("\nWARNING: Zip file [" + zipFile.getName() + "] contains an entry with problematic characters in its filename");
				}
			}
		}
	}

	public static void copyZipData(ZipFile zipFile, Collection<ZipEntry> captures, File baseDirectory, ZipOutputStream zos, PreProgressNotifyable prog) throws IOException {
		int counter = 1;
		for (ZipEntry srcEntry: captures) {
			try {
				String name = String.format("%02d", counter++) + "_" + FilenameUtils.getName(cleanup(srcEntry));
				zipEntry(zipFile, srcEntry, FilenameUtils.separatorsToUnix(new File(baseDirectory, name).getPath()), zos, prog);
			} catch (IllegalArgumentException e) {
				System.out.println("\nWARNING: Zip file [" + zipFile.getName() + "] contains an entry with problematic characters in its filename");
			}
		}
	}

	public static void copyZipData(List<ZipReference> extras, File baseDirectory, ZipOutputStream zos, PreProgressNotifyable prog) throws IOException {
		for (ZipReference srcReference: extras) {
			try {
				zipEntry(srcReference.zipFile_, srcReference.zipEntry_, FilenameUtils.separatorsToUnix(new File(baseDirectory, srcReference.name_).getPath()), zos, prog);
			} catch (IllegalArgumentException e) {
				System.out.println("\nWARNING: Zip file [" + srcReference.zipFile_.getName() + "] contains an entry with problematic characters in its filename");
			}
		}
	}

	private static ZipEntry zipEntry(ZipFile zipFile, ZipEntry srcEntry, String dstName, ZipOutputStream zos, PreProgressNotifyable prog) throws IOException {
		ZipEntry dstEntry = new ZipEntry(dstName);
		dstEntry.setComment(srcEntry.getComment());
		dstEntry.setTime(srcEntry.getTime());
		zos.putNextEntry(dstEntry);
		if (!srcEntry.isDirectory()) {
			prog.setPreProgress(srcEntry.getSize());
			prog.incrProgress(IOUtils.copy(zipFile.getInputStream(srcEntry), zos));
		}
		zos.closeEntry();
		return dstEntry;
	}

	private static String cleanupGameTitle(String gameTitle) {
		return gameTitle.replace("/'", "_").replace("':", "_").replace("/", "_").replace("?", "_").replace(":", "_").replace("'", "_").replace("*", "_").replace("\\", "_").replace("\u2219",
			"\u00B7").replace("\u014D", "o").replace("\u0142", "l").replace("\u015B", "s").replace("\u010D", "c").replace("\u25CF", "#u25cf").replace("\u015B", "s").replace("\u0107", "c");
	}

	private static String cleanup(ZipEntry entry) {
		return entry.getName().replace((char)15, '\u263C');
	}

	static Map<String, DosboxVersion> ensureGameDosboxversions(File utilDir, File dstDir, List<String> impTitles) throws IOException, ZipException {
		try (ZipFile utilZipFile = new ZipFile(new File(utilDir, UTIL_ZIP));
			 ZipInputStream extdosZipInputStream = new ZipInputStream(utilZipFile.getInputStream(utilZipFile.getEntry(UTIL_EXTDOS_ZIP)))) {
			final List<String> dosboxEntries = IOUtils.readLines(utilZipFile.getInputStream(utilZipFile.getEntry(UTIL_DOSBOX_TXT)), StandardCharsets.UTF_8);
			final Map<String, String> gameDosboxExeMap = dosboxEntries.stream().map(x -> StringUtils.split(x, ':'))
					.filter(x -> impTitles.isEmpty() || impTitles.contains(x[0]))
					.collect(Collectors.toMap(x -> x[0], x -> x[1], (e1, e2) -> e1));
			return ensureDosboxVersions(extdosZipInputStream, dstDir, gameDosboxExeMap);
		}
	}

	private static Map<String, DosboxVersion> ensureDosboxVersions(ZipInputStream extdosZipInputStream, File dstDir, Map<String, String> gameToDosboxExeMap) {
		Map<String, DosboxVersion> result = new HashMap<>();
		Map<String, DosboxVersion> added = new HashMap<>();
		
		Map<String, DosboxVersionExo> dosboxExeToExoMap = gameToDosboxExeMap.values().stream().distinct()
				.collect(Collectors.toMap(exe -> exe, exe -> DOSBOX_EXE_TO_EXO.get(exe)));
		
		DosboxVersionRepository dosboxRepo = new DosboxVersionRepository();
		System.out.print("Importing eXoDOS DOSBox versions ...");
		
		try {
			List<DosboxVersion> dbversionsList = dosboxRepo.listAll();
			
			for (Map.Entry<String, DosboxVersionExo> entry: dosboxExeToExoMap.entrySet()) {
				String dbvTitle = entry.getValue().title_;
				
				if (dstDir == null) {
					Optional<DosboxVersion> dbv = dbversionsList.stream().filter(x -> x.getTitle().equals(dbvTitle)).findFirst();
					if (dbv.isPresent()) {
						result.put(entry.getKey(), dbv.get());
						continue;
					}
				}
				
				File dstImportDir = dstDir == null ? new File(DOSBOX_IMPORT_DIR): new File(dstDir, DOSBOX_IMPORT_DIR);
				File dbvPath = new File(dstImportDir, entry.getValue().executable_ == null ? entry.getKey() : entry.getValue().executable_);
				File path = dbvPath.getParentFile();
				File confFile = entry.getValue().conf_ == null
						? new File(path, FileLocationService.DOSBOX_CONF_STRING) : new File(path, entry.getValue().conf_);
				
				DosboxVersion newDbv = DosboxVersionFactory.create(
						dbvTitle, entry.getValue().version_, false, entry.getValue().multiconf_, false, null, 
						path.getPath(), StringUtils.EMPTY, StringUtils.EMPTY, confFile.getPath());
				
				if (dstDir == null) {
					dosboxRepo.add(newDbv);
					dbversionsList = dosboxRepo.listAll();
				}
				
				System.out.print(".");
				
				added.put(entry.getKey(), newDbv);
			}
			
			System.out.println(" done");
			
			if (!added.isEmpty()) {
				System.out.print("Extracting eXoDOS DOSBox versions files ...");
				
				Set<String> files = added.values().stream().distinct().map(x -> FilesUtils.toArchivePath(FilesUtils.makeRelativeTo(dstDir, x.getPath()), true))
						.collect(Collectors.toSet());
				
				try {
					ZipEntry ze = null;
					while ((ze = extdosZipInputStream.getNextEntry()) != null) {
						if (!ze.isDirectory()) {
							String name = DOSBOX_IMPORT_DIR + StringUtils.removeStart(ze.getName(), UTIL_EXTDOS_EMU_DOSBOX);
						 	if (files.stream().anyMatch(x -> name.startsWith(x))) {
						 		File dstFile = dstDir == null 
						 				? FileLocationService.getInstance().dosboxRelative().canonicalize(new File(name))
						 				: new File(dstDir, name);
						 		FilesUtils.createDir(dstFile.getParentFile());
								try (FileOutputStream out = new FileOutputStream(dstFile)) {
									extdosZipInputStream.transferTo(out);
								}
						 	}
						}
					}
				} catch (IOException e) {
					System.err.println("There was a problem extracting " + UTIL_EXTDOS_ZIP + " from " + UTIL_ZIP);
				}
				
				System.out.println(" done");
				System.out.print("Creating dosbox.conf for each version ...");
				
				PrintStream tmp = System.out;
				
				added.values().forEach(x -> {
					try {
						if (!FilesUtils.isReadableFile(x.getConfigurationCanonicalFile())) {
							System.setOut(new PrintStream(OutputStream.nullOutputStream()));
							ExecuteUtils.doCreateDosboxConf(x);
							
							x.resetAndLoadConfiguration();
						}
					} catch (IOException e) {
						System.err.println("There was a problem creating dosbox.conf for " + x.getTitle());
					} finally {
						System.setOut(tmp);
						System.out.print(".");
					}
				});
				
				System.out.println(" done");
			}
			
			result.putAll(added);
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return gameToDosboxExeMap.entrySet().stream()
				.collect(Collectors.toMap(e -> e.getKey(), e -> result.get(e.getValue())));
	}
}
