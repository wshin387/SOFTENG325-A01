package se325.assignment01.concert.service.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.persistence.*;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;



@Entity
@Table(name = "CONCERTS")
public class Concert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="ID",nullable = false, unique = true)
    private Long id;

    @Column(name="TITLE")
    private String title;

    @Column(name="IMAGE_NAME")
    private String imageName;

    @Column(name="BLURB", length=1024)
    private String blurb;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "CONCERT_DATES")
    @Column(name="DATE")
    private Set<LocalDateTime> dates;

    @ManyToMany(cascade = CascadeType.PERSIST,fetch = FetchType.EAGER)
    @JoinTable(name = "CONCERT_PERFORMER",
        joinColumns = @JoinColumn(name="CONCERT_ID", referencedColumnName = "id"),
        inverseJoinColumns = @JoinColumn(name = "PERFORMER_ID", referencedColumnName = "id"))
    @Column(name="PERFORMER")
    private Set<Performer> performers;

    public Concert(){
    }
    public Concert(Long id, String title, String imageName, String blurb,Set<Performer> performers){
        this.id = id;
        this.title = title;
        this.imageName = imageName;
        this.blurb = blurb;
        this.performers = performers;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setImage(String imageName) {
        this.imageName = imageName;
    }

    public void setBlurb(String blurb) {
        this.blurb = blurb;
    }

    public void setDates(Set<LocalDateTime> dates) {
        this.dates = dates;
    }

    public void setPerformers(Set<Performer> performers) {
        this.performers = performers;
    }

    public Set<LocalDateTime> getDates() {
        return this.dates;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getImage() {
        return imageName;
    }

    public String getBlurb() {
        return blurb;
    }

    public Set<Performer> getPerformers() {
        return this.performers;
    }


    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Concert))
            return false;
        if (obj == this)
            return true;

        Concert rhs = (Concert) obj;

        return new EqualsBuilder().
                append(title, rhs.title).
                isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 31).
                append(title).hashCode();
    }
}
