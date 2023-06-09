package com.nicholasDaily.blogpage.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import java.time.LocalDate;

@Entity
@Table(name="programming_log")
public class LogEntry {
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name="id")
	int id;
	
	@Column(name="log_date")
	LocalDate logDate;

	@Column(name="description")
	String description;

	@Column(name="duration")
	double duration;

	@Column(name="rate")
	double rate;

	@Column(name="paid")
	boolean paid;

	public LogEntry(){}

	public LogEntry(int id, LocalDate logDate, String description, double duration, double rate, boolean paid){
		this.id = id;
		this.logDate = logDate;
		this.description = description;
		this.duration = duration;
		this.rate = rate;
		this.paid = paid;
	}

	public void setId(int id){
		this.id = id;
	}

	public void setLogDate(LocalDate logDate){
		this.logDate = logDate;
	}

	public void setDescription(String description){
		this.description = description;
	}

	public void setDuration(double duration){
		this.duration = duration;
	}

	public void setRate(double rate){
		this.rate = rate;
	}

	public void setPaid(boolean paid){
		this.paid = paid;
	}

	public int getId(){
		return this.id;
	}

	public LocalDate getLogDate(){
		return this.logDate;
	}

	public String getDescription(){
		return this.description;
	}

	public double getDuration(){
		return this.duration;
	}

	public double getRate(){
		return this.rate;
	}

	public boolean getPaid(){
		return this.paid;
	}
}
