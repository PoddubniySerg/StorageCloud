package ru.netology.storagecloud.controllers;

import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.netology.storagecloud.model.requests.Login;
import ru.netology.storagecloud.model.responses.AuthTokenResponse;
import ru.netology.storagecloud.services.tokens.LoginLogoutService;

@RequiredArgsConstructor
@RestController
@RequestMapping
public class LoginLogoutController {

    private final LoginLogoutService loginLogoutService;

    @PermitAll
    @PostMapping("/login")
    public AuthTokenResponse login(@RequestBody Login login) {
        return new AuthTokenResponse(loginLogoutService.checkLogin(login));
    }

    @PostMapping("/logout")
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        try {
            loginLogoutService.logout(request, response);
        } catch (Exception e) {
//            TODO nothing
        }
    }
}
