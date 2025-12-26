package tn.ahmed.taskservice;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter 
@Setter 
@AllArgsConstructor 
@NoArgsConstructor 
@Builder
public class Task {
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("title")
    private String title;
    
    @JsonProperty("description")
    private String description;
}
