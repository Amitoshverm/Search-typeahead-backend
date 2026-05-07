package com.bookmarks.TypeAhead.search;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class Trie {

    TrieNode root = new TrieNode(' ');

    public void insert(String word) {
        TrieNode current = root;
        int l =  word.length();
        for (int i = 0; i < l; i++) {
            int idx = word.charAt(i) - 'a';
            if (idx < 0 || idx >= 26) return;
            if (current.children[idx] == null) {
                current.children[idx] = new TrieNode(word.charAt(i));
            }
            current = current.children[idx];
        }
        current.freq++;
    }
    public boolean search(String word) {
        TrieNode current = root;
        int l =  word.length();
        for (int i = 0; i < l; i++) {
            int idx = word.charAt(i) - 'a';
            if (idx < 0 || idx >= 26) return false;
            if (current.children[idx] == null) {
                return false;
            }
            current = current.children[idx];
        }
        return current.freq > 0;
    }

    public List<String> getSuggestions(String prefix) {
        // minimum prefix length check
        if (prefix.length() < 2) {
            return new ArrayList<>();
        }

        TrieNode current = root;
        List<String> results = new ArrayList<>();

        // step 1 — walk to end of prefix
        for (int i = 0; i < prefix.length(); i++) {
            int index = prefix.charAt(i) - 'a';
            if (index < 0 || index >= 26) return results;
            if (current.children[index] == null) {
                return results; // prefix not found
            }
            current = current.children[index];
        }

        // step 2 — collect all words with their frequencies
        List<int[]> freqList = new ArrayList<>(); // stores [wordIndex, freq]
        List<String> allWords = new ArrayList<>();
        dfs(current, new StringBuilder(prefix), allWords, freqList);

        // step 3 — sort by frequency descending
        freqList.sort((a, b) -> b[1] - a[1]);

        // step 4 — return top 5
        List<String> topSuggestions = new ArrayList<>();
        for (int i = 0; i < Math.min(5, freqList.size()); i++) {
            topSuggestions.add(allWords.get(freqList.get(i)[0]));
        }

        return topSuggestions;
    }

    private void dfs(TrieNode node, StringBuilder current,
                     List<String> words, List<int[]> freqList) {
        if (node.freq > 0) {
            freqList.add(new int[]{words.size(), node.freq});
            words.add(current.toString());
        }
        for (int i = 0; i < 26; i++) {
            if (node.children[i] != null) {
                current.append((char) ('a' + i));
                dfs(node.children[i], current, words, freqList);
                current.deleteCharAt(current.length() - 1);
            }
        }
    }
    // load from DB with existing frequency
    public void insertWithFrequency(String word, int frequency) {
        TrieNode current = root;
        for (int i = 0; i < word.length(); i++) {
            int index = word.charAt(i) - 'a';
            if (index < 0 || index >= 26) return;
            if (current.children[index] == null) {
                current.children[index] = new TrieNode(word.charAt(i));
            }
            current = current.children[index];
        }
        current.freq = frequency; // set directly, don't increment
    }

    // increment frequency of existing word
    public void incrementFrequency(String word) {
        TrieNode current = root;
        for (int i = 0; i < word.length(); i++) {
            int index = word.charAt(i) - 'a';
            if (index < 0 || index >= 26) return;
            if (current.children[index] == null) return; // word doesn't exist
            current = current.children[index];
        }
        current.freq++;
    }
}
