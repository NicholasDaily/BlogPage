package com.nicholasDaily.blogpage.entity;

import jakarta.persistence.*;

@Entity
@Table(name="log_to_tag")
@IdClass(LogToTagCompositeKey.class)
public class LogToTag{
	@Id
	@Column(name="log_id")
	int logId;
	@Id
	@Column(name="tag_id")
	int tagId;

	public LogToTag(){}

	public LogToTag(int logId, int tagId){
		this.logId = logId;
		this.tagId = tagId;
	}

	public void setLogId(int logId){
		this.logId = logId;
	}

	public void setTagId(int tagId){
		this.tagId = tagId;
	}

	public int getLogId(){
		return this.logId;
	}

	public int getTagId(){
		return this.tagId;
	}
}

