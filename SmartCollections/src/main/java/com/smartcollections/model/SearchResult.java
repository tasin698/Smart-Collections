package com.smartcollections.model;

import java.io.Serializable;

/**
 * Represents a search result with relevance scoring.
 * 
 * Used in conjunction with PriorityQueue to return search results
 * ranked by relevance (frequency-based scoring).
 * 
 * KEY DESIGN:
 * - Implements Comparable for automatic ranking in PriorityQueue
 * - Higher score = more relevant = appears first in results
 * - Combines multiple scoring factors (tag frequency, keyword matches)
 */
public class SearchResult implements Comparable<SearchResult>, Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private final Item item;
    private final double relevanceScore;
    private final int tagFrequency;
    private final int keywordMatches;
    
    /**
     * Creates a search result with calculated relevance.
     * 
     * @param item The matched item
     * @param tagFrequency How frequently the item's tags appear in the corpus
     * @param keywordMatches Number of keywords that matched
     */
    public SearchResult(Item item, int tagFrequency, int keywordMatches) {
        this.item = item;
        this.tagFrequency = tagFrequency;
        this.keywordMatches = keywordMatches;
        this.relevanceScore = calculateRelevance();
    }
    
    /**
     * Calculates relevance score based on multiple factors.
     * 
     * Formula: 
     *   score = (keywordMatches * 10) + (tagFrequency * 2) + (item.rating * 5)
     * 
     * This weights:
     * - Keyword matches highest (direct relevance)
     * - Tag frequency medium (popularity/importance)
     * - Item rating (user preference)
     */
    private double calculateRelevance() {
        double score = 0.0;
        
        // Keyword match bonus (most important)
        score += keywordMatches * 10.0;
        
        // Tag frequency bonus
        score += tagFrequency * 2.0;
        
        // User rating bonus
        score += item.getRating() * 5.0;
        
        // Recency bonus (newer items get slight boost)
        long daysOld = java.time.Duration.between(
            item.getCreatedAt(), 
            java.time.LocalDateTime.now()
        ).toDays();
        
        if (daysOld < 7) {
            score += 5.0;  // Created within a week
        } else if (daysOld < 30) {
            score += 2.0;  // Created within a month
        }
        
        return score;
    }
    
    // ==================== GETTERS ====================
    
    public Item getItem() {
        return item;
    }
    
    public double getRelevanceScore() {
        return relevanceScore;
    }
    
    public int getTagFrequency() {
        return tagFrequency;
    }
    
    public int getKeywordMatches() {
        return keywordMatches;
    }
    
    // ==================== COMPARABLE IMPLEMENTATION ====================
    
    /**
     * Compares search results by relevance score.
     * 
     * HIGHER score = MORE relevant = APPEARS FIRST
     * 
     * PriorityQueue is a min-heap, so we reverse the comparison
     * to get max-heap behavior (highest score first).
     */
    @Override
    public int compareTo(SearchResult other) {
        // Reverse comparison for descending order
        int scoreComparison = Double.compare(
            other.relevanceScore, 
            this.relevanceScore
        );
        
        if (scoreComparison != 0) {
            return scoreComparison;
        }
        
        // If scores are equal, prioritize more recent items
        return other.item.getCreatedAt().compareTo(this.item.getCreatedAt());
    }
    
    @Override
    public String toString() {
        return "SearchResult{" +
                "item=" + item.getTitle() +
                ", score=" + String.format("%.2f", relevanceScore) +
                ", tagFreq=" + tagFrequency +
                ", matches=" + keywordMatches +
                '}';
    }
}
