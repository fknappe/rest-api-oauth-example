package com.example.oauth.rest.domain.document;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Set;

@Data @NoArgsConstructor
@Document(collection = "user")
public class OAuthUser {

    @Id
    private String id;

    private String username;

    private String password;

    private Set<String> roles;
}
