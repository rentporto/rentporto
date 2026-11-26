package pt.rentporto.fleet.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Place where vehicles are picked up and returned. The stations are reference data, added by the migrations of the
 * service.
 */
@Entity
@Table(name = "stations")
public class Station {

    @Id
    @Column(name = "code")
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    protected Station() {
    }

    public String code() {
        return code;
    }

    public String name() {
        return name;
    }
}
