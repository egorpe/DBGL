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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.dbgl.gui.controls.Button_;
import org.dbgl.gui.controls.Chain;
import org.dbgl.gui.controls.Mess_;
import org.dbgl.gui.controls.SearchEngineSelector;
import org.dbgl.gui.controls.TextControl_;
import org.dbgl.model.SearchEngineImageInformation;
import org.dbgl.model.aggregate.Profile;
import org.dbgl.model.aggregate.Template;
import org.dbgl.model.conf.Autoexec;
import org.dbgl.model.entity.TemplateProfileBase;
import org.dbgl.model.repository.BaseRepository;
import org.dbgl.model.repository.TemplateRepository;
import org.dbgl.model.repository.TitledEntityRepository;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;


public abstract class EditProfileDialog<T> extends EditConfigurableDialog<T> {

	protected String[] developersList_, publishersList_, genresList_, yearsList_, statusList_;
	protected List<String[]> customList_;
	protected List<Template> templatesList_;

	protected Text title_, notes_;
	protected SearchEngineSelector engineSelector_;
	protected Text[] link_, linkTitle_;
	protected Button[] linkBrowseButton_;
	protected Combo developer_, publisher_, genre_, year_;
	protected Scale custom9_;
	protected Combo templateCombo_;
	protected Button loadfix_;
	protected Combo loadfixValue_;
	protected Text img1_, img2_, img3_, main_, setup_;
	protected Composite webImagesSpaceHolder_;
	protected SearchEngineImageInformation[] imageInformation_;
	protected Button[] imgButtons_;

	protected int templateIndex_;

	protected EditProfileDialog(Shell parent) {
		super(parent, "profiledialog");
	}

	@Override
	protected boolean prepare() {
		if (!super.prepare())
			return false;

		try {
			TemplateRepository templRepo = new TemplateRepository();
			TitledEntityRepository titledRepo = new TitledEntityRepository();

			templatesList_ = templRepo.listAll(dbversionsList_);
			developersList_ = titledRepo.developers();
			publishersList_ = titledRepo.publishers();
			genresList_ = titledRepo.genres();
			yearsList_ = titledRepo.years();
			statusList_ = titledRepo.statuses();

			customList_ = new ArrayList<>();
			for (int i = 0; i < Profile.NR_OF_CUSTOM_STRING_DROPDOWNS; i++) {
				customList_.add(titledRepo.customValues(i));
			}

			templateIndex_ = BaseRepository.indexOfDefault(templatesList_);

			return true;
		} catch (Exception e) {
			Mess_.on(getParent()).exception(e).warning();
			return false;
		}
	}

	protected ToolBar createInfoTab() {
		Composite composite = createTabWithComposite("dialog.profile.tab.info", 6);

		title_ = Chain.on(composite).lbl(l -> l.key("dialog.profile.title")).txt(t -> t.horSpan(4)).profile(Profile::getTitle, Profile::setTitle).build(metaControls_).getText();

		ToolBar toolBar = createToolBar(composite);

		engineSelector_ = new SearchEngineSelector(toolBar, false);

		developer_ = Chain.on(composite).lbl(l -> l.key("dialog.profile.developer")).cmb(c -> c.horSpan(2).autoSelect(developersList_)).profile(Profile::getDeveloper, Profile::setDeveloper).build(
			metaControls_).getCombo();
		publisher_ = Chain.on(composite).lbl(l -> l.key("dialog.profile.publisher")).cmb(c -> c.horSpan(2).autoSelect(publishersList_)).profile(Profile::getPublisher, Profile::setPublisher).build(
			metaControls_).getCombo();
		genre_ = Chain.on(composite).lbl(l -> l.key("dialog.profile.genre")).cmb(c -> c.horSpan(2).autoSelect(genresList_)).profile(Profile::getGenre, Profile::setGenre).build(
			metaControls_).getCombo();
		year_ = Chain.on(composite).lbl(l -> l.key("dialog.profile.year")).cmb(c -> c.horSpan(2).autoSelect(yearsList_)).profile(Profile::getYear, Profile::setYear).build(metaControls_).getCombo();

		link_ = new Text[Profile.NR_OF_LINK_DESTINATIONS];
		linkBrowseButton_ = new Button[Profile.NR_OF_LINK_TITLES];
		linkTitle_ = new Text[Profile.NR_OF_LINK_TITLES];

		for (int i = 0; i < Profile.NR_OF_LINK_TITLES / 2; i++) {
			int j = i + 1;
			Chain chnLink = Chain.on(composite).lbl(l -> l.key("dialog.profile.link", new Object[] {j})).txt(t -> t).profile(i, Profile::getLinkDestination, Profile::setLinkDestination).but(
				b -> b.browse(false, Button_.BrowseType.FILE, Button_.CanonicalType.DOC, false)).build(metaControls_);
			link_[i] = chnLink.getText();
			linkBrowseButton_[i] = chnLink.getButton();
			linkTitle_[i] = Chain.on(composite).lbl(l -> l.key("dialog.profile.linktitle")).txt(t -> t.horSpan(2)).profile(i, Profile::getLinkTitle, Profile::setLinkTitle).build(
				metaControls_).getText();
		}

		Chain.on(composite).lbl(l -> l.key("dialog.profile.status")).cmb(c -> c.horSpan(2).autoSelect(statusList_)).profile(Profile::getStatus, Profile::setStatus).build(metaControls_);
		Chain.on(composite).lbl(l -> l.key("dialog.profile.favorite")).but(b -> b.layoutData(new GridData(SWT.FILL, SWT.CENTER, true, false))).lbl(l -> l).profile(Profile::getFavorite,
			Profile::setFavorite).build(metaControls_);

		notes_ = Chain.on(composite).lbl(l -> l.key("dialog.profile.notes")).txt(t -> t.horSpan(5).multi().wrap()).profile(Profile::getNotes, Profile::setNotes).build(metaControls_).getText();
		notes_.setFont(stringToFont(display_, settings_.getValues("gui", "notesfont"), notes_.getFont()));
		notes_.addDisposeListener(e -> notes_.getFont().dispose());

		Composite customComposite = createTabWithComposite("dialog.profile.tab.custominfo", 5);

		for (int i = 0; i < Profile.NR_OF_CUSTOM_STRING_DROPDOWNS; i++) {
			int j = i + 1;
			int k = i;
			Chain.on(customComposite).lbl(l -> l.txt(settings_.getValue("gui", "custom" + j))).cmb(c -> c.horSpan(4).autoSelect(customList_.get(k))).profile(i, Profile::getCustomString,
				Profile::setCustomString).build(metaControls_);
		}
		for (int i = 0; i < Profile.NR_OF_CUSTOM_STRING_FIELDS_1; i++) {
			int j = i + 1;
			Chain.on(customComposite).lbl(l -> l.txt(settings_.getValue("gui", "custom" + (j + Profile.NR_OF_CUSTOM_STRING_DROPDOWNS)))).txt(t -> t.horSpan(4)).profile(
				i + Profile.NR_OF_CUSTOM_STRING_DROPDOWNS, Profile::getCustomString, Profile::setCustomString).build(metaControls_);
		}

		custom9_ = Chain.on(customComposite).lbl(l -> l.txt(settings_.getValue("gui", "custom9"))).scl(s -> s.horSpan(2)).profile(0, Profile::getCustomInt, Profile::setCustomInt).build(
			metaControls_).getScale();
		Chain.on(customComposite).lbl(l -> l.txt(settings_.getValue("gui", "custom10"))).spn(s -> s.min(Integer.MIN_VALUE).max(Integer.MAX_VALUE)).profile(1, Profile::getCustomInt,
			Profile::setCustomInt).build(metaControls_);
		for (int i = Profile.NR_OF_LINK_TITLES / 2; i < Profile.NR_OF_LINK_TITLES; i++) {
			int j = i + 1;
			Chain chnLink = Chain.on(customComposite).lbl(l -> l.key("dialog.profile.link", new Object[] {j})).txt(t -> t).profile(i, Profile::getLinkDestination, Profile::setLinkDestination).but(
				b -> b.browse(false, Button_.BrowseType.FILE, Button_.CanonicalType.DOC, false)).build(metaControls_);
			link_[i] = chnLink.getText();
			linkBrowseButton_[i] = chnLink.getButton();
			linkTitle_[i] = Chain.on(customComposite).lbl(l -> l.key("dialog.profile.linktitle")).txt(t -> t).profile(i, Profile::getLinkTitle, Profile::setLinkTitle).build(metaControls_).getText();
		}
		for (int i = 0; i < Profile.NR_OF_CUSTOM_STRING_FIELDS_2; i++) {
			int columns = i % 2 == 0 ? 2: 1;
			int j = i + 1;
			Chain.on(customComposite).lbl(
				l -> l.txt(settings_.getValue("gui", "custom" + (j + Profile.NR_OF_CUSTOM_STRING_DROPDOWNS + Profile.NR_OF_CUSTOM_INTS + Profile.NR_OF_CUSTOM_STRING_FIELDS_1)))).txt(
					t -> t.horSpan(columns)).profile(i + Profile.NR_OF_CUSTOM_STRING_DROPDOWNS + Profile.NR_OF_CUSTOM_STRING_FIELDS_1, Profile::getCustomString, Profile::setCustomString).build(
						metaControls_);
		}

		return toolBar;
	}

	@Override
	protected Group createGeneralTab(String capturesText, String configFileText) {
		Group associationGroup = super.createGeneralTab(capturesText, configFileText);

		templateCombo_ = Chain.on(associationGroup).lbl(l -> l.key("dialog.profile.template")).cmb(
			c -> c.horSpan(2).wide().items(templatesList_.stream().map(Template::getTitle).toArray(String[]::new)).select(templateIndex_).visibleItemCount(20)).combo();

		Button_.on(associationGroup).text().key("dialog.profile.loadsettings").tooltip("dialog.profile.loadsettings.tooltip").listen(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				if (templateCombo_.getSelectionIndex() != -1) {
					if (setButton_.isEnabled()) {
						Mess_.on(shell_).key("dialog.template.required.dosboxassociation").bind(setButton_, getTabItemByControl(setButton_)).valid();
						return;
					}
					doPerformDosboxConfAction(DosboxConfAction.LOAD_TEMPLATE, dbversionsList_.get(dbversionCombo_.getSelectionIndex()));
				}
			}
		}).ctrl();

		Button_.on(associationGroup).text().key("dialog.profile.reloadsettings").tooltip("dialog.profile.reloadsettings.tooltip").listen(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				if (templateCombo_.getSelectionIndex() != -1) {
					if (setButton_.isEnabled()) {
						Mess_.on(shell_).key("dialog.template.required.dosboxassociation").bind(setButton_, getTabItemByControl(setButton_)).valid();
						return;
					}
					doPerformDosboxConfAction(DosboxConfAction.RELOAD_TEMPLATE, dbversionsList_.get(dbversionCombo_.getSelectionIndex()));
				}
			}
		}).ctrl().setEnabled(dbversionIndex_ != -1);

		return associationGroup;
	}

	@Override
	protected Group createMachineTab() {
		Group memoryGroup = super.createMachineTab();

		loadfix_ = Chain.on(memoryGroup).lbl(l -> l.key("dialog.profile.loadfix")).but(b -> b.listen(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				loadfixValue_.setEnabled(loadfix_.getSelection());
			}
		})).autoexec(Autoexec::getLoadfix, Autoexec::setLoadfix).build(metaControls_).getButton();

		loadfixValue_ = Chain.on(memoryGroup).cmb(c -> c.editable().items("profile", "loadfix_value")).lbl(l -> l.key("dialog.profile.kb")).autoexec(Autoexec::getLoadfixValueAsString,
			Autoexec::setLoadfixValue).build(metaControls_).getCombo();

		Chain.on(memoryGroup).lbl(l -> l.key("dialog.profile.loadhigh")).but(b -> b.horSpan(3)).autoexec(Autoexec::getLoadhigh, Autoexec::setLoadhigh).build(metaControls_);

		return memoryGroup;
	}

	@Override
	protected void createMountingTab(TemplateProfileBase configurable, boolean multiEdit) {
		super.createMountingTab(configurable, multiEdit);

		Composite booterComposite = (Composite)booterExpandItem_.getControl();
		booterComposite.setLayout(new GridLayout(4, false));

		Chain img1Chain = Chain.on(booterComposite).lbl(l -> l.key("dialog.profile.booterimage1")).txt(t -> t).autoexec(Autoexec::getImg1, Autoexec::setImg1).but(
			b -> b.browse(false, Button_.BrowseType.FILE, Button_.CanonicalType.BOOTER, false)).but(b -> b.grab(mountingpointsList_, true)).build(metaControls_);
		img1_ = img1Chain.getText();

		Chain img2Chain = Chain.on(booterComposite).lbl(l -> l.key("dialog.profile.booterimage2")).txt(t -> t).autoexec(Autoexec::getImg2, Autoexec::setImg2).but(
			b -> b.browse(false, Button_.BrowseType.FILE, Button_.CanonicalType.BOOTER, false)).but(b -> b.grab(mountingpointsList_, true)).build(metaControls_);
		img2_ = img2Chain.getText();

		Chain img3Chain = Chain.on(booterComposite).lbl(l -> l.key("dialog.profile.booterimage3")).txt(t -> t).autoexec(Autoexec::getImg3, Autoexec::setImg3).but(
			b -> b.browse(false, Button_.BrowseType.FILE, Button_.CanonicalType.BOOTER, false)).but(b -> b.grab(mountingpointsList_, true)).build(metaControls_);
		img3_ = img3Chain.getText();

		Chain.on(booterComposite).lbl(l -> l.key("dialog.profile.booterdriveletter")).cmb(c -> c.horSpan(3).items(new String[] {"", "A", "C", "D"}).visibleItemCount(4)).autoexec(
			Autoexec::getImgDriveletter, Autoexec::setImgDriveletter).build(metaControls_);

		booterExpandItem_.setHeight(booterComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);

		Composite dosComposite = (Composite)dosExpandItem_.getControl();
		dosComposite.setLayout(new GridLayout(7, false));

		main_ = Chain.on(dosComposite).lbl(l -> l.key("dialog.profile.mainexe")).txt(t -> t.horSpan(4)).autoexec(Autoexec::getMain, Autoexec::setMain).but(
			b -> b.browse(false, Button_.BrowseType.FILE, Button_.CanonicalType.EXE, false)).but(b -> b.grab(mountingpointsList_, false)).build(metaControls_).getText();

		Chain.on(dosComposite).lbl(l -> l).lbl(l -> l.key("dialog.profile.mainparameters")).txt(t -> t.horSpan(3)).lbl(l -> l.horSpan(2)).autoexec(Autoexec::getParameters,
			Autoexec::setParameters).build(metaControls_);

		Chain setupChain = Chain.on(dosComposite).lbl(l -> l.key("dialog.profile.setupexe")).txt(t -> t.horSpan(4)).but(
			b -> b.browse(false, Button_.BrowseType.FILE, Button_.CanonicalType.EXE, false)).profile(Profile::getSetupString, Profile::setSetupFileLocation).but(
				b -> b.grab(mountingpointsList_, false)).build(metaControls_);
		setup_ = setupChain.getText();

		Chain.on(dosComposite).lbl(l -> l).lbl(l -> l.key("dialog.profile.setupparameters")).txt(t -> t.horSpan(3)).lbl(l -> l.horSpan(2)).profile(Profile::getSetupParams,
			Profile::setSetupParams).build(metaControls_);

		Text alt1 = Chain.on(dosComposite).lbl(l -> l.key("dialog.profile.altexe", new Object[] {1})).txt(t -> t.horSpan(2)).profile(0, Profile::getAltExeString, Profile::setAltExeFileLocation).build(
			metaControls_).getText();
		Chain alt1Chain = Chain.on(dosComposite).lbl(l -> l).txt(t -> t).but(b -> b.browse(false, Button_.BrowseType.FILE, Button_.CanonicalType.EXE, false)).profile(0, Profile::getAltExeParam,
			Profile::setAltExeParam).but(b -> b.grab(mountingpointsList_, false)).build(metaControls_);

		Text alt2 = Chain.on(dosComposite).lbl(l -> l.key("dialog.profile.altexe", new Object[] {2})).txt(t -> t.horSpan(2)).profile(1, Profile::getAltExeString, Profile::setAltExeFileLocation).build(
			metaControls_).getText();
		Chain alt2Chain = Chain.on(dosComposite).lbl(l -> l).txt(t -> t).but(b -> b.browse(false, Button_.BrowseType.FILE, Button_.CanonicalType.EXE, false)).profile(1, Profile::getAltExeParam,
			Profile::setAltExeParam).but(b -> b.grab(mountingpointsList_, false)).build(metaControls_);

		dosExpandItem_.setHeight(dosComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);

		for (int i = 0; i < Profile.NR_OF_LINK_DESTINATIONS; i++) {
			linkBrowseButton_[i].setData(Button_.DATA_ALT_CONTROL, main_);
		}

		img1Chain.getButton().setData(Button_.DATA_ALT_CONTROL, main_);
		img2Chain.getButton().setData(Button_.DATA_ALT_CONTROL, img1_);
		img3Chain.getButton().setData(Button_.DATA_ALT_CONTROL, img1_);
		setupChain.getButton().setData(Button_.DATA_ALT_CONTROL, main_);
		alt1Chain.getButtons().get(0).setData(Button_.DATA_CONTROL, new TextControl_(alt1));
		alt1Chain.getButtons().get(1).setData(Button_.DATA_CONTROL, new TextControl_(alt1));
		alt1Chain.getButton().setData(Button_.DATA_ALT_CONTROL, main_);
		alt2Chain.getButtons().get(0).setData(Button_.DATA_CONTROL, new TextControl_(alt2));
		alt2Chain.getButtons().get(1).setData(Button_.DATA_CONTROL, new TextControl_(alt2));
		alt2Chain.getButton().setData(Button_.DATA_ALT_CONTROL, main_);

		if (multiEdit) {
			Stream.of(booterComposite.getChildren()).forEach(x -> x.setEnabled(false));
			Stream.of(dosComposite.getChildren()).forEach(x -> x.setEnabled(false));
		}
	}
}
