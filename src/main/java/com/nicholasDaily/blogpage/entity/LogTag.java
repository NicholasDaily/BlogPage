package com.nicholasDaily.blogpage.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;

@Entity
@Table(name="log_tags")
public class LogTag {
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name="id")
	int id;

	@Column(name="tag")
	String tag;

	public LogTag(){}

	public LogTag(int id, String tag){
		this.id = id;
		this.tag = tag;
	}

	public void setId(int id){
		this.id = id;
	}

	public void setTag(String tag){
		this.tag = tag;
	}

	public int getId(){
		return this.id;
	}

	public String getTag(){
		return this.tag;
	}
}
