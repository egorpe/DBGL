package org.dbgl.gui.controls;

import org.dbgl.service.SettingsService;
import org.eclipse.swt.graphics.Color;


public class DarkTheme {

	public static final Color defaultForeground = new Color(224, 224, 224);

	public static final Color dialogBackground = new Color(81, 86, 88);
	public static final Color inputBackground = new Color(47, 47, 47);

	public static final Color toolbarBackground = new Color(71, 76, 78);

	public static final Color tableHeaderBackground = new Color(56, 61, 63);
	public static final Color tableHeaderForeground = new Color(204, 204, 204);
	public static final Color tableHighlightedBackground = new Color(80, 80, 20);

	public static final Color tabForeground = new Color(187, 187, 187);
	public static final Color tabSelectedForeground = new Color(247, 248, 248);

	public static final Color linkForeground = new Color(111, 197, 238);

	public static final Color conflictingForeground = new Color(138, 201, 242);
	public static final Color changedForeground = new Color(108, 210, 17);

	private DarkTheme() {
	}

	public static boolean forced() {
		return SettingsService.getInstance().getIntValue("gui", "theme") == 1;
	}
}
