package org.openmrs.module.amrscore.api.utils;

public class ReportRequest {
	
	private String locationUuid;
	
	private String report_id;
	
	private String start_date;
	
	private String end_date;
	
	private String sp_name;
	
	private String uuid;
	
	private String parent_locationUuid;
	
	private String parent_locationName;
	
	private String user_uuid;
	
	private String user_name;
	
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
	
	public String getUuid() {
		return uuid;
	}
	
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	
	public String getParent_locationUuid() {
		return parent_locationUuid;
	}
	
	public void setParent_locationUuid(String parent_locationUuid) {
		this.parent_locationUuid = parent_locationUuid;
	}
	
	public String getParent_locationName() {
		return parent_locationName;
	}
	
	public void setParent_locationName(String parent_locationName) {
		this.parent_locationName = parent_locationName;
	}
	
	public String getUser_uuid() {
		return user_uuid;
	}
	
	public void setUser_uuid(String user_uuid) {
		this.user_uuid = user_uuid;
	}
	
	public String getUser_name() {
		return user_name;
	}
	
	public void setUser_name(String user_name) {
		this.user_name = user_name;
	}
}
