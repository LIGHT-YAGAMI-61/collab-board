package com.harsh.collab_board.controller;



import com.harsh.collab_board.entity.BoardList;
import com.harsh.collab_board.service.BoardListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/boards/{boardId}/lists")
public class BoardListController {

        @Autowired
    private BoardListService boardListService ;

    @PostMapping
    public BoardList createList(@PathVariable Long boardId , @RequestBody BoardList boardList , org.springframework.security.core.Authentication authentication ) {
        return boardListService.createList(boardId , boardList , authentication.getName()) ;
    }

    @GetMapping
    public List<BoardList> getListsByBoard(@PathVariable Long boardId , org.springframework.security.core.Authentication authentication ) {
        return boardListService.getListsByBoard(boardId , authentication.getName()) ;
    }

    @PutMapping("/{id}")
    public BoardList updateList(@PathVariable Long boardId , @PathVariable Long id ,   @RequestBody BoardList boardList , org.springframework.security.core.Authentication authentication ) {
        return boardListService.updateList(id , boardList , authentication.getName()) ;
    }

    @DeleteMapping("/{id}")
    public void deleteList(@PathVariable Long boardId , @PathVariable Long id , org.springframework.security.core.Authentication authentication ) {
        boardListService.deleteList(id , authentication.getName());
    }

}
