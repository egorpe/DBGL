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
package org.dbgl.model.conf;

public class GenerationAwareConfiguration extends Configuration {

	public enum Generation {
		GEN_063, GEN_065, GEN_070, GEN_073
	}

	public GenerationAwareConfiguration() {
		super();
	}

	public Generation getGeneration() {
		if (isDosboxVersion073OrAbove())
			return Generation.GEN_073;
		if (isDosboxVersion070or071or072())
			return Generation.GEN_070;
		if (isDosboxVersion065())
			return Generation.GEN_065;
		if (isDosboxVersion063())
			return Generation.GEN_063;
		return null;
	}

	public boolean isDosboxVersion073OrAbove() {
		return hasValue("cpu", "cputype") && hasValue("midi", "mididevice") && hasValue("midi", "midiconfig") && hasValue("sblaster", "sbmixer") && hasValue("sblaster", "oplemu")
				&& hasValue("gus", "gusirq") && hasValue("gus", "gusdma");
	}

	public boolean isDosboxVersion070or071or072() {
		return hasValue("joystick", "joysticktype") && hasValue("joystick", "timed") && hasValue("joystick", "autofire") && hasValue("joystick", "swap34") && hasValue("joystick", "buttonwrap")
				&& hasValue("dos", "keyboardlayout");
	}

	public boolean isDosboxVersion065() {
		return hasValue("sdl", "windowresolution") && hasValue("sdl", "usescancodes") && hasValue("sblaster", "sbtype") && hasValue("sblaster", "sbbase") && hasValue("gus", "gusrate")
				&& hasValue("gus", "gusbase") && hasValue("speaker", "tandy") && hasValue("bios", "joysticktype") && hasValue("serial", "serial1") && hasValue("dos", "umb");
	}

	public boolean isDosboxVersion063() {
		return hasValue("sdl", "fullfixed") && hasValue("sdl", "hwscale") && hasValue("midi", "intelligent") && hasValue("sblaster", "type") && hasValue("sblaster", "base") && hasValue("gus", "rate")
				&& hasValue("gus", "base") && hasValue("modem", "modem") && hasValue("modem", "comport") && hasValue("modem", "listenport") && hasValue("directserial", "directserial");
	}

	public void downgradeOneGeneration(Generation generation) {
		switch (generation) {
			case GEN_073: // towards Gen_070
				switchSetting("gus", "gusirq", "gus", "irq1");
				if (hasValue("gus", "irq1")) {
					setValue("gus", "irq2", getValue("gus", "irq1"));
				}
				switchSetting("gus", "gusdma", "gus", "dma1");
				if (hasValue("gus", "dma1")) {
					setValue("gus", "dma2", getValue("gus", "dma1"));
				}
				if (hasValue("dosbox", "machine")) {
					String mach = getValue("dosbox", "machine");
					if (!(mach.equalsIgnoreCase("cga") || mach.equalsIgnoreCase("hercules") || mach.equalsIgnoreCase("pcjr") || mach.equalsIgnoreCase("tandy"))) {
						// if machine was NOT set to cga/hercules/pcjr/tandy, remove the value to have it reset
						removeValue("dosbox", "machine");
					}
				}
				if (hasValue("dos", "keyboardlayout") && getValue("dos", "keyboardlayout").equalsIgnoreCase("auto")) {
					// if keyboard layout was set to auto, remove the value to have it reset
					removeValue("dos", "keyboardlayout");
				}
				switchSetting("midi", "mididevice", "midi", "device");
				switchSetting("midi", "midiconfig", "midi", "config");
				switchSetting("sblaster", "sbmixer", "sblaster", "mixer");
				break;
			case GEN_070: // towards Gen_065
				switchSetting("joystick", "joysticktype", "bios", "joysticktype");
				break;
			case GEN_065: // towards Gen_063
				if (hasValue("midi", "mpu401")) {
					String mpu = getValue("midi", "mpu401");
					setValue("midi", "mpu401", !mpu.equalsIgnoreCase("none"));
					setValue("midi", "intelligent", mpu.equalsIgnoreCase("intelligent"));
				}
				switchSetting("sblaster", "sbtype", "sblaster", "type");
				switchSetting("sblaster", "sbbase", "sblaster", "base");
				switchSetting("gus", "gusrate", "gus", "rate");
				switchSetting("gus", "gusbase", "gus", "base");
				break;
			default:
				throw new RuntimeException("Cannot downgrade below generation Gen_063");
		}
	}

	public void upgradeOneGeneration(Generation generation) {
		switch (generation) {
			case GEN_063: // towards Gen_065
				boolean mpu = !hasValue("midi", "mpu401") || getBooleanValue("midi", "mpu401");
				boolean intelli = !hasValue("midi", "intelligent") || getBooleanValue("midi", "intelligent");
				setValue("midi", "mpu401", mpu ? (intelli ? "intelligent": "uart"): "none");
				removeValue("midi", "intelligent");
				switchSetting("sblaster", "type", "sblaster", "sbtype");
				switchSetting("sblaster", "base", "sblaster", "sbbase");
				switchSetting("gus", "rate", "gus", "gusrate");
				switchSetting("gus", "base", "gus", "gusbase");
				break;
			case GEN_065: // towards Gen_070
				switchSetting("bios", "joysticktype", "joystick", "joysticktype");
				break;
			case GEN_070: // towards Gen_073
				switchSetting("gus", "irq1", "gus", "gusirq");
				if (hasValue("gus", "irq2"))
					removeValue("gus", "irq2");
				switchSetting("gus", "dma1", "gus", "gusdma");
				if (hasValue("gus", "dma2"))
					removeValue("gus", "dma2");
				if (hasValue("dosbox", "machine") && getValue("dosbox", "machine").equalsIgnoreCase("vga")) {
					// if machine was set to vga, remove the value to have it reset
					removeValue("dosbox", "machine");
				}
				if (hasValue("dos", "keyboardlayout") && getValue("dos", "keyboardlayout").equalsIgnoreCase("none")) {
					// if keyboard layout was set to none, remove the value to have it reset
					removeValue("dos", "keyboardlayout");
				}
				switchSetting("midi", "device", "midi", "mididevice");
				switchSetting("midi", "config", "midi", "midiconfig");
				switchSetting("sblaster", "mixer", "sblaster", "sbmixer");
				break;
			default:
				throw new RuntimeException("Cannot upgrade above generation Gen_073");
		}
	}
}
