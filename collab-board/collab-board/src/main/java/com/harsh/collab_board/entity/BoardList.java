package com.harsh.collab_board.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "board_lists")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BoardList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id ;

    @Column(nullable = false)
    private String title ;

    @Column(name = "position")
    private Integer position ;

    @ManyToOne
    @JoinColumn(name = "board_id" , nullable = false)
    @com.fasterxml.jackson.annotation.JsonBackReference
    private Board board ;

    @OneToMany(mappedBy = "boardList" , cascade = CascadeType.ALL , orphanRemoval = true)
    @com.fasterxml.jackson.annotation.JsonManagedReference
    @jakarta.persistence.OrderBy("position ASC")
    private List<Card> cards = new ArrayList<>() ;
}
