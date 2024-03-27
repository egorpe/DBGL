package exodos;

import org.apache.commons.lang3.StringUtils;
import org.dbgl.gui.interfaces.PreProgressNotifyable;


class DotProgress implements PreProgressNotifyable {

	private static final int A_DOT_ONCE_PER_X_TICKS = 100;
	
	int counter_;

	public DotProgress(String title) {
		System.out.print(StringUtils.LF + title + " ");
	}

	@Override
	public void setTotal(long total) {
		// not used
	}

	@Override
	public void incrProgress(long progress) {
		if (counter_++ % A_DOT_ONCE_PER_X_TICKS == 0)
			System.out.print('.');
	}

	@Override
	public void setProgress(long progress) {
		// not used
	}

	@Override
	public void setPreProgress(long preProgress) {
		// not used
	}
}
