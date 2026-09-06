package com.harsh.collab_board.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Entity
@Table(name = "card_character" , uniqueConstraints = {
        @UniqueConstraint(columnNames = {"card_id" , "char_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardCharacter {

        @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id ;

        @Column(name = "char_id" , nullable = false)
    private String charId ; // unique id for this character , e.g "siteId-42"

    @ManyToOne
    @JoinColumn(name = "card_id" , nullable = false)
    private Card card ;

    @Column(name = "value")
    private String value ; // the actual character (String not char , so we can store it even after "deletion")

    @Column(name = "after_id")
    private String afterId ; // charId of the Character this comes after ; null = start of text

    @Column(name = "site_id" , nullable = false)
    private String siteId ; // which client/tab created this character

    @Column(name = "seq" , nullable = false)
    private Long seq ; // that sites local counter , used for tie-breaking

    @Column(name = "deleted" , nullable = false)
    private boolean deleted = false ;

    @Column(name = "created_at")
    private LocalDateTime createdAt ;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now() ;
    }

}
