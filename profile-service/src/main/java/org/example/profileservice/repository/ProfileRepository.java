package org.example.profileservice.repository;


import org.example.profileservice.model.Profile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProfileRepository extends MongoRepository<Profile, String> {

    Profile findByEmail(String email);

}
