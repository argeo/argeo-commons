package org.argeo.cms.pgsql.util;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.postgresql.Driver;

/** Simple PostgreSQL check. */
public class CheckPg {

	public List<String> listTables() {
		String osUser = System.getProperty("user.name");

		String url = "jdbc:postgresql://localhost/" + osUser;
		Properties props = new Properties();
		props.setProperty("user", osUser);
		props.setProperty("password", "changeit");
		List<String> result = new ArrayList<>();

		Driver driver = new Driver();
		try (Connection conn = driver.connect(url, props); Statement s = conn.createStatement();) {
			s.execute("SELECT * FROM pg_catalog.pg_tables");
			ResultSet rs = s.getResultSet();
			while (rs.next()) {
				result.add(rs.getString("tablename"));
			}
			return result;
		} catch (SQLException e) {
			throw new IllegalStateException(e);
		}
	}

	public static void main(String[] args) {
		new CheckPg().listTables().forEach(System.out::println);
	}

}
