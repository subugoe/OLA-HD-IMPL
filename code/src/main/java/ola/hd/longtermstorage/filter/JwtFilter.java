package ola.hd.longtermstorage.filter;

import io.jsonwebtoken.ExpiredJwtException;
import ola.hd.longtermstorage.component.TokenProvider;
import ola.hd.longtermstorage.utils.SecurityConstants;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class JwtFilter extends GenericFilterBean {

    private final TokenProvider tokenProvider;

    public JwtFilter(TokenProvider tokenProvider) {

        this.tokenProvider = tokenProvider;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {

        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;

        try {
            String jwt = this.resolveToken(httpServletRequest);

            // If the token exists, validate it
            if (StringUtils.hasText(jwt)) {
                if (this.tokenProvider.validateToken(jwt)) {
                    Authentication authentication = this.tokenProvider.getAuthentication(jwt);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }

            // Otherwise, let the request go to the next filter (username, password authentication)
            filterChain.doFilter(servletRequest, servletResponse);

            // Remove the authentication after the request to remain stateless
            SecurityContextHolder.clearContext();

        } catch (ExpiredJwtException ex) {

            // TODO: for debug only
            ex.printStackTrace();

            // TODO: throw exception here. Handled by an entry point later
            throw new BadCredentialsException(ex.getMessage(), ex);
            //((HttpServletResponse) servletResponse).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    private String resolveToken(HttpServletRequest request) {

        String bearerToken = request.getHeader(SecurityConstants.AUTHORIZATION_HEADER);

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(SecurityConstants.TOKEN_PREFIX)) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
