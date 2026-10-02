package com.gayatri.dentalclinic.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "bills")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    @Column(precision = 38, scale = 2)
    private BigDecimal totalAmount;
    @Column(precision = 38, scale = 2)
    private BigDecimal discount;
    @Column(precision = 38, scale = 2)
    private BigDecimal finalAmount;
    private LocalDate billDate;



}
