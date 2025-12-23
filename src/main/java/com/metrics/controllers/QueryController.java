package com.metrics.controllers;


import com.metrics.models.query.QueryRequest;
import com.metrics.models.query.QueryResponse;
import com.metrics.services.query.QueryProcessingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/query")
@RequiredArgsConstructor
public class QueryController {

    private final QueryProcessingService queryProcessingService;

    @PostMapping
    public QueryResponse query(@RequestBody QueryRequest request) {
        return queryProcessingService.execute(request);
    }
}
