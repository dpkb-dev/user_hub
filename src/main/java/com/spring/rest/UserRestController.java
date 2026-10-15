package com.spring.rest;

import com.spring.dto.*;
import com.spring.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name= "User APIs")
public class UserRestController {

    private final UserService userService;

    /*http://localhost:8080/countries*/
    @Operation(summary = "Get Countries", description = "Fetches a list of countries from the database")
    @GetMapping("/countries")
    public ResponseEntity<ApiResponse<List<CountryDto>>> getCountries(){

        log.debug("getCountries() - execution started");

        ApiResponse<List<CountryDto>> response = new ApiResponse<>();
        List<CountryDto> countries = userService.getCountries();
        if(countries.isEmpty()){
            response.setStatus(500);
            response.setMessage("No countries found");
            response.setData(null);
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }else{
            response.setStatus(200);
            response.setMessage("Fetched Countries Successfully");
            response.setData(countries);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }

    }

    /*http://localhost:8080/states/1*/
    @GetMapping("/states/{countryId}")
    public ResponseEntity<ApiResponse<List<StateDto>>> getStates(@PathVariable Integer countryId){
     ApiResponse<List<StateDto>> response= new ApiResponse<>();
        List<StateDto> states = userService.getStates(countryId);
        if (states.isEmpty()) {
            response.setStatus(500);
            response.setMessage("No States Found");
            response.setData(null);
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        } else {
            response.setStatus(200);
            response.setMessage("States fetched Successfully");
            response.setData(states);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
    }

    @GetMapping("/cities/{stateId}")
    public ResponseEntity<ApiResponse<List<CityDto>> >getCities(@PathVariable Integer stateId){
        ApiResponse<List<CityDto>> response=new ApiResponse<>();
        List<CityDto> cities=userService.getCities(stateId);
        if(cities.isEmpty()){
            response.setStatus(500);
            response.setMessage("No Cities Found");
            response.setData(null);
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        } else{
            response.setStatus(200);
            response.setMessage("Cities Fetched Successfully");
            response.setData(cities);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }

    }

    @GetMapping("/unique/{email}")
    public ResponseEntity<ApiResponse<String>> checkEmail(@PathVariable String email){
        ApiResponse<String> response=new ApiResponse();
        boolean isEmailUnique = userService.isEmailUnique(email);
        if(isEmailUnique){
            response.setStatus(200);
            response.setMessage("Email is Unique");
            response.setData("UNIQUE");
            return new ResponseEntity<>(response, HttpStatus.OK);
        }else{
            response.setStatus(500);
            response.setMessage("Email is not Unique means duplicate");
            response.setData("DUPLICATE");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    @PostMapping("/user")
    public ResponseEntity<ApiResponse<String>> registerUser(@RequestBody UserDto userDto){
        ApiResponse<String> response=new ApiResponse();
        boolean isRegistered = userService.register(userDto);
        if(isRegistered){
            response.setStatus(200);
            response.setMessage("User Registered Successfully");
            response.setData("SUCCESS");
            return new ResponseEntity<>(response, HttpStatus.OK);
        }else{
            response.setStatus(500);
            response.setMessage("User Registration Failed");
            response.setData("FAILED");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserDto>> login(@RequestBody UserDto userDto){
        ApiResponse<UserDto> response=new ApiResponse();
        UserDto user = userService.login(userDto.getEmail(), userDto.getPwd());
        if(user!=null){//user is found
            response.setStatus(200);
            response.setMessage("User Logged in Successfully");
            response.setData(user);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }else {
            response.setStatus(500);
            response.setMessage("User Login Failed");
            response.setData(null);
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    @PostMapping("/resetpwd")
    public ResponseEntity<ApiResponse<String>> resetPwd(@RequestBody ResetPwdDto resetPwdDto){
        ApiResponse<String> response = new ApiResponse<>();
        boolean isPwdUpdated = userService.resetPwd(resetPwdDto);
        if(isPwdUpdated){//pwd updated
            response.setStatus(200);
            response.setMessage("Password Updated Successfully");
            response.setData("SUCCESS");
            return new ResponseEntity<>(response, HttpStatus.OK);
        }else {
            response.setStatus(500);
            response.setMessage("Password Update Failed");
            response.setData("FAILED");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/quote")
    public ResponseEntity<ApiResponse<QuoteApiResponseDto>> getQuotation(){
      ApiResponse<QuoteApiResponseDto> response=new ApiResponse<>();
        QuoteApiResponseDto quote = userService.getQuote();
        if(quote!=null){//user is found
            response.setStatus(200);
            response.setMessage("Quote Fetched Successfully");
            response.setData(quote);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }else {
            response.setStatus(500);
            response.setMessage("Quote Fetch Failed");
            response.setData(null);
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


}
