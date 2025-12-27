package tn.ahmed.emailservice;

import lombok.*;

/**
 * Email Request Model
 *
 * Represents an email to be sent via Dapr Output Binding
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EmailRequest {

    /**
     * Recipient email address
     */
    private String to;

    /**
     * Email subject
     */
    private String subject;

    /**
     * Email body (text or HTML)
     */
    private String body;

    /**
     * Sender email (optional, can be set in binding config)
     */
    private String from;
}