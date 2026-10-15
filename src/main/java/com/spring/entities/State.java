package com.spring.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
public class State {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer stateId;
    private String stateName;

    // State is on the MANY side → FK (country_id) lives here
    @ManyToOne
    @JoinColumn(name = "country_id")
    private  Country country;
}

