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

import org.dbgl.gui.abstractdialog.SizeControlledButtonDialog;
import org.dbgl.gui.controls.Chain;
import org.dbgl.gui.controls.Group_;
import org.dbgl.model.MixerCommand;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell;


public class EditMixerDialog extends SizeControlledButtonDialog<String> {

	private final MixerCommand mixerCommand_;

	public EditMixerDialog(Shell parent, String command) {
		super(parent, "mixerdialog");

		mixerCommand_ = new MixerCommand(command);
	}

	@Override
	protected String getDialogTitle() {
		return text_.get("dialog.mixer.title");
	}

	@Override
	protected void onShellCreated() {
		contents_.setLayout(new GridLayout(MixerCommand.CHANNELS.size(), true));

		for (String channelName: MixerCommand.CHANNELS) {
			Group channelGroup = Group_.on(contents_).layout(new GridLayout(2, false)).layoutData(new GridData(SWT.FILL, SWT.FILL, true, true)).key("dialog.mixer." + channelName).build();
			Chain chnChannel = Chain.on(channelGroup).lbl(l -> l).lbl(l -> l).scl(
				s -> s.vertical().select(MixerCommand.MAX_VOLUME_LEVEL - mixerCommand_.getVolumeFor(channelName).getLeft()).max(MixerCommand.MAX_VOLUME_LEVEL).pageIncr(10)).scl(
					s -> s.vertical().select(MixerCommand.MAX_VOLUME_LEVEL - mixerCommand_.getVolumeFor(channelName).getRight()).max(MixerCommand.MAX_VOLUME_LEVEL).pageIncr(10)).but(
						b -> b.horSpan(2).key("dialog.mixer.lockbalance").select(mixerCommand_.getVolumeFor(channelName).getLeft() == mixerCommand_.getVolumeFor(channelName).getRight())).build();
			Label left = chnChannel.getLabels().get(0);
			Label right = chnChannel.getLabels().get(1);
			Scale scaleLeft = chnChannel.getScales().get(0);
			Scale scaleRight = chnChannel.getScales().get(1);

			setVolumeBar(left, right, scaleLeft, scaleRight);

			scaleLeft.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent arg0) {
					if (chnChannel.getButton().getSelection()) {
						scaleRight.setSelection(scaleLeft.getSelection());
					}
					mixerCommand_.setVolumeFor(channelName, MixerCommand.MAX_VOLUME_LEVEL - scaleLeft.getSelection(), MixerCommand.MAX_VOLUME_LEVEL - scaleRight.getSelection());
					setVolumeBar(left, right, scaleLeft, scaleRight);
				}
			});

			scaleRight.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent arg0) {
					if (chnChannel.getButton().getSelection()) {
						scaleLeft.setSelection(scaleRight.getSelection());
					}
					mixerCommand_.setVolumeFor(channelName, MixerCommand.MAX_VOLUME_LEVEL - scaleLeft.getSelection(), MixerCommand.MAX_VOLUME_LEVEL - scaleRight.getSelection());
					setVolumeBar(left, right, scaleLeft, scaleRight);
				}
			});
		}

		createOkCancelButtons(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				result_ = mixerCommand_.toString();
				shell_.close();
			}
		});
	}

	private void setVolumeBar(Label left, Label right, Scale scaleLeft, Scale scaleRight) {
		left.setText(text_.get("dialog.mixer.leftchannelvolume", new Integer[] {MixerCommand.MAX_VOLUME_LEVEL - scaleLeft.getSelection()}));
		left.pack();
		right.setText(text_.get("dialog.mixer.rightchannelvolume", new Integer[] {MixerCommand.MAX_VOLUME_LEVEL - scaleRight.getSelection()}));
		right.pack();
	}
}
