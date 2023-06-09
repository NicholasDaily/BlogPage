package com.nicholasDaily.blogpage.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import java.time.LocalDate;

@Entity
@Table(name="active_sessions")
public class Session {
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name="session_id")
	String sessionId;

	@Column(name="user_id")
	int userId;

	@Column(name="expire_date")
	LocalDate expireDate;

	public Session(){}

	public Session(String sessionId, int userId, LocalDate expireDate){
		this.sessionId = sessionId;
		this.userId = userId;
		this.expireDate = expireDate;
	}

	public void setSessionId(String sessionId){
		this.sessionId = sessionId;
	}

	public void setUserId(int userId){
		this.userId = userId;
	}

	public void setExpireDate(){
		this.expireDate = expireDate;
	}

	public String getSessionId(){
		return this.sessionId;
	}

	public int getUserId(){
		return this.userId;
	}

	public LocalDate getExpireDate(){
		return this.expireDate;
	}
}
