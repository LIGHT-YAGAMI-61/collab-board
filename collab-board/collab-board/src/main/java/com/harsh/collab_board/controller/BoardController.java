package com.harsh.collab_board.controller;


import com.harsh.collab_board.entity.Board;
import com.harsh.collab_board.service.BoardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/boards")
public class BoardController {


    @Autowired
    private BoardService boardService ;

    @PostMapping
    public Board createBoard(@RequestBody Board board , org.springframework.security.core.Authentication authentication ) {

        return boardService.createBoard(board , authentication.getName()) ;
    }

    @GetMapping
    public List<Board> getAllBoards(org.springframework.security.core.Authentication authentication) {
        return boardService.getAllBoards(authentication.getName()) ;
    }

    @GetMapping("/{id}")
    public Board getBoardById(@PathVariable Long id  , org.springframework.security.core.Authentication authentication) {
        return boardService.getBoardById(id , authentication.getName()) ;
    }

    @PutMapping("/{id}")
    public Board updateBoard(@PathVariable Long id, @RequestBody Board board, org.springframework.security.core.Authentication authentication) {
                return boardService.updateBoard(id, board, authentication.getName());
            }

    @DeleteMapping("/{id}")
    public void deleteBoard(@PathVariable Long id , org.springframework.security.core.Authentication authentication ) {
        boardService.deleteBoard(id , authentication.getName()) ;
    }


}
