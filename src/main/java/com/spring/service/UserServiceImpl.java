package com.spring.service;

import com.spring.dto.*;
import com.spring.entities.City;
import com.spring.entities.Country;
import com.spring.entities.State;
import com.spring.entities.User;
import com.spring.repo.CityRepository;
import com.spring.repo.CountryRepository;
import com.spring.repo.StateRepository;
import com.spring.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Random;


@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    //private final MapperBuilder mapperBuilder;
    private final UserRepository userRepository;
    
    private final  CountryRepository countryRepository;

    private final StateRepository stateRepository;

    private final  CityRepository cityRepository;

    private final  EmailService emailService;

    private final ModelMapper mapper;

    /*public UserServiceImpl(MapperBuilder mapperBuilder) {
        this.mapperBuilder = mapperBuilder;
    }*/

    @Override
    public List<CountryDto> getCountries() {
        List<Country> countries = countryRepository.findAll();
        return countries.stream()
                .map(country-> mapper.map(country, CountryDto.class))
                .toList();
    }

    @Override
    public List<StateDto> getStates(Integer countryId) {
        List<State> states = stateRepository.findByCountryCountryId(countryId);
        return states.stream()
                .map(state-> mapper.map(state, StateDto.class))
                .toList();
    }

    @Override
    public List<CityDto> getCities(Integer stateId) {

        List<City> cities = cityRepository.findByStateStateId(stateId);

        return cities.stream()
                .map(city -> mapper.map(city, CityDto.class))
                .toList();
    }

    @Override
    public boolean isEmailUnique(String email) {
        return !userRepository.existsByEmail(email);
    }

    @Override
    public boolean register(UserDto userDto)
    {
        if (userRepository.existsByEmail(userDto.getEmail())) {
            return false;                                            // email already registered
        }
        User user = mapper.map(userDto, User.class);
        user.setPhoneNumber(userDto.getPhno());

        user.setPassword(generateRandomPassword(5));
        user.setPasswordUpdated("NO");

        Country country= countryRepository.findById(userDto.getCountryId()).orElseThrow();
        State state= stateRepository.findById(userDto.getStateId()).orElseThrow();
        City city= cityRepository.findById(userDto.getCityId()).orElseThrow();
        user.setCountry(country);
        user.setState(state);
        user.setCity(city);

        User savedUser= userRepository.save(user);

        if(savedUser != null){
            String subject= "Deeps IT - Your Account Created Successfully";
            String body = "<h2> Your Temporary Pwd :"+ user.getPassword() +"</h2>";
            return emailService.sendEmail(subject, body, user.getEmail());
        }
        return false;
    }

    @Override
    public UserDto login(String email, String pwd) {
        User user=userRepository.findByEmailAndPassword(email, pwd);
        if(user != null){
            UserDto dto = mapper.map(user, UserDto.class);
            dto.setPwdUpdated(user.getPasswordUpdated());   // "NO" after register, "YES" after reset
            dto.setPhno(user.getPhoneNumber());
            return dto;
        }

        return null;
    }

    @Override
    public boolean resetPwd(ResetPwdDto resetPwdDto) {
        User user = userRepository.findByEmail(resetPwdDto.getEmail());
        if (user == null) {
            return false;                                            // email not registered
        }
        if (!user.getPassword().equals(resetPwdDto.getOldPwd())) {
            return false;                                            // old password wrong
        }
        if (resetPwdDto.getNewPwd() == null
                || !resetPwdDto.getNewPwd().equals(resetPwdDto.getConfirmPwd())) {
            return false;                                            // new & confirm don't match
        }
        user.setPassword(resetPwdDto.getNewPwd());
        user.setPasswordUpdated("YES");
        userRepository.save(user);                                   // UPDATE (user already has an id)
        return true;
    }

    @Override
    public QuoteApiResponseDto getQuote() {
        String apiUrl="https://dummyjson.com/quotes/random";
        RestTemplate rt= new RestTemplate();
        return rt.getForEntity(apiUrl, QuoteApiResponseDto.class).getBody();
    }

    private String generateRandomPassword(int pwdLength) {
        Random random = new Random();
        String chars= "ABCDEFGHIJKLMNOPQRSTUVWXYZ123456789";
        StringBuilder buffer=new StringBuilder(pwdLength);
        for(int i=0; i<pwdLength; i++){
            int randomIndex=random.nextInt(chars.length());
            char ch = chars.charAt(randomIndex);
            buffer.append(ch);
        }
        return buffer.toString();
    }


}

