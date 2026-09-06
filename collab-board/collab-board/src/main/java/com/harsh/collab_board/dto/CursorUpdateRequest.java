package com.harsh.collab_board.dto;

// What the browser sends to /app/cursor (and /app/cursor/clear) when a
// user's cursor moves in a title field or the description editor.
public class CursorUpdateRequest {
    private Long boardId;
    private String fieldType;   // "BOARD_TITLE" | "LIST_TITLE" | "CARD_TITLE" | "CARD_DESCRIPTION"
    private Long entityId;      // boardId, listId, or cardId depending on fieldType
    private Integer charIndex;  // used for title fields; null for description
    private String anchorCharId; // used for description; null for title fields

    public Long getBoardId() { return boardId; }
    public void setBoardId(Long boardId) { this.boardId = boardId; }

    public String getFieldType() { return fieldType; }
    public void setFieldType(String fieldType) { this.fieldType = fieldType; }

    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }

    public Integer getCharIndex() { return charIndex; }
    public void setCharIndex(Integer charIndex) { this.charIndex = charIndex; }

    public String getAnchorCharId() { return anchorCharId; }
    public void setAnchorCharId(String anchorCharId) { this.anchorCharId = anchorCharId; }
}