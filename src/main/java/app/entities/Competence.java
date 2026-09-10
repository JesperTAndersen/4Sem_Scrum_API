package app.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@Builder
@EqualsAndHashCode
@AllArgsConstructor
@Entity
@Table(name = "competences")
public class Competence
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "competence_id", nullable = false)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "rate", nullable = false)
    private BigDecimal rate;
}
