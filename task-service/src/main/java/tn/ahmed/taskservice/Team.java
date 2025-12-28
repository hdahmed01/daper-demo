package tn.ahmed.taskservice;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Team {
    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("members")
    private List<TeamMember> members;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    @JsonProperty("active")
    @Builder.Default
    private boolean active = true;
}

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
class TeamMember {
    @JsonProperty("userId")
    private String userId;

    @JsonProperty("email")
    private String email;

    @JsonProperty("name")
    private String name;

    @JsonProperty("role")
    private TeamRole role;

    @JsonProperty("joinedAt")
    private LocalDateTime joinedAt;
}

enum TeamRole {
    MEMBER,
    LEAD,
    ADMIN
}