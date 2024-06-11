package org.openmrs.module.amrscore.api.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnector {
	
	private static final String DB_URL = "jdbc:mysql://10.50.80.24:3306/amrs_etl?zeroDateTimeBehavior=convertToNull";
	
	private static final String USER = "erugut";
	
	private static final String PASSWORD = "Ampath@2020";
	
	public static Connection getConnection() throws SQLException {
		return DriverManager.getConnection(DB_URL, USER, PASSWORD);
	}
	
	// Example method to perform a database query
	
}
