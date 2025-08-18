package com.example.WebSocketSSE.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)                 // 모르는 필드는 무시
@JsonInclude(JsonInclude.Include.NON_NULL)                  // null 필드는 응답에서 생략
public class ChatMessageDto {

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)   // 클라가 보내도 무시
    private Long id;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)   // 서버에서만 세팅
    private Long roomId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)   // 서버에서만 세팅
    private Long senderId;

    @NotBlank(message = "content is required")              // 컨트롤러에서 @Valid 쓸 때만 동작
    private String content;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)   // 서버에서만 세팅
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime createdAt;
}
