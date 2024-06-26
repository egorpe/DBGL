/*
 * Copyright (C) 2009,2010 Matthias Treydte <mt@waldheinz.de>
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; If not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.dbgl.util.fat;

/**
 * The boot sector layout as used by the FAT12 / FAT16 variants.
 *
 * @author Matthias Treydte &lt;matthias.treydte at meetwise.com&gt;
 */
final class Fat16BootSector extends BootSector {

	/**
	 * The maximum number of clusters for a FAT12 file system. This is actually the number of clusters where mkdosfs stop complaining about a FAT16 partition having not enough sectors, so it would be
	 * misinterpreted as FAT12 without special handling.
	 *
	 * @see #getNrLogicalSectors()
	 */
	public static final int MAX_FAT12_CLUSTERS = 4084;

	public static final int MAX_FAT16_CLUSTERS = 65524;

	/**
	 * The offset to the sectors per FAT value.
	 */
	public static final int SECTORS_PER_FAT_OFFSET = 0x16;

	/**
	 * The offset to the root directory entry count value.
	 *
	 * @see #getRootDirEntryCount()
	 * @see #setRootDirEntryCount(int)
	 */
	public static final int ROOT_DIR_ENTRIES_OFFSET = 0x11;

	/**
	 * Creates a new {@code Fat16BootSector} for the specified device.
	 *
	 * @param device the {@code BlockDevice} holding the boot sector
	 */
	public Fat16BootSector(BlockDevice device, long offset) {
		super(device, offset);
	}

	/**
	 * Gets the number of sectors/fat for FAT 12/16.
	 *
	 * @return int
	 */
	@Override
	public long getSectorsPerFat() {
		return get16(SECTORS_PER_FAT_OFFSET);
	}

	@Override
	public FatType getFatType() {
		long rootDirSectors = ((getRootDirEntryCount() * 32) + (getBytesPerSector() - 1)) / getBytesPerSector();
		long dataSectors = getSectorCount() - (getNrReservedSectors() + (getNrFats() * getSectorsPerFat()) + rootDirSectors);
		long clusterCount = dataSectors / getSectorsPerCluster();

		if (clusterCount > MAX_FAT16_CLUSTERS)
			throw new IllegalStateException("too many clusters for FAT12/16: " + clusterCount);

		return clusterCount > MAX_FAT12_CLUSTERS ? FatType.FAT16: FatType.FAT12;
	}

	@Override
	public long getSectorCount() {
		return getNrLogicalSectors() == 0 ? getNrTotalSectors(): getNrLogicalSectors();
	}

	/**
	 * Gets the number of entries in the root directory.
	 *
	 * @return int the root directory entry count
	 */
	@Override
	public int getRootDirEntryCount() {
		return get16(ROOT_DIR_ENTRIES_OFFSET);
	}
}
