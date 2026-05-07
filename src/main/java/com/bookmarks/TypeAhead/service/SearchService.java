package com.bookmarks.TypeAhead.service;

import com.bookmarks.TypeAhead.entity.SearchTerm;
import com.bookmarks.TypeAhead.repository.SearchTermRepository;
import com.bookmarks.TypeAhead.search.Trie;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class SearchService implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    private final Trie trie;
    private final SearchTermRepository searchTermRepository;
    private final RedisTemplate<String, Object> redisTemplate;


    public SearchService(Trie trie,
                         SearchTermRepository searchTermRepository,
                         RedisTemplate<String, Object> redisTemplate) {
        this.trie = trie;
        this.searchTermRepository = searchTermRepository;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        loadTrieFromDatabase();
    }

    public void loadTrieFromDatabase() {
        List<SearchTerm> allTerms = searchTermRepository.findAll();
        log.info("Terms from DB: {}", allTerms.size());
        for (SearchTerm term : allTerms) {
            log.debug("Inserting: {} freq: {}", term.getWord(), term.getFrequency());
            trie.insertWithFrequency(term.getWord(), term.getFrequency());
        }
        log.info("Trie loaded with {} terms", allTerms.size());
    }


    public List<String> getSuggestions(String prefix) {
        if (prefix == null || prefix.length() < 2) {
            return new ArrayList<>();
        }

        String lowerPrefix = prefix.toLowerCase();
        String cacheKey = "suggest:" + lowerPrefix;

        // 1. check Redis
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.info("Cache hit for: {}", lowerPrefix);
            return (List<String>) cached;
        }

        // 2. ask Trie
        log.info("Cache miss for: {}", lowerPrefix);
        List<String> suggestions = trie.getSuggestions(lowerPrefix);

        // 3. save to Redis
        redisTemplate.opsForValue().set(cacheKey, suggestions, 300, TimeUnit.SECONDS);

        return suggestions;
    }
    public void searchWord(String word) {
//        String lowerWord = word.toLowerCase();
//
//        String clean = word.toLowerCase().replaceAll("[^a-z]", "");
//        if (clean.length() < 2) return;

        // 1. check if word exists in DB
        Optional<SearchTerm> existing = searchTermRepository.findByWord(word);

        if (existing.isPresent()) {
            // increment frequency
            SearchTerm term = existing.get();
            term.setFrequency(term.getFrequency() + 1);
            searchTermRepository.save(term);
        } else {
            // add new word
            SearchTerm newTerm = new SearchTerm();
            newTerm.setWord(word);
            newTerm.setFrequency(1);
            searchTermRepository.save(newTerm);
            trie.insert(word); // add to Trie
        }

        // 2. update Trie frequency
        trie.incrementFrequency(word);

        // 3. invalidate Redis cache for all prefixes of this word
        for (int i = 2; i <= word.length(); i++) {
            String prefix = word.substring(0, i);
            redisTemplate.delete("suggest:" + prefix);
        }
    }
}
