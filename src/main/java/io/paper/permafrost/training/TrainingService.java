package io.paper.permafrost.training;

import java.util.Set;
import java.util.UUID;

public interface TrainingService {
    TrainingSession getSession(UUID player);
    TrainingSession createSession(UUID leader, TrainingType type);

    void invite(TrainingSession session, UUID target);
    boolean isInvited(TrainingSession session, UUID target);

    void join(TrainingSession session, UUID player);
    void leave(UUID player);

    boolean isLeader(TrainingSession session, UUID player);
}