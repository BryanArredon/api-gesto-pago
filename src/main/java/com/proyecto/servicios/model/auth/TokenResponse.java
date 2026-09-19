package com.proyecto.servicios.model.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
    private String nombre;
    private List<String> roles;

    public static TokenResponse ok(String accessToken, String refreshToken, Long expiresIn,
                                   String nombre, List<String> roles) {
        return new TokenResponse(accessToken, refreshToken, "Bearer", expiresIn, nombre, roles);
    }
}