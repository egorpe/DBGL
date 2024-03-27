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
package org.dbgl.gui.abstractdialog;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.dbgl.exception.DrivelettersExhaustedException;
import org.dbgl.gui.controls.Button_;
import org.dbgl.gui.controls.CTabFolder_;
import org.dbgl.gui.controls.Chain;
import org.dbgl.gui.controls.Composite_;
import org.dbgl.gui.controls.DaControlConvertorAdapter;
import org.dbgl.gui.controls.Group_;
import org.dbgl.gui.controls.List_;
import org.dbgl.gui.controls.Mess_;
import org.dbgl.gui.controls.Text_;
import org.dbgl.gui.dialog.EditMixerDialog;
import org.dbgl.gui.dialog.EditMountDialog;
import org.dbgl.gui.dialog.EditNativeCommandDialog;
import org.dbgl.gui.interfaces.DaControlConvertor;
import org.dbgl.model.NativeCommand;
import org.dbgl.model.aggregate.DosboxVersion;
import org.dbgl.model.conf.Autoexec;
import org.dbgl.model.conf.Configuration;
import org.dbgl.model.entity.TemplateProfileBase;
import org.dbgl.model.helper.DriveLetterHelper;
import org.dbgl.model.repository.BaseRepository;
import org.dbgl.model.repository.DosboxVersionRepository;
import org.dbgl.service.FileLocationService;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.events.ExpandAdapter;
import org.eclipse.swt.events.ExpandEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ExpandBar;
import org.eclipse.swt.widgets.ExpandItem;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;


public abstract class EditConfigurableDialog<T> extends SizeControlledTabbedDialog<T> {

	protected enum DosboxConfAction {
		SET, SWITCH, RELOAD, LOAD_TEMPLATE, RELOAD_TEMPLATE
	}

	public enum MountsValidationResult {
		OK, INVALID_OVERLAY_NO_BASEDRIVE, INVALID_OVERLAY_MIXED_ABSREL_PATHS, INVALID_OVERLAY_PATHS_EQUAL
	}

	protected final List<Chain> metaControls_ = new ArrayList<>();
	protected Chain nativeCommandControl_;

	protected List<DosboxVersion> dbversionsList_;
	protected int dbversionIndex_;
	protected Combo dbversionCombo_;
	protected Button setButton_;
	protected ExpandItem booterExpandItem_, dosExpandItem_;
	protected org.eclipse.swt.widgets.List mountingpointsList_;

	private Button switchButton_, ipx_;
	private Text ipxNet_;
	private Combo machine_;
	protected org.eclipse.swt.widgets.List nativeCommandsList_;

	protected EditConfigurableDialog(Shell parent, String dialogName) {
		super(parent, dialogName);
	}

	protected abstract void doPerformDosboxConfAction(DosboxConfAction action, DosboxVersion newDosboxVersion);

	@Override
	protected boolean prepare() {
		try {
			dbversionsList_ = new DosboxVersionRepository().listAll();
			dbversionIndex_ = BaseRepository.indexOfDefault(dbversionsList_);
			return true;
		} catch (SQLException e) {
			Mess_.on(getParent()).exception(e).warning();
			return false;
		}
	}

	protected void updateControlsByConfigurable(TemplateProfileBase configurable) {
		Configuration combinedConf = configurable.getCombinedConfiguration();
		Autoexec combinedAuto = combinedConf.getAutoexec();
		DosboxVersion dosbox = configurable.getDosboxVersion();

		// enable or disable controls
		metaControls_.forEach(x -> x.enableOrDisableControl(configurable));

		metaControls_.stream().forEach(x -> x.setComboValues(dosbox.getDynamicOptions()));

		// set possible values for certain dropdowns
		String[] machineValues = dosbox.isUsingNewMachineConfig() ? settings_.getValues("profile", "machine073"): settings_.getValues("profile", "machine");
		if (!Arrays.equals(machine_.getItems(), machineValues)) {
			machine_.setItems(machineValues);
		}

		if (mountingpointsList_.isEnabled() && !Arrays.equals(mountingpointsList_.getItems(), configurable.getMountStringsForUI()))
			mountingpointsList_.setItems(configurable.getMountStringsForUI());
		dosExpandItem_.setExpanded(!combinedAuto.isBooter());
		booterExpandItem_.setExpanded(combinedAuto.isBooter());

		// set control values
		metaControls_.forEach(x -> x.setControlByConfigurable(configurable, combinedConf));
		nativeCommandControl_.setControlByConfigurable(configurable, combinedConf);

		setButton_.setEnabled(false);
		switchButton_.setEnabled(false);

		ipxNet_.setEnabled(ipx_.getSelection());
	}

	protected void updateConfigurableByControls(TemplateProfileBase configurable) {
		Configuration combinedConf = configurable.getCombinedConfiguration();
		metaControls_.forEach(x -> x.updateConfigurableByControl(configurable, combinedConf));
		nativeCommandControl_.updateConfigurableByControl(configurable, combinedConf);

		configurable.setBooter(booterExpandItem_.getExpanded());
	}

	protected Group createGeneralTab(String capturesText, String configFileText) {
		Composite composite = createTabWithComposite("dialog.template.tab.general", new GridLayout());

		Group associationGroup = Group_.on(composite).layoutData(new GridData(SWT.FILL, SWT.CENTER, true, false)).layout(new GridLayout(5, false)).key("dialog.template.association").build();
		dbversionCombo_ = Chain.on(associationGroup).lbl(l -> l.key("dialog.template.dosboxversion")).cmb(
			c -> c.wide().items(dbversionsList_.stream().map(DosboxVersion::getTitle).toArray(String[]::new)).select(dbversionIndex_).visibleItemCount(20)).combo();
		setButton_ = Button_.on(associationGroup).text().key("dialog.template.set").tooltip("dialog.template.set.tooltip").listen(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				doPerformDosboxConfAction(DosboxConfAction.SET, dbversionsList_.get(dbversionCombo_.getSelectionIndex()));
			}
		}).ctrl();
		switchButton_ = Button_.on(associationGroup).text().key("dialog.template.switch").tooltip("dialog.template.switch.tooltip").listen(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				doPerformDosboxConfAction(DosboxConfAction.SWITCH, dbversionsList_.get(dbversionCombo_.getSelectionIndex()));
			}
		}).ctrl();
		Button_.on(associationGroup).text().key("dialog.template.reloadsettings").tooltip("dialog.template.reloadsettings.tooltip").listen(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				doPerformDosboxConfAction(DosboxConfAction.RELOAD, dbversionsList_.get(dbversionCombo_.getSelectionIndex()));
			}
		}).ctrl();
		dbversionCombo_.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				setButton_.setEnabled(true);
				switchButton_.setEnabled(true);
			}
		});

		Group miscGroup = Group_.on(composite).layoutData(new GridData(SWT.FILL, SWT.CENTER, true, false)).layout(new GridLayout(3, false)).key("dialog.template.miscellaneous").build();
		Chain.on(miscGroup).lbl(l -> l).lbl(l -> l.key("dialog.template.active")).lbl(l -> l.key("dialog.template.inactive")).build();
		Chain.on(miscGroup).lbl(l -> l.key("dialog.template.priority")).cmb(c -> c.tooltip("dialog.template.priority.tooltip").items("profile", "priority_active")).cmb(
			c -> c.tooltip("dialog.template.priority.tooltip").items("profile", "priority_inactive")).section("sdl").item("priority").build(metaControls_);
		Chain.on(miscGroup).lbl(l -> l.key("dialog.template.waitonerror")).but(b -> b.horSpan(2)).section("sdl").item("waitonerror").build(metaControls_);
		Chain.on(miscGroup).lbl(l -> l.key("dialog.template.exitafterwards")).but(b -> b.horSpan(2)).autoexec(Autoexec::getExit, Autoexec::setExit).build(metaControls_);
		Chain.on(miscGroup).lbl(l -> l.key("dialog.template.languagefile")).txt(t -> t.horSpan(2).tooltip("dialog.template.languagefile.tooltip")).section("dosbox").item("language").build(
			metaControls_);
		Chain.on(miscGroup).lbl(l -> l.key("dialog.template.captures")).txt(t -> t.horSpan(2).tooltip("dialog.template.captures.tooltip").val(capturesText).nonEditable()).build();
		Chain.on(miscGroup).lbl(l -> l.key("dialog.profile.configfile")).txt(t -> t.horSpan(2).val(configFileText).nonEditable()).build();

		return associationGroup;
	}

	protected void createDisplayTab() {
		CTabFolder subTabFolder = createSubTabs("dialog.template.tab.display", 1, 2);
		Composite releaseComposite = (Composite)subTabFolder.getChildren()[0];
		Composite experimentalComposite = (Composite)subTabFolder.getChildren()[1];

		Group groupRelease = Group_.on(releaseComposite).layoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false)).layout(new GridLayout(4, false)).key("dialog.template.general").build();

		Chain.on(groupRelease).lbl(l -> l.key("dialog.template.output")).cmb(c -> c.horSpan(3).tooltip("dialog.template.output.tooltip").items("profile", "output")).section("sdl").item(
			"output").build(metaControls_);
		Chain.on(groupRelease).lbl(l -> l.key("dialog.template.frameskip")).cmb(
			c -> c.horSpan(3).tooltip("dialog.template.frameskip.tooltip").items("profile", "frameskip").visibleItemCount(15)).section("render").item("frameskip").build(metaControls_);
		Chain.on(groupRelease).lbl(l -> l.key("dialog.template.scaler")).cmb(c -> c.tooltip("dialog.template.scaler.tooltip").items("profile", "scaler").visibleItemCount(15)).lbl(
			l -> l.key("dialog.template.scalerforced")).but(b -> b.tooltip("dialog.template.scalerforced.tooltip")).section("render").item("scaler").convert(new DaControlConvertorAdapter() {
				@Override
				public String toConfValue(String existingValue, String[] values) {
					String joinedValues = values[0];
					if (Boolean.parseBoolean(values[1]))
						joinedValues += " forced";
					if (StringUtils.isBlank(existingValue))
						return joinedValues;

					String[] exVals = toControlValues(existingValue);
					if (exVals.length != values.length) {
						System.err.println("configuration values mismatch: [" + existingValue + "] vs. [" + joinedValues + "]");
						return existingValue;
					}

					joinedValues = StringUtils.isBlank(values[0]) ? exVals[0]: values[0];
					if (Boolean.parseBoolean(values[1]))
						joinedValues += " forced";
					return joinedValues;
				}

				@Override
				public String toConfValueForDisplay(String[] values) {
					String result = toConfValue(null, values);
					switch (result) {
						case StringUtils.EMPTY:
							return "... <not forced>";
						case " forced":
							return "... forced";
						default:
							return result;
					}
				}

				@Override
				public String[] toControlValues(String value) {
					if (value == null)
						return new String[0];
					String[] results = new String[2];
					if (value.endsWith("forced")) {
						results[0] = value.substring(0, Math.max(value.length() - 7, 0));
						results[1] = String.valueOf(true);
					} else {
						results[0] = value;
						results[1] = String.valueOf(false);
					}
					return results;
				}
			}).build(metaControls_);

		Chain.on(groupRelease).lbl(l -> l.key("dialog.template.fullscreenresolution")).cmb(
			c -> c.horSpan(3).tooltip("dialog.template.fullscreenresolution.tooltip").items("profile", "fullresolution")).section("sdl").item("fullresolution").build(metaControls_);
		Chain.on(groupRelease).lbl(l -> l.key("dialog.template.windowresolution")).cmb(
			c -> c.horSpan(3).tooltip("dialog.template.windowresolution.tooltip").items("profile", "windowresolution")).section("sdl").item("windowresolution").build(metaControls_);
		Chain.on(groupRelease).lbl(l -> l.key("dialog.template.fullscreen")).but(b -> b.horSpan(3).tooltip("dialog.template.fullscreen.tooltip")).section("sdl").item("fullscreen").build(
			metaControls_);
		Chain.on(groupRelease).lbl(l -> l.key("dialog.template.doublebuffering")).but(b -> b.horSpan(3).tooltip("dialog.template.doublebuffering.tooltip")).section("sdl").item("fulldouble").build(
			metaControls_);
		Chain.on(groupRelease).lbl(l -> l.key("dialog.template.aspectcorrection")).but(b -> b.horSpan(3).tooltip("dialog.template.aspectcorrection.tooltip")).section("render").item("aspect").build(
			metaControls_);

		Group groupExpGeneral = Group_.on(experimentalComposite).layoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 1, 3)).layout(new GridLayout(3, false)).key(
			"dialog.template.general").build();
		String[] glShaderFiles = FileLocationService.getInstance().listGlShaderFilenames();
		String[] glShaders = glShaderFiles.length > 0 ? ArrayUtils.addAll(settings_.getValues("profile", "glshader"), glShaderFiles): settings_.getValues("profile", "glshader");
		Chain.on(groupExpGeneral).lbl(l -> l.key("dialog.template.glshader")).cmb(c -> c.editable().wide().items(glShaders)).but(
			b -> b.browse(true, Button_.BrowseType.FILE, Button_.CanonicalType.GLSHADER, false)).section("render").item("glshader").build(metaControls_);
		Chain.on(groupExpGeneral).lbl(l -> l.key("dialog.template.autofit")).but(b -> b.horSpan(2)).section("render").item("autofit").build(metaControls_);
		Combo pixelshader = Chain.on(groupExpGeneral).lbl(l -> l.key("dialog.template.pixelshader")).cmb(c -> c.horSpan(2).visibleItemCount(20)).section("sdl").item("pixelshader").build(
			metaControls_).getCombo();
		String[] shaders = FileLocationService.getInstance().listShaderFilenames();
		if (shaders.length > 0) {
			pixelshader.setItems(shaders);
			pixelshader.add("none", 0);
		} else {
			pixelshader.setItems(settings_.getValues("profile", "pixelshader"));
		}
		Chain.on(groupExpGeneral).lbl(l -> l.key("dialog.template.linewise")).but(b -> b.horSpan(2)).section("render").item("linewise").build(metaControls_);
		Chain.on(groupExpGeneral).lbl(l -> l.key("dialog.template.char9")).but(b -> b.horSpan(2)).section("render").item("char9").build(metaControls_);
		Chain.on(groupExpGeneral).lbl(l -> l.key("dialog.template.multiscan")).but(b -> b.horSpan(2)).section("render").item("multiscan").build(metaControls_);
		Chain.on(groupExpGeneral).lbl(l -> l.key("dialog.template.cgasnow")).but(b -> b.horSpan(2)).section("cpu", "video").item("cgasnow", "cgasnow").build(metaControls_);
		Chain.on(groupExpGeneral).lbl(l -> l.key("dialog.template.overscan")).cmb(c -> c.horSpan(2).editable().items("profile", "overscan")).section("sdl").item("overscan").build(metaControls_);
		Chain.on(groupExpGeneral).lbl(l -> l.key("dialog.template.vsyncmode")).cmb(c -> c.horSpan(2).items("profile", "vsyncmode")).section("vsync").item("vsyncmode").build(metaControls_);
		Chain.on(groupExpGeneral).lbl(l -> l.key("dialog.template.vsyncrate")).txt(t -> t.horSpan(2)).section("vsync").item("vsyncrate").build(metaControls_);
		Chain.on(groupExpGeneral).lbl(l -> l.key("dialog.template.forcerate")).txt(t -> t.horSpan(2)).section("cpu", "video").item("forcerate", "forcerate").build(metaControls_);
		Chain.on(groupExpGeneral).lbl(l -> l.key("dialog.template.videoram")).cmb(c -> c.horSpan(2).items("profile", "vmemsize")).section("dosbox", "video").item("vmemsize", "vmemsize").build(
			metaControls_);
		Chain.on(groupExpGeneral).lbl(l -> l.key("dialog.template.fullborderless")).but(b -> b.horSpan(2).tooltip("dialog.template.fullborderless.tooltip")).section("sdl").item(
			"fullborderless").build(metaControls_);
		Chain.on(groupExpGeneral).lbl(l -> l.key("dialog.template.glfullvsync")).but(b -> b.horSpan(2).tooltip("dialog.template.glfullvsync.tooltip")).section("sdl").item("glfullvsync").build(
			metaControls_);

		Group groupExpGlide = Group_.on(experimentalComposite).layout(new GridLayout(2, false)).key("dialog.template.glide").build();
		Chain.on(groupExpGlide).lbl(l -> l.key("dialog.template.glide")).cmb(c -> c.items("profile", "glide")).section("glide", "voodoo").item("glide", "glide").build(metaControls_);
		Chain.on(groupExpGlide).lbl(l -> l.key("dialog.template.glideport")).txt(t -> t).section("glide").item("port", "grport").build(metaControls_);
		Chain.on(groupExpGlide).lbl(l -> l.key("dialog.template.lfbglide")).cmb(c -> c.items("profile", "lfbglide")).section("glide", "voodoo").item("lfb", "lfb").build(metaControls_);
		Chain.on(groupExpGlide).lbl(l -> l.key("dialog.template.splash3dfx")).but(b -> b).section("glide", "voodoo").item("splash", "splash").build(metaControls_);

		Group groupExpVoodoo = Group_.on(experimentalComposite).layout(new GridLayout(2, false)).key("dialog.template.voodoo").build();
		Chain.on(groupExpVoodoo).lbl(l -> l.key("dialog.template.voodoo")).cmb(c -> c.items("profile", "voodoo")).section("pci", "voodoo").item("voodoo", "voodoo_card").build(metaControls_);
		Chain.on(groupExpVoodoo).lbl(l -> l.key("dialog.template.voodoomem")).cmb(c -> c.items("profile", "voodoomem")).section("pci").item("voodoomem").build(metaControls_);

		Group groupExpPPScaling = Group_.on(experimentalComposite).layout(new GridLayout(2, false)).key("dialog.template.pixelperfectscaling").build();
		Chain.on(groupExpPPScaling).lbl(l -> l.key("dialog.template.surfacenpsharpness")).spn(s -> s.min(0).max(100)).section("sdl").item("surfacenp-sharpness").build(metaControls_);
	}

	protected Group createMachineTab() {
		CTabFolder subTabFolder = createSubTabs("dialog.template.tab.machine", 1, 1);
		Composite releaseComposite = (Composite)subTabFolder.getChildren()[0];
		Composite experimentalComposite = (Composite)subTabFolder.getChildren()[1];

		Group cpuGroup = Group_.on(releaseComposite).layout(new GridLayout(6, false)).key("dialog.template.cpu").build();
		machine_ = Chain.on(cpuGroup).lbl(l -> l.key("dialog.template.machine")).cmb(c -> c.tooltip("dialog.template.machine.tooltip").visibleItemCount(20)).section("dosbox").item("machine").build(
			metaControls_).getCombo();
		Chain.on(cpuGroup).lbl(l -> l.key("dialog.template.cputype")).cmb(c -> c.horSpan(3).tooltip("dialog.template.cputype.tooltip").items("profile", "cputype")).section("cpu").item(
			"cputype").build(metaControls_);
		Chain.on(cpuGroup).lbl(l -> l.key("dialog.template.core")).cmb(c -> c.horSpan(5).tooltip("dialog.template.core.tooltip").items("profile", "core")).section("cpu").item("core").build(
			metaControls_);
		Chain.on(cpuGroup).lbl(l -> l.key("dialog.template.cycles")).cmb(c -> c.editable().tooltip("dialog.template.cycles.tooltip").items("profile", "cycles").visibleItemCount(15)).section(
			"cpu").item("cycles").build(metaControls_);
		Chain.on(cpuGroup).lbl(l -> l.key("dialog.template.up")).cmb(c -> c.editable().tooltip("dialog.template.up.tooltip").items("profile", "cycles_up")).section("cpu").item("cycleup").build(
			metaControls_);
		Chain.on(cpuGroup).lbl(l -> l.key("dialog.template.down")).cmb(c -> c.editable().tooltip("dialog.template.down.tooltip").items("profile", "cycles_down")).section("cpu").item(
			"cycledown").build(metaControls_);

		Group memoryGroup = Group_.on(releaseComposite).layout(new GridLayout(4, false)).key("dialog.template.memory").build();
		Chain.on(memoryGroup).lbl(l -> l.key("dialog.template.memorysize")).cmb(c -> c.horSpan(3).tooltip("dialog.template.memorysize.tooltip").items("profile", "memsize")).section("dosbox").item(
			"memsize").build(metaControls_);
		Chain.on(memoryGroup).lbl(l -> l.key("dialog.template.xms")).but(b -> b.horSpan(3).tooltip("dialog.template.xms.tooltip")).section("dos").item("xms").build(metaControls_);
		Chain.on(memoryGroup).lbl(l -> l.key("dialog.template.ems")).cmb(c -> c.horSpan(3).tooltip("dialog.template.ems.tooltip").items("profile", "ems")).section("dos").item("ems").build(
			metaControls_);
		Chain.on(memoryGroup).lbl(l -> l.key("dialog.template.umb")).cmb(c -> c.horSpan(3).tooltip("dialog.template.umb.tooltip").items("profile", "umb")).section("dos").item("umb").build(
			metaControls_);

		Group expMemoryGroup = Group_.on(experimentalComposite).layout(new GridLayout(2, false)).key("dialog.template.memory").build();
		Chain.on(expMemoryGroup).lbl(l -> l.key("dialog.template.memorysizekb")).txt(t -> t).section("dosbox").item("memsizekb").build(metaControls_);
		Chain.on(expMemoryGroup).lbl(l -> l.key("dialog.template.memalias")).cmb(c -> c.editable().items("profile", "memalias")).section("dosbox").item("memalias").build(metaControls_);

		return memoryGroup;
	}

	protected void createAudioTab() {
		CTabFolder subTabFolder = createSubTabs("dialog.template.tab.audio", 3, 3);
		Composite releaseComposite = (Composite)subTabFolder.getChildren()[0];
		Composite experimentalComposite = (Composite)subTabFolder.getChildren()[1];

		Group generalGroup = Group_.on(releaseComposite).layoutData(new GridData(SWT.FILL, SWT.FILL, true, false)).layout(new GridLayout(3, false)).key("dialog.template.general").build();
		Chain.on(generalGroup).lbl(l -> l.key("dialog.template.silentmode")).but(b -> b.horSpan(2).tooltip("dialog.template.silentmode.tooltip")).section("mixer").item("nosound").build(metaControls_);
		Chain.on(generalGroup).lbl(l -> l.key("dialog.template.samplerate")).cmb(c -> c.horSpan(2).tooltip("dialog.template.samplerate.tooltip").items("profile", "rate")).section("mixer").item(
			"rate").build(metaControls_);
		Chain.on(generalGroup).lbl(l -> l.key("dialog.template.blocksize")).cmb(c -> c.horSpan(2).tooltip("dialog.template.blocksize.tooltip").items("profile", "blocksize")).section("mixer").item(
			"blocksize").build(metaControls_);
		Chain.on(generalGroup).lbl(l -> l.key("dialog.template.prebuffer")).cmb(c -> c.horSpan(2).editable().tooltip("dialog.template.prebuffer.tooltip").items("profile", "prebuffer")).section(
			"mixer").item("prebuffer").build(metaControls_);
		Chain.on(generalGroup).lbl(l -> l.key("dialog.template.mpu401")).cmb(c -> c.horSpan(2).tooltip("dialog.template.mpu401.tooltip").items("profile", "mpu401")).section("midi", "midi").item(
			"intelligent", "mpu401").convert(new DaControlConvertorAdapter() {
				@Override
				public String toConfValue(String existingValue, String[] values) {
					return values[0];
				}

				@Override
				public String[] toControlValues(String value) {
					if (value == null)
						return new String[0];
					return new String[] {value};
				}

				@Override
				public String[] toConfValues(String[] values) {
					String[] result = new String[2];
					result[0] = String.valueOf(!values[0].equalsIgnoreCase("none"));
					result[1] = String.valueOf(!values[0].equalsIgnoreCase("uart"));
					return result;
				}

				@Override
				public String[] toControlValues(String[] values) {
					boolean intelligent = Boolean.parseBoolean(values[0]);
					boolean mpu = Boolean.parseBoolean(values[1]);
					return new String[] {mpu ? intelligent ? "intelligent": "uart": "none"};
				}
			}).build(metaControls_);

		Chain.on(generalGroup).lbl(l -> l.key("dialog.template.mididevice")).cmb(c -> c.horSpan(2).tooltip("dialog.template.mididevice.tooltip").items("profile", "device")).section("midi").item(
			"device", "mididevice").build(metaControls_);
		Chain.on(generalGroup).lbl(l -> l.key("dialog.template.midiconfig")).txt(t -> t.horSpan(2).tooltip("dialog.template.midiconfig.tooltip")).section("midi").item("config", "midiconfig").build(
			metaControls_);

		Chain chnMixer = Chain.on(generalGroup).lbl(l -> l.key("dialog.template.mixercommand")).txt(t -> t).autoexec(Autoexec::getMixer, Autoexec::setMixer).but(Button_.Builder::threedots).build(
			metaControls_);
		chnMixer.getButton().addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				String command = new EditMixerDialog(shell_, chnMixer.getText().getText()).open();
				if (command != null)
					chnMixer.getText().setText(command);
			}
		});

		Group soundblasterGroup = Group_.on(releaseComposite).layout(new GridLayout(2, false)).key("dialog.template.soundblaster").build();
		Chain.on(soundblasterGroup).lbl(l -> l.key("dialog.template.sbtype")).cmb(c -> c.tooltip("dialog.template.sbtype.tooltip").items("profile", "sbtype")).section("sblaster").item("type",
			"sbtype").build(metaControls_);
		Chain.on(soundblasterGroup).lbl(l -> l.key("dialog.template.sboplrate")).cmb(c -> c.tooltip("dialog.template.sboplrate.tooltip").items("profile", "oplrate")).section("sblaster").item(
			"oplrate").build(metaControls_);
		Chain.on(soundblasterGroup).lbl(l -> l.key("dialog.template.sboplmode")).cmb(c -> c.tooltip("dialog.template.sboplmode.tooltip").items("profile", "oplmode")).section("sblaster").item(
			"oplmode").build(metaControls_);
		Chain.on(soundblasterGroup).lbl(l -> l.key("dialog.template.sboplemu")).cmb(c -> c.tooltip("dialog.template.sboplemu.tooltip").items("profile", "oplemu")).section("sblaster").item(
			"oplemu").build(metaControls_);
		Chain.on(soundblasterGroup).lbl(l -> l.key("dialog.template.sbaddress")).cmb(c -> c.tooltip("dialog.template.sbaddress.tooltip").items("profile", "sbbase")).section("sblaster").item("base",
			"sbbase").build(metaControls_);
		Chain.on(soundblasterGroup).lbl(l -> l.key("dialog.template.sbirq")).cmb(c -> c.tooltip("dialog.template.sbirq.tooltip").items("profile", "irq")).section("sblaster").item("irq").build(
			metaControls_);
		Chain.on(soundblasterGroup).lbl(l -> l.key("dialog.template.sbdma")).cmb(c -> c.tooltip("dialog.template.sbdma.tooltip").items("profile", "dma")).section("sblaster").item("dma").build(
			metaControls_);
		Chain.on(soundblasterGroup).lbl(l -> l.key("dialog.template.sbhdma")).cmb(c -> c.tooltip("dialog.template.sbhdma.tooltip").items("profile", "hdma")).section("sblaster").item("hdma").build(
			metaControls_);
		Chain.on(soundblasterGroup).lbl(l -> l.key("dialog.template.mixer")).but(b -> b.tooltip("dialog.template.mixer.tooltip")).section("sblaster").item("mixer", "sbmixer").build(metaControls_);

		Group gusGroup = Group_.on(releaseComposite).layoutData(new GridData(SWT.FILL, SWT.FILL, true, false)).layout(new GridLayout(2, false)).key("dialog.template.gravisultrasound").build();
		Chain.on(gusGroup).lbl(l -> l.key("dialog.template.enablegus")).but(b -> b.tooltip("dialog.template.enablegus.tooltip")).section("gus").item("gus").build(metaControls_);
		Chain.on(gusGroup).lbl(l -> l.key("dialog.template.gusrate")).cmb(c -> c.tooltip("dialog.template.gusrate.tooltip").items("profile", "gusrate")).section("gus").item("rate", "gusrate").build(
			metaControls_);
		Chain.on(gusGroup).lbl(l -> l.key("dialog.template.gusaddress")).cmb(c -> c.tooltip("dialog.template.gusaddress.tooltip").items("profile", "gusbase")).section("gus").item("base",
			"gusbase").build(metaControls_);
		Chain.on(gusGroup).lbl(l -> l.key("dialog.template.gusirq1")).cmb(c -> c.tooltip("dialog.template.gusirq1.tooltip").items("profile", "irq1")).section("gus").item("irq1", "gusirq").build(
			metaControls_);
		Chain.on(gusGroup).lbl(l -> l.key("dialog.template.gusirq2")).cmb(c -> c.tooltip("dialog.template.gusirq1.tooltip").items("profile", "irq2")).section("gus").item("irq2").build(metaControls_);
		Chain.on(gusGroup).lbl(l -> l.key("dialog.template.gusdma1")).cmb(c -> c.tooltip("dialog.template.gusdma1.tooltip").items("profile", "dma1")).section("gus").item("dma1", "gusdma").build(
			metaControls_);
		Chain.on(gusGroup).lbl(l -> l.key("dialog.template.gusdma2")).cmb(c -> c.tooltip("dialog.template.gusdma1.tooltip").items("profile", "dma2")).section("gus").item("dma2").build(metaControls_);
		Chain.on(gusGroup).lbl(l -> l.key("dialog.template.ultradir")).txt(t -> t.tooltip("dialog.template.ultradir.tooltip")).section("gus").item("ultradir").build(metaControls_).getText();

		Group speakerGroup = Group_.on(releaseComposite).layoutData(new GridData(SWT.FILL, SWT.FILL, true, false)).layout(new GridLayout(2, false)).key("dialog.template.pcspeaker").build();
		Chain.on(speakerGroup).lbl(l -> l.key("dialog.template.enablepcspeaker")).but(b -> b.tooltip("dialog.template.enablepcspeaker.tooltip")).section("speaker").item("pcspeaker").build(
			metaControls_);
		Chain.on(speakerGroup).lbl(l -> l.key("dialog.template.pcrate")).cmb(c -> c.tooltip("dialog.template.pcrate.tooltip").items("profile", "pcrate")).section("speaker").item("pcrate").build(
			metaControls_);

		Group tandyGroup = Group_.on(releaseComposite).layoutData(new GridData(SWT.FILL, SWT.FILL, false, false)).layout(new GridLayout(2, false)).key("dialog.template.tandy").build();
		Chain.on(tandyGroup).lbl(l -> l.key("dialog.template.enabletandy")).cmb(c -> c.tooltip("dialog.template.enabletandy.tooltip").items("profile", "tandy")).section("speaker").item("tandy").build(
			metaControls_);
		Chain.on(tandyGroup).lbl(l -> l.key("dialog.template.tandyrate")).cmb(c -> c.tooltip("dialog.template.tandyrate.tooltip").items("profile", "tandyrate")).section("speaker").item(
			"tandyrate").build(metaControls_);

		Group disneyGroup = Group_.on(releaseComposite).layoutData(new GridData(SWT.FILL, SWT.FILL, true, false)).layout(new GridLayout(2, false)).key("dialog.template.miscellaneous").build();
		Chain.on(disneyGroup).lbl(l -> l.key("dialog.template.enablesoundsource")).but(b -> b.tooltip("dialog.template.enablesoundsource.tooltip")).section("speaker").item("disney").build(
			metaControls_);

		Group generalExpGroup = Group_.on(experimentalComposite).layoutData(new GridData(SWT.FILL, SWT.FILL, true, false)).layout(new GridLayout(2, false)).key("dialog.template.general").build();
		Chain.on(generalExpGroup).lbl(l -> l.key("dialog.template.swapstereo")).but(b -> b).section("mixer").item("swapstereo").build(metaControls_);

		Group ps1ExpGroup = Group_.on(experimentalComposite).layoutData(new GridData(SWT.FILL, SWT.FILL, false, false)).layout(new GridLayout(2, false)).key("dialog.template.ps1").build();
		Chain.on(ps1ExpGroup).lbl(l -> l.key("dialog.template.ps1enable")).but(b -> b).section("speaker").item("ps1audio").onOff().build(metaControls_);
		Chain.on(ps1ExpGroup).lbl(l -> l.key("dialog.template.ps1rate")).cmb(c -> c.items("profile", "ps1rate")).section("speaker").item("ps1audiorate").build(metaControls_);

		Group mt32ExpGroup = Group_.on(experimentalComposite).layoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 3)).layout(new GridLayout(3, false)).key("dialog.template.mt32").build();
		Chain.on(mt32ExpGroup).lbl(l -> l.key("dialog.template.mt32.romdir")).txt(t -> t).but(b -> b.browse(true, Button_.BrowseType.DIR, Button_.CanonicalType.NONE, false)).section("midi").item(
			"mt32.romdir").build(metaControls_);
		Chain.on(mt32ExpGroup).lbl(l -> l.key("dialog.template.swapstereo")).but(b -> b.horSpan(2)).section("midi").item("mt32.reverse.stereo").build(metaControls_);
		Chain.on(mt32ExpGroup).lbl(l -> l.key("dialog.template.mt32.verboselogging")).but(b -> b.horSpan(2)).section("midi").item("mt32.verbose").build(metaControls_);
		Chain.on(mt32ExpGroup).lbl(l -> l.key("dialog.template.mt32.multithread")).but(b -> b.horSpan(2)).section("midi").item("mt32.thread").build(metaControls_);
		Chain.on(mt32ExpGroup).lbl(l -> l.key("dialog.template.mt32.chunk")).spn(s -> s.horSpan(2).min(2).max(100)).section("midi").item("mt32.chunk").build(metaControls_);
		Chain.on(mt32ExpGroup).lbl(l -> l.key("dialog.template.mt32.prebuffer")).spn(s -> s.horSpan(2).min(3).max(200)).section("midi").item("mt32.prebuffer").build(metaControls_);
		Chain.on(mt32ExpGroup).lbl(l -> l.key("dialog.template.mt32.partials")).spn(s -> s.horSpan(2).min(0).max(256)).section("midi").item("mt32.partials").build(metaControls_);
		Chain.on(mt32ExpGroup).lbl(l -> l.key("dialog.template.mt32.dac")).cmb(c -> c.horSpan(2).items("profile", "mt32dac")).section("midi").item("mt32.dac").build(metaControls_);
		Chain.on(mt32ExpGroup).lbl(l -> l.key("dialog.template.mt32.analog")).cmb(c -> c.horSpan(2).items("profile", "mt32analog")).section("midi").item("mt32.analog").build(metaControls_);
		Chain.on(mt32ExpGroup).lbl(l -> l.key("dialog.template.mt32.reverbmode")).cmb(c -> c.horSpan(2).items("profile", "mt32reverbmode")).section("midi").item("mt32.reverb.mode").build(
			metaControls_);
		Chain.on(mt32ExpGroup).lbl(l -> l.key("dialog.template.mt32.reverbtime")).cmb(c -> c.horSpan(2).items("profile", "mt32reverbtime")).section("midi").item("mt32.reverb.time").build(
			metaControls_);
		Chain.on(mt32ExpGroup).lbl(l -> l.key("dialog.template.mt32.reverblevel")).cmb(c -> c.horSpan(2).items("profile", "mt32reverblevel")).section("midi").item("mt32.reverb.level").build(
			metaControls_);

		Group fluidExpGroup = Group_.on(experimentalComposite).layoutData(new GridData(SWT.FILL, SWT.FILL, true, false)).layout(new GridLayout(3, false)).key(
			"dialog.template.fluidsynth").build();
		Chain.on(fluidExpGroup).lbl(l -> l.key("dialog.template.fluidsynth.driver")).cmb(c -> c.horSpan(2).editable().items("profile", "fluidsynthdriver")).section("midi").item("fluid.driver").build(
			metaControls_);
		Chain.on(fluidExpGroup).lbl(l -> l.key("dialog.template.fluidsynth.soundfont")).txt(t -> t).but(b -> b.browse(true, Button_.BrowseType.FILE, Button_.CanonicalType.NONE, false)).section(
			"midi").item("fluid.soundfont").build(metaControls_);
		Chain.on(fluidExpGroup).lbl(l -> l.key("dialog.template.fluidsynth.samplerate")).cmb(c -> c.horSpan(2).items("profile", "fluidsynthsamplerate")).section("midi").item("fluid.samplerate").build(
			metaControls_);
		Chain.on(fluidExpGroup).lbl(l -> l.key("dialog.template.fluidsynth.gain")).txt(t -> t.horSpan(2)).section("midi").item("fluid.gain").build(metaControls_);

		Group innovaExpGroup = Group_.on(experimentalComposite).layoutData(new GridData(SWT.FILL, SWT.FILL, false, false)).layout(new GridLayout(2, false)).key("dialog.template.innova").build();
		Chain.on(innovaExpGroup).lbl(l -> l.key("dialog.template.innovaenable")).but(b -> b).section("innova").item("innova").build(metaControls_);
		Chain.on(innovaExpGroup).lbl(l -> l.key("dialog.template.innovarate")).cmb(c -> c.items("profile", "innovarate")).section("innova").item("samplerate").build(metaControls_);
		Chain.on(innovaExpGroup).lbl(l -> l.key("dialog.template.innovaaddress")).cmb(c -> c.items("profile", "innovabase")).section("innova").item("sidbase").build(metaControls_);
		Chain.on(innovaExpGroup).lbl(l -> l.key("dialog.template.innovaquality")).cmb(c -> c.items("profile", "innovaquality")).section("innova").item("quality").build(metaControls_);

		Group imfcExpGroup = Group_.on(experimentalComposite).layoutData(new GridData(SWT.FILL, SWT.FILL, true, false)).layout(new GridLayout(2, false)).key("dialog.template.imfc").build();
		Chain.on(imfcExpGroup).lbl(l -> l.key("dialog.template.imfcenable")).but(b -> b).section("imfc").item("imfc").build(metaControls_);
		Chain.on(imfcExpGroup).lbl(l -> l.key("dialog.template.imfcbase")).cmb(c -> c.items("profile", "imfcbase")).section("imfc").item("imfc_base").build(metaControls_);
		Chain.on(imfcExpGroup).lbl(l -> l.key("dialog.template.imfcirq")).cmb(c -> c.items("profile", "imfcirq")).section("imfc").item("imfc_irq").build(metaControls_);
		Chain.on(imfcExpGroup).lbl(l -> l.key("dialog.template.imfcfilter")).cmb(c -> c.editable().items("profile", "imfcfilter")).section("imfc").item("imfc_filter").build(metaControls_);
			
		Group soundblasterExpGroup = Group_.on(experimentalComposite).layoutData(new GridData(SWT.FILL, SWT.FILL, false, false)).layout(new GridLayout(2, false)).key(
			"dialog.template.soundblaster").build();
		Chain.on(soundblasterExpGroup).lbl(l -> l.key("dialog.template.hardwaresbaddress")).cmb(c -> c.items("profile", "hardwaresbbase")).section("sblaster").item("hardwarebase").build(
			metaControls_);
		Chain.on(soundblasterExpGroup).lbl(l -> l.key("dialog.template.goldplay")).but(b -> b).section("sblaster").item("goldplay").build(metaControls_);
		Chain.on(soundblasterExpGroup).lbl(l -> l.key("dialog.template.fmstrength")).spn(s -> s.min(1).max(1000)).section("sblaster").item("fmstrength").build(metaControls_);
	}

	protected void createIOTab() {
		CTabFolder subTabFolder = createSubTabs("dialog.template.tab.io", 3, 4);
		Composite releaseComposite = (Composite)subTabFolder.getChildren()[0];
		Composite experimentalComposite = (Composite)subTabFolder.getChildren()[1];

		Group mouseGroup = Group_.on(releaseComposite).layoutData(new GridData(SWT.FILL, SWT.FILL, false, false)).layout(new GridLayout(2, false)).key("dialog.template.mouse").build();
		Chain.on(mouseGroup).lbl(l -> l.key("dialog.template.autolock")).but(b -> b.tooltip("dialog.template.autolock.tooltip")).section("sdl").item("autolock").build(metaControls_);
		Chain.on(mouseGroup).lbl(l -> l.key("dialog.template.sensitivity")).cmb(
			c -> c.editable().items("profile", "sensitivity").visibleItemCount(20).tooltip("dialog.template.sensitivity.tooltip")).section("sdl").item("sensitivity").convert(new DaControlConvertor() {
				@Override
				public String toConfValue(String existingValue, String[] values) {
					return String.join(",", values);
				}

				@Override
				public String toConfValueForDisplay(String[] values) {
					return toConfValue(null, values);
				}

				@Override
				public String[] toControlValues(String value) {
					return value == null ? new String[0]: new String[] {value};
				}

				@Override
				public String[] toConfValues(String[] values) {
					return new String[0];
				}

				@Override
				public String[] toControlValues(String[] values) {
					return new String[0];
				}
			}).build(metaControls_);

		Group keyboardGroup = Group_.on(releaseComposite).layoutData(new GridData(SWT.FILL, SWT.FILL, true, false)).layout(new GridLayout(3, false)).key("dialog.template.keyboard").build();
		Chain.on(keyboardGroup).lbl(l -> l.key("dialog.template.usescancodes")).but(b -> b.horSpan(2).tooltip("dialog.template.usescancodes.tooltip")).section("sdl").item("usescancodes").build(
			metaControls_);
		Text mapperfile = Chain.on(keyboardGroup).lbl(l -> l.key("dialog.template.mapperfile")).txt(t -> t.tooltip("dialog.template.mapperfile.tooltip")).section("sdl").item("mapperfile").build(
			metaControls_).getText();
		Button_.on(keyboardGroup).star().listen(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				mapperfile.setText(settings_.getValue("profile", "uniquemapperfile"));
			}
		}).ctrl();
		Chain.on(keyboardGroup).lbl(l -> l.key("dialog.template.keyboardlayout")).cmb(
			c -> c.horSpan(2).wide().editable().tooltip("dialog.template.keyboardlayout.tooltip").items("profile", "keyboardlayout").visibleItemCount(15)).section("dos").item("keyboardlayout").build(
				metaControls_);
		Chain.on(keyboardGroup).lbl(l -> l.key("dialog.template.keybcommand")).txt(t -> t.horSpan(2)).autoexec(Autoexec::getKeyb, Autoexec::setKeyb).build(metaControls_);

		Group joystickGroup = Group_.on(releaseComposite).layoutData(new GridData(SWT.FILL, SWT.FILL, true, false)).layout(new GridLayout(2, false)).key("dialog.template.joystick").build();
		Chain.on(joystickGroup).lbl(l -> l.key("dialog.template.joysticktype")).cmb(c -> c.tooltip("dialog.template.joysticktype.tooltip").items("profile", "joysticktype")).section("bios",
			"joystick").item("joysticktype", "joysticktype").build(metaControls_);
		Chain.on(joystickGroup).lbl(l -> l.key("dialog.template.timedemulation")).but(b -> b.tooltip("dialog.template.timedemulation.tooltip")).section("joystick").item("timed").build(metaControls_);
		Chain.on(joystickGroup).lbl(l -> l.key("dialog.template.autofire")).but(b -> b.tooltip("dialog.template.autofire.tooltip")).section("joystick").item("autofire").build(metaControls_);
		Chain.on(joystickGroup).lbl(l -> l.key("dialog.template.swap34")).but(b -> b.tooltip("dialog.template.swap34.tooltip")).section("joystick").item("swap34").build(metaControls_);
		Chain.on(joystickGroup).lbl(l -> l.key("dialog.template.buttonwrapping")).but(b -> b.tooltip("dialog.template.buttonwrapping.tooltip")).section("joystick").item("buttonwrap").build(
			metaControls_);

		Group modemGroup = Group_.on(releaseComposite).layoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1)).layout(new GridLayout(2, false)).key("dialog.template.modem").build();
		Chain.on(modemGroup).lbl(l -> l.key("dialog.template.serial1")).txt(t -> t.tooltip("dialog.template.serial.tooltip")).section("serial").item("serial1").build(metaControls_);
		Chain.on(modemGroup).lbl(l -> l.key("dialog.template.serial2")).txt(t -> t.tooltip("dialog.template.serial.tooltip")).section("serial").item("serial2").build(metaControls_);
		Chain.on(modemGroup).lbl(l -> l.key("dialog.template.serial3")).txt(t -> t.tooltip("dialog.template.serial.tooltip")).section("serial").item("serial3").build(metaControls_);
		Chain.on(modemGroup).lbl(l -> l.key("dialog.template.serial4")).txt(t -> t.tooltip("dialog.template.serial.tooltip")).section("serial").item("serial4").build(metaControls_);

		Group networkGroup = Group_.on(releaseComposite).layoutData(new GridData(SWT.FILL, SWT.FILL, true, false)).layout(new GridLayout(2, false)).key("dialog.template.network").build();
		ipx_ = Chain.on(networkGroup).lbl(l -> l.key("dialog.template.enableipx")).but(b -> b.tooltip("dialog.template.enableipx.tooltip")).section("ipx").item("ipx").build(metaControls_).getButton();
		ipxNet_ = Chain.on(networkGroup).lbl(l -> l.horSpan(2).key("dialog.template.ipxnetcommand")).txt(t -> t.horSpan(2)).autoexec(Autoexec::getIpxnet, Autoexec::setIpxnet).build(
			metaControls_).getText();

		ipx_.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				ipxNet_.setEnabled(ipx_.getSelection());
			}
		});

		Group mouseExpGroup = Group_.on(experimentalComposite).layoutData(new GridData(SWT.FILL, SWT.FILL, true, false)).layout(new GridLayout(2, false)).key("dialog.template.mouse").build();
		Chain.on(mouseExpGroup).lbl(l -> l.key("dialog.template.int33")).but(b -> b).section("dos").item("int33").build(metaControls_);
		Chain.on(mouseExpGroup).lbl(l -> l.key("dialog.template.biosps2")).but(b -> b).section("dos").item("biosps2").build(metaControls_);
		Chain.on(mouseExpGroup).lbl(l -> l.key("dialog.template.aux")).but(b -> b).section("keyboard").item("aux").build(metaControls_);
		Chain.on(mouseExpGroup).lbl(l -> l.key("dialog.template.auxdevice")).cmb(c -> c.items("profile", "auxdevice")).section("keyboard").item("auxdevice").build(metaControls_);

		Group miscExpGroup = Group_.on(experimentalComposite).layoutData(new GridData(SWT.FILL, SWT.FILL, true, false)).layout(new GridLayout(2, false)).key("dialog.template.miscellaneous").build();
		Chain.on(miscExpGroup).lbl(l -> l.key("dialog.template.files")).spn(s -> s.min(8).max(255)).section("dos", "config").item("files", "files").build(metaControls_);
		Chain.on(miscExpGroup).lbl(l -> l.key("dialog.template.isapnpbios")).but(b -> b).section("cpu").item("isapnpbios").build(metaControls_);
		Chain.on(miscExpGroup).lbl(l -> l.key("dialog.template.ide1")).but(b -> b).section("ide, primary").item("enable").build(metaControls_);
		Chain.on(miscExpGroup).lbl(l -> l.key("dialog.template.ide2")).but(b -> b).section("ide, secondary").item("enable").build(metaControls_);
		Chain.on(miscExpGroup).lbl(l -> l.key("dialog.template.ide3")).but(b -> b).section("ide, tertiary").item("enable").build(metaControls_);
		Chain.on(miscExpGroup).lbl(l -> l.key("dialog.template.ide4")).but(b -> b).section("ide, quaternary").item("enable").build(metaControls_);
		Chain.on(miscExpGroup).lbl(l -> l.key("dialog.template.automount")).but(b -> b).section("dos").item("automount").build(metaControls_);

		Group printerExpGroup = Group_.on(experimentalComposite).layoutData(new GridData(SWT.FILL, SWT.FILL, true, false)).layout(new GridLayout(2, false)).key("dialog.template.printer").build();
		Chain.on(printerExpGroup).lbl(l -> l.key("dialog.template.printerenable")).but(b -> b).section("printer").item("printer").build(metaControls_);
		Chain.on(printerExpGroup).lbl(l -> l.key("dialog.template.printerdpi")).spn(s -> s.min(0).max(Short.MAX_VALUE)).section("printer").item("dpi").build(metaControls_);
		Chain.on(printerExpGroup).lbl(l -> l.key("dialog.template.printerwidth")).spn(s -> s.min(0).max(Short.MAX_VALUE)).section("printer").item("width").build(metaControls_);
		Chain.on(printerExpGroup).lbl(l -> l.key("dialog.template.printerheight")).spn(s -> s.min(0).max(Short.MAX_VALUE)).section("printer").item("height").build(metaControls_);
		Chain.on(printerExpGroup).lbl(l -> l.key("dialog.template.printeroutput")).cmb(c -> c.items("profile", "printeroutput")).section("printer").item("printoutput").build(metaControls_);
		Chain.on(printerExpGroup).lbl(l -> l.key("dialog.template.printermultipage")).but(b -> b).section("printer").item("multipage").build(metaControls_);
		Chain.on(printerExpGroup).lbl(l -> l.key("dialog.template.printerdocpath")).txt(t -> t).section("printer").item("docpath").build(metaControls_);
		Chain.on(printerExpGroup).lbl(l -> l.key("dialog.template.printertimeout")).spn(s -> s.min(0).max(Short.MAX_VALUE)).section("printer").item("timeout").build(metaControls_);

		Group joystickExpGroup = Group_.on(experimentalComposite).layoutData(new GridData(SWT.FILL, SWT.FILL, true, false)).layout(new GridLayout(2, false)).key("dialog.template.joystick").build();
		Chain.on(joystickExpGroup).lbl(l -> l.key("dialog.template.circularinput")).but(b -> b.tooltip("dialog.template.circularinput.tooltip")).section("joystick").item("circularinput").build(
			metaControls_);
		Chain.on(joystickExpGroup).lbl(l -> l.key("dialog.template.deadzone")).spn(s -> s.tooltip("dialog.template.deadzone.tooltip").min(0).max(100)).section("joystick").item("deadzone").build(
			metaControls_);

		Group parallelExpGroup = Group_.on(experimentalComposite).layoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1)).layout(new GridLayout(2, false)).key(
			"dialog.template.parallel").build();
		Chain.on(parallelExpGroup).lbl(l -> l.key("dialog.template.parallel1")).txt(t -> t).section("parallel").item("parallel1").build(metaControls_);
		Chain.on(parallelExpGroup).lbl(l -> l.key("dialog.template.parallel2")).txt(t -> t).section("parallel").item("parallel2").build(metaControls_);
		Chain.on(parallelExpGroup).lbl(l -> l.key("dialog.template.parallel3")).txt(t -> t).section("parallel").item("parallel3").build(metaControls_);
		Chain.on(parallelExpGroup).lbl(l -> l.key("dialog.template.dongle")).but(b -> b).section("parallel").item("dongle").build(metaControls_);

		Group ne2000ExpGroup = Group_.on(experimentalComposite).layoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1)).layout(new GridLayout(2, false)).key("dialog.template.ne2000").build();
		Chain.on(ne2000ExpGroup).lbl(l -> l.key("dialog.template.ne2000enable")).but(b -> b).section("ne2000").item("ne2000").build(metaControls_);
		Chain.on(ne2000ExpGroup).lbl(l -> l.key("dialog.template.ne2000base")).txt(t -> t).section("ne2000").item("nicbase").build(metaControls_);
		Chain.on(ne2000ExpGroup).lbl(l -> l.key("dialog.template.ne2000irq")).txt(t -> t).section("ne2000").item("nicirq").build(metaControls_);
		Chain.on(ne2000ExpGroup).lbl(l -> l.key("dialog.template.ne2000macaddress")).txt(t -> t).section("ne2000").item("macaddr").build(metaControls_);
		Chain.on(ne2000ExpGroup).lbl(l -> l.key("dialog.template.ne2000realnic")).txt(t -> t).section("ne2000").item("realnic").build(metaControls_);
	}

	protected void createCustomCommandsTab() {
		CTabFolder subTabFolder = createSubTabs("dialog.template.tab.customcommands", "dialog.template.tab.dosboxautoexec", 2, "dialog.template.tab.customconf", 2, "dialog.template.tab.native", 2);
		Composite dosboxComposite = (Composite)subTabFolder.getChildren()[0];
		Composite confComposite = (Composite)subTabFolder.getChildren()[1];
		Composite nativeComposite = (Composite)subTabFolder.getChildren()[2];

		for (int i = 0; i < Autoexec.SECTIONS; i++) {
			int j = i + 1;
			Chain.on(dosboxComposite).lbl(l -> l.key("dialog.template.customcommand" + j)).txt(Text_.Builder::multi).autoexec(i, Autoexec::getCustomSection, Autoexec::setCustomSection).build(
				metaControls_);
		}

		Chain.on(confComposite).lbl(l -> l.key("dialog.template.tab.beforeautoexec")).txt(Text_.Builder::multi).customSection(TemplateProfileBase::getCustomSection,
			TemplateProfileBase::setCustomSection).build(metaControls_);

		nativeCommandControl_ = Chain.on(nativeComposite).lbl(l -> l.key("dialog.template.nativecommands")).lbl(l -> l).lst(l -> l).nativeCommands(TemplateProfileBase::getNativeCommands,
			TemplateProfileBase::setNativeCommandsWithObject).build();
		nativeCommandsList_ = nativeCommandControl_.getList();
		nativeCommandsList_.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDoubleClick(MouseEvent event) {
				if (nativeCommandsList_.getSelectionIndex() == -1) {
					doAddNativeCommand();
				} else {
					doEditNativeCommand();
				}
			}
		});
		Composite nativeButComp = Composite_.on(nativeComposite).layoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false)).innerLayout(1).vertSpacing().build();
		Chain.on(nativeButComp).but(b -> b.text().key("dialog.template.mount.add").listen(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				doAddNativeCommand();
			}
		})).but(b -> b.text().key("dialog.template.mount.edit").listen(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				doEditNativeCommand();
			}
		})).but(b -> b.text().key("dialog.template.mount.remove").listen(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				doRemoveNativeCommand();
			}
		})).but(b -> b.arrow(true).listen(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				int sel = nativeCommandsList_.getSelectionIndex();
				if (sel > 0) {
					List<NativeCommand> cmds = getNativeCommands();
					Collections.swap(cmds, sel, sel - 1);
					nativeCommandControl_.setControlByNativeCommands(cmds);
					nativeCommandsList_.select(sel - 1);

				}
			}
		})).but(b -> b.arrow(false).listen(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				int sel = nativeCommandsList_.getSelectionIndex();
				if (sel >= 0 && sel < nativeCommandsList_.getItemCount() - 1) {
					List<NativeCommand> cmds = getNativeCommands();
					Collections.swap(cmds, sel, sel + 1);
					nativeCommandControl_.setControlByNativeCommands(cmds);
					nativeCommandsList_.select(sel + 1);
				}
			}
		})).build();
	}

	private List<NativeCommand> getNativeCommands() {
		return new ArrayList<>(nativeCommandControl_.getCurrentNativeCommands());
	}

	protected void doAddNativeCommand() {
		EditNativeCommandDialog cmdDialog = new EditNativeCommandDialog(shell_, null);
		NativeCommand cmd = cmdDialog.open();
		if (cmd != null) {
			int nr = nativeCommandsList_.getSelectionIndex() + 1;
			List<NativeCommand> cmds = getNativeCommands();
			cmds.add(nr, cmd);
			nativeCommandControl_.setControlByNativeCommands(cmds);
		}
	}

	protected void doEditNativeCommand() {
		int sel = nativeCommandsList_.getSelectionIndex();
		if (sel != -1) {
			List<NativeCommand> cmds = getNativeCommands();
			NativeCommand cmd = cmds.get(sel);
			if (!cmd.isDosboxCommand()) {
				EditNativeCommandDialog cmdDialog = new EditNativeCommandDialog(shell_, cmd);
				cmd = cmdDialog.open();
				if (cmd != null) {
					cmds.set(sel, cmd);
					nativeCommandControl_.setControlByNativeCommands(cmds);
				}
			}
		}
	}

	protected void doRemoveNativeCommand() {
		int sel = nativeCommandsList_.getSelectionIndex();
		if (sel != -1) {
			List<NativeCommand> cmds = getNativeCommands();
			NativeCommand cmd = cmds.get(sel);
			if (!cmd.isDosboxCommand()) {
				cmds.remove(sel);
				nativeCommandControl_.setControlByNativeCommands(cmds);
				nativeCommandsList_.select(Math.min(sel, nativeCommandsList_.getItemCount() - 1));
			}
		}
	}

	protected void createMountingTab(TemplateProfileBase configurable, boolean multiEdit) {
		Composite composite = createTabWithComposite("dialog.template.tab.mounting", new GridLayout());

		Group mountGroup = Group_.on(composite).layoutData(new GridData(SWT.FILL, SWT.CENTER, true, false)).layout(new GridLayout(2, false)).key("dialog.template.mountingoverview").build();
		mountingpointsList_ = List_.on(mountGroup).build();

		Group executeGroup = Group_.on(composite).layoutData(new GridData(SWT.FILL, SWT.FILL, true, true)).layout(new FillLayout()).key("dialog.template.execute").build();

		ExpandBar dosBooterExpandBar = createExpandBar(executeGroup);

		Composite booterComposite = Composite_.on(dosBooterExpandBar).build();
		Composite dosComposite = Composite_.on(dosBooterExpandBar).build();

		booterExpandItem_ = createExpandItem(dosBooterExpandBar, "dialog.template.booter", false, booterComposite);
		dosExpandItem_ = createExpandItem(dosBooterExpandBar, "dialog.template.dos", false, dosComposite);

		dosBooterExpandBar.addExpandListener(new ExpandAdapter() {
			@Override
			public void itemCollapsed(ExpandEvent e) {
				dosBooterExpandBar.getItem((((ExpandItem)e.item).getText().equals(text_.get("dialog.template.dos"))) ? 0: 1).setExpanded(true);
				display_.asyncExec(composite::layout);
			}

			@Override
			public void itemExpanded(ExpandEvent e) {
				dosBooterExpandBar.getItem((((ExpandItem)e.item).getText().equals(text_.get("dialog.template.dos"))) ? 0: 1).setExpanded(false);
				display_.asyncExec(composite::layout);
			}
		});

		mountingpointsList_.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDoubleClick(MouseEvent event) {
				if (mountingpointsList_.getSelectionIndex() == -1) {
					doAddMount(shell_, booterExpandItem_.getExpanded(), mountingpointsList_, configurable);
				} else {
					doEditMount(shell_, mountingpointsList_, configurable);
				}
			}
		});
		Composite mntButComp = Composite_.on(mountGroup).innerLayout(1).vertSpacing().build();
		Chain.on(mntButComp).but(b -> b.text().key("dialog.template.mount.add").listen(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				doAddMount(shell_, booterExpandItem_.getExpanded(), mountingpointsList_, configurable);
			}
		})).but(b -> b.text().key("dialog.template.mount.edit").listen(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				doEditMount(shell_, mountingpointsList_, configurable);
			}
		})).but(b -> b.text().key("dialog.template.mount.remove").listen(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				doRemoveMount(configurable);
			}
		})).build();

		if (multiEdit)
			Arrays.asList(mntButComp.getChildren()).forEach(x -> x.setEnabled(false));
	}

	public static void doAddMount(Shell shell, boolean floppy, org.eclipse.swt.widgets.List mountingpoints, TemplateProfileBase configurable) {
		char drive;
		try {
			drive = DriveLetterHelper.getFirstAvailable(floppy, configurable.getNettoMountedDrives());
		} catch (DrivelettersExhaustedException e) {
			// nothing we can do, just take a sensible default
			drive = 'C';
		}
		EditMountDialog addMountDialog = new EditMountDialog(shell, null, drive);
		String mount = addMountDialog.open();
		if (mount != null) {
			configurable.addMount(mount);
			mountingpoints.setItems(configurable.getMountStringsForUI());
			mountingpoints.select(mountingpoints.getItemCount() - 1);
		}
	}

	public static void doEditMount(Shell shell, org.eclipse.swt.widgets.List mountingpoints, TemplateProfileBase configurable) {
		int mounts = mountingpoints.getItemCount();
		int sel = mountingpoints.getSelectionIndex();
		if (sel == -1 && mounts == 1) {
			sel = 0;
			mountingpoints.select(sel);
		}
		if ((sel != -1) && (!configurable.getMountingPointsForUI().get(sel).isUnmounted())) {
			EditMountDialog editMountDialog = new EditMountDialog(shell, mountingpoints.getItem(sel), 'C');
			String mount = editMountDialog.open();
			if (mount != null) {
				configurable.editMountBasedOnIndexUI(sel, mount);
				mountingpoints.setItems(configurable.getMountStringsForUI());
				if (mountingpoints.getItemCount() == mounts) {
					mountingpoints.select(sel);
				} else {
					mountingpoints.select(mountingpoints.getItemCount() - 1);
				}
			}
		}
	}

	protected void doRemoveMount(TemplateProfileBase configurable) {
		doRemoveMount(mountingpointsList_, configurable);
	}

	public static void doRemoveMount(org.eclipse.swt.widgets.List mountingpoints, TemplateProfileBase configurable) {
		int mounts = mountingpoints.getItemCount();
		int sel = mountingpoints.getSelectionIndex();
		if (sel == -1 && mounts == 1) {
			sel = 0;
			mountingpoints.select(sel);
		}
		if (sel != -1) {
			configurable.removeMountBasedOnIndexUI(sel);
			mountingpoints.setItems(configurable.getMountStringsForUI());
			if (mountingpoints.getItemCount() == mounts) {
				mountingpoints.select(sel);
			} else {
				if (mountingpoints.getItemCount() > 0) {
					mountingpoints.select(mountingpoints.getItemCount() - 1);
				}
			}
		}
	}

	private CTabFolder createSubTabs(String titleKey, int numColumns1, int numColumns2) {
		return createSubTabs(titleKey, "dialog.template.tab.releaseoptions", numColumns1, "dialog.template.tab.experimentaloptions", numColumns2);
	}

	private CTabFolder createSubTabs(String titleKey, String key1, int numColumns1, String key2, int numColumns2) {
		CTabFolder subTabFolder = CTabFolder_.on(createTabWithComposite(titleKey, new FillLayout())).ctrl();
		Composite_.on(subTabFolder).layout(new GridLayout(numColumns1, false)).tab(key1).build();
		Composite_.on(subTabFolder).layout(new GridLayout(numColumns2, false)).tab(key2).build();
		return subTabFolder;
	}

	private CTabFolder createSubTabs(String titleKey, String key1, int numColumns1, String key2, int numColumns2, String key3, int numColumns3) {
		CTabFolder subTabFolder = CTabFolder_.on(createTabWithComposite(titleKey, new FillLayout())).ctrl();
		Composite_.on(subTabFolder).layout(new GridLayout(numColumns1, false)).tab(key1).build();
		Composite_.on(subTabFolder).layout(new GridLayout(numColumns2, false)).tab(key2).build();
		Composite_.on(subTabFolder).layout(new GridLayout(numColumns3, false)).tab(key3).build();
		return subTabFolder;
	}

	protected void validateMounts(TemplateProfileBase configurable, Mess_.Builder mess) {
		if (mess.noErrors()) {
			String msg = null;
			switch (configurable.checkMounts()) {
				case INVALID_OVERLAY_NO_BASEDRIVE:
					msg = text_.get("dialog.template.notice.invalidmounts.overlaynobasedrive");
					break;
				case INVALID_OVERLAY_MIXED_ABSREL_PATHS:
					msg = text_.get("dialog.template.notice.invalidmounts.overlaymixedpaths");
					break;
				case INVALID_OVERLAY_PATHS_EQUAL:
					msg = text_.get("dialog.template.notice.invalidmounts.overlaypathsequal");
					break;
				default:
			}
			if ((msg != null) && !Mess_.on(shell_).txt(msg + StringUtils.LF + text_.get("dialog.template.notice.invalidmounts.ignore")).confirm()) {
				mess.txt(msg).bind(mountingpointsList_, getTabItemByControl(mountingpointsList_));
			}
		}
	}
}
