package com.spring.repo;

import com.spring.entities.City;
import com.spring.entities.State;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CityRepository extends JpaRepository<City, Integer> {

    public List<City> findByStateStateId(Integer countryId);
}
