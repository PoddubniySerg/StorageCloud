package ru.netology.storagecloud.services.tokens;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.netology.storagecloud.model.errors.ErrorMessage;
import ru.netology.storagecloud.model.requests.Login;
import ru.netology.storagecloud.model.token.AuthToken;
import ru.netology.storagecloud.repositories.database.entities.TokenEntity;
import ru.netology.storagecloud.repositories.tokens.TokenGenerator;
import ru.netology.storagecloud.repositories.tokens.TokenJpaRepository;

@Service
public class LoginLogoutService extends DaoAuthenticationProvider {

    protected final static String TOKEN_HEADER_NAME = "auth-token";

    protected final TokenJpaRepository tokenJpaRepository;
    protected final TokenEncoder tokenEncoder;
    protected final TokenDecoder tokenDecoder;

    public LoginLogoutService(
            TokenGenerator tokenGenerator,
            PasswordEncoder passwordEncoder,
            UserDetailsService userDetailsService,
            TokenJpaRepository tokenJpaRepository) {
        this.tokenJpaRepository = tokenJpaRepository;
        this.tokenEncoder = tokenGenerator;
        this.tokenDecoder = tokenGenerator;
        this.setPasswordEncoder(passwordEncoder);
        this.setUserDetailsService(userDetailsService);
    }

    public String checkLogin(Login login) {
        try {
            final var username = login.getLogin();
            final var authentication = new UsernamePasswordAuthenticationToken(username, login.getPassword());
            return generateToken(authentication).token();
        } catch (Exception e) {
            throw new BadCredentialsException(ErrorMessage.BAD_CREDENTIALS);
        }
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        final var tokenString = request.getHeader(TOKEN_HEADER_NAME).split(" ")[1].trim();
        final var token = tokenDecoder.readToken(tokenString);
        final var tokenEntity = tokenJpaRepository.findById(token.username()).orElse(null);
        logout(tokenEntity, response);
    }

    private AuthToken generateToken(UsernamePasswordAuthenticationToken authentication) {
        final var result = this.authenticate(authentication);
        final var token = tokenEncoder.generateToken(result.getName());
        final var tokenEntity = TokenEntity.builder()
                .username(token.username())
                .token(token.token())
                .start(token.start())
                .expiration(token.expiration())
                .isActive(true)
                .build();
        this.tokenJpaRepository.save(tokenEntity);
        return token;
    }

    private void logout(TokenEntity tokenEntity, HttpServletResponse response) {
        if (tokenEntity != null) {
            tokenEntity.setActive(false);
            tokenJpaRepository.save(tokenEntity);
        }
        response.addCookie(new Cookie("JSESSIONID", null));
        SecurityContextHolder.clearContext();
    }
}
