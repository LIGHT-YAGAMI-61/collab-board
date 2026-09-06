package com.harsh.collab_board.repository;

import com.harsh.collab_board.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CardRepository extends JpaRepository<Card , Long> {

        List<Card> findByBoardListIdOrderByPositionAsc(Long boardListId ) ;
}
