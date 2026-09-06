package com.harsh.collab_board.repository;

import com.harsh.collab_board.entity.CardCharacter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CardCharacterRepository extends JpaRepository<CardCharacter , Long> {

    List<CardCharacter> findByCardId(Long cardId) ;

    Optional<CardCharacter> findByCardIdAndCharId(Long cardId , String charId ) ;

    void deleteByCardId(Long cardId) ;
}
