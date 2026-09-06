package com.harsh.collab_board.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "boards")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Board {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id ;

    @Column(nullable = false)
    private String title ;

    private String description ;

    @Column(name = "created_at")
    private LocalDateTime createdAt ;

    @OneToMany(mappedBy = "board" , cascade = CascadeType.ALL , orphanRemoval = true)
    @com.fasterxml.jackson.annotation.JsonManagedReference
    @jakarta.persistence.OrderBy("position ASC")
    private List<BoardList> lists = new ArrayList<>() ;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now() ;
    }


}
