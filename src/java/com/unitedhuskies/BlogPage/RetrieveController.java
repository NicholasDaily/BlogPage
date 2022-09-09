package com.unitedhuskies.BlogPage;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;

@Controller
public class RetrieveController {
	@Autowired
	NamedParameterJdbcTemplate jdbcTemplate;
	
	@GetMapping("/getPosts")
	public ResponseEntity<String> getPosts(@RequestParam Map<String, String> params){
		String json = "";
		String query = "";
		
		SqlRowSet result;
		if(params.get("has_posts").equals("true")) {
			query = "SELECT id from blog_posts WHERE id < :postId ORDER BY id DESC LIMIT :postLimit;";
			result = jdbcTemplate.queryForRowSet(query, new MapSqlParameterSource()
					.addValue("postId", Integer.parseInt(params.get("post_id")))
					.addValue("postLimit", Integer.parseInt(params.get("post_limit"))));
		}else {
			query = "SELECT id from blog_posts ORDER BY id DESC LIMIT :postLimit";
			result = jdbcTemplate.queryForRowSet(query, new MapSqlParameterSource()
					.addValue("postLimit", Integer.parseInt(params.get("post_limit"))));
		}
		 
		json += "[";
		boolean hasPosts = false;
		while(result.next()) {
			hasPosts = true;
			int post_id = result.getInt("id");
			json += "{";
			json += "\"PostID\":" + post_id + ",";
			json += "\"items\":[";
			query = "SELECT * FROM blog_content WHERE blog_post_id = :postId;";
			SqlRowSet post_content = jdbcTemplate.queryForRowSet(query, new MapSqlParameterSource()
					.addValue("postId", post_id));
			boolean noContent = true;
			while(post_content.next()) {
				noContent = false;
				int type = post_content.getInt("type");
				String content = post_content.getString("content");
				if(type == 2 || type == 3) {
					if(content.contains("/")) {
						String[] contentSegments = content.split("/");
						content = contentSegments[contentSegments.length - 1];
					}
				}
				if(type == 2) 
					content = "/getImg?file=" + content;
				if(type == 3)
					content = "/getFile?file=" + content;
				json += "{";
				json+= "\"type\":" + type + ",";
				if(type == 1) 
					content = jsonEntities(content);
				json += "\"content\":\"" + content + "\"";
				json += "},";
			}
			
			if(!noContent)
				json = json.substring(0, json.length() - 1) + "]},";
			else {
				json += "]},";
			}
		}
		if(!hasPosts)
			return new ResponseEntity<String>("", HttpStatus.OK);
		json = json.substring(0, json.length() - 1) + "]";
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.APPLICATION_JSON);
		return new ResponseEntity<String>(json, responseHeaders, HttpStatus.OK);
	}
	
	@GetMapping("/getLogEntries")
	public ResponseEntity<String> getLogEntries(@RequestParam (name="date") Optional<String> dateParam, @CookieValue(name="session_id", defaultValue="") String session_id) throws ParseException{
		boolean userLoggedIn = getActiveUser(session_id) == null ? false : true;
		DateTimeFormatter yyyymmdd = DateTimeFormatter.ofPattern("yyy-MM-dd");
		DateTimeFormatter ddmmyyyy = DateTimeFormatter.ofPattern("dd-MM-yyyy");
		String json = "";
		json += "[";
		String dateString = dateParam.orElseGet(() -> LocalDate.now().format(yyyymmdd));
		LocalDate date = Instant.ofEpochMilli(new SimpleDateFormat("yyyy-MM-dd").parse(dateString).getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
		LocalDate mon = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
		LocalDate sun = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
		String query = "SELECT * FROM programming_log WHERE log_date BETWEEN "
		 + ":date_start AND :date_end ORDER BY log_date ASC;";
		SqlRowSet results = jdbcTemplate.queryForRowSet(query, new MapSqlParameterSource()
				.addValue("date_start", mon.format(yyyymmdd))
				.addValue("date_end", sun.format(yyyymmdd)));
		boolean endReached = false;
		if(!results.next()) {
			endReached = true;
		}
		
		for(int i = 0; i < 7; i++){
			json += "{";
			double rate = 12.00;
			LocalDate dayDate = mon.plusDays(i);
			LocalDate dayFromResults; 
			if(!endReached) {
				dayFromResults = Instant.ofEpochMilli(new SimpleDateFormat("yyyy-MM-dd").parse(results.getString("log_date")).getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
			}else {
				dayFromResults = null;
			}
			String dayDateFormatted = dayDate.format(ddmmyyyy);
			String description = "No entry for " + dayDateFormatted;
			ArrayList<String> tags = new ArrayList<String>();
			
			double hours = 0.00;
			double pay = 0.00;
			boolean status = true;
			
			if(dayDate.equals(dayFromResults)) {
				description = results.getString("description");
				hours = results.getDouble("duration");
				status = results.getBoolean("paid");
				hours = results.getDouble("duration");
				status = results.getBoolean("paid");
				int logId = results.getInt("id");
				pay = hours * rate;
				String tagQuery = ""
						+ "SELECT log_tags.tag\n"
						+ "FROM log_to_tag\n"
						+ "INNER JOIN log_tags ON log_to_tag.tag_id=log_tags.id\n"
						+ "INNER JOIN programming_log ON log_to_tag.log_id=programming_log.id\n"
						+ "WHERE programming_log.id = :logId;";
				SqlRowSet tagResults = jdbcTemplate.queryForRowSet(tagQuery, new MapSqlParameterSource()
						.addValue("logId", logId));
				while(tagResults.next()) {
					tags.add(tagResults.getString("tag"));
				}
				endReached = !results.next();
			}
			
			json += "\"date\":" + "\"" + dayDateFormatted + "\",";
			json += "\"duration\":" + hours + ",";
			json += "\"description\":" + "\"" + jsonEntities(description) + "\",";
			json += "\"tags\":" + "[";
			for(int j = 0; j < tags.size(); j++) {
				json += "\"" + tags.get(j) + "\"";
				if(!(j == tags.size() - 1)) {
					json += ",";
				}
			}
			json += "]";
			json += ",";
			if(userLoggedIn) {
				json += "\"loggedIn\":" + "true,";
				json += "\"status\":" + status + ",";
				json += "\"amount\":" + pay;
			}else {
				json += "\"loggedIn\":" + "false";
			}
			
			json += "},";
			
		}
		json = json.substring(0, json.length() - 1) + "]";
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.APPLICATION_JSON);
		return new ResponseEntity<String>(json, responseHeaders, HttpStatus.OK);
	}
	
	private String getActiveUser(String session_id) {
		Map<String, Object> result;
		String user = "";
		if(session_id.length() > 0) {
			String sql = "SELECT users.name, active_sessions.expire_date "
					+ "FROM users "
					+ "INNER JOIN active_sessions ON users.id=active_sessions.user_id "
					+ "WHERE active_sessions.session_id = :session_id;";
			result = jdbcTemplate.queryForMap(sql, new MapSqlParameterSource()
					.addValue("session_id", session_id));
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
	
	@GetMapping("/getDownloads")
	public ResponseEntity<String> getDownloads(){
		String json = "{\"links\":[";
		String sql = "SELECT * FROM blog_content WHERE type = 3 ORDER BY id DESC";
		SqlRowSet response = jdbcTemplate.queryForRowSet(sql, new MapSqlParameterSource());
		
		while(response.next()) {
			String content = response.getString("content");
			if(content.contains("/")) {
				String[] contentSegments = content.split("/");
				content = contentSegments[contentSegments.length - 1];
			}
			content = "/getFile?file=" + content;
			json += "\"" + content + "\", ";
		}
		
		json = json.substring(0, json.length() - 2) + "]}";;
		
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.APPLICATION_JSON);
		return new ResponseEntity<String>(json, responseHeaders, HttpStatus.OK);
	}
	
	@GetMapping("/getApps")
	public ResponseEntity<String> getApps(){
		String json = "{\"links\":[";
		String sql = "SELECT * FROM online_apps ORDER BY id DESC";
		SqlRowSet response = jdbcTemplate.queryForRowSet(sql, new MapSqlParameterSource());
		
		while(response.next()) {
			String content = response.getString("src");
			content = "/online-apps/" + content;
			json += "\"" + content + "\", ";
		}
		
		json = json.substring(0, json.length() - 2) + "]}";;
		
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.APPLICATION_JSON);
		return new ResponseEntity<String>(json, responseHeaders, HttpStatus.OK);
	}
	
	@GetMapping("/userLoggedIn")
	public ResponseEntity<?> userLoggedIn(@CookieValue(name="session_id", defaultValue="") String session_id) throws DataAccessException, ParseException{
		HttpStatus status;
		HttpHeaders responseHeaders = new HttpHeaders();
		boolean updateCookie = false;
		Map<String, Object> result;
		String user = "";
		if(session_id.length() > 0) {
			String sql = "SELECT users.name, active_sessions.expire_date "
					+ "FROM users "
					+ "INNER JOIN active_sessions ON users.id=active_sessions.user_id "
					+ "WHERE active_sessions.session_id = :session_id;";
			try {
			result = jdbcTemplate.queryForMap(sql, new MapSqlParameterSource()
					.addValue("session_id", session_id));
			user = (String) result.get("name");
			System.out.println();
			Date expires= Date.from(Instant.now());
			expires = (Date) result.get("expire_date");
			Date today = Date.from(Instant.now());
			if(today.after(expires)) {
				return null;
			}else if(15 >= Math.abs(today.compareTo(expires))) {
				updateCookie = true;
			}
			}catch(EmptyResultDataAccessException e) {
				user = null;
			}
			
		}else {
			user = null;
		}
		
		
		if(user != null) {
			if(updateCookie) {
				ResponseCookie cookie = ResponseCookie.from("session_id", session_id)
						.httpOnly(true)
						.secure(false)
						.path("/")
						.maxAge(60 * 60 * 24 * 30)
						.build();
				LocalDate expireDate = LocalDate.now();
				SimpleDateFormat sqlFormatDate = new SimpleDateFormat("yyyy-MM-dd");
				Calendar c = Calendar.getInstance();
				c.setTime(sqlFormatDate.parse(expireDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))));
				c.add(Calendar.DATE, 30);
				String expireDateSQL = sqlFormatDate.format(c.getTime());
				String insert_session = "UPDATE active_sessions "
						+ "SET expire_date = :expireDateSQL "
						+ "WHERE session_id = :session_id;";
				jdbcTemplate.update(insert_session, new MapSqlParameterSource()
						.addValue("expireDateSQL", expireDateSQL)
						.addValue("session_id", session_id));
				responseHeaders.add(HttpHeaders.SET_COOKIE, cookie.toString());
			}
			status = HttpStatus.OK;
		}else {
			if(session_id.length() > 0) {
				String remove_session = "DELETE FROM active_sessions WHERE session_id = :session_id";
				jdbcTemplate.update(remove_session, new MapSqlParameterSource()
						.addValue("session_id", session_id));
				ResponseCookie response = ResponseCookie.from("session_id", null).maxAge(0).build();
				responseHeaders.add(HttpHeaders.SET_COOKIE, response.toString());
			}
			status = HttpStatus.FORBIDDEN;
		}
		return new ResponseEntity<>(responseHeaders, status);
	}
	
	@GetMapping("/getImg")
	public ResponseEntity<?> getImage(@RequestParam(name="file") String fileName) throws IOException{
		if(!fileName.contains("\\") && !fileName.contains("/")) {
			
			HttpHeaders responseHeaders = new HttpHeaders();
			Optional<MediaType> mediaType = MediaTypeFactory.getMediaType(fileName);
			String dir;
			
			dir = "/volumes/azurefile/home/javahusky/resources/images/";
			System.out.println("MediaType: " + mediaType.get());
			String path = dir + fileName;
			System.out.println("Path: " + path);
			responseHeaders.setContentType(mediaType.get());
			byte[] fileContent = Files.readAllBytes(new File(path).toPath());
			return ResponseEntity.ok().headers(responseHeaders).body(fileContent);
		}else {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		
	}
	
	@GetMapping("/getFile")
	public ResponseEntity<?> getFile(@RequestParam(name="file") String fileName) throws IOException{
		if(!fileName.contains("\\") && !fileName.contains("/")) {
			
			HttpHeaders responseHeaders = new HttpHeaders();
			Optional<MediaType> mediaType = MediaTypeFactory.getMediaType(fileName);
			String dir;
			dir = "/volumes/azurefile/home/javahusky/resources/files/";
			System.out.println("MediaType: " + mediaType.get());
			String path = dir + fileName;
			System.out.println("Path: " + path);
			responseHeaders.setContentType(mediaType.get());
			responseHeaders.set(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + fileName);
			byte[] fileContent = Files.readAllBytes(new File(path).toPath());
			return ResponseEntity.ok().headers(responseHeaders).body(fileContent);
		}else {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		
	}
	
}
