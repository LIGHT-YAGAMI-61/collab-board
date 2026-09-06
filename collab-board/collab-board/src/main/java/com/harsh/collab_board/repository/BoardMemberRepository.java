package com.harsh.collab_board.repository;


import com.harsh.collab_board.entity.BoardMember;
import com.harsh.collab_board.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BoardMemberRepository extends JpaRepository<BoardMember , Long> {

    List<BoardMember> findByBoardId(Long boardId) ;

    List<BoardMember> findByUserId(Long userId) ;

    Optional<BoardMember> findByBoardIdAndUserId(Long boardId , Long userId) ;

    boolean existsByBoardIdAndUserId(Long boardID , Long userId ) ;

//    Long user(User user);
}
