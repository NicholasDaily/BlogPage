package com.nicholasDaily.blogpage.dao;

import com.nicholasDaily.blogpage.entity.PostContent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlogContentRepository extends JpaRepository<PostContent, Integer>{

} 
