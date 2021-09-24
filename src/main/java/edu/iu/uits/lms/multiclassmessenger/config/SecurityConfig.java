package edu.iu.uits.lms.multiclassmessenger.config;

import edu.iu.uits.lms.lti.security.LtiAuthenticationProvider;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
public class SecurityConfig {

    @Configuration
    @Order(SecurityProperties.BASIC_AUTH_ORDER - 2)
    public static class MultiClassMessengerWebSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http.authenticationProvider(new LtiAuthenticationProvider());
            http
                  .requestMatchers().antMatchers("/lti", "/**")
                  .and()
                  .authorizeRequests()
                  .antMatchers("/lti").permitAll()
                  .antMatchers("/**").hasRole(LtiAuthenticationProvider.LTI_USER);

            //Need to disable csrf so that we can use POST via REST
            http.csrf().disable();

            //Need to disable the frame options so we can embed this in another tool
            http.headers().frameOptions().disable();

            http.exceptionHandling().accessDeniedPage("/accessDenied");
        }

        @Override
        public void configure(WebSecurity web) throws Exception {
            // ignore everything except paths specified
            web.ignoring().antMatchers("/jsrivet/**", "/webjars/**", "/actuator/**", "/css/**", "/js/**");
        }

    }

//    @Configuration
//    @Order(SecurityProperties.BASIC_AUTH_ORDER - 1)
//    public static class MultiClassMessengerCatchAllSecurityConfig extends WebSecurityConfigurerAdapter {
//
//        @Override
//        public void configure(HttpSecurity http) throws Exception {
//            http.requestMatchers().antMatchers("/**")
//                  .and()
//                  .authorizeRequests()
//                  .anyRequest().authenticated();
//        }
//    }
}
