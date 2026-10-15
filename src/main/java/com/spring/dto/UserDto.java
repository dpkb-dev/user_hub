package com.spring.dto;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;   // see note below

@Data
public class UserDto {

    private Integer userId;
    private String name;
    private String email;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String pwd;
    private String pwdUpdated;
    private Long phno;
    private Integer countryId;
    private Integer stateId;
    private Integer cityId;
}
