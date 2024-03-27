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
package org.dbgl.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.StringUtils;


public class MixerCommand {

	public static final List<String> CHANNELS = Arrays.asList("master", "spkr", "sb", "gus", "fm", "disney", "cdaudio");
	public static final int DEFAULT_VOLUME_LEVEL = 100; // percent
	public static final int MAX_VOLUME_LEVEL = 200; // percent

	public static class ChannelVolume {

		private String name_;
		private int left_, right_;

		public ChannelVolume(String name) {
			name_ = name;
			left_ = DEFAULT_VOLUME_LEVEL;
			right_ = DEFAULT_VOLUME_LEVEL;
		}

		public void setVolume(String command) {
			try {
				String[] vols = StringUtils.split(command, ':');
				if (vols.length == 2)
					setVolume(Integer.parseInt(vols[0]), Integer.parseInt(vols[1]));
			} catch (NumberFormatException e) {
				// ignore this command
			}
		}

		private void setVolume(int left, int right) {
			left_ = left;
			right_ = right;
		}

		public int getLeft() {
			return left_;
		}

		public int getRight() {
			return right_;
		}

		@Override
		public String toString() {
			return left_ != DEFAULT_VOLUME_LEVEL || right_ != DEFAULT_VOLUME_LEVEL ? new StringBuffer(name_).append(' ').append(left_).append(':').append(right_).toString(): StringUtils.EMPTY;
		}
	}

	private List<ChannelVolume> volumes_;

	public MixerCommand(String command) {
		volumes_ = new ArrayList<>();
		for (String channel: CHANNELS)
			volumes_.add(new ChannelVolume(channel));

		String[] elements = StringUtils.split(command, ' ');
		for (int i = 0; i < elements.length; i += 2) {
			for (ChannelVolume vol: volumes_) {
				if (elements[i].equalsIgnoreCase(vol.name_) && ((i + 1) < elements.length))
					vol.setVolume(elements[i + 1]);
			}
		}
	}

	public ChannelVolume getVolumeFor(String channel) {
		for (ChannelVolume vol: volumes_) {
			if (channel.equalsIgnoreCase(vol.name_)) {
				return vol;
			}
		}
		return null;
	}

	public void setVolumeFor(String channel, int left, int right) {
		for (ChannelVolume vol: volumes_) {
			if (channel.equalsIgnoreCase(vol.name_)) {
				vol.setVolume(left, right);
			}
		}
	}

	@Override
	public String toString() {
		return StringUtils.join(volumes_, ' ').trim().replaceAll("\\s+", " ");
	}
}
