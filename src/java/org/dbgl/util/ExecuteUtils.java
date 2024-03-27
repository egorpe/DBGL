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
package org.dbgl.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.swing.filechooser.FileSystemView;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.dbgl.gui.controls.Mess_;
import org.dbgl.model.FileLocation;
import org.dbgl.model.NativeCommand;
import org.dbgl.model.aggregate.DosboxVersion;
import org.dbgl.model.aggregate.Profile;
import org.dbgl.model.aggregate.Template;
import org.dbgl.model.entity.TemplateProfileBase;
import org.dbgl.model.factory.ProfileFactory;
import org.dbgl.model.factory.TemplateFactory;
import org.dbgl.service.FileLocationService;
import org.dbgl.service.SettingsService;
import org.dbgl.service.TextService;
import org.eclipse.swt.widgets.Display;


public class ExecuteUtils {

	public enum ProfileRunMode {
		NORMAL, SETUP, ALT1, ALT2, INSTALLER
	}

	private static class ConfigurableExecutor extends Thread {

		private final TemplateProfileBase templateOrProfile_;
		private final DosboxVersion dosboxVersion_;
		private final File conf_;
		private final Display display_;

		public ConfigurableExecutor(TemplateProfileBase templateOrProfile, DosboxVersion dosboxVersion, File conf, Display display) {
			templateOrProfile_ = templateOrProfile;
			dosboxVersion_ = dosboxVersion;
			conf_ = conf;
			display_ = display;
		}

		@Override
		public void run() {
			try {
				for (NativeCommand cmd: templateOrProfile_.getNativeCommands()) {
					if (cmd.isDosboxCommand()) {
						doRunDosbox(dosboxVersion_, new String[] {"-conf", conf_.getPath()}, false, dosboxVersion_.getCwd(), true);
					} else {
						execute(cmd.getCanonicalCommandAndParameters(), cmd.getCanonicalCwd(), cmd.isWaitFor());
					}
				}
			} catch (IOException e) {
				if (!display_.isDisposed()) {
					display_.syncExec(() -> Mess_.on(display_.getActiveShell()).exception(e).warning());
				}
			}
		}
	}

	private static class StreamGobbler extends Thread {

		private final InputStream istream_;
		private final String logType_;

		public StreamGobbler(InputStream istream, String logType) {
			istream_ = istream;
			logType_ = logType;
		}

		@Override
		public void run() {
			try (BufferedReader bReader = new BufferedReader(new InputStreamReader(istream_))) {
				String line = null;
				while ((line = bReader.readLine()) != null)
					System.out.println(logType_ + "> " + line);

			} catch (IOException ioe) {
				ioe.printStackTrace();
			} finally {
				try {
					istream_.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static void doRunDosbox(DosboxVersion dbversion) throws IOException {
		doRunDosbox(dbversion, new String[] {}, true, dbversion.getCwd(), false);
	}

	public static void doCreateDosboxConf(DosboxVersion dbversion) throws IOException {
		String quote = SystemUtils.IS_WINDOWS ? "'": "\"";
		if (dbversion.getVersionAsInt() <= 650)
			quote = StringUtils.EMPTY;
		doRunDosbox(dbversion, new String[] {"-c", "config -writeconf " + quote + dbversion.getConfigurationCanonicalFile() + quote, "-c", "exit"}, false, dbversion.getCwd(), true);
	}

	public static void doRunTemplate(Template template, Display display) throws IOException {
		Template preparedTemplate = TemplateFactory.createCopy(template);
		preparedTemplate.loadConfigurationData(TextService.getInstance(), template.getConfigurationString(), template.getConfigurationCanonicalFile());
		preparedTemplate.getConfiguration().setFileLocation(FileLocationService.getInstance().getTmpConfLocation());
		preparedTemplate.saveConfiguration(true);
		new ConfigurableExecutor(preparedTemplate, preparedTemplate.getDosboxVersion(), preparedTemplate.getConfigurationCanonicalFile(), display).start();
	}

	public static void doRunProfile(ProfileRunMode mode, Profile prof, boolean prepareOnly, Display display) throws IOException {
		doRunProfile(mode, prof, prof.getDosboxVersion(), prepareOnly, display);
	}

	public static void doRunProfile(ProfileRunMode mode, Profile prof, DosboxVersion dosboxVersion, boolean prepareOnly, Display display) throws IOException {
		Profile preparedProfile = prof;

		if (mode != ProfileRunMode.NORMAL || prepareOnly) {
			preparedProfile = ProfileFactory.createCopy(prof);
			preparedProfile.loadConfigurationData(TextService.getInstance(), prof.getConfigurationString(),
				mode == ProfileRunMode.INSTALLER ? new File(StringUtils.EMPTY): prof.getConfigurationCanonicalFile());
			switch (mode) {
				case SETUP:
					preparedProfile.setAutoexecSettings(preparedProfile.getSetupString(), preparedProfile.getSetupParams());
					break;
				case ALT1:
					preparedProfile.setAutoexecSettings(preparedProfile.getAltExeStrings()[0], preparedProfile.getAltExeParams()[0]);
					break;
				case ALT2:
					preparedProfile.setAutoexecSettings(preparedProfile.getAltExeStrings()[1], preparedProfile.getAltExeParams()[1]);
					break;
				case INSTALLER:
					preparedProfile.setAutoexecSettings(true, true);
					break;
				default:
			}
			preparedProfile.getConfiguration().setFileLocation(FileLocationService.getInstance().getTmpConfLocation());
			preparedProfile.saveConfiguration(prepareOnly);
		}

		if (mode == ProfileRunMode.INSTALLER)
			doRunDosbox(dosboxVersion, new String[] {"-conf", preparedProfile.getConfigurationCanonicalFile().getPath()}, false, dosboxVersion.getCwd(), true);
		else
			new ConfigurableExecutor(preparedProfile, dosboxVersion, preparedProfile.getConfigurationCanonicalFile(), display).start();
	}

	public static void createShortcut(Profile profile) throws IOException {
		DosboxVersion dbversion = profile.getDosboxVersion();
		String strictFilename = profile.getTitle().replaceAll("[\\/:*?\"<>|]", " ").trim();
		StringBuilder params = new StringBuilder(128);
		if (dbversion.isMultiConfig()) {
			params.append("-conf \"\"").append(dbversion.getConfigurationCanonicalFile()).append("\"\" ");
		}
		params.append("-conf \"\"").append(profile.getConfigurationCanonicalFile()).append("\"\"");
		if (SettingsService.getInstance().getBooleanValue("dosbox", "hideconsole")) {
			params.append(" -noconsole");
		}

		if (SystemUtils.IS_WINDOWS) {

			File desktopDir = FileSystemView.getFileSystemView().getHomeDirectory();
			File lnkFile = new File(desktopDir, strictFilename + ".lnk");
			File vbsFile = new FileLocation("shortcut.vbs", FileLocationService.getInstance().dataRelative()).getCanonicalFile();
			try (BufferedWriter vbsWriter = new BufferedWriter(new FileWriter(vbsFile))) {
				vbsWriter.write("Set oWS = WScript.CreateObject(\"WScript.Shell\")" + SystemUtils.EOLN);
				vbsWriter.write("Set oLink = oWS.CreateShortcut(\"" + lnkFile.getCanonicalPath() + "\")" + SystemUtils.EOLN);
				vbsWriter.write("oLink.TargetPath = \"" + dbversion.getCanonicalExecutable() + "\"" + SystemUtils.EOLN);
				vbsWriter.write("oLink.Arguments = \"" + params.toString() + "\"" + SystemUtils.EOLN);
				vbsWriter.write("oLink.Description = \"" + TextService.getInstance().get("general.shortcut.title", new Object[] {strictFilename}) + "\"" + SystemUtils.EOLN);
				vbsWriter.write("oLink.WorkingDirectory = \"" + FileLocationService.getInstance().getDosroot() + "\"" + SystemUtils.EOLN);
				vbsWriter.write("oLink.Save" + SystemUtils.EOLN);
			}
			Process proc = Runtime.getRuntime().exec(new String[] {"CSCRIPT", vbsFile.getPath()}, null, vbsFile.getParentFile());
			try (InputStream err = proc.getErrorStream(); InputStream out = proc.getInputStream()) {
				StreamGobbler errorGobbler = new StreamGobbler(err, "CSCRIPT stderr");
				StreamGobbler outputGobbler = new StreamGobbler(out, "CSCRIPT stdout");
				outputGobbler.start();
				errorGobbler.start();
				proc.waitFor();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			FilesUtils.removeFile(vbsFile);

		} else if (SystemUtils.IS_LINUX) {

			File desktopDir = determineLinuxDesktopDir();
			if (desktopDir != null) {
				File desktopFile = new File(desktopDir, strictFilename + ".desktop");
				try (BufferedWriter desktopWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(desktopFile), StandardCharsets.UTF_8))) {
					desktopWriter.write("[Desktop Entry]" + SystemUtils.EOLN);
					desktopWriter.write("Version=1.0" + SystemUtils.EOLN);
					desktopWriter.write("Type=Application" + SystemUtils.EOLN);
					desktopWriter.write("Name=" + strictFilename + SystemUtils.EOLN);
					desktopWriter.write("Comment=" + TextService.getInstance().get("general.shortcut.title", new Object[] {strictFilename}) + SystemUtils.EOLN);
					desktopWriter.write("Icon=" + new File(dbversion.getExecutable().getParent(), "dosbox.ico").getPath() + SystemUtils.EOLN);
					desktopWriter.write("TryExec=" + dbversion.getExecutable().getPath() + SystemUtils.EOLN);
					desktopWriter.write("Exec=" + dbversion.getExecutable().getPath() + " " + StringUtils.replace(params.toString(), "\"\"", "\"") + SystemUtils.EOLN);
					desktopWriter.write("Path=" + FileLocationService.getInstance().getDosroot() + SystemUtils.EOLN);
				}
				if (!desktopFile.setExecutable(true)) {
					throw new IOException(TextService.getInstance().get("general.error.savefile", new Object[] {desktopDir.getPath()}));
				}
			}

		}
	}

	private static File determineLinuxDesktopDir() {
		try {
			Process process = Runtime.getRuntime().exec(new String[] {"xdg-user-dir", "DESKTOP"});
			try (InputStream is = process.getInputStream()) {
				process.waitFor();
				String xdgDesktopDir = IOUtils.toString(is, StandardCharsets.UTF_8);
				if (xdgDesktopDir != null) {
					xdgDesktopDir = xdgDesktopDir.replace(SystemUtils.EOLN, "");
					if (!xdgDesktopDir.isEmpty()) {
						File desktopDirFile = new File(xdgDesktopDir);
						if (desktopDirFile.exists() && desktopDirFile.isDirectory())
							return desktopDirFile;
					}
				}
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (Exception e) {
			e.printStackTrace();
		}

		File defaultDesktopDir = new File(SystemUtils.USER_HOME, "Desktop");
		if (defaultDesktopDir.exists() && defaultDesktopDir.isDirectory())
			return defaultDesktopDir;

		return null;
	}

	private static void doRunDosbox(DosboxVersion dbversion, String[] parameters, boolean forceDBConf, File cwd, boolean waitFor) throws IOException {
		List<String> commandItems = new ArrayList<>();
		if (dbversion.isUsingCurses()) {
			if (SystemUtils.IS_WINDOWS) {
				commandItems.add("rundll32");
				commandItems.add("SHELL32.DLL,ShellExec_RunDLL");
			} else if (SystemUtils.IS_LINUX) {
				commandItems.add("xterm");
				commandItems.add("-e");
			}
		}
		commandItems.add(dbversion.getCanonicalExecutable().getPath());
		if ((dbversion.isMultiConfig() && FilesUtils.isReadableFile(dbversion.getConfiguration().getFile())) || forceDBConf) {
			// selected default dosbox config file
			commandItems.add("-conf");
			commandItems.add(dbversion.getConfigurationCanonicalFile().getPath());
		}
		commandItems.addAll(Arrays.asList(parameters));
		if (dbversion.getExecutableParameters().length() > 0) {
			Collections.addAll(commandItems, StringUtils.split(dbversion.getExecutableParameters(), ' '));
		}
		if (SettingsService.getInstance().getBooleanValue("dosbox", "hideconsole"))
			commandItems.add("-noconsole");
		if (SystemUtils.IS_OSX && dbversion.isUsingCurses()) {
			String command = StringUtils.join(commandItems, ' ');
			commandItems.clear();
			commandItems.add("osascript");
			commandItems.add("-e");
			commandItems.add("tell application \"Terminal\" to do script \"cd '" + cwd + "'; " + command + "; exit;\"");
		}

		try {
			execute(commandItems, cwd, waitFor);
		} catch (IOException e) {
			e.printStackTrace();
			throw new IOException(TextService.getInstance().get("general.error.startdosbox", new Object[] {StringUtils.join(commandItems, ' ')}));
		}
	}

	private static void execute(List<String> execCommands, File cwd, boolean waitFor) throws IOException {
		System.out.print(StringUtils.join(execCommands, ' '));
		ProcessBuilder pb = new ProcessBuilder(execCommands.toArray(new String[execCommands.size()]));
		pb.directory(cwd);
		Map<String, String> environment = pb.environment();
		Map<String, String> env = SettingsService.getInstance().getBooleanValue("environment", "use")
				? StringRelatedUtils.stringArrayToMap(SettingsService.getInstance().getValues("environment", "value"))
				: null;
		if (env != null) {
			environment.putAll(env);
			System.out.print(env);
		}
		System.out.println();
		Process proc = pb.start();
		try {
			StreamGobbler errorGobbler = new StreamGobbler(proc.getErrorStream(), "DOSBox stderr");
			StreamGobbler outputGobbler = new StreamGobbler(proc.getInputStream(), "DOSBox stdout");
			outputGobbler.start();
			errorGobbler.start();
			if (waitFor)
				proc.waitFor();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
