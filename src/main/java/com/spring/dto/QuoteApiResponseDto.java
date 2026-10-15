package com.spring.dto;

import lombok.Data;

@Data
public class QuoteApiResponseDto {

    private Integer id;
    private String quote;
    private String author;
}
