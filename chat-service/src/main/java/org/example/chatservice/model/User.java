package org.example.chatservice.model;


import lombok.Data;
import org.springframework.data.annotation.Id;

@Data
public class User {

    @Id
    private String id;
    private String email;
    private String name;

}
