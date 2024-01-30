package network;

import java.io.IOException;

public interface On_arrival {
    void on_arrival(byte[] msg);
    default void timedout() throws IOException {
        Server.disconnect(true);
    }
}