package com.nicholasDaily.blogpage.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;

@Entity
@Table(name="blog_content")
public class PostContent {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name="id")
	int id;

	@Column(name="blog_post_id")
	int blogPostId;

	@Column(name="type")
	int type;

	@Column(name="content")
	String content;

	public PostContent(){}

	public PostContent(int id, int blogPostId, int type, String content){
		this.id = id;
		this.blogPostId = blogPostId;
		this.type = type;
		this.content = content;
	}

	public void setId(int id){
		this.id = id;
	}

	public void setBlogPostId(int blogPostId){
		this.blogPostId = blogPostId;
	}

	public void setType(int type){
		this.type = type;
	}

	public void setContent(String content){
		this.content = content;
	}

	public int getId(){
		return this.id;
	}

	public int getBlogPostId(){
		return this.blogPostId;
	}

	public int getType(){
		return this.type;
	}

	public String getContent(){
		return this.content;
	}

}
