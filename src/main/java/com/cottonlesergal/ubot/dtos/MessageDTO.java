package com.cottonlesergal.ubot.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Data Transfer Object for Discord messages
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageDTO {
    private String id;
    private String content;
    private String channelId;
    private String authorId;
    private String authorName;
    private String authorAvatarUrl;
    private Long timestamp;
    private Boolean edited;
    private Long editedTimestamp;
    private List<AttachmentDTO> attachments;
    private List<EmbedDTO> embeds;
    private Map<String, ReactionDTO> reactions;
    private String referencedMessageId;

    /**
     * Nested class for message attachments
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttachmentDTO {
        private String id;
        private String fileName;
        private String url;
        private Long size;
        private Integer width;
        private Integer height;
    }

    /**
     * Nested class for message embeds
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmbedDTO {
        private String title;
        private String description;
        private String url;
        private Long timestamp;
        private Integer color;
        private EmbedAuthorDTO author;
        private List<EmbedFieldDTO> fields;
        private EmbedMediaDTO image;
        private EmbedMediaDTO thumbnail;
        private EmbedFooterDTO footer;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmbedAuthorDTO {
        private String name;
        private String url;
        private String iconUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmbedFieldDTO {
        private String name;
        private String value;
        private Boolean inline;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmbedMediaDTO {
        private String url;
        private Integer width;
        private Integer height;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmbedFooterDTO {
        private String text;
        private String iconUrl;
    }

    /**
     * Nested class for message reactions
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReactionDTO {
        private String emoji;
        private Integer count;
        private Boolean selfReacted;
    }
}