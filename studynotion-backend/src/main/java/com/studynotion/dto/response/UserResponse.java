package com.studynotion.dto.response;

import com.studynotion.entity.User;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String accountType;
    private String image;
    private String gender;
    private String dateOfBirth;
    private String about;
    private String contactNumber;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .accountType(user.getAccountType().name())
                .image(user.getImage())
                .gender(user.getGender())
                .dateOfBirth(user.getDateOfBirth())
                .about(user.getAbout())
                .contactNumber(user.getContactNumber())
                .build();
    }
}
