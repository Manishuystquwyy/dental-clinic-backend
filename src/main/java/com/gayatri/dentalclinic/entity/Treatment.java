package com.gayatri.dentalclinic.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "treatment")
public class Treatment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    @Column(name = "treatment_name")
    private String treatmentName;

    private String description;

    private BigDecimal cost;
}
