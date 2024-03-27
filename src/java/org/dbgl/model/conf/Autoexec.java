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
package org.dbgl.model.conf;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.dbgl.exception.InvalidMountstringException;
import org.dbgl.model.FileLocation;
import org.dbgl.model.conf.mount.DirMount;
import org.dbgl.model.conf.mount.ImageMount;
import org.dbgl.model.conf.mount.Mount;
import org.dbgl.model.factory.MountFactory;
import org.dbgl.service.FileLocationService;
import org.dbgl.util.FilesUtils;
import org.dbgl.util.SystemUtils;


public class Autoexec {

	class DosPath {
		String drive_, directory_, file_;

		public DosPath(FileLocation hostFileLocation, List<Mount> mountingpoints) {
			int minLengthMount = Integer.MAX_VALUE;
			for (Mount mount: mountingpoints) {
				File dosboxDir = mount.canBeUsedFor(hostFileLocation);
				if (dosboxDir != null && (dosboxDir.getPath().length() < minLengthMount)) {
					drive_ = mount.getDrive() + ":";
					directory_ = dosboxDir.getParent() == null ? StringUtils.EMPTY: FilenameUtils.separatorsToWindows(dosboxDir.getParent());
					file_ = hostFileLocation.getFile().getName();
					minLengthMount = dosboxDir.getPath().length();
				}
			}
		}
	}

	private static final String DFR_4DOS_PREFIX = "C:\\FREEDOS\\4DOS.COM /C ";
	private static final String DFR_DOS32A_PREFIX = "C:\\FREEDOS\\DOS32A.EXE ";

	private static final String[] CUSTOM_SECTION_MARKERS = {"@REM START", "@REM /START", "@REM PRE-LAUNCH", "@REM /PRE-LAUNCH", "@REM POST-LAUNCH", "@REM /POST-LAUNCH", "@REM FINISH", "@REM /FINISH"};
	public static final int SECTIONS = CUSTOM_SECTION_MARKERS.length / 2;

	private static final Pattern LOADHIGH_PATRN = Pattern.compile("^(?:lh|loadhigh)\\s+(.*)$", Pattern.CASE_INSENSITIVE);
	private static final Pattern LOADFIX_PATRN = Pattern.compile("^(?:loadfix\\s+(-f)\\s*|loadfix(\\s+-\\d+)?(\\s+.*)?)$", Pattern.CASE_INSENSITIVE);
	private static final Pattern BOOT_PATRN = Pattern.compile("^(?:boot)(?=\\s+.\\S+)((?:\\s+[^-][^\\s]*)*)(?:\\s+-l\\s+([acd]):?)?\\s*$", Pattern.CASE_INSENSITIVE);
	private static final Pattern BOOTIMGS_PATRN = Pattern.compile("^((?:\"[^\"]+\")|(?:[^\\s]+))(?:\\s((?:\"[^\"]+\")|(?:[^\\s]+)))?(?:\\s((?:\"[^\"]+\")|(?:[^\\s]+)))?$", Pattern.CASE_INSENSITIVE);
	private static final int DEFAULT_LOADFIX_VALUE = 64;

	private String main_, parameters_, img1_, img2_, img3_, imgDriveletter_, mixer_, keyb_, ipxnet_;
	private Boolean loadfix_, loadhigh_, exit_, pause_, booterByDefault_;
	private Integer loadfixValue_;
	private String[] customSections_;
	private List<Mount> mountingpoints_;

	public Autoexec() {
		main_ = StringUtils.EMPTY;
		parameters_ = StringUtils.EMPTY;
		img1_ = StringUtils.EMPTY;
		img2_ = StringUtils.EMPTY;
		img3_ = StringUtils.EMPTY;
		imgDriveletter_ = StringUtils.EMPTY;
		mixer_ = StringUtils.EMPTY;
		keyb_ = StringUtils.EMPTY;
		ipxnet_ = StringUtils.EMPTY;
		loadfix_ = false;
		loadhigh_ = false;
		exit_ = false;
		pause_ = false;
		booterByDefault_ = false;
		loadfixValue_ = 0;
		customSections_ = new String[SECTIONS];
		for (int i = 0; i < SECTIONS; i++)
			customSections_[i] = StringUtils.EMPTY;
		mountingpoints_ = new ArrayList<>();
	}

	public Autoexec(Autoexec autoexec) {
		main_ = autoexec.main_;
		parameters_ = autoexec.parameters_;
		img1_ = autoexec.img1_;
		img2_ = autoexec.img2_;
		img3_ = autoexec.img3_;
		imgDriveletter_ = autoexec.imgDriveletter_;
		mixer_ = autoexec.mixer_;
		keyb_ = autoexec.keyb_;
		ipxnet_ = autoexec.ipxnet_;
		loadfix_ = autoexec.loadfix_;
		loadhigh_ = autoexec.loadhigh_;
		exit_ = autoexec.exit_;
		pause_ = autoexec.pause_;
		booterByDefault_ = autoexec.booterByDefault_;
		loadfixValue_ = autoexec.loadfixValue_;
		customSections_ = Arrays.copyOf(autoexec.customSections_, SECTIONS);
		mountingpoints_ = new ArrayList<>();
	}

	public String getMain() {
		return main_;
	}

	public void setMain(String main) {
		main_ = main;
	}

	public String getParameters() {
		return parameters_;
	}

	public void setParameters(String parameters) {
		parameters_ = parameters;
	}

	public String getImg1() {
		return img1_;
	}

	public void setImg1(String img1) {
		img1_ = img1;
	}

	public String getImg2() {
		return img2_;
	}

	public void setImg2(String img2) {
		img2_ = img2;
	}

	public String getImg3() {
		return img3_;
	}

	public void setImg3(String img3) {
		img3_ = img3;
	}

	public String getImgDriveletter() {
		return imgDriveletter_;
	}

	public void setImgDriveletter(String imgDriveletter) {
		imgDriveletter_ = imgDriveletter;
	}

	public String getMixer() {
		return mixer_;
	}

	public void setMixer(String mixer) {
		mixer_ = mixer;
	}

	public String getKeyb() {
		return keyb_;
	}

	public void setKeyb(String keyb) {
		keyb_ = keyb;
	}

	public String getIpxnet() {
		return ipxnet_;
	}

	public void setIpxnet(String ipxnet) {
		ipxnet_ = ipxnet;
	}

	public Boolean isLoadfix() {
		return loadfix_;
	}

	public String getLoadfix() {
		return BooleanUtils.toStringTrueFalse(loadfix_);
	}

	public void setLoadfix(Boolean loadfix) {
		loadfix_ = loadfix;
	}

	public void setLoadfix(String loadfix) {
		if (StringUtils.isNotBlank(loadfix))
			loadfix_ = Boolean.valueOf(loadfix);
		else
			loadfix_ = null;
	}

	public Boolean isLoadhigh() {
		return loadhigh_;
	}

	public String getLoadhigh() {
		return BooleanUtils.toStringTrueFalse(loadhigh_);
	}

	public void setLoadhigh(Boolean loadhigh) {
		loadhigh_ = loadhigh;
	}

	public void setLoadhigh(String loadhigh) {
		if (StringUtils.isNotBlank(loadhigh))
			loadhigh_ = Boolean.valueOf(loadhigh);
		else
			loadhigh_ = null;
	}

	public Boolean isExit() {
		return exit_;
	}

	public String getExit() {
		return BooleanUtils.toStringTrueFalse(exit_);
	}

	public void setExit(Boolean exit) {
		exit_ = exit;
	}

	public void setExit(String exit) {
		if (StringUtils.isNotBlank(exit))
			exit_ = Boolean.valueOf(exit);
		else
			exit_ = null;
	}

	public Boolean getPause() {
		return pause_;
	}

	public void setPause(Boolean pause) {
		pause_ = pause;
	}

	public Boolean getBooterByDefault() {
		return booterByDefault_;
	}

	public void setBooterByDefault(Boolean booterByDefault) {
		booterByDefault_ = booterByDefault;
	}

	public int getLoadfixValue() {
		return loadfixValue_;
	}

	public String getLoadfixValueAsString() {
		return Objects.toString(loadfixValue_, null);
	}

	public void setLoadfixValue(String loadfixValue) {
		try {
			loadfixValue_ = NumberUtils.createInteger(loadfixValue);
		} catch (NumberFormatException e) {
			// ignore invalid values
		}
	}

	public String[] getCustomSections() {
		return customSections_;
	}

	public String getCustomSection(int index) {
		return customSections_[index];
	}

	public void setCustomSections(String[] customSections) {
		customSections_ = customSections;
	}

	public void setCustomSection(int index, String customSection) {
		customSections_[index] = customSection;
	}

	public String[] getSetPathsFromCustomSections() {
		for (String section: customSections_) {
			String[] commands = section.split(SystemUtils.EOLN);
			for (String command: commands) {
				if (command.toLowerCase().startsWith("set path=")) {
					return command.substring(9).trim().split(";");
				} else if (command.toLowerCase().startsWith("path=")) {
					return command.substring(5).trim().split(";");
				}
			}
		}
		return new String[0];
	}

	private void insertCustomSection(StringBuilder sb, int sectionNr) {
		if (StringUtils.isNotEmpty(customSections_[sectionNr])) {
			sb.append(CUSTOM_SECTION_MARKERS[sectionNr * 2 + 0]).append(SystemUtils.EOLN);
			sb.append(StringUtils.chomp(customSections_[sectionNr])).append(SystemUtils.EOLN);
			sb.append(CUSTOM_SECTION_MARKERS[sectionNr * 2 + 1]).append(SystemUtils.EOLN);
		}
	}

	public List<Mount> getMountingpoints() {
		return mountingpoints_;
	}

	public void setMountingpoints(List<Mount> mountingpoints) {
		mountingpoints_ = mountingpoints;
	}

	public long countImageMounts() {
		return mountingpoints_.stream().filter(ImageMount.class::isInstance).count();
	}

	public File[] findFirstImageMountCanonicalPath() {
		return mountingpoints_.stream().filter(ImageMount.class::isInstance).findFirst().map(x -> ((ImageMount)x).getCanonicalImgPaths()).orElse(new File[0]);
	}

	private boolean hasImageMountMatch(String file) {
		return mountingpoints_.stream().anyMatch(x -> x instanceof ImageMount && ((ImageMount)x).matchesImgPath(file));
	}

	private String findImageMatchByDrive(char driveLetter) {
		return mountingpoints_.stream().filter(x -> (x instanceof ImageMount) && (driveLetter == x.getDrive())).findFirst().map(x -> ((ImageMount)x).getImgPaths()[0].getPath()).orElse(null);
	}

	public void addMount(String mount, File baseDir) {
		try {
			Mount mnt = MountFactory.create(mount);
			mnt.setBaseDir(baseDir);
			mountingpoints_.add(mnt);
		} catch (InvalidMountstringException e) {
			System.err.println("Invalid mount command \"" + mount + "\"");
		}
	}

	public void addMount(String mount) {
		try {
			mountingpoints_.add(MountFactory.create(mount));
		} catch (InvalidMountstringException e) {
			System.err.println("Invalid mount command \"" + mount + "\"");
		}
	}

	public boolean isDos() {
		if (StringUtils.isBlank(main_) && StringUtils.isBlank(img1_))
			return BooleanUtils.isNotTrue(booterByDefault_);
		return StringUtils.isNotBlank(main_);
	}

	public boolean isBooter() {
		if (StringUtils.isBlank(main_) && StringUtils.isBlank(img1_))
			return BooleanUtils.isTrue(booterByDefault_);
		return StringUtils.isNotBlank(img1_);
	}

	public String getGameMain() {
		return isDos() ? main_: img1_;
	}

	public File getCanonicalGameDir() {
		File result = null;
		if (isDos() || isBooter()) {
			File file = new FileLocation(isDos() ? main_: img1_, FileLocationService.getInstance().dosrootRelative()).getCanonicalFile();
			File main = FilesUtils.determineMainFile(file);
			result = (main != null && !main.equals(file) && main.getParentFile() != null) ? main.getParentFile(): file.getParentFile();
		}
		return result;
	}

	public void setGameMain(String file) {
		if (isDos())
			main_ = file;
		else
			img1_ = file;
	}

	public boolean isIncomplete(List<Mount> combinedMountingpoints) {
		if (isDos())
			return !canBeReachedUsingMounts(false, main_, combinedMountingpoints);
		else if (isBooter())
			return !canBeReachedUsingMounts(true, img1_, combinedMountingpoints);
		else
			return true;
	}

	public String getDosFilename(String location, List<Mount> combinedMountingpoints) {
		return new DosPath(new FileLocation(location, FileLocationService.getInstance().dosrootRelative()), combinedMountingpoints).file_;
	}

	public boolean canBeReachedUsingMounts(boolean booter, String location, List<Mount> combinedMountingpoints) {
		return (booter && hasImageMountMatch(location)) || (new DosPath(new FileLocation(location, FileLocationService.getInstance().dosrootRelative()), combinedMountingpoints).drive_ != null);
	}
	
	private static boolean hasDrive(char drive, List<Mount> mountingpoints) {
		return (drive == 'z') || (drive == 'Z') // DOSBox internal drive 
				|| mountingpoints.stream().anyMatch(x -> x.matchesDrive(drive));
	}

	public String load(List<String> lines, List<Mount> dbMounts, File cwd) {
		StringBuilder warningsLog = new StringBuilder();

		File baseDir = (cwd == null) ? FileLocationService.getInstance().getDosroot(): cwd;

		char driveletter = '\0';
		String directory = StringUtils.EMPTY;
		String executable = StringUtils.EMPTY;
		String image1 = StringUtils.EMPTY;
		String image2 = StringUtils.EMPTY;
		String image3 = StringUtils.EMPTY;
		int exeIndex = -1;
		StringBuilder customEchos = new StringBuilder();
		List<String> leftOvers = new ArrayList<>();
		int customSection = -1;

		line: for (String orgLine: lines) {
			orgLine = orgLine.trim();

			for (int i = 0; i < SECTIONS; i++) {
				if (orgLine.startsWith(CUSTOM_SECTION_MARKERS[i * 2])) {
					customSection = i;
					continue line;
				}
			}
			if (customSection > -1) {
				if (orgLine.startsWith(CUSTOM_SECTION_MARKERS[customSection * 2 + 1]))
					customSection = -1;
				else
					customSections_[customSection] += orgLine + SystemUtils.EOLN;
				continue;
			}

			orgLine = StringUtils.removeEnd(StringUtils.removeStart(orgLine, "@"), ">null").trim();

			Matcher loadhighMatcher = LOADHIGH_PATRN.matcher(orgLine);
			Matcher loadfixMatcher = LOADFIX_PATRN.matcher(orgLine);
			Matcher bootMatcher = BOOT_PATRN.matcher(orgLine);

			if (loadhighMatcher.matches()) {
				loadhigh_ = true;
				orgLine = loadhighMatcher.group(1).trim();
			}

			if (loadfixMatcher.matches()) {
				if (BooleanUtils.isNotTrue(loadfix_)) {
					loadfixValue_ = DEFAULT_LOADFIX_VALUE;
					loadfix_ = true;
				}
				if (loadfixMatcher.group(1) != null) // -f
					continue;
				if (loadfixMatcher.group(2) != null) {
					try {
						loadfixValue_ = Integer.parseInt(loadfixMatcher.group(2).trim().substring(1));
					} catch (NumberFormatException e) {
						// use default value of 64
					}
				}
				if (loadfixMatcher.group(3) == null)
					continue;
				orgLine = loadfixMatcher.group(3).trim();
			}

			String line = orgLine.toLowerCase();

			if (line.startsWith("mount ") || line.startsWith("imgmount ")) {
				if (cwd == null)
					addMount(orgLine);
				else
					addMount(orgLine, baseDir);
			} else if ((line.endsWith(":") && line.length() == 2) || (line.endsWith(":\\") && line.length() == 3)) {
				if (driveletter != Character.toUpperCase(line.charAt(0))) {
					// drive letter change
					List<Mount> mountingpoints = dbMounts != null 
							? Stream.concat(dbMounts.stream(), mountingpoints_.stream()).toList()
							: mountingpoints_;
					if (hasDrive(line.charAt(0), mountingpoints)) {
						driveletter = Character.toUpperCase(line.charAt(0));
						directory = StringUtils.EMPTY;						
					} else {
						warningsLog.append("autoexec: invalid drive change\n");
					}
				}
			} else if (line.startsWith("cd\\")) {
				if (driveletter != '\0') {
					directory = FilenameUtils.separatorsToSystem(orgLine).substring(2);
				}
			} else if (line.startsWith("cd ")) {
				if (driveletter != '\0') {
					String add = FilenameUtils.separatorsToSystem(orgLine).substring(3);
					if (add.startsWith(File.separator))
						directory = add;
					else
						directory = new File(directory, add).getPath();
				}
			} else if (line.startsWith("cd.")) {
				if (driveletter != '\0') {
					String add = FilenameUtils.separatorsToSystem(orgLine).substring(2);
					if (!directory.equals(StringUtils.EMPTY) || !add.equals(".."))
						directory = new File(directory, add).getPath();
				}
			} else if (line.startsWith("keyb ") || line.startsWith("keyb.com ")) {
				keyb_ = orgLine.substring(line.indexOf(' ') + 1);
			} else if (line.startsWith("mixer ") || line.startsWith("mixer.com ")) {
				mixer_ = orgLine.substring(line.indexOf(' ') + 1);
			} else if (line.startsWith("ipxnet ") || line.startsWith("ipxnet.com ")) {
				ipxnet_ = orgLine.substring(line.indexOf(' ') + 1);
			} else if (line.startsWith("loadrom ") || line.startsWith("loadrom.com ")) {
				customSections_[1] += line;
			} else if (line.equals("pause")) {
				pause_ = true;
			} else if (line.startsWith("z:\\config.com")) {
				// just ignore
			} else if ((exeIndex = StringUtils.indexOfAny(line, FilesUtils.dosExecutables())) != -1) {
				executable = orgLine;

				// D-Fend Reloaded special cases
				if (executable.startsWith(DFR_4DOS_PREFIX))
					executable = executable.substring(DFR_4DOS_PREFIX.length());
				if (executable.startsWith(DFR_DOS32A_PREFIX))
					executable = executable.substring(DFR_DOS32A_PREFIX.length());

				// If there is a space BEFORE executable name, strip everything before it
				int spaceBeforeIndex = executable.lastIndexOf(' ', exeIndex);
				if (spaceBeforeIndex != -1) {
					executable = executable.substring(spaceBeforeIndex + 1);
				}
				// If there is a space AFTER executable name, take that part as parameters
				int spaceAfterIndex = executable.indexOf(' ');
				if (spaceAfterIndex != -1) {
					parameters_ = orgLine.substring(spaceBeforeIndex + spaceAfterIndex + 2);
					executable = executable.substring(0, spaceAfterIndex);
				}
			} else if (bootMatcher.matches()) {
				Matcher bootImgsMatcher = BOOTIMGS_PATRN.matcher(bootMatcher.group(1).trim());
				if (bootImgsMatcher.matches()) {
					if (bootImgsMatcher.group(1) != null)
						image1 = FilenameUtils.separatorsToSystem(StringUtils.strip(bootImgsMatcher.group(1), "\""));
					if (bootImgsMatcher.group(2) != null)
						image2 = FilenameUtils.separatorsToSystem(StringUtils.strip(bootImgsMatcher.group(2), "\""));
					if (bootImgsMatcher.group(3) != null)
						image3 = FilenameUtils.separatorsToSystem(StringUtils.strip(bootImgsMatcher.group(3), "\""));
				}
				if (bootMatcher.group(2) != null)
					imgDriveletter_ = bootMatcher.group(2).trim().toUpperCase();
				if (StringUtils.isEmpty(image1) && StringUtils.isNotEmpty(imgDriveletter_)) {
					String matchingImageMount = findImageMatchByDrive(imgDriveletter_.charAt(0));
					if (matchingImageMount != null) {
						image1 = matchingImageMount;
					} else {
						char driveNumber = (char)(imgDriveletter_.charAt(0) - 17);
						matchingImageMount = findImageMatchByDrive(driveNumber);
						if (matchingImageMount != null)
							image1 = matchingImageMount;
					}
				}
				if ((File.separator + "file").equals(image1)) {
					img1_ = "file"; // for template if . was unavailable
				}
			} else if (line.equals("exit") || line.startsWith("exit ")) {
				exit_ = true;
				break; // no further parsing of lines as DOSBox must have exited
			} else if (line.equals("echo off")) {
				// just ignore
			} else if (line.equals("echo") || line.startsWith("echo ") || line.startsWith("echo.")) {
				customEchos.append(orgLine).append(SystemUtils.EOLN);
			} else if (line.startsWith(":") || line.equals("cd") || line.equals("cls") || line.startsWith("cls ") || line.startsWith("cls\\") || line.equals("rem") || line.startsWith("rem ")
					|| line.startsWith("goto ") || line.startsWith("if errorlevel ")) {
				// just ignore
			} else if (StringUtils.isNotBlank(line)) {
				leftOvers.add(orgLine);
			}
		}

		if (StringUtils.isNotEmpty(customEchos) && !customEchos.toString().equalsIgnoreCase("echo." + SystemUtils.EOLN)) {
			customSections_[1] += "@echo off" + SystemUtils.EOLN + customEchos; // add echo commands to pre-launch custom section
			customSections_[1] += "pause" + SystemUtils.EOLN; // add pause statement to make it all readable
		}

		if (executable.equals(StringUtils.EMPTY) && image1.equals(StringUtils.EMPTY) && !leftOvers.isEmpty()) {
			// add all-but-the-last statement to pre-launch custom section
			for (int i = 0; i < leftOvers.size() - 1; i++)
				customSections_[1] += leftOvers.get(i).trim() + SystemUtils.EOLN;

			// the last statement should be the main game
			executable = leftOvers.get(leftOvers.size() - 1).trim();
			boolean isCalledBatch = executable.toLowerCase().startsWith("call ");
			if (isCalledBatch)
				executable = executable.substring(5);
			int spaceAfterIndex = executable.indexOf(' ');
			if (spaceAfterIndex != -1) {
				parameters_ = executable.substring(spaceAfterIndex + 1);
				executable = executable.substring(0, spaceAfterIndex);
			}
			executable += isCalledBatch ? FilesUtils.dosExecutables()[2]: FilesUtils.dosExecutables()[0];
		}

		if (executable.length() > 2 && executable.charAt(1) == ':') {
			driveletter = executable.charAt(0);
			executable = executable.substring(2);
		} else if (image1.length() > 2 && image1.charAt(1) == ':') {
			driveletter = image1.charAt(0);
			image1 = image1.substring(2);
			if (image2.length() > 2 && image2.charAt(1) == ':')
				image2 = image2.substring(2);
			if (image3.length() > 2 && image3.charAt(1) == ':')
				image3 = image3.substring(2);
		}

		List<Mount> mountingpointsForExecutables = dbMounts != null ? Stream.concat(dbMounts.stream(), mountingpoints_.stream()).toList(): mountingpoints_;
		for (Mount mount: mountingpointsForExecutables) {
			if (mount instanceof DirMount) {
				if (StringUtils.isNotBlank(executable) && mount.matchesDrive(driveletter))
					main_ = constructPath(((DirMount)mount).getPath(), directory, executable);
				if (StringUtils.isNotBlank(image1) && mount.matchesDrive(driveletter))
					img1_ = constructPath(((DirMount)mount).getPath(), directory, image1);
				if (StringUtils.isNotBlank(image2) && mount.matchesDrive(driveletter))
					img2_ = constructPath(((DirMount)mount).getPath(), directory, image2);
				if (StringUtils.isNotBlank(image3) && mount.matchesDrive(driveletter))
					img3_ = constructPath(((DirMount)mount).getPath(), directory, image3);
			} else if (mount instanceof ImageMount) {
				if (StringUtils.isNotBlank(executable) && mount.matchesDrive(driveletter))
					main_ = constructPath(((ImageMount)mount).getImgPaths()[0], directory, executable);
				if (StringUtils.isNotBlank(image1) && ((ImageMount)mount).matchesImgPath(image1))
					img1_ = image1;
				if (StringUtils.isNotBlank(image2) && ((ImageMount)mount).matchesImgPath(image2))
					img2_ = image2;
				if (StringUtils.isNotBlank(image3) && ((ImageMount)mount).matchesImgPath(image3))
					img3_ = image3;
			}
		}

		if (StringUtils.isBlank(main_) && StringUtils.isBlank(img1_)) {
			if (StringUtils.isNotBlank(image1)) {
				if (cwd == null)
					addMount("imgmount a \"" + image1 + "\"");
				else
					addMount("imgmount a \"" + image1 + "\"", baseDir);
				img1_ = image1;
			}
			if (StringUtils.isNotBlank(image2)) {
				if (cwd == null)
					addMount("imgmount b \"" + image2 + "\"");
				else
					addMount("imgmount b \"" + image2 + "\"", baseDir);
				img2_ = image2;
			}
			if (StringUtils.isNotBlank(image3)) {
				if (cwd == null)
					addMount("imgmount c \"" + image3 + "\"");
				else
					addMount("imgmount c \"" + image3 + "\"", baseDir);
				img3_ = image3;
			}
		}

		if (exit_ == null)
			exit_ = false;

		return warningsLog.toString();
	}

	private static String constructPath(File mountDir, String dosPath, String executable) {
		String normalizedPath = Paths.get(mountDir.getPath(), dosPath, executable).normalize().toString();
		FileLocation location = new FileLocation(normalizedPath, FileLocationService.getInstance().dosrootRelative());
		return location.getFile().getPath();
	}

	public void setGameMainPath(File mainParentFile) {
		if (isDos()) {
			main_ = new File(mainParentFile, new File(main_).getName()).getPath();
		} else if (isBooter()) {
			img1_ = new File(mainParentFile, new File(img1_).getName()).getPath();
			if (StringUtils.isNotEmpty(img2_))
				img2_ = new File(mainParentFile, new File(img2_).getName()).getPath();
			if (StringUtils.isNotEmpty(img3_))
				img3_ = new File(mainParentFile, new File(img3_).getName()).getPath();
		}
	}

	public void setBaseDir(File baseDir) {
		for (Mount mount: mountingpoints_) {
			mount.setBaseDir(baseDir);
		}
		if (isDos()) {
			setMain(FilesUtils.concat(baseDir, main_));
		} else if (isBooter()) {
			setImg1(FilesUtils.concat(baseDir, img1_));
			if (StringUtils.isNotEmpty(img2_))
				setImg2(FilesUtils.concat(baseDir, img2_));
			if (StringUtils.isNotEmpty(img3_))
				setImg3(FilesUtils.concat(baseDir, img3_));
		}
	}

	public void migrate(FileLocation fromPath, FileLocation toPath) {
		for (Mount mount: mountingpoints_) {
			mount.migrate(fromPath, toPath);
		}

		if (isDos()) {
			FileLocation main = new FileLocation(main_, FileLocationService.getInstance().dosrootRelative());
			String newMainString = FilesUtils.migrate(main, fromPath, toPath).getFile().getPath();
			if (canBeReachedUsingMounts(false, newMainString, mountingpoints_))
				main_ = newMainString;
		} else {
			FileLocation img1 = new FileLocation(img1_, FileLocationService.getInstance().dosrootRelative());
			String newImg1String = FilesUtils.migrate(img1, fromPath, toPath).getFile().getPath();
			if (canBeReachedUsingMounts(true, newImg1String, mountingpoints_))
				img1_ = newImg1String;

			if (StringUtils.isNotEmpty(img2_)) {
				FileLocation img2 = new FileLocation(img2_, FileLocationService.getInstance().dosrootRelative());
				String newImg2String = FilesUtils.migrate(img2, fromPath, toPath).getFile().getPath();
				if (canBeReachedUsingMounts(true, newImg2String, mountingpoints_))
					img2_ = newImg2String;
			}

			if (StringUtils.isNotEmpty(img3_)) {
				FileLocation img3 = new FileLocation(img3_, FileLocationService.getInstance().dosrootRelative());
				String newImg3String = FilesUtils.migrate(img3, fromPath, toPath).getFile().getPath();
				if (canBeReachedUsingMounts(true, newImg3String, mountingpoints_))
					img3_ = newImg3String;
			}
		}
	}

	public void removeUnequalValuesIn(Autoexec autoexec) {
		if (!StringUtils.equals(getMixer(), autoexec.getMixer()))
			setMixer(null);
		if (!StringUtils.equals(getKeyb(), autoexec.getKeyb()))
			setKeyb(null);
		if (!StringUtils.equals(getIpxnet(), autoexec.getIpxnet()))
			setIpxnet(null);
		if (!StringUtils.equals(getLoadfix(), autoexec.getLoadfix()))
			setLoadfix((String)null);
		if (!StringUtils.equals(getLoadhigh(), autoexec.getLoadhigh()))
			setLoadhigh((String)null);
		if (!StringUtils.equals(getExit(), autoexec.getExit()))
			setExit((String)null);
		if (!StringUtils.equals(getLoadfixValueAsString(), autoexec.getLoadfixValueAsString()))
			setLoadfixValue(null);
		for (int i = 0; i < Autoexec.SECTIONS; i++) {
			if (!StringUtils.equals(getCustomSection(i), autoexec.getCustomSection(i)))
				setCustomSection(i, null);
		}
	}

	public void removeUnnecessaryMounts(Autoexec autoexec) {
		mountingpoints_ = mountingpoints_.stream().filter(x -> !x.isUnmounted() && autoexec.mountingpoints_.stream().noneMatch(y -> y.getDrive() == x.getDrive())).collect(Collectors.toList());
	}

	@Override
	public String toString() {
		return toString(false, null);
	}

	public String toString(boolean prepareOnly, List<Mount> combinedMountingpoints) {
		StringBuilder result = new StringBuilder();

		List<Mount> mountingpointsForExecutables = combinedMountingpoints != null ? combinedMountingpoints: mountingpoints_;

		insertCustomSection(result, 0);
		if (StringUtils.isNotBlank(keyb_)) {
			result.append("keyb.com ").append(keyb_).append(SystemUtils.EOLN);
		}
		if (StringUtils.isNotBlank(ipxnet_)) {
			result.append("ipxnet.com ").append(ipxnet_).append(SystemUtils.EOLN);
		}
		for (Mount mount: mountingpoints_) {
			result.append(mount.toString()).append(SystemUtils.EOLN);
		}
		if (StringUtils.isNotBlank(mixer_)) {
			result.append("mixer.com ").append(mixer_).append(SystemUtils.EOLN);
		}

		if (StringUtils.isNotBlank(main_)) {

			FileLocation main = new FileLocation(main_, FileLocationService.getInstance().dosrootRelative());
			DosPath dosLocation = new DosPath(main, mountingpointsForExecutables);
			if (dosLocation.drive_ != null) {
				result.append(dosLocation.drive_ + SystemUtils.EOLN);
				result.append("cd \\" + dosLocation.directory_ + SystemUtils.EOLN);
				if (BooleanUtils.isTrue(loadfix_)) {
					result.append("loadfix -").append(loadfixValue_ != null && loadfixValue_ > 0 ? loadfixValue_: DEFAULT_LOADFIX_VALUE).append(SystemUtils.EOLN);
				}
				insertCustomSection(result, 1);
				if (!prepareOnly) {
					if (BooleanUtils.isTrue(loadhigh_)) {
						result.append("loadhigh ");
					}
					if (main_.toLowerCase().endsWith(FilesUtils.dosExecutables()[2])) {
						result.append("call ");
					}
					result.append(dosLocation.file_);
					if (StringUtils.isNotBlank(parameters_)) {
						result.append(' ').append(parameters_);
					}
					result.append(SystemUtils.EOLN);
					insertCustomSection(result, 2);
				}
			}

		} else if (StringUtils.isNotBlank(img1_)) {

			if (BooleanUtils.isTrue(loadfix_)) {
				result.append("loadfix -").append(loadfixValue_ > 0 ? loadfixValue_: DEFAULT_LOADFIX_VALUE).append(SystemUtils.EOLN);
			}
			insertCustomSection(result, 1);
			if (!prepareOnly) {
				result.append("boot");

				if (img1_.equals("file")) { // booter template
					result.append(" \\file");
				} else if (hasImageMountMatch(img1_)) {
					result.append(" \"").append(img1_).append("\"");
				} else {
					FileLocation img1 = new FileLocation(img1_, FileLocationService.getInstance().dosrootRelative());
					DosPath dosLocation = new DosPath(img1, mountingpointsForExecutables);
					if (dosLocation.drive_ != null) {
						String q = dosLocation.file_.indexOf(' ') == -1 ? StringUtils.EMPTY: "\"";
						result.append(" ").append(q).append(dosLocation.drive_).append('\\').append(dosLocation.directory_);
						if (StringUtils.isNotBlank(dosLocation.directory_)) {
							result.append('\\');
						}
						result.append(dosLocation.file_).append(q);
					}
				}
				if (hasImageMountMatch(img2_)) {
					result.append(" \"").append(img2_).append("\"");
				} else {
					if (StringUtils.isNotBlank(img2_)) {
						FileLocation img2 = new FileLocation(img2_, FileLocationService.getInstance().dosrootRelative());
						DosPath dosLocation = new DosPath(img2, mountingpointsForExecutables);
						if (dosLocation.drive_ != null) {
							String q = dosLocation.file_.indexOf(' ') == -1 ? StringUtils.EMPTY: "\"";
							result.append(" ").append(q).append(dosLocation.drive_).append('\\').append(dosLocation.directory_);
							if (StringUtils.isNotBlank(dosLocation.directory_)) {
								result.append('\\');
							}
							result.append(dosLocation.file_).append(q);
						}
					}
				}
				if (hasImageMountMatch(img3_)) {
					result.append(" \"").append(img3_).append("\"");
				} else {
					if (StringUtils.isNotBlank(img3_)) {
						FileLocation img3 = new FileLocation(img3_, FileLocationService.getInstance().dosrootRelative());
						DosPath dosLocation = new DosPath(img3, mountingpointsForExecutables);
						if (dosLocation.drive_ != null) {
							String q = dosLocation.file_.indexOf(' ') == -1 ? StringUtils.EMPTY: "\"";
							result.append(" ").append(q).append(dosLocation.drive_).append('\\').append(dosLocation.directory_);
							if (StringUtils.isNotBlank(dosLocation.directory_)) {
								result.append('\\');
							}
							result.append(dosLocation.file_).append(q);
						}
					}
				}
				if (StringUtils.isNotBlank(imgDriveletter_)) {
					result.append(" -l ").append(imgDriveletter_);
				}
				result.append(SystemUtils.EOLN);
				insertCustomSection(result, 2);
			}

		} else {

			// template
			insertCustomSection(result, 1);
			insertCustomSection(result, 2);

		}

		if (!prepareOnly) {
			if (BooleanUtils.isTrue(loadfix_)) {
				result.append("loadfix -f").append(SystemUtils.EOLN);
			}

			insertCustomSection(result, 3);

			if (BooleanUtils.isTrue(pause_)) {
				result.append("pause").append(SystemUtils.EOLN);
			}

			if (BooleanUtils.isTrue(exit_)) {
				result.append("exit").append(SystemUtils.EOLN);
			}
		}

		if (result.length() > 0) {
			result.insert(0, "[autoexec]" + SystemUtils.EOLN);
		}

		return result.toString();
	}
}
