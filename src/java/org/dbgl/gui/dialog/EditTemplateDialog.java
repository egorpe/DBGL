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
import org.apache.commons.lang3.StringUtils;
import org.dbgl.gui.abstractdialog.EditConfigurableDialog;
import org.dbgl.gui.controls.Chain;
import org.dbgl.gui.controls.Label_;
import org.dbgl.gui.controls.Mess_;
import org.dbgl.model.aggregate.DosboxVersion;
import org.dbgl.model.aggregate.Template;
import org.dbgl.model.entity.TemplateProfileBase;
import org.dbgl.model.factory.TemplateFactory;
import org.dbgl.model.repository.BaseRepository;
import org.dbgl.model.repository.TemplateRepository;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;


public class EditTemplateDialog extends EditConfigurableDialog<Template> {

	private static final boolean MULTI_EDIT = false;

	private Text title_;

	private Template template_;
	private final boolean editing_;

	public EditTemplateDialog(Shell parent, Template template) {
		super(parent, "templatedialog");
		template_ = template;
		editing_ = template_ != null;
	}

	@Override
	protected String getDialogTitle() {
		return editing_ ? text_.get("dialog.template.title.edit", new Object[] {template_.getTitle(), template_.getId()}): text_.get("dialog.template.title.add");
	}

	@Override
	protected boolean prepare() {
		if (!super.prepare())
			return false;

		try {
			if (editing_) {
				dbversionIndex_ = BaseRepository.indexOf(dbversionsList_, template_.getDosboxVersion());
			} else {
				template_ = TemplateFactory.create(BaseRepository.findDefault(dbversionsList_));
			}
			String warningsLog = template_.resetAndLoadConfiguration();
			if (StringUtils.isNotBlank(warningsLog))
				Mess_.on(getParent()).txt(warningsLog).warning();
			return true;
		} catch (IOException e) {
			Mess_.on(getParent()).exception(e).warning();
			return false;
		}
	}

	@Override
	protected void doPerformDosboxConfAction(DosboxConfAction action, DosboxVersion newDosboxVersion) {
		try {
			updateConfigurableByControls(template_);

			StringBuilder warningsLog = new StringBuilder(newDosboxVersion.resetAndLoadConfiguration());

			if (action == DosboxConfAction.SET)
				warningsLog.append(template_.setToDosboxVersion(newDosboxVersion));
			else if (action == DosboxConfAction.SWITCH)
				warningsLog.append(template_.switchToDosboxVersion(newDosboxVersion));
			else if (action == DosboxConfAction.RELOAD)
				warningsLog.append(template_.reloadDosboxVersion(newDosboxVersion));

			updateControlsByConfigurable(template_);

			if (StringUtils.isNotEmpty(warningsLog)) {
				Mess_.on(getParent()).txt(warningsLog.toString()).warning();
			}
		} catch (IOException e) {
			Mess_.on(getParent()).exception(e).warning();
		}
	}

	@Override
	protected void onShellCreated() {
		createInfoTab();
		createGeneralTab(text_.get("dialog.profile.automatic"), editing_ ? template_.getConfigurationCanonicalFile().getPath(): SettingsDialog.getConfFilenames().get(0));
		createDisplayTab();
		createMachineTab();
		createAudioTab();
		createIOTab();
		createCustomCommandsTab();
		createMountingTab(template_, MULTI_EDIT);
		createOkCancelButtons();

		updateControlsByConfigurable(template_);

		if (!editing_)
			title_.setFocus();
	}

	protected void createInfoTab() {
		Composite composite = createTabWithComposite("dialog.template.tab.info", 2);

		title_ = Chain.on(composite).lbl(l -> l.key("dialog.template.title")).txt(t -> t).template(Template::getTitle, Template::setTitle).build(metaControls_).getText();
		Chain.on(composite).lbl(l -> l.key("dialog.template.default")).but(b -> b).template(Template::getDefault, Template::setDefault).build(metaControls_);
	}

	@Override
	protected void createMountingTab(TemplateProfileBase configurable, boolean mltiEdit) {
		super.createMountingTab(configurable, mltiEdit);

		Composite booterComposite = (Composite)booterExpandItem_.getControl();
		booterComposite.setLayout(new GridLayout());
		Label_.on(booterComposite).key("dialog.profile.booterimage1").build();

		Composite dosComposite = (Composite)dosExpandItem_.getControl();
		dosComposite.setLayout(new GridLayout());
		Label_.on(dosComposite).key("dialog.profile.mainexe").build();
	}

	protected void createOkCancelButtons() {
		createOkCancelButtons(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				if (!isValid()) {
					return;
				}
				try {
					updateConfigurableByControls(template_);

					if (editing_) {
						new TemplateRepository().update(template_);
						result_ = template_;
					} else {
						result_ = new TemplateRepository().add(template_);
					}
				} catch (Exception e) {
					Mess_.on(shell_).exception(e).warning();
				}
				shell_.close();
			}
		});
	}

	private boolean isValid() {
		Mess_.Builder mess = Mess_.on(shell_);
		if (StringUtils.isBlank(title_.getText())) {
			mess.key("dialog.template.required.title").bind(title_, getTabItemByControl(title_));
		}
		if (setButton_.isEnabled()) {
			mess.key("dialog.template.required.dosboxassociation").bind(setButton_, getTabItemByControl(setButton_));
		}
		validateMounts(template_, mess);
		return mess.valid();
	}
}
