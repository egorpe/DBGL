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
package org.dbgl.model.aggregate;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.dbgl.model.FileLocation;
import org.dbgl.model.GenericStats;
import org.dbgl.model.ICanBeDefault;
import org.dbgl.model.conf.GenerationAwareConfiguration.Generation;
import org.dbgl.model.entity.Configurable;
import org.dbgl.service.FileLocationService;
import org.dbgl.service.ITextService;
import org.dbgl.service.TextService;


public class DosboxVersion extends Configurable implements ICanBeDefault, Comparable<DosboxVersion> {

	private String version_, executableParameters_;
	private boolean default_, multiConfig_, usingCurses_;
	private Map<String, String> dynamicOptions_;
	private FileLocation path_, exe_;
	private GenericStats stats_;

	public DosboxVersion() {
		super();
	}

	public String getVersion() {
		return version_;
	}

	public void setVersion(String version) {
		version_ = version;
	}

	public int getVersionAsInt() {
		if (StringUtils.isBlank(version_))
			return 0;

		int revisionMarker = version_.indexOf('-');
		if (revisionMarker != -1)
			return (Integer.valueOf(version_.substring(2, revisionMarker)) * 10) + Integer.valueOf(version_.substring(revisionMarker + 1));

		return Integer.valueOf(version_.substring(2)) * 10;
	}

	public int distance(DosboxVersion other) {
		return Math.abs(other.getVersionAsInt() - getVersionAsInt());
	}

	@Override
	public boolean isDefault() {
		return default_;
	}

	public void setDefault(boolean isDefault) {
		default_ = isDefault;
	}

	public boolean isMultiConfig() {
		return multiConfig_;
	}

	public void setMultiConfig(boolean multiConfig) {
		multiConfig_ = multiConfig;
	}

	public boolean isUsingCurses() {
		return usingCurses_;
	}

	public void setUsingCurses(boolean usingCurses) {
		usingCurses_ = usingCurses;
	}

	public Map<String, String> getDynamicOptions() {
		return dynamicOptions_;
	}

	public void setDynamicOptions(Map<String, String> dynamicOptions) {
		dynamicOptions_ = dynamicOptions;
	}

	public File getPath() {
		return path_.getFile();
	}

	public File getCanonicalPath() {
		return path_.getCanonicalFile();
	}

	public void setPath(String path) {
		path_ = new FileLocation(path, FileLocationService.getInstance().dosboxRelative());
	}

	public File getExe() {
		return exe_.getFile();
	}

	public File getCanonicalExe() {
		return exe_.getCanonicalFile();
	}

	public void setExe(String exe) {
		exe_ = new FileLocation(exe, FileLocationService.getInstance().dosboxRelative());
	}

	public File getExecutable() {
		return StringUtils.isBlank(getExe().getPath()) ? new File(getPath(), FileLocationService.DOSBOX_EXE_STRING): getExe();
	}

	public File getCanonicalExecutable() {
		return StringUtils.isBlank(getExe().getPath()) ? new File(getCanonicalPath(), FileLocationService.DOSBOX_EXE_STRING): getCanonicalExe();
	}

	public String getExecutableParameters() {
		return executableParameters_;
	}

	public void setExecutableParameters(String executableParameters) {
		executableParameters_ = executableParameters;
	}

	public void setConfigurationFileLocation(String file) {
		configuration_.setFileLocation(new FileLocation(file, FileLocationService.getInstance().dosboxRelative()));
	}

	public String getConfigurationString() {
		return configuration_.toString(null);
	}

	public String resetAndLoadConfiguration() throws IOException {
		return configuration_.reloadWithAutoexec(TextService.getInstance(), null);
	}

	public String loadConfigurationData(ITextService text, String data, File file) throws IOException {
		return configuration_.loadDataWithAutoexec(text, data, file, null);
	}

	public File getCwd() {
		return FileLocationService.getInstance().getDosroot();
	}

	public GenericStats getStats() {
		return stats_;
	}

	public void setStats(GenericStats stats) {
		stats_ = stats;
	}

	public boolean isUsingNewMachineConfig() {
		return getGeneration().ordinal() >= Generation.GEN_073.ordinal();
	}

	public Generation getGeneration() {
		Generation result = configuration_.getGeneration();
		if (result != null)
			return result;

		int ver = getVersionAsInt();
		if (ver >= 730)
			return Generation.GEN_073;
		else if (ver >= 700)
			return Generation.GEN_070;
		else if (ver >= 650)
			return Generation.GEN_065;
		else
			return Generation.GEN_063;
	}

	@Override
	public int compareTo(DosboxVersion comp) {
		int ver1 = getVersionAsInt();
		int ver2 = comp.getVersionAsInt();
		if (ver1 != ver2) {
			return (ver1 - ver2);
		}
		return getTitle().compareTo(comp.getTitle());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof DosboxVersion))
			return false;
		DosboxVersion that = (DosboxVersion)obj;
		return StringUtils.equals(version_, that.version_) && StringUtils.equals(getTitle(), that.getTitle());
	}

	@Override
	public int hashCode() {
		return Objects.hash(version_, getTitle());
	}
}
