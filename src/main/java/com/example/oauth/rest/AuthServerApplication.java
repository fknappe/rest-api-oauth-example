package com.example.oauth.rest;

import com.example.oauth.rest.domain.document.OAuthRefreshToken;
import com.google.common.collect.Sets;
import com.example.oauth.rest.domain.document.OAuthAccessToken;
import com.example.oauth.rest.domain.document.OAuthClientDetails;
import com.example.oauth.rest.domain.document.OAuthUser;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.core.authority.AuthorityUtils;

@SpringBootApplication
public class AuthServerApplication {

    public static void main(String[] args) {
        final ConfigurableApplicationContext context = SpringApplication.run(AuthServerApplication.class, args);

        //if (args.length > 0 && "init".equalsIgnoreCase(args[0])) {

            MongoTemplate mongoTemplate = context.getBean(MongoTemplate.class);

            mongoTemplate.dropCollection(OAuthUser.class);
            mongoTemplate.dropCollection(OAuthClientDetails.class);
            mongoTemplate.dropCollection(OAuthAccessToken.class);
            mongoTemplate.dropCollection(OAuthRefreshToken.class);

            // init the users
            OAuthUser mongoUser = new OAuthUser();
            mongoUser.setUsername("user");
            mongoUser.setPassword("user");
            mongoUser.setRoles(Sets.newHashSet(("ROLE_USER")));
            mongoTemplate.save(mongoUser);

            // init the client details
            OAuthClientDetails clientDetails = new OAuthClientDetails();
            clientDetails.setClientId("web-client");
            clientDetails.setClientSecret("web-client-secret");
            clientDetails.setSecretRequired(true);
            clientDetails.setResourceIds(Sets.newHashSet("greeting"));
            clientDetails.setScope(Sets.newHashSet("read"));
            clientDetails.setAuthorizedGrantTypes(Sets.newHashSet("refresh_token",
                    "password", "client_credentials"));
            clientDetails.setRegisteredRedirectUri(Sets.newHashSet("http://localhost:8082/rest"));
            clientDetails.setAuthorities(AuthorityUtils.createAuthorityList("ROLE_USER"));
            clientDetails.setAccessTokenValiditySeconds(60);
            clientDetails.setRefreshTokenValiditySeconds(14400);
            clientDetails.setAutoApprove(true);
            mongoTemplate.save(clientDetails);
        //}
    }
}
