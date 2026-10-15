package com.spring.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
public class City {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer cityId;
    private String cityName;

    // City is on the MANY side → FK (state_id) lives here
    @ManyToOne
    @JoinColumn(name = "state_id")
    private State state;
}
