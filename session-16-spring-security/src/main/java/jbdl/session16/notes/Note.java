package jbdl.session16.notes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/** A short private note. `owner` is the username of whoever created it. */
@Entity
public class Note {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String owner;

    @Column(nullable = false, length = 500)
    private String text;

    protected Note() {
    }

    public Note(String owner, String text) {
        this.owner = owner;
        this.text = text;
    }

    public Long getId() {
        return id;
    }

    public String getOwner() {
        return owner;
    }

    public String getText() {
        return text;
    }
}
