package dev.zirota.tradingtask.service;

import dev.zirota.tradingtask.dto.instrument.CreateInstrumentRequest;
import dev.zirota.tradingtask.dto.instrument.InstrumentResponse;
import dev.zirota.tradingtask.entity.Instrument;
import dev.zirota.tradingtask.repository.InstrumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class InstrumentService {

    private final InstrumentRepository instrumentRepository;

    @Transactional
    public InstrumentResponse create(CreateInstrumentRequest request) {
        String seccode = request.seccode().toUpperCase(Locale.ROOT);
        if (instrumentRepository.existsBySeccode(seccode)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Instrument already exists");
        }
        Instrument instrument = new Instrument(seccode, request.lotSize(), request.minPriceStep(),
                request.quoteCurrency().getCurrencyCode());
        return InstrumentResponse.from(instrumentRepository.save(instrument));
    }

    @Transactional(readOnly = true)
    public List<InstrumentResponse> findAll() {
        return instrumentRepository.findAll(Sort.by("seccode")).stream()
                .map(InstrumentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public InstrumentResponse findBySeccode(String seccode) {
        Instrument instrument = instrumentRepository.findBySeccode(seccode.toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Instrument not found"));
        return InstrumentResponse.from(instrument);
    }
}
