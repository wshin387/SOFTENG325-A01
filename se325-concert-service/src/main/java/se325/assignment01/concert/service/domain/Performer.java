package se325.assignment01.concert.service.domain;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.*;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import se325.assignment01.concert.common.types.Genre;

@Entity
@Table(name = "PERFORMERS")
public class Performer{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "NAME")
    private String name;
    @Column(name = "IMAGE_NAME")
    private String imageName;

    @Enumerated(EnumType.STRING)
    @Column(name="GENRE")
    private Genre genre;

    @Column(name="BLURB", length = 1024)
    private String blurb;

    @ManyToMany(cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)
    private Set<Concert> concerts;

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setImage(String imageName) {
        this.imageName = imageName;
    }

    public void setGenre(Genre genre) {
        this.genre = genre;
    }

    public void setBlurb(String blurb) {
        this.blurb = blurb;
    }

    public void setConcerts(Set<Concert> concerts) {
        this.concerts = concerts;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getImage() {
        return imageName;
    }

    public Genre getGenre() {
        return genre;
    }

    public String getBlurb() {
        return blurb;
    }

    public Set<Concert> getConcerts() {
        return concerts;
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("Performer, id: ");
        buffer.append(id);
        buffer.append(", name: ");
        buffer.append(name);
        buffer.append(", s3 image: ");
        buffer.append(imageName);
        buffer.append(", genre: ");
        buffer.append(genre.toString());

        return buffer.toString();
    }
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Performer))
            return false;


        if (obj == this)
            return true;

        Performer rhs = (Performer) obj;

        return new EqualsBuilder().
                append(name, rhs.name).
                isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 31).
                append(name).hashCode();
    }
}
