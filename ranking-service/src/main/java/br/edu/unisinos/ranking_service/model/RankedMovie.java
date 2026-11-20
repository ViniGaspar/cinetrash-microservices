package br.edu.unisinos.ranking_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ranked_movies")
@Getter
@Setter
@NoArgsConstructor
public class RankedMovie {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(name = "movie_id", nullable = false, unique = true)
    private Long movieId;

    @Column(name = "external_id", nullable = false)
    private Long externalId;

    @Column(nullable = false)
    private String title;

    @Column(name = "poster_path", length = 512)
    private String posterPath;

    @Column(name = "release_year")
    private Integer releaseYear;
}
