package org.openmrs.module.amrscore.web.controller;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.api.LocationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.amrscore.api.utils.FormsManager;
import org.openmrs.module.amrscore.api.utils.ReportRequest;
import org.openmrs.module.amrscore.api.utils.ZScoreUtil;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.v1_0.controller.BaseRestController;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.SyncFailedException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.Date;

import org.openmrs.GlobalProperty;

import static org.openmrs.module.amrscore.api.db.DatabaseConnector.getConnection;

@Controller
@RequestMapping("/rest/" + RestConstants.VERSION_1 + "/amrscore")
public class RestController extends BaseRestController {
	
	@Override
	public String getNamespace() {
		return "v1/amrscore";
	}
	
	@RequestMapping(value = "hello", method = RequestMethod.GET)
	@ResponseBody
	public String sayHello() {
		return "Hello, OpenMRS!";
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/patient", produces = "application/json")
	@ResponseBody
	public Object getPatientIdByPatientUuid(@RequestParam("patientUuid") String patientUuid) {
		ObjectNode patientNode = JsonNodeFactory.instance.objectNode();
		if (org.apache.commons.lang3.StringUtils.isBlank(patientUuid)) {
			return new ResponseEntity<Object>("You must specify patientUuid in the request!", new HttpHeaders(),
			        HttpStatus.BAD_REQUEST);
		}
		
		Patient patient = Context.getPatientService().getPatientByUuid(patientUuid);
		ObjectNode patientObj = JsonNodeFactory.instance.objectNode();
		
		if (patient == null) {
			return new ResponseEntity<Object>("The provided patient was not found in the system!", new HttpHeaders(),
			        HttpStatus.NOT_FOUND);
		}
		patientNode.put("patientId", patient.getPatientId());
		patientNode.put("name", patient.getPerson().getPersonName().getFullName());
		patientNode.put("age", patient.getAge());
		
		patientObj.put("results", patientNode);
		
		return patientObj.toString();
		
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/forms")
	@ResponseBody
	public Object getAllAvailableFormsForVisit(HttpServletRequest request, @RequestParam("patientUuid") String patientUuid) {
		String forms = "";
		if (org.apache.commons.lang3.StringUtils.isBlank(patientUuid)) {
			return new ResponseEntity<Object>("You must specify patientUuid in the request!", new HttpHeaders(),
			        HttpStatus.BAD_REQUEST);
		} else {
			forms = FormsManager.retrieveFormsForVisit(patientUuid);
		}
		System.out.println("Checking Available Forms");
		return forms;// new ResponseEntity<Object>("forms", new HttpHeaders(), HttpStatus.BAD_REQUEST);
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/zscore")
	@ResponseBody
	public Object calculateZScore(@RequestParam("sex") String sex, @RequestParam("weight") Double weight,
	        @RequestParam("height") Double height) {
		ObjectNode resultNode = JsonNodeFactory.instance.objectNode();
		Integer result = ZScoreUtil.calculateZScore(height, weight, sex);
		
		if (result < -4) { // this is an indication of an error. We can break it down further for appropriate messages
			return new ResponseEntity<Object>("Could not compute the zscore for the patient!", new HttpHeaders(),
			        HttpStatus.NOT_FOUND);
		}
		resultNode.put("wfl_score", result);
		return resultNode.toString();
		
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/preappointment")
	@ResponseBody
	public String getPreappointment(HttpServletRequest request, @RequestParam("locationUUID") String locationUUID,
	        @RequestParam("yearWeek") String yearWeek) throws IOException {
		String urll = "https://ngx.ampath.or.ke/etl-latest/etl/ml-weekly-predictions?locationUuids=" + locationUUID
		        + "&yearWeek=" + yearWeek;
		URL url = new URL(urll);
		//GlobalProperty gps = Context.getGlobalPropertyService();
		HttpURLConnection http = (HttpURLConnection) url.openConnection();
		http.setRequestMethod("GET");
		String data = "";
		// Set Basic Authentication header
		String username = "";
		String password = "";
		String auth = username + ":" + password;
		byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes());
		String authHeaderValue = "Basic " + new String(encodedAuth);
		http.setRequestProperty("Authorization", authHeaderValue);
		
		int statusCode = http.getResponseCode();
		if (statusCode == HttpURLConnection.HTTP_OK) {
			// Read the response body
			BufferedReader in = new BufferedReader(new InputStreamReader(http.getInputStream()));
			String inputLine;
			StringBuilder response = new StringBuilder();
			
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
			data = response.toString();
			// Print the response as a string
			System.out.println("Response: " + response.toString());
		} else {
			data = String.valueOf(statusCode);
			System.out.println("Failed to get response. Status code: " + statusCode);
		}
		return data;
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/flags", produces = "application/json")
	// gets all flags for a patient
	@ResponseBody
	public Object getAllPatientFlags(HttpServletRequest request, @RequestParam("patientUuid") String patientUuid) {
		if (org.apache.commons.lang3.StringUtils.isBlank(patientUuid)) {
			return new ResponseEntity<Object>("You must specify patientUuid in the request!", new HttpHeaders(),
			        HttpStatus.BAD_REQUEST);
		}
		Patient patient = Context.getPatientService().getPatientByUuid(patientUuid);
		ObjectNode flagsObj = JsonNodeFactory.instance.objectNode();
		
		if (patient == null) {
			return new ResponseEntity<Object>("The provided patient was not found in the system!", new HttpHeaders(),
			        HttpStatus.NOT_FOUND);
		}
		ArrayNode flags = JsonNodeFactory.instance.arrayNode();
		
		flagsObj.put("results", flags);
		
		return flagsObj.toString();
		
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/default-facility", produces = "application/json")
	@ResponseBody
	public Object getDefaultConfiguredFacility() {
		
		Location location = Context.getUserContext().getLocation();
		ObjectNode locationNode = JsonNodeFactory.instance.objectNode();
		
		locationNode.put("locationId", location.getLocationId());
		locationNode.put("uuid", location.getUuid());
		locationNode.put("display", location.getName());
		
		return locationNode.toString();
		
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/facilityName", produces = "application/json")
	@ResponseBody
	public Object getFacilityName(@RequestParam("facilityCode") String facilityCode) {
		LocationService locationService = Context.getLocationService();
		Location location = locationService.getLocation(facilityCode);
		SimpleObject locationResponseObj = new SimpleObject();
		if (location != null) {
			locationResponseObj.put("name", location.getName());
			Context.getUserContext().setLocation(location);
		} else {
			locationResponseObj.put("name", "Location Not Available");
		}
		return locationResponseObj;
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/parentlocation" ,produces = "application/json")
	@ResponseBody
	public Object getParentLocation(HttpServletRequest request, @RequestParam("locationUuid") String locationUuid) throws SQLException {
		JSONObject resultObject = new JSONObject();

		ObjectNode reportsNode = JsonNodeFactory.instance.objectNode();
		Location location = Context.getUserContext().getLocation();
		ObjectMapper mapper = new ObjectMapper();
		ArrayNode reportsArray = mapper.createArrayNode();

		//HttpServletRequest request, @RequestParam("locationUuid") String locationUuid
		try (Connection conn = getConnection()) {
			Statement statement = conn.createStatement();
			String parent_id ="";
			String parent_uuid ="";
			String parent_name="";
			String location_name = "",location_uuid="",location_id="";

			String  sql="SELECT * FROM amrs_etl.location where uuid='"+ locationUuid +"';";
					System.out.println("SQL is Here "+ sql);
			ResultSet resultSet = statement.executeQuery(sql);
			while (resultSet.next()) {
				location_id =resultSet.getString("location_id");
				location_uuid =resultSet.getString("uuid");
				location_name =resultSet.getString("location_name");
				parent_id =resultSet.getString("parent_location");
				parent_uuid =resultSet.getString("parent_uuid");
				parent_name =resultSet.getString("parent_name");
			}

			if(parent_id.equals("")){
				ObjectNode rowNode = mapper.createObjectNode();
				rowNode.put("parent_locationUuid", location_uuid);
				rowNode.put("parent_locationName", location_name);
				reportsArray.add(rowNode);

			}else {

					ObjectNode rowNode = mapper.createObjectNode();
					rowNode.put("parent_locationUuid", parent_uuid);
					rowNode.put("parent_locationName", parent_name);
					reportsArray.add(rowNode);
			}
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
			System.out.println("AMRS Core Errors "+ e.getMessage());
		}

		resultObject.put("results", reportsArray);

		return resultObject.toString();

	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/locations" ,produces = "application/json")
	@ResponseBody
	public Object getAllLocationFromParent(HttpServletRequest request, @RequestParam("locationUuid") String locationUuid) throws SQLException {
		JSONObject resultObject = new JSONObject();

		ObjectNode reportsNode = JsonNodeFactory.instance.objectNode();
		Location location = Context.getUserContext().getLocation();
		ObjectMapper mapper = new ObjectMapper();
		ArrayNode reportsArray = mapper.createArrayNode();

		//HttpServletRequest request, @RequestParam("locationUuid") String locationUuid
		try (Connection conn = getConnection()) {
			Statement statement = conn.createStatement();
			String parent_id ="";
			ResultSet resultSet = statement.executeQuery("SELECT * FROM amrs_etl.location where uuid='"+ locationUuid +"';");
			while (resultSet.next()) {
				 parent_id =resultSet.getString("parent_location");
			}

			if(parent_id.equals("")){
				ObjectNode rowNode = mapper.createObjectNode();
				rowNode.put("uuid", resultSet.getString("uuid"));
				rowNode.put("location_name", resultSet.getString("location_name"));
				reportsArray.add(rowNode);

			}else {
				Statement statementR = conn.createStatement();

				ResultSet resultChildren = statementR.executeQuery("SELECT * FROM amrs_etl.location where parent_location='" + parent_id + "' order by location_name ;");

				while (resultChildren.next()) {
					ObjectNode rowNode = mapper.createObjectNode();
					rowNode.put("uuid", resultChildren.getString("uuid"));
					rowNode.put("location_name", resultChildren.getString("location_name"));
					reportsArray.add(rowNode);

				}
			}
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
			System.out.println("AMRS Core Errors "+ e.getMessage());
		}

		resultObject.put("results", reportsArray);

		return resultObject.toString();

	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/reports", produces = "application/json")
	@ResponseBody
	public Object getAllReportsNames() {

		JSONObject resultObject = new JSONObject();
		//ArrayNode reportsArray = mapper.createArrayNode();

		ObjectNode reportsNode = JsonNodeFactory.instance.objectNode();
		Location location = Context.getUserContext().getLocation();
		ObjectMapper mapper = new ObjectMapper();
		ArrayNode reportsArray = mapper.createArrayNode();
		ArrayNode finalReportsArray = mapper.createArrayNode();

		//HttpServletRequest request, @RequestParam("locationUuid") String locationUuid
			try (Connection conn = getConnection()) {
				 Statement statement = conn.createStatement();
				 ResultSet resultSet = statement.executeQuery("SELECT * FROM amrs_etl.reports where status='1' order by category_name asc;");
				while (resultSet.next()) {
					ObjectNode rowNode = mapper.createObjectNode();
					rowNode.put("id", resultSet.getString("id"));
					rowNode.put("report_name", resultSet.getString("report_name"));
					rowNode.put("category", resultSet.getString("category"));
					rowNode.put("status", resultSet.getString("status"));
					rowNode.put("category_name", resultSet.getString("category_name"));
					rowNode.put("uuid", resultSet.getString("uuid"));
					// Add more columns as needed, e.g., rowNode.put("anotherColumn", resultSet.getString("anotherColumn"));
					reportsArray.add(rowNode);
					// Example: Get data from resultSet and add it to locationNode
					// reportsNode.put("columnName", resultSet.getString("columnName"));
				}
			} catch (SQLException e) {
				e.printStackTrace();
				System.out.println("AMRS Core Errors "+ e.getMessage());
			}
		resultObject.put("results", reportsArray);
		finalReportsArray.add(String.valueOf(resultObject));

		//return resultObject.toString();
		return new ResponseEntity<Object>(resultObject.toString(), HttpStatus.OK);


	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/reports/category" ,produces = "application/json")
	@ResponseBody
	public Object getAllReports(@RequestParam("categoryName") String categoryName) {

		JSONObject resultObject = new JSONObject();

		ObjectNode reportsNode = JsonNodeFactory.instance.objectNode();
		Location location = Context.getUserContext().getLocation();
		ObjectMapper mapper = new ObjectMapper();
		ArrayNode reportsArray = mapper.createArrayNode();

		//HttpServletRequest request, @RequestParam("locationUuid") String locationUuid
		try (Connection conn = getConnection()) {
			Statement statement = conn.createStatement();
			ResultSet resultSet = statement.executeQuery("SELECT * FROM amrs_etl.reports where category in('"+ categoryName +"');");
			while (resultSet.next()) {
				ObjectNode rowNode = mapper.createObjectNode();
				rowNode.put("id", resultSet.getString("id"));
				rowNode.put("report_name", resultSet.getString("report_name"));
				rowNode.put("description", resultSet.getString("description"));
				rowNode.put("category", resultSet.getString("description"));
				rowNode.put("status", resultSet.getString("status"));
				rowNode.put("sp_name", resultSet.getString("sp_name"));
				rowNode.put("uuid", resultSet.getString("uuid"));
				// Add more columns as needed, e.g., rowNode.put("anotherColumn", resultSet.getString("anotherColumn"));
				reportsArray.add(rowNode);
				// Example: Get data from resultSet and add it to locationNode
				// reportsNode.put("columnName", resultSet.getString("columnName"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
			System.out.println("AMRS Core Errors "+ e.getMessage());
		}
		resultObject.put("results", reportsArray);

		return reportsArray.toString();

	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/reports/logs" ,produces = "application/json")
	@ResponseBody
	public Object getReportsLogs(HttpServletRequest request, @RequestParam("locationUuid") String locationUuid,@RequestParam("report_id") String report_id) {

		JSONObject resultObject = new JSONObject();

		ObjectNode reportsNode = JsonNodeFactory.instance.objectNode();
		Location location = Context.getUserContext().getLocation();
		ObjectMapper mapper = new ObjectMapper();
		ArrayNode reportsArray = mapper.createArrayNode();
        //System.out.println("Current Location "+ location.getUuid());

		try (Connection conn = getConnection()) {
			Statement statement = conn.createStatement();
			ResultSet resultSet = statement.executeQuery("SELECT \n" +
					"r.uuid,\n" +
					"r.id,\n" +
					"r.report_name,\n" +
					"r.description,\n" +
					"r.category,\n" +
					"r.status,\n" +
					"r.sp_name,\n" +
					"rl.id log_id,\n" +
					"rl.start_date,\n" +
					"rl.end_date,\n" +
					"rl.month,\n" +
					"rl.year,\n" +
					"rl.status \n" +
					" FROM amrs_etl.reports r\n" +
					"inner join amrs_etl.reports_logs rl on r.id=rl.report_id where locationuuid in ('"+ locationUuid +"') and report_id='"+ report_id+"' and rl.status=1;");
			while (resultSet.next()) {
				ObjectNode rowNode = mapper.createObjectNode();
				rowNode.put("id", resultSet.getString("id"));
				rowNode.put("log_id", resultSet.getString("log_id"));
				rowNode.put("report_name", resultSet.getString("report_name"));
				rowNode.put("description", resultSet.getString("description"));
				rowNode.put("category", resultSet.getString("description"));
				rowNode.put("start_date", resultSet.getString("start_date"));
				rowNode.put("end_date", resultSet.getString("end_date"));
				rowNode.put("month", resultSet.getString("month"));
				rowNode.put("year", resultSet.getString("year"));
				rowNode.put("status_code", resultSet.getString("status"));
				rowNode.put("sp_name", resultSet.getString("sp_name"));
				rowNode.put("uuid", resultSet.getString("uuid"));
				String status="";
				if(resultSet.getString("status").equals("0")){
					status="Please Wait !!! Generating the Report";
				}else if (resultSet.getString("status").equals("1")){
					status="Completed - Can Regenerate";

				} else if (resultSet.getString("status").equals("2")) {
					status="Completed - Frozen ";
				}else{
					status="No availabe Status";
				}
				rowNode.put("status", status);
				// Add more columns as needed, e.g., rowNode.put("anotherColumn", resultSet.getString("anotherColumn"));
				reportsArray.add(rowNode);
				// Example: Get data from resultSet and add it to locationNode
				// reportsNode.put("columnName", resultSet.getString("columnName"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
			System.out.println("AMRS Core Errors "+ e.getMessage());
		}
		resultObject.put("results",reportsArray);

		// return new ResponseEntity<Object>(reportsArray.toString(), HttpStatus.OK);

		return reportsArray.toString();

	}
	
	@RequestMapping(method = RequestMethod.POST, value = "/reports/generate" ,produces = "application/json")
	@ResponseBody
	public Object postReportsLogs(HttpServletRequest request,
								  @RequestBody ReportRequest reportRequest) {
		JSONObject resultObject = new JSONObject();
		ObjectNode reportsNode = JsonNodeFactory.instance.objectNode();
		Location location = Context.getUserContext().getLocation();
		ObjectMapper mapper = new ObjectMapper();
		ArrayNode reportsArray = mapper.createArrayNode();
		try (Connection conn = getConnection()) {
			Date nowDate = new Date();

			String Sqql = "select * from amrs_etl.reports_logs where locationUuid ='" + reportRequest.getLocationUuid() + "' and  report_id ='" + reportRequest.getReport_id() + "' and start_date ='" + reportRequest.getStart_date() + "' and end_date= '" + reportRequest.getEnd_date() + "' and uuid ='" + reportRequest.getUuid() + "' and status ='0'";
			System.out.println("SQL statement "+Sqql);
			Statement statement = conn.createStatement();
			ResultSet resultSet = statement.executeQuery(Sqql);

			int rowCount = 0;
			while (resultSet.next()) {
				rowCount++;
			}

			if (rowCount != 0) {
				ObjectNode rowNode = mapper.createObjectNode();
				rowNode.put("1", "Report currently Processing. Please Wait !!! ");
				reportsArray.add(rowNode);

			} else {

				String sql = "INSERT INTO amrs_etl.reports_logs (locationUuid, report_id, start_date,end_date,month,year,status,uuid,parentLocationUuid,parentName,userUuid,username,dateCreated) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ? ,? ,? ,?)";
				System.out.println("Sql " + sql);
				PreparedStatement preparedStatement = conn.prepareStatement(sql);
				preparedStatement.setString(1, reportRequest.getLocationUuid());
				preparedStatement.setString(2, reportRequest.getReport_id());
				preparedStatement.setString(3, reportRequest.getStart_date());
				preparedStatement.setString(4, reportRequest.getEnd_date());
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
				// Parse the dates
				LocalDate startDate = LocalDate.parse(reportRequest.getStart_date(), formatter);
				LocalDate endDate = LocalDate.parse(reportRequest.getEnd_date(), formatter);

				String startMonthName = startDate.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
				String endMonthName = endDate.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
				String year = String.valueOf(startDate.getYear());
				preparedStatement.setString(5, startMonthName);
				preparedStatement.setString(6, year);
				preparedStatement.setString(7, "1");
				preparedStatement.setString(8,  reportRequest.getUuid());
				preparedStatement.setString(9,  reportRequest.getParent_locationUuid());
				preparedStatement.setString(10, reportRequest.getParent_locationName());
				preparedStatement.setString(11, reportRequest.getUser_uuid());
				preparedStatement.setString(12, reportRequest.getUser_name());
				preparedStatement.setString(13, nowDate.toString());
				int rowsAffected = preparedStatement.executeUpdate();
				ObjectNode rowNode = mapper.createObjectNode();
				rowNode.put("Rows inserted", rowsAffected);
				reportsArray.add(rowNode);
				// Generate the report

				long startTime = System.currentTimeMillis();

				String sp_sql = "{CALL " + reportRequest.getSp_name() + "(?, ?, ?)}";
				CallableStatement callableStatement = conn.prepareCall(sp_sql);
				// Set the values for the parameters
				callableStatement.setString(1, reportRequest.getLocationUuid()); //locationUUid
				callableStatement.setString(2, reportRequest.getStart_date()); //Startdate
				callableStatement.setString(3, reportRequest.getEnd_date()); // Enddate
				// Execute the stored procedure
				callableStatement.execute();
				long endTime = System.currentTimeMillis();

				// Calculate the elapsed time
				long elapsedTime = endTime - startTime;
				//Update status and TAT
				String updatesql = "update amrs_etl.reports_logs set tat='" + elapsedTime + "', status='1' where locationUuid ='" + reportRequest.getLocationUuid() + "' and  report_id ='" + reportRequest.getReport_id() + "' and start_date ='" + reportRequest.getStart_date() + "' and end_date= '" + reportRequest.getEnd_date() + "' and uuid ='" + reportRequest.getUuid() + "' and tat is null";
				PreparedStatement preparedStatementt = conn.prepareStatement(updatesql);
				int rowsAffectedt = preparedStatementt.executeUpdate();
				System.out.println("Rows affected: " + rowsAffectedt);


				//Done
				System.out.println("Stored procedure executed successfully.");
				conn.close();
			}
			} catch(SQLException e){
				e.printStackTrace();
				System.out.println("AMRS Core Errors " + e.getMessage());
			}


		resultObject.put("results", reportsArray);
		return reportsArray.toString();

	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/old/reports/view" ,produces = "application/json")
	@ResponseBody
	public Object getHTSData(@RequestParam("id") String reportUuid,HttpServletRequest request) throws SQLException {
		JSONArray jsonArray = new JSONArray();
		JSONObject resultObject = new JSONObject();

		String reportId = "";
		String tablename = "";
		String spName = "";
		String location = "";
		String startDate = "";
		String endDate = "";

		try (Connection conn = getConnection()) {

			String logSql = "SELECT r.id, r.uuid,r.report_name,r.sp_name, r.tbl_name,rl.start_date,rl.end_date,rl.locationUuid\n" +
					" FROM amrs_etl.reports r \n" +
					"inner join amrs_etl.reports_logs rl on r.id=rl.report_id\n" +
					"where rl.status=1 and rl.id ='" + reportUuid + "' ";
			try (PreparedStatement logStatement = conn.prepareStatement(logSql);
				 ResultSet resultLogSet = logStatement.executeQuery()) {
				while (resultLogSet.next()) {
					reportId = resultLogSet.getString("uuid");
					tablename = resultLogSet.getString("tbl_name");
					spName = resultLogSet.getString("sp_name");
					location = resultLogSet.getString("locationUuid");
					startDate = resultLogSet.getString("start_date");
					endDate = resultLogSet.getString("end_date");

				}
			} catch (Exception e) {
				System.out.println("Error occurred " + e.getMessage());
			}
			if (tablename.length() == 0) {
				resultObject.put("results", jsonArray);
			} else {
				//System.out.println("Imefika Hapa, table ndo hii  "+ tablename);
				String sql = "SELECT * FROM amrs_etl." + tablename + " where location_uuid in('" + location + "') and visit_date>='" + startDate + "'  and visit_date <= '" + endDate + "'";
			   //System.out.println("sql ndo hii "+ sql);
				try (PreparedStatement statement = conn.prepareStatement(sql);
					 ResultSet resultSet = statement.executeQuery()) {
					ResultSetMetaData metaData = resultSet.getMetaData();
					int columnCount = metaData.getColumnCount();
					System.out.println("Total Columns: " + columnCount);

					String[] columnNames = new String[columnCount];
					for (int i = 1; i <= columnCount; i++) {
						int y = i-1;
						columnNames[y] = metaData.getColumnName(i);
					}
					// Print out all column names
					System.out.println("Column Names:");
					for (String columnName : columnNames) {
						System.out.println(columnName);
					}
					while (resultSet.next()) {
						JSONObject jsonObject = new JSONObject();
						for (String columnName : columnNames) {
							Object value = resultSet.getObject(columnName);
							jsonObject.put(columnName, value);
						}
						jsonArray.put(jsonObject);
					}
					/////////
					//JSONObject resultObject = new JSONObject();
					resultObject.put("results", jsonArray);
					System.out.println(jsonArray.toString());
				} catch (SQLException e) {
					e.printStackTrace();
					System.out.println("AMRS Core Errors " + e.getMessage());
					// Return an error response
					return Collections.singletonMap("error", "An error occurred while fetching data.");
				}
			}
		}
		return resultObject.toString();
	}
	
	//New
	@RequestMapping(method = RequestMethod.GET, value = "/reports/view" ,produces = "application/json")
	@ResponseBody
	public Object getData(@RequestParam("id") String reportUuid,
						  @RequestParam("multilocations") List<String>multilocation,
						  HttpServletRequest request) throws SQLException {
		JSONArray jsonArray = new JSONArray();
		JSONObject resultObject = new JSONObject();

		String reportId = "";
		String tablename = "";
		String spName = "";
		String location = "";
		String startDate = "";
		String endDate = "";

		try (Connection conn = getConnection()) {
			String logSql = "SELECT r.id, r.uuid, r.report_name, r.sp_name, r.tbl_name, rl.start_date, rl.end_date, rl.locationUuid " +
					"FROM amrs_etl.reports r " +
					"INNER JOIN amrs_etl.reports_logs rl ON r.id = rl.report_id " +
					"WHERE rl.status = 1 AND rl.id = ? order by rl.id desc";

			try (PreparedStatement logStatement = conn.prepareStatement(logSql)) {
				logStatement.setString(1, reportUuid);
				try (ResultSet resultLogSet = logStatement.executeQuery()) {
					if (resultLogSet.next()) {
						reportId = resultLogSet.getString("uuid");
						tablename = resultLogSet.getString("tbl_name");
						spName = resultLogSet.getString("sp_name");
						location = resultLogSet.getString("locationUuid");
						startDate = resultLogSet.getString("start_date");
						endDate = resultLogSet.getString("end_date");
					}
				} catch (SQLException e) {
					System.out.println("Error occurred while fetching log data: " + e.getMessage());
					return Collections.singletonMap("error", "An error occurred while fetching log data.");
				}
			}

			if (tablename.isEmpty()) {
				resultObject.put("results", jsonArray);
				return resultObject.toString();
			}

			// Debugging step: Get and print the table schema
			String schemaSql = "SELECT * FROM amrs_etl." + tablename + " LIMIT 1";
			try (Statement schemaStatement = conn.createStatement();
				 ResultSet schemaResultSet = schemaStatement.executeQuery(schemaSql)) {
				ResultSetMetaData schemaMetaData = schemaResultSet.getMetaData();
				int schemaColumnCount = schemaMetaData.getColumnCount();
				System.out.println("Total Columns in Schema: " + schemaColumnCount);

				String[] schemaColumnNames = new String[schemaColumnCount];
				for (int i = 1; i <= schemaColumnCount; i++) {
					schemaColumnNames[i - 1] = schemaMetaData.getColumnName(i);
				}

				System.out.println("Schema Column Names:");
				for (String columnName : schemaColumnNames) {
					System.out.println(columnName);
				}
			} catch (SQLException e) {
				e.printStackTrace();
				System.out.println("Error occurred while fetching table schema: " + e.getMessage());
				return Collections.singletonMap("error", "An error occurred while fetching table schema.");
			}

			int dd = multilocation.size();

			String locations_uuid= multilocation.toString();

			String locations_uuid_result = locations_uuid.substring(1, locations_uuid.length() - 1);

			System.out.println("Locations_uuid "+ locations_uuid_result);

			String sql = "SELECT * FROM amrs_etl." + tablename + " WHERE location_uuid in ("+locations_uuid_result+") AND visit_date >= '"+startDate+"' AND visit_date <= '"+endDate+"'";

            System.out.println("Sql is "+sql);
            try (Statement statement = conn.createStatement()) {
				//statement.setString(1, location);
				//statement.setString(1, locations_uuid_result);
				//statement.setString(2, startDate);
				//statement.setString(3, endDate);

				try (ResultSet resultSet = statement.executeQuery(sql)) {
					ResultSetMetaData metaData = resultSet.getMetaData();
					int columnCount = metaData.getColumnCount();
					System.out.println("Total Columns in Query Result: " + columnCount);

					String[] columnNames = new String[columnCount];
					for (int i = 1; i <= columnCount; i++) {
						columnNames[i - 1] = metaData.getColumnName(i);
					}

					System.out.println("Query Result Column Names:");
					for (String columnName : columnNames) {
						System.out.println(columnName);
					}

					while (resultSet.next()) {
						JSONObject jsonObject = new JSONObject();
						for (String columnName : columnNames) {
							Object value = resultSet.getObject(columnName);
							if (value == null) {
								jsonObject.put(columnName, JSONObject.NULL);
							} else {
								jsonObject.put(columnName, value);
							}
						}
						jsonArray.put(jsonObject);
					}

					resultObject.put("results", jsonArray);
					System.out.println(jsonArray.toString());
				}
			} catch (SQLException e) {
				e.printStackTrace();
				System.out.println("AMRS Core Errors: " + e.getMessage());
				return Collections.singletonMap("error", "An error occurred while fetching data.");
			}
		}
		return resultObject.toString();
	}
}
