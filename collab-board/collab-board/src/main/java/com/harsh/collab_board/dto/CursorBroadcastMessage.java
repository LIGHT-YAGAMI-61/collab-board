package com.harsh.collab_board.dto;

// What the server broadcasts out to /topic/board/{boardId}/cursors.
// Same shape as the request, plus "type" and "username" which the
// server fills in itself (never trust the client to say who it is).
public class CursorBroadcastMessage {
    private String type; // "CURSOR_UPDATE" | "CURSOR_CLEARED"
    private String username;
    private String fieldType;
    private Long entityId;
    private Integer charIndex;
    private String anchorCharId;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFieldType() { return fieldType; }
    public void setFieldType(String fieldType) { this.fieldType = fieldType; }

    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }

    public Integer getCharIndex() { return charIndex; }
    public void setCharIndex(Integer charIndex) { this.charIndex = charIndex; }

    public String getAnchorCharId() { return anchorCharId; }
    public void setAnchorCharId(String anchorCharId) { this.anchorCharId = anchorCharId; }
}