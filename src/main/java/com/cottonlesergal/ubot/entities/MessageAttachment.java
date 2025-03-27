package com.cottonlesergal.ubot.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing a Discord message attachment
 */
@Entity
@Table(name = "message_attachments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageAttachment {

    @Id
    private String id;  // Discord attachment ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String url;

    private Long size;

    private Integer width;

    private Integer height;
}