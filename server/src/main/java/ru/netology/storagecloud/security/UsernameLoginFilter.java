package ru.netology.storagecloud.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import ru.netology.storagecloud.exceptions.UnauthorizedException;
import ru.netology.storagecloud.model.errors.ErrorMessage;
import ru.netology.storagecloud.model.token.AuthToken;
import ru.netology.storagecloud.services.tokens.TokenDecoder;

import java.io.IOException;

@RequiredArgsConstructor
public class UsernameLoginFilter extends UsernamePasswordAuthenticationFilter {

    private static final String TOKEN_HEADER_NAME = "auth-token";
    private static final String TOKEN_START_WITH = "Bearer ";

    private final TokenDecoder decoder;
    private AuthToken token;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        super.doFilter(request, response, chain);
        chain.doFilter(request, response);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        try {
            return super.attemptAuthentication(request, response);
        } catch (Exception e) {
            return null;
        }
    }

    @SneakyThrows
    @Override
    protected String obtainUsername(HttpServletRequest request) {
        try {
            final var tokenHeader = request.getHeader(TOKEN_HEADER_NAME);

            if (!tokenHeader.startsWith(TOKEN_START_WITH))
                throw new UnauthorizedException(ErrorMessage.UNAUTHORIZED_ERROR);

            final var token = tokenHeader.split(" ")[1].trim();
            this.token = decoder.readToken(token);
            if (this.token == null) this.token = AuthToken.builder().token("").username("").build();
            return this.token.username();

        } catch (Exception e) {
            throw new UnauthorizedException(ErrorMessage.UNAUTHORIZED_ERROR);
        }
    }

    @Override
    protected String obtainPassword(HttpServletRequest request) {
        return this.token.token();
    }
}
