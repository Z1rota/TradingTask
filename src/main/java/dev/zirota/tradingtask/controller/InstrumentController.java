package dev.zirota.tradingtask.controller;

import dev.zirota.tradingtask.dto.instrument.CreateInstrumentRequest;
import dev.zirota.tradingtask.dto.instrument.InstrumentResponse;
import dev.zirota.tradingtask.service.InstrumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/instruments")
@RequiredArgsConstructor
public class InstrumentController {

    private final InstrumentService instrumentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InstrumentResponse create(@Valid @RequestBody CreateInstrumentRequest request) {
        return instrumentService.create(request);
    }

    @GetMapping
    public List<InstrumentResponse> findAll() {
        return instrumentService.findAll();
    }

    @GetMapping("/{seccode}")
    public InstrumentResponse findBySeccode(@PathVariable String seccode) {
        return instrumentService.findBySeccode(seccode);
    }
}
