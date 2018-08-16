package com.example.oauth.rest.config;

import com.example.oauth.rest.services.MongoTokenStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.InMemoryTokenStore;

@Configuration
@EnableAuthorizationServer
public class OAuthAuthorizationConfig extends AuthorizationServerConfigurerAdapter
{

    @Autowired
    private AuthenticationManager authenticationManager;

    @Bean
    public TokenStore mongoTokenStore() {
        return new MongoTokenStore();
    }

    public TokenStore inMemoryTokenStore() {
        return new InMemoryTokenStore();
    }

    /*@Bean
    public ClientDetailsService clientDetailsService() {
        return new MongoClientDetailsService();
    }

    @Bean
    protected UserDetailsService userDetailsService() {
        return new MongoUserDetailsServices();
    }

    @Bean
    @Primary
    public DefaultTokenServices tokenServices() {
        DefaultTokenServices defaultTokenServices = new DefaultTokenServices();
        defaultTokenServices.setTokenStore(mongoTokenStore());
        defaultTokenServices.setSupportRefreshToken(true);
        return defaultTokenServices;
    }*/

    @Override
    public void configure(AuthorizationServerEndpointsConfigurer configurer) throws Exception {
        configurer.tokenStore(inMemoryTokenStore())
                .authenticationManager(authenticationManager);

        /*configurer.tokenServices(tokenServices())
                .userDetailsService(userDetailsService())
                .authenticationManager(authenticationManager)
                .allowedTokenEndpointRequestMethods(HttpMethod.POST)
                .setClientDetailsService(clientDetailsService());*/
    }

    @Override
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
        clients.inMemory()
                .withClient("web-client")
                .secret("web-client-secret")
                .resourceIds("greeting")
                .authorizedGrantTypes("client_credentials", "password", "refresh_token")
                .authorities("ROLE_CLIENT")
                .accessTokenValiditySeconds(60)
                .refreshTokenValiditySeconds(14400)
                .scopes("read", "write", "trust");
    }

    @Override
    public void configure(final AuthorizationServerSecurityConfigurer oauthServer) {
        oauthServer.tokenKeyAccess("permitAll()")
                .checkTokenAccess("isAuthenticated()")
                .allowFormAuthenticationForClients();
    }
}
