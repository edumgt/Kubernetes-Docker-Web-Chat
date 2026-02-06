package org.example.chatservice.model;


import lombok.Data;
import lombok.ToString;
import org.springframework.data.annotation.Id;

@ToString
@Data
public class Profile {

    @Id
    private String id;
    private String email;
    private String displayName;
    private String profilePicUrl;



}
