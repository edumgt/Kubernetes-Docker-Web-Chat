package org.example.profileservice.model;


import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "profiles")
public class Profile {

    @Id
    private String id;
    private String email;
    private String displayName;
    private String profilePicUrl;



}
