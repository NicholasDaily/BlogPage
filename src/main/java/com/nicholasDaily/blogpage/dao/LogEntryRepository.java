package com.nicholasDaily.blogpage.dao;

import com.nicholasDaily.blogpage.entity.LogEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogEntryRepository extends JpaRepository<LogEntry, Integer>{

} 
