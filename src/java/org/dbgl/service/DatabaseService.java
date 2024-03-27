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
package org.dbgl.service;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.lang3.StringUtils;
import org.dbgl.gui.controls.Mess_;
import org.dbgl.util.FilesUtils;
import org.dbgl.util.StringRelatedUtils;
import org.eclipse.swt.widgets.Shell;
import org.hsqldb.jdbc.pool.JDBCPooledDataSource;


public class DatabaseService {

	private static final String TEST_QRY = "SELECT TOP 1 ID FROM GAMES";
	private static final String GET_VERSION = "SELECT MAJORVERSION, MINORVERSION FROM VERSION";
	private static final String UP_TO_V050_QRY = "ALTER TABLE GAMES ADD COLUMN CONFFILE VARCHAR(256);" + "ALTER TABLE GAMES ADD COLUMN CAPTURES VARCHAR(256);"
			+ "CREATE TABLE VERSION(MAJORVERSION INTEGER NOT NULL, MINORVERSION INTEGER NOT NULL);" + "INSERT INTO VERSION VALUES(0, 50);" + "UPDATE GAMES SET" + " CAPTURES = '"
			+ FileLocationService.CAPTURES_DIR_STRING + "' || GAMES.ID," + " CONFFILE = '" + FileLocationService.PROFILES_DIR_STRING + "' || GAMES.ID || '" + FilesUtils.CONF_EXT + "';";
	private static final String UP_TO_V051_QRY = "ALTER TABLE DOSBOXVERSIONS ADD COLUMN PARAMETERS VARCHAR(256) DEFAULT '';" + "UPDATE VERSION SET MINORVERSION = 51;";
	private static final String UP_TO_V056_QRY = "ALTER TABLE GAMES ADD COLUMN LINK3 VARCHAR(256) DEFAULT '';" + "ALTER TABLE GAMES ADD COLUMN LINK4 VARCHAR(256) DEFAULT '';"
			+ "ALTER TABLE GAMES ADD COLUMN CUST1_ID INTEGER DEFAULT 0;" + "ALTER TABLE GAMES ADD COLUMN CUST2_ID INTEGER DEFAULT 0;" + "ALTER TABLE GAMES ADD COLUMN CUST3_ID INTEGER DEFAULT 0;"
			+ "ALTER TABLE GAMES ADD COLUMN CUST4_ID INTEGER DEFAULT 0;" + "ALTER TABLE GAMES ADD COLUMN CUSTOM5 VARCHAR(256) DEFAULT '';"
			+ "ALTER TABLE GAMES ADD COLUMN CUSTOM6 VARCHAR(256) DEFAULT '';" + "ALTER TABLE GAMES ADD COLUMN CUSTOM7 VARCHAR(256) DEFAULT '';"
			+ "ALTER TABLE GAMES ADD COLUMN CUSTOM8 VARCHAR(256) DEFAULT '';" + "ALTER TABLE GAMES ADD COLUMN CUSTOM9  INTEGER DEFAULT 0;" + "ALTER TABLE GAMES ADD COLUMN CUSTOM10 INTEGER DEFAULT 0;"
			+ "CREATE TABLE CUSTOM1(ID INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 0) NOT NULL PRIMARY KEY, VALUE VARCHAR(256) NOT NULL);"
			+ "CREATE TABLE CUSTOM2(ID INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 0) NOT NULL PRIMARY KEY, VALUE VARCHAR(256) NOT NULL);"
			+ "CREATE TABLE CUSTOM3(ID INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 0) NOT NULL PRIMARY KEY, VALUE VARCHAR(256) NOT NULL);"
			+ "CREATE TABLE CUSTOM4(ID INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 0) NOT NULL PRIMARY KEY, VALUE VARCHAR(256) NOT NULL);" + "INSERT INTO CUSTOM1(VALUE) VALUES('');"
			+ "INSERT INTO CUSTOM2(VALUE) VALUES('');" + "INSERT INTO CUSTOM3(VALUE) VALUES('');" + "INSERT INTO CUSTOM4(VALUE) VALUES('');" + "UPDATE VERSION SET MINORVERSION = 56;";
	private static final String UP_TO_V062_QRY = "ALTER TABLE DOSBOXVERSIONS ADD COLUMN VERSION VARCHAR(256) DEFAULT '0.72' NOT NULL;" + "UPDATE VERSION SET MINORVERSION = 62;";
	private static final String UP_TO_V065_QRY = "ALTER TABLE GAMES ADD COLUMN LINK1_TITLE VARCHAR(256) DEFAULT '';" + "ALTER TABLE GAMES ADD COLUMN LINK2_TITLE VARCHAR(256) DEFAULT '';"
			+ "ALTER TABLE GAMES ADD COLUMN LINK3_TITLE VARCHAR(256) DEFAULT '';" + "ALTER TABLE GAMES ADD COLUMN LINK4_TITLE VARCHAR(256) DEFAULT '';" + "UPDATE VERSION SET MINORVERSION = 65;";
	private static final String UP_TO_V067_QRY = "ALTER TABLE DOSBOXVERSIONS ADD COLUMN USINGCURSES BOOLEAN;" + "UPDATE VERSION SET MINORVERSION = 67;";
	private static final String UP_TO_V068_QRY = "CREATE MEMORY TABLE FILTERS(ID INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 0) NOT NULL PRIMARY KEY,TITLE VARCHAR(256) NOT NULL,FILTER VARCHAR(256) NOT NULL,CONF_FILTER VARCHAR(256) NOT NULL);"
			+ "UPDATE VERSION SET MINORVERSION = 68;";
	private static final String UP_TO_V072_QRY = "ALTER TABLE DOSBOXVERSIONS ADD COLUMN CONFFILE VARCHAR(256) DEFAULT '' NOT NULL;" + "UPDATE DOSBOXVERSIONS SET CONFFILE = CONCAT(PATH, '"
			+ File.separatorChar + FileLocationService.DOSBOX_CONF_STRING + "');" + "UPDATE VERSION SET MINORVERSION = 72;";
	private static final String UP_TO_V073_QRY = "ALTER TABLE GAMES ADD COLUMN ALT1 VARCHAR(256) DEFAULT '';" + "ALTER TABLE GAMES ADD COLUMN ALT1_PARAMS VARCHAR(256) DEFAULT '';"
			+ "ALTER TABLE GAMES ADD COLUMN ALT2 VARCHAR(256) DEFAULT '';" + "ALTER TABLE GAMES ADD COLUMN ALT2_PARAMS VARCHAR(256) DEFAULT '';"
			+ "ALTER TABLE GAMES ADD COLUMN LINK5 VARCHAR(256) DEFAULT '';" + "ALTER TABLE GAMES ADD COLUMN LINK6 VARCHAR(256) DEFAULT '';"
			+ "ALTER TABLE GAMES ADD COLUMN LINK7 VARCHAR(256) DEFAULT '';" + "ALTER TABLE GAMES ADD COLUMN LINK8 VARCHAR(256) DEFAULT '';"
			+ "ALTER TABLE GAMES ADD COLUMN LINK5_TITLE VARCHAR(256) DEFAULT '';" + "ALTER TABLE GAMES ADD COLUMN LINK6_TITLE VARCHAR(256) DEFAULT '';"
			+ "ALTER TABLE GAMES ADD COLUMN LINK7_TITLE VARCHAR(256) DEFAULT '';" + "ALTER TABLE GAMES ADD COLUMN LINK8_TITLE VARCHAR(256) DEFAULT '';" + "UPDATE VERSION SET MINORVERSION = 73;";
	private static final String UP_TO_V074_QRY = "ALTER TABLE DOSBOXVERSIONS ALTER COLUMN DEFAULT RENAME TO ISDEFAULT;" + "ALTER TABLE TEMPLATES ALTER COLUMN DEFAULT RENAME TO ISDEFAULT;"
			+ "UPDATE VERSION SET MINORVERSION = 74;";
	private static final String UP_TO_V075_QRY = "CREATE MEMORY TABLE NATIVECOMMANDS(ID INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 0) NOT NULL PRIMARY KEY,COMMAND VARCHAR(256) NOT NULL,PARAMETERS VARCHAR(256) NOT NULL,CWD VARCHAR(256) NOT NULL,WAITFOR BOOLEAN,ORDERNR INTEGER,GAME_ID INTEGER,TEMPLATE_ID INTEGER,"
			+ "CONSTRAINT SYS_FK_180 FOREIGN KEY(GAME_ID) REFERENCES GAMES(ID),CONSTRAINT SYS_FK_181 FOREIGN KEY(TEMPLATE_ID) REFERENCES TEMPLATES(ID));" + "UPDATE VERSION SET MINORVERSION = 75;";
	private static final String UP_TO_V076_QRY = "ALTER TABLE GAMES ADD COLUMN STATS_CREATED TIMESTAMP(0) DEFAULT CURRENT_TIMESTAMP NOT NULL;"
			+ "ALTER TABLE GAMES ADD COLUMN STATS_LASTMODIFY TIMESTAMP(0);" + "ALTER TABLE GAMES ADD COLUMN STATS_LASTRUN TIMESTAMP(0);"
			+ "ALTER TABLE GAMES ADD COLUMN STATS_RUNS INTEGER DEFAULT 0 NOT NULL;" + "ALTER TABLE GAMES ADD COLUMN STATS_LASTSETUP TIMESTAMP(0);"
			+ "ALTER TABLE GAMES ADD COLUMN STATS_SETUPS INTEGER DEFAULT 0 NOT NULL;" + "ALTER TABLE DOSBOXVERSIONS ADD COLUMN STATS_CREATED TIMESTAMP(0) DEFAULT CURRENT_TIMESTAMP NOT NULL;"
			+ "ALTER TABLE DOSBOXVERSIONS ADD COLUMN STATS_LASTMODIFY TIMESTAMP(0);" + "ALTER TABLE DOSBOXVERSIONS ADD COLUMN STATS_LASTRUN TIMESTAMP(0);"
			+ "ALTER TABLE DOSBOXVERSIONS ADD COLUMN STATS_RUNS INTEGER DEFAULT 0 NOT NULL;" + "ALTER TABLE TEMPLATES ADD COLUMN STATS_CREATED TIMESTAMP(0) DEFAULT CURRENT_TIMESTAMP NOT NULL;"
			+ "ALTER TABLE TEMPLATES ADD COLUMN STATS_LASTMODIFY TIMESTAMP(0);" + "ALTER TABLE TEMPLATES ADD COLUMN STATS_LASTRUN TIMESTAMP(0);"
			+ "ALTER TABLE TEMPLATES ADD COLUMN STATS_RUNS INTEGER DEFAULT 0 NOT NULL;" + "UPDATE VERSION SET MINORVERSION = 76;";
	private static final String UP_TO_V077_QRY = "CREATE MEMORY TABLE LOG (" + "ID INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 0) NOT NULL PRIMARY KEY,"
			+ "TIME TIMESTAMP(0) DEFAULT CURRENT_TIMESTAMP NOT NULL," + "EVENT TINYINT NOT NULL, ENTITY_TYPE TINYINT NOT NULL," + "ENTITY_ID INT NOT NULL, ENTITY_TITLE VARCHAR(256) NOT NULL);"
			+ "UPDATE VERSION SET MINORVERSION = 77;";
	private static final String UP_TO_V081_QRY = "ALTER TABLE GAMES ADD COLUMN CUSTOM11 VARCHAR(256) DEFAULT ''; ALTER TABLE GAMES ADD COLUMN CUSTOM12 VARCHAR(256) DEFAULT '';"
			+ "ALTER TABLE GAMES ADD COLUMN CUSTOM13 VARCHAR(256) DEFAULT ''; ALTER TABLE GAMES ADD COLUMN CUSTOM14 VARCHAR(256) DEFAULT ''; UPDATE VERSION SET MINORVERSION = 81;";
	private static final String UP_TO_V090_QRY = "ALTER TABLE DOSBOXVERSIONS ADD COLUMN EXEFILE VARCHAR(256) DEFAULT '' NOT NULL;" + "UPDATE VERSION SET MINORVERSION = 90;";
	private static final String UP_TO_V092_QRY = "ALTER TABLE PUBLYEARS ALTER COLUMN YEAR SET DATA TYPE VARCHAR(256);" + "UPDATE VERSION SET MINORVERSION = 92;";
	private static final String UP_TO_V093_QRY = "ALTER TABLE GAMES ADD COLUMN MOUNT_IDX INTEGER DEFAULT 0; ALTER TABLE DOSBOXVERSIONS ADD COLUMN DYNAMIC_OPTIONS OTHER DEFAULT NULL; UPDATE VERSION SET MINORVERSION = 93;";
	private static final String UP_TO_V094A_QRY = "UPDATE GAMES SET LINK1 = RIGHT(LINK1, LENGTH(LINK1) - LENGTH(?) - 1) WHERE LINK1 LIKE ?;"
			+ "UPDATE GAMES SET LINK2 = RIGHT(LINK2, LENGTH(LINK2) - LENGTH(?) - 1) WHERE LINK2 LIKE ?;"
			+ "UPDATE GAMES SET LINK3 = RIGHT(LINK3, LENGTH(LINK3) - LENGTH(?) - 1) WHERE LINK3 LIKE ?;"
			+ "UPDATE GAMES SET LINK4 = RIGHT(LINK4, LENGTH(LINK4) - LENGTH(?) - 1) WHERE LINK4 LIKE ?;"
			+ "UPDATE GAMES SET LINK5 = RIGHT(LINK5, LENGTH(LINK5) - LENGTH(?) - 1) WHERE LINK5 LIKE ?;"
			+ "UPDATE GAMES SET LINK6 = RIGHT(LINK6, LENGTH(LINK6) - LENGTH(?) - 1) WHERE LINK6 LIKE ?;"
			+ "UPDATE GAMES SET LINK7 = RIGHT(LINK7, LENGTH(LINK7) - LENGTH(?) - 1) WHERE LINK7 LIKE ?;"
			+ "UPDATE GAMES SET LINK8 = RIGHT(LINK8, LENGTH(LINK8) - LENGTH(?) - 1) WHERE LINK8 LIKE ?;";
	private static final String UP_TO_V094B_QRY = "UPDATE VERSION SET MINORVERSION = 94;";
	private static final String UP_TO_V098_QRY = "ALTER TABLE FILTERS ALTER COLUMN FILTER SET DATA TYPE LONGVARCHAR;" + "UPDATE VERSION SET MINORVERSION = 98;";
	private static final String CREATE_INITIAL_DB = "SET WRITE_DELAY 1;" + "CREATE MEMORY TABLE VERSION(MAJORVERSION INTEGER NOT NULL,MINORVERSION INTEGER NOT NULL);"
			+ "CREATE MEMORY TABLE DEVELOPERS(ID INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 0) NOT NULL PRIMARY KEY,NAME VARCHAR(256) NOT NULL);"
			+ "CREATE MEMORY TABLE PUBLISHERS(ID INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 0) NOT NULL PRIMARY KEY,NAME VARCHAR(256) NOT NULL);"
			+ "CREATE MEMORY TABLE GENRES(ID INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 0) NOT NULL PRIMARY KEY,NAME VARCHAR(256) NOT NULL);"
			+ "CREATE MEMORY TABLE PUBLYEARS(ID INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 0) NOT NULL PRIMARY KEY,YEAR VARCHAR(256) NOT NULL);"
			+ "CREATE MEMORY TABLE STATUS(ID INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 0) NOT NULL PRIMARY KEY,STAT VARCHAR(256) NOT NULL);"
			+ "CREATE MEMORY TABLE CUSTOM1(ID INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 0) NOT NULL PRIMARY KEY,VALUE VARCHAR(256) NOT NULL);"
			+ "CREATE MEMORY TABLE CUSTOM2(ID INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 0) NOT NULL PRIMARY KEY,VALUE VARCHAR(256) NOT NULL);"
			+ "CREATE MEMORY TABLE CUSTOM3(ID INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 0) NOT NULL PRIMARY KEY,VALUE VARCHAR(256) NOT NULL);"
			+ "CREATE MEMORY TABLE CUSTOM4(ID INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 0) NOT NULL PRIMARY KEY,VALUE VARCHAR(256) NOT NULL);"
			+ "CREATE MEMORY TABLE DOSBOXVERSIONS(ID INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 0) NOT NULL PRIMARY KEY,TITLE VARCHAR(256) NOT NULL,PATH VARCHAR(256) NOT NULL,EXEFILE VARCHAR(256) NOT NULL,CONFFILE VARCHAR(256) NOT NULL,MULTICONF BOOLEAN,ISDEFAULT BOOLEAN,PARAMETERS VARCHAR(256) DEFAULT '',VERSION VARCHAR(256) NOT NULL,USINGCURSES BOOLEAN,STATS_CREATED TIMESTAMP(0) DEFAULT CURRENT_TIMESTAMP NOT NULL,STATS_LASTMODIFY TIMESTAMP(0),STATS_LASTRUN TIMESTAMP(0),STATS_RUNS INTEGER DEFAULT 0 NOT NULL, DYNAMIC_OPTIONS OTHER DEFAULT NULL);"
			+ "CREATE MEMORY TABLE TEMPLATES(ID INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 0) NOT NULL PRIMARY KEY,TITLE VARCHAR(256) NOT NULL,DBVERSION_ID INTEGER,ISDEFAULT BOOLEAN,STATS_CREATED TIMESTAMP(0) DEFAULT CURRENT_TIMESTAMP NOT NULL,STATS_LASTMODIFY TIMESTAMP(0),STATS_LASTRUN TIMESTAMP(0),STATS_RUNS INTEGER DEFAULT 0 NOT NULL,CONSTRAINT SYS_FK_185 FOREIGN KEY(DBVERSION_ID) REFERENCES DOSBOXVERSIONS(ID));"
			+ "CREATE MEMORY TABLE GAMES(ID INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 0) NOT NULL PRIMARY KEY,"
			+ "TITLE VARCHAR(256) NOT NULL,DEV_ID INTEGER,PUBL_ID INTEGER,GENRE_ID INTEGER,YEAR_ID INTEGER,STAT_ID INTEGER,NOTES LONGVARCHAR,FAVORITE BOOLEAN,"
			+ "SETUP VARCHAR(256),SETUP_PARAMS VARCHAR(256),ALT1 VARCHAR(256) DEFAULT '',ALT1_PARAMS VARCHAR(256) DEFAULT '',ALT2 VARCHAR(256) DEFAULT '',ALT2_PARAMS VARCHAR(256) DEFAULT '',"
			+ "CONFFILE VARCHAR(256),CAPTURES VARCHAR(256),DBVERSION_ID INTEGER," + "LINK1 VARCHAR(256),LINK2 VARCHAR(256),LINK3 VARCHAR(256) DEFAULT '',LINK4 VARCHAR(256) DEFAULT '',"
			+ "LINK5 VARCHAR(256) DEFAULT '',LINK6 VARCHAR(256) DEFAULT '',LINK7 VARCHAR(256) DEFAULT '',LINK8 VARCHAR(256) DEFAULT '',"
			+ "LINK1_TITLE VARCHAR(256) DEFAULT '',LINK2_TITLE VARCHAR(256) DEFAULT '',LINK3_TITLE VARCHAR(256) DEFAULT '',LINK4_TITLE VARCHAR(256) DEFAULT '',"
			+ "LINK5_TITLE VARCHAR(256) DEFAULT '',LINK6_TITLE VARCHAR(256) DEFAULT '',LINK7_TITLE VARCHAR(256) DEFAULT '',LINK8_TITLE VARCHAR(256) DEFAULT '',"
			+ "CUST1_ID INTEGER DEFAULT 0,CUST2_ID INTEGER DEFAULT 0,CUST3_ID INTEGER DEFAULT 0,CUST4_ID INTEGER DEFAULT 0,"
			+ "CUSTOM5 VARCHAR(256) DEFAULT '',CUSTOM6 VARCHAR(256) DEFAULT '',CUSTOM7 VARCHAR(256) DEFAULT '',CUSTOM8 VARCHAR(256) DEFAULT '',"
			+ "CUSTOM9 INTEGER DEFAULT 0,CUSTOM10 INTEGER DEFAULT 0," + "STATS_CREATED TIMESTAMP(0) DEFAULT CURRENT_TIMESTAMP NOT NULL,STATS_LASTMODIFY TIMESTAMP(0),"
			+ "STATS_LASTRUN TIMESTAMP(0),STATS_RUNS INTEGER DEFAULT 0 NOT NULL,STATS_LASTSETUP TIMESTAMP(0),STATS_SETUPS INTEGER DEFAULT 0 NOT NULL,"
			+ "CUSTOM11 VARCHAR(256) DEFAULT '',CUSTOM12 VARCHAR(256) DEFAULT '',CUSTOM13 VARCHAR(256) DEFAULT '',CUSTOM14 VARCHAR(256) DEFAULT '', MOUNT_IDX INTEGER DEFAULT 0,"
			+ "CONSTRAINT SYS_FK_165 FOREIGN KEY(DEV_ID) REFERENCES DEVELOPERS(ID),CONSTRAINT SYS_FK_166 FOREIGN KEY(PUBL_ID) REFERENCES PUBLISHERS(ID),"
			+ "CONSTRAINT SYS_FK_167 FOREIGN KEY(GENRE_ID) REFERENCES GENRES(ID),CONSTRAINT SYS_FK_168 FOREIGN KEY(YEAR_ID) REFERENCES PUBLYEARS(ID),"
			+ "CONSTRAINT SYS_FK_169 FOREIGN KEY(DBVERSION_ID) REFERENCES DOSBOXVERSIONS(ID),CONSTRAINT SYS_FK_170 FOREIGN KEY(STAT_ID) REFERENCES STATUS(ID));"
			+ "CREATE MEMORY TABLE FILTERS(ID INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 0) NOT NULL PRIMARY KEY,TITLE VARCHAR(256) NOT NULL,FILTER LONGVARCHAR NOT NULL,CONF_FILTER VARCHAR(256) NOT NULL);"
			+ "CREATE MEMORY TABLE NATIVECOMMANDS(ID INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 0) NOT NULL PRIMARY KEY,COMMAND VARCHAR(256) NOT NULL,PARAMETERS VARCHAR(256) NOT NULL,CWD VARCHAR(256) NOT NULL,WAITFOR BOOLEAN,ORDERNR INTEGER,GAME_ID INTEGER,TEMPLATE_ID INTEGER,"
			+ "CONSTRAINT SYS_FK_180 FOREIGN KEY(GAME_ID) REFERENCES GAMES(ID),CONSTRAINT SYS_FK_181 FOREIGN KEY(TEMPLATE_ID) REFERENCES TEMPLATES(ID));" + "CREATE MEMORY TABLE LOG ("
			+ "ID INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 0) NOT NULL PRIMARY KEY,TIME TIMESTAMP(0) DEFAULT CURRENT_TIMESTAMP NOT NULL,"
			+ "EVENT TINYINT NOT NULL, ENTITY_TYPE TINYINT NOT NULL," + "ENTITY_ID INT NOT NULL, ENTITY_TITLE VARCHAR(256) NOT NULL);"
			+ "INSERT INTO CUSTOM1(VALUE) VALUES(''); INSERT INTO CUSTOM2(VALUE) VALUES('');" + "INSERT INTO CUSTOM3(VALUE) VALUES(''); INSERT INTO CUSTOM4(VALUE) VALUES('');"
			+ "INSERT INTO VERSION VALUES(0,98);";

	public class Transaction implements AutoCloseable {

		private Connection connection_;
		private boolean committed_;

		private Transaction(Connection connection) throws SQLException {
			connection_ = connection;
			connection_.setAutoCommit(false);
		}

		public Statement createStatement() throws SQLException {
			return connection_.createStatement();
		}

		public PreparedStatement prepareStatement(String sql) throws SQLException {
			return connection_.prepareStatement(sql);
		}

		public void commit() throws SQLException {
			connection_.commit();
			committed_ = true;
		}

		@Override
		public void close() throws SQLException {
			if (!committed_)
				connection_.rollback();
			connection_.setAutoCommit(true);
		}
	}

	private JDBCPooledDataSource dataSource_;
	private boolean initializedNewDatabase_ = false;

	private DatabaseService() {
		SettingsService settings = SettingsService.getInstance();
		ITextService text = TextService.getInstance();
		System.out.println(text.get("database.notice.startup"));
		String connString = FileLocationService.getInstance().getCanonicalConnectionString(settings.getValue("database", "connectionstring"));
		try {
			// Register the JDBC driver for dBase
			Class.forName("org.hsqldb.jdbcDriver");
			dataSource_ = new JDBCPooledDataSource();
			dataSource_.setDatabase(connString);
			dataSource_.setUser(settings.getValue("database", "username"));
			dataSource_.setPassword(StringUtils.defaultString(settings.getValue("database", "password")));
			initializeIfNecessary();
			upgradeIfNecessary();
		} catch (SQLException e) {
			Shell shell = new Shell();
			Mess_.on(shell).key("database.error.initconnection", StringRelatedUtils.toString(e)).exception(e).fatal();
			throw new RuntimeException();
		} catch (ClassNotFoundException e) {
			Shell shell = new Shell();
			Mess_.on(shell).key("database.error.registerdriver", StringRelatedUtils.toString(e)).exception(e).fatal();
			throw new RuntimeException();
		}
	}

	private void initializeIfNecessary() throws SQLException {
		try (Connection con = getConnection(); Statement stmt = con.createStatement()) {
			stmt.executeQuery(TEST_QRY);
		} catch (SQLException emptydatabase) {
			// Probably empty database, fill it
			try (Transaction transaction = startTransaction(); Statement stmt2 = transaction.createStatement()) {
				for (String s: CREATE_INITIAL_DB.split(";"))
					stmt2.addBatch(s);
				stmt2.executeBatch();
				transaction.commit();
				initializedNewDatabase_ = true;
			} catch (SQLException e) {
				e.printStackTrace();
				throw new SQLException(TextService.getInstance().get("database.error.query", new Object[] {"create initial tables"}));
			}
		}
	}

	private void upgradeIfNecessary() throws SQLException {
		int[] version = getVersion();
		if (version[0] <= 0 && version[1] < 50)
			upgradeToVersion(UP_TO_V050_QRY, 50);
		if (version[0] <= 0 && version[1] < 51)
			upgradeToVersion(UP_TO_V051_QRY, 51);
		if (version[0] <= 0 && version[1] < 56)
			upgradeToVersion(UP_TO_V056_QRY, 56);
		if (version[0] <= 0 && version[1] < 62)
			upgradeToVersion(UP_TO_V062_QRY, 62);
		if (version[0] <= 0 && version[1] < 65)
			upgradeToVersion(UP_TO_V065_QRY, 65);
		if (version[0] <= 0 && version[1] < 67)
			upgradeToVersion(UP_TO_V067_QRY, 67);
		if (version[0] <= 0 && version[1] < 68)
			upgradeToVersion(UP_TO_V068_QRY, 68);
		if (version[0] <= 0 && version[1] < 72)
			upgradeToVersion(UP_TO_V072_QRY, 72);
		if (version[0] <= 0 && version[1] < 73)
			upgradeToVersion(UP_TO_V073_QRY, 73);
		if (version[0] <= 0 && version[1] < 74)
			upgradeToVersion(UP_TO_V074_QRY, 74);
		if (version[0] <= 0 && version[1] < 75)
			upgradeToVersion(UP_TO_V075_QRY, 75);
		if (version[0] <= 0 && version[1] < 76)
			upgradeToVersion(UP_TO_V076_QRY, 76);
		if (version[0] <= 0 && version[1] < 77)
			upgradeToVersion(UP_TO_V077_QRY, 77);
		if (version[0] <= 0 && version[1] < 81)
			upgradeToVersion(UP_TO_V081_QRY, 81);
		if (version[0] <= 0 && version[1] < 90)
			upgradeToVersion(UP_TO_V090_QRY, 90);
		if (version[0] <= 0 && version[1] < 92)
			upgradeToVersion(UP_TO_V092_QRY, 92);
		if (version[0] <= 0 && version[1] < 93)
			upgradeToVersion(UP_TO_V093_QRY, 93);
		if (version[0] <= 0 && version[1] < 94)
			upgradeToVersion(UP_TO_V094A_QRY, UP_TO_V094B_QRY, FileLocationService.getInstance().getDataDir().getPath(), 94);
		if (version[0] <= 0 && version[1] < 98)
			upgradeToVersion(UP_TO_V098_QRY, 98);
	}

	private int[] getVersion() {
		try (Connection con = getConnection(); Statement stmt = con.createStatement(); ResultSet resultset = stmt.executeQuery(GET_VERSION)) {
			resultset.next();
			return new int[] {resultset.getInt(1), resultset.getInt(2)}; // major, minor
		} catch (SQLException e) {
			return new int[] {0, 0}; // assume version < 0.50 (0.0)
		}
	}

	private void upgradeToVersion(String query, int minorVersion) throws SQLException {
		System.out.println(TextService.getInstance().get("database.notice.upgrade", new Object[] {0, minorVersion}));
		try (Transaction transaction = startTransaction(); Statement stmt = transaction.createStatement()) {
			for (String s: query.split(";"))
				stmt.addBatch(s);
			stmt.executeBatch();
			transaction.commit();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new SQLException(TextService.getInstance().get("database.error.upgrade", new Object[] {0, minorVersion}));
		}
	}
	
	private void upgradeToVersion(String queryA, String queryB, String val, int minorVersion) throws SQLException {
		System.out.println(TextService.getInstance().get("database.notice.upgrade", new Object[] {0, minorVersion}));
		try (Transaction transaction = startTransaction()) {
			for (String s: queryA.split(";")) {
				try (PreparedStatement pstmt = transaction.prepareStatement(s)) {
					pstmt.setString(1, val);
					pstmt.setString(2, val + "%");
					pstmt.executeUpdate();
				}
			}
			try (Statement stmt = transaction.createStatement()) {
				for (String s: queryB.split(";"))
					stmt.addBatch(s);
				stmt.executeBatch();
			}
			transaction.commit();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new SQLException(TextService.getInstance().get("database.error.upgrade", new Object[] {0, minorVersion}));
		}
	}

	private static class DatabaseServiceHolder {
		private static final DatabaseService instance_ = new DatabaseService();
	}

	public static DatabaseService getInstance() {
		return DatabaseServiceHolder.instance_;
	}

	public Connection getConnection() throws SQLException {
		return dataSource_.getPooledConnection().getConnection();
	}

	public Transaction startTransaction() throws SQLException {
		return new Transaction(getConnection());
	}

	public boolean isInitializedNewDatabase() {
		return initializedNewDatabase_;
	}

	public void shutdown() throws SQLException {
		System.out.println(TextService.getInstance().get("database.notice.shutdown"));
		try (Transaction transaction = startTransaction(); Statement stmt = transaction.createStatement()) {
			stmt.execute("SHUTDOWN");
			transaction.commit();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new SQLException(TextService.getInstance().get("database.error.shutdown"));
		}
	}
}