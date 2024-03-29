package com.nicholasDaily.blogpage;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import javax.servlet.ServletContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import java.io.File;
import java.io.FileOutputStream;
import java.text.ParseException;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;

@Controller
public class updateController {
	@Autowired
	NamedParameterJdbcTemplate jdbcTemplate;
	
	@PostMapping("/login")
	public ResponseEntity<?> logIn(@RequestParam Map<String, String> params) throws ParseException{
		
		 //check database for login info
		
		String query = "SELECT * FROM users WHERE email = :email;";
		Map<String, Object> result = jdbcTemplate.queryForMap(query, new MapSqlParameterSource()
				.addValue("email", params.get("email")));
		
		boolean userAuthenticated = params.get("password").equals(result.get("password"));
		if(userAuthenticated) {
			HttpHeaders responseHeaders = new HttpHeaders();
			String sessionId = generateRandomString(60);
			
			ResponseCookie cookie = ResponseCookie.from("session_id", sessionId)
			.httpOnly(true)
			.secure(false)
			.path("/")
			.maxAge(60 * 60 * 24 * 30)
			.build();
			//update database
			LocalDate expireDate = LocalDate.now();
			String expireDateSQL = expireDate.plusDays(30).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
			String insert_session = "INSERT INTO active_sessions(session_id, user_id, expire_date) "
								+ "VALUES(:sessionId, :id, :expireDateSQL)";
			jdbcTemplate.update(insert_session, new MapSqlParameterSource()
					.addValue("sessionId", sessionId)
					.addValue("id", result.get("id"))
					.addValue("expireDateSQL", expireDateSQL));
			responseHeaders.add(HttpHeaders.SET_COOKIE, cookie.toString());
			return new ResponseEntity<>(responseHeaders, HttpStatus.OK);
		}else {
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}
	}
	
	@PostMapping("/logout")
	public ResponseEntity<?> removeCookie(@CookieValue(name="session_id", defaultValue="") String cookie){
		if(cookie.length() > 0) {
			String remove_session = "DELETE FROM active_sessions WHERE session_id = :cookie";
			jdbcTemplate.update(remove_session, new MapSqlParameterSource()
					.addValue("cookie", cookie));
			ResponseCookie response = ResponseCookie.from("session_id", null).maxAge(0).build();
			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.add(HttpHeaders.SET_COOKIE, response.toString());
			return new ResponseEntity<>(responseHeaders, HttpStatus.OK);
		}
		return new ResponseEntity<>(HttpStatus.OK);
	}
	
	@PostMapping("/logEntryValidation")
	public ResponseEntity<?> logEntryValidation(@CookieValue(name="session_id", defaultValue="") String sessionId, @RequestParam Map<String, String> parameters){
		String currentUser = getActiveUser(sessionId);
		DateTimeFormatter yyyymmdd = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		if(currentUser != null) {
			LocalDate dateToday = LocalDate.now();
			
			LocalDate date;
			if(parameters.get("entry-date").length() == 10)
				date = LocalDate.from(yyyymmdd.parse(parameters.get("entry-date")));
			else
				date = dateToday;
			double rate = 12.00;
			double hours = Double.parseDouble(parameters.get("hours"));
			String description = parameters.get("log-description");
			boolean paid = false; 
			boolean entryExisting = false;
			if(hours == 0) paid = true;
			String[] tagsTemp = parameters.get("tag-text").replaceAll("\\s", "").split("#");
			String[] tags = new String[tagsTemp.length - 1];
			for(int i = 1; i < tagsTemp.length; i++)
				tags[i - 1] = tagsTemp[i];
			LocalDate weekStart = dateToday.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
			
			boolean updateHours = ((weekStart.isBefore(date)) || currentUser.equals("Tamino") || weekStart.equals(date));
			boolean validEntry = (dateToday.equals(date) || date.isBefore(dateToday));
			String query = "SELECT id FROM programming_log WHERE log_date = :date;";
			SqlRowSet results = jdbcTemplate.queryForRowSet(query, new MapSqlParameterSource()
					.addValue("date", date.format(yyyymmdd)));
			entryExisting = results.next();
			System.out.println("existing?: " + entryExisting);
			
			if(entryExisting) {
				if(updateHours) {
					query = "UPDATE programming_log "
							+ "SET description = :description, duration = :hours WHERE log_date = :date;";
					jdbcTemplate.update(query, new MapSqlParameterSource()
							.addValue("description", description)
							.addValue("hours", hours)
							.addValue("date", date.format(yyyymmdd)));
				}else {
					query = "UPDATE programming_log "
							+ "SET description = :description WHERE log_date = :date;";
					jdbcTemplate.update(query, new MapSqlParameterSource()
							.addValue("description", description)
							.addValue("date", date.format(yyyymmdd)));
				}
			}else if(!entryExisting && validEntry) {
				if(updateHours) {
					query = "INSERT INTO programming_log(log_date, description, duration, rate, paid) "
							+ "VALUES(:date, :description, :duration, :rate, :paid);";
					jdbcTemplate.update(query, new MapSqlParameterSource()
							.addValue("date", date.format(yyyymmdd)) 
							.addValue("description", description)
							.addValue("duration", hours)
							.addValue("rate", rate)
							.addValue("paid", paid));
						
				}else {
					query = "INSERT INTO programming_log(log_date, description, rate, paid) "
							+ "VALUES(:date, :description, :rate, :paid);";
					jdbcTemplate.update(query, new MapSqlParameterSource()
							.addValue("date", date.format(yyyymmdd)) 
							.addValue("description", description)
							.addValue("rate", rate)
							.addValue("paid", paid));
				}
			}else {
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}
			query = "SELECT id FROM programming_log WHERE log_date = :date;";
			Integer logId = jdbcTemplate.queryForObject(query, new MapSqlParameterSource().addValue("date", date.format(yyyymmdd)), Integer.class);
			
			if(entryExisting) {
				query = "DELETE FROM log_to_tag WHERE log_id = :logId;";
				jdbcTemplate.update(query, new MapSqlParameterSource()
						.addValue("logId", logId.intValue()));
				
			}
			if(tags.length > 0) {
				if(logId != null) {
					Integer tagId = 0;
					for(int i = 0; i < tags.length; i++) {
						String tag = '#' + tags[i];
						tagId = getTagId(tag);
						if(tagId == null) {
							query = "INSERT INTO log_tags(tag) VALUES(:tag);";
							jdbcTemplate.update(query, new MapSqlParameterSource()
									.addValue("tag", tag));
							tagId = getTagId(tag);
						}
						query = "INSERT INTO log_to_tag(log_id, tag_id) VALUES(:logId, :tagId);";
						jdbcTemplate.update(query, new MapSqlParameterSource()
								.addValue("logId", logId.intValue())
								.addValue("tagId", tagId.intValue()));
					}
						
				}
			}
			
			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.setContentType(MediaType.APPLICATION_JSON);
			return new ResponseEntity<>(HttpStatus.OK);
		}else {
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}
	}
	
	private Integer getTagId(String tag) {
		String query = "SELECT id FROM log_tags WHERE tag = :tag;";
		try {
			return jdbcTemplate.queryForObject(query, new MapSqlParameterSource()
					.addValue("tag", tag), Integer.class);
		}catch(EmptyResultDataAccessException e) {
			return null;
		}
	}
	
	private String generateRandomString(int length) {
		String AlphaNumericString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                + "0123456789"
                + "abcdefghijklmnopqrstuvxyz"
                + "_-";
		StringBuilder sessionId = new StringBuilder(length);
		for(int i = 0; i < length; i++) {
			int index = (int)(AlphaNumericString.length() * Math.random());
			sessionId.append(AlphaNumericString.charAt(index));
		}
		
		return sessionId.toString();
	}
	
	/*[{
	 * "PostID": postid,
	 * "items":[
	 * {"type":2,
	 *  "content":"/getImg?file=filename.png"},
	 *  {"type":1,
	 *  "content:"aegrarhegra""
	 *  },
	 *  {
	 *  "type":3
	 *  "content":"/getFile?file=filename.txt"
	 *  }
	 * ]
	 * }]*/
	
	@PostMapping("/validatePost")
	public ResponseEntity<?> validatePost(@RequestParam(
			name="upload-file") MultipartFile[] files,
			@RequestParam(name="upload-image") MultipartFile[] images,
			@RequestParam(name="post-text") String text,
			@CookieValue(name="session_id", defaultValue="") String cookie){
		String json = "";
		try {
			String activeUser = getActiveUser(cookie);
			json += "[{";
			if(activeUser.length() > 0) {
				LocalDate date = LocalDate.now();
				String sql = "SELECT id FROM users WHERE name = :activeUser;";
				int id = jdbcTemplate.queryForObject(sql, 
						new MapSqlParameterSource().addValue("activeUser", activeUser) ,Integer.class);
				String query = "INSERT INTO blog_posts(user_id, date) VALUES(:userId, :date);";
				jdbcTemplate.update(query, new MapSqlParameterSource()
						.addValue("userId", id)
						.addValue("date", date.toString()));
				int postId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM blog_posts ", new MapSqlParameterSource(), Integer.class);
				json += "\"PostID\":" + postId + ",";
				json += "\"items\":[";
				query = "INSERT INTO blog_content(blog_post_id, type, content) VALUES(:blog_post_id, :type, :content);";
				if(!(files[0].getOriginalFilename().length() == 0)) {
					for(int i = 0; i < files.length; i++) {
						String filePath = "/volumes/azurefile/home/javahusky/resources/files/";
						String fileName = files[i].getOriginalFilename();
						File file = new File(filePath + fileName);
						int num = 1;
						while(file.exists()) {
							String[] fileNameParts = files[i].getOriginalFilename().split("\\."); 
							System.out.println("Name: " + files[i].getOriginalFilename());
							System.out.println("Parts: " + Arrays.toString(files[i].getOriginalFilename().split(".")));
							
							fileName = fileNameParts[0] + "(" + num + ")." + fileNameParts[fileNameParts.length - 1];
							file = new File(filePath + fileName);
							num++;
						}
						FileOutputStream output = new FileOutputStream(file);
						output.write(files[i].getBytes());
						output.close();
						jdbcTemplate.update(query, new MapSqlParameterSource()
								.addValue("blog_post_id", postId) 
								.addValue("type", 3)
								.addValue("content", "files/" + fileName));
						json += "{";
						json += "\"type\":3,";
						json += "\"content\":\"/getFile?file=" + jsonEntities(fileName) + "\"";
						json += "},";
						
					}
				}
				if(!(images[0].getOriginalFilename().length() == 0)) {
					for(int i = 0; i < images.length; i++) {
						System.out.println(images[i].getOriginalFilename());
						String[] fileNameSections = images[i].getOriginalFilename().split("\\.");
						System.out.println(Arrays.toString(fileNameSections));
						String fileExtention = '.' + fileNameSections[fileNameSections.length - 1];
						String newFileName = generateRandomString(15) + fileExtention;
						String filePath = "/volumes/azurefile/home/javahusky/resources/images/";
						File path = new File(filePath + newFileName);
						while(path.exists()) {
							newFileName = generateRandomString(15) + fileExtention;
							path = new File(filePath + newFileName);
						}
						FileOutputStream output = new FileOutputStream(path);
						output.write(images[i].getBytes());
						output.close();
						System.out.println(newFileName);
						jdbcTemplate.update(query, new MapSqlParameterSource()
								.addValue("blog_post_id", postId) 
								.addValue("type", 2)
								.addValue("content", "img/" + newFileName));
						json += "{";
						json += "\"type\":2,";
						json += "\"content\":\"/getImg?file=" + jsonEntities(newFileName) + "\"";
						json += "},";
					}
				}
				if(text.length() > 0) {
					jdbcTemplate.update(query, new MapSqlParameterSource()
							.addValue("blog_post_id", postId) 
							.addValue("type", 1)
							.addValue("content", text));
					json += "{";
					json += "\"type\":1,";
					json += "\"content\":\"" + jsonEntities(text) + "\"";
					json += "},";
				}
				System.out.println("JSON BEFORE: " + json);
				json = json.substring(0, json.length() - 1) + "]";
				System.out.println("JSON AFTER: " + json);
			    json += "}]";
			    
			}
			
		}catch(Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.APPLICATION_JSON);
		return new ResponseEntity<>(json, responseHeaders, HttpStatus.OK);
	}
	
	@PostMapping("/updatePayStatus")
	public ResponseEntity<?> updatePayStatus(@CookieValue(name="session_id", defaultValue="") String session, @RequestParam Map<String, String> params){
		DateTimeFormatter yyyymmdd = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		boolean isWeek = params.get("isWeek").equals("TRUE");
		boolean value = params.get("value").equals("TRUE");
		LocalDate date = LocalDate.from(yyyymmdd.parse(params.get("identifier")));
		String activeUser = getActiveUser(session);
		if(activeUser != null && activeUser.equals("Tamino")) {
			if(isWeek) {
				LocalDate weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
				LocalDate weekEnd = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
				String query = "UPDATE programming_log SET paid = :value WHERE log_date BETWEEN :weekStart AND :weekEnd;";
				int rowsEffected = jdbcTemplate.update(query, new MapSqlParameterSource()
						.addValue("value", value)
						.addValue("weekStart", weekStart.format(yyyymmdd))
						.addValue("weekEnd", weekEnd.format(yyyymmdd)));
				if(rowsEffected > 0) 
					return new ResponseEntity<>(HttpStatus.OK);
				else
					return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
				
			}else {
				String query = "UPDATE programming_log SET paid = :value WHERE log_date = :date;";
				int rowsEffected = jdbcTemplate.update(query, new MapSqlParameterSource()
						.addValue("value", value)
						.addValue("date", date.format(yyyymmdd)));
				if(rowsEffected > 0)
					return new ResponseEntity<>(HttpStatus.OK);
				else
					return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}
		}else {
			return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
		}
	}
	
	private String getActiveUser(String session_id) {
		Map<String, Object> result;
		String user = "";
		if(session_id.length() > 0) {
			String sql = "SELECT users.name, active_sessions.expire_date "
					+ "FROM users "
					+ "INNER JOIN active_sessions ON users.id=active_sessions.user_id "
					+ "WHERE active_sessions.session_id = :sessionID";
			 result = jdbcTemplate.queryForMap(sql, new MapSqlParameterSource()
					.addValue("sessionID", session_id));
			user = (String) result.get("name");
			System.out.println();
			Date expires= Date.from(Instant.now());
			expires = (Date) result.get("expire_date");
			Date today = Date.from(Instant.now());
			if(today.after(expires)) {
				return null;
			}
		}else {
			return null;
		}
		
		return user;
		
	}
	
	private String jsonEntities(String json) {
		json = json.replaceAll("\\\\", "\\\\\\\\");
		json = json.replaceAll("\"", "\\\\\"");
		json = json.replaceAll("\\n", "\\\\n");
		json = json.replaceAll("\\r", "\\\\r");
		json = json.replaceAll("\\t", "\\\\t");
		return json;
	}
}
