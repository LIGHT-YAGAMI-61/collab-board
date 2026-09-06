package com.harsh.collab_board.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "board_members")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BoardMember {

        @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id ;

        @ManyToOne
    @JoinColumn(name = "board_id", nullable = false)
    private Board board ;

        @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user ;

        @Column(nullable = false)
        private String role ; // ADMIN or MEMEBER

        @Column(name = "joined_at")
    private LocalDateTime joinedAt ;

        @PrePersist
    protected void onCreate() {
            joinedAt = LocalDateTime.now() ;
        }
}
