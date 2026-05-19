package com.gayatri.dentalclinic.entity;

import com.gayatri.dentalclinic.enums.PaymentMode;
import com.gayatri.dentalclinic.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "bill_id")
    private Bill bill;

    @Enumerated(EnumType.STRING)
    private PaymentMode paymentMode;

    private BigDecimal amount;

    private LocalDate paymentDate;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private String gatewayOrderId;
    private String gatewayPaymentId;
    @Column(length = 512)
    private String gatewaySignature;
}
