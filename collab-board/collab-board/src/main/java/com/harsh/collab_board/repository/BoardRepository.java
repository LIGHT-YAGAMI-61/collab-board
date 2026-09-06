package com.harsh.collab_board.repository;

import com.harsh.collab_board.entity.Board;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardRepository extends JpaRepository<Board , Long> {

}
