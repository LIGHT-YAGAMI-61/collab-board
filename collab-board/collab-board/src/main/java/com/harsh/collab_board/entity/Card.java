package com.harsh.collab_board.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id ;

    @Column(nullable = false)
    private String title ;

    @Column(name = "position")
    private Integer position ;

    @Column(columnDefinition = "TEXT")
    private String description ;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt ;

    @Version
    private Long version ;

    @ManyToOne
    @JoinColumn(name = "board_list_id" , nullable = false)
    @com.fasterxml.jackson.annotation.JsonBackReference
    private BoardList boardList ;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        updatedAt = LocalDateTime.now() ;


    }


}
