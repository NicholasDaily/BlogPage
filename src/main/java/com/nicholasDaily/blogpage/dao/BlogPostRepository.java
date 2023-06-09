package com.nicholasDaily.blogpage.dao;

import com.nicholasDaily.blogpage.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlogPostRepository extends JpaRepository<Post, Integer>{

} 
