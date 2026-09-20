package com.proyecto.servicios.service;

import com.proyecto.servicios.model.auth.LoginRequest;
import com.proyecto.servicios.model.auth.RefreshRequest;
import com.proyecto.servicios.model.auth.RegisterRequest;
import com.proyecto.servicios.model.auth.TokenResponse;

public interface AuthService {

    TokenResponse login(LoginRequest request);

    TokenResponse register(RegisterRequest request);

    TokenResponse refresh(RefreshRequest request);

    void logout(String refreshToken);
}