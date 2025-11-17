package org.example.profile;

import java.time.LocalDateTime;
import java.util.Map;

public class Profile {
    private String userId;
    private String targetRole;
    private Map<String, Integer> skills;
    private int experienceYears;
    private LocalDateTime updatedAt;

    public Profile(String userId, String targetRole, Map<String, Integer> skills, int experienceYears) {
        this.userId = userId;
        this.targetRole = targetRole;
        this.skills = skills;
        this.experienceYears = experienceYears;
        this.updatedAt = LocalDateTime.now();
    }

    public String getUserId() { return userId; }
    public String getTargetRole() { return targetRole; }
    public Map<String, Integer> getSkills() { return skills; }
    public int getExperienceYears() { return experienceYears; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setTargetRole(String targetRole) { this.targetRole = targetRole; }
    public void setSkills(Map<String, Integer> skills) { this.skills = skills; }
    public void setExperienceYears(int experienceYears) { this.experienceYears = experienceYears; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "Profile{" +
                "targetRole='" + targetRole + '\'' +
                ", skills=" + skills +
                ", experienceYears=" + experienceYears +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
