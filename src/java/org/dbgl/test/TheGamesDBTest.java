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
package org.dbgl.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.io.IOException;
import java.util.List;
import org.dbgl.model.SearchEngineImageInformation;
import org.dbgl.model.WebProfile;
import org.dbgl.util.searchengine.TheGamesDBSearchEngine;
import org.junit.Test;


public class TheGamesDBTest {

	@Test
	public void testGetEntries() {
		try {
			List<WebProfile> entries1 = TheGamesDBSearchEngine.getInstance().getEntries("doom", new String[] {"pc"});
			assertEquals(25, entries1.size());

			List<WebProfile> entries2 = TheGamesDBSearchEngine.getInstance().getEntries("mario", new String[] {});
			assertEquals(231, entries2.size());

			List<WebProfile> entries3 = TheGamesDBSearchEngine.getInstance().getEntries("noresultsplease", new String[] {"pc"});
			assertEquals(0, entries3.size());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testGetEntryDetailsDoom() {
		try {
			WebProfile doom = new WebProfile();
			doom.setTitle("Doom");
			doom.setPlatform("PC");
			doom.setYear("1993");
			doom.setUrl("http://legacy.thegamesdb.net/api/GetGame.php?id=745");
			doom = TheGamesDBSearchEngine.getInstance().getEntryDetailedInformation(doom);
			assertEquals("iDSoftware", doom.getDeveloperName());
			assertEquals("iDSoftware", doom.getPublisherName());
			assertEquals("Action, Shooter", doom.getGenre());
			assertEquals("1993", doom.getYear());
			assertTrue(doom.getRank() > 85);
			assertEquals("In Doom, a nameless space marine, gets punitively posted to Mars after assaulting a commanding officer, who ordered his unit to fire upon "
					+ "civilians. The Martian marine base acts as security for the Union Aerospace Corporation UAC, a multi-planetary conglomerate, which is performing "
					+ "secret experiments with teleportation by creating gateways between the two moons of Mars, Phobos and Deimos. Suddenly, one of these UAC "
					+ "experiments goes horribly wrong; computer systems on Phobos malfunction, Deimos disappears entirely and \"something fragging evil\" starts "
					+ "pouring out of the gateways, killing or possessing all UAC personnel! \nResponding to a frantic distress call from the overrun scientists, "
					+ "the Martian marine unit is quickly sent to Phobos to investigate, where you, the space marine, are left to guard the hangar with only a pistol "
					+ "while the rest of the group proceeds inside to discover their worst nightmare. As you advance further, terrifying screams echo through the vast "
					+ "halls, followed by a disturbing silence ... it seems, all your buddies are dead and you're all on your own now - fight back, exterminate every "
					+ "evil creature and get your ticket back home to earth!",
				doom.getNotes());

			SearchEngineImageInformation[] images = TheGamesDBSearchEngine.getInstance().getEntryImages(doom, Integer.MAX_VALUE, Integer.MAX_VALUE, true);
			assertEquals(3, images.length);

			images = TheGamesDBSearchEngine.getInstance().getEntryImages(doom, Integer.MAX_VALUE, Integer.MAX_VALUE, false);
			assertEquals(3, images.length);

			assertEquals("back", images[0].getDescription());
			assertEquals("front", images[1].getDescription());

			images = TheGamesDBSearchEngine.getInstance().getEntryImages(doom, 2, 2, true);
			assertEquals(3, images.length);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testGetEntryDetailsZelda() {
		try {
			WebProfile zelda = new WebProfile();
			zelda.setTitle("The Legend of Zelda");
			zelda.setPlatform("Nintendo Entertainment System (NES)");
			zelda.setYear("1986");
			zelda.setUrl("http://legacy.thegamesdb.net/api/GetGame.php?id=113");
			zelda = TheGamesDBSearchEngine.getInstance().getEntryDetailedInformation(zelda);
			assertEquals("Nintendo R&D4", zelda.getDeveloperName());
			assertEquals("Nintendo", zelda.getPublisherName());
			assertEquals("Action, Adventure", zelda.getGenre());
			assertEquals("1986", zelda.getYear());
			assertTrue(zelda.getRank() > 75);
			assertTrue(zelda.getNotes().startsWith("Welcome to the Legend of Zelda."));

			SearchEngineImageInformation[] images = TheGamesDBSearchEngine.getInstance().getEntryImages(zelda, Integer.MAX_VALUE, Integer.MAX_VALUE, true);
			assertEquals(7, images.length);

			assertEquals("back", images[0].getDescription());
			assertEquals("front", images[1].getDescription());
			assertEquals("", images[2].getDescription());
			assertEquals("", images[4].getDescription());
			assertEquals("", images[5].getDescription());
			assertEquals("", images[6].getDescription());

			images = TheGamesDBSearchEngine.getInstance().getEntryImages(zelda, 1, 2, false);
			assertEquals(3, images.length);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
