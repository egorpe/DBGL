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

import org.dbgl.service.SettingsService;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Shell;


public abstract class SizeControlledDialog<T> extends BaseDialog<T> {

	private final String dialogName_;
	private boolean listenerEnabled_;

	protected final SettingsService settings_;

	protected SizeControlledDialog(Shell parent, int shellStyle, String dialogName) {
		super(parent, shellStyle);

		dialogName_ = dialogName;
		listenerEnabled_ = true;

		settings_ = SettingsService.getInstance();
	}

	protected SizeControlledDialog(Shell parent, String dialogName) {
		this(parent, SWT.TITLE | SWT.CLOSE | SWT.BORDER | SWT.RESIZE | SWT.APPLICATION_MODAL, dialogName);
	}

	@Override
	protected void onShellInit() {
		if (shellStyle_ == SWT.PRIMARY_MODAL) {
			if (settings_.getBooleanValue("gui", "maximized")) {
				shell_.setMaximized(true);
			} else {
				shell_.setLocation(settings_.getIntValue("gui", "x"), settings_.getIntValue("gui", "y"));
				shell_.setSize(settings_.getIntValue("gui", "width"), settings_.getIntValue("gui", "height"));
			}

			shell_.addControlListener(new ControlAdapter() {
				@Override
				public void controlMoved(ControlEvent event) {
					if (!shell_.getMaximized()) {
						Rectangle rec = shell_.getBounds();
						settings_.setIntValue("gui", "x", rec.x);
						settings_.setIntValue("gui", "y", rec.y);
					}
				}
			});
		} else {
			shell_.setSize(settings_.getIntValue("gui", dialogName_ + "_width"), settings_.getIntValue("gui", dialogName_ + "_height"));
		}

		shell_.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent event) {
				if (listenerEnabled_) {
					boolean isMaximized = shell_.getMaximized();
					if (!isMaximized) {
						Rectangle rec = shell_.getBounds();
						if (shellStyle_ == SWT.PRIMARY_MODAL) {
							settings_.setIntValue("gui", "width", rec.width);
							settings_.setIntValue("gui", "height", rec.height);
							settings_.setIntValue("gui", "x", rec.x);
							settings_.setIntValue("gui", "y", rec.y);
						} else {
							settings_.setIntValue("gui", dialogName_ + "_width", rec.width);
							settings_.setIntValue("gui", dialogName_ + "_height", rec.height);
						}
					}
					if (shellStyle_ == SWT.PRIMARY_MODAL)
						settings_.setBooleanValue("gui", "maximized", isMaximized);
				}
			}
		});
	}

	public void setListenerEnabled(boolean listenerEnabled) {
		listenerEnabled_ = listenerEnabled;
	}
}
