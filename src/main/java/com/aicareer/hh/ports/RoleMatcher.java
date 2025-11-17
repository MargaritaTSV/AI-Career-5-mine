package com.aicareer.hh.ports;

import java.util.Set;
import com.aicareer.hh.model.RoleMatch;

public interface RoleMatcher {
    RoleMatch match(Set<String> vacancySkills, Set<String> roleSkills, String targetRole);
}
