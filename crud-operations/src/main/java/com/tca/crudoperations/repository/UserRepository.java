package com.tca.crudoperations.repository;

import com.tca.crudoperations.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {




}
