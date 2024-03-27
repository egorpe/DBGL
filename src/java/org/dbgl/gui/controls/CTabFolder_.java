package org.dbgl.gui.controls;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.widgets.Composite;


public class CTabFolder_ {

	private CTabFolder_() {
	}

	public static Builder on(Composite composite) {
		return new Builder(composite);
	}

	public static final class Builder extends ControlBuilder<Builder> {

		Builder(Composite composite) {
			super(composite, SWT.NONE, SWT.FILL, SWT.CENTER, true, false);
		}

		public CTabFolder ctrl() {
			CTabFolder tabFolder = new CTabFolder(composite_, style_);
			if (DarkTheme.forced()) {
				tabFolder.setBackground(composite_.getBackground());
				tabFolder.setForeground(DarkTheme.tabForeground);
				tabFolder.setSelectionBackground(composite_.getBackground());
				tabFolder.setSelectionForeground(DarkTheme.tabSelectedForeground);
			}
			tabFolder.setLayoutData(layoutData());
			return tabFolder;
		}
	}
}
