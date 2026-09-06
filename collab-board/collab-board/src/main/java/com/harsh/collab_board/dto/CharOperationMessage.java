package com.harsh.collab_board.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CharOperationMessage {

    private String type; // CHAR_INSERTED, CHAR_DELETED
    private Long cardId;
    private String charId;
    private String value;    // present for CHAR_INSERTED
    private String afterId;  // present for CHAR_INSERTED
    private String siteId;   // present for CHAR_INSERTED
    private Long seq;        // present for CHAR_INSERTED
}