package org.openmrs.module.amrscore.api.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class AfyastatDatabaseConnector {
	
	private static final String DB_URL = "jdbc:mysql://10.50.80.58:3306/amrs_etl";
	
	private static final String USER = "kenyaemr";
	
	private static final String PASSWORD = "kenyaemr";
	
	public static Connection getConnection() throws SQLException {
		return DriverManager.getConnection(DB_URL, USER, PASSWORD);
	}
	
}
