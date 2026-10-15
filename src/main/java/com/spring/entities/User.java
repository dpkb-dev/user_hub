package com.spring.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.sql.Timestamp;

@Setter
@Getter
@Entity
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer userId;
    private String userName;
    private String email;
    private Long phoneNumber;
    private String password;
    private String passwordUpdated;

    @ManyToOne
    @JoinColumn(name = "country_id")
    private Country country;

    @ManyToOne
    @JoinColumn(name = "state_id")
    private State state;

    @ManyToOne
    @JoinColumn(name = "city_id")
    private City city;

    private String passwordReset;

    @CreationTimestamp
    @Column(updatable = false)          // set once at insert, never changed afterwards
    private Timestamp createdAt;

    @UpdateTimestamp
    private Timestamp lastUpdatedAt;    // refreshed on every update
}
