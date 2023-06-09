package com.nicholasDaily.blogpage.dao;

import com.nicholasDaily.blogpage.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Integer>{

} 
