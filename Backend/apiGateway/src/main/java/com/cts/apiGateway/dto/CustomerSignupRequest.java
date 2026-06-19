package com.cts.apiGateway.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerSignupRequest {
    private String email;
    private String password;
    private String name;
    private String bankName;
    private String accountNo;
    private String accountType;
    private Double balance;
}
