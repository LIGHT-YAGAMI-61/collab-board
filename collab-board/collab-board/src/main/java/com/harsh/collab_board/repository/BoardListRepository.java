package com.harsh.collab_board.repository;

import com.harsh.collab_board.entity.Board;
import com.harsh.collab_board.entity.BoardList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BoardListRepository extends JpaRepository<BoardList , Long> {

    List<BoardList> findByBoardIdOrderByPositionAsc(Long boardId) ;

    Long board(Board board);
}
