package nl.timtoys.corruptedadventure.entity.nightmare.director;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/** Whom a Nightmare comes for: the candidate with the most threat, if that reaches the threshold. Pure. */
public final class NightmareTargets {
    /** A player in the Nightmare's ring who counts in a fight, with the threat of that ring. */
    public record Candidate(UUID player, float threat) {
    }

    private NightmareTargets() {
    }

    /** The highest threat at or above {@code threshold}; on a tie the first. Empty if nobody reaches it. */
    public static Optional<UUID> choose(Collection<Candidate> candidates, float threshold) {
        Candidate best = null;
        for (Candidate candidate : candidates) {
            if (candidate.threat() >= threshold && (best == null || candidate.threat() > best.threat())) {
                best = candidate;
            }
        }
        return Optional.ofNullable(best).map(Candidate::player);
    }
}
