package com.example.oauth.rest.services;

import com.example.oauth.rest.domain.document.OAuthAccessToken;
import com.example.oauth.rest.domain.document.OAuthRefreshToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.AuthenticationKeyGenerator;
import org.springframework.security.oauth2.provider.token.DefaultAuthenticationKeyGenerator;
import org.springframework.security.oauth2.provider.token.TokenStore;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MongoTokenStore implements TokenStore
{

    private AuthenticationKeyGenerator authenticationKeyGenerator = new DefaultAuthenticationKeyGenerator();

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public OAuth2Authentication readAuthentication(OAuth2AccessToken accessToken) {
        return readAuthentication(accessToken.getValue());
    }

    @Override
    public OAuth2Authentication readAuthentication(String token) {
        Query query = new Query();
        query.addCriteria(Criteria.where(OAuthAccessToken.TOKEN_ID).is(extractTokenKey(token)));

        OAuthAccessToken accessToken = mongoTemplate.findOne(query, OAuthAccessToken.class);
        return accessToken != null ? accessToken.getAuthentication() : null;
    }

    @Override
    public void storeAccessToken(OAuth2AccessToken accessToken, OAuth2Authentication authentication) {
        String refreshToken = null;
        if (accessToken.getRefreshToken() != null) {
            refreshToken = accessToken.getRefreshToken().getValue();
        }

        if (readAccessToken(accessToken.getValue()) != null) {
            this.removeAccessToken(accessToken);
        }

        OAuthAccessToken mongoAccessToken = new OAuthAccessToken();
        mongoAccessToken.setTokenId(extractTokenKey(accessToken.getValue()));
        mongoAccessToken.setToken(accessToken);
        mongoAccessToken.setAuthenticationId(authenticationKeyGenerator.extractKey(authentication));
        mongoAccessToken.setUsername(authentication.isClientOnly() ? null : authentication.getName());
        mongoAccessToken.setClientId(authentication.getOAuth2Request().getClientId());
        mongoAccessToken.setAuthentication(authentication);
        mongoAccessToken.setRefreshToken(extractTokenKey(refreshToken));

        mongoTemplate.save(mongoAccessToken);
    }

    @Override
    public OAuth2AccessToken readAccessToken(String tokenValue) {
        Query query = new Query();
        query.addCriteria(Criteria.where(OAuthAccessToken.TOKEN_ID).is(extractTokenKey(tokenValue)));

        OAuthAccessToken mongoAccessToken = mongoTemplate.findOne(query, OAuthAccessToken.class);
        return mongoAccessToken != null ? mongoAccessToken.getToken() : null;
    }

    @Override
    public void removeAccessToken(OAuth2AccessToken oAuth2AccessToken) {
        Query query = new Query();
        query.addCriteria(Criteria.where(OAuthAccessToken.TOKEN_ID).is(extractTokenKey(oAuth2AccessToken.getValue())));
        mongoTemplate.remove(query, OAuthAccessToken.class);
    }

    @Override
    public void storeRefreshToken(OAuth2RefreshToken refreshToken, OAuth2Authentication authentication) {
        OAuthRefreshToken token = new OAuthRefreshToken();
        token.setTokenId(extractTokenKey(refreshToken.getValue()));
        token.setToken(refreshToken);
        token.setAuthentication(authentication);
        mongoTemplate.save(token);
    }

    @Override
    public OAuth2RefreshToken readRefreshToken(String tokenValue) {
        Query query = new Query();
        query.addCriteria(Criteria.where(OAuthRefreshToken.TOKEN_ID).is(extractTokenKey(tokenValue)));

        OAuthRefreshToken mongoRefreshToken = mongoTemplate.findOne(query, OAuthRefreshToken.class);
        return mongoRefreshToken != null ? mongoRefreshToken.getToken() : null;
    }

    @Override
    public OAuth2Authentication readAuthenticationForRefreshToken(OAuth2RefreshToken refreshToken) {
        Query query = new Query();
        query.addCriteria(Criteria.where(OAuthRefreshToken.TOKEN_ID).is(extractTokenKey(refreshToken.getValue())));

        OAuthRefreshToken mongoRefreshToken = mongoTemplate.findOne(query, OAuthRefreshToken.class);
        return mongoRefreshToken != null ? mongoRefreshToken.getAuthentication() : null;
    }

    @Override
    public void removeRefreshToken(OAuth2RefreshToken refreshToken) {
        Query query = new Query();
        query.addCriteria(Criteria.where(OAuthRefreshToken.TOKEN_ID).is(extractTokenKey(refreshToken.getValue())));
        mongoTemplate.remove(query, OAuthRefreshToken.class);
    }

    @Override
    public void removeAccessTokenUsingRefreshToken(OAuth2RefreshToken refreshToken) {
        Query query = new Query();
        query.addCriteria(Criteria.where(OAuthAccessToken.REFRESH_TOKEN).is(extractTokenKey(refreshToken.getValue())));
        mongoTemplate.remove(query, OAuthAccessToken.class);
    }

    @Override
    public OAuth2AccessToken getAccessToken(OAuth2Authentication authentication) {
        OAuth2AccessToken accessToken = null;
        String authenticationId = authenticationKeyGenerator.extractKey(authentication);

        Query query = new Query();
        query.addCriteria(Criteria.where(OAuthAccessToken.AUTHENTICATION_ID).is(authenticationId));

        OAuthAccessToken mongoAccessToken = mongoTemplate.findOne(query, OAuthAccessToken.class);
        if (mongoAccessToken != null) {
            accessToken = mongoAccessToken.getToken();
            if(accessToken != null && !authenticationId.equals(this.authenticationKeyGenerator.extractKey(this.readAuthentication(accessToken)))) {
                this.removeAccessToken(accessToken);
                this.storeAccessToken(accessToken, authentication);
            }
        }
        return accessToken;
    }

    @Override
    public Collection<OAuth2AccessToken> findTokensByClientIdAndUserName(String clientId, String username) {
        Collection<OAuth2AccessToken> tokens = new ArrayList<OAuth2AccessToken>();
        Query query = new Query();
        query.addCriteria(Criteria.where(OAuthAccessToken.CLIENT_ID).is(clientId));
        query.addCriteria(Criteria.where(OAuthAccessToken.USERNAME).is(username));
        List<OAuthAccessToken> accessTokens = mongoTemplate.find(query, OAuthAccessToken.class);
        for (OAuthAccessToken accessToken : accessTokens) {
            tokens.add(accessToken.getToken());
        }
        return tokens;
    }

    @Override
    public Collection<OAuth2AccessToken> findTokensByClientId(String clientId) {
        Collection<OAuth2AccessToken> tokens = new ArrayList<>();
        Query query = new Query();
        query.addCriteria(Criteria.where(OAuthAccessToken.CLIENT_ID).is(clientId));
        List<OAuthAccessToken> accessTokens = mongoTemplate.find(query, OAuthAccessToken.class);
        for (OAuthAccessToken accessToken : accessTokens) {
            tokens.add(accessToken.getToken());
        }
        return tokens;
    }

    private String extractTokenKey(String value) {
        if(value == null) {
            return null;
        } else {
            MessageDigest digest;
            try {
                digest = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException var5) {
                throw new IllegalStateException("MD5 algorithm not available.  Fatal (should be in the JDK).");
            }

            try {
                byte[] e = digest.digest(value.getBytes("UTF-8"));
                return String.format("%032x", new Object[]{new BigInteger(1, e)});
            } catch (UnsupportedEncodingException var4) {
                throw new IllegalStateException("UTF-8 encoding not available.  Fatal (should be in the JDK).");
            }
        }
    }
}
