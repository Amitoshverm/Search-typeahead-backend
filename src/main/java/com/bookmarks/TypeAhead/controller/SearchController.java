package com.bookmarks.TypeAhead.controller;

import com.bookmarks.TypeAhead.service.SearchService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/suggest")
    public ResponseEntity<List<String>> getSuggestions(@RequestParam String prefix) {
        String clean = prefix.toLowerCase().replaceAll("[^a-z]", "");
        if (clean.length() < 2) return ResponseEntity.ok(List.of());
        List<String> suggestions = searchService.getSuggestions(clean);
        return new ResponseEntity<>(suggestions, HttpStatus.OK);
    }

    @PostMapping("/query")
    public ResponseEntity<Void> searchWord(@RequestParam String word) {
        String clean = word.toLowerCase().replaceAll("[^a-z]", "");
        if (clean.length() < 2) return new ResponseEntity<>(HttpStatus.OK);
        searchService.searchWord(clean);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
