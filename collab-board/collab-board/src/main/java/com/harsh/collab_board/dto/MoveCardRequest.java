package com.harsh.collab_board.dto;


import lombok.Data;

@Data
public class MoveCardRequest {

        private Long targetListId ;
        private Integer targetPosition ;
}
