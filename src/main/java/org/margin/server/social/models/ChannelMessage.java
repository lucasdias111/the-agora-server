package org.margin.server.social.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.margin.server.users.models.User;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "channel_messages")
public class ChannelMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull
    private User fromUserId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    private Channel channelId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id", nullable = false)
    private Space space;

    @Column(name = "from_user_server")
    private String fromUserServer;

    @NotBlank
    @Size(min = 1, max = 5000)
    private String message;

    private Boolean isEdited = false;
    private Date createdAt;
    private String federatedMessageId;

    public ChannelMessage(User fromUserId,
                          Channel channelId,
                          Space space,
                          String message,
                          String federatedMessageId) {
        this.fromUserId = fromUserId;
        this.channelId = channelId;
        this.space = space;
        this.message = message;
        this.federatedMessageId = federatedMessageId;
        this.createdAt = new Date();
    }
}
