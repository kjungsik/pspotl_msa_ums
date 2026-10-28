package org.example.pspotl.common.dto;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class NotificationRequestDto implements Serializable {
    private String notificationId;
    private String receiverUserId;
    private String messageType; // e.g., MARKETING, TRANSACTION
    private String title;
    private String content;
    private long timestamp;
}