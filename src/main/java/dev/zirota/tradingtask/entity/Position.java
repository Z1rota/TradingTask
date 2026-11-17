package dev.zirota.tradingtask.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;

@Entity
@Table(name = "positions", uniqueConstraints = @UniqueConstraint(
        name = "uk_positions_account_instrument", columnNames = {"account_id", "instrument_id"}))
@Check(constraints = "quantity >= 0 and reserved_quantity >= 0 and reserved_quantity <= quantity")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Position {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(nullable = false)
    private Instrument instrument;

    @Column(nullable = false)
    private long quantity;

    @Column(nullable = false)
    private long reservedQuantity;

    public Position(Account account, Instrument instrument) {
        if (!account.getCurrency().equals(instrument.getQuoteCurrency())) {
            throw new IllegalArgumentException("Account and instrument currencies must match");
        }
        this.account = account;
        this.instrument = instrument;
    }
}
