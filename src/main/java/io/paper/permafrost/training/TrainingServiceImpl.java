package io.paper.permafrost.training;

import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import io.paper.permafrost.Main;

public class TrainingServiceImpl implements TrainingService {

    private final Main main;

    public TrainingServiceImpl(Main main) {
        this.main = main;
    }

    @Override
    public TrainingSession getSession(UUID player) {
        return main.getDataLoader().getTrainingSession(player);
    }

    @Override
    public TrainingSession createSession(UUID leader, TrainingType type) {
        TrainingSession session = new TrainingSession(leader, type, main);
        main.getDataLoader().addPlayerToTraining(leader, session);
        return session;
    }

    @Override
    public void invite(TrainingSession session, UUID target) {
        session.invite(target);
    }

    @Override
    public boolean isInvited(TrainingSession session, UUID target) {
        return session.isInvited(target);
    }

    @Override
    public void join(TrainingSession session, UUID player) {
        session.addPlayer(player);
        session.consumeInvite(player); // optional but recommended
        main.getDataLoader().addPlayerToTraining(player, session);
    }

    @Override
    public void leave(UUID player) {
        TrainingSession session = main.getDataLoader().getTrainingSession(player);
        if (session == null) return;

        boolean leaderLeaving = session.getLeader().equals(player);

        if (leaderLeaving) {
            // end session for everyone using your existing endSession()
            session.endSession();
            return;
        }

        // normal member leave
        session.removePlayer(player);
        main.getDataLoader().removePlayerFromTraining(player);

        Player p = Bukkit.getPlayer(player);
        if (p != null) p.performCommand("spawn");
    }

    @Override
    public boolean isLeader(TrainingSession session, UUID player) {
        return session.getLeader().equals(player);
    }
}
