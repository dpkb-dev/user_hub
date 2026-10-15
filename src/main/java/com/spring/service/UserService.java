package com.spring.service;

import com.spring.dto.*;
import com.spring.entities.City;

import java.util.List;


public interface UserService {

    public List<CountryDto> getCountries();
    public List<StateDto> getStates(Integer countryId);
    public List<CityDto> getCities(Integer cityId);
    public boolean isEmailUnique(String email);
    public boolean register(UserDto userDto);
    public UserDto login(String email, String pwd);
    public boolean resetPwd(ResetPwdDto resetPwdDto);
    public QuoteApiResponseDto getQuote();

}
