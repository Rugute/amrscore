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
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Base64;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

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
	
	@RequestMapping(method = RequestMethod.GET, value = "/patient")
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
		String username = "erugut";
		String password = "nNoel@2019";
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
	
	@RequestMapping(method = RequestMethod.GET, value = "/flags")
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
	
	@RequestMapping(method = RequestMethod.GET, value = "/default-facility")
	@ResponseBody
	public Object getDefaultConfiguredFacility() {
		
		Location location = Context.getUserContext().getLocation();
		ObjectNode locationNode = JsonNodeFactory.instance.objectNode();
		
		locationNode.put("locationId", location.getLocationId());
		locationNode.put("uuid", location.getUuid());
		locationNode.put("display", location.getName());
		
		return locationNode.toString();
		
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/facilityName")
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
	
	@RequestMapping(method = RequestMethod.GET, value = "/reports")
	@ResponseBody
	public Object getAllReportsNames() {
		JSONObject resultObject = new JSONObject();

		ObjectNode reportsNode = JsonNodeFactory.instance.objectNode();
		Location location = Context.getUserContext().getLocation();
		ObjectMapper mapper = new ObjectMapper();
		ArrayNode reportsArray = mapper.createArrayNode();

		//HttpServletRequest request, @RequestParam("locationUuid") String locationUuid
			try (Connection conn = getConnection()) {
				 Statement statement = conn.createStatement();
				 ResultSet resultSet = statement.executeQuery("SELECT * FROM amrs_etl.reports order by reports_name asc;");
				while (resultSet.next()) {
					ObjectNode rowNode = mapper.createObjectNode();
					rowNode.put("id", resultSet.getString("id"));
					rowNode.put("report_name", resultSet.getString("report_name"));
					rowNode.put("category", resultSet.getString("category"));
					rowNode.put("status", resultSet.getString("status"));
					rowNode.put("status", resultSet.getString("status"));
					// Add more columns as needed, e.g., rowNode.put("anotherColumn", resultSet.getString("anotherColumn"));
					reportsArray.add(rowNode);
					// Example: Get data from resultSet and add it to locationNode
					// reportsNode.put("columnName", resultSet.getString("columnName"));
				}
			} catch (SQLException e) {
				e.printStackTrace();
				System.out.println("AMRS Core Errors "+ e.getMessage());
			}
		resultObject.put("result", reportsArray);

		return resultObject.toString();

	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/reports/category")
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
				// Add more columns as needed, e.g., rowNode.put("anotherColumn", resultSet.getString("anotherColumn"));
				reportsArray.add(rowNode);
				// Example: Get data from resultSet and add it to locationNode
				// reportsNode.put("columnName", resultSet.getString("columnName"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
			System.out.println("AMRS Core Errors "+ e.getMessage());
		}
		resultObject.put("result", reportsArray);

		return resultObject.toString();

	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/reports/logs")
	@ResponseBody
	public Object getReportsLogs(HttpServletRequest request, @RequestParam("locationUuid") String locationUuid,@RequestParam("report_id") String report_id) {
		ObjectNode reportsNode = JsonNodeFactory.instance.objectNode();
		Location location = Context.getUserContext().getLocation();
		ObjectMapper mapper = new ObjectMapper();
		ArrayNode reportsArray = mapper.createArrayNode();
        //System.out.println("Current Location "+ location.getUuid());

		try (Connection conn = getConnection()) {
			Statement statement = conn.createStatement();
			ResultSet resultSet = statement.executeQuery("SELECT * FROM amrs_etl.reports r\n" +
					"inner join reports_logs rl on r.id=rl.report_id where locationuuid="+ locationUuid +" and report_id="+ report_id+";");
			while (resultSet.next()) {
				ObjectNode rowNode = mapper.createObjectNode();
				rowNode.put("id", resultSet.getString("id"));
				rowNode.put("report_name", resultSet.getString("report_name"));
				rowNode.put("description", resultSet.getString("description"));
				rowNode.put("category", resultSet.getString("description"));
				rowNode.put("start_date", resultSet.getString("start_date"));
				rowNode.put("end_date", resultSet.getString("end_date"));
				rowNode.put("month", resultSet.getString("month"));
				rowNode.put("year", resultSet.getString("year"));
				rowNode.put("status_code", resultSet.getString("status"));
				rowNode.put("sp_name", resultSet.getString("sp_name"));
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

		return reportsArray.toString();

	}
	
	@RequestMapping(method = RequestMethod.POST, value = "/reports/generate")
	@ResponseBody
	public Object postReportsLogs(HttpServletRequest request,
								  @RequestBody ReportRequest reportRequest) {
		ObjectNode reportsNode = JsonNodeFactory.instance.objectNode();
		Location location = Context.getUserContext().getLocation();
		ObjectMapper mapper = new ObjectMapper();
		ArrayNode reportsArray = mapper.createArrayNode();
		try (Connection conn = getConnection()) {

			String sql = "INSERT INTO amrs_etl.reports_logs (locationUuid, report_id, start_date,end_date,month,year,status) VALUES (?, ?, ?, ?, ?, ?, ?)";
			PreparedStatement preparedStatement = conn.prepareStatement(sql);
			preparedStatement.setString(1, reportRequest.getLocationUuid());
			preparedStatement.setString(2, reportRequest.getReport_id());
			preparedStatement.setString(3, reportRequest.getStart_date());
			preparedStatement.setString(4, reportRequest.getEnd_date());
			/*preparedStatement.setString(1, locationUuid);
			preparedStatement.setString(2, report_id);
			preparedStatement.setString(3, start_date);
			preparedStatement.setString(4, end_date); */
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
			// Parse the dates
			LocalDate startDate = LocalDate.parse(reportRequest.getStart_date(), formatter);
			LocalDate endDate = LocalDate.parse(reportRequest.getEnd_date(), formatter);

			String startMonthName = startDate.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
			String endMonthName = endDate.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
			String year = String.valueOf(startDate.getYear());
			preparedStatement.setString(5, startMonthName);
			preparedStatement.setString(6, year);
			preparedStatement.setString(7, "0");
			int rowsAffected = preparedStatement.executeUpdate();
			ObjectNode rowNode = mapper.createObjectNode();
			rowNode.put("Rows inserted", rowsAffected);
			reportsArray.add(rowNode);
			// Generate the report

			String sp_sql = "{CALL "+reportRequest.getSp_name()+"(?, ?, ?)}";
			CallableStatement callableStatement = conn.prepareCall(sp_sql);
				// Set the values for the parameters
				callableStatement.setString(1, reportRequest.getLocationUuid()); //locationUUid
				callableStatement.setString(2, reportRequest.getStart_date()); //Startdate
				callableStatement.setString(3, reportRequest.getEnd_date()); // Enddate

				// Execute the stored procedure
				callableStatement.execute();

				//Update status and TAT

				//Done
				System.out.println("Stored procedure executed successfully.");
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
			System.out.println("AMRS Core Errors "+ e.getMessage());
		}
		return reportsArray.toString();

	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/reports/view")
	@ResponseBody
	public Object getHTSData(HttpServletRequest request) throws SQLException {
		JSONArray jsonArray = new JSONArray();
		JSONObject resultObject = new JSONObject();

		try (Connection conn = getConnection()) {
			String sql = "SELECT * FROM amrs_etl.etl_hts_test limit 10";
			try (PreparedStatement statement = conn.prepareStatement(sql);
				 ResultSet resultSet = statement.executeQuery()) {

				ResultSetMetaData metaData = resultSet.getMetaData();
				int columnCount = metaData.getColumnCount();
				System.out.println("Total Columns: " + columnCount);

				String[] columnNames = new String[columnCount];
				for (int i = 1; i <= columnCount; i++) {
					columnNames[i - 1] = metaData.getColumnName(i);
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
				resultObject.put("result", jsonArray);
				System.out.println(jsonArray.toString());
			} catch (SQLException e) {
				e.printStackTrace();
				System.out.println("AMRS Core Errors " + e.getMessage());
				// Return an error response
				return Collections.singletonMap("error", "An error occurred while fetching data.");
			}

			//return jsonArray.toString();
			return  resultObject.toString();
		}
	}
}
