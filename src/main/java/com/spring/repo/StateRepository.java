package com.spring.repo;

import com.spring.entities.State;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StateRepository extends JpaRepository<State, Integer> {

    public List<State> findByCountryCountryId(Integer countryId);
}
