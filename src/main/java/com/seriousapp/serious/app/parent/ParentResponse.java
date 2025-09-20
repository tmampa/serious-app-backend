package com.seriousapp.serious.app.parent;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ParentResponse {
    private Long id;
    private String name;
    private String email;
    private String relationship;
}
