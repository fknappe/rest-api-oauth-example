package com.example.oauth.rest.services;

import com.example.oauth.rest.domain.document.OAuthClientDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.ClientRegistrationException;

public class MongoClientDetailsService implements ClientDetailsService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public ClientDetails loadClientByClientId(String clientId) throws ClientRegistrationException {
        Query query = new Query();
        query.addCriteria(Criteria.where(OAuthClientDetails.CLIENT_ID).is(clientId));
        OAuthClientDetails clientDetails = mongoTemplate.findOne(query, OAuthClientDetails.class);
        if (clientDetails == null) {
            throw new ClientRegistrationException(String.format("Client with id %s not found", clientId));
        }
        return clientDetails;
    }
}
