package dev.lifesteal.homes.data;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HomeRepository extends AutoCloseable {

    List<StoredHome> findAll(UUID playerId);

    Optional<StoredHome> find(UUID playerId, String key);

    int count(UUID playerId);

    void save(StoredHome home);

    boolean delete(UUID playerId, String key);

    @Override
    void close();
}
