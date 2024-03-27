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
package org.dbgl.gui.dialog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.dbgl.gui.abstractdialog.EditProfileDialog;
import org.dbgl.gui.controls.Chain;
import org.dbgl.gui.controls.Mess_;
import org.dbgl.gui.dialog.wizard.UpdateMultiProfileDialog;
import org.dbgl.model.NativeCommand;
import org.dbgl.model.aggregate.DosboxVersion;
import org.dbgl.model.aggregate.Profile;
import org.dbgl.model.aggregate.Template;
import org.dbgl.model.conf.Configuration;
import org.dbgl.model.entity.TemplateProfileBase;
import org.dbgl.model.factory.ProfileFactory;
import org.dbgl.model.repository.BaseRepository;
import org.dbgl.util.searchengine.WebSearchEngine;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;


public class EditMultiProfileDialog extends EditProfileDialog<List<Profile>> {

	private static final boolean MULTI_EDIT = true;

	private List<Profile> profiles_, orgProfiles_;
	private Profile profile_;
	private boolean dbversionChanged_, templateReloaded_;

	public EditMultiProfileDialog(Shell parent, List<Profile> profiles) {
		super(parent);
		profiles_ = profiles;
		orgProfiles_ = profiles_.stream().map(ProfileFactory::createCopy).toList();
	}

	@Override
	protected String getDialogTitle() {
		return text_.get("dialog.multiprofile.title.edit", new Object[] {profiles_.size()});
	}

	@Override
	protected boolean prepare() {
		if (!super.prepare())
			return false;

		try {
			StringBuilder warningsLog = new StringBuilder();

			profile_ = ProfileFactory.combine(dbversionsList_, profiles_, warningsLog);
			dbversionIndex_ = BaseRepository.indexOf(dbversionsList_, profile_.getDosboxVersion());
			templateIndex_ = -1;

			if (StringUtils.isNotEmpty(warningsLog)) {
				Mess_.on(getParent()).txt(warningsLog.toString()).warning();
			}
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	protected void doPerformDosboxConfAction(DosboxConfAction action, DosboxVersion newDosboxVersion) {
		try {
			StringBuilder warningsLog = new StringBuilder();

			for (Profile prof: profiles_) {
				Configuration combinedConf = prof.getCombinedConfiguration();
				metaControls_.stream().filter(Chain::hasChangedValue).forEach(x -> x.updateConfigurableByControl(prof, combinedConf));
				Stream.of(nativeCommandControl_).filter(Chain::hasChangedValue).forEach(x -> x.updateConfigurableByControl(prof, combinedConf));

				warningsLog.append(newDosboxVersion.resetAndLoadConfiguration());

				if (action == DosboxConfAction.SET) {
					warningsLog.append(prof.setToDosboxVersion(newDosboxVersion));
					dbversionChanged_ = true;
				} else if (action == DosboxConfAction.SWITCH) {
					warningsLog.append(prof.switchToDosboxVersion(newDosboxVersion));
					dbversionChanged_ = true;
				} else if (action == DosboxConfAction.RELOAD) {
					warningsLog.append(prof.reloadDosboxVersion(newDosboxVersion));
					dbversionChanged_ = true;
				} else if (action == DosboxConfAction.LOAD_TEMPLATE) {
					Template templ = templatesList_.get(templateCombo_.getSelectionIndex());
					templ.setDosboxVersion(newDosboxVersion);
					warningsLog.append(templ.resetAndLoadConfiguration());
					warningsLog.append(prof.loadTemplate(newDosboxVersion, templ));
				} else if (action == DosboxConfAction.RELOAD_TEMPLATE) {
					Template templ = templatesList_.get(templateCombo_.getSelectionIndex());
					templ.setDosboxVersion(newDosboxVersion);
					warningsLog.append(templ.resetAndLoadConfiguration());
					warningsLog.append(prof.reloadTemplate(newDosboxVersion, templ));
					templateReloaded_ = true;
				}
			}

			profile_ = ProfileFactory.combine(dbversionsList_, profiles_, warningsLog);

			updateControlsByConfigurable(profile_);

			if (StringUtils.isNotEmpty(warningsLog)) {
				Mess_.on(getParent()).txt(warningsLog.toString()).warning();
			}
		} catch (IOException e) {
			Mess_.on(getParent()).exception(e).warning();
		}
	}

	@Override
	protected void updateControlsByConfigurable(TemplateProfileBase configurable) {
		super.updateControlsByConfigurable(configurable);

		loadfixValue_.setEnabled(loadfix_.getSelection());
	}

	@Override
	protected void onShellCreated() {
		createInfoTab();
		createGeneralTab(text_.get("dialog.profile.automatic"),
			SettingsDialog.getConfLocations().get(settings_.getIntValue("profiledefaults", "confpath")) + ", " + SettingsDialog.getConfFilenames().get(settings_.getIntValue("profiledefaults", "conffile")));
		createDisplayTab();
		createMachineTab();
		createAudioTab();
		createIOTab();
		createCustomCommandsTab();
		createMountingTab(profile_, MULTI_EDIT);
		createOkCancelButtons();

		metaControls_.forEach(Chain::multiEdit);
		nativeCommandControl_.multiEdit();

		updateControlsByConfigurable(profile_);

		if (profile_.getNativeCommands() == null) {
			List<NativeCommand> nativeCommands = new ArrayList<>();
			NativeCommand.insertDosboxCommand(nativeCommands);
			nativeCommandControl_.setControlByNativeCommands(nativeCommands);
		}

		metaControls_.forEach(Chain::bindListenersAndSetLabelColor);
		nativeCommandControl_.bindListenersAndSetLabelColor();
	}

	@Override
	protected ToolBar createInfoTab() {
		ToolBar toolBar = super.createInfoTab();
		engineSelector_.addToggleSelectionListener(false);
		return toolBar;
	}

	protected void createOkCancelButtons() {
		createOkCancelButtons(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				if (!isValid()) {
					return;
				}
				try {
					UpdateMultiProfileDialog empDialog = new UpdateMultiProfileDialog(shell_, orgProfiles_, profiles_, dbversionChanged_, templateReloaded_, metaControls_, nativeCommandControl_,
							engineSelector_.isSelected() ? WebSearchEngine.getBySimpleName(settings_.getValue("gui", "searchengine")): null);

					if (empDialog.open() != null) {
						result_ = profiles_;
						shell_.close();
					}
				} catch (Exception e) {
					Mess_.on(shell_).exception(e).warning();
				}
			}
		});
	}

	protected boolean isValid() {
		Mess_.Builder mess = Mess_.on(shell_);
		if (setButton_.isEnabled()) {
			mess.key("dialog.template.required.dosboxassociation").bind(setButton_, getTabItemByControl(setButton_));
		}
		return mess.valid();
	}
}
