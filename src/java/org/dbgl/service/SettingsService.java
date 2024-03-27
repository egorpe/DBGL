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
package org.dbgl.service;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.dbgl.gui.controls.ProfilesList.ProfilesListType;
import org.dbgl.model.FileLocation;
import org.dbgl.model.conf.Settings;
import org.dbgl.util.FilesUtils;
import org.dbgl.util.SystemUtils;
import org.dbgl.util.searchengine.MetropolisSearchEngine;
import org.dbgl.util.searchengine.MobyGamesSearchEngine;
import org.dbgl.util.searchengine.PouetSearchEngine;
import org.dbgl.util.searchengine.TheGamesDBSearchEngine;
import org.dbgl.util.searchengine.WebSearchEngine;


public class SettingsService {

	public static final List<String> SUPPORTED_DOSBOX_RELEASES = Arrays.asList("0.63", "0.65", "0.70", "0.71", "0.72", "0.73", "0.74", "0.74-2", "0.74-3");
	public static final String LATEST_SUPPORTED_DOSBOX_RELEASE = SUPPORTED_DOSBOX_RELEASES.get(SUPPORTED_DOSBOX_RELEASES.size() - 1);

	public static final List<WebSearchEngine> ALL_WEBSEARCH_ENGINES = Arrays.asList(MobyGamesSearchEngine.getInstance(), MetropolisSearchEngine.getInstance(), PouetSearchEngine.getInstance(),
		TheGamesDBSearchEngine.getInstance());

	private static final String SETTINGS_CONF = "settings.conf";

	private static final String SAMPLE_RATES = "8000 11025 16000 22050 32000 44100 48000 49716";
	private static final String BASE_ADDRS = "220 240 260 280 2a0 2c0 2e0 300";
	private static final String IRQS = "3 5 7 9 10 11 12";
	private static final String DMAS = "0 1 3 5 6 7";

	private Settings settings_;

	private SettingsService() {
		settings_ = new Settings();
		setDefaultSettings();

		String conf = (SystemUtils.USE_USER_HOME_DIR || !FilesUtils.isWritableDirectory(new File("."))) ? new File(SystemUtils.USER_DATA_DIR, SETTINGS_CONF).getPath(): SETTINGS_CONF;
		settings_.setFileLocation(new FileLocation(conf));

		try {
			String warnings = settings_.load(GenericTextService.getInstance());
			if (StringUtils.isNotEmpty(warnings))
				System.out.println(warnings);
		} catch (IOException e) {
			// if settings could not be read, use only the defaults
		}
	}

	private static class SettingsServiceHolder {
		private static final SettingsService instance_ = new SettingsService();
	}

	public static SettingsService getInstance() {
		return SettingsServiceHolder.instance_;
	}

	public void save() throws IOException {
		settings_.save();
	}

	public static List<WebSearchEngine> availableWebSearchEngines() {
		return ALL_WEBSEARCH_ENGINES.stream().filter(WebSearchEngine::available).toList();
	}

	public SimpleDateFormat dateTimeFormat() {
		return new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
	}

	public String getValue(String sectionTitle, String sectionItem) {
		return settings_.getValue(sectionTitle, sectionItem);
	}

	public void setValue(String sectionTitle, String sectionItem, String value) {
		settings_.setValue(sectionTitle, sectionItem, value);
	}

	public boolean getBooleanValue(String sectionTitle, String sectionItem) {
		return settings_.getBooleanValue(sectionTitle, sectionItem);
	}

	public void setBooleanValue(String sectionTitle, String sectionItem, boolean value) {
		settings_.setValue(sectionTitle, sectionItem, value);
	}

	public int getIntValue(String sectionTitle, String sectionItem) {
		return settings_.getIntValue(sectionTitle, sectionItem);
	}

	public void setIntValue(String sectionTitle, String sectionItem, int value) {
		settings_.setValue(sectionTitle, sectionItem, value);
	}

	public String getMultilineValue(String sectionTitle, String sectionItem, String delimiter) {
		return settings_.getMultilineValue(sectionTitle, sectionItem, delimiter);
	}

	public void setMultilineValue(String sectionTitle, String sectionItem, String values, String delimiter) {
		settings_.setMultilineValue(sectionTitle, sectionItem, values, delimiter);
	}

	public int[] getIntValues(String sectionTitle, String sectionItem) {
		return settings_.getIntValues(sectionTitle, sectionItem);
	}

	public void setIntValues(String sectionTitle, String sectionItem, int[] values) {
		settings_.setValues(sectionTitle, sectionItem, values);
	}

	public boolean[] getBooleanValues(String sectionTitle, String sectionItem) {
		return settings_.getBooleanValues(sectionTitle, sectionItem);
	}

	public void setBooleanValues(String sectionTitle, String sectionItem, boolean[] values) {
		settings_.setValues(sectionTitle, sectionItem, values);
	}

	public String[] getValues(String sectionTitle, String sectionItem) {
		return settings_.getValues(sectionTitle, sectionItem);
	}

	public String[] getProfileSectionItemNames() {
		return settings_.getItemNames("profile");
	}

	private void setDefaultSettings() {
		settings_.setValue("gui", "width", "904");
		settings_.setValue("gui", "height", "475");
		settings_.setValue("gui", "x", "10");
		settings_.setValue("gui", "y", "10");
		settings_.setValue("gui", "profiledialog_width", "768");
		settings_.setValue("gui", "profiledialog_height", "588");
		settings_.setValue("gui", "profileloaderdialog_width", "375");
		settings_.setValue("gui", "profileloaderdialog_height", "300");
		settings_.setValue("gui", "profiledeletedialog_width", "550");
		settings_.setValue("gui", "profiledeletedialog_height", "224");
		settings_.setValue("gui", "multiprofiledialog_width", "600");
		settings_.setValue("gui", "multiprofiledialog_height", "375");
		settings_.setValue("gui", "addgamewizard_width", "500");
		settings_.setValue("gui", "addgamewizard_height", "375");
		settings_.setValue("gui", "dosboxdialog_width", "600");
		settings_.setValue("gui", "dosboxdialog_height", "400");
		settings_.setValue("gui", "templatedialog_width", "768");
		settings_.setValue("gui", "templatedialog_height", "588");
		settings_.setValue("gui", "mountdialog_width", "640");
		settings_.setValue("gui", "mountdialog_height", "500");
		settings_.setValue("gui", "imgsizedialog_width", "330");
		settings_.setValue("gui", "imgsizedialog_height", "260");
		settings_.setValue("gui", "archivebrowser_width", "400");
		settings_.setValue("gui", "mobygamesbrowser_width", "650");
		settings_.setValue("gui", "mobygamesbrowser_height", "375");
		settings_.setValue("gui", "mixerdialog_width", "950");
		settings_.setValue("gui", "mixerdialog_height", "450");
		settings_.setValue("gui", "archivebrowser_height", "375");
		settings_.setValue("gui", "dfendimportdialog_width", "600");
		settings_.setValue("gui", "dfendimportdialog_height", "375");
		settings_.setValue("gui", "exportlistdialog_width", "550");
		settings_.setValue("gui", "exportlistdialog_height", "190");
		settings_.setValue("gui", "nativecommanddialog_width", "520");
		settings_.setValue("gui", "nativecommanddialog_height", "225");
		settings_.setValue("gui", "export_width", "550");
		settings_.setValue("gui", "export_height", "500");
		settings_.setValue("gui", "import_width", "654");
		settings_.setValue("gui", "import_height", "500");
		settings_.setValue("gui", "migratedialog_width", "600");
		settings_.setValue("gui", "migratedialog_height", "375");
		settings_.setValue("gui", "settingsdialog_width", "715");
		settings_.setValue("gui", "settingsdialog_height", "560");
		settings_.setValue("gui", "shareconfdialog_width", "560");
		settings_.setValue("gui", "shareconfdialog_height", "540");
		settings_.setValue("gui", "sharedconfbrowser_width", "860");
		settings_.setValue("gui", "sharedconfbrowser_height", "540");
		settings_.setValue("gui", "importdirdialog_width", "1680");
		settings_.setValue("gui", "importdirdialog_height", "800");
		settings_.setValue("gui", "importdir_column_1width", "70");
		settings_.setValue("gui", "importdir_column_2width", "400");
		settings_.setValue("gui", "importdir_column_3width", "260");
		settings_.setValue("gui", "importdir_column_4width", "165");
		settings_.setValue("gui", "importdir_column_5width", "165");
		settings_.setValue("gui", "importdir_column_6width", "240");
		settings_.setValue("gui", "importdir_column_7width", "60");
		settings_.setValue("gui", "importdir_column_8width", "105");

		settings_.setValue("gui", "log_width", "860");
		settings_.setValue("gui", "log_height", "540");
		settings_.setValue("gui", "filterdialog_width", "725");
		settings_.setValue("gui", "filterdialog_height", "540");
		settings_.setValue("gui", "filtertab", "0");
		settings_.setValue("gui", "maximized", false);
		settings_.setValue("gui", "column1width", "150");
		settings_.setValue("gui", "column2width", "48");
		settings_.setValue("gui", "column3width", "100");
		settings_.setValue("gui", "column4width", "100");
		settings_.setValue("gui", "column5width", "70");
		settings_.setValue("gui", "column6width", "40");
		settings_.setValue("gui", "column7width", "60");
		settings_.setValue("gui", "column8width", "60");
		settings_.setValue("gui", "column9width", "38");
		settings_.setValue("gui", "column10width", "40");
		settings_.setValue("gui", "column11width", "70");
		settings_.setValue("gui", "column12width", "70");
		settings_.setValue("gui", "column13width", "70");
		settings_.setValue("gui", "column14width", "70");
		settings_.setValue("gui", "column15width", "70");
		settings_.setValue("gui", "column16width", "70");
		settings_.setValue("gui", "column17width", "70");
		settings_.setValue("gui", "column18width", "70");
		settings_.setValue("gui", "column19width", "44");
		settings_.setValue("gui", "column20width", "44");
		settings_.setValue("gui", "column21width", "82");
		settings_.setValue("gui", "column22width", "70");
		settings_.setValue("gui", "column23width", "150");
		settings_.setValue("gui", "column24width", "150");
		settings_.setValue("gui", "column25width", "150");
		settings_.setValue("gui", "column26width", "150");
		settings_.setValue("gui", "column27width", "70");
		settings_.setValue("gui", "column28width", "70");
		settings_.setValue("gui", "column29width", "70");
		settings_.setValue("gui", "column30width", "70");
		settings_.setValue("gui", "column31width", "70");
		settings_.setValue("gui", "column32width", "70");
		settings_.setValue("gui", "column1visible", true);
		settings_.setValue("gui", "column2visible", true);
		settings_.setValue("gui", "column3visible", true);
		settings_.setValue("gui", "column4visible", true);
		settings_.setValue("gui", "column5visible", true);
		settings_.setValue("gui", "column6visible", true);
		settings_.setValue("gui", "column7visible", true);
		settings_.setValue("gui", "column8visible", true);
		settings_.setValue("gui", "column9visible", true);
		settings_.setValue("gui", "column10visible", false);
		settings_.setValue("gui", "column11visible", false);
		settings_.setValue("gui", "column12visible", false);
		settings_.setValue("gui", "column13visible", false);
		settings_.setValue("gui", "column14visible", false);
		settings_.setValue("gui", "column15visible", false);
		settings_.setValue("gui", "column16visible", false);
		settings_.setValue("gui", "column17visible", false);
		settings_.setValue("gui", "column18visible", false);
		settings_.setValue("gui", "column19visible", false);
		settings_.setValue("gui", "column20visible", false);
		settings_.setValue("gui", "column21visible", false);
		settings_.setValue("gui", "column22visible", false);
		settings_.setValue("gui", "column23visible", false);
		settings_.setValue("gui", "column24visible", false);
		settings_.setValue("gui", "column25visible", false);
		settings_.setValue("gui", "column26visible", false);
		settings_.setValue("gui", "column27visible", false);
		settings_.setValue("gui", "column28visible", false);
		settings_.setValue("gui", "column29visible", false);
		settings_.setValue("gui", "column30visible", false);
		settings_.setValue("gui", "column31visible", false);
		settings_.setValue("gui", "column32visible", false);
		settings_.setValue("gui", "column2_1width", "300");
		settings_.setValue("gui", "column2_2width", "250");
		settings_.setValue("gui", "column2_3width", "150");
		settings_.setValue("gui", "column2_4width", "68");
		settings_.setValue("gui", "column2_5width", "46");
		settings_.setValue("gui", "column2_6width", "150");
		settings_.setValue("gui", "column2_7width", "150");
		settings_.setValue("gui", "column2_8width", "150");
		settings_.setValue("gui", "column2_9width", "112");
		settings_.setValue("gui", "column3_1width", "500");
		settings_.setValue("gui", "column3_2width", "50");
		settings_.setValue("gui", "column3_3width", "68");
		settings_.setValue("gui", "column3_4width", "150");
		settings_.setValue("gui", "column3_5width", "150");
		settings_.setValue("gui", "column3_6width", "150");
		settings_.setValue("gui", "column3_7width", "112");
		settings_.setValue("gui", "sortcolumn", "0 8");
		settings_.setValue("gui", "sortascending", "true true");
		settings_.setValue("gui", "columnorder", "0 1 2 3 4 5 6 7 8");
		settings_.setValue("gui", "sashweights", "777 222");
		settings_.setValue("gui", "screenshotsheight", "100");
		settings_.setValue("gui", "screenshotscolumnheight", "50");
		settings_.setValue("gui", "screenshotscolumnstretch", false);
		settings_.setValue("gui", "screenshotscolumnkeepaspectratio", false);
		settings_.setValue("gui", "screenshotsvisible", true);
		settings_.setValue("gui", "screenshotsmaxzoompercentage", "300");
		settings_.setValue("gui", "screenshotsmaxwidth", "1920");
		settings_.setValue("gui", "screenshotsmaxheight", "1440");
		settings_.setValue("gui", "autosortonupdate", false);
		settings_.setValue("gui", "screenshotsfilename", true);
		settings_.setValue("gui", "buttondisplay", 0);
		settings_.setValue("gui", "custom1", "Custom1");
		settings_.setValue("gui", "custom2", "Custom2");
		settings_.setValue("gui", "custom3", "Custom3");
		settings_.setValue("gui", "custom4", "Custom4");
		settings_.setValue("gui", "custom5", "Custom5");
		settings_.setValue("gui", "custom6", "Custom6");
		settings_.setValue("gui", "custom7", "Custom7");
		settings_.setValue("gui", "custom8", "Custom8");
		settings_.setValue("gui", "custom9", "Custom9");
		settings_.setValue("gui", "custom10", "Custom10");
		settings_.setValue("gui", "custom11", "Custom11");
		settings_.setValue("gui", "custom12", "Custom12");
		settings_.setValue("gui", "custom13", "Custom13");
		settings_.setValue("gui", "custom14", "Custom14");
		settings_.setValue("gui", "searchengine", "mobygames");

		settings_.setValue("gui", "notesfont", "Courier 10 0"); // 0 = SWT.NORMAL
		settings_.setValue("gui", "notesvisible", true);
		settings_.setValue("gui", "theme", 0);
		settings_.setValue("gui", "linking", 0);

		settings_.setValue("gui", "viewstyle", ProfilesListType.TABLE.toString().toLowerCase());
		settings_.setValue("gui", "small_tile_width", 100);
		settings_.setValue("gui", "small_tile_height", 82);
		settings_.setValue("gui", "medium_tile_width", 132);
		settings_.setValue("gui", "medium_tile_height", 102);
		settings_.setValue("gui", "large_tile_width", 164);
		settings_.setValue("gui", "large_tile_height", 122);
		settings_.setValue("gui", "small_box_width", 75);
		settings_.setValue("gui", "small_box_height", 100);
		settings_.setValue("gui", "medium_box_width", 120);
		settings_.setValue("gui", "medium_box_height", 150);
		settings_.setValue("gui", "large_box_width", 150);
		settings_.setValue("gui", "large_box_height", 200);
		settings_.setValue("gui", "tile_title_trunc_pos", "end");

		settings_.setValue("gui", "gallerybackgroundcolor", "-1");

		settings_.setValue("profiledefaults", "confpath", 0);
		settings_.setValue("profiledefaults", "conffile", 0);
		settings_.setValue("profiledefaults", "capturespath", 1);

		settings_.setValue("dosbox", "hideconsole", false);

		settings_.setValue("communication", "port_enabled", SystemUtils.IS_WINDOWS);
		settings_.setValue("communication", "port", "4740");

		settings_.setValue("database", "connectionstring", "jdbc:hsqldb:file:./db/database");
		settings_.setValue("database", "username", "sa");
		settings_.setValue("database", "pasword", "");

		settings_.setValue("mobygames_database", "connectionstring", "jdbc:hsqldb:file:./db/mobygames;shutdown=true");
		settings_.setValue("mobygames_database", "username", "sa");
		settings_.setValue("mobygames_database", "pasword", "");

		settings_.setValue("directory", "data", ".");
		settings_.setValue("directory", "dosbox", ".");
		settings_.setValue("directory", "tmpinstall", "TMP_INST");
		settings_.setValue("directory", "orgimages", "ORGIMAGE");
		settings_.setValue("directory", "parentscanberelative", false);

		settings_.setValue("locale", "language", "en");
		settings_.setValue("locale", "country", "");
		settings_.setValue("locale", "variant", "");

		settings_.setValue("log", "enabled", true);
		
		settings_.setValue("websearchengine", "search_term_filter", "freeware (freeware) shareware (shareware)");

		settings_.setValue("mobygames", "platform_filter", "dos pc<space>booter");
		settings_.setValue("mobygames", "platform_filter_ids", "2 4");
		settings_.setValue("mobygames", "set_title", true);
		settings_.setValue("mobygames", "set_developer", true);
		settings_.setValue("mobygames", "set_publisher", true);
		settings_.setValue("mobygames", "set_year", true);
		settings_.setValue("mobygames", "set_genre", true);
		settings_.setValue("mobygames", "set_link", true);
		settings_.setValue("mobygames", "set_description", true);
		settings_.setValue("mobygames", "set_rank", true);
		settings_.setValue("mobygames", "choose_coverart", false);
		settings_.setValue("mobygames", "choose_screenshot", false);
		settings_.setValue("mobygames", "force_all_regions_coverart", false);
		settings_.setValue("mobygames", "multi_max_coverart", 0);
		settings_.setValue("mobygames", "multi_max_screenshot", 0);
		settings_.setValue("mobygames", "image_width", 128);
		settings_.setValue("mobygames", "image_height", 80);
		settings_.setValue("mobygames", "image_columns", 2);
		settings_.setValue("mobygames", "api_key", "moby_cLvVy0ShCSgvuv2175i6KhOO6VU");

		settings_.setValue("metropolis", "platform_filter", "dos pc<space>booter");
		settings_.setValue("metropolis", "region", "United States");
		settings_.setValue("metropolis", "set_title", true);
		settings_.setValue("metropolis", "set_developer", true);
		settings_.setValue("metropolis", "set_publisher", true);
		settings_.setValue("metropolis", "set_year", true);
		settings_.setValue("metropolis", "set_genre", true);
		settings_.setValue("metropolis", "set_link", true);
		settings_.setValue("metropolis", "set_description", true);
		settings_.setValue("metropolis", "set_rank", true);
		settings_.setValue("metropolis", "choose_coverart", false);
		settings_.setValue("metropolis", "choose_screenshot", false);
		settings_.setValue("metropolis", "force_all_regions_coverart", false);
		settings_.setValue("metropolis", "multi_max_coverart", 0);
		settings_.setValue("metropolis", "multi_max_screenshot", 0);
		settings_.setValue("metropolis", "image_width", 128);
		settings_.setValue("metropolis", "image_height", 80);
		settings_.setValue("metropolis", "image_columns", 2);

		settings_.setValue("pouet", "platform_filter", "ms-dos ms-dos/gus");
		settings_.setValue("pouet", "set_title", true);
		settings_.setValue("pouet", "set_developer", true);
		settings_.setValue("pouet", "set_year", true);
		settings_.setValue("pouet", "set_genre", true);
		settings_.setValue("pouet", "set_link", true);
		settings_.setValue("pouet", "set_rank", true);
		settings_.setValue("pouet", "choose_coverart", false);
		settings_.setValue("pouet", "choose_screenshot", false);
		settings_.setValue("pouet", "multi_max_coverart", 0);
		settings_.setValue("pouet", "multi_max_screenshot", 0);

		settings_.setValue("thegamesdb", "platform_filter", "pc");
		settings_.setValue("thegamesdb", "set_title", true);
		settings_.setValue("thegamesdb", "set_developer", true);
		settings_.setValue("thegamesdb", "set_publisher", true);
		settings_.setValue("thegamesdb", "set_year", true);
		settings_.setValue("thegamesdb", "set_genre", true);
		settings_.setValue("thegamesdb", "set_link", true);
		settings_.setValue("thegamesdb", "set_description", true);
		settings_.setValue("thegamesdb", "set_rank", true);
		settings_.setValue("thegamesdb", "choose_coverart", false);
		settings_.setValue("thegamesdb", "choose_screenshot", false);
		settings_.setValue("thegamesdb", "multi_max_coverart", 0);
		settings_.setValue("thegamesdb", "multi_max_screenshot", 0);

		settings_.setValue("environment", "use", false);
		settings_.setValue("environment", "value", "");

		settings_.setValue("profile", "priority_active", "lowest lower normal higher highest");
		settings_.setValue("profile", "priority_inactive", "lowest lower normal higher highest pause");
		settings_.setValue("profile", "output", "ddraw overlay opengl openglnb surface");
		settings_.setValue("profile", "frameskip", "0 1 2 3 4 5 6 7 8 9 10");
		settings_.setValue("profile", "scaler", "none normal2x normal3x advmame2x advmame3x advinterp2x advinterp3x hq2x hq3x 2xsai super2xsai supereagle tv2x tv3x rgb2x rgb3x scan2x scan3x");
		settings_.setValue("profile", "fullresolution", "original desktop 0x0 320x200 640x480 800x600 1024x768 1280x768 1280x960 1280x1024");
		settings_.setValue("profile", "windowresolution", "original 320x200 640x480 800x600 1024x768 1280x768 1280x960 1280x1024");
		settings_.setValue("profile", "glshader", "none advinterp2x advinterp3x advmame2x advmame3x rgb2x rgb3x scan2x scan3x tv2x tv3x sharp");
		settings_.setValue("profile", "machine", "cga hercules pcjr tandy vga");
		settings_.setValue("profile", "machine073", "cga hercules pcjr tandy ega vgaonly svga_s3 svga_et3000 svga_et4000 svga_paradise vesa_nolfb vesa_oldvbe");
		settings_.setValue("profile", "cputype", "auto 386 386_slow 486_slow pentium_slow 386_prefetch");
		settings_.setValue("profile", "core", "dynamic full normal simple auto");
		settings_.setValue("profile", "cycles",
			"350 500 750 1000 2000 3000 4000 5000 7500 10000 12500 15000 17500 20000 25000 30000 32500 35000 40000 45000 50000 55000 60000 auto max<space>50% max<space>80% max<space>90% max");
		settings_.setValue("profile", "cycles_up", "10 20 50 100 500 1000 2000 5000 10000");
		settings_.setValue("profile", "cycles_down", "10 20 50 100 500 1000 2000 5000 10000");
		settings_.setValue("profile", "memsize", "0 1 2 4 8 16 32 63");
		settings_.setValue("profile", "ems", "false emsboard emm386 true");
		settings_.setValue("profile", "umb", "false true max");
		settings_.setValue("profile", "loadfix_value", "1 63 64 127");
		settings_.setValue("profile", "rate", SAMPLE_RATES);
		settings_.setValue("profile", "blocksize", "256 512 1024 2048 4096 8192");
		settings_.setValue("profile", "prebuffer", "10 20 25");
		settings_.setValue("profile", "mpu401", "none intelligent uart");
		settings_.setValue("profile", "device", "alsa default coreaudio coremidi none oss win32");
		settings_.setValue("profile", "sbtype", "none gb sb1 sb2 sbpro1 sbpro2 sb16");
		settings_.setValue("profile", "oplrate", SAMPLE_RATES);
		settings_.setValue("profile", "oplmode", "auto cms opl2 dualopl2 opl3 opl3gold none");
		settings_.setValue("profile", "oplemu", "default compat fast mame");
		settings_.setValue("profile", "sbbase", BASE_ADDRS);
		settings_.setValue("profile", "irq", IRQS);
		settings_.setValue("profile", "dma", DMAS);
		settings_.setValue("profile", "hdma", DMAS);
		settings_.setValue("profile", "gusrate", SAMPLE_RATES);
		settings_.setValue("profile", "gusbase", BASE_ADDRS);
		settings_.setValue("profile", "irq1", IRQS);
		settings_.setValue("profile", "irq2", IRQS);
		settings_.setValue("profile", "dma1", DMAS);
		settings_.setValue("profile", "dma2", DMAS);
		settings_.setValue("profile", "pcrate", SAMPLE_RATES);
		settings_.setValue("profile", "tandy", "auto off on");
		settings_.setValue("profile", "tandyrate", SAMPLE_RATES);
		settings_.setValue("profile", "sensitivity", "10 20 30 40 50 60 70 80 90 100 100,-50 125 150 175 200 250 300 350 400 450 500 550 600 700 800 900 1000");
		settings_.setValue("profile", "joysticktype", "auto none 2axis 4axis 4axis_2 ch fcs");
		settings_.setValue("profile", "mount_type", "cdrom dir floppy overlay");
		settings_.setValue("profile", "imgmount_type", "iso floppy hdd");
		settings_.setValue("profile", "imgmount_fs", "iso fat none");
		settings_.setValue("profile", "zipmount_type", "cdrom dir floppy");
		settings_.setValue("profile", "freesize", "1 10 100 200 500 1000");
		settings_.setValue("profile", "lowlevelcd_type", "aspi ioctl ioctl_dx ioctl_dio ioctl_mci noioctl");
		settings_.setValue("profile", "keyboardlayout",
			"auto none ba234 be120 bg241 bg442 bl463 br274 br275 by463 ca58 ca445 cf58 cf445 cf501 cz243 de129 de453 dk159 dv103 ee454 el220 el319 el459 es172 es173 et454 fi153 fo fr120 fr189 "
					+ "gk220 gk319 gk459 gr129 gr453 hr234 hu208 is161 is197 is458 it141 it142 la171 lt210 lt211 lt212 lt221 lt456 lh103 mk449 ml47 mt47 nl143 no155 ph pl214 pl457 po163 rh103 "
					+ "ro333 ro446 ru441 ru443 sd150 sf150 sg150 si234 sk245 sp172 sp173 sq448 sq452 sr118 sr450 su153 sv153 tm tr179 tr440 ua465 uk166 uk168 ur465 us103 ux103 yc118 yc450 yu234");

		settings_.setValue("profile", "pixelshader",
			"none 2xSaI.fx 2xSaI_sRGB.fx 2xSaL.fx 2xSaL_Ls.fx 2xSaL2xAA.fx " + "2xSaLAA.fx 4xSaL.fx 4xSoft.fx 4xSoft_PS3.0.fx AdvancedAA.fx bilinear.fx Cartoon.fx ColorSketch.fx CRT.D3D.fx "
					+ "CRT.D3D.br.fx CRT-simple.D3D.fx CRT-simple.D3D.br.fx DotnBloom.D3D.fx GS2x.fx GS2xFilter.fx Gs2xLS.fx Gs2xSmartFilter.fx GS2xSuper.fx GS2xTwo.fx GS4x.fx GS4xColorScale.fx "
					+ "GS4xFilter.fx GS4xHqFilter.fx GS4xScale.fx GS4xSoft.fx HQ2x.fx Lanczos.fx Lanczos12.fx Lanczos16.fx Matrix.fx MCAmber.fx MCGreen.fx MCHerc.fx MCOrange.fx none.fx point.fx "
					+ "scale2x.fx scale2x_ps14.fx Scale2xPlus.fx " + "Scale4x.fx SimpleAA.fx Sketch.fx Super2xSaI.fx SuperEagle.fx Tv.fx");
		settings_.setValue("profile", "overscan", "1 2 3 4 5 6 7 8 9 10");
		settings_.setValue("profile", "vsyncmode", "off on force host");
		settings_.setValue("profile", "lfbglide", "full full_noaux read read_noaux write write_noaux none");
		settings_.setValue("profile", "vmemsize", "-1 0 1 2 4 8");
		settings_.setValue("profile", "glide", "false true emu");
		settings_.setValue("profile", "voodoo", "false software opengl auto");
		settings_.setValue("profile", "voodoomem", "standard max");
		settings_.setValue("profile", "memalias", "0 24 26");
		settings_.setValue("profile", "hardwaresbbase", "210 220 230 240 250 260 280");
		settings_.setValue("profile", "mt32dac", "0 1 2 3 auto");
		settings_.setValue("profile", "mt32reverbmode", "0 1 2 3 auto");
		settings_.setValue("profile", "mt32reverbtime", "0 1 2 3 4 5 6 7");
		settings_.setValue("profile", "mt32reverblevel", "0 1 2 3 4 5 6 7");
		settings_.setValue("profile", "mt32analog", "0 1 2 3");
		settings_.setValue("profile", "fluidsynthdriver", "pulseaudio alsa oss coreaudio dsound portaudio sndman jack file default");
		settings_.setValue("profile", "fluidsynthsamplerate", SAMPLE_RATES);
		settings_.setValue("profile", "imfcbase", "2a20 2a30");
		settings_.setValue("profile", "imfcirq", "2 3 4 5 6 7");
		settings_.setValue("profile", "imfcfilter", "on off");
		settings_.setValue("profile", "ps1rate", SAMPLE_RATES);
		settings_.setValue("profile", "innovarate", SAMPLE_RATES);
		settings_.setValue("profile", "innovabase", BASE_ADDRS);
		settings_.setValue("profile", "innovaquality", "0 1 2 3");
		settings_.setValue("profile", "auxdevice", "none 2button 3button intellimouse intellimouse45");
		settings_.setValue("profile", "printeroutput", "png ps bmp printer");
		settings_.setValue("profile", "uniquemapperfile", "%1$d.map");

		settings_.setValue("addgamewizard", "requiresinstallation", false);
		settings_.setValue("addgamewizard", "consultsearchengine", true);
		settings_.setValue("addgamewizard", "consultdbconfws", true);
		settings_.setValue("addgamewizard", "useuniquemapperfile", false);

		settings_.setValue("exportwizard", "gamedata", false);

		settings_.setValue("confsharing", "endpoint", "https://share.dbgl.org/DBConfWS/apiv1/");
	}
}
