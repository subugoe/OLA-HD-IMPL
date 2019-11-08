package ola.hd.longtermstorage.config;

import ola.hd.longtermstorage.component.CustomAuthenticationEntryPoint;
import ola.hd.longtermstorage.component.TokenProvider;
import ola.hd.longtermstorage.filter.JwtFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.BeanIds;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private final TokenProvider tokenProvider;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @Autowired
    public SecurityConfig(TokenProvider tokenProvider,
                          CustomAuthenticationEntryPoint customAuthenticationEntryPoint) {
        this.tokenProvider = tokenProvider;
        this.customAuthenticationEntryPoint = customAuthenticationEntryPoint;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // @formatter:off
        http
                .csrf().disable()
                .cors().and()
                .sessionManagement()
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                    .and()
                .exceptionHandling()
                    .authenticationEntryPoint(this.customAuthenticationEntryPoint)
                    .and()
                .addFilterBefore(new JwtFilter(this.tokenProvider), BasicAuthenticationFilter.class)
                .authorizeRequests()
                    .antMatchers("/export",
                            "/download",
                            "/download-file/**",
                            "/login",
                            "/search*/**",
                            "/configuration/**", "/swagger*/**", "/webjars/**").permitAll()
                    .anyRequest().authenticated()
                    .and()
                .httpBasic();
        // @formatter:on
    }

    @Bean(BeanIds.AUTHENTICATION_MANAGER)
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }
}
