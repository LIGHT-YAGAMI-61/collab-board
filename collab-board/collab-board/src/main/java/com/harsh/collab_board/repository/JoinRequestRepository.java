package com.harsh.collab_board.repository;

import com.harsh.collab_board.entity.JoinRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JoinRequestRepository extends JpaRepository<JoinRequest, Long> {

    List<JoinRequest> findByBoardIdAndStatus(Long boardId, String status);

    Optional<JoinRequest> findByBoardIdAndUserIdAndStatus(Long boardId, Long userId, String status);
}