package dev.zirota.tradingtask.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "instruments", uniqueConstraints = @UniqueConstraint(name = "uk_instruments_seccode", columnNames = "seccode"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Instrument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String seccode;

    @Column(nullable = false, length = 16)
    private String type = "STOCK";

    @Column(nullable = false)
    private int lotSize;

    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal minPriceStep;

    @Column(nullable = false, length = 3)
    private String quoteCurrency;

    public Instrument(String seccode, int lotSize, BigDecimal minPriceStep, String quoteCurrency) {
        this.seccode = seccode;
        this.lotSize = lotSize;
        this.minPriceStep = minPriceStep;
        this.quoteCurrency = quoteCurrency;
    }
}
