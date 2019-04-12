package ola.hd.longtermstorage.config;

import ola.hd.longtermstorage.component.CustomAuthenticationEntryPoint;
import ola.hd.longtermstorage.component.MyLdapAuthenticationProvider;
import ola.hd.longtermstorage.component.TokenProvider;
import ola.hd.longtermstorage.filter.JwtFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.BeanIds;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private final UserDetailsService userDetailsService;
    private final TokenProvider tokenProvider;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final MyLdapAuthenticationProvider myLdapAuthenticationProvider;

    @Autowired
    public SecurityConfig(@Qualifier("ldapUserService") UserDetailsService userDetailsService,
                          TokenProvider tokenProvider,
                          CustomAuthenticationEntryPoint customAuthenticationEntryPoint,
                          MyLdapAuthenticationProvider myLdapAuthenticationProvider) {
        this.userDetailsService = userDetailsService;
        this.tokenProvider = tokenProvider;
        this.customAuthenticationEntryPoint = customAuthenticationEntryPoint;
        this.myLdapAuthenticationProvider = myLdapAuthenticationProvider;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // @formatter:off
        http
                .csrf().disable()
                .sessionManagement()
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                    .and()
                .exceptionHandling()
                    .authenticationEntryPoint(this.customAuthenticationEntryPoint)
                    .and()
                .addFilterBefore(new JwtFilter(this.tokenProvider), BasicAuthenticationFilter.class)
                .authorizeRequests()
                    .antMatchers("/bag").authenticated()
                    .anyRequest().permitAll()
                    .and()
                .httpBasic();
        // @formatter:on
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(myLdapAuthenticationProvider);
        auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
    }

    @Bean(BeanIds.AUTHENTICATION_MANAGER)
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
