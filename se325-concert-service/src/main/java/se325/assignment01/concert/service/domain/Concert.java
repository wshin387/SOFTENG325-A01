package se325.assignment01.concert.service.domain;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.*;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

@Entity
public class Concert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String imageName;
    private String blurb;

    @ElementCollection
    @CollectionTable(name = "CONCERT_DATES")
    private Set<LocalDateTime> dates;

    @ManyToMany
    @CollectionTable(name = "CONCERT_PERFORMER")
    private Set<Performer> performers;

    public Concert(Long id, String title, Set<LocalDateTime> dates, Set<Performer> performers, String imageName, String blurb) {
        this.id = id;
        this.title = title;
        this.dates = dates;
        this.performers = performers;
        this.imageName = imageName;
        this.blurb = blurb;
    }

    public Concert(String title, Set<LocalDateTime> dates, Set<Performer> performers, String imageName, String blurb) {
        this(null, title, dates, performers, imageName, blurb);
    }

    public Concert() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Set<LocalDateTime> getDates() {
        return dates;
    }

    public void setDates(Set<LocalDateTime> dates) {
        this.dates = dates;
    }

    public Set<Performer> getPerformers() {
        return performers;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public void setPerformers(Set<Performer> performers) {
        this.performers = performers;
    }

    public String getBlurb() {

        return blurb;
    }

    public void setBlurb(String blurb) {
        this.blurb = blurb;
    }
}
