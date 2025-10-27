package com.smartcollections.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a study task associated with an Item.
 * 
 * Tasks are managed in a PriorityQueue where:
 * - Higher priority = processed first
 * - Earlier deadline = higher priority
 * 
 * KEY DESIGN DECISIONS:
 * - Implements Comparable for PriorityQueue ordering
 * - Priority calculation: combines explicit priority + deadline urgency
 * - Serializable for persistence
 * - Immutable task ID
 */
public class Task implements Serializable, Comparable<Task> {
    
    private static final long serialVersionUID = 1L;
    
    // Unique identifier
    private final String taskId;
    
    // Associated item
    private String itemId;
    private String itemTitle;  // Cached for display
    
    // Task details
    private String description;
    private TaskPriority priority;
    private LocalDateTime deadline;
    private LocalDateTime createdAt;
    private TaskStatus status;
    
    // Optional fields
    private String notes;
    private int estimatedMinutes;
    
    /**
     * Task priority levels.
     */
    public enum TaskPriority {
        LOW(1),
        MEDIUM(2),
        HIGH(3),
        URGENT(4);
        
        private final int value;
        
        TaskPriority(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
    }
    
    /**
     * Task status.
     */
    public enum TaskStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        CANCELLED
    }
    
    /**
     * Constructor for creating a new task.
     */
    public Task(String itemId, String itemTitle, String description, 
                TaskPriority priority, LocalDateTime deadline) {
        this.taskId = UUID.randomUUID().toString();
        this.itemId = itemId;
        this.itemTitle = itemTitle;
        this.description = description;
        this.priority = priority != null ? priority : TaskPriority.MEDIUM;
        this.deadline = deadline;
        this.createdAt = LocalDateTime.now();
        this.status = TaskStatus.PENDING;
        this.estimatedMinutes = 0;
    }
    
    // ==================== GETTERS ====================
    
    public String getTaskId() {
        return taskId;
    }
    
    public String getItemId() {
        return itemId;
    }
    
    public String getItemTitle() {
        return itemTitle;
    }
    
    public String getDescription() {
        return description;
    }
    
    public TaskPriority getPriority() {
        return priority;
    }
    
    public LocalDateTime getDeadline() {
        return deadline;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public TaskStatus getStatus() {
        return status;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public int getEstimatedMinutes() {
        return estimatedMinutes;
    }
    
    // ==================== SETTERS ====================
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public void setPriority(TaskPriority priority) {
        this.priority = priority;
    }
    
    public void setDeadline(LocalDateTime deadline) {
        this.deadline = deadline;
    }
    
    public void setStatus(TaskStatus status) {
        this.status = status;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public void setEstimatedMinutes(int estimatedMinutes) {
        this.estimatedMinutes = estimatedMinutes;
    }
    
    // ==================== PRIORITY CALCULATION ====================
    
    /**
     * Calculates the effective priority score for queue ordering.
     * 
     * Formula: base_priority + deadline_urgency_bonus
     * - Base priority: 1-4 (from TaskPriority enum)
     * - Deadline urgency: +10 if within 24 hours, +5 if within 7 days
     * 
     * This ensures tasks with sooner deadlines are processed first,
     * even if they have lower explicit priority.
     */
    public int getEffectivePriority() {
        int basePriority = priority.getValue();
        int urgencyBonus = calculateUrgencyBonus();
        return basePriority + urgencyBonus;
    }
    
    /**
     * Calculates urgency bonus based on deadline proximity.
     */
    private int calculateUrgencyBonus() {
        if (deadline == null) {
            return 0;
        }
        
        LocalDateTime now = LocalDateTime.now();
        
        // Already overdue
        if (deadline.isBefore(now)) {
            return 20;  // Maximum urgency!
        }
        
        long hoursUntilDeadline = java.time.Duration.between(now, deadline).toHours();
        
        if (hoursUntilDeadline <= 24) {
            return 10;  // Within 24 hours
        } else if (hoursUntilDeadline <= 168) { // 7 days
            return 5;   // Within a week
        }
        
        return 0;  // No urgency bonus
    }
    
    /**
     * Checks if the task is overdue.
     */
    public boolean isOverdue() {
        return deadline != null && 
               deadline.isBefore(LocalDateTime.now()) && 
               status != TaskStatus.COMPLETED &&
               status != TaskStatus.CANCELLED;
    }
    
    /**
     * Checks if the task is due soon (within 24 hours).
     */
    public boolean isDueSoon() {
        if (deadline == null) return false;
        LocalDateTime now = LocalDateTime.now();
        long hoursUntilDeadline = java.time.Duration.between(now, deadline).toHours();
        return hoursUntilDeadline > 0 && hoursUntilDeadline <= 24;
    }
    
    // ==================== COMPARABLE IMPLEMENTATION ====================
    
    /**
     * Compares tasks for PriorityQueue ordering.
     * 
     * HIGHER effective priority = HIGHER in queue (processed first)
     * PriorityQueue is a min-heap by default, so we reverse the comparison.
     * 
     * If priorities are equal, compare by deadline (sooner first).
     */
    @Override
    public int compareTo(Task other) {
        // Compare by effective priority (reversed for max-heap behavior)
        int priorityComparison = Integer.compare(
            other.getEffectivePriority(), 
            this.getEffectivePriority()
        );
        
        if (priorityComparison != 0) {
            return priorityComparison;
        }
        
        // If priorities are equal, compare by deadline (sooner first)
        if (this.deadline != null && other.deadline != null) {
            return this.deadline.compareTo(other.deadline);
        }
        
        // If one has no deadline, prioritize the one with a deadline
        if (this.deadline != null) return -1;
        if (other.deadline != null) return 1;
        
        // Finally, compare by creation time (older first)
        return this.createdAt.compareTo(other.createdAt);
    }
    
    // ==================== OBJECT METHODS ====================
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return Objects.equals(taskId, task.taskId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(taskId);
    }
    
    @Override
    public String toString() {
        return "Task{" +
                "taskId='" + taskId + '\'' +
                ", itemTitle='" + itemTitle + '\'' +
                ", priority=" + priority +
                ", deadline=" + deadline +
                ", status=" + status +
                ", effectivePriority=" + getEffectivePriority() +
                '}';
    }
}
