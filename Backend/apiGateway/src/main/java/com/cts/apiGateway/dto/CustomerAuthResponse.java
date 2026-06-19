package com.cts.apiGateway.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerAuthResponse {
    private String token;
    private String email;
    private String username;
    private String name;
    private String bankName;
    private String accountNo;
    private String accountType;
    private Double balance;
    private Double riskScore;
    private String role;
    private String message;
}
