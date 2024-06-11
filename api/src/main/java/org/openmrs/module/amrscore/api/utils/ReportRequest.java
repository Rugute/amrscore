package org.openmrs.module.amrscore.api.utils;

public class ReportRequest {
	
	private String locationUuid;
	
	private String report_id;
	
	private String start_date;
	
	private String end_date;
	
	private String sp_name;
	
	// Getters and Setters
	public String getLocationUuid() {
		return locationUuid;
	}
	
	public void setLocationUuid(String locationUuid) {
		this.locationUuid = locationUuid;
	}
	
	public String getReport_id() {
		return report_id;
	}
	
	public void setReport_id(String report_id) {
		this.report_id = report_id;
	}
	
	public String getStart_date() {
		return start_date;
	}
	
	public void setStart_date(String start_date) {
		this.start_date = start_date;
	}
	
	public String getEnd_date() {
		return end_date;
	}
	
	public void setEnd_date(String end_date) {
		this.end_date = end_date;
	}
	
	public String getSp_name() {
		return sp_name;
	}
	
	public void setSp_name(String sp_name) {
		this.sp_name = sp_name;
	}
}
