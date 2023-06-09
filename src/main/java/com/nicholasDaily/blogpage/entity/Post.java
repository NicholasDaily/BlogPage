package com.nicholasDaily.blogpage.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import java.time.LocalDate;

@Entity
@Table(name="blog_posts")
public class Post {
	@Id
	@Column(name="id")
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	int id;
	
	@Column(name="date")
	LocalDate date;

	@Column(name="user_id")
	int userId;

	public Post(){}

	public Post(int id, LocalDate date, int userId){
		this.id = id;
		this.date = date;
		this.userId = userId;
	}

    public void setId(int id){
		this.id = id;
	}

	public void setDate(LocalDate date){
		this.date = date;
	}

	public void setUserId(int userId){
		this.userId = userId;
	}

	public int getId(){
		return this.id;
	}

	public LocalDate getDate(){
		return this.date;
	}

	public int userId(){
		return this.userId;
	}
}
